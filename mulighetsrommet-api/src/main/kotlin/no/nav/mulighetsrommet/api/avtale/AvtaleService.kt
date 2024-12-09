package no.nav.mulighetsrommet.api.avtale

import arrow.core.*
import arrow.core.raise.either
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotliquery.TransactionalSession
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.db.AvtaleRepository
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.domain.dto.EndringshistorikkDto
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.gjennomforing.task.InitialLoadTiltaksgjennomforinger
import no.nav.mulighetsrommet.api.responses.*
import no.nav.mulighetsrommet.api.services.DocumentClass
import no.nav.mulighetsrommet.api.services.EndretAv
import no.nav.mulighetsrommet.api.services.EndringshistorikkService
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.domain.dto.*
import no.nav.mulighetsrommet.notifications.NotificationRepository
import no.nav.mulighetsrommet.notifications.NotificationType
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class AvtaleService(
    private val avtaler: AvtaleRepository,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
    private val arrangorService: ArrangorService,
    private val notificationRepository: NotificationRepository,
    private val validator: AvtaleValidator,
    private val endringshistorikkService: EndringshistorikkService,
    private val db: Database,
    private val gjennomforingPublisher: InitialLoadTiltaksgjennomforinger,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun get(id: UUID): AvtaleDto? = avtaler.get(id)

    suspend fun upsert(request: AvtaleRequest, navIdent: NavIdent): Either<List<ValidationError>, AvtaleDto> {
        val previous = avtaler.get(request.id)
        return syncArrangorerFromBrreg(request)
            .flatMap { (arrangor, underenheter) ->
                val dbo = request.run {
                    AvtaleDbo(
                        id = id,
                        navn = navn,
                        avtalenummer = avtalenummer,
                        websaknummer = websaknummer,
                        tiltakstypeId = tiltakstypeId,
                        arrangorId = arrangor.id,
                        arrangorUnderenheter = underenheter.map { it.id },
                        arrangorKontaktpersoner = arrangorKontaktpersoner,
                        startDato = startDato,
                        sluttDato = sluttDato,
                        opsjonMaksVarighet = opsjonsmodellData?.opsjonMaksVarighet,
                        avtaletype = avtaletype,
                        antallPlasser = null,
                        administratorer = administratorer,
                        prisbetingelser = prisbetingelser,
                        navEnheter = navEnheter,
                        beskrivelse = beskrivelse,
                        faneinnhold = faneinnhold,
                        personopplysninger = personopplysninger,
                        personvernBekreftet = personvernBekreftet,
                        amoKategorisering = amoKategorisering,
                        opsjonsmodell = opsjonsmodellData?.opsjonsmodell,
                        customOpsjonsmodellNavn = opsjonsmodellData?.customOpsjonsmodellNavn,
                        utdanningslop = utdanningslop,
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
                    logEndring(operation, dto, EndretAv.NavAnsatt(navIdent), tx)

                    schedulePublishGjennomforingerForAvtale(dto)

                    dto
                }
            }
    }

    private fun schedulePublishGjennomforingerForAvtale(dto: AvtaleDto) {
        gjennomforingPublisher.schedule(
            input = InitialLoadTiltaksgjennomforinger.Input(avtaleId = dto.id),
            id = dto.id,
            startTime = Instant.now().plus(5, ChronoUnit.MINUTES),
        )
    }

    private suspend fun syncArrangorerFromBrreg(request: AvtaleRequest): Either<List<ValidationError>, Pair<ArrangorDto, List<ArrangorDto>>> = either {
        val arrangor = syncArrangorFromBrreg(request.arrangorOrganisasjonsnummer).bind()
        val underenheter = request.arrangorUnderenheter.mapOrAccumulate({ e1, e2 -> e1 + e2 }) {
            syncArrangorFromBrreg(it).bind()
        }.bind()
        Pair(arrangor, underenheter)
    }

    private suspend fun syncArrangorFromBrreg(orgnr: Organisasjonsnummer): Either<List<ValidationError>, ArrangorDto> = arrangorService
        .getOrSyncArrangorFromBrreg(orgnr)
        .mapLeft {
            ValidationError.of(
                AvtaleRequest::arrangorOrganisasjonsnummer,
                "Tiltaksarrangøren finnes ikke i Brønnøysundregistrene",
            ).nel()
        }

    fun getAll(
        filter: AvtaleFilter,
        pagination: Pagination,
    ): PaginatedResponse<AvtaleDto> {
        val (totalCount, items) = avtaler.getAll(
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

        return PaginatedResponse.of(pagination, totalCount, items)
    }

    fun avbrytAvtale(id: UUID, navIdent: NavIdent, aarsak: AvbruttAarsak?): StatusResponse<Unit> {
        if (aarsak == null) {
            return Either.Left(BadRequest(message = "Årsak mangler"))
        }
        val avtale = avtaler.get(id) ?: return Either.Left(NotFound("Avtalen finnes ikke"))

        if (aarsak is AvbruttAarsak.Annet && aarsak.name.length > 100) {
            return Either.Left(BadRequest(message = "Beskrivelse kan ikke inneholde mer enn 100 tegn"))
        }

        if (aarsak is AvbruttAarsak.Annet && aarsak.name.isEmpty()) {
            return Either.Left(BadRequest(message = "Beskrivelse er obligatorisk når “Annet” er valgt som årsak"))
        }

        if (avtale.status != AvtaleStatus.AKTIV) {
            return Either.Left(BadRequest(message = "Avtalen er allerede avsluttet og kan derfor ikke avbrytes."))
        }

        val (_, gjennomforinger) = tiltaksgjennomforinger.getAll(
            avtaleId = id,
            statuser = listOf(TiltaksgjennomforingStatus.GJENNOMFORES),
        )

        if (gjennomforinger.isNotEmpty()) {
            val plural = gjennomforinger.size > 1
            val message = listOf(
                "Avtalen har",
                gjennomforinger.size,
                if (plural) "aktive gjennomføringer" else "aktiv gjennomføring",
                "og kan derfor ikke avbrytes.",
            ).joinToString(" ")
            return Either.Left(BadRequest(message))
        }

        db.transaction { tx ->
            avtaler.avbryt(tx, id, LocalDateTime.now(), aarsak)
            val dto = getOrError(id, tx)
            logEndring("Avtale ble avbrutt", dto, EndretAv.NavAnsatt(navIdent), tx)
        }

        return Either.Right(Unit)
    }

    fun getEndringshistorikk(id: UUID): EndringshistorikkDto = endringshistorikkService.getEndringshistorikk(DocumentClass.AVTALE, id)

    private fun getOrError(id: UUID, tx: TransactionalSession): AvtaleDto {
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
        dto: AvtaleDto,
        endretAv: EndretAv.NavAnsatt,
        tx: TransactionalSession,
    ) {
        endringshistorikkService.logEndring(
            tx,
            DocumentClass.AVTALE,
            operation,
            endretAv,
            dto.id,
        ) {
            Json.encodeToJsonElement(dto)
        }
    }

    fun frikobleKontaktpersonFraAvtale(
        kontaktpersonId: UUID,
        avtaleId: UUID,
        navIdent: NavIdent,
    ): Either<StatusResponseError, String> {
        val avtale = avtaler.get(avtaleId) ?: return Either.Left(NotFound("Avtalen finnes ikke"))
        return db.transaction { tx ->
            avtaler.frikobleKontaktpersonFraAvtale(kontaktpersonId = kontaktpersonId, avtaleId = avtaleId, tx = tx)
                .map {
                    logEndring(
                        "Kontaktperson ble fjernet fra avtalen via arrangørsidene",
                        avtale,
                        EndretAv.NavAnsatt(navIdent),
                        tx,
                    )
                    it
                }
                .mapLeft {
                    logger.error("Klarte ikke fjerne kontaktperson fra avtale: KontaktpersonId = '$kontaktpersonId', avtaleId = '$avtaleId'")
                    ServerError("Klarte ikke fjerne kontaktperson fra avtalen")
                }
        }
    }
}
