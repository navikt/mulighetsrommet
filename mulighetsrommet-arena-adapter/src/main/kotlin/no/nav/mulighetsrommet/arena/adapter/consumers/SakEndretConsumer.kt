package no.nav.mulighetsrommet.arena.adapter.consumers

import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import no.nav.mulighetsrommet.arena.adapter.Database
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.domain.adapter.AdapterSak
import no.nav.mulighetsrommet.domain.arena.ArenaSak
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SakEndretConsumer(
    db: Database,
    override val topic: String,
    private val client: MulighetsrommetApiClient
) : TopicConsumer<ArenaSak>(db) {

    override val logger: Logger = LoggerFactory.getLogger(SakEndretConsumer::class.java)

    override fun toDomain(payload: JsonElement): ArenaSak {
        return Json.decodeFromJsonElement(payload.jsonObject["after"]!!)
    }

    override fun shouldProcessEvent(payload: ArenaSak): Boolean {
        return sakIsRelatedToTiltaksgjennomforing(payload)
    }

    override fun resolveKey(payload: ArenaSak): String {
        return payload.SAK_ID.toString()
    }

    override fun handleEvent(payload: ArenaSak) {
        client.sendRequest(HttpMethod.Put, "/api/v1/arena/sak", payload.toAdapterSak())
        logger.debug("processed sak endret event")
    }

    private fun sakIsRelatedToTiltaksgjennomforing(payload: ArenaSak): Boolean = payload.SAKSKODE == "TILT"

    private fun ArenaSak.toAdapterSak() = AdapterSak(
        id = this.SAK_ID,
        aar = this.AAR,
        lopenummer = this.LOPENRSAK,
    )
}
