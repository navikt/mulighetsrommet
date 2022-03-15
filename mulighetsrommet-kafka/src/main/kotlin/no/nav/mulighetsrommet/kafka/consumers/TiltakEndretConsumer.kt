package no.nav.mulighetsrommet.kafka.consumers

import kotlinx.serialization.json.*
import no.nav.mulighetsrommet.kafka.ProcessingUtils.isInsertArenaOperation
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory

object TiltakEndretConsumer {

    private val logger = LoggerFactory.getLogger(TiltakEndretConsumer::class.java)

    fun process(payload: JsonElement) {
        if (isInsertArenaOperation(payload.jsonObject)) handleInsert(payload.jsonObject) else handleUpdate(payload.jsonObject)
    }

    private fun handleInsert(payload: JsonObject) {
        val lol = payload["after"]!!.jsonObject["TILTAKSNAVN"]!!.jsonPrimitive.content
        logger.debug("insert: $lol")
    }

    private fun handleUpdate(payload: JsonObject) {
        val lol = payload["after"]!!.jsonObject["TILTAKSNAVN"]!!.jsonPrimitive.content
        logger.debug("update: $lol")
    }
}
