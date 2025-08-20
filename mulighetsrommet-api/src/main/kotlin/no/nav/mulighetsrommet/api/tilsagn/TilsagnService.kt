package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.OkonomiConfig
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkDto
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnRequest
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.api.BesluttTotrinnskontrollRequest
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.mulighetsrommet.notifications.NotificationMetadata
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import no.nav.tiltak.okonomi.*
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

class TilsagnService(
    val config: Config,
    private val db: ApiDatabase,
    private val navAnsattService: NavAnsattService,
) {
    data class Config(
        val okonomiConfig: OkonomiConfig,
        val bestillingTopic: String,
    )

    fun upsert(request: TilsagnRequest, navIdent: NavIdent): Either<List<FieldError>, Tilsagn> = db.transaction {
        val gjennomforing = requireNotNull(queries.gjennomforing.get(request.gjennomforingId)) {
            "Gjennomforingen finnes ikke"
        }
        requireNotNull(gjennomforing.avtaleId) {
            "Gjennomforingen mangler avtale"
        }
        val avtale = requireNotNull(queries.avtale.get(gjennomforing.avtaleId)) {
            "Avtalen finnes ikke"
        }

        val totrinnskontroll = Totrinnskontroll(
            id = UUID.randomUUID(),
            entityId = request.id,
            behandletAv = navIdent,
            aarsaker = emptyList(),
            forklaring = null,
            type = Totrinnskontroll.Type.OPPRETT,
            behandletTidspunkt = LocalDateTime.now(),
            besluttelse = null,
            besluttetAv = null,
            besluttetTidspunkt = null,
            besluttetAvNavn = null,
            behandletAvNavn = null,
        )

        val previous = queries.tilsagn.get(request.id)
        return TilsagnValidator
            .validate(
                next = request,
                previous = previous,
                gjennomforingSluttDato = gjennomforing.sluttDato,
                tiltakstypeNavn = gjennomforing.tiltakstype.navn,
                arrangorSlettet = gjennomforing.arrangor.slettet,
                minimumTilsagnPeriodeStart = config.okonomiConfig.minimumTilsagnPeriodeStart[gjennomforing.tiltakstype.tiltakskode],
            )
            .flatMap { TilsagnValidator.validateAvtaltSats(request.beregning, avtale) }
            .flatMap { beregnTilsagn(request.beregning) }
            .map { beregning ->
                val lopenummer = previous?.lopenummer
                    ?: queries.tilsagn.getNextLopenummeByGjennomforing(gjennomforing.id)

                val bestillingsnummer = previous?.bestilling?.bestillingsnummer
                    ?: "A-${gjennomforing.lopenummer}-$lopenummer"

                TilsagnDbo(
                    id = request.id,
                    gjennomforingId = request.gjennomforingId,
                    type = request.type,
                    periode = Periode.fromInclusiveDates(request.periodeStart, request.periodeSlutt),
                    lopenummer = lopenummer,
                    kostnadssted = request.kostnadssted,
                    bestillingsnummer = bestillingsnummer,
                    bestillingStatus = null,
                    belopBrukt = 0,
                    beregning = beregning,
                )
            }
            .map { dbo ->
                queries.tilsagn.upsert(dbo)
                queries.totrinnskontroll.upsert(totrinnskontroll)

                val dto = queries.tilsagn.getOrError(dbo.id)

                logEndring("Sendt til godkjenning", dto, navIdent)
                dto
            }
    }

    fun getEndringshistorikk(id: UUID): EndringshistorikkDto = db.session {
        queries.endringshistorikk.getEndringshistorikk(DocumentClass.TILSAGN, id)
    }

    fun slettTilsagn(id: UUID, navIdent: NavIdent): Either<List<FieldError>, Unit> = db.transaction {
        val tilsagn = queries.tilsagn.getOrError(id)

        if (tilsagn.status != TilsagnStatus.RETURNERT) {
            return FieldError.of("Kan ikke slette tilsagn som er godkjent").nel().left()
        }

        val totrinnskontroll = queries.totrinnskontroll.getOrError(entityId = id, type = Totrinnskontroll.Type.OPPRETT)
        if (totrinnskontroll.besluttetAv == navIdent && totrinnskontroll.behandletAv is NavIdent) {
            sendNotifikasjonSlettetTilsagn(tilsagn, besluttetAv = navIdent, behandletAv = totrinnskontroll.behandletAv)
        }

        queries.tilsagn.delete(id)

        Unit.right()
    }

    fun tilAnnulleringRequest(id: UUID, navIdent: NavIdent, request: AarsakerOgForklaringRequest<TilsagnStatusAarsak>): Tilsagn = db.transaction {
        val tilsagn = queries.tilsagn.getOrError(id)

        setTilAnnullering(tilsagn, navIdent, request.aarsaker.map { it.name }, request.forklaring)
    }

    fun tilGjorOppRequest(id: UUID, navIdent: NavIdent, request: AarsakerOgForklaringRequest<TilsagnStatusAarsak>): Tilsagn = db.transaction {
        val tilsagn = queries.tilsagn.getOrError(id)

        setTilOppgjort(tilsagn, navIdent, request.aarsaker.map { it.name }, request.forklaring)
    }

    fun beregnTilsagn(input: TilsagnBeregningInput): Either<List<FieldError>, TilsagnBeregning> {
        return TilsagnValidator.validateBeregningInput(input)
            .map {
                when (input) {
                    is TilsagnBeregningFri.Input -> TilsagnBeregningFri.beregn(input)
                    is TilsagnBeregningFastSatsPerTiltaksplassPerManed.Input -> TilsagnBeregningFastSatsPerTiltaksplassPerManed.beregn(input)
                    is TilsagnBeregningPrisPerManedsverk.Input -> TilsagnBeregningPrisPerManedsverk.beregn(input)
                    is TilsagnBeregningPrisPerUkesverk.Input -> TilsagnBeregningPrisPerUkesverk.beregn(input)
                }
            }
    }

    fun beslutt(id: UUID, request: BesluttTotrinnskontrollRequest<TilsagnStatusAarsak>, navIdent: NavIdent): Either<List<FieldError>, Tilsagn> = db.transaction {
        val tilsagn = queries.tilsagn.getOrError(id)

        val ansatt = requireNotNull(queries.ansatt.getByNavIdent(navIdent))
        if (!ansatt.hasKontorspesifikkRolle(Rolle.BESLUTTER_TILSAGN, setOf(tilsagn.kostnadssted.enhetsnummer))) {
            return FieldError.of("Du kan ikke beslutte tilsagnet fordi du mangler budsjettmyndighet ved tilsagnets kostnadssted (${tilsagn.kostnadssted.navn})")
                .nel()
                .left()
        }

        return when (tilsagn.status) {
            TilsagnStatus.OPPGJORT, TilsagnStatus.ANNULLERT, TilsagnStatus.GODKJENT, TilsagnStatus.RETURNERT -> {
                FieldError.of("Tilsagnet kan ikke besluttes fordi det har status ${tilsagn.status}").nel().left()
            }

            TilsagnStatus.TIL_GODKJENNING -> {
                when (request.besluttelse) {
                    Besluttelse.GODKJENT -> godkjennTilsagn(tilsagn, navIdent).onRight {
                        publishOpprettBestilling(it)
                    }

                    Besluttelse.AVVIST -> returnerTilsagn(tilsagn, request, navIdent)
                }
            }

            TilsagnStatus.TIL_ANNULLERING -> {
                when (request.besluttelse) {
                    Besluttelse.GODKJENT -> annullerTilsagn(tilsagn, navIdent).onRight {
                        publishAnnullerBestilling(it)
                    }

                    Besluttelse.AVVIST -> avvisAnnullering(tilsagn, request, navIdent)
                }
            }

            TilsagnStatus.TIL_OPPGJOR -> {
                when (request.besluttelse) {
                    Besluttelse.GODKJENT -> gjorOppTilsagn(tilsagn, navIdent).onRight {
                        publishGjorOppBestilling(it)
                    }

                    Besluttelse.AVVIST -> avvisOppgjor(tilsagn, request, navIdent)
                }
            }
        }
    }

    fun republishOpprettBestilling(bestillingsnummer: String): Tilsagn = db.transaction {
        val tilsagn = queries.tilsagn.getOrError(bestillingsnummer)
        publishOpprettBestilling(tilsagn)
        tilsagn
    }

    fun gjorOppAutomatisk(id: UUID, queryContext: QueryContext): Tilsagn {
        var tilsagn = queryContext.queries.tilsagn.getOrError(id)
        tilsagn = queryContext.setTilOppgjort(tilsagn, Tiltaksadministrasjon, emptyList(), null)
        return queryContext.gjorOppTilsagn(tilsagn, Tiltaksadministrasjon).getOrElse {
            // TODO returner valideringsfeil i stedet for å kaste exception
            throw IllegalStateException(it.first().detail)
        }
    }

    private fun QueryContext.godkjennTilsagn(
        tilsagn: Tilsagn,
        besluttetAv: NavIdent,
    ): Either<List<FieldError>, Tilsagn> {
        if (tilsagn.status != TilsagnStatus.TIL_GODKJENNING) {
            return FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_GODKJENNING} for å godkjennes")
                .nel()
                .left()
        }

        val opprettelse = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.OPPRETT)
        if (besluttetAv == opprettelse.behandletAv) {
            return FieldError.of("Du kan ikke beslutte et tilsagn du selv har opprettet").nel().left()
        }

        val besluttetOpprettelse = opprettelse.copy(
            besluttetAv = besluttetAv,
            besluttetTidspunkt = LocalDateTime.now(),
            besluttelse = Besluttelse.GODKJENT,
        )
        queries.totrinnskontroll.upsert(besluttetOpprettelse)
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.GODKJENT)

        val dto = queries.tilsagn.getOrError(tilsagn.id)
        logEndring("Tilsagn godkjent", dto, besluttetAv)
        return dto.right()
    }

    private fun QueryContext.returnerTilsagn(
        tilsagn: Tilsagn,
        besluttelse: BesluttTotrinnskontrollRequest<TilsagnStatusAarsak>,
        besluttetAv: NavIdent,
    ): Either<List<FieldError>, Tilsagn> {
        if (tilsagn.status != TilsagnStatus.TIL_GODKJENNING) {
            return FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_GODKJENNING} for å returneres")
                .nel()
                .left()
        }

        if (besluttelse.aarsaker.isEmpty()) {
            return FieldError.of("Årsaker er påkrevd").nel().left()
        }

        val opprettelse = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.OPPRETT)
        val avvistOpprettelse = opprettelse.copy(
            besluttetAv = besluttetAv,
            besluttetTidspunkt = LocalDateTime.now(),
            besluttelse = Besluttelse.AVVIST,
            aarsaker = besluttelse.aarsaker.map { it.name },
            forklaring = besluttelse.forklaring,
        )
        queries.totrinnskontroll.upsert(avvistOpprettelse)
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.RETURNERT)

        val dto = queries.tilsagn.getOrError(tilsagn.id)
        logEndring("Tilsagn returnert", dto, besluttetAv)
        return dto.right()
    }

    private fun QueryContext.setTilAnnullering(
        tilsagn: Tilsagn,
        behandletAv: Agent,
        aarsaker: List<String>,
        forklaring: String?,
    ): Tilsagn {
        require(tilsagn.status == TilsagnStatus.GODKJENT) {
            "Kan bare annullere godkjente tilsagn"
        }

        val totrinnskontroll = Totrinnskontroll(
            id = UUID.randomUUID(),
            entityId = tilsagn.id,
            behandletAv = behandletAv,
            aarsaker = aarsaker,
            forklaring = forklaring,
            type = Totrinnskontroll.Type.ANNULLER,
            behandletTidspunkt = LocalDateTime.now(),
            besluttelse = null,
            besluttetAv = null,
            besluttetTidspunkt = null,
            besluttetAvNavn = null,
            behandletAvNavn = null,
        )
        queries.totrinnskontroll.upsert(totrinnskontroll)
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.TIL_ANNULLERING)

        val dto = queries.tilsagn.getOrError(tilsagn.id)
        logEndring("Sendt til annullering", dto, behandletAv)
        return dto
    }

    private fun QueryContext.annullerTilsagn(
        tilsagn: Tilsagn,
        besluttetAv: NavIdent,
    ): Either<List<FieldError>, Tilsagn> {
        if (tilsagn.status != TilsagnStatus.TIL_ANNULLERING) {
            return FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_ANNULLERING} for at annullering skal godkjennes")
                .nel()
                .left()
        }

        val annullering = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.ANNULLER)
        if (besluttetAv == annullering.behandletAv) {
            return FieldError.of("Du kan ikke beslutte annullering du selv har opprettet").nel().left()
        }

        val besluttetAnnullering = annullering.copy(
            besluttetAv = besluttetAv,
            besluttetTidspunkt = LocalDateTime.now(),
            besluttelse = Besluttelse.GODKJENT,
        )
        queries.totrinnskontroll.upsert(besluttetAnnullering)
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.ANNULLERT)

        val dto = queries.tilsagn.getOrError(tilsagn.id)
        logEndring("Tilsagn annullert", dto, besluttetAv)
        return dto.right()
    }

    private fun QueryContext.avvisAnnullering(
        tilsagn: Tilsagn,
        besluttelse: BesluttTotrinnskontrollRequest<TilsagnStatusAarsak>,
        besluttetAv: NavIdent,
    ): Either<List<FieldError>, Tilsagn> {
        if (tilsagn.status != TilsagnStatus.TIL_ANNULLERING) {
            return FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_ANNULLERING} for at annullering skal avvises")
                .nel()
                .left()
        }

        val annullering = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.ANNULLER)
        val avvistAnnullering = annullering.copy(
            besluttetAv = besluttetAv,
            besluttetTidspunkt = LocalDateTime.now(),
            besluttelse = Besluttelse.AVVIST,
            aarsaker = besluttelse.aarsaker.map { it.name },
            forklaring = besluttelse.forklaring,
        )
        queries.totrinnskontroll.upsert(avvistAnnullering)
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.GODKJENT)

        if (annullering.behandletAv is NavIdent) {
            sendNotifikasjonOmAvvistAnnullering(tilsagn, besluttetAv, annullering.behandletAv)
        }

        val dto = queries.tilsagn.getOrError(tilsagn.id)
        logEndring("Annullering avvist", dto, besluttetAv)
        return dto.right()
    }

    private fun QueryContext.setTilOppgjort(
        tilsagn: Tilsagn,
        agent: Agent,
        aarsaker: List<String>,
        forklaring: String?,
    ): Tilsagn {
        require(tilsagn.status == TilsagnStatus.GODKJENT) {
            "Kan bare gjøre opp godkjente tilsagn"
        }

        val totrinnskontroll = Totrinnskontroll(
            id = UUID.randomUUID(),
            entityId = tilsagn.id,
            behandletAv = agent,
            aarsaker = aarsaker,
            forklaring = forklaring,
            type = Totrinnskontroll.Type.GJOR_OPP,
            behandletTidspunkt = LocalDateTime.now(),
            besluttelse = null,
            besluttetAv = null,
            besluttetTidspunkt = null,
            besluttetAvNavn = null,
            behandletAvNavn = null,
        )
        queries.totrinnskontroll.upsert(totrinnskontroll)
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.TIL_OPPGJOR)

        val dto = queries.tilsagn.getOrError(tilsagn.id)
        logEndring("Sendt til oppgjør", dto, agent)
        return dto
    }

    private fun QueryContext.gjorOppTilsagn(
        tilsagn: Tilsagn,
        besluttetAv: Agent,
    ): Either<List<FieldError>, Tilsagn> {
        if (tilsagn.status != TilsagnStatus.TIL_OPPGJOR) {
            return FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_OPPGJOR} for at oppgjør skal godkjennes")
                .nel()
                .left()
        }

        val oppgjor = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.GJOR_OPP)
        if (besluttetAv is NavIdent && oppgjor.behandletAv == besluttetAv) {
            return FieldError.of("Du kan ikke beslutte oppgjør du selv har opprettet").nel().left()
        }

        val godkjentOppgjor = oppgjor.copy(
            besluttetAv = besluttetAv,
            besluttetTidspunkt = LocalDateTime.now(),
            besluttelse = Besluttelse.GODKJENT,
        )
        queries.totrinnskontroll.upsert(godkjentOppgjor)
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.OPPGJORT)

        val dto = queries.tilsagn.getOrError(tilsagn.id)
        logEndring("Tilsagn oppgjort", dto, besluttetAv)
        return dto.right()
    }

    private fun avvisOppgjor(
        tilsagn: Tilsagn,
        besluttelse: BesluttTotrinnskontrollRequest<TilsagnStatusAarsak>,
        besluttetAv: NavIdent,
    ): Either<List<FieldError>, Tilsagn> = db.transaction {
        if (tilsagn.status != TilsagnStatus.TIL_OPPGJOR) {
            return FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_OPPGJOR} for at oppgjør skal avvises")
                .nel()
                .left()
        }

        val oppgjor = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.GJOR_OPP)
        val avvistOppgjor = oppgjor.copy(
            besluttetAv = besluttetAv,
            besluttetTidspunkt = LocalDateTime.now(),
            besluttelse = Besluttelse.AVVIST,
            aarsaker = besluttelse.aarsaker.map { it.name },
            forklaring = besluttelse.forklaring,
        )
        queries.totrinnskontroll.upsert(avvistOppgjor)
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.GODKJENT)

        if (oppgjor.behandletAv is NavIdent) {
            sendNotifikasjonOmAvvistOppgjor(tilsagn, besluttetAv, oppgjor.behandletAv)
        }

        val dto = queries.tilsagn.getOrError(tilsagn.id)
        logEndring("Oppgjør avvist", dto, besluttetAv)
        return dto.right()
    }

    private fun QueryContext.sendNotifikasjonOmAvvistAnnullering(
        tilsagn: Tilsagn,
        besluttetAv: NavIdent,
        behandletAv: NavIdent,
    ) {
        val beslutterNavn = getAnsattNavn(besluttetAv)
        val tilsagnDisplayName = tilsagn.type.displayName().lowercase()

        val notification = ScheduledNotification(
            title = "Et $tilsagnDisplayName du sendte til annullering er blitt avvist",
            description = """$beslutterNavn avviste annulleringen av $tilsagnDisplayName med kostnadssted ${tilsagn.kostnadssted.navn} for gjennomføringen
                    | "${tilsagn.gjennomforing.navn}". Kontakt $beslutterNavn om dette er feil.
            """.trimMargin(),
            metadata = NotificationMetadata(
                linkText = "Gå til tilsagn",
                link = "/gjennomforinger/${tilsagn.gjennomforing.id}/tilsagn/${tilsagn.id}",
            ),
            createdAt = Instant.now(),
            targets = nonEmptyListOf(behandletAv),
        )
        queries.notifications.insert(notification)
    }

    private fun QueryContext.sendNotifikasjonOmAvvistOppgjor(
        tilsagn: Tilsagn,
        besluttetAv: NavIdent,
        behandletAv: NavIdent,
    ) {
        val beslutterNavn = getAnsattNavn(besluttetAv)
        val tilsagnDisplayName = tilsagn.type.displayName().lowercase()

        val notification = ScheduledNotification(
            title = "Et $tilsagnDisplayName du sendte til oppgjør er blitt avvist",
            description = """$beslutterNavn avviste oppgjøret av $tilsagnDisplayName med kostnadssted ${tilsagn.kostnadssted.navn}
                    | for gjennomføringen "${tilsagn.gjennomforing.navn}". Kontakt $beslutterNavn om dette er feil.
            """.trimMargin(),
            metadata = NotificationMetadata(
                linkText = "Gå til tilsagn",
                link = "/gjennomforinger/${tilsagn.gjennomforing.id}/tilsagn/${tilsagn.id}",
            ),
            createdAt = Instant.now(),
            targets = nonEmptyListOf(behandletAv),
        )
        queries.notifications.insert(notification)
    }

    private fun QueryContext.sendNotifikasjonSlettetTilsagn(
        tilsagn: Tilsagn,
        besluttetAv: NavIdent,
        behandletAv: NavIdent,
    ) {
        val beslutterNavn = getAnsattNavn(besluttetAv)
        val tilsagnDisplayName = tilsagn.type.displayName().lowercase()

        val notification = ScheduledNotification(
            title = "Et $tilsagnDisplayName du sendte til godkjenning er blitt slettet",
            description = """$beslutterNavn slettet et $tilsagnDisplayName med kostnadssted ${tilsagn.kostnadssted.navn} for gjennomføringen ${tilsagn.gjennomforing.navn}. Kontakt $beslutterNavn om dette er feil.""",
            metadata = NotificationMetadata(
                linkText = "Gå til gjennomføringen",
                link = "/gjennomforinger/${tilsagn.gjennomforing.id}",
            ),
            targets = nonEmptyListOf(behandletAv),
            createdAt = Instant.now(),
        )
        queries.notifications.insert(notification)
    }

    private fun getAnsattNavn(navIdent: NavIdent): String {
        val beslutterAnsatt = navAnsattService.getNavAnsattByNavIdent(navIdent)
        return beslutterAnsatt?.displayName() ?: navIdent.value
    }

    private fun QueryContext.publishOpprettBestilling(tilsagn: Tilsagn) {
        val opprettelse = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.OPPRETT)
        check(opprettelse.besluttetAv != null && opprettelse.besluttetTidspunkt != null) {
            "Tilsagn id=${tilsagn.id} må være besluttet godkjent for å sendes til økonomi"
        }

        val gjennomforing = checkNotNull(queries.gjennomforing.get(tilsagn.gjennomforing.id)) {
            "Fant ikke gjennomforing for tilsagn"
        }

        val avtale = checkNotNull(gjennomforing.avtaleId?.let { queries.avtale.get(it) }) {
            "Gjennomføring ${gjennomforing.id} mangler avtale"
        }

        val bestilling = OpprettBestilling(
            bestillingsnummer = tilsagn.bestilling.bestillingsnummer,
            tilskuddstype = when (tilsagn.type) {
                TilsagnType.INVESTERING -> Tilskuddstype.TILTAK_INVESTERINGER
                else -> Tilskuddstype.TILTAK_DRIFTSTILSKUDD
            },
            tiltakskode = gjennomforing.tiltakstype.tiltakskode,
            arrangor = gjennomforing.arrangor.organisasjonsnummer,
            kostnadssted = tilsagn.kostnadssted.enhetsnummer,
            avtalenummer = avtale.sakarkivNummer?.value,
            belop = tilsagn.beregning.output.belop,
            periode = tilsagn.periode,
            behandletAv = opprettelse.behandletAv.toOkonomiPart(),
            behandletTidspunkt = opprettelse.behandletTidspunkt,
            besluttetAv = opprettelse.besluttetAv.toOkonomiPart(),
            besluttetTidspunkt = opprettelse.besluttetTidspunkt,
        )

        storeOkonomiMelding(bestilling.bestillingsnummer, OkonomiBestillingMelding.Bestilling(bestilling))
    }

    private fun QueryContext.publishAnnullerBestilling(tilsagn: Tilsagn) {
        val annullering = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.ANNULLER)
        check(annullering.besluttetAv != null && annullering.besluttetTidspunkt != null) {
            "Tilsagn id=${tilsagn.id} må være besluttet annullert for å sendes som annullert til økonomi"
        }

        val annullerBestilling = AnnullerBestilling(
            bestillingsnummer = tilsagn.bestilling.bestillingsnummer,
            behandletAv = annullering.behandletAv.toOkonomiPart(),
            behandletTidspunkt = annullering.behandletTidspunkt,
            besluttetAv = annullering.besluttetAv.toOkonomiPart(),
            besluttetTidspunkt = annullering.besluttetTidspunkt,
        )

        storeOkonomiMelding(
            tilsagn.bestilling.bestillingsnummer,
            OkonomiBestillingMelding.Annullering(annullerBestilling),
        )
    }

    private fun QueryContext.publishGjorOppBestilling(tilsagn: Tilsagn) {
        val oppgjor = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.GJOR_OPP)
        check(oppgjor.besluttetAv != null && oppgjor.besluttetTidspunkt != null) {
            "Tilsagn id=${tilsagn.id} må være besluttet oppgjort for å kunne sendes til økonomi"
        }

        val faktura = GjorOppBestilling(
            bestillingsnummer = tilsagn.bestilling.bestillingsnummer,
            behandletAv = oppgjor.behandletAv.toOkonomiPart(),
            behandletTidspunkt = oppgjor.behandletTidspunkt,
            besluttetAv = oppgjor.besluttetAv.toOkonomiPart(),
            besluttetTidspunkt = oppgjor.besluttetTidspunkt,
        )

        storeOkonomiMelding(tilsagn.bestilling.bestillingsnummer, OkonomiBestillingMelding.GjorOppBestilling(faktura))
    }

    private fun QueryContext.logEndring(
        operation: String,
        dto: Tilsagn,
        endretAv: Agent,
    ) {
        queries.endringshistorikk.logEndring(
            DocumentClass.TILSAGN,
            operation,
            endretAv,
            dto.id,
            LocalDateTime.now(),
        ) {
            Json.encodeToJsonElement(dto)
        }
    }

    private fun QueryContext.storeOkonomiMelding(bestillingsnummer: String, message: OkonomiBestillingMelding) {
        val record = StoredProducerRecord(
            config.bestillingTopic,
            bestillingsnummer.toByteArray(),
            Json.encodeToString(message).toByteArray(),
            null,
        )
        queries.kafkaProducerRecord.storeRecord(record)
    }
}
