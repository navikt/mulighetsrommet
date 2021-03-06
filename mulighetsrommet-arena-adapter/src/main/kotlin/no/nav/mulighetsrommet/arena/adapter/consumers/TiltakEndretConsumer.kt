package no.nav.mulighetsrommet.arena.adapter.consumers

import io.ktor.http.*
import kotlinx.serialization.json.JsonElement
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.consumers.helpers.ArenaEventHelpers
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
) : TopicConsumer<ArenaTiltak>() {

    override val logger: Logger = LoggerFactory.getLogger(TiltakEndretConsumer::class.java)

    override fun toDomain(payload: JsonElement): ArenaTiltak = ArenaEventHelpers.decodeAfter(payload)

    override fun resolveKey(payload: ArenaTiltak): String {
        return payload.TILTAKSKODE
    }

    override suspend fun handleEvent(payload: ArenaTiltak) {
        client.sendRequest(HttpMethod.Put, "/api/v1/arena/tiltakstyper", payload.toAdapterTiltak())
        logger.debug("processed tiltak endret event")
    }

    private fun ArenaTiltak.toAdapterTiltak() = AdapterTiltak(
        navn = this.TILTAKSNAVN,
        innsatsgruppe = ProcessingUtils.toInnsatsgruppe(this.TILTAKSKODE),
        tiltakskode = this.TILTAKSKODE,
        fraDato = ProcessingUtils.getArenaDateFromTo(this.DATO_FRA),
        tilDato = ProcessingUtils.getArenaDateFromTo(this.DATO_TIL)
    )
}
