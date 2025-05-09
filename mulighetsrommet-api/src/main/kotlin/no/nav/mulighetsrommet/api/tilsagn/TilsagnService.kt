package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.OkonomiConfig
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.responses.StatusResponse
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.tilsagn.api.BesluttTilsagnRequest
import no.nav.mulighetsrommet.api.tilsagn.api.TilAnnulleringRequest
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnRequest
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.tiltak.okonomi.*
import java.time.LocalDateTime
import java.util.*

class TilsagnService(
    val config: Config,
    private val db: ApiDatabase,
) {
    data class Config(
        val okonomiConfig: OkonomiConfig,
        val bestillingTopic: String,
    )

    fun upsert(request: TilsagnRequest, navIdent: NavIdent): Either<List<FieldError>, Tilsagn> = db.transaction {
        val gjennomforing = queries.gjennomforing.get(request.gjennomforingId)
            ?: return FieldError
                .of("Tiltaksgjennomforingen finnes ikke", TilsagnRequest::gjennomforingId)
                .nel()
                .left()

        val minTilsagnCreationDate =
            config.okonomiConfig.minimumTilsagnPeriodeStart[gjennomforing.tiltakstype.tiltakskode]
        if (minTilsagnCreationDate == null) {
            return FieldError
                .of(
                    "Tilsagn for tiltakstype ${gjennomforing.tiltakstype.navn} er ikke støttet enda",
                    TilsagnRequest::periodeStart,
                )
                .nel()
                .left()
        } else if (request.periodeStart < minTilsagnCreationDate) {
            return FieldError
                .of(
                    "Minimum startdato for tilsagn til ${gjennomforing.tiltakstype.navn} er ${minTilsagnCreationDate.formaterDatoTilEuropeiskDatoformat()}",
                    TilsagnRequest::periodeStart,
                )
                .nel()
                .left()
        } else if (gjennomforing.sluttDato !== null && request.periodeSlutt > gjennomforing.sluttDato) {
            return FieldError
                .of(
                    "Sluttdato for tilsagnet kan ikke være etter gjennomføringsperioden",
                    TilsagnRequest::periodeSlutt,
                )
                .nel()
                .left()
        }

        val previous = queries.tilsagn.get(request.id)

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
        )

        validateTilsagnBeregningInput(gjennomforing, request.beregning)
            .flatMap { beregnTilsagn(it) }
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
            .flatMap { dbo ->
                TilsagnValidator.validate(dbo, previous)
            }
            .map { dbo ->
                queries.tilsagn.upsert(dbo)
                queries.totrinnskontroll.upsert(totrinnskontroll)

                val dto = queries.tilsagn.getOrError(dbo.id)

                logEndring("Sendt til godkjenning", dto, navIdent)
                dto
            }
    }

    fun tilAnnulleringRequest(id: UUID, navIdent: NavIdent, request: TilAnnulleringRequest) = db.transaction {
        val tilsagn = queries.tilsagn.get(id) ?: throw StatusException(HttpStatusCode.NotFound, "Fant ikke tilsagn")

        setTilAnnullering(tilsagn, navIdent, request.aarsaker.map { it.name }, request.forklaring)
    }

    fun tilGjorOppRequest(id: UUID, navIdent: NavIdent, request: TilAnnulleringRequest) = db.transaction {
        val tilsagn = queries.tilsagn.get(id) ?: throw StatusException(HttpStatusCode.NotFound, "Fant ikke tilsagn")

        setTilOppgjort(tilsagn, navIdent, request.aarsaker.map { it.name }, request.forklaring)
    }

    fun beregnTilsagn(input: TilsagnBeregningInput): Either<List<FieldError>, TilsagnBeregning> {
        return TilsagnValidator.validateBeregningInput(input)
            .map {
                when (input) {
                    is TilsagnBeregningForhandsgodkjent.Input -> TilsagnBeregningForhandsgodkjent.beregn(input)
                    is TilsagnBeregningFri.Input -> TilsagnBeregningFri.beregn(input)
                }
            }
    }

    fun beslutt(id: UUID, besluttelse: BesluttTilsagnRequest, navIdent: NavIdent): StatusResponse<Tilsagn> = db.transaction {
        val tilsagn = queries.tilsagn.getOrError(id)

        val ansatt = requireNotNull(queries.ansatt.getByNavIdent(navIdent))
        if (!ansatt.hasKontorspesifikkRolle(Rolle.BESLUTTER_TILSAGN, setOf(tilsagn.kostnadssted.enhetsnummer))) {
            return ValidationError(errors = listOf(FieldError.root("Du kan ikke beslutte tilsagnet fordi du mangler budsjettmyndighet ved tilsagnets kostnadssted (${tilsagn.kostnadssted.navn})"))).left()
        }

        return when (tilsagn.status) {
            TilsagnStatus.OPPGJORT, TilsagnStatus.ANNULLERT, TilsagnStatus.GODKJENT, TilsagnStatus.RETURNERT ->
                BadRequest("Tilsagnet kan ikke besluttes fordi det har status ${tilsagn.status}").left()

            TilsagnStatus.TIL_GODKJENNING -> {
                when (besluttelse) {
                    BesluttTilsagnRequest.GodkjentTilsagnRequest -> godkjennTilsagn(tilsagn, navIdent)
                    is BesluttTilsagnRequest.AvvistTilsagnRequest -> returnerTilsagn(tilsagn, besluttelse, navIdent)
                }
            }

            TilsagnStatus.TIL_ANNULLERING -> {
                when (besluttelse) {
                    BesluttTilsagnRequest.GodkjentTilsagnRequest -> annullerTilsagn(tilsagn, navIdent)
                    is BesluttTilsagnRequest.AvvistTilsagnRequest -> avvisAnnullering(
                        tilsagn,
                        besluttelse,
                        navIdent,
                    )
                }
            }

            TilsagnStatus.TIL_OPPGJOR -> {
                val oppgjor = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.GJOR_OPP)
                if (oppgjor.behandletAv == navIdent) {
                    return ValidationError(errors = listOf(FieldError.root("Du kan ikke beslutte oppgjør du selv har opprettet"))).left()
                }

                when (besluttelse) {
                    BesluttTilsagnRequest.GodkjentTilsagnRequest ->
                        gjorOppTilsagn(tilsagn, navIdent)
                            .also {
                                // Ved manuell oppgjør må vi sende melding til OeBS, det trenger vi ikke
                                // når vi gjør opp på en delutbetaling.
                                storeGjorOppBestilling(it)
                            }
                            .right()

                    is BesluttTilsagnRequest.AvvistTilsagnRequest -> avvisOppgjor(tilsagn, besluttelse, navIdent)
                }
            }
        }
    }

    private fun godkjennTilsagn(tilsagn: Tilsagn, besluttetAv: NavIdent): StatusResponse<Tilsagn> = db.transaction {
        require(tilsagn.status == TilsagnStatus.TIL_GODKJENNING)

        val opprettelse = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.OPPRETT)
        if (besluttetAv == opprettelse.behandletAv) {
            return ValidationError(errors = listOf(FieldError.root("Du kan ikke beslutte et tilsagn du selv har opprettet"))).left()
        }

        val besluttetOpprettelse = opprettelse.copy(
            besluttetAv = besluttetAv,
            besluttetTidspunkt = LocalDateTime.now(),
            besluttelse = Besluttelse.GODKJENT,
        )
        queries.totrinnskontroll.upsert(besluttetOpprettelse)
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.GODKJENT)

        storeOpprettBestilling(tilsagn, besluttetOpprettelse)

        val dto = queries.tilsagn.getOrError(tilsagn.id)
        logEndring("Tilsagn godkjent", dto, besluttetAv)
        dto.right()
    }

    private fun returnerTilsagn(
        tilsagn: Tilsagn,
        besluttelse: BesluttTilsagnRequest.AvvistTilsagnRequest,
        besluttetAv: NavIdent,
    ): StatusResponse<Tilsagn> = db.transaction {
        require(tilsagn.status == TilsagnStatus.TIL_GODKJENNING)

        val opprettelse = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.OPPRETT)
        if (besluttetAv == opprettelse.behandletAv) {
            return ValidationError(errors = listOf(FieldError.root("Du kan ikke beslutte et tilsagn du selv har opprettet"))).left()
        }
        if (besluttelse.aarsaker.isEmpty()) {
            return BadRequest(detail = "Årsaker er påkrevd").left()
        }

        queries.totrinnskontroll.upsert(
            opprettelse.copy(
                besluttetAv = besluttetAv,
                besluttetTidspunkt = LocalDateTime.now(),
                besluttelse = Besluttelse.AVVIST,
                aarsaker = besluttelse.aarsaker.map { it.name },
                forklaring = besluttelse.forklaring,
            ),
        )
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.RETURNERT)

        val dto = queries.tilsagn.getOrError(tilsagn.id)
        logEndring("Tilsagn returnert", dto, besluttetAv)
        dto.right()
    }

    private fun QueryContext.annullerTilsagn(tilsagn: Tilsagn, besluttetAv: NavIdent): StatusResponse<Tilsagn> {
        require(tilsagn.status == TilsagnStatus.TIL_ANNULLERING)

        val annullering = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.ANNULLER)
        if (besluttetAv == annullering.behandletAv) {
            return ValidationError(errors = listOf(FieldError.root("Du kan ikke beslutte annullering du selv har opprettet"))).left()
        }

        val besluttetAnnullering = annullering.copy(
            besluttetAv = besluttetAv,
            besluttetTidspunkt = LocalDateTime.now(),
            besluttelse = Besluttelse.GODKJENT,
        )
        queries.totrinnskontroll.upsert(besluttetAnnullering)
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.ANNULLERT)

        storeAnnullerBestilling(tilsagn, besluttetAnnullering)

        val dto = queries.tilsagn.getOrError(tilsagn.id)
        logEndring("Tilsagn annullert", dto, besluttetAv)
        return dto.right()
    }

    private fun QueryContext.avvisAnnullering(
        tilsagn: Tilsagn,
        besluttelse: BesluttTilsagnRequest.AvvistTilsagnRequest,
        besluttetAv: Agent,
    ): StatusResponse<Tilsagn> {
        require(tilsagn.status == TilsagnStatus.TIL_ANNULLERING)

        val annullering = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.ANNULLER)
        if (besluttetAv == annullering.behandletAv) {
            return ValidationError(errors = listOf(FieldError.root("Du kan ikke avvise annullering du selv har opprettet"))).left()
        }

        queries.totrinnskontroll.upsert(
            annullering.copy(
                besluttetAv = besluttetAv,
                besluttetTidspunkt = LocalDateTime.now(),
                besluttelse = Besluttelse.AVVIST,
                aarsaker = besluttelse.aarsaker.map { it.name },
                forklaring = besluttelse.forklaring,
            ),
        )
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.GODKJENT)

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

        queries.totrinnskontroll.upsert(
            Totrinnskontroll(
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
            ),
        )
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.TIL_OPPGJOR)

        val dto = queries.tilsagn.getOrError(tilsagn.id)
        logEndring("Sendt til oppgjør", dto, agent)
        return dto
    }

    private fun QueryContext.gjorOppTilsagn(
        tilsagn: Tilsagn,
        besluttetAv: Agent,
    ): Tilsagn {
        require(tilsagn.status == TilsagnStatus.TIL_OPPGJOR)

        val oppgjor = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.GJOR_OPP)

        queries.totrinnskontroll.upsert(
            oppgjor.copy(
                besluttetAv = besluttetAv,
                besluttetTidspunkt = LocalDateTime.now(),
                besluttelse = Besluttelse.GODKJENT,
            ),
        )
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.OPPGJORT)

        val dto = queries.tilsagn.getOrError(tilsagn.id)
        logEndring("Tilsagn oppgjort", dto, besluttetAv)
        return dto
    }

    private fun avvisOppgjor(
        tilsagn: Tilsagn,
        besluttelse: BesluttTilsagnRequest.AvvistTilsagnRequest,
        besluttetAv: Agent,
    ): StatusResponse<Tilsagn> = db.transaction {
        require(tilsagn.status == TilsagnStatus.TIL_OPPGJOR)

        val oppgjor = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.GJOR_OPP)
        if (besluttetAv == oppgjor.behandletAv) {
            return ValidationError(errors = listOf(FieldError.root("Du kan ikke avvise oppgjør du selv har opprettet"))).left()
        }

        queries.totrinnskontroll.upsert(
            oppgjor.copy(
                besluttetAv = besluttetAv,
                besluttetTidspunkt = LocalDateTime.now(),
                besluttelse = Besluttelse.AVVIST,
                aarsaker = besluttelse.aarsaker.map { it.name },
                forklaring = besluttelse.forklaring,
            ),
        )
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.GODKJENT)

        val dto = queries.tilsagn.getOrError(tilsagn.id)
        logEndring("Oppgjør avvist", dto, besluttetAv)
        return dto.right()
    }

    fun gjorOppAutomatisk(id: UUID, queryContext: QueryContext): Tilsagn {
        var tilsagn = queryContext.queries.tilsagn.getOrError(id)

        tilsagn = queryContext.setTilOppgjort(tilsagn, Tiltaksadministrasjon, emptyList(), null)

        return queryContext.gjorOppTilsagn(tilsagn, Tiltaksadministrasjon)
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

        queries.totrinnskontroll.upsert(
            Totrinnskontroll(
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
            ),
        )
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.TIL_ANNULLERING)

        val dto = queries.tilsagn.getOrError(tilsagn.id)
        logEndring("Sendt til annullering", dto, behandletAv)
        return dto
    }

    fun slettTilsagn(id: UUID): StatusResponse<Unit> = db.transaction {
        val tilsagn = queries.tilsagn.get(id) ?: return NotFound("Fant ikke tilsagn").left()

        if (tilsagn.status != TilsagnStatus.RETURNERT) {
            return BadRequest("Kan ikke slette tilsagn som er godkjent").left()
        }

        queries.tilsagn.delete(id).right()
    }

    fun getAll() = db.session {
        queries.tilsagn.getAll()
    }

    fun getEndringshistorikk(id: UUID): EndringshistorikkDto = db.session {
        queries.endringshistorikk.getEndringshistorikk(DocumentClass.TILSAGN, id)
    }

    private fun validateTilsagnBeregningInput(
        gjennomforing: GjennomforingDto,
        input: TilsagnBeregningInput,
    ): Either<List<FieldError>, TilsagnBeregningInput> {
        return when (input) {
            is TilsagnBeregningForhandsgodkjent.Input -> TilsagnValidator.validateForhandsgodkjentSats(
                gjennomforing.tiltakstype.tiltakskode,
                input,
            )

            else -> input.right()
        }
    }

    private fun QueryContext.storeOpprettBestilling(tilsagn: Tilsagn, opprettelse: Totrinnskontroll) {
        require(opprettelse.besluttetAv != null && opprettelse.besluttetTidspunkt != null) {
            "Tilsagn id=${tilsagn.id} må være besluttet godkjent for å sendes til økonomi"
        }

        val gjennomforing = requireNotNull(queries.gjennomforing.get(tilsagn.gjennomforing.id)) {
            "Fant ikke gjennomforing for tilsagn"
        }

        val avtale = requireNotNull(gjennomforing.avtaleId?.let { queries.avtale.get(it) }) {
            "Gjennomføring ${gjennomforing.id} mangler avtale"
        }

        val arrangor = requireNotNull(
            avtale.arrangor?.let {
                OpprettBestilling.Arrangor(
                    hovedenhet = avtale.arrangor.organisasjonsnummer,
                    underenhet = gjennomforing.arrangor.organisasjonsnummer,
                )
            },
        ) {
            "Avtale ${avtale.id} mangler arrangør"
        }

        val bestilling = OpprettBestilling(
            bestillingsnummer = tilsagn.bestilling.bestillingsnummer,
            tilskuddstype = when (tilsagn.type) {
                TilsagnType.INVESTERING -> Tilskuddstype.TILTAK_INVESTERINGER
                else -> Tilskuddstype.TILTAK_DRIFTSTILSKUDD
            },
            tiltakskode = gjennomforing.tiltakstype.tiltakskode,
            arrangor = arrangor,
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

    private fun QueryContext.storeAnnullerBestilling(tilsagn: Tilsagn, annullering: Totrinnskontroll) {
        require(annullering.besluttetAv != null && annullering.besluttetTidspunkt != null) {
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

    private fun QueryContext.storeGjorOppBestilling(tilsagn: Tilsagn) {
        val oppgjor = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.GJOR_OPP)
        require(oppgjor.besluttetAv != null && oppgjor.besluttetTidspunkt != null) {
            "Tilsagn id=${tilsagn.id} må være besluttet oppgjort for å sende null melding til økonomi"
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
