package no.nav.mulighetsrommet.ktor

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

val json = Json {
    ignoreUnknownKeys = true
}

/**
 * Utility to decode the body of [HttpRequestData] to the type [T].
 */
@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> HttpRequestData.decodeRequestBody(): T {
    return json.decodeFromString(T::class.serializer(), (body as TextContent).text)
}

/**
 * Utility extension to be used in combination with the ktor [MockEngine] to create an HTTP response with the type [T]
 * encoded as JSON.
 */
@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> MockRequestHandleScope.respondJson(
    content: T,
    status: HttpStatusCode = HttpStatusCode.OK,
): HttpResponseData {
    val headers = headersOf(
        HttpHeaders.ContentType, ContentType.Application.Json.toString()
    )
    return respond(json.encodeToString(T::class.serializer(), content), status, headers)
}

/**
 * Utility to create a [MockEngine] based on the provided [requestHandlers].
 *
 * The created [MockEngine] needs an explicit handler for each possible HTTP request it receives and throws an
 * [IllegalStateException] if it receives an HTTP request without a corresponding handler.
 */
fun createMockEngine(
    vararg requestHandlers: Pair<String, suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData>
) = MockEngine { request ->
    for ((path, handler) in requestHandlers) {
        if (request.url.encodedPath == path) {
            return@MockEngine handler(request)
        }
    }
    throw IllegalStateException("Mock-response missing for path ${request.url.encodedPath}")
}
