package no.nav.mulighetsrommet.api.tilsagn

import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.okonomi.*
import org.apache.kafka.clients.producer.ProducerRecord
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

    fun behandleGodkjentTilsagn(tilsagnId: UUID): Unit = db.session {
        val tilsagn = requireNotNull(queries.tilsagn.get(tilsagnId)) {
            "Tilsagn med id=$tilsagnId finnes ikke"
        }
        require(tilsagn.status == TilsagnStatus.GODKJENT) {
            "Tilsagn er ikke godkjent id=$tilsagnId status=${tilsagn.status}"
        }

        val opprettelse = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.OPPRETT)
        require(opprettelse.besluttetAv != null && opprettelse.besluttetTidspunkt != null) {
            "Tilsagn id=$tilsagnId må være besluttet godkjent for å sendes til økonomi"
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

        publish(bestilling.bestillingsnummer, OkonomiBestillingMelding.Bestilling(bestilling))
    }

    fun behandleAnnullertTilsagn(tilsagnId: UUID): Unit = db.session {
        val tilsagn = requireNotNull(queries.tilsagn.get(tilsagnId)) {
            "Tilsagn med id=$tilsagnId finnes ikke"
        }
        require(tilsagn.status == TilsagnStatus.ANNULLERT) {
            "Tilsagn er ikke annullert id=$tilsagnId status=${tilsagn.status}"
        }

        val annullering = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.ANNULLER)
        require(annullering.besluttetAv != null && annullering.besluttetTidspunkt != null) {
            "Tilsagn id=$tilsagnId må være besluttet annullert for å sendes som annullert til økonomi"
        }

        val annullerBestilling = AnnullerBestilling(
            bestillingsnummer = tilsagn.bestilling.bestillingsnummer,
            behandletAv = annullering.behandletAv.toOkonomiPart(),
            behandletTidspunkt = annullering.behandletTidspunkt,
            besluttetAv = annullering.besluttetAv.toOkonomiPart(),
            besluttetTidspunkt = annullering.besluttetTidspunkt,
        )

        publish(tilsagn.bestilling.bestillingsnummer, OkonomiBestillingMelding.Annullering(annullerBestilling))
    }

    fun behandleOppgjortTilsagn(tilsagnId: UUID): Unit = db.session {
        val tilsagn = requireNotNull(queries.tilsagn.get(tilsagnId)) {
            "Tilsagn med id=$tilsagnId finnes ikke"
        }
        require(tilsagn.status == TilsagnStatus.OPPGJORT) {
            "Tilsagn er ikke oppgjort id=$tilsagnId status=${tilsagn.status}"
        }

        val oppgjor = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.GJOR_OPP)
        require(oppgjor.besluttetAv != null && oppgjor.besluttetTidspunkt != null) {
            "Tilsagn id=$tilsagnId må være besluttet oppgjort for å sende null melding til økonomi"
        }

        val faktura = FrigjorBestilling(
            bestillingsnummer = tilsagn.bestilling.bestillingsnummer,
            behandletAv = oppgjor.behandletAv.toOkonomiPart(),
            behandletTidspunkt = oppgjor.behandletTidspunkt,
            besluttetAv = oppgjor.besluttetAv.toOkonomiPart(),
            besluttetTidspunkt = oppgjor.besluttetTidspunkt,
        )

        publish(tilsagn.bestilling.bestillingsnummer, OkonomiBestillingMelding.Frigjoring(faktura))
    }

    fun behandleGodkjentUtbetalinger(tilsagnId: UUID): Unit = db.transaction {
        val tilsagn = requireNotNull(queries.tilsagn.get(tilsagnId)) {
            "Tilsagn med id=$tilsagnId finnes ikke"
        }
        require(tilsagn.status in listOf(TilsagnStatus.GODKJENT, TilsagnStatus.OPPGJORT)) {
            "Tilsagn er ikke i riktig status id=$tilsagnId status=${tilsagn.status}"
        }

        queries.delutbetaling.getSkalSendesTilOkonomi(tilsagnId)
            .filter { it.status == DelutbetalingStatus.GODKJENT }
            .map {
                val opprettelse = queries.totrinnskontroll.getOrError(it.id, Totrinnskontroll.Type.OPPRETT)
                Pair(opprettelse, it)
            }
            .sortedBy { (opprettelse) -> opprettelse.besluttetTidspunkt }
            .forEach { (opprettelse, delutbetaling) ->
                log.info("Sender delutbetaling med utbetalingId: ${delutbetaling.utbetalingId} tilsagnId: ${delutbetaling.tilsagnId} på kafka")
                val utbetaling = requireNotNull(queries.utbetaling.get(delutbetaling.utbetalingId)) {
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

                queries.delutbetaling.setSendtTilOkonomi(
                    delutbetaling.utbetalingId,
                    delutbetaling.tilsagnId,
                    LocalDateTime.now(),
                )
                val message = OkonomiBestillingMelding.Faktura(faktura)
                publish(faktura.bestillingsnummer, message)
            }
    }

    private fun publish(bestillingsnummer: String, message: OkonomiBestillingMelding) {
        val record: ProducerRecord<ByteArray, ByteArray?> = ProducerRecord(
            config.topic,
            bestillingsnummer.toByteArray(),
            Json.encodeToString(message).toByteArray(),
        )
        db.session { queries.kafkaProducerRecord.insert(record) }
    }
}

fun Agent.toOkonomiPart(): OkonomiPart = when (this) {
    is NavIdent -> OkonomiPart.NavAnsatt(this)
    is Tiltaksadministrasjon -> OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON)
    Arrangor, Arena -> throw IllegalStateException("ugyldig agent")
}
