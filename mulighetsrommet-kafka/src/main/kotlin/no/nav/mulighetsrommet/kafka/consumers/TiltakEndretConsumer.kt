package no.nav.mulighetsrommet.kafka.consumers

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import no.nav.mulighetsrommet.kafka.domain.Tiltakskode
import no.nav.mulighetsrommet.kafka.domain.Tiltakstype
import no.nav.mulighetsrommet.kafka.utils.ProcessingUtils
import no.nav.mulighetsrommet.kafka.utils.ProcessingUtils.isInsertArenaOperation
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

object TiltakEndretConsumer {

    private val logger = LoggerFactory.getLogger(TiltakEndretConsumer::class.java)
//    private val client = HttpClient()
    private var endpointUri = ""

    fun process(payload: JsonElement) {
        logger.debug("ASDALDSAD?D????")
        if (isInsertArenaOperation(payload.jsonObject)) handleInsert(payload.jsonObject) else handleUpdate(payload.jsonObject)
    }

    fun setEndpointUri(uri: String = "http://localhost:8080") {
        endpointUri = "$uri/api/tiltakstyper"
    }

    private fun handleInsert(payload: JsonObject) {
        logger.debug("HALLO INSERT")
        val newTiltakstype = payload["after"]!!.jsonObject.toTiltakstype()
//        sendRequest(HttpMethod.Post, newTiltakstype)
    }

    private fun handleUpdate(payload: JsonObject) {
        logger.debug("HALLO UPDATE")
        val updatedTiltakstype = payload["after"]!!.jsonObject.toTiltakstype()
//        sendRequest(HttpMethod.Put, updatedTiltakstype)
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

//    @OptIn(InternalAPI::class)
//    private fun sendRequest(method: HttpMethod, tiltakstype: Tiltakstype) = runBlocking {
//        val response: HttpResponse = client.request(endpointUri) {
//            contentType(ContentType.Application.Json)
//            body = tiltakstype
//            method
//        }
//        logger.debug("sent request status ${response.status}")
//    }
}
