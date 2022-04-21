package no.nav.mulighetsrommet.kafka.consumers

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import no.nav.mulighetsrommet.domain.Tiltaksgjennomforing
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.Tiltakstype
import no.nav.mulighetsrommet.kafka.utils.ProcessingUtils
import no.nav.mulighetsrommet.kafka.utils.ProcessingUtils.isInsertArenaOperation
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class TiltakgjennomforingEndretConsumer(private val client: HttpClient) {

    private val logger = LoggerFactory.getLogger(TiltakgjennomforingEndretConsumer::class.java)
    private var resourceUri = "/api/tiltaksgjennomforinger"

    fun process(payload: JsonElement) {
        if (isInsertArenaOperation(payload.jsonObject)) handleInsert(payload.jsonObject) else handleUpdate(payload.jsonObject)
    }

    private fun handleInsert(payload: JsonObject) {
        val newTiltaksgjennomforing = payload["after"]!!.jsonObject.toTiltaksgjennomforing()
        sendRequest(HttpMethod.Post, newTiltaksgjennomforing, resourceUri)
    }

    private fun handleUpdate(payload: JsonObject) {
        val updateTiltaksgjennomforing = payload["after"]!!.jsonObject.toTiltaksgjennomforing()
        sendRequest(HttpMethod.Put, updateTiltaksgjennomforing, "$resourceUri/${updateTiltaksgjennomforing.arenaId}")
    }

    private fun JsonObject.toTiltaksgjennomforing() = Tiltaksgjennomforing(
        navn = this["LOKALTNAVN"]!!.jsonPrimitive.content,
        tiltakskode = Tiltakskode.valueOf(this["TILTAKSKODE"]!!.jsonPrimitive.content),
        fraDato = ProcessingUtils.getArenaDateFromTo(this["DATO_FRA"]!!.jsonPrimitive.content),
        tilDato = ProcessingUtils.getArenaDateFromTo(this["DATO_TIL"]!!.jsonPrimitive.content),
        arrangorId = this["ARBGIV_ID_ARRANGOR"]!!.jsonPrimitive.content.toIntOrNull(),
        arenaId = this["TILTAKGJENNOMFORING_ID"]!!.jsonPrimitive.content.toInt(),
        tiltaksnummer = 0,
        sakId = this["SAK_ID"]!!.jsonPrimitive.content.toInt()
    )

    @OptIn(InternalAPI::class)
    private fun sendRequest(m: HttpMethod, tiltaksgjennomforing: Tiltaksgjennomforing, requestUri: String) = runBlocking {
        val response: HttpResponse = client.request(requestUri) {
            contentType(ContentType.Application.Json)
            body = Json.encodeToString(tiltaksgjennomforing)
            method = m
        }
        if (response.status == HttpStatusCode.InternalServerError) throw Exception("Request to mulighetsrommet-api failed")
        logger.debug("sent request status ${response.status}")
    }
}
