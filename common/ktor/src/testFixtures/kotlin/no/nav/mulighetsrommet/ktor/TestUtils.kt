package no.nav.mulighetsrommet.ktor

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
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
    val serializedContent = if (content is String) {
        content
    } else {
        JsonIgnoreUnknownKeys.encodeToString(T::class.serializer(), content)
    }
    return respond(serializedContent, status, headers)
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

        if (urlMatches(expectedUrl = mockUrl, actualUrl = request.url)) {
            return@MockEngine handler(request)
        }
    }

    throw IllegalStateException("Mock-response is missing for request method=${request.method.value} url=${request.url}")
}

private fun urlMatches(expectedUrl: Url, actualUrl: Url): Boolean {
    if (actualUrl.encodedPath != expectedUrl.encodedPath) {
        return false
    }

    if (!parametersMatches(expectedUrl.parameters, actualUrl.parameters)) {
        return false
    }

    return true
}

private fun parametersMatches(expectedParameters: Parameters, actualParameters: Parameters): Boolean {
    if (expectedParameters.isEmpty()) {
        return true
    }

    return expectedParameters.entries()
        .all { (key, expectedValue) ->
            val actualValue = actualParameters.getAll(key)
                ?: throw IllegalStateException("Expected to find '$key' in request parameters, but it was missing")
            return actualValue == expectedValue
        }
}

fun Url.getLastPathParameterAsUUID(): UUID {
    return encodedPath.split("/").last().toUUID()
}
