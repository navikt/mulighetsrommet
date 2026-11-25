package no.nav.mulighetsrommet.ktor.plugins

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.server.application.createApplicationPlugin
import io.opentelemetry.api.trace.Span
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import no.nav.mulighetsrommet.model.ProblemDetail

val ProblemDetailResponsePlugin = createApplicationPlugin("ProblemDetailResponsePlugin") {
    onCallRespond { _, body ->
        if (body is ProblemDetail) {
            transformBody {
                problemDetailResponseContent(body)
            }
        }
    }
}

internal fun problemDetailResponseContent(problem: ProblemDetail): TextContent {
    val json = Json.encodeToJsonElement(problem).jsonObject
    val spanContext = Span.current().spanContext
    val enriched = if (spanContext.isValid) {
        JsonObject(json + ("traceId" to JsonPrimitive(spanContext.traceId)))
    } else {
        json
    }

    return TextContent(
        text = enriched.toString(),
        contentType = ContentType.Application.ProblemJson,
        status = HttpStatusCode.fromValue(problem.status),
    )
}
