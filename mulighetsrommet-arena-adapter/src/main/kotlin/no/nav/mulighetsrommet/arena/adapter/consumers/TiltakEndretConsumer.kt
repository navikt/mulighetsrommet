package no.nav.mulighetsrommet.arena.adapter.consumers

import io.ktor.http.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.utils.ProcessingUtils
import no.nav.mulighetsrommet.domain.Tiltakstype
import org.slf4j.LoggerFactory

class TiltakEndretConsumer(private val client: MulighetsrommetApiClient) {

    private val logger = LoggerFactory.getLogger(TiltakEndretConsumer::class.java)

    fun process(payload: JsonElement) {
        val tiltakstype = payload.jsonObject["after"]!!.jsonObject.toTiltakstype()
        client.sendRequest(HttpMethod.Put, "/api/arena/tiltakstyper", tiltakstype)
        logger.debug("processed tiltak endret event")
    }

    private fun JsonObject.toTiltakstype() = Tiltakstype(
        navn = this["TILTAKSNAVN"]!!.jsonPrimitive.content,
        innsatsgruppe = 1,
        tiltakskode = this["TILTAKSKODE"]!!.jsonPrimitive.content,
        fraDato = ProcessingUtils.getArenaDateFromTo(this["DATO_FRA"]!!.jsonPrimitive.content),
        tilDato = ProcessingUtils.getArenaDateFromTo(this["DATO_TIL"]!!.jsonPrimitive.content)
    )
}
