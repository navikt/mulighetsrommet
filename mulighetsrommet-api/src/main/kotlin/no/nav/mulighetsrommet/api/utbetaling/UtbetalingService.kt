package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.right
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotliquery.TransactionalSession
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navenhet.buildRegionList
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.api.*
import no.nav.mulighetsrommet.api.utbetaling.db.DelutbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.api.utbetaling.task.JournalforUtbetaling
import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.okonomi.OkonomiBestillingMelding
import no.nav.tiltak.okonomi.OpprettFaktura
import no.nav.tiltak.okonomi.toOkonomiPart
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

class UtbetalingService(
    private val config: Config,
    private val db: ApiDatabase,
    private val tilsagnService: TilsagnService,
    private val personService: PersonService,
    private val journalforUtbetaling: JournalforUtbetaling,
) {
    data class Config(
        val bestillingTopic: String,
    )

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun getByGjennomforing(id: UUID): List<UtbetalingKompaktDto> = db.session {
        queries.utbetaling.getByGjennomforing(id).map { utbetaling ->
            val delutbetalinger = queries.delutbetaling.getByUtbetalingId(utbetaling.id)

            val (belopUtbetalt, kostnadssteder) = when (utbetaling.status) {
                Utbetaling.UtbetalingStatus.FERDIG_BEHANDLET ->
                    Pair(
                        delutbetalinger.sumOf {
                            it.belop
                        },
                        delutbetalinger.map { delutbetaling ->
                            queries.tilsagn.getOrError(delutbetaling.tilsagnId).kostnadssted
                        }.distinct(),
                    )
                else -> (null to emptyList())
            }

            UtbetalingKompaktDto(
                id = utbetaling.id,
                status = AdminUtbetalingStatus.fromUtbetalingStatus(utbetaling.status),
                periode = utbetaling.periode,
                kostnadssteder = kostnadssteder,
                belopUtbetalt = belopUtbetalt,
                type = UtbetalingType.from(utbetaling),
            )
        }
    }

    fun godkjentAvArrangor(
        utbetalingId: UUID,
        kid: Kid?,
    ): Either<List<FieldError>, AutomatiskUtbetalingResult> = db.transaction {
        val utbetaling = queries.utbetaling.getOrError(utbetalingId)
        if (utbetaling.status != Utbetaling.UtbetalingStatus.OPPRETTET) {
            return FieldError.of("Utbetaling er allerede godkjent").nel().left()
        }

        queries.utbetaling.setGodkjentAvArrangor(utbetalingId, LocalDateTime.now())
        queries.utbetaling.setKid(utbetalingId, kid)
        journalforUtbetaling.schedule(utbetalingId, Instant.now(), session as TransactionalSession, emptyList())
        queries.utbetaling.setStatus(utbetalingId, Utbetaling.UtbetalingStatus.INNSENDT)
        logEndring("Utbetaling sendt inn", getOrError(utbetalingId), Arrangor)

        automatiskUtbetaling(utbetalingId)
            .also { log.info("Automatisk utbetaling for utbetaling=$utbetalingId resulterte i: $it") }
            .right()
    }

    fun opprettUtbetaling(
        request: UtbetalingValidator.OpprettUtbetaling,
        agent: Agent,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        if (queries.utbetaling.get(request.id) != null) {
            return listOf(FieldError.of("Utbetalingen er allerede opprettet")).left()
        }

        queries.utbetaling.upsert(
            UtbetalingDbo(
                id = request.id,
                gjennomforingId = request.gjennomforingId,
                kontonummer = request.kontonummer,
                kid = request.kidNummer,
                beregning = UtbetalingBeregningFri.beregn(
                    input = UtbetalingBeregningFri.Input(
                        belop = request.belop,
                    ),
                ),
                periode = Periode.fromInclusiveDates(
                    request.periodeStart,
                    request.periodeSlutt,
                ),
                innsender = agent,
                beskrivelse = request.beskrivelse,
                tilskuddstype = request.tilskuddstype,
                godkjentAvArrangorTidspunkt = if (agent is Arrangor) {
                    LocalDateTime.now()
                } else {
                    null
                },
                status = Utbetaling.UtbetalingStatus.INNSENDT,
            ),
        )

        val dto = getOrError(request.id)
        logEndring("Utbetaling sendt inn", dto, agent)

        if (agent is Arrangor) {
            journalforUtbetaling.schedule(
                utbetalingId = dto.id,
                startTime = Instant.now(),
                tx = session as TransactionalSession,
                vedlegg = request.vedlegg,
            )
        }

        dto.right()
    }

    fun opprettDelutbetalinger(
        request: OpprettDelutbetalingerRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        val utbetaling = getOrError(request.utbetalingId)

        val opprettDelutbetalinger = request.delutbetalinger.map { req ->
            val tilsagn = queries.tilsagn.getOrError(req.tilsagnId)
            UtbetalingValidator.OpprettDelutbetaling(
                id = req.id,
                gjorOppTilsagn = req.gjorOppTilsagn,
                tilsagn = tilsagn,
                belop = req.belop,
            )
        }
        UtbetalingValidator
            .validateOpprettDelutbetalinger(utbetaling, opprettDelutbetalinger, request.begrunnelseMindreBetalt)
            .map { delutbetalinger ->
                // Slett de som ikke er med i requesten
                queries.delutbetaling.getByUtbetalingId(utbetaling.id)
                    .filter { delutbetaling -> delutbetaling.id !in request.delutbetalinger.map { it.id } }
                    .forEach { delutbetaling ->
                        require(delutbetaling.status == DelutbetalingStatus.RETURNERT) {
                            "Fatal! Delutbetaling kan ikke slettes fordi den har status: ${delutbetaling.status}"
                        }
                        queries.delutbetaling.delete(delutbetaling.id)
                    }

                delutbetalinger.forEach {
                    upsertDelutbetaling(utbetaling, it.tilsagn, it.id, it.belop, it.gjorOppTilsagn, navIdent)
                }
                queries.utbetaling.setStatus(utbetaling.id, Utbetaling.UtbetalingStatus.TIL_ATTESTERING)
                if (request.begrunnelseMindreBetalt != null) {
                    queries.utbetaling.setBegrunnelseMindreBetalt(utbetaling.id, request.begrunnelseMindreBetalt)
                }

                val dto = getOrError(utbetaling.id)
                logEndring("Utbetaling sendt til attestering", dto, navIdent)
                dto
            }
    }

    fun besluttDelutbetaling(
        id: UUID,
        request: BesluttDelutbetalingRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Delutbetaling> = db.transaction {
        val delutbetaling = queries.delutbetaling.getOrError(id)
        if (delutbetaling.status != DelutbetalingStatus.TIL_ATTESTERING) {
            return listOf(FieldError.of("Utbetaling er ikke satt til attestering")).left()
        }

        val kostnadssted = queries.tilsagn.getOrError(delutbetaling.tilsagnId).kostnadssted
        val ansatt = checkNotNull(queries.ansatt.getByNavIdent(navIdent))
        if (!ansatt.hasKontorspesifikkRolle(Rolle.ATTESTANT_UTBETALING, setOf(kostnadssted.enhetsnummer))) {
            return listOf(FieldError.of("Kan ikke attestere utbetalingen fordi du ikke er attestant ved tilsagnets kostnadssted (${kostnadssted.navn})")).left()
        }

        when (request) {
            is BesluttDelutbetalingRequest.Avvist -> {
                if (request.aarsaker.isEmpty()) {
                    return listOf(FieldError.of("Du må velge minst én årsak")).left()
                } else if (DelutbetalingReturnertAarsak.FEIL_ANNET in request.aarsaker && request.forklaring.isNullOrBlank()) {
                    return listOf(FieldError.of("Du må skrive en forklaring når du velger 'Annet'")).left()
                }

                returnerDelutbetaling(delutbetaling, request.aarsaker, request.forklaring, navIdent)
            }

            is BesluttDelutbetalingRequest.Godkjent -> {
                val opprettelse = queries.totrinnskontroll.getOrError(delutbetaling.id, Totrinnskontroll.Type.OPPRETT)
                if (navIdent == opprettelse.behandletAv) {
                    return listOf(FieldError.of("Kan ikke attestere en utbetaling du selv har opprettet")).left()
                }

                val tilsagnOpprettelse =
                    queries.totrinnskontroll.getOrError(delutbetaling.tilsagnId, Totrinnskontroll.Type.OPPRETT)
                if (navIdent == tilsagnOpprettelse.besluttetAv) {
                    return listOf(FieldError.of("Kan ikke attestere en utbetaling der du selv har besluttet tilsagnet")).left()
                }

                godkjennDelutbetaling(delutbetaling, navIdent)
            }
        }

        queries.delutbetaling.getOrError(id).right()
    }

    fun republishFaktura(fakturanummer: String): Delutbetaling = db.transaction {
        val delutbetaling = queries.delutbetaling.getOrError(fakturanummer)
        publishOpprettFaktura(delutbetaling)
        delutbetaling
    }

    private fun QueryContext.automatiskUtbetaling(utbetalingId: UUID): AutomatiskUtbetalingResult {
        val utbetaling = queries.utbetaling.getOrError(utbetalingId)

        when (utbetaling.beregning) {
            is UtbetalingBeregningFri -> return AutomatiskUtbetalingResult.FEIL_PRISMODELL

            is UtbetalingBeregningPrisPerManedsverkMedDeltakelsesmengder,
            is UtbetalingBeregningPrisPerManedsverk,
            is UtbetalingBeregningPrisPerUkesverk,
            -> Unit
        }

        val relevanteTilsagn = queries.tilsagn.getAll(
            gjennomforingId = utbetaling.gjennomforing.id,
            statuser = listOf(TilsagnStatus.GODKJENT),
            typer = listOf(TilsagnType.TILSAGN, TilsagnType.EKSTRATILSAGN),
            periodeIntersectsWith = utbetaling.periode,
        )
        if (relevanteTilsagn.size != 1) {
            return AutomatiskUtbetalingResult.FEIL_ANTALL_TILSAGN
        }

        val tilsagn = relevanteTilsagn[0]
        if (tilsagn.gjenstaendeBelop() < utbetaling.beregning.output.belop) {
            return AutomatiskUtbetalingResult.IKKE_NOK_PENGER
        }

        val delutbetalinger = queries.delutbetaling.getByUtbetalingId(utbetalingId)
        if (delutbetalinger.isNotEmpty()) {
            return AutomatiskUtbetalingResult.DELUTBETALINGER_ALLEREDE_OPPRETTET
        }

        val delutbetalingId = UUID.randomUUID()
        upsertDelutbetaling(
            utbetaling = utbetaling,
            tilsagn = tilsagn,
            id = delutbetalingId,
            belop = utbetaling.beregning.output.belop,
            gjorOppTilsagn = tilsagn.periode.getLastInclusiveDate() in utbetaling.periode,
            behandletAv = Tiltaksadministrasjon,
        )

        val delutbetaling = queries.delutbetaling.getOrError(delutbetalingId)
        godkjennDelutbetaling(delutbetaling, Tiltaksadministrasjon)

        return AutomatiskUtbetalingResult.GODKJENT
    }

    private fun QueryContext.upsertDelutbetaling(
        utbetaling: Utbetaling,
        tilsagn: Tilsagn,
        id: UUID,
        belop: Int,
        gjorOppTilsagn: Boolean,
        behandletAv: Agent,
    ) {
        require(tilsagn.status == TilsagnStatus.GODKJENT) {
            "Tilsagn er ikke godkjent id=${tilsagn.id} status=${tilsagn.status}"
        }

        val periode = requireNotNull(utbetaling.periode.intersect(tilsagn.periode)) {
            "Utbetalingsperiode og tilsagnsperiode overlapper ikke"
        }

        val delutbetaling = queries.delutbetaling.get(id)

        val lopenummer = delutbetaling?.lopenummer
            ?: queries.delutbetaling.getNextLopenummerByTilsagn(tilsagn.id)

        val fakturanummer = delutbetaling?.faktura?.fakturanummer
            ?: "${tilsagn.bestilling.bestillingsnummer}-$lopenummer"

        val dbo = DelutbetalingDbo(
            id = id,
            utbetalingId = utbetaling.id,
            tilsagnId = tilsagn.id,
            status = DelutbetalingStatus.TIL_ATTESTERING,
            periode = periode,
            belop = belop,
            gjorOppTilsagn = gjorOppTilsagn,
            lopenummer = lopenummer,
            fakturanummer = fakturanummer,
            fakturaStatus = null,
            fakturaStatusSistOppdatert = LocalDateTime.now(),
        )

        queries.delutbetaling.upsert(dbo)
        queries.totrinnskontroll.upsert(
            Totrinnskontroll(
                id = UUID.randomUUID(),
                entityId = id,
                behandletAv = behandletAv,
                aarsaker = emptyList(),
                forklaring = null,
                type = Totrinnskontroll.Type.OPPRETT,
                behandletTidspunkt = LocalDateTime.now(),
                besluttelse = null,
                besluttetAv = null,
                besluttetTidspunkt = null,
                behandletAvNavn = null,
                besluttetAvNavn = null,
            ),
        )
    }

    private fun QueryContext.godkjennDelutbetaling(
        delutbetaling: Delutbetaling,
        besluttetAv: Agent,
    ) {
        val opprettelse = queries.totrinnskontroll.getOrError(delutbetaling.id, Totrinnskontroll.Type.OPPRETT)
        require(opprettelse.besluttetAv == null) {
            "Utbetaling er allerede besluttet"
        }

        queries.delutbetaling.setStatus(delutbetaling.id, DelutbetalingStatus.GODKJENT)
        queries.totrinnskontroll.upsert(
            opprettelse.copy(
                besluttetAv = besluttetAv,
                besluttelse = Besluttelse.GODKJENT,
                besluttetTidspunkt = LocalDateTime.now(),
                aarsaker = emptyList(),
                forklaring = null,
            ),
        )

        val utbetaling = getOrError(delutbetaling.utbetalingId)
        val delutbetalinger = queries.delutbetaling.getByUtbetalingId(delutbetaling.utbetalingId)

        delutbetalinger.forEach {
            val tilsagn = queries.tilsagn.getOrError(it.tilsagnId)
            if (tilsagn.status != TilsagnStatus.GODKJENT) {
                return returnerDelutbetaling(
                    it,
                    listOf(DelutbetalingReturnertAarsak.TILSAGN_FEIL_STATUS),
                    null,
                    Tiltaksadministrasjon,
                )
            }
        }

        if (delutbetalinger.all { it.status == DelutbetalingStatus.GODKJENT }) {
            godkjennUtbetaling(utbetaling, delutbetalinger)
        }
    }

    private fun QueryContext.godkjennUtbetaling(
        utbetaling: Utbetaling,
        delutbetalinger: List<Delutbetaling>,
    ) {
        queries.delutbetaling.setStatusForDelutbetalingerForBetaling(
            utbetaling.id,
            DelutbetalingStatus.OVERFORT_TIL_UTBETALING,
        )

        delutbetalinger.forEach { delutbetaling ->
            val tilsagn = queries.tilsagn.getOrError(delutbetaling.tilsagnId)
            val benyttetBelop = tilsagn.belopBrukt + delutbetaling.belop
            queries.tilsagn.setBruktBelop(tilsagn.id, benyttetBelop)
            if (delutbetaling.gjorOppTilsagn || benyttetBelop == tilsagn.beregning.output.belop) {
                tilsagnService.gjorOppAutomatisk(delutbetaling.tilsagnId, this)
            }
            publishOpprettFaktura(delutbetaling)
        }

        queries.utbetaling.setStatus(utbetaling.id, Utbetaling.UtbetalingStatus.FERDIG_BEHANDLET)
        logEndring("Overført til utbetaling", utbetaling, Tiltaksadministrasjon)
    }

    private fun QueryContext.returnerDelutbetaling(
        delutbetaling: Delutbetaling,
        aarsaker: List<DelutbetalingReturnertAarsak>,
        forklaring: String?,
        besluttetAv: Agent,
    ) {
        setReturnertDelutbetaling(delutbetaling, aarsaker, forklaring, besluttetAv)

        // Set også de resterende delutbetalingene som returnert
        queries.delutbetaling.getByUtbetalingId(delutbetaling.utbetalingId)
            .filter { it.id != delutbetaling.id }
            .forEach {
                setReturnertDelutbetaling(
                    it,
                    listOf(DelutbetalingReturnertAarsak.PROPAGERT_RETUR),
                    null,
                    Tiltaksadministrasjon,
                )
            }

        queries.utbetaling.setStatus(delutbetaling.utbetalingId, Utbetaling.UtbetalingStatus.RETURNERT)
        logEndring(
            "Utbetaling returnert",
            getOrError(delutbetaling.utbetalingId),
            besluttetAv,
        )
    }

    private fun QueryContext.setReturnertDelutbetaling(
        delutbetaling: Delutbetaling,
        aarsaker: List<DelutbetalingReturnertAarsak>,
        forklaring: String?,
        besluttetAv: Agent,
    ) {
        val opprettelse = queries.totrinnskontroll.getOrError(delutbetaling.id, Totrinnskontroll.Type.OPPRETT)
        queries.delutbetaling.setStatus(delutbetaling.id, DelutbetalingStatus.RETURNERT)
        queries.totrinnskontroll.upsert(
            opprettelse.copy(
                besluttetAv = besluttetAv,
                besluttelse = Besluttelse.AVVIST,
                aarsaker = aarsaker.map { it.name },
                forklaring = forklaring,
                besluttetTidspunkt = LocalDateTime.now(),
            ),
        )
    }

    private fun QueryContext.logEndring(
        operation: String,
        dto: Utbetaling,
        endretAv: Agent,
    ) {
        queries.endringshistorikk.logEndring(
            DocumentClass.UTBETALING,
            operation,
            endretAv,
            dto.id,
            LocalDateTime.now(),
        ) {
            Json.encodeToJsonElement(dto)
        }
    }

    private fun QueryContext.publishOpprettFaktura(delutbetaling: Delutbetaling) {
        check(delutbetaling.status == DelutbetalingStatus.GODKJENT) {
            "Delutbetaling må være godkjent for "
        }

        val opprettelse = queries.totrinnskontroll.getOrError(delutbetaling.id, Totrinnskontroll.Type.OPPRETT)
        check(opprettelse.besluttetAv != null && opprettelse.besluttetTidspunkt != null && opprettelse.besluttelse == Besluttelse.GODKJENT) {
            "Delutbetaling id=${delutbetaling.id} må være besluttet godkjent for å sendes til økonomi"
        }

        val utbetaling = queries.utbetaling.getOrError(delutbetaling.utbetalingId)
        val kontonummer = checkNotNull(utbetaling.betalingsinformasjon.kontonummer) {
            "Kontonummer mangler for utbetaling med id=${delutbetaling.utbetalingId}"
        }

        val tilsagn = queries.tilsagn.getOrError(delutbetaling.tilsagnId)

        val beskrivelse = """
            Tiltakstype: ${tilsagn.tiltakstype.navn}
            Periode: ${tilsagn.periode.formatPeriode()}
            Tilsagnsnummer: ${tilsagn.bestilling.bestillingsnummer}
        """.trimIndent()

        val faktura = OpprettFaktura(
            fakturanummer = delutbetaling.faktura.fakturanummer,
            bestillingsnummer = tilsagn.bestilling.bestillingsnummer,
            betalingsinformasjon = OpprettFaktura.Betalingsinformasjon(
                kontonummer = kontonummer,
                kid = utbetaling.betalingsinformasjon.kid,
            ),
            belop = delutbetaling.belop,
            periode = delutbetaling.periode,
            behandletAv = opprettelse.behandletAv.toOkonomiPart(),
            behandletTidspunkt = opprettelse.behandletTidspunkt,
            besluttetAv = opprettelse.besluttetAv.toOkonomiPart(),
            besluttetTidspunkt = opprettelse.besluttetTidspunkt,
            gjorOppBestilling = delutbetaling.gjorOppTilsagn,
            beskrivelse = beskrivelse,
        )

        queries.delutbetaling.setSendtTilOkonomi(
            delutbetaling.utbetalingId,
            delutbetaling.tilsagnId,
            LocalDateTime.now(),
        )
        val message = OkonomiBestillingMelding.Faktura(faktura)
        storeOkonomiMelding(faktura.bestillingsnummer, message)
    }

    private fun QueryContext.storeOkonomiMelding(bestillingsnummer: String, message: OkonomiBestillingMelding) {
        log.info("Lagrer faktura for delutbeatling med bestillingsnummer=$bestillingsnummer for publisering på kafka")

        val record = StoredProducerRecord(
            config.bestillingTopic,
            bestillingsnummer.toByteArray(),
            Json.encodeToString(message).toByteArray(),
            null,
        )
        queries.kafkaProducerRecord.storeRecord(record)
    }

    private fun QueryContext.getOrError(id: UUID): Utbetaling {
        return queries.utbetaling.getOrError(id)
    }

    suspend fun getUtbetalingBeregning(utbetaling: Utbetaling, filter: BeregningFilter): UtbetalingBeregningDto = db.session {
        val norskIdentById = queries.deltaker
            .getAll(gjennomforingId = utbetaling.gjennomforing.id)
            .filter { it.id in utbetaling.beregning.output.deltakelser.map { it.deltakelseId } }
            .associate { it.id to it.norskIdent }

        val personer = personService.getPersoner(norskIdentById.values.mapNotNull { it })
        val regioner = buildRegionList(
            personer.mapNotNull { it.value.geografiskEnhet } + personer.mapNotNull { it.value.region },
        )

        val deltakelsePersoner = utbetaling.beregning.output.deltakelser.map {
            val norskIdent = norskIdentById.getValue(it.deltakelseId)
            val person = norskIdent?.let { personer.getValue(norskIdent) }
            it to person
        }.filter { (_, person) -> filter.navEnheter.isEmpty() || person?.geografiskEnhet?.enhetsnummer in filter.navEnheter }

        return UtbetalingBeregningDto.from(utbetaling, deltakelsePersoner, regioner)
    }
}
