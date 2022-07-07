package no.nav.mulighetsrommet.arena.adapter.consumers

import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.utils.ProcessingUtils
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltaksgjennomforing
import no.nav.mulighetsrommet.domain.arena.ArenaTiltaksgjennomforing
import org.slf4j.LoggerFactory

class TiltakgjennomforingEndretConsumer(
    override val topic: String,
    private val client: MulighetsrommetApiClient
) : TopicConsumer<ArenaTiltaksgjennomforing, ArenaTiltaksgjennomforing>() {

    private val logger = LoggerFactory.getLogger(TiltakgjennomforingEndretConsumer::class.java)

    override fun toDomain(payload: String): ArenaTiltaksgjennomforing {
        return Json.decodeFromJsonElement(Json.parseToJsonElement(payload).jsonObject["after"]!!)
    }

    override fun resolveKey(payload: ArenaTiltaksgjennomforing): String {
        return payload.TILTAKGJENNOMFORING_ID.toString()
    }

    override fun processEvent(payload: ArenaTiltaksgjennomforing) {
        client.sendRequest(HttpMethod.Put, "/api/v1/arena/tiltaksgjennomforinger", payload.toAdapterTiltaksgjennomforing())
        logger.debug("processed tiltakgjennomforing endret event")
    }

    private fun ArenaTiltaksgjennomforing.toAdapterTiltaksgjennomforing() = AdapterTiltaksgjennomforing(
        id = this.TILTAKGJENNOMFORING_ID,
        navn = this.LOKALTNAVN,
        tiltakskode = this.TILTAKSKODE,
        fraDato = ProcessingUtils.getArenaDateFromTo(this.DATO_FRA),
        tilDato = ProcessingUtils.getArenaDateFromTo(this.DATO_TIL),
        arrangorId = this.ARBGIV_ID_ARRANGOR,
        tiltaksnummer = 0,
        sakId = this.SAK_ID,
    )

}
