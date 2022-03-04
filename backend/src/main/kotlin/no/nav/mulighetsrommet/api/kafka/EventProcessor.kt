package no.nav.mulighetsrommet.api.kafka

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import no.nav.mulighetsrommet.api.domain.Tiltakskode
import no.nav.mulighetsrommet.api.domain.Tiltakstype
import no.nav.mulighetsrommet.api.kafka.ProcessingUtils.getArenaDateFormat
import no.nav.mulighetsrommet.api.kafka.ProcessingUtils.getArenaOperationType
import no.nav.mulighetsrommet.api.services.TiltakstypeService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class EventProcessor(private val topicMap: Map<String, String>, private val tiltakstypeService: TiltakstypeService) {

    private val logger = LoggerFactory.getLogger(EventProcessor::class.java)

    suspend fun process(record: ConsumerRecord<String, String>) {
        val arenaJson = Json.parseToJsonElement(record.value()).jsonObject
        when (record.topic()) {
            topicMap["tiltakendret"] -> handleTiltakstype(arenaJson)
            else -> logger.info("Klarte ikke å mappe topic. Ukjent topic: ${record.topic()}")
        }
    }

    private suspend fun handleTiltakstype(json: JsonObject) {
        val operationType = getArenaOperationType(json)
        if (operationType == ArenaEventOperationType.INSERT) {
            insertTiltakstype(json)
        } else {
            updateTiltakstype(json)
        }
    }

    /**
     * Gjør om string value fra arena event til Json. Henter så ut "after" JsonObject.
     * Mapper dette til tiltakstype DTO og kaller tiltakstypeService for opprettelse.
     */
    private suspend fun insertTiltakstype(json: JsonObject) {
        logger.debug("Processing event from topic: ${topicMap["tiltakendret"]} (insert)")
        val tiltakstype = toTiltakstypeFromArenaJson(json["after"]!!.jsonObject)
        tiltakstypeService.createTiltakstype(tiltakstype)
    }

    private suspend fun updateTiltakstype(json: JsonObject) {
        logger.debug("Processing event from topic: ${topicMap["tiltakendret"]} (update)")
        val tiltakstype = toTiltakstypeFromArenaJson(json["after"]!!.jsonObject)
        tiltakstypeService.updateTiltakstype(tiltakstype)
    }

    private fun toTiltakstypeFromArenaJson(json: JsonObject): Tiltakstype {
        return Tiltakstype(
            navn = json["TILTAKSNAVN"]!!.jsonPrimitive.content,
            innsatsgruppe = 1,
            tiltakskode = Tiltakskode.valueOf(
                json["TILTAKSKODE"]!!.jsonPrimitive.content
            ),
            fraDato = LocalDateTime.parse(
                json["DATO_FRA"]!!.jsonPrimitive.content,
                getArenaDateFormat()
            ),
            tilDato = LocalDateTime.parse(
                json["DATO_TIL"]!!.jsonPrimitive.content,
                getArenaDateFormat()
            ),
            createdBy = json["REG_USER"]!!.jsonPrimitive.content,
            updatedBy = json["MOD_USER"]!!.jsonPrimitive.content
        )
    }
}
