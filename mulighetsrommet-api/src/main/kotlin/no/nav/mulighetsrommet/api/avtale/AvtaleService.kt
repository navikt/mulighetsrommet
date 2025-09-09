package no.nav.mulighetsrommet.api.avtale

import arrow.core.*
import arrow.core.raise.either
import io.ktor.http.*
import io.ktor.server.plugins.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.MrExceptions
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.mapper.AvtaleDboMapper
import no.nav.mulighetsrommet.api.avtale.model.*
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkDto
import no.nav.mulighetsrommet.api.gjennomforing.task.InitialLoadGjennomforinger
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.responses.PaginatedResponse
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class AvtaleService(
    private val db: ApiDatabase,
    private val validator: AvtaleValidator,
    private val gjennomforingPublisher: InitialLoadGjennomforinger,
) {
    suspend fun upsert(
        request: AvtaleRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, AvtaleDto> = either {
        val previous = get(request.id)

        val dbo = validator
            .validate(request, previous)
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

    fun upsertPrismodell(
        id: UUID,
        request: PrismodellRequest,
        navIdent: NavIdent,
    ): Either<NonEmptyList<FieldError>, AvtaleDto> = either {
        val previous = get(id)
            ?: throw StatusException(HttpStatusCode.NotFound, "Fant ikke avtale")

        validator
            .validatePrismodell(request, previous.tiltakstype.tiltakskode, previous.tiltakstype.navn)
            .bind()

        db.transaction {
            queries.avtale.upsertPrismodell(
                id,
                request.type,
                request.prisbetingelser,
                request.satser.map {
                    AvtaltSats(
                        gjelderFra = it.gjelderFra,
                        sats = it.pris,
                    )
                },
            )

            val dto = getOrError(id)
            logEndring("Prismodell oppdatert", dto, navIdent)

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

    fun avsluttAvtale(id: UUID, avsluttetTidspunkt: LocalDateTime, endretAv: Agent) = db.transaction {
        val avtale = getOrError(id)

        check(avtale.status == AvtaleStatusDto.Aktiv) {
            "Avtalen må være aktiv for å kunne avsluttes"
        }

        val tidspunktForSlutt = avtale.sluttDato?.plusDays(1)?.atStartOfDay()
        check(tidspunktForSlutt != null && !avsluttetTidspunkt.isBefore(tidspunktForSlutt)) {
            "Avtalen kan ikke avsluttes før sluttdato"
        }

        queries.avtale.setStatus(id, AvtaleStatus.AVSLUTTET, null, null)

        val dto = getOrError(id)
        logEndring("Avtalen ble avsluttet", dto, endretAv)
    }

    fun avbrytAvtale(
        id: UUID,
        avbruttAv: NavIdent,
        tidspunkt: LocalDateTime,
        aarsakerOgForklaring: AarsakerOgForklaringRequest<AvbruttAarsak>,
    ): Either<List<FieldError>, AvtaleDto> = db.transaction {
        val avtale = getOrError(id)

        val errors = buildList {
            when (avtale.status) {
                is AvtaleStatusDto.Utkast, is AvtaleStatusDto.Aktiv -> Unit
                is AvtaleStatusDto.Avbrutt -> add(FieldError.root("Avtalen er allerede avbrutt"))
                is AvtaleStatusDto.Avsluttet -> add(FieldError.root("Avtalen er allerede avsluttet"))
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
                add(FieldError.root(message))
            }
        }
        if (errors.isNotEmpty()) {
            return errors.left()
        }

        queries.avtale.setStatus(id, AvtaleStatus.AVBRUTT, tidspunkt, aarsakerOgForklaring)

        val dto = getOrError(id)
        logEndring("Avtalen ble avbrutt", dto, avbruttAv)

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
                ?: return FieldError.of("Forrige sluttdato mangler fra opsjonen som skal slettes").left()
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

    fun handlinger(avtale: AvtaleDto, navIdent: NavIdent): Set<AvtaleHandling> {
        val ansatt = db.session { queries.ansatt.getByNavIdent(navIdent) }
            ?: throw MrExceptions.navAnsattNotFound(navIdent)

        val avtalerSkriv = ansatt.hasGenerellRolle(Rolle.AVTALER_SKRIV)

        return setOfNotNull(
            AvtaleHandling.AVBRYT.takeIf {
                when (avtale.status) {
                    AvtaleStatusDto.Utkast,
                    AvtaleStatusDto.Aktiv,
                    -> avtalerSkriv
                    is AvtaleStatusDto.Avbrutt,
                    AvtaleStatusDto.Avsluttet,
                    -> false
                }
            },
            AvtaleHandling.OPPRETT_GJENNOMFORING.takeIf {
                when (avtale.status) {
                    AvtaleStatusDto.Aktiv -> ansatt.hasGenerellRolle(Rolle.TILTAKSGJENNOMFORINGER_SKRIV)
                    is AvtaleStatusDto.Avbrutt,
                    AvtaleStatusDto.Avsluttet,
                    AvtaleStatusDto.Utkast,
                    -> false
                }
            },
            AvtaleHandling.OPPDATER_PRIS.takeIf {
                Prismodeller.getPrismodellerForTiltak(avtale.tiltakstype.tiltakskode).size > 1 && avtalerSkriv
            },
            AvtaleHandling.REGISTRER_OPSJON.takeIf {
                avtale.opsjonsmodell.opsjonMaksVarighet != null && avtalerSkriv
            },
            AvtaleHandling.DUPLISER.takeIf {
                avtalerSkriv
            },
            AvtaleHandling.REDIGER.takeIf {
                avtalerSkriv
            },
        )
    }

    private fun schedulePublishGjennomforingerForAvtale(dto: AvtaleDto) {
        gjennomforingPublisher.schedule(
            input = InitialLoadGjennomforinger.Input(avtaleId = dto.id),
            id = dto.id,
            startTime = Instant.now().plus(30, ChronoUnit.SECONDS),
        )
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

fun resolveStatus(
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
