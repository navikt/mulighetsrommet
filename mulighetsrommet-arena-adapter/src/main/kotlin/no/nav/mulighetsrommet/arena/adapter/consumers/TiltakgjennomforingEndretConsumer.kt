package no.nav.mulighetsrommet.arena.adapter.consumers

import io.ktor.http.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.utils.ProcessingUtils
import no.nav.mulighetsrommet.domain.Tiltaksgjennomforing
import no.nav.mulighetsrommet.domain.arena.ArenaTiltaksgjennomforing
import org.slf4j.LoggerFactory

class TiltakgjennomforingEndretConsumer(
    override val topic: String,
    private val client: MulighetsrommetApiClient
) : TopicConsumer<ArenaTiltaksgjennomforing>() {

    private val logger = LoggerFactory.getLogger(TiltakgjennomforingEndretConsumer::class.java)

    override fun resolveKey(payload: ArenaTiltaksgjennomforing): String {
        return payload.TILTAKGJENNOMFORING_ID.toString()
    }

    override fun processEvent(payload: ArenaTiltaksgjennomforing) {
        client.sendRequest(HttpMethod.Put, "/api/v1/arena/tiltaksgjennomforinger", payload.toTiltaksgjennomforing())
        logger.debug("processed tiltakgjennomforing endret event")
    }

    private fun ArenaTiltaksgjennomforing.toTiltaksgjennomforing() = Tiltaksgjennomforing(
        navn = this.LOKALTNAVN,
        tiltakskode = this.TILTAKSKODE,
        fraDato = ProcessingUtils.getArenaDateFromTo(this.DATO_FRA),
        tilDato = ProcessingUtils.getArenaDateFromTo(this.DATO_TIL),
        arrangorId = this.ARBGIV_ID_ARRANGOR,
        arenaId = this.TILTAKGJENNOMFORING_ID,
        tiltaksnummer = 0,
        sakId = this.SAK_ID
    )
}
