package no.nav.mulighetsrommet.kafka.consumers

import io.ktor.http.*
import kotlinx.serialization.json.*
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.Tiltakstype
import no.nav.mulighetsrommet.kafka.MulighetsrommetApiClient
import no.nav.mulighetsrommet.kafka.utils.ProcessingUtils
import no.nav.mulighetsrommet.kafka.utils.ProcessingUtils.isInsertArenaOperation
import org.slf4j.LoggerFactory

class TiltakEndretConsumer(private val client: MulighetsrommetApiClient) {

    private val logger = LoggerFactory.getLogger(TiltakEndretConsumer::class.java)
    private var resourceUri = "/api/tiltakstyper"

    fun process(payload: JsonElement) {
        if (isInsertArenaOperation(payload.jsonObject)) handleInsert(payload.jsonObject) else handleUpdate(payload.jsonObject)
    }

    private fun handleInsert(payload: JsonObject) {
        val newTiltakstype = payload["after"]!!.jsonObject.toTiltakstype()
        client.sendRequest(HttpMethod.Post, resourceUri, newTiltakstype)
        logger.debug("processed tiltak endret insert")
    }

    private fun handleUpdate(payload: JsonObject) {
        val updatedTiltakstype = payload["after"]!!.jsonObject.toTiltakstype()
        client.sendRequest(HttpMethod.Put, "$resourceUri/${updatedTiltakstype.tiltakskode}", updatedTiltakstype)
        logger.debug("processed tiltak endret update")
    }

    private fun JsonObject.toTiltakstype() = Tiltakstype(
        navn = this["TILTAKSNAVN"]!!.jsonPrimitive.content,
        innsatsgruppe = 1,
        tiltakskode = Tiltakskode.valueOf(this["TILTAKSKODE"]!!.jsonPrimitive.content),
        fraDato = ProcessingUtils.getArenaDateFromTo(this["DATO_FRA"]!!.jsonPrimitive.content),
        tilDato = ProcessingUtils.getArenaDateFromTo(this["DATO_TIL"]!!.jsonPrimitive.content)
    )
}
