package no.nav.mulighetsrommet.ktor

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.mulighetsrommet.utils.toUUID
import java.util.*

/**
 * Utility to decode the body of [HttpRequestData] to the type [T].
 */
@OptIn(InternalSerializationApi::class)
inline fun <reified T : Any> HttpRequestData.decodeRequestBody(): T {
    return JsonIgnoreUnknownKeys.decodeFromString(T::class.serializer(), (body as TextContent).text)
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
        HttpHeaders.ContentType,
        ContentType.Application.Json.toString(),
    )
    return respond(JsonIgnoreUnknownKeys.encodeToString(T::class.serializer(), content), status, headers)
}

/**
 * Utility to create a [MockEngine] based on the provided [requestHandlers].
 *
 * The created [MockEngine] needs an explicit handler for each possible HTTP request it receives and throws an
 * [IllegalStateException] if it receives an HTTP request without a corresponding handler.
 */
fun createMockEngine(
    vararg requestHandlers: Pair<String, suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData>,
) = MockEngine { request ->
    for ((uri, handler) in requestHandlers) {
        val mockUrl = Url(uri)

        if (urlMatches(request.url, mockUrl)) {
            return@MockEngine handler(request)
        }
    }

    throw IllegalStateException("Mock-response is missing for request method=${request.method.value} url=${request.url}")
}

private fun urlMatches(requestUrl: Url, mockUrl: Url): Boolean {
    if (requestUrl.encodedPath != mockUrl.encodedPath) {
        return false
    }

    if (!mockUrl.parameters.isEmpty() && requestUrl.parameters != mockUrl.parameters) {
        return false
    }

    return true
}

fun Url.getLastPathParameterAsUUID(): UUID {
    return encodedPath.split("/").last().toUUID()
}
