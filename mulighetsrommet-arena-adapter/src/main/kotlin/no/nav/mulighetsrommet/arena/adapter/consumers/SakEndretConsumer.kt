package no.nav.mulighetsrommet.arena.adapter.consumers

import io.ktor.http.*
import kotlinx.serialization.json.*
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.utils.ProcessingUtils.isInsertArenaOperation
import no.nav.mulighetsrommet.domain.arena.ArenaSak
import org.slf4j.LoggerFactory

class SakEndretConsumer(private val client: MulighetsrommetApiClient) {

    private val logger = LoggerFactory.getLogger(SakEndretConsumer::class.java)
    private var resourceUri = "/api/arena/sak"

    fun process(payload: JsonElement) {
        if (isInsertArenaOperation(payload.jsonObject)) handleInsert(payload.jsonObject) else handleUpdate(payload.jsonObject)
    }

    private fun handleInsert(payload: JsonObject) {
        val newSak = payload["after"]!!.jsonObject.toSak()
        client.sendRequest(HttpMethod.Put, "$resourceUri/${newSak.sakId}", newSak)
        logger.debug("processed sak endret insert")
    }

    private fun handleUpdate(payload: JsonObject) {
        val updatedSak = payload["after"]!!.jsonObject.toSak()
        client.sendRequest(HttpMethod.Put, "$resourceUri/${updatedSak.sakId}", updatedSak)
        logger.debug("processed sak endret update")
    }

    private fun JsonObject.toSak() = ArenaSak(
        sakId = this["SAK_ID"]!!.jsonPrimitive.content.toInt(),
        aar = this["AAR"]!!.jsonPrimitive.content.toInt(),
        lopenrsak = this["LOPENRSAK"]!!.jsonPrimitive.content.toInt(),
        aetatenhetAnsvarlig = this["AETATENHET_ANSVARLIG"]!!.jsonPrimitive.content.toInt(),
    )
}
