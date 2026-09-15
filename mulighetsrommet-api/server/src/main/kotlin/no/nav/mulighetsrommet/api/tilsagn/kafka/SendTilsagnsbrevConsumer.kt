package no.nav.mulighetsrommet.api.tilsagn.kafka

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollHendelse
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollHendelseOld
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.task.SendTilsagnsbrevSaga
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Skedulerer utsending av tilsagnsbrev når et tilsagn for en enkeltplass-gjennomføring blir godkjent.
 */
class SendTilsagnsbrevConsumer(
    private val db: ApiDatabase,
    private val sendTilsagnsbrevSaga: SendTilsagnsbrevSaga,
) : KafkaTopicConsumer<UUID, JsonElement>(
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: UUID, message: JsonElement) {
        val totrinnskontrollHendelse = try {
            JsonIgnoreUnknownKeys.decodeFromJsonElement<TotrinnskontrollHendelse?>(message)
        } catch (_: Throwable) {
            JsonIgnoreUnknownKeys.decodeFromJsonElement<TotrinnskontrollHendelseOld>(message).toNew()
        }
        if (totrinnskontrollHendelse == null) {
            logger.warn("Mottok tombstone for totrinnskontroll med key=$key")
            return
        }

        if (totrinnskontrollHendelse.type != TotrinnskontrollType.TILSAGN_OPPRETTELSE) {
            return
        }
        if (totrinnskontrollHendelse.status != TotrinnskontrollHendelse.Status.GODKJENT) {
            return
        }

        val tilsagnId = totrinnskontrollHendelse.entityId

        val tilsagn = db.session { queries.tilsagn.get(tilsagnId) }
        if (tilsagn == null) {
            logger.warn("Fant ikke tilsagn med id=$tilsagnId for totrinnskontroll hendelse")
            return
        }

        if (tilsagn.status != TilsagnStatus.GODKJENT) {
            logger.info("Tilsagn med id=$tilsagnId er ikke lenger godkjent (status=${tilsagn.status})")
            return
        }

        val gjennomforing = db.session { queries.gjennomforing.getGjennomforingOrError(tilsagn.gjennomforing.id) }
        if (gjennomforing !is GjennomforingEnkeltplass) {
            return
        }

        sendTilsagnsbrevSaga.schedule(tilsagnId)
    }
}
