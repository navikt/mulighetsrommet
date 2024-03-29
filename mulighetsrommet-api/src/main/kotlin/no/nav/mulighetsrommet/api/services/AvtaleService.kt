package no.nav.mulighetsrommet.api.services

import arrow.core.*
import arrow.core.raise.either
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotliquery.TransactionalSession
import no.nav.mulighetsrommet.api.avtaler.AvtaleValidator
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.api.domain.dto.ArrangorDto
import no.nav.mulighetsrommet.api.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.api.domain.dto.AvtaleNotificationDto
import no.nav.mulighetsrommet.api.domain.dto.EndringshistorikkDto
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.routes.v1.AvtaleRequest
import no.nav.mulighetsrommet.api.routes.v1.responses.*
import no.nav.mulighetsrommet.api.utils.AvtaleFilter
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering.Opphav
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dto.Avtalestatus
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus
import no.nav.mulighetsrommet.notifications.NotificationRepository
import no.nav.mulighetsrommet.notifications.NotificationType
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import java.time.Instant
import java.time.LocalDate
import java.util.*

class AvtaleService(
    private val avtaler: AvtaleRepository,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
    private val tiltakstyperMigrert: List<Tiltakskode>,
    private val arrangorService: ArrangorService,
    private val notificationRepository: NotificationRepository,
    private val validator: AvtaleValidator,
    private val endringshistorikkService: EndringshistorikkService,
    private val db: Database,
) {
    fun get(id: UUID): AvtaleAdminDto? {
        return avtaler.get(id)
    }

    suspend fun upsert(request: AvtaleRequest, navIdent: NavIdent): Either<List<ValidationError>, AvtaleAdminDto> {
        val previous = avtaler.get(request.id)
        return syncArrangorerFromBrreg(request)
            .flatMap { (arrangor, underenheter) ->
                val dbo = request.run {
                    AvtaleDbo(
                        id = id,
                        navn = navn,
                        avtalenummer = avtalenummer,
                        tiltakstypeId = tiltakstypeId,
                        arrangorId = arrangor.id,
                        arrangorUnderenheter = underenheter.map { it.id },
                        arrangorKontaktpersonId = arrangorKontaktpersonId,
                        startDato = startDato,
                        sluttDato = sluttDato,
                        avtaletype = avtaletype,
                        antallPlasser = null,
                        url = url,
                        administratorer = administratorer,
                        prisbetingelser = prisbetingelser,
                        navEnheter = navEnheter,
                        beskrivelse = beskrivelse,
                        faneinnhold = faneinnhold,
                    )
                }
                validator.validate(dbo, previous)
            }
            .map { dbo ->
                db.transaction { tx ->
                    if (previous?.toDbo() == dbo) {
                        return@transaction previous
                    }

                    avtaler.upsert(dbo, tx)

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

    private suspend fun syncArrangorerFromBrreg(request: AvtaleRequest): Either<List<ValidationError>, Pair<ArrangorDto, List<ArrangorDto>>> =
        either {
            val arrangor = syncArrangorFromBrreg(request.arrangorOrganisasjonsnummer).bind()
            val underenheter = request.arrangorUnderenheter.mapOrAccumulate({ e1, e2 -> e1 + e2 }) {
                syncArrangorFromBrreg(it).bind()
            }.bind()
            Pair(arrangor, underenheter)
        }

    private suspend fun syncArrangorFromBrreg(orgnr: String): Either<List<ValidationError>, ArrangorDto> {
        return arrangorService
            .getOrSyncArrangorFromBrreg(orgnr)
            .mapLeft {
                ValidationError.of(
                    AvtaleRequest::arrangorOrganisasjonsnummer,
                    "Tiltaksarrangøren finnes ikke i Brønnøysundregistrene",
                ).nel()
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
            avtaletyper = filter.avtaletyper,
            navRegioner = filter.navRegioner,
            sortering = filter.sortering,
            dagensDato = filter.dagensDato,
            arrangorIds = filter.arrangorIds,
            administratorNavIdent = filter.administratorNavIdent,
        )

        return PaginatedResponse.of(pagination, totalCount, items)
    }

    fun getAllAvtalerSomNarmerSegSluttdato(): List<AvtaleNotificationDto> {
        return avtaler.getAllAvtalerSomNarmerSegSluttdato()
    }

    fun avbrytAvtale(id: UUID, navIdent: NavIdent, dagensDato: LocalDate = LocalDate.now()): StatusResponse<Unit> {
        val avtale = avtaler.get(id) ?: return Either.Left(NotFound("Avtalen finnes ikke"))

        if (avtale.opphav == Opphav.ARENA && !tiltakstyperMigrert.contains(Tiltakskode.fromArenaKode(avtale.tiltakstype.arenaKode))) {
            return Either.Left(BadRequest(message = "Avtalen har opprinnelse fra Arena og kan ikke bli avbrutt fra admin-flate."))
        }

        if (avtale.avtalestatus != Avtalestatus.Aktiv) {
            return Either.Left(BadRequest(message = "Avtalen er allerede avsluttet og kan derfor ikke avbrytes."))
        }

        val gjennomforinger = tiltaksgjennomforinger.getAll(
            avtaleId = id,
            statuser = listOf(
                Tiltaksgjennomforingsstatus.GJENNOMFORES,
                Tiltaksgjennomforingsstatus.PLANLAGT,
            ),
            dagensDato = dagensDato,
        ).second

        val (antallAktiveGjennomforinger, antallPlanlagteGjennomforinger) = gjennomforinger.partition { it.status == Tiltaksgjennomforingsstatus.GJENNOMFORES }
        if (antallAktiveGjennomforinger.isNotEmpty()) {
            return Either.Left(
                BadRequest(
                    message = "Avtalen har ${antallAktiveGjennomforinger.size} ${
                        if (antallAktiveGjennomforinger.size > 1) "aktive tiltaksgjennomføringer" else "aktiv tiltaksgjennomføring"
                    } koblet til seg. Du må frikoble ${
                        if (antallAktiveGjennomforinger.size > 1) "gjennomføringene" else "gjennomføringen"
                    } før du kan avbryte avtalen.",
                ),
            )
        }

        if (antallPlanlagteGjennomforinger.isNotEmpty()) {
            return Either.Left(
                BadRequest(
                    message = "Avtalen har ${antallPlanlagteGjennomforinger.size} ${
                        if (antallPlanlagteGjennomforinger.size > 1) "planlagte tiltaksgjennomføringer" else "planlagt tiltaksgjennomføring"
                    } koblet til seg. Du må flytte eller avslutte ${
                        if (antallPlanlagteGjennomforinger.size > 1) "gjennomføringene" else "gjennomføringen"
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
        navIdent: NavIdent,
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
        navIdent: NavIdent,
        tx: TransactionalSession,
    ) {
        endringshistorikkService.logEndring(tx, DocumentClass.AVTALE, operation, navIdent.value, dto.id) {
            Json.encodeToJsonElement(dto)
        }
    }
}
