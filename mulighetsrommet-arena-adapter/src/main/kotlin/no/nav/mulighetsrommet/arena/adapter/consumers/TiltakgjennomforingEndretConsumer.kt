package no.nav.mulighetsrommet.arena.adapter.consumers

import io.ktor.http.*
import kotlinx.serialization.json.JsonElement
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.consumers.helpers.ArenaEventHelpers
import no.nav.mulighetsrommet.arena.adapter.kafka.TopicConsumer
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository
import no.nav.mulighetsrommet.arena.adapter.utils.ProcessingUtils
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltaksgjennomforing
import no.nav.mulighetsrommet.domain.arena.ArenaTiltaksgjennomforing
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TiltakgjennomforingEndretConsumer(
    override val topic: String,
    override val events: EventRepository,
    private val client: MulighetsrommetApiClient
) : TopicConsumer<ArenaTiltaksgjennomforing>() {

    override val logger: Logger = LoggerFactory.getLogger(TiltakgjennomforingEndretConsumer::class.java)

    override fun toDomain(payload: JsonElement): ArenaTiltaksgjennomforing = ArenaEventHelpers.decodeAfter(payload)

    override fun resolveKey(payload: ArenaTiltaksgjennomforing): String {
        return payload.TILTAKGJENNOMFORING_ID.toString()
    }

    override fun handleEvent(payload: ArenaTiltaksgjennomforing) {
        client.sendRequest(
            HttpMethod.Put,
            "/api/v1/arena/tiltaksgjennomforinger",
            payload.toAdapterTiltaksgjennomforing()
        )
        logger.debug("processed tiltakgjennomforing endret event")
    }

    private fun ArenaTiltaksgjennomforing.toAdapterTiltaksgjennomforing() = AdapterTiltaksgjennomforing(
        id = this.TILTAKGJENNOMFORING_ID,
        navn = this.LOKALTNAVN,
        tiltakskode = this.TILTAKSKODE,
        fraDato = ProcessingUtils.getArenaDateFromTo(this.DATO_FRA),
        tilDato = ProcessingUtils.getArenaDateFromTo(this.DATO_TIL),
        arrangorId = this.ARBGIV_ID_ARRANGOR,
        sakId = this.SAK_ID,
    )
}
