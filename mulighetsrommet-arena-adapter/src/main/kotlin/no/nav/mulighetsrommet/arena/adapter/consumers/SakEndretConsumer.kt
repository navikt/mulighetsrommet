package no.nav.mulighetsrommet.arena.adapter.consumers

import io.ktor.http.*
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.domain.adapter.AdapterSak
import no.nav.mulighetsrommet.domain.arena.ArenaSak
import org.slf4j.LoggerFactory

class SakEndretConsumer(
    override val topic: String,
    private val client: MulighetsrommetApiClient
) : TopicConsumer<ArenaSak>() {

    private val logger = LoggerFactory.getLogger(SakEndretConsumer::class.java)

    override fun shouldProcessEvent(payload: ArenaSak): Boolean {
        return sakIsRelatedToTiltaksgjennomforing(payload)
    }

    override fun resolveKey(payload: ArenaSak): String {
        return payload.SAK_ID.toString()
    }

    override fun processEvent(payload: ArenaSak) {
        client.sendRequest(HttpMethod.Put, "/api/v1/arena/sak", payload.toSak())
        logger.debug("processed sak endret event")
    }

    private fun sakIsRelatedToTiltaksgjennomforing(payload: ArenaSak): Boolean = payload.SAKSTATUSKODE == "TILT"

    private fun ArenaSak.toSak() = AdapterSak(
        sakId = this.SAK_ID,
        aar = this.AAR,
        lopenrsak = this.LOPENRSAK,
        sakskode = this.SAKSKODE
    )
}
