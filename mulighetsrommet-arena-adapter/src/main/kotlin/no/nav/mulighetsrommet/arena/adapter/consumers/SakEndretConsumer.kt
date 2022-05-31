package no.nav.mulighetsrommet.arena.adapter.consumers

import io.ktor.http.*
import kotlinx.serialization.json.*
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.domain.arena.ArenaSak
import org.slf4j.LoggerFactory

class SakEndretConsumer(private val client: MulighetsrommetApiClient) {

    private val logger = LoggerFactory.getLogger(SakEndretConsumer::class.java)

    fun process(payload: JsonElement) {
        val updatedSak = payload.jsonObject["after"]!!.jsonObject.toSak()

        if (updatedSak.sakskode == "TILT") {
            client.sendRequest(HttpMethod.Put, "/api/arena/sak/${updatedSak.sakId}", updatedSak)
            logger.debug("processed sak endret event")
        }
    }

    private fun JsonObject.toSak() = ArenaSak(
        sakId = this["SAK_ID"]!!.jsonPrimitive.content.toInt(),
        aar = this["AAR"]!!.jsonPrimitive.content.toInt(),
        lopenrsak = this["LOPENRSAK"]!!.jsonPrimitive.content.toInt(),
        sakskode = this["SAKSKODE"]!!.jsonPrimitive.content
    )
}
