package no.nav.mulighetsrommet.api.tilsagn

import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.okonomi.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class OkonomiBestillingService(
    private val config: Config,
    private val db: ApiDatabase,
) {
    private val log = LoggerFactory.getLogger(javaClass.simpleName)

    data class Config(
        val topic: String,
    )

    fun behandleGodkjentTilsagn(tilsagn: Tilsagn, ctx: QueryContext) {
        require(tilsagn.status == TilsagnStatus.GODKJENT) {
            "Tilsagn er ikke godkjent id=${tilsagn.id} status=${tilsagn.status}"
        }

        val opprettelse = ctx.queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.OPPRETT)
        require(opprettelse.besluttetAv != null && opprettelse.besluttetTidspunkt != null) {
            "Tilsagn id=${tilsagn.id} må være besluttet godkjent for å sendes til økonomi"
        }

        val gjennomforing = requireNotNull(ctx.queries.gjennomforing.get(tilsagn.gjennomforing.id)) {
            "Fant ikke gjennomforing for tilsagn"
        }

        val avtale = requireNotNull(gjennomforing.avtaleId?.let { ctx.queries.avtale.get(it) }) {
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
            kostnadssted = NavEnhetNummer(tilsagn.kostnadssted.enhetsnummer),
            // TODO: håndter avtalenummer fra p360, eller erstatter til Mercell
            avtalenummer = avtale.websaknummer?.value,
            belop = tilsagn.beregning.output.belop,
            periode = tilsagn.periode,
            behandletAv = opprettelse.behandletAv.toOkonomiPart(),
            behandletTidspunkt = opprettelse.behandletTidspunkt,
            besluttetAv = opprettelse.besluttetAv.toOkonomiPart(),
            besluttetTidspunkt = opprettelse.besluttetTidspunkt,
        )

        ctx.storeOkonomiMelding(bestilling.bestillingsnummer, OkonomiBestillingMelding.Bestilling(bestilling))
    }

    fun behandleAnnullertTilsagn(tilsagn: Tilsagn, ctx: QueryContext) {
        val annullering = ctx.queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.ANNULLER)
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

        ctx.storeOkonomiMelding(tilsagn.bestilling.bestillingsnummer, OkonomiBestillingMelding.Annullering(annullerBestilling))
    }

    fun behandleOppgjortTilsagn(tilsagn: Tilsagn, ctx: QueryContext): Unit = db.session {
        require(tilsagn.status == TilsagnStatus.OPPGJORT) {
            "Tilsagn er ikke oppgjort id=${tilsagn.id} status=${tilsagn.status}"
        }

        val oppgjor = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.GJOR_OPP)
        require(oppgjor.besluttetAv != null && oppgjor.besluttetTidspunkt != null) {
            "Tilsagn id=${tilsagn.id} må være besluttet oppgjort for å sende null melding til økonomi"
        }

        val faktura = FrigjorBestilling(
            bestillingsnummer = tilsagn.bestilling.bestillingsnummer,
            behandletAv = oppgjor.behandletAv.toOkonomiPart(),
            behandletTidspunkt = oppgjor.behandletTidspunkt,
            besluttetAv = oppgjor.besluttetAv.toOkonomiPart(),
            besluttetTidspunkt = oppgjor.besluttetTidspunkt,
        )

        ctx.storeOkonomiMelding(tilsagn.bestilling.bestillingsnummer, OkonomiBestillingMelding.Frigjoring(faktura))
    }

    fun behandleGodkjentUtbetalinger(tilsagnId: UUID, ctx: QueryContext) {
        val tilsagn = requireNotNull(ctx.queries.tilsagn.get(tilsagnId)) {
            "Tilsagn med id=$tilsagnId finnes ikke"
        }
        require(tilsagn.status in listOf(TilsagnStatus.GODKJENT, TilsagnStatus.OPPGJORT)) {
            "Tilsagn er ikke i riktig status id=$tilsagnId status=${tilsagn.status}"
        }

        ctx.queries.delutbetaling.getSkalSendesTilOkonomi(tilsagnId)
            .filter { it.status == DelutbetalingStatus.GODKJENT }
            .map {
                val opprettelse = ctx.queries.totrinnskontroll.getOrError(it.id, Totrinnskontroll.Type.OPPRETT)
                Pair(opprettelse, it)
            }
            .sortedBy { (opprettelse) -> opprettelse.besluttetTidspunkt }
            .forEach { (opprettelse, delutbetaling) ->
                log.info("Sender delutbetaling med utbetalingId: ${delutbetaling.utbetalingId} tilsagnId: ${delutbetaling.tilsagnId} på kafka")
                val utbetaling = requireNotNull(ctx.queries.utbetaling.get(delutbetaling.utbetalingId)) {
                    "Utbetaling med id=${delutbetaling.utbetalingId} finnes ikke"
                }
                val kontonummer = requireNotNull(utbetaling.betalingsinformasjon.kontonummer) {
                    "Kontonummer mangler for utbetaling med id=${utbetaling.id}"
                }
                requireNotNull(opprettelse.besluttetTidspunkt)
                requireNotNull(opprettelse.besluttetAv)

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
                    frigjorBestilling = delutbetaling.gjorOppTilsagn,
                )

                ctx.queries.delutbetaling.setSendtTilOkonomi(
                    delutbetaling.utbetalingId,
                    delutbetaling.tilsagnId,
                    LocalDateTime.now(),
                )
                val message = OkonomiBestillingMelding.Faktura(faktura)
                ctx.storeOkonomiMelding(faktura.bestillingsnummer, message)
            }
    }

    private fun QueryContext.storeOkonomiMelding(bestillingsnummer: String, message: OkonomiBestillingMelding) {
        val record = StoredProducerRecord(
            config.topic,
            bestillingsnummer.toByteArray(),
            Json.encodeToString(message).toByteArray(),
            null,
        )
        queries.kafkaProducerRecord.storeRecord(record)
    }
}

fun Agent.toOkonomiPart(): OkonomiPart = when (this) {
    is NavIdent -> OkonomiPart.NavAnsatt(this)
    is Tiltaksadministrasjon -> OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON)
    Arrangor, Arena -> throw IllegalStateException("ugyldig agent")
}
