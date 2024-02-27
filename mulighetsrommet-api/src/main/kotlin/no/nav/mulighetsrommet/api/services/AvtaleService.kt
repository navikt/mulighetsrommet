package no.nav.mulighetsrommet.api.services

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.nel
import arrow.core.toNonEmptyListOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotliquery.TransactionalSession
import no.nav.mulighetsrommet.api.avtaler.AvtaleValidator
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.api.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.api.domain.dto.AvtaleNotificationDto
import no.nav.mulighetsrommet.api.domain.dto.EndringshistorikkDto
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.UtkastRepository
import no.nav.mulighetsrommet.api.routes.v1.AvtaleRequest
import no.nav.mulighetsrommet.api.routes.v1.responses.*
import no.nav.mulighetsrommet.api.utils.AvtaleFilter
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering.Opphav
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dto.Avtalestatus
import no.nav.mulighetsrommet.notifications.NotificationRepository
import no.nav.mulighetsrommet.notifications.NotificationType
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import java.time.Instant
import java.util.*

class AvtaleService(
    private val avtaler: AvtaleRepository,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
    private val virksomhetService: VirksomhetService,
    private val notificationRepository: NotificationRepository,
    private val utkastRepository: UtkastRepository,
    private val validator: AvtaleValidator,
    private val endringshistorikkService: EndringshistorikkService,
    private val db: Database,
) {
    fun get(id: UUID): AvtaleAdminDto? {
        return avtaler.get(id)
    }

    suspend fun upsert(request: AvtaleRequest, navIdent: String): Either<List<ValidationError>, AvtaleAdminDto> {
        val previous = avtaler.get(request.id)
        return virksomhetService.getOrSyncHovedenhetFromBrreg(request.leverandorOrganisasjonsnummer)
            .mapLeft {
                ValidationError
                    .of(
                        AvtaleDbo::leverandorOrganisasjonsnummer,
                        "Leverandøren finnes ikke Brønnøysundregistrene",
                    )
                    .nel()
            }
            .flatMap {
                validator.validate(request.toDbo(), previous)
            }
            .map { dbo ->
                db.transaction { tx ->
                    if (previous?.toDbo() == dbo) {
                        return@transaction previous
                    }

                    avtaler.upsert(dbo, tx)
                    utkastRepository.delete(dbo.id, tx)

                    dispatchNotificationToNewAdministrators(tx, dbo, navIdent)

                    val dto = getOrError(dbo.id, tx)

                    val operation = if (previous == null) {
                        "Opprettet avtale"
                    } else {
                        "Redigerte avtale"
                    }
                    logEndring(operation, dto, navIdent, tx)
                    dto
                }
            }
    }

    fun getAll(
        filter: AvtaleFilter,
        pagination: PaginationParams = PaginationParams(),
    ): PaginatedResponse<AvtaleAdminDto> {
        val (totalCount, items) = avtaler.getAll(
            pagination = pagination,
            tiltakstypeIder = filter.tiltakstypeIder,
            search = filter.search,
            statuser = filter.statuser,
            navRegioner = filter.navRegioner,
            sortering = filter.sortering,
            dagensDato = filter.dagensDato,
            leverandorOrgnr = filter.leverandorOrgnr,
            administratorNavIdent = filter.administratorNavIdent,
        )

        return PaginatedResponse.of(pagination, totalCount, items)
    }

    fun getAllAvtalerSomNarmerSegSluttdato(): List<AvtaleNotificationDto> {
        return avtaler.getAllAvtalerSomNarmerSegSluttdato()
    }

    fun avbrytAvtale(id: UUID, navIdent: String): StatusResponse<Unit> {
        val avtale = avtaler.get(id) ?: return Either.Left(NotFound("Avtalen finnes ikke"))

        if (avtale.opphav == Opphav.ARENA) {
            return Either.Left(BadRequest(message = "Avtalen har opprinnelse fra Arena og kan ikke bli avbrutt fra admin-flate."))
        }

        if (avtale.avtalestatus != Avtalestatus.Aktiv) {
            return Either.Left(BadRequest(message = "Avtalen er allerede avsluttet og kan derfor ikke avbrytes."))
        }

        val (antallGjennomforinger) = tiltaksgjennomforinger.getAll(avtaleId = id)
        if (antallGjennomforinger > 0) {
            return Either.Left(
                BadRequest(
                    message = "Avtalen har $antallGjennomforinger ${
                        if (antallGjennomforinger > 1) "tiltaksgjennomføringer" else "tiltaksgjennomføring"
                    } koblet til seg. Du må frikoble ${
                        if (antallGjennomforinger > 1) "gjennomføringene" else "gjennomføringen"
                    } før du kan avbryte avtalen.",
                ),
            )
        }

        db.transaction { tx ->
            avtaler.setAvslutningsstatus(tx, id, Avslutningsstatus.AVBRUTT)
            val dto = getOrError(id, tx)
            logEndring("Avtale ble avbrutt", dto, navIdent, tx)
        }

        return Either.Right(Unit)
    }

    fun getEndringshistorikk(id: UUID): EndringshistorikkDto {
        return endringshistorikkService.getEndringshistorikk(DocumentClass.AVTALE, id)
    }

    private fun getOrError(id: UUID, tx: TransactionalSession): AvtaleAdminDto {
        val dto = avtaler.get(id, tx)
        return requireNotNull(dto) { "Avtale med id=$id finnes ikke" }
    }

    private fun dispatchNotificationToNewAdministrators(
        tx: TransactionalSession,
        dbo: AvtaleDbo,
        navIdent: String,
    ) {
        val currentAdministratorer = get(dbo.id)?.administratorer?.map { it.navIdent }?.toSet() ?: setOf()

        val administratorsToNotify =
            (dbo.administratorer - currentAdministratorer - navIdent).toNonEmptyListOrNull() ?: return

        val notification = ScheduledNotification(
            type = NotificationType.NOTIFICATION,
            title = "Du har blitt satt som administrator på avtalen \"${dbo.navn}\"",
            targets = administratorsToNotify,
            createdAt = Instant.now(),
        )
        notificationRepository.insert(notification, tx)
    }

    private fun logEndring(
        operation: String,
        dto: AvtaleAdminDto,
        navIdent: String,
        tx: TransactionalSession,
    ) {
        endringshistorikkService.logEndring(tx, DocumentClass.AVTALE, operation, navIdent, dto.id) {
            Json.encodeToJsonElement(dto)
        }
    }
}
