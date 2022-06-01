package no.nav.mulighetsrommet.arena.adapter.consumers

import io.ktor.http.*
import kotlinx.serialization.json.*
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.utils.ProcessingUtils
import no.nav.mulighetsrommet.arena.adapter.utils.ProcessingUtils.isInsertArenaOperation
import no.nav.mulighetsrommet.domain.Deltaker
import org.slf4j.LoggerFactory

class TiltakdeltakerEndretConsumer(private val client: MulighetsrommetApiClient) {

    private val logger = LoggerFactory.getLogger(TiltakdeltakerEndretConsumer::class.java)
    private var resourceUri = "/api/arena/deltakere"

    fun process(payload: JsonElement) {
        if (isInsertArenaOperation(payload.jsonObject)) handleInsert(payload.jsonObject) else handleUpdate(payload.jsonObject)
    }

    private fun handleInsert(payload: JsonObject) {
        val newDeltaker = payload["after"]!!.jsonObject.toDeltaker()
        client.sendRequest(HttpMethod.Post, resourceUri, newDeltaker)
        logger.debug("processed deltaker endret insert")
    }

    private fun handleUpdate(payload: JsonObject) {
        val updatedDeltaker = payload["after"]!!.jsonObject.toDeltaker()
        client.sendRequest(HttpMethod.Put, "$resourceUri/${updatedDeltaker.arenaId}", updatedDeltaker)
        logger.debug("processed tiltak endret update")
    }

    private fun JsonObject.toDeltaker() = Deltaker(
        arenaId = this["TILTAKDELTAKER_ID"]!!.jsonPrimitive.content.toInt(),
        tiltaksgjennomforingId = this["TILTAKGJENNOMFORING_ID"]!!.jsonPrimitive.content.toInt(),
        personId = this["PERSON_ID"]!!.jsonPrimitive.content.toInt(),
        fraDato = ProcessingUtils.getArenaDateFromTo(this["DATO_FRA"]!!.jsonPrimitive.content),
        tilDato = ProcessingUtils.getArenaDateFromTo(this["DATO_TIL"]!!.jsonPrimitive.content),
        status = ProcessingUtils.toDeltakerstatus(this["DELTAKERSTATUSKODE"]!!.jsonPrimitive.content)
    )
}
