package no.nav.mulighetsrommet.arena.adapter.consumers

import io.ktor.http.*
import kotlinx.serialization.json.JsonElement
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.consumers.helpers.ArenaEventHelpers
import no.nav.mulighetsrommet.arena.adapter.kafka.TopicConsumer
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository
import no.nav.mulighetsrommet.domain.adapter.AdapterSak
import no.nav.mulighetsrommet.domain.arena.ArenaSak
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SakEndretConsumer(
    override val topic: String,
    override val events: EventRepository,
    private val client: MulighetsrommetApiClient
) : TopicConsumer<ArenaSak>() {

    override val logger: Logger = LoggerFactory.getLogger(SakEndretConsumer::class.java)

    override fun toDomain(payload: JsonElement): ArenaSak = ArenaEventHelpers.decodeAfter(payload)

    override fun shouldProcessEvent(payload: ArenaSak): Boolean {
        return sakIsRelatedToTiltaksgjennomforing(payload)
    }

    override fun resolveKey(payload: ArenaSak): String {
        return payload.SAK_ID.toString()
    }

    override suspend fun handleEvent(payload: ArenaSak) {
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
