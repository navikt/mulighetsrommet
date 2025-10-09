package no.nav.mulighetsrommet.api.avtale

import arrow.core.*
import arrow.core.raise.either
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.avtale.api.AvtaleHandling
import no.nav.mulighetsrommet.api.avtale.api.AvtaleRequest
import no.nav.mulighetsrommet.api.avtale.api.OpprettOpsjonLoggRequest
import no.nav.mulighetsrommet.api.avtale.api.PersonvernRequest
import no.nav.mulighetsrommet.api.avtale.api.VeilederinfoRequest
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.mapper.AvtaleDboMapper
import no.nav.mulighetsrommet.api.avtale.mapper.toDbo
import no.nav.mulighetsrommet.api.avtale.model.*
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkDto
import no.nav.mulighetsrommet.api.gjennomforing.task.InitialLoadGjennomforinger
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavIdent
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
    ): Either<List<FieldError>, Avtale> = either {
        val previous = get(request.id)

        val dbo = validator
            .validate(request, previous)
            .bind()

        if (previous != null && AvtaleDboMapper.fromAvtale(previous) == dbo) {
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

    fun upsertPersonvern(
        avtaleId: UUID,
        request: PersonvernRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Avtale> = either {
        val previous = get(avtaleId)
            ?: throw StatusException(HttpStatusCode.NotFound, "Fant ikke avtale")

        val dbo = request.toDbo()

        db.transaction {
            queries.avtale.updatePersonvern(previous.id, dbo)

            val dto = getOrError(previous.id)
            logEndring("Redigerte avtale", dto, navIdent)

            schedulePublishGjennomforingerForAvtale(dto)

            dto
        }
    }

    fun upsertVeilederinfo(
        avtaleId: UUID,
        request: VeilederinfoRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Avtale> = either {
        val previous = get(avtaleId)
            ?: throw StatusException(HttpStatusCode.NotFound, "Fant ikke avtale")

        val navEnheter = validator.validateNavEnheter(request.navEnheter).bind()
        val dbo = request.toDbo(navEnheter)

        db.transaction {
            queries.avtale.updateVeilederinfo(previous.id, dbo)

            val dto = getOrError(previous.id)
            logEndring("Redigerte avtale", dto, navIdent)

            schedulePublishGjennomforingerForAvtale(dto)

            dto
        }
    }

    fun upsertPrismodell(
        id: UUID,
        request: PrismodellRequest,
        navIdent: NavIdent,
    ): Either<NonEmptyList<FieldError>, Avtale> = either {
        val previous = get(id)
            ?: throw StatusException(HttpStatusCode.NotFound, "Fant ikke avtale")

        val dbo = validator
            .validatePrismodell(request, previous.tiltakstype.tiltakskode, previous.tiltakstype.navn)
            .bind()

        db.transaction {
            queries.avtale.upsertPrismodell(id, dbo)

            val dto = getOrError(id)
            logEndring("Prismodell oppdatert", dto, navIdent)

            schedulePublishGjennomforingerForAvtale(dto)

            dto
        }
    }

    fun get(id: UUID): Avtale? = db.session {
        queries.avtale.get(id)
    }

    fun avsluttAvtale(id: UUID, avsluttetTidspunkt: LocalDateTime, endretAv: Agent) = db.transaction {
        val avtale = getOrError(id)

        check(avtale.status == AvtaleStatus.Aktiv) {
            "Avtalen må være aktiv for å kunne avsluttes"
        }

        val tidspunktForSlutt = avtale.sluttDato?.plusDays(1)?.atStartOfDay()
        check(tidspunktForSlutt != null && !avsluttetTidspunkt.isBefore(tidspunktForSlutt)) {
            "Avtalen kan ikke avsluttes før sluttdato"
        }

        queries.avtale.setStatus(id, AvtaleStatusType.AVSLUTTET, null, null, null)

        val dto = getOrError(id)
        logEndring("Avtalen ble avsluttet", dto, endretAv)
    }

    fun avbrytAvtale(
        id: UUID,
        avbruttAv: NavIdent,
        tidspunkt: LocalDateTime,
        aarsakerOgForklaring: AarsakerOgForklaringRequest<AvbrytAvtaleAarsak>,
    ): Either<List<FieldError>, Avtale> = db.transaction {
        val avtale = getOrError(id)

        val errors = buildList {
            when (avtale.status) {
                is AvtaleStatus.Utkast, is AvtaleStatus.Aktiv -> Unit
                is AvtaleStatus.Avbrutt -> add(FieldError.root("Avtalen er allerede avbrutt"))
                is AvtaleStatus.Avsluttet -> add(FieldError.root("Avtalen er allerede avsluttet"))
            }

            val (_, gjennomforinger) = queries.gjennomforing.getAll(
                avtaleId = id,
                statuser = listOf(GjennomforingStatusType.GJENNOMFORES),
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

        queries.avtale.setStatus(
            id = id,
            status = AvtaleStatusType.AVBRUTT,
            tidspunkt = tidspunkt,
            aarsaker = aarsakerOgForklaring.aarsaker,
            forklaring = aarsakerOgForklaring.forklaring,
        )

        val dto = getOrError(id)
        logEndring("Avtalen ble avbrutt", dto, avbruttAv)

        dto.right()
    }

    fun registrerOpsjon(
        avtaleId: UUID,
        request: OpprettOpsjonLoggRequest,
        navIdent: NavIdent,
        today: LocalDate = LocalDate.now(),
    ): Either<List<FieldError>, Avtale> = either {
        db.transaction {
            val avtale = getOrError(avtaleId)
            requireNotNull(avtale.sluttDato) {
                "Sluttdato på avtalen er null"
            }

            val dbo = validator.validateOpprettOpsjonLoggRequest(
                request,
                avtale,
                navIdent,
            ).bind()

            queries.opsjoner.insert(dbo)

            if (dbo.sluttDato != null) {
                updateAvtaleVarighet(avtaleId, dbo.sluttDato, today)
            }

            val operation = when (request.type) {
                OpprettOpsjonLoggRequest.Type.CUSTOM_LENGDE,
                OpprettOpsjonLoggRequest.Type.ETT_AAR,
                -> "Opsjon registrert"

                OpprettOpsjonLoggRequest.Type.SKAL_IKKE_UTLOSE_OPSJON -> "Registrert at opsjon ikke skal utløses for avtalen"
            }
            logEndring(operation, getOrError(avtaleId), navIdent)
        }
    }

    fun slettOpsjon(
        avtaleId: UUID,
        opsjonId: UUID,
        slettesAv: NavIdent,
        today: LocalDate = LocalDate.now(),
    ): Either<FieldError, Avtale> = db.transaction {
        val opsjoner = queries.opsjoner.getByAvtaleId(avtaleId)

        val sisteOpsjon = opsjoner.firstOrNull()
        if (sisteOpsjon == null || sisteOpsjon.id != opsjonId) {
            return FieldError.of("Opsjonen kan ikke slettes fordi det ikke er den siste utløste opsjonen").left()
        }

        if (sisteOpsjon.status == OpsjonLoggStatus.OPSJON_UTLOST) {
            val nySluttDato = sisteOpsjon.forrigeSluttDato
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

    private fun schedulePublishGjennomforingerForAvtale(dto: Avtale) {
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
            AvtaleStatusType.UTKAST, AvtaleStatusType.AVBRUTT -> currentStatus
            AvtaleStatusType.AKTIV, AvtaleStatusType.AVSLUTTET -> if (!nySluttDato.isBefore(today)) {
                AvtaleStatusType.AKTIV
            } else {
                AvtaleStatusType.AVSLUTTET
            }
        }
        if (newStatus != currentStatus) {
            queries.avtale.setStatus(avtaleId, newStatus, null, null, null)
        }
    }

    private fun QueryContext.getOrError(id: UUID): Avtale {
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
        dto: Avtale,
        endretAv: Agent,
    ): Avtale {
        queries.endringshistorikk.logEndring(
            DocumentClass.AVTALE,
            operation,
            endretAv,
            dto.id,
            LocalDateTime.now(),
        ) {
            Json.encodeToJsonElement(dto)
        }
        return dto
    }

    fun handlinger(avtale: Avtale, ansatt: NavAnsatt): Set<AvtaleHandling> {
        return setOfNotNull(
            AvtaleHandling.AVBRYT.takeIf {
                when (avtale.status) {
                    AvtaleStatus.Utkast,
                    AvtaleStatus.Aktiv,
                    -> true

                    is AvtaleStatus.Avbrutt,
                    AvtaleStatus.Avsluttet,
                    -> false
                }
            },
            AvtaleHandling.OPPRETT_GJENNOMFORING.takeIf {
                when (avtale.status) {
                    AvtaleStatus.Aktiv -> true
                    is AvtaleStatus.Avbrutt,
                    AvtaleStatus.Avsluttet,
                    AvtaleStatus.Utkast,
                    -> false
                }
            },
            AvtaleHandling.OPPDATER_PRIS.takeIf {
                avtale.prismodell.type !== PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK
            },
            AvtaleHandling.REGISTRER_OPSJON.takeIf {
                avtale.opsjonsmodell.opsjonMaksVarighet != null
            },
            AvtaleHandling.DUPLISER,
            AvtaleHandling.REDIGER,
        )
            .filter {
                tilgangTilHandling(it, ansatt)
            }
            .toSet()
    }

    companion object {
        fun tilgangTilHandling(handling: AvtaleHandling, ansatt: NavAnsatt): Boolean {
            return when (handling) {
                AvtaleHandling.OPPRETT_GJENNOMFORING -> ansatt.hasGenerellRolle(Rolle.TILTAKSGJENNOMFORINGER_SKRIV)
                AvtaleHandling.AVBRYT,
                AvtaleHandling.OPPDATER_PRIS,
                AvtaleHandling.REGISTRER_OPSJON,
                AvtaleHandling.DUPLISER,
                AvtaleHandling.REDIGER,
                -> ansatt.hasGenerellRolle(Rolle.AVTALER_SKRIV)
            }
        }
    }
}

fun resolveStatus(
    request: AvtaleRequest,
    previous: Avtale?,
    today: LocalDate,
): AvtaleStatusType = if (request.arrangor == null) {
    AvtaleStatusType.UTKAST
} else if (previous?.status?.type == AvtaleStatusType.AVBRUTT) {
    previous.status.type
} else if (request.sluttDato == null || !request.sluttDato.isBefore(today)) {
    AvtaleStatusType.AKTIV
} else {
    AvtaleStatusType.AVSLUTTET
}
