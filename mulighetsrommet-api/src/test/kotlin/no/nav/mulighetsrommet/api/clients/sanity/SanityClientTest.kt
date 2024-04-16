package no.nav.mulighetsrommet.api.clients.sanity

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import no.nav.mulighetsrommet.api.domain.dto.SanityResponse
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import java.util.*

class SanityClientTest : FunSpec({

    val testConfig = SanityClient.Config(projectId = "123", dataset = "test", token = null, apiVersion = "v2024-01-01")

    val mockResponse = SanityResponse.Result(
        ms = 100,
        query = "",
        result = Json.decodeFromString("""["foo", "bar"]"""),
    )

    test("should query url generated from the Config with default sanity perspective") {
        val engine = createMockEngine(
            "/v2024-01-01/data/query/test?query=*[_type == 'foo']&perspective=published" to {
                respondJson(mockResponse)
            },
        )
        val client = SanityClient(engine, testConfig)

        val result = client.query("*[_type == 'foo']")

        result.shouldBeTypeOf<SanityResponse.Result> {
            it.decode<List<String>>() shouldBe listOf("foo", "bar")
        }
    }

    test("should add additional parameters as query parameters prefixed with $") {
        val engine = createMockEngine(
            "/v2024-01-01/data/query/test?query=*[]&\$id=\"b97b6d59-09af-44e3-bbd5-09c7030f4be2\"&\$string=\"foo\"&\$boolean=true&\$number=1.2&\$array=[\"bar\"]" to {
                respondJson(mockResponse)
            },
        )
        val client = SanityClient(engine, testConfig)

        val result = client.query(
            "*[]",
            params = listOf(
                SanityParam.of("id", UUID.fromString("b97b6d59-09af-44e3-bbd5-09c7030f4be2")),
                SanityParam.of("string", "foo"),
                SanityParam.of("boolean", true),
                SanityParam.of("number", 1.2),
                SanityParam.of("array", listOf("bar")),
            ),
        )

        result.shouldBeTypeOf<SanityResponse.Result>()
    }

    test("returns error response when query fails") {
        val error = Json.decodeFromString<JsonElement>(
            """
            {
                "error": {
                    "query": "*[_type == ${'$'}type][0]",
                    "description": "param ${'$'}type referenced, but not provided",
                    "start": 11,
                    "end": 15,
                    "type": "queryParseError"
                }
            }
            """.trimIndent(),
        )

        val engine = createMockEngine(
            "/v2024-01-01/data/query/test?query=*[_type == \$type]" to {
                respondJson(error, status = HttpStatusCode.BadRequest)
            },
        )
        val client = SanityClient(engine, testConfig)

        val result = client.query("*[_type == \$type]")

        result.shouldBeTypeOf<SanityResponse.Error> {
            it.error.getValue("type").jsonPrimitive.content shouldBe "queryParseError"
        }
    }
})
