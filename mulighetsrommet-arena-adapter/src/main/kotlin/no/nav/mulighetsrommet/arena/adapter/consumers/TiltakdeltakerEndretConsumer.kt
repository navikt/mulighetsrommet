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
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltakdeltaker
import no.nav.mulighetsrommet.domain.arena.ArenaTiltakdeltaker
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TiltakdeltakerEndretConsumer(
    override val consumerConfig: ConsumerConfig,
    override val events: EventRepository,
    private val client: MulighetsrommetApiClient
) : TopicConsumer<ArenaEvent<ArenaTiltakdeltaker>>() {

    override val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun decodeEvent(payload: JsonElement): ArenaEvent<ArenaTiltakdeltaker> =
        ArenaEventHelpers.decodeEvent(payload)

    override fun resolveKey(event: ArenaEvent<ArenaTiltakdeltaker>): String {
        return event.data.TILTAKDELTAKER_ID.toString()
    }

    override suspend fun handleEvent(event: ArenaEvent<ArenaTiltakdeltaker>) {
        val method = if (event.operation == ArenaOperation.Delete) HttpMethod.Delete else HttpMethod.Put
        client.sendRequest(method, "/api/v1/arena/deltaker", event.data.toAdapterTiltakdeltaker()) {
            status.isSuccess() || status == HttpStatusCode.Conflict
        }
    }

    private fun ArenaTiltakdeltaker.toAdapterTiltakdeltaker() = AdapterTiltakdeltaker(
        id = this.TILTAKDELTAKER_ID,
        tiltaksgjennomforingId = this.TILTAKGJENNOMFORING_ID,
        personId = this.PERSON_ID,
        fraDato = ProcessingUtils.getArenaDateFromTo(this.DATO_FRA),
        tilDato = ProcessingUtils.getArenaDateFromTo(this.DATO_TIL),
        status = ProcessingUtils.toDeltakerstatus(this.DELTAKERSTATUSKODE)
    )
}
