package no.nav.mulighetsrommet.api.tilskuddbehandling.kafka

import arrow.core.flatMap
import arrow.core.getOrElse
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningType
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnInputLinjeRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddMottaker
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollBesluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollHendelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollType
import no.nav.mulighetsrommet.api.utbetaling.model.AutomatisertUtbetalingResult
import no.nav.mulighetsrommet.api.utbetaling.model.UpsertUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.service.UtbetalingService
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.tiltak.okonomi.Tilskuddstype
import org.slf4j.LoggerFactory
import java.util.UUID

class TilskuddArrangorUtbetalingConsumer(
    private val db: ApiDatabase,
    private val utbetalingService: UtbetalingService,
    private val tilsagnService: TilsagnService,
) : KafkaTopicConsumer<UUID, JsonElement>(
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: UUID, message: JsonElement) {
        val totrinnskontrollHendelse = JsonIgnoreUnknownKeys.decodeFromJsonElement<TotrinnskontrollHendelse?>(message)
        if (totrinnskontrollHendelse == null) {
            logger.warn("Mottok tombstone for totrinnskontroll med key=$key")
            return
        }

        if (totrinnskontrollHendelse.type != TotrinnskontrollType.TILSKUDD_OPPRETTELSE) {
            return
        }
        if (totrinnskontrollHendelse.besluttelse != TotrinnskontrollBesluttelse.GODKJENT) {
            return
        }

        val behandling = db.session { queries.tilskuddBehandling.get(key) }
            ?: throw IllegalStateException("Fant ikke attestert tilskudd_behandling id=$key")

        utbetalTilskuddTilArrangor(behandling)
    }

    private suspend fun utbetalTilskuddTilArrangor(behandling: TilskuddBehandlingDto) {
        behandling.tilskudd
            .filter { it.vedtakResultat.type == VedtakResultat.INNVILGELSE }
            .filter { it.utbetalingMottaker == TilskuddMottaker.ARRANGOR }
            // Idempotency check
            .filter { db.session { queries.utbetaling.getByTilskudd(it.id) } == null }
            .forEach { t ->
                db.transaction {
                    opprettOgGodkjennTilsagn(
                        gjennomforingId = behandling.gjennomforingId,
                        kostnadssted = behandling.kostnadssted.enhetsnummer,
                        periode = behandling.periode,
                        belop = requireNotNull(t.utbetalingBelop) {
                            "Utbetaling beløp var null ved inngivelse av tilskudd til arrangør"
                        },
                        prisbetingelser = null,
                    )
                    val utbetaling = opprettOgBetalUtbetaling(
                        gjennomforingId = behandling.gjennomforingId,
                        periode = behandling.periode,
                        belop = t.utbetalingBelop,
                        kid = t.kid,
                    )
                    queries.tilskuddBehandling.setUtbetaling(t.id, utbetaling.id)
                }
            }
    }

    private fun TransactionalQueryContext.opprettOgGodkjennTilsagn(
        gjennomforingId: UUID,
        kostnadssted: NavEnhetNummer,
        periode: Periode,
        belop: ValutaBelop,
        prisbetingelser: String?,
    ): Tilsagn {
        return tilsagnService.upsertInTx(
            TilsagnRequest(
                id = UUID.randomUUID(),
                type = TilsagnType.TILSAGN,
                gjennomforingId = gjennomforingId,
                kostnadssted = kostnadssted,
                beregning = TilsagnBeregningRequest(
                    type = TilsagnBeregningType.FRI,
                    valuta = belop.valuta,
                    prisbetingelser = prisbetingelser,
                    linjer = listOf(
                        TilsagnInputLinjeRequest(
                            id = UUID.randomUUID(),
                            beskrivelse = "Automatisk tilsagn for opplæringstilskudd",
                            pris = belop,
                            antall = 1,
                        ),
                    ),
                ),
                kommentar = null,
                beskrivelse = null,
                periodeStart = periode.start.toString(),
                periodeSlutt = periode.getLastInclusiveDate().toString(),
                deltakere = emptyList(),
            ),
            Tiltaksadministrasjon,
        )
            .flatMap {
                tilsagnService.godkjennTilsagnInTx(it.id, Tiltaksadministrasjon)
            }
            .getOrElse {
                throw IllegalStateException("Feil under opprettelse av tilsagn for tilskudd. Errors: $it")
            }
    }

    private suspend fun TransactionalQueryContext.opprettOgBetalUtbetaling(
        gjennomforingId: UUID,
        periode: Periode,
        belop: ValutaBelop,
        kid: Kid?,
    ): Utbetaling {
        return utbetalingService.opprettUtbetaling(
            UpsertUtbetaling.Generering(
                id = UUID.randomUUID(),
                periode = periode,
                gjennomforingId = gjennomforingId,
                beregning = UtbetalingBeregningFri.from(belop),
                tilskuddstype = Tilskuddstype.TILTAK_OPPLAERING_TILSKUDD,
                kid = kid,
                blokkeringer = emptySet(),
            ),
            Tiltaksadministrasjon,
        )
            .map {
                when (val result = utbetalingService.automatisertUtbetalingVedEttRelevantTilsagn(it.id)) {
                    AutomatisertUtbetalingResult.GODKJENT -> Unit
                    else -> throw IllegalStateException("Feil ved automatisk utbetaling av tilskudd til arrangør. Errors: $result")
                }
                it
            }
            .getOrElse {
                throw IllegalStateException("Feil ved automatisk utbetaling av tilskudd til arrangør. Errors: $it")
            }
    }
}
