package no.nav.mulighetsrommet.arena.adapter.consumers

import io.ktor.http.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.utils.ProcessingUtils
import no.nav.mulighetsrommet.domain.Tiltaksgjennomforing
import org.slf4j.LoggerFactory

class TiltakgjennomforingEndretConsumer(private val client: MulighetsrommetApiClient) {

    private val logger = LoggerFactory.getLogger(TiltakgjennomforingEndretConsumer::class.java)

    fun process(payload: JsonElement) {
        val updateTiltaksgjennomforing = payload.jsonObject["after"]!!.jsonObject.toTiltaksgjennomforing()
        client.sendRequest(HttpMethod.Put, "/api/arena/tiltaksgjennomforinger", updateTiltaksgjennomforing)
        logger.debug("processed tiltakgjennomforing endret event")
    }

    private fun JsonObject.toTiltaksgjennomforing() = Tiltaksgjennomforing(
        navn = this["LOKALTNAVN"]!!.jsonPrimitive.content,
        tiltakskode = this["TILTAKSKODE"]!!.jsonPrimitive.content,
        fraDato = ProcessingUtils.getArenaDateFromTo(this["DATO_FRA"]!!.jsonPrimitive.content),
        tilDato = ProcessingUtils.getArenaDateFromTo(this["DATO_TIL"]!!.jsonPrimitive.content),
        arrangorId = this["ARBGIV_ID_ARRANGOR"]!!.jsonPrimitive.content.toIntOrNull(),
        arenaId = this["TILTAKGJENNOMFORING_ID"]!!.jsonPrimitive.content.toInt(),
        tiltaksnummer = 0,
        sakId = this["SAK_ID"]!!.jsonPrimitive.content.toInt()
    )
}
