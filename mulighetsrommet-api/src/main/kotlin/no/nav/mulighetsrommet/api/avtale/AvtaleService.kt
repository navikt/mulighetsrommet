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
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.domain.dto.EndringshistorikkDto
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.endringshistorikk.EndretAv
import no.nav.mulighetsrommet.api.gjennomforing.task.InitialLoadTiltaksgjennomforinger
import no.nav.mulighetsrommet.api.responses.*
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.domain.dto.*
import no.nav.mulighetsrommet.notifications.NotificationType
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class AvtaleService(
    private val db: ApiDatabase,
    private val arrangorService: ArrangorService,
    private val validator: AvtaleValidator,
    private val gjennomforingPublisher: InitialLoadTiltaksgjennomforinger,
) {
    fun get(id: UUID): AvtaleDto? = db.session {
        Queries.avtale.get(id)
    }

    suspend fun upsert(request: AvtaleRequest, navIdent: NavIdent): Either<List<ValidationError>, AvtaleDto> = db.tx {
        val previous = Queries.avtale.get(request.id)
        syncArrangorerFromBrreg(request)
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
                        prismodell = prismodell,
                    )
                }
                validator.validate(dbo, previous)
            }
            .map { dbo ->
                if (previous?.toDbo() == dbo) {
                    return@map previous
                }

                Queries.avtale.upsert(dbo)

                dispatchNotificationToNewAdministrators(dbo, navIdent)

                val dto = getOrError(dbo.id)

                val operation = if (previous == null) {
                    "Opprettet avtale"
                } else {
                    "Redigerte avtale"
                }
                logEndring(operation, dto, EndretAv.NavAnsatt(navIdent))

                schedulePublishGjennomforingerForAvtale(dto)

                dto
            }
    }

    fun getAll(
        filter: AvtaleFilter,
        pagination: Pagination,
    ): PaginatedResponse<AvtaleDto> = db.session {
        val (totalCount, items) = Queries.avtale.getAll(
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

    fun avbrytAvtale(id: UUID, navIdent: NavIdent, aarsak: AvbruttAarsak?): StatusResponse<Unit> = db.tx {
        if (aarsak == null) {
            return@tx Either.Left(BadRequest(message = "Årsak mangler"))
        }
        val avtale = Queries.avtale.get(id) ?: return@tx Either.Left(NotFound("Avtalen finnes ikke"))

        if (aarsak is AvbruttAarsak.Annet && aarsak.name.length > 100) {
            return@tx Either.Left(BadRequest(message = "Beskrivelse kan ikke inneholde mer enn 100 tegn"))
        }

        if (aarsak is AvbruttAarsak.Annet && aarsak.name.isEmpty()) {
            return@tx Either.Left(BadRequest(message = "Beskrivelse er obligatorisk når “Annet” er valgt som årsak"))
        }

        if (avtale.status != AvtaleStatus.AKTIV) {
            return@tx Either.Left(BadRequest(message = "Avtalen er allerede avsluttet og kan derfor ikke avbrytes."))
        }

        val (_, gjennomforinger) = Queries.gjennomforing.getAll(
            avtaleId = id,
            statuser = listOf(TiltaksgjennomforingStatus.GJENNOMFORES),
        )

        if (gjennomforinger.isNotEmpty()) {
            val message = listOf(
                "Avtalen har",
                gjennomforinger.size,
                if (gjennomforinger.size > 1) "aktive gjennomføringer" else "aktiv gjennomføring",
                "og kan derfor ikke avbrytes.",
            ).joinToString(" ")

            return@tx Either.Left(BadRequest(message))
        }

        Queries.avtale.avbryt(id, LocalDateTime.now(), aarsak)
        val dto = getOrError(id)
        logEndring("Avtale ble avbrutt", dto, EndretAv.NavAnsatt(navIdent))

        Either.Right(Unit)
    }

    fun frikobleKontaktpersonFraAvtale(
        kontaktpersonId: UUID,
        avtaleId: UUID,
        navIdent: NavIdent,
    ): Unit = db.tx {
        Queries.avtale.frikobleKontaktpersonFraAvtale(kontaktpersonId = kontaktpersonId, avtaleId = avtaleId)

        val avtale = getOrError(avtaleId)
        logEndring(
            "Kontaktperson ble fjernet fra avtalen via arrangørsidene",
            avtale,
            EndretAv.NavAnsatt(navIdent),
        )
    }

    fun getEndringshistorikk(id: UUID): EndringshistorikkDto = db.session {
        Queries.endringshistorikk.getEndringshistorikk(DocumentClass.AVTALE, id)
    }

    private fun schedulePublishGjennomforingerForAvtale(dto: AvtaleDto) {
        gjennomforingPublisher.schedule(
            input = InitialLoadTiltaksgjennomforinger.Input(avtaleId = dto.id),
            id = dto.id,
            startTime = Instant.now().plus(5, ChronoUnit.MINUTES),
        )
    }

    private suspend fun QueryContext.syncArrangorerFromBrreg(
        request: AvtaleRequest,
    ): Either<List<ValidationError>, Pair<ArrangorDto, List<ArrangorDto>>> = either {
        val arrangor = syncArrangorFromBrreg(request.arrangorOrganisasjonsnummer).bind()
        val underenheter = request.arrangorUnderenheter.mapOrAccumulate({ e1, e2 -> e1 + e2 }) {
            syncArrangorFromBrreg(it).bind()
        }.bind()
        Pair(arrangor, underenheter)
    }

    private suspend fun syncArrangorFromBrreg(
        orgnr: Organisasjonsnummer,
    ): Either<List<ValidationError>, ArrangorDto> = arrangorService
        .getOrSyncArrangorFromBrreg(orgnr)
        .mapLeft {
            ValidationError.of(
                AvtaleRequest::arrangorOrganisasjonsnummer,
                "Tiltaksarrangøren finnes ikke i Brønnøysundregistrene",
            ).nel()
        }

    private fun QueryContext.getOrError(id: UUID): AvtaleDto {
        val dto = Queries.avtale.get(id)
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
            type = NotificationType.NOTIFICATION,
            title = "Du har blitt satt som administrator på avtalen \"${dbo.navn}\"",
            targets = administratorsToNotify,
            createdAt = Instant.now(),
        )
        Queries.notifications.insert(notification)
    }

    private fun QueryContext.logEndring(
        operation: String,
        dto: AvtaleDto,
        endretAv: EndretAv.NavAnsatt,
    ) {
        Queries.endringshistorikk.logEndring(
            DocumentClass.AVTALE,
            operation,
            endretAv,
            dto.id,
        ) {
            Json.encodeToJsonElement(dto)
        }
    }
}
