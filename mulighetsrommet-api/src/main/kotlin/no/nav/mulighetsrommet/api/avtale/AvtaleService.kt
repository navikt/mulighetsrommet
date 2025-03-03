package no.nav.mulighetsrommet.api.avtale

import arrow.core.Either
import arrow.core.mapOrAccumulate
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.toNonEmptyListOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.avtale.model.toDbo
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkDto
import no.nav.mulighetsrommet.api.gjennomforing.task.InitialLoadGjennomforinger
import no.nav.mulighetsrommet.api.responses.*
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.model.*
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
    private val gjennomforingPublisher: InitialLoadGjennomforinger,
) {
    suspend fun upsert(
        request: AvtaleRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, AvtaleDto> = either {
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
        val previous = get(request.id)
        val dbo = validator.validate(toAvtaleDbo(request, arrangor), previous).bind()

        if (previous?.toDbo() == dbo) {
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

    fun avbrytAvtale(id: UUID, navIdent: NavIdent, aarsak: AvbruttAarsak?): StatusResponse<Unit> = db.transaction {
        if (aarsak == null) {
            return Either.Left(BadRequest(detail = "Årsak mangler"))
        }
        val avtale = queries.avtale.get(id) ?: return Either.Left(NotFound("Avtalen finnes ikke"))

        if (aarsak is AvbruttAarsak.Annet && aarsak.name.length > 100) {
            return Either.Left(BadRequest(detail = "Beskrivelse kan ikke inneholde mer enn 100 tegn"))
        }

        if (aarsak is AvbruttAarsak.Annet && aarsak.name.isEmpty()) {
            return Either.Left(BadRequest(detail = "Beskrivelse er obligatorisk når “Annet” er valgt som årsak"))
        }

        if (avtale.status != AvtaleStatus.AKTIV) {
            return Either.Left(BadRequest(detail = "Avtalen er allerede avsluttet og kan derfor ikke avbrytes."))
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
                "og kan derfor ikke avbrytes.",
            ).joinToString(" ")

            return Either.Left(BadRequest(message))
        }

        queries.avtale.avbryt(id, LocalDateTime.now(), aarsak)
        val dto = getOrError(id)
        logEndring("Avtale ble avbrutt", dto, navIdent)

        Either.Right(Unit)
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
            startTime = Instant.now().plus(5, ChronoUnit.MINUTES),
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
            type = NotificationType.NOTIFICATION,
            title = "Du har blitt satt som administrator på avtalen \"${dbo.navn}\"",
            targets = administratorsToNotify,
            createdAt = Instant.now(),
        )
        queries.notifications.insert(notification)
    }

    private fun QueryContext.logEndring(
        operation: String,
        dto: AvtaleDto,
        endretAv: NavIdent,
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

private fun toAvtaleDbo(
    request: AvtaleRequest,
    arrangor: AvtaleDbo.Arrangor?,
): AvtaleDbo = request.run {
    AvtaleDbo(
        id = id,
        navn = navn,
        avtalenummer = avtalenummer,
        websaknummer = websaknummer,
        tiltakstypeId = tiltakstypeId,
        arrangor = arrangor,
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
