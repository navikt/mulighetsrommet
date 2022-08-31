package no.nav.mulighetsrommet.arena.adapter.consumers

import io.ktor.http.*
import kotlinx.serialization.json.JsonElement
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.consumers.helpers.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.consumers.helpers.ArenaEventHelpers
import no.nav.mulighetsrommet.arena.adapter.consumers.helpers.ArenaOperation
import no.nav.mulighetsrommet.arena.adapter.kafka.TopicConsumer
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository
import no.nav.mulighetsrommet.domain.adapter.AdapterSak
import no.nav.mulighetsrommet.domain.arena.ArenaSak
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SakEndretConsumer(
    override val consumerConfig: ConsumerConfig,
    override val events: EventRepository,
    private val client: MulighetsrommetApiClient
) : TopicConsumer<ArenaEvent<ArenaSak>>() {

    override val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun decodeEvent(payload: JsonElement): ArenaEvent<ArenaSak> = ArenaEventHelpers.decodeEvent(payload)

    override fun shouldProcessEvent(event: ArenaEvent<ArenaSak>): Boolean {
        return sakIsRelatedToTiltaksgjennomforing(event.data)
    }

    override fun resolveKey(event: ArenaEvent<ArenaSak>): String {
        return event.data.SAK_ID.toString()
    }

    override suspend fun handleEvent(event: ArenaEvent<ArenaSak>) {
        val method = if (event.operation == ArenaOperation.Delete) HttpMethod.Delete else HttpMethod.Put
        client.sendRequest(method, "/api/v1/arena/sak", event.data.toAdapterSak()) {
            status.isSuccess() || status == HttpStatusCode.Conflict
        }
    }

    private fun sakIsRelatedToTiltaksgjennomforing(payload: ArenaSak): Boolean = payload.SAKSKODE == "TILT"

    private fun ArenaSak.toAdapterSak() = AdapterSak(
        id = this.SAK_ID,
        aar = this.AAR,
        lopenummer = this.LOPENRSAK,
    )
}
