package no.nav.mulighetsrommet.arena.adapter.consumers

import io.ktor.http.*
import kotlinx.serialization.json.*
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.utils.ProcessingUtils
import no.nav.mulighetsrommet.arena.adapter.utils.ProcessingUtils.isInsertArenaOperation
import no.nav.mulighetsrommet.domain.Tiltaksgjennomforing
import no.nav.mulighetsrommet.domain.Tiltakskode
import org.slf4j.LoggerFactory

class SakEndretConsumer(private val client: MulighetsrommetApiClient) {

    private val logger = LoggerFactory.getLogger(SakEndretConsumer::class.java)
    private var resourceUri = "/api/arena/tiltaksgjennomforinger"

    fun process(payload: JsonElement) {
        if (isInsertArenaOperation(payload.jsonObject)) handleInsert(payload.jsonObject) else handleUpdate(payload.jsonObject)
    }

    private fun handleInsert(payload: JsonObject) {
        val newSak = payload["after"]!!.jsonObject.toSak()
        logger.debug("${newTiltaksgjennomforing.sakId}")
        client.sendRequest(HttpMethod.Post, resourceUri, newTiltaksgjennomforing)
        logger.debug("processed tiltakgjennomforing endret insert")
    }

    private fun handleUpdate(payload: JsonObject) {
        val updatedSak = payload["after"]!!.jsonObject.toSak()
        logger.debug("ARENA ID: ${updateTiltaksgjennomforing.arenaId}")
        client.sendRequest(HttpMethod.Put, "$resourceUri/${updateTiltaksgjennomforing.arenaId}", updateTiltaksgjennomforing)
        logger.debug("processed tiltakgjennomforing endret update")
    }

    private fun JsonObject.toSak() = ArenaSak(
        aar = this["AAR"]!!.jsonPrimitive.content.toInt(),
        tiltaksnummer = this["LOPENRSAK"]!!.jsonPrimitive.content.toInt(),
        enhet = this["DATO_FRA"]!!.jsonPrimitive.content.toInt(),
    )

    data class ArenaSak(
        val aar: Int,
        val tiltaksnummer: Int,
        val enhet: Int
    )
}
