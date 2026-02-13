package no.nav.mulighetsrommet.api.gjennomforing.service

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkDto
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingRequest
import no.nav.mulighetsrommet.api.gjennomforing.api.SetStengtHosArrangorRequest
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingAvtaleDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingKontaktpersonDbo
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingV2Mapper
import no.nav.mulighetsrommet.api.gjennomforing.model.AvbrytGjennomforingAarsak
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingStatus
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.api.navenhet.NavEnhetHelpers
import no.nav.mulighetsrommet.api.navenhet.toDto
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.database.utils.IntegrityConstraintViolation
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class GjennomforingAvtaleService(
    private val config: Config,
    private val db: ApiDatabase,
    private val navAnsattService: NavAnsattService,
) {
    data class Config(
        val gjennomforingV2Topic: String,
    )

    suspend fun upsert(
        request: GjennomforingRequest,
        navIdent: NavIdent,
        today: LocalDate = LocalDate.now(),
    ): Either<List<FieldError>, GjennomforingAvtale> = either {
        val previous = getGruppetiltak(request.id)
        val ctx = getValidatorCtx(request, previous, today)

        val dbo = GjennomforingValidator
            .validate(
                request.copy(
                    veilederinformasjon = request.veilederinformasjon.copy(
                        navKontorer = sanitizeNavEnheter(
                            request.veilederinformasjon.navRegioner,
                            request.veilederinformasjon.navKontorer,
                        ),
                    ),
                ),
                ctx,
            )
            .onRight { dbo ->
                dbo.kontaktpersoner.forEach {
                    navAnsattService.addUserToKontaktpersoner(it.navIdent)
                }
            }
            .bind()

        if (previous != null && isEqual(previous, dbo)) {
            return@either previous
        }

        db.transaction {
            queries.gjennomforing.upsertGjennomforingAvtale(dbo)

            dispatchNotificationToNewAdministrators(dbo, navIdent)

            val operation = if (ctx.previous == null) {
                "Opprettet gjennomføring"
            } else {
                "Redigerte gjennomføring"
            }
            val dto = logEndring(operation, dbo.id, navIdent)

            queries.gjennomforing.setFreeTextSearch(dbo.id, listOf(dbo.navn, dto.arrangor.navn))

            publishToKafka(dto)
            dto
        }
    }

    fun getValidatorCtx(
        request: GjennomforingRequest,
        previous: GjennomforingAvtale?,
        today: LocalDate,
    ): GjennomforingValidator.Ctx = db.session {
        val avtale = queries.avtale.getOrError(request.avtaleId)
        val kontaktpersoner = request.kontaktpersoner.mapNotNull { queries.ansatt.getByNavIdent(it.navIdent) }
        val administratorer = request.administratorer.mapNotNull { queries.ansatt.getByNavIdent(it) }
        val arrangor = request.arrangorId?.let { queries.arrangor.getById(it) }
        val antallDeltakere = queries.deltaker.getByGjennomforingId(request.id).size
        val status = resolveStatus(previous?.status?.type, request, today)
        return GjennomforingValidator.Ctx(
            previous = previous?.let {
                GjennomforingValidator.Ctx.Gjennomforing(
                    avtaleId = it.avtaleId,
                    oppstart = it.oppstart,
                    arrangorId = it.arrangor.id,
                    status = it.status.type,
                    sluttDato = it.sluttDato,
                    pameldingType = it.pameldingType,
                )
            },
            avtale = avtale,
            arrangor = arrangor,
            kontaktpersoner = kontaktpersoner,
            administratorer = administratorer,
            antallDeltakere = antallDeltakere,
            status = status,
        )
    }

    fun getGruppetiltak(id: UUID): GjennomforingAvtale? = db.session {
        queries.gjennomforing.getGjennomforingAvtale(id)
    }

    fun setPublisert(id: UUID, publisert: Boolean, navIdent: NavIdent): Unit = db.transaction {
        queries.gjennomforing.setPublisert(id, publisert)
        val operation = if (publisert) {
            "Tiltak publisert"
        } else {
            "Tiltak avpublisert"
        }
        logEndring(operation, id, navIdent)
    }

    fun setTilgjengeligForArrangorDato(
        id: UUID,
        tilgjengeligForArrangorDato: LocalDate?,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Unit> = db.transaction {
        val gjennomforing = getOrError(id)

        GjennomforingValidator
            .validateTilgjengeligForArrangorDato(
                tilgjengeligForArrangorDato,
                gjennomforing.startDato,
            )
            .map {
                queries.gjennomforing.setTilgjengeligForArrangorDato(id, it)
                val operation = "Endret dato for tilgang til Deltakeroversikten"
                val dto = logEndring(operation, id, navIdent)
                publishToKafka(dto)
            }
    }

    fun avsluttGjennomforing(
        id: UUID,
        avsluttetTidspunkt: LocalDateTime,
        endretAv: Agent,
    ): GjennomforingAvtale = db.transaction {
        val gjennomforing = getOrError(id)

        check(gjennomforing.status is GjennomforingStatus.Gjennomfores) {
            "Gjennomføringen må være aktiv for å kunne avsluttes"
        }

        val tidspunktForSlutt = gjennomforing.sluttDato?.plusDays(1)?.atStartOfDay()
        check(tidspunktForSlutt != null && !avsluttetTidspunkt.isBefore(tidspunktForSlutt)) {
            "Gjennomføringen kan ikke avsluttes før sluttdato"
        }

        queries.gjennomforing.setStatus(
            id = id,
            status = GjennomforingStatusType.AVSLUTTET,
            sluttDato = null,
            aarsaker = null,
            forklaring = null,
        )
        queries.gjennomforing.setPublisert(id, false)
        queries.gjennomforing.setApentForPamelding(id, false)

        val dto = logEndring("Gjennomføringen ble avsluttet", id, endretAv)
        publishToKafka(dto)
        dto
    }

    fun avbrytGjennomforing(
        id: UUID,
        avbruttAv: Agent,
        sluttDato: LocalDate,
        aarsakerOgForklaring: AarsakerOgForklaringRequest<AvbrytGjennomforingAarsak>,
    ): Either<List<FieldError>, GjennomforingAvtale> = db.transaction {
        val gjennomforing = getOrError(id)

        when (gjennomforing.status) {
            is GjennomforingStatus.Gjennomfores -> Unit

            is GjennomforingStatus.Avlyst, is GjennomforingStatus.Avbrutt ->
                return FieldError.root("Gjennomføringen er allerede avbrutt").nel().left()

            is GjennomforingStatus.Avsluttet ->
                return FieldError.root("Gjennomføringen er allerede avsluttet").nel().left()
        }

        val (status, nySluttDato) = if (sluttDato.isBefore(gjennomforing.startDato)) {
            GjennomforingStatusType.AVLYST to null
        } else if (gjennomforing.sluttDato == null || sluttDato.isBefore(gjennomforing.sluttDato)) {
            GjennomforingStatusType.AVBRUTT to sluttDato
        } else {
            throw Exception("Gjennomføring allerede avsluttet")
        }

        queries.gjennomforing.setStatus(
            id = id,
            status = status,
            sluttDato = nySluttDato,
            aarsaker = aarsakerOgForklaring.aarsaker,
            forklaring = aarsakerOgForklaring.forklaring,
        )
        queries.gjennomforing.setPublisert(id, false)
        queries.gjennomforing.setApentForPamelding(id, false)

        val dto = logEndring("Gjennomføringen ble avbrutt", id, avbruttAv)
        publishToKafka(dto)
        dto.right()
    }

    fun setApentForPamelding(id: UUID, apentForPamelding: Boolean, agent: Agent): Unit = db.transaction {
        queries.gjennomforing.setApentForPamelding(id, apentForPamelding)

        val operation = if (apentForPamelding) {
            "Åpnet for påmelding"
        } else {
            "Stengte for påmelding"
        }
        val dto = logEndring(operation, id, agent)
        publishToKafka(dto)
    }

    fun setStengtHosArrangor(
        id: UUID,
        periode: Periode,
        beskrivelse: String,
        navIdent: NavIdent,
    ): Either<NonEmptyList<FieldError>, GjennomforingAvtale> = db.transaction {
        return query {
            queries.gjennomforing.setStengtHosArrangor(id, periode, beskrivelse)
        }.mapLeft {
            if (it is IntegrityConstraintViolation.ExclusionViolation) {
                FieldError.of(
                    "Perioden kan ikke overlappe med andre perioder",
                    SetStengtHosArrangorRequest::periodeStart,
                ).nel()
            } else {
                throw it.error
            }
        }.map {
            val operation = listOf(
                "Registrerte stengt hos arrangør i perioden",
                periode.start.formaterDatoTilEuropeiskDatoformat(),
                "-",
                periode.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat(),
            ).joinToString(separator = " ")
            val dto = logEndring(operation, id, navIdent)
            publishToKafka(dto)
            dto
        }
    }

    fun deleteStengtHosArrangor(id: UUID, periodeId: Int, navIdent: NavIdent) = db.transaction {
        queries.gjennomforing.deleteStengtHosArrangor(periodeId)

        val dto = logEndring("Fjernet periode med stengt hos arrangør", id, navIdent)
        publishToKafka(dto)
    }

    fun getEndringshistorikk(id: UUID): EndringshistorikkDto = db.session {
        return queries.endringshistorikk.getEndringshistorikk(DocumentClass.GJENNOMFORING, id)
    }

    fun frikobleKontaktpersonFraGjennomforing(
        kontaktpersonId: UUID,
        gjennomforingId: UUID,
        navIdent: NavIdent,
    ): Unit = db.transaction {
        queries.gjennomforing.frikobleKontaktpersonFraGjennomforing(
            kontaktpersonId = kontaktpersonId,
            gjennomforingId = gjennomforingId,
        )

        logEndring("Kontaktperson ble fjernet fra gjennomføringen", gjennomforingId, navIdent)
    }

    private fun resolveStatus(
        previous: GjennomforingStatusType?,
        request: GjennomforingRequest,
        today: LocalDate,
    ): GjennomforingStatusType {
        return when (previous) {
            GjennomforingStatusType.AVLYST, GjennomforingStatusType.AVBRUTT -> previous

            else -> if (request.sluttDato == null || !request.sluttDato.isBefore(today)) {
                GjennomforingStatusType.GJENNOMFORES
            } else {
                GjennomforingStatusType.AVSLUTTET
            }
        }
    }

    private fun QueryContext.getOrError(id: UUID): GjennomforingAvtale {
        return queries.gjennomforing.getGjennomforingAvtaleOrError(id)
    }

    private fun QueryContext.dispatchNotificationToNewAdministrators(
        dbo: GjennomforingAvtaleDbo,
        navIdent: NavIdent,
    ) {
        val currentAdministratorer = getGruppetiltak(dbo.id)?.administratorer?.map { it.navIdent }?.toSet()
            ?: setOf()

        val administratorsToNotify = (dbo.administratorer - currentAdministratorer - navIdent).toNonEmptyListOrNull()
            ?: return

        val notification = ScheduledNotification(
            title = "Du har blitt satt som administrator på gjennomføringen \"${dbo.navn}\"",
            targets = administratorsToNotify,
            createdAt = Instant.now(),
        )
        queries.notifications.insert(notification)
    }

    private fun QueryContext.logEndring(
        operation: String,
        gjennomforingId: UUID,
        endretAv: Agent,
    ): GjennomforingAvtale {
        val gjennomforing = getOrError(gjennomforingId)
        queries.endringshistorikk.logEndring(
            DocumentClass.GJENNOMFORING,
            operation,
            endretAv,
            gjennomforingId,
            LocalDateTime.now(),
        ) {
            Json.encodeToJsonElement(gjennomforing)
        }
        return gjennomforing
    }

    private fun QueryContext.publishToKafka(gjennomforing: GjennomforingAvtale) {
        val gjennomforingV2: TiltaksgjennomforingV2Dto = TiltaksgjennomforingV2Mapper.fromGjennomforing(gjennomforing)
        val recordV2 = StoredProducerRecord(
            config.gjennomforingV2Topic,
            gjennomforingV2.id.toString().toByteArray(),
            Json.encodeToString(TiltaksgjennomforingV2Dto.serializer(), gjennomforingV2).toByteArray(),
            null,
        )
        queries.kafkaProducerRecord.storeRecord(recordV2)
    }

    // Filtrer vekk underenheter uten fylke
    private fun sanitizeNavEnheter(
        navRegioner: List<NavEnhetNummer>,
        navKontorer: List<NavEnhetNummer>,
    ): List<NavEnhetNummer> = db.session {
        return NavEnhetHelpers.buildNavRegioner(
            (navRegioner + navKontorer).mapNotNull { queries.enhet.get(it)?.toDto() },
        )
            .flatMap { it.enheter.map { it.enhetsnummer } }
    }
}

private fun isEqual(
    previous: GjennomforingAvtale,
    dbo: GjennomforingAvtaleDbo,
): Boolean = dbo == GjennomforingAvtaleDbo(
    id = previous.id,
    navn = previous.navn,
    tiltakstypeId = previous.tiltakstype.id,
    arrangorId = previous.arrangor.id,
    arrangorKontaktpersoner = previous.arrangor.kontaktpersoner.map { it.id },
    startDato = previous.startDato,
    sluttDato = previous.sluttDato,
    status = previous.status.type,
    antallPlasser = previous.antallPlasser,
    avtaleId = previous.avtaleId,
    administratorer = previous.administratorer.map { it.navIdent },
    navEnheter = previous.kontorstruktur
        .flatMap { (region, kontorer) ->
            kontorer.map { kontor -> kontor.enhetsnummer } + region.enhetsnummer
        }
        .toSet(),
    oppstart = previous.oppstart,
    kontaktpersoner = previous.kontaktpersoner.map {
        GjennomforingKontaktpersonDbo(
            navIdent = it.navIdent,
            beskrivelse = it.beskrivelse,
        )
    },
    oppmoteSted = previous.oppmoteSted,
    faneinnhold = previous.faneinnhold,
    beskrivelse = previous.beskrivelse,
    deltidsprosent = previous.deltidsprosent,
    estimertVentetidVerdi = previous.estimertVentetid?.verdi,
    estimertVentetidEnhet = previous.estimertVentetid?.enhet,
    tilgjengeligForArrangorDato = previous.tilgjengeligForArrangorDato,
    amoKategorisering = previous.amoKategorisering,
    utdanningslop = previous.utdanningslop?.toDbo(),
    pameldingType = previous.pameldingType,
    prismodellId = previous.prismodell.id,
)
