package no.nav.mulighetsrommet.arena.adapter.consumers

import io.ktor.http.*
import kotlinx.serialization.json.JsonElement
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.consumers.helpers.ArenaEventHelpers
import no.nav.mulighetsrommet.arena.adapter.kafka.TopicConsumer
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository
import no.nav.mulighetsrommet.arena.adapter.utils.ProcessingUtils
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltakdeltaker
import no.nav.mulighetsrommet.domain.arena.ArenaTiltakdeltaker
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TiltakdeltakerEndretConsumer(
    override val topic: String,
    override val events: EventRepository,
    private val client: MulighetsrommetApiClient
) : TopicConsumer<ArenaTiltakdeltaker>() {

    override val logger: Logger = LoggerFactory.getLogger(TiltakdeltakerEndretConsumer::class.java)

    override fun toDomain(payload: JsonElement): ArenaTiltakdeltaker = ArenaEventHelpers.decodeAfter(payload)

    override fun resolveKey(payload: ArenaTiltakdeltaker): String {
        return payload.TILTAKDELTAKER_ID.toString()
    }

    override fun handleEvent(payload: ArenaTiltakdeltaker) {
        client.sendRequest(HttpMethod.Put, "/api/v1/arena/deltakere", payload.toAdapterTiltakdeltaker())
        logger.debug("processed tiltak endret event")
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
