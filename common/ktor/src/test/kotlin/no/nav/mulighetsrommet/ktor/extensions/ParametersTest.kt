package no.nav.mulighetsrommet.ktor.extensions

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class ParametersTest : FunSpec({
    context("getJsonObject") {
        test("should return null when requested name does not exist in parameters") {
            val parameters = ParametersBuilder().build()

            val json = parameters.getJsonObject("person")

            json shouldBe null
        }

        test("should parse parameters encoded as OpenAPI deepObject to json object") {
            val builder = ParametersBuilder()
            builder.append("person[firstName]", "Donald")
            builder.append("person[lastName]", "Duck")
            val parameters = builder.build()

            val json = parameters.getJsonObject("person")

            json shouldBe JsonObject(
                mapOf(
                    "firstName" to JsonPrimitive("Donald"),
                    "lastName" to JsonPrimitive("Duck"),
                ),
            )
        }
    }
})
