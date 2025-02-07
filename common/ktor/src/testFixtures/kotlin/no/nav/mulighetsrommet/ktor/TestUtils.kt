package no.nav.mulighetsrommet.ktor

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
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
        JsonIgnoreUnknownKeys.encodeToString(content)
    }
    return respond(serializedContent, status, headers)
}

inline fun <reified T : Any> MockRequestHandleScope.respondJson(
    content: T,
    serializer: KSerializer<T>,
    status: HttpStatusCode = HttpStatusCode.OK,
): HttpResponseData {
    val headers = headersOf(
        HttpHeaders.ContentType,
        ContentType.Application.Json.toString(),
    )
    val serializedContent = JsonIgnoreUnknownKeys.encodeToString(serializer, content)
    return respond(serializedContent, status, headers)
}

/**
 * Utility to create a [MockEngine] based on the provided [MockEngineBuilder].
 *
 * The created [MockEngine] needs an explicit handler for each possible HTTP request it receives and throws an
 * [IllegalStateException] if it receives an HTTP request without a corresponding handler.
 */
fun createMockEngine(builder: MockEngineBuilder.() -> Unit = {}): MockEngine {
    val engineBuilder = MockEngineBuilder().apply(builder)
    return MockEngine { request ->
        for ((method, uriPattern, handler) in engineBuilder.requestHandlers) {
            val isMatch = method == request.method &&
                when (uriPattern) {
                    is Regex -> urlMatchesRegex(uriPattern, request.url)
                    is String -> urlMatchesPath(Url(uriPattern), request.url)
                    else -> throw IllegalArgumentException("URI pattern must be either a String or Regex")
                }

            if (isMatch) {
                return@MockEngine handler(request)
            }
        }

        throw IllegalStateException("Mock-response is missing for request method=${request.method.value} url=${request.url}")
    }
}

typealias MockRequestHandler = suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData

@DslMarker
annotation class MockEngineDsl

@MockEngineDsl
class MockEngineBuilder {
    internal val requestHandlers = mutableListOf<Triple<HttpMethod, Any, MockRequestHandler>>()

    @MockEngineDsl
    fun get(uri: String, handler: MockRequestHandler) {
        requestHandlers.add(Triple(HttpMethod.Get, uri, handler))
    }

    @MockEngineDsl
    fun post(uri: String, handler: MockRequestHandler) {
        requestHandlers.add(Triple(HttpMethod.Post, uri, handler))
    }

    @MockEngineDsl
    fun put(uri: String, handler: MockRequestHandler) {
        requestHandlers.add(Triple(HttpMethod.Put, uri, handler))
    }

    @MockEngineDsl
    fun delete(uri: String, handler: MockRequestHandler) {
        requestHandlers.add(Triple(HttpMethod.Delete, uri, handler))
    }

    @MockEngineDsl
    fun get(uri: Regex, handler: MockRequestHandler) {
        requestHandlers.add(Triple(HttpMethod.Get, uri, handler))
    }

    @MockEngineDsl
    fun post(uri: Regex, handler: MockRequestHandler) {
        requestHandlers.add(Triple(HttpMethod.Post, uri, handler))
    }

    @MockEngineDsl
    fun put(uri: Regex, handler: MockRequestHandler) {
        requestHandlers.add(Triple(HttpMethod.Put, uri, handler))
    }

    @MockEngineDsl
    fun delete(uri: Regex, handler: MockRequestHandler) {
        requestHandlers.add(Triple(HttpMethod.Delete, uri, handler))
    }
}

private fun urlMatchesRegex(expectedRegex: Regex, actualUrl: Url): Boolean {
    return expectedRegex.matches(actualUrl.toString())
}

private fun urlMatchesPath(expectedUrl: Url, actualUrl: Url): Boolean {
    return expectedUrl.encodedPath == actualUrl.encodedPath &&
        parametersMatches(expectedUrl.parameters, actualUrl.parameters)
}

private fun parametersMatches(expectedParameters: Parameters, actualParameters: Parameters): Boolean {
    if (expectedParameters.isEmpty()) {
        return true
    }

    return expectedParameters.entries().all { (key, expectedValue) ->
        val actualValue = actualParameters.getAll(key)
            ?: throw IllegalStateException("Expected to find '$key' in request parameters, but it was missing")
        return actualValue == expectedValue
    }
}

fun Url.getLastPathParameterAsUUID(): UUID {
    return encodedPath.split("/").last().toUUID()
}
