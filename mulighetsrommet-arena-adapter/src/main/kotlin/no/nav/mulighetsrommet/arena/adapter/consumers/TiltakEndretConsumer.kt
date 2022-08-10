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
import no.nav.mulighetsrommet.arena.adapter.utils.ProcessingUtils
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltak
import no.nav.mulighetsrommet.domain.arena.ArenaTiltak
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TiltakEndretConsumer(
    override val consumerConfig: ConsumerConfig,
    override val events: EventRepository,
    private val client: MulighetsrommetApiClient
) : TopicConsumer<ArenaEvent<ArenaTiltak>>() {

    override val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun decodeEvent(payload: JsonElement): ArenaEvent<ArenaTiltak> = ArenaEventHelpers.decodeEvent(payload)

    override fun resolveKey(event: ArenaEvent<ArenaTiltak>): String {
        return event.data.TILTAKSKODE
    }

    override suspend fun handleEvent(event: ArenaEvent<ArenaTiltak>) {
        val method = if (event.operation == ArenaOperation.Delete) HttpMethod.Delete else HttpMethod.Put
        client.sendRequest(method, "/api/v1/arena/tiltakstype", event.data.toAdapterTiltak())
    }

    private fun ArenaTiltak.toAdapterTiltak() = AdapterTiltak(
        navn = this.TILTAKSNAVN,
        innsatsgruppe = ProcessingUtils.toInnsatsgruppe(this.TILTAKSKODE),
        tiltakskode = this.TILTAKSKODE,
        fraDato = ProcessingUtils.getArenaDateFromTo(this.DATO_FRA),
        tilDato = ProcessingUtils.getArenaDateFromTo(this.DATO_TIL)
    )
}
