package no.nav.mulighetsrommet.arena.adapter.consumers

import io.ktor.http.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.domain.arena.ArenaSak
import org.slf4j.LoggerFactory

class SakEndretConsumer(
    override val topic: String,
    private val client: MulighetsrommetApiClient
) : TopicConsumer() {

    private val logger = LoggerFactory.getLogger(SakEndretConsumer::class.java)

    override fun shouldProcessEvent(payload: JsonElement): Boolean {
        return sakIsRelatedToTiltaksgjennomforing(payload)
    }

    override fun resolveKey(payload: JsonElement): String {
        return payload.jsonObject["after"]!!.jsonObject["SAK_ID"]!!.jsonPrimitive.content
    }

    override fun processEvent(payload: JsonElement) {
        val sak = payload.jsonObject["after"]!!.jsonObject.toSak()
        client.sendRequest(HttpMethod.Put, "/api/v1/arena/sak", sak)
        logger.debug("processed sak endret event")
    }

    private fun sakIsRelatedToTiltaksgjennomforing(payload: JsonElement): Boolean {
        val sakskode = payload.jsonObject["after"]!!.jsonObject["SAKSKODE"]!!.jsonPrimitive.content
        return sakskode == "TILT"
    }

    private fun JsonObject.toSak() = ArenaSak(
        sakId = this["SAK_ID"]!!.jsonPrimitive.content.toInt(),
        aar = this["AAR"]!!.jsonPrimitive.content.toInt(),
        lopenrsak = this["LOPENRSAK"]!!.jsonPrimitive.content.toInt(),
        sakskode = this["SAKSKODE"]!!.jsonPrimitive.content
    )
}
