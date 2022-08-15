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
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltaksgjennomforing
import no.nav.mulighetsrommet.domain.arena.ArenaTiltaksgjennomforing
import no.nav.mulighetsrommet.domain.arena.JaNeiStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TiltakgjennomforingEndretConsumer(
    override val consumerConfig: ConsumerConfig,
    override val events: EventRepository,
    private val client: MulighetsrommetApiClient
) : TopicConsumer<ArenaEvent<ArenaTiltaksgjennomforing>>() {

    override val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun decodeEvent(payload: JsonElement): ArenaEvent<ArenaTiltaksgjennomforing> =
        ArenaEventHelpers.decodeEvent(payload)

    override fun resolveKey(event: ArenaEvent<ArenaTiltaksgjennomforing>): String {
        return event.data.TILTAKGJENNOMFORING_ID.toString()
    }

    override suspend fun handleEvent(event: ArenaEvent<ArenaTiltaksgjennomforing>) {
        val method = if (event.operation == ArenaOperation.Delete) HttpMethod.Delete else HttpMethod.Put
        client.sendRequest(method, "/api/v1/arena/tiltaksgjennomforing", event.data.toAdapterTiltaksgjennomforing())
    }

    private fun ArenaTiltaksgjennomforing.toAdapterTiltaksgjennomforing() = AdapterTiltaksgjennomforing(
        id = this.TILTAKGJENNOMFORING_ID,
        navn = this.LOKALTNAVN,
        tiltakskode = this.TILTAKSKODE,
        fraDato = ProcessingUtils.getArenaDateFromTo(this.DATO_FRA),
        tilDato = ProcessingUtils.getArenaDateFromTo(this.DATO_TIL),
        arrangorId = this.ARBGIV_ID_ARRANGOR,
        sakId = this.SAK_ID,
        apentForInnsok = this.STATUS_TREVERDIKODE_INNSOKNING != JaNeiStatus.Nei,
        antallPlasser = this.ANTALL_DELTAKERE,
    )
}
