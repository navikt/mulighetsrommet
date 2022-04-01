package no.nav.mulighetsrommet.kafka.consumers

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.Tiltakstype
import no.nav.mulighetsrommet.kafka.utils.ProcessingUtils
import no.nav.mulighetsrommet.kafka.utils.ProcessingUtils.isInsertArenaOperation
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class TiltakEndretConsumer(private val client: HttpClient) {

    private val logger = LoggerFactory.getLogger(TiltakEndretConsumer::class.java)
    private var resourceUri = "/api/tiltakstyper"

    fun process(payload: JsonElement) {
        if (isInsertArenaOperation(payload.jsonObject)) handleInsert(payload.jsonObject) else handleUpdate(payload.jsonObject)
    }

    private fun handleInsert(payload: JsonObject) {
        val newTiltakstype = payload["after"]!!.jsonObject.toTiltakstype()
        sendRequest(HttpMethod.Post, newTiltakstype, resourceUri)
    }

    private fun handleUpdate(payload: JsonObject) {
        val updatedTiltakstype = payload["after"]!!.jsonObject.toTiltakstype()
        sendRequest(HttpMethod.Put, updatedTiltakstype, "$resourceUri/${updatedTiltakstype.tiltakskode}")
    }

    private fun JsonObject.toTiltakstype() = Tiltakstype(
        navn = this["TILTAKSNAVN"]!!.jsonPrimitive.content,
        innsatsgruppe = 1,
        tiltakskode = Tiltakskode.valueOf(this["TILTAKSKODE"]!!.jsonPrimitive.content),
        fraDato = LocalDateTime.parse(this["DATO_FRA"]!!.jsonPrimitive.content, ProcessingUtils.getArenaDateFormat()),
        tilDato = LocalDateTime.parse(this["DATO_TIL"]!!.jsonPrimitive.content, ProcessingUtils.getArenaDateFormat()),
        createdBy = this["REG_USER"]!!.jsonPrimitive.content,
        updatedBy = this["MOD_USER"]!!.jsonPrimitive.content
    )

    @OptIn(InternalAPI::class)
    private fun sendRequest(m: HttpMethod, tiltakstype: Tiltakstype, requestUri: String) = runBlocking {
        val response: HttpResponse = client.request(requestUri) {
            contentType(ContentType.Application.Json)
            body = Json.encodeToString(tiltakstype)
            method = m
        }
        logger.debug("sent request status ${response.status}")
    }
}
