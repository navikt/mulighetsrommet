package no.nav.mulighetsrommet.arena.adapter.consumers

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.serialization.json.*
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.utils.ProcessingUtils
import no.nav.mulighetsrommet.arena.adapter.utils.ProcessingUtils.isInsertArenaOperation
import no.nav.mulighetsrommet.domain.Tiltaksgjennomforing
import org.slf4j.LoggerFactory

class TiltakgjennomforingEndretConsumer(private val client: MulighetsrommetApiClient) {

    private val logger = LoggerFactory.getLogger(TiltakgjennomforingEndretConsumer::class.java)
    private var resourceUri = "/api/arena/tiltaksgjennomforinger"

    fun process(payload: JsonElement) {
        if (isInsertArenaOperation(payload.jsonObject)) handleInsert(payload.jsonObject) else handleUpdate(payload.jsonObject)
    }

    private fun handleInsert(payload: JsonObject) {
        val newTiltaksgjennomforing = payload["after"]!!.jsonObject.toTiltaksgjennomforing()
        client.sendRequest(HttpMethod.Post, resourceUri, newTiltaksgjennomforing)
        logger.debug("processed tiltakgjennomforing endret insert")
    }

    private fun handleUpdate(payload: JsonObject) {
        val updateTiltaksgjennomforing = payload["after"]!!.jsonObject.toTiltaksgjennomforing()
        client.sendRequest(HttpMethod.Put, "$resourceUri/${updateTiltaksgjennomforing.arenaId}", updateTiltaksgjennomforing)
        logger.debug("processed tiltakgjennomforing endret update")
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
