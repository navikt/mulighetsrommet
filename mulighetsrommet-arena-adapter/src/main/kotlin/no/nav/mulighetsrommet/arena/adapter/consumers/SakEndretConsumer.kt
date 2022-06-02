package no.nav.mulighetsrommet.arena.adapter.consumers

import io.ktor.http.*
import kotlinx.serialization.json.*
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.domain.arena.ArenaSak
import org.slf4j.LoggerFactory

class SakEndretConsumer(private val client: MulighetsrommetApiClient) {

    private val logger = LoggerFactory.getLogger(SakEndretConsumer::class.java)

    fun process(payload: JsonElement) {
        val sak = payload.jsonObject["after"]!!.jsonObject.toSak()

        if (sakIsRelatedToTiltaksgjennomforing(sak)) {
            client.sendRequest(HttpMethod.Put, "/api/arena/sak", sak)
            logger.debug("processed sak endret event")
        }
    }

    private fun sakIsRelatedToTiltaksgjennomforing(updatedSak: ArenaSak) =
        updatedSak.sakskode == "TILT"

    private fun JsonObject.toSak() = ArenaSak(
        sakId = this["SAK_ID"]!!.jsonPrimitive.content.toInt(),
        aar = this["AAR"]!!.jsonPrimitive.content.toInt(),
        lopenrsak = this["LOPENRSAK"]!!.jsonPrimitive.content.toInt(),
        sakskode = this["SAKSKODE"]!!.jsonPrimitive.content
    )
}
