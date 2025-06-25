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
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattService
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
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.*
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
            "Tiltaksgjennomforingen finnes ikke"
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
        return TilsagnValidator.validate(
            next = request,
            previous = previous,
            gjennomforingSluttDato = gjennomforing.sluttDato,
            tiltakstypeNavn = gjennomforing.tiltakstype.navn,
            arrangorSlettet = gjennomforing.arrangor.slettet,
            minimumTilsagnPeriodeStart = config.okonomiConfig.minimumTilsagnPeriodeStart[gjennomforing.tiltakstype.tiltakskode],
        )
            .flatMap {
                when (request.beregning) {
                    is TilsagnBeregningForhandsgodkjent.Input -> TilsagnValidator.validateForhandsgodkjentSats(
                        gjennomforing.tiltakstype.tiltakskode,
                        request.beregning,
                    )
                    else -> request.beregning.right()
                }
            }
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
                when (besluttelse) {
                    BesluttTilsagnRequest.GodkjentTilsagnRequest -> {
                        val oppgjor =
                            queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.GJOR_OPP)
                        if (oppgjor.behandletAv == navIdent) {
                            return ValidationError(errors = listOf(FieldError.root("Du kan ikke beslutte oppgjør du selv har opprettet"))).left()
                        }

                        val oppgjortTilsagn = gjorOppTilsagn(tilsagn, navIdent)
                        publishGjorOppBestilling(oppgjortTilsagn)
                        oppgjortTilsagn.right()
                    }

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

        val dto = queries.tilsagn.getOrError(tilsagn.id)
        logEndring("Tilsagn godkjent", dto, besluttetAv)
        publishOpprettBestilling(dto)

        dto.right()
    }

    private fun returnerTilsagn(
        tilsagn: Tilsagn,
        besluttelse: BesluttTilsagnRequest.AvvistTilsagnRequest,
        besluttetAv: NavIdent,
    ): StatusResponse<Tilsagn> = db.transaction {
        require(tilsagn.status == TilsagnStatus.TIL_GODKJENNING)

        val opprettelse = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.OPPRETT)
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

        val dto = queries.tilsagn.getOrError(tilsagn.id)
        logEndring("Tilsagn annullert", dto, besluttetAv)
        publishAnnullerBestilling(dto)

        return dto.right()
    }

    private fun QueryContext.avvisAnnullering(
        tilsagn: Tilsagn,
        besluttelse: BesluttTilsagnRequest.AvvistTilsagnRequest,
        besluttetAv: Agent,
    ): StatusResponse<Tilsagn> {
        require(tilsagn.status == TilsagnStatus.TIL_ANNULLERING)

        val annullering = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.ANNULLER)

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
        val beslutterAnsatt = navAnsattService.getNavAnsattByNavIdent(
            besluttetAv as NavIdent,
        )
        val beslutterNavn = beslutterAnsatt?.displayName() ?: besluttetAv.textRepr()
        val tilsagnDisplayName = tilsagn.type.displayName().lowercase()

        queries.notifications.insert(
            ScheduledNotification(
                title = "Et $tilsagnDisplayName du sendte til annullering er blitt avvist",
                description = """$beslutterNavn avviste annulleringen av $tilsagnDisplayName med kostnadssted ${tilsagn.kostnadssted.navn} for gjennomføringen
                    | "${tilsagn.gjennomforing.navn}". Kontakt $beslutterNavn om dette er feil.
                """.trimMargin(),
                metadata = NotificationMetadata(
                    linkText = "Gå til tilsagn",
                    link = "/gjennomforinger/${tilsagn.gjennomforing.id}/tilsagn/${tilsagn.id}",
                ),
                createdAt = Instant.now(),
                targets = nonEmptyListOf(annullering.behandletAv as NavIdent),
            ),
        )
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
                besluttetAvNavn = null,
                behandletAvNavn = null,
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

        val beslutterAnsatt = navAnsattService.getNavAnsattByNavIdent(
            besluttetAv as NavIdent,
        )
        val beslutterNavn = beslutterAnsatt?.displayName() ?: oppgjor.besluttetAv
        val tilsagnDisplayName = tilsagn.type.displayName().lowercase()

        queries.notifications.insert(
            ScheduledNotification(
                title = "Et $tilsagnDisplayName du sendte til oppgjør er blitt avvist",
                description = """$beslutterNavn avviste oppgjøret av $tilsagnDisplayName med kostnadssted ${tilsagn.kostnadssted.navn}
                    | for gjennomføringen "${tilsagn.gjennomforing.navn}". Kontakt $beslutterNavn om dette er feil.
                """.trimMargin(),
                metadata = NotificationMetadata(
                    linkText = "Gå til tilsagn",
                    link = "/gjennomforinger/${tilsagn.gjennomforing.id}/tilsagn/${tilsagn.id}",
                ),
                createdAt = Instant.now(),
                targets = nonEmptyListOf(oppgjor.behandletAv as NavIdent),
            ),
        )
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
                besluttetAvNavn = null,
                behandletAvNavn = null,
            ),
        )
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.TIL_ANNULLERING)

        val dto = queries.tilsagn.getOrError(tilsagn.id)
        logEndring("Sendt til annullering", dto, behandletAv)
        return dto
    }

    fun slettTilsagn(id: UUID, navIdent: NavIdent): StatusResponse<Unit> = db.transaction {
        val tilsagn = queries.tilsagn.get(id) ?: return NotFound("Fant ikke tilsagn").left()

        if (tilsagn.status != TilsagnStatus.RETURNERT) {
            return BadRequest("Kan ikke slette tilsagn som er godkjent").left()
        }
        val totrinnskontroll = queries.totrinnskontroll.get(entityId = id, type = Totrinnskontroll.Type.OPPRETT)

        queries.tilsagn.delete(id)
        if (totrinnskontroll?.besluttetAv == navIdent && totrinnskontroll.behandletAv is NavIdent) {
            sendNotifikasjonSlettetTilsagn(tilsagn, besluttetAv = navIdent, behandletAv = totrinnskontroll.behandletAv)
        }

        Unit.right()
    }

    private fun QueryContext.sendNotifikasjonSlettetTilsagn(
        tilsagn: Tilsagn,
        besluttetAv: NavIdent,
        behandletAv: NavIdent,
    ) {
        val beslutterAnsatt = navAnsattService.getNavAnsattByNavIdent(besluttetAv)
        val beslutterNavn = beslutterAnsatt?.displayName() ?: besluttetAv.value
        val tilsagnDisplayName = tilsagn.type.displayName().lowercase()

        val title = "Et $tilsagnDisplayName du sendte til godkjenning er blitt slettet"
        val description =
            """$beslutterNavn slettet et $tilsagnDisplayName med kostnadssted ${tilsagn.kostnadssted.navn} for gjennomføringen ${tilsagn.gjennomforing.navn}. Kontakt $beslutterNavn om dette er feil."""

        val notice = ScheduledNotification(
            title = title,
            description = description,
            metadata = NotificationMetadata(
                linkText = "Gå til gjennomføringen",
                link = "/gjennomforinger/${tilsagn.gjennomforing.id}",
            ),
            targets = nonEmptyListOf(behandletAv),
            createdAt = Instant.now(),
        )
        queries.notifications.insert(notice)
    }

    fun getEndringshistorikk(id: UUID): EndringshistorikkDto = db.session {
        queries.endringshistorikk.getEndringshistorikk(DocumentClass.TILSAGN, id)
    }

    private fun QueryContext.publishOpprettBestilling(tilsagn: Tilsagn) {
        val opprettelse = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.OPPRETT)
        check(opprettelse.besluttetAv != null && opprettelse.besluttetTidspunkt != null) {
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
