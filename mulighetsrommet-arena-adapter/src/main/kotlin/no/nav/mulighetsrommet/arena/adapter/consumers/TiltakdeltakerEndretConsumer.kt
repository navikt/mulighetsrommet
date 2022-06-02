package no.nav.mulighetsrommet.arena.adapter.consumers

import io.ktor.http.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.utils.ProcessingUtils
import no.nav.mulighetsrommet.domain.Deltaker
import org.slf4j.LoggerFactory

class TiltakdeltakerEndretConsumer(private val client: MulighetsrommetApiClient) {

    private val logger = LoggerFactory.getLogger(TiltakdeltakerEndretConsumer::class.java)

    fun process(payload: JsonElement) {
        val updatedDeltaker = payload.jsonObject["after"]!!.jsonObject.toDeltaker()
        client.sendRequest(HttpMethod.Put, "/api/arena/deltakere", updatedDeltaker)
        logger.debug("processed tiltak endret event")
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
