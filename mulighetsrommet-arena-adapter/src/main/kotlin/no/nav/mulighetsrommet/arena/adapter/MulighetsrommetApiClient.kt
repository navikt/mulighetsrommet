package no.nav.mulighetsrommet.arena.adapter

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class MulighetsrommetApiClient(uriBase: String) {

    private val logger = LoggerFactory.getLogger(MulighetsrommetApiClient::class.java)
    private val client: HttpClient

    init {
        logger.debug("Init MulighetsrommetApiClient")
        client = HttpClient(CIO) {
            defaultRequest {
                url.takeFrom(
                    URLBuilder().takeFrom(uriBase).apply {
                        encodedPath += url.encodedPath
                    }
                )
            }
        }
    }

    @OptIn(InternalAPI::class)
    internal inline fun <reified T> sendRequest(m: HttpMethod, requestUri: String, payload: T) = runBlocking {
        val response: HttpResponse = client.request(requestUri) {
            contentType(ContentType.Application.Json)
            body = Json.encodeToString(payload)
            method = m
        }
        if (!response.status.isSuccess()) throw Exception("Request to mulighetsrommet-api failed")
        logger.debug("sent request status ${response.status} (${response.request.url})")
    }
}
