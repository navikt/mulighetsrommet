package no.nav.mulighetsrommet.api.avtale

import arrow.core.*
import arrow.core.raise.either
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.mapper.AvtaleDboMapper
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.avtale.model.AvtaleStatusDto
import no.nav.mulighetsrommet.api.avtale.model.OpsjonLoggEntry
import no.nav.mulighetsrommet.api.avtale.model.OpsjonLoggStatus
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkDto
import no.nav.mulighetsrommet.api.gjennomforing.task.InitialLoadGjennomforinger
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.responses.PaginatedResponse
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class AvtaleService(
    private val db: ApiDatabase,
    private val arrangorService: ArrangorService,
    private val validator: AvtaleValidator,
    private val gjennomforingPublisher: InitialLoadGjennomforinger,
) {
    suspend fun upsert(
        request: AvtaleRequest,
        navIdent: NavIdent,
        today: LocalDate = LocalDate.now(),
    ): Either<List<FieldError>, AvtaleDto> = either {
        val previous = get(request.id)

        val arrangor = request.arrangor?.let {
            val (arrangor, underenheter) = syncArrangorerFromBrreg(
                it.hovedenhet,
                it.underenheter,
            ).bind()
            AvtaleDbo.Arrangor(
                hovedenhet = arrangor.id,
                underenheter = underenheter.map { underenhet -> underenhet.id },
                kontaktpersoner = it.kontaktpersoner,
            )
        }

        val status = resolveStatus(request, previous, today)
        val dbo = validator
            .validate(AvtaleDboMapper.fromAvtaleRequest(request, arrangor, status), previous)
            .bind()

        if (previous != null && AvtaleDboMapper.fromAvtaleDto(previous) == dbo) {
            return@either previous
        }

        db.transaction {
            queries.avtale.upsert(dbo)

            dispatchNotificationToNewAdministrators(dbo, navIdent)

            val dto = getOrError(dbo.id)
            val operation = if (previous == null) {
                "Opprettet avtale"
            } else {
                "Redigerte avtale"
            }
            logEndring(operation, dto, navIdent)

            schedulePublishGjennomforingerForAvtale(dto)

            dto
        }
    }

    fun get(id: UUID): AvtaleDto? = db.session {
        queries.avtale.get(id)
    }

    fun getAll(
        filter: AvtaleFilter,
        pagination: Pagination,
    ): PaginatedResponse<AvtaleDto> = db.session {
        val (totalCount, items) = queries.avtale.getAll(
            pagination = pagination,
            tiltakstypeIder = filter.tiltakstypeIder,
            search = filter.search,
            statuser = filter.statuser,
            avtaletyper = filter.avtaletyper,
            navRegioner = filter.navRegioner,
            sortering = filter.sortering,
            arrangorIds = filter.arrangorIds,
            administratorNavIdent = filter.administratorNavIdent,
            personvernBekreftet = filter.personvernBekreftet,
        )

        PaginatedResponse.of(pagination, totalCount, items)
    }

    fun avsluttAvtale(
        id: UUID,
        avsluttetTidspunkt: LocalDateTime,
        endretAv: Agent,
    ): Either<FieldError, AvtaleDto> = db.transaction {
        val avtale = getOrError(id)

        if (avtale.status !is AvtaleStatusDto.Aktiv) {
            return FieldError.root("Avtalen må være aktiv for å kunne avsluttes").left()
        }

        val tidspunktForSlutt = avtale.sluttDato?.plusDays(1)?.atStartOfDay()
        if (tidspunktForSlutt == null || avsluttetTidspunkt.isBefore(tidspunktForSlutt)) {
            return FieldError.root("Avtalen kan ikke avsluttes før sluttdato").left()
        }

        queries.avtale.setStatus(id, AvtaleStatus.AVSLUTTET, null, null)

        val dto = getOrError(id)
        logEndring("Avtalen ble avsluttet", dto, endretAv)

        dto.right()
    }

    fun avbrytAvtale(id: UUID, navIdent: NavIdent, aarsak: AvbruttAarsak): Either<FieldError, AvtaleDto> = db.transaction {
        val avtale = getOrError(id)

        if (aarsak is AvbruttAarsak.Annet && aarsak.beskrivelse.isBlank()) {
            return FieldError.root("Beskrivelse er obligatorisk når “Annet” er valgt som årsak").left()
        }

        when (avtale.status) {
            is AvtaleStatusDto.Utkast, is AvtaleStatusDto.Aktiv -> Unit
            is AvtaleStatusDto.Avbrutt -> return FieldError.root("Avtalen er allerede avbrutt").left()
            is AvtaleStatusDto.Avsluttet -> return FieldError.root("Avtalen er allerede avsluttet").left()
        }

        val (_, gjennomforinger) = queries.gjennomforing.getAll(
            avtaleId = id,
            statuser = listOf(GjennomforingStatus.GJENNOMFORES),
        )
        if (gjennomforinger.isNotEmpty()) {
            val message = listOf(
                "Avtalen har",
                gjennomforinger.size,
                if (gjennomforinger.size > 1) "aktive gjennomføringer" else "aktiv gjennomføring",
                "og kan derfor ikke avbrytes",
            ).joinToString(" ")
            return FieldError.root(message).left()
        }

        queries.avtale.setStatus(id, AvtaleStatus.AVBRUTT, LocalDateTime.now(), aarsak)

        val dto = getOrError(id)
        logEndring("Avtalen ble avbrutt", dto, navIdent)

        dto.right()
    }

    fun registrerOpsjon(
        entry: OpsjonLoggEntry,
        today: LocalDate = LocalDate.now(),
    ): Either<FieldError, AvtaleDto> = db.transaction {
        if (entry.status == OpsjonLoggStatus.OPSJON_UTLOST) {
            val avtale = getOrError(entry.avtaleId)

            val skalIkkeUtloseOpsjonerForAvtale = avtale.opsjonerRegistrert
                ?.any { it.status === OpsjonLoggStatus.SKAL_IKKE_UTLOSE_OPSJON } == true
            if (skalIkkeUtloseOpsjonerForAvtale) {
                return FieldError.of(OpsjonLoggEntry::status, "Kan ikke utløse flere opsjoner").left()
            }

            val maksVarighet = avtale.opsjonsmodell.opsjonMaksVarighet
            if (entry.sluttdato != null && entry.sluttdato.isAfter(maksVarighet)) {
                return FieldError.of(
                    OpsjonLoggEntry::sluttdato,
                    "Ny sluttdato er forbi maks varighet av avtalen",
                ).left()
            }

            if (entry.forrigeSluttdato == null) {
                return FieldError.of(OpsjonLoggEntry::forrigeSluttdato, "Forrige sluttdato må være satt").left()
            }
        }

        queries.opsjoner.insert(entry)

        if (entry.sluttdato != null) {
            updateAvtaleVarighet(entry.avtaleId, entry.sluttdato, today)
        }

        val avtale = getOrError(entry.avtaleId)
        val operation = when (entry.status) {
            OpsjonLoggStatus.OPSJON_UTLOST -> "Opsjon registrert"
            OpsjonLoggStatus.SKAL_IKKE_UTLOSE_OPSJON -> "Registrert at opsjon ikke skal utløses for avtalen"
        }
        logEndring(operation, avtale, entry.registrertAv)
        avtale.right()
    }

    fun slettOpsjon(
        avtaleId: UUID,
        opsjonId: UUID,
        slettesAv: NavIdent,
        today: LocalDate = LocalDate.now(),
    ): Either<FieldError, AvtaleDto> = db.transaction {
        val opsjoner = queries.opsjoner.getByAvtaleId(avtaleId)

        val sisteOpsjon = opsjoner.firstOrNull()
        if (sisteOpsjon == null || sisteOpsjon.id != opsjonId) {
            return FieldError.of("Opsjonen kan ikke slettes fordi det ikke er den siste utløste opsjonen").left()
        }

        if (sisteOpsjon.status == OpsjonLoggStatus.OPSJON_UTLOST) {
            val nySluttDato = sisteOpsjon.forrigeSluttdato
            if (nySluttDato == null) {
                return FieldError.of("Forrige sluttdato mangler fra opsjonen som skal slettes").left()
            }
            updateAvtaleVarighet(avtaleId, nySluttDato, today)
        }

        queries.opsjoner.delete(opsjonId)

        val avtale = getOrError(avtaleId)
        logEndring("Opsjon slettet", avtale, slettesAv)
        avtale.right()
    }

    fun frikobleKontaktpersonFraAvtale(
        kontaktpersonId: UUID,
        avtaleId: UUID,
        navIdent: NavIdent,
    ): Unit = db.transaction {
        queries.avtale.frikobleKontaktpersonFraAvtale(kontaktpersonId = kontaktpersonId, avtaleId = avtaleId)

        val avtale = getOrError(avtaleId)
        logEndring(
            "Kontaktperson ble fjernet fra avtalen via arrangørsidene",
            avtale,
            navIdent,
        )
    }

    fun getEndringshistorikk(id: UUID): EndringshistorikkDto = db.session {
        queries.endringshistorikk.getEndringshistorikk(DocumentClass.AVTALE, id)
    }

    private fun schedulePublishGjennomforingerForAvtale(dto: AvtaleDto) {
        gjennomforingPublisher.schedule(
            input = InitialLoadGjennomforinger.Input(avtaleId = dto.id),
            id = dto.id,
            startTime = Instant.now().plus(30, ChronoUnit.SECONDS),
        )
    }

    private suspend fun syncArrangorerFromBrreg(
        orgnr: Organisasjonsnummer,
        underenheterOrgnummere: List<Organisasjonsnummer>,
    ): Either<List<FieldError>, Pair<ArrangorDto, List<ArrangorDto>>> = either {
        val arrangor = syncArrangorFromBrreg(orgnr).bind()
        val underenheter = underenheterOrgnummere.mapOrAccumulate({ e1, e2 -> e1 + e2 }) {
            syncArrangorFromBrreg(it).bind()
        }.bind()
        Pair(arrangor, underenheter)
    }

    private suspend fun syncArrangorFromBrreg(
        orgnr: Organisasjonsnummer,
    ): Either<List<FieldError>, ArrangorDto> = arrangorService
        .getArrangorOrSyncFromBrreg(orgnr)
        .mapLeft {
            FieldError.of(
                "Tiltaksarrangøren finnes ikke i Brønnøysundregistrene",
                AvtaleRequest::arrangor,
                AvtaleRequest.Arrangor::hovedenhet,
            ).nel()
        }

    private fun resolveStatus(
        request: AvtaleRequest,
        previous: AvtaleDto?,
        today: LocalDate,
    ): AvtaleStatus = if (request.arrangor == null) {
        AvtaleStatus.UTKAST
    } else if (previous?.status is AvtaleStatusDto.Avbrutt) {
        previous.status.type
    } else if (request.sluttDato == null || !request.sluttDato.isBefore(today)) {
        AvtaleStatus.AKTIV
    } else {
        AvtaleStatus.AVSLUTTET
    }

    private fun QueryContext.updateAvtaleVarighet(avtaleId: UUID, nySluttDato: LocalDate, today: LocalDate) {
        queries.avtale.setSluttDato(avtaleId, nySluttDato)

        val currentStatus = getOrError(avtaleId).status.type
        val newStatus = when (currentStatus) {
            AvtaleStatus.UTKAST, AvtaleStatus.AVBRUTT -> currentStatus
            AvtaleStatus.AKTIV, AvtaleStatus.AVSLUTTET -> if (!nySluttDato.isBefore(today)) {
                AvtaleStatus.AKTIV
            } else {
                AvtaleStatus.AVSLUTTET
            }
        }
        if (newStatus != currentStatus) {
            queries.avtale.setStatus(avtaleId, newStatus, null, null)
        }
    }

    private fun QueryContext.getOrError(id: UUID): AvtaleDto {
        val dto = queries.avtale.get(id)
        return requireNotNull(dto) { "Avtale med id=$id finnes ikke" }
    }

    private fun QueryContext.dispatchNotificationToNewAdministrators(
        dbo: AvtaleDbo,
        navIdent: NavIdent,
    ) {
        val currentAdministratorer = get(dbo.id)?.administratorer?.map { it.navIdent }?.toSet() ?: setOf()

        val administratorsToNotify =
            (dbo.administratorer - currentAdministratorer - navIdent).toNonEmptyListOrNull() ?: return

        val notification = ScheduledNotification(
            title = "Du har blitt satt som administrator på avtalen \"${dbo.navn}\"",
            targets = administratorsToNotify,
            createdAt = Instant.now(),
        )
        queries.notifications.insert(notification)
    }

    private fun QueryContext.logEndring(
        operation: String,
        dto: AvtaleDto,
        endretAv: Agent,
    ) {
        queries.endringshistorikk.logEndring(
            DocumentClass.AVTALE,
            operation,
            endretAv,
            dto.id,
            LocalDateTime.now(),
        ) {
            Json.encodeToJsonElement(dto)
        }
    }
}
