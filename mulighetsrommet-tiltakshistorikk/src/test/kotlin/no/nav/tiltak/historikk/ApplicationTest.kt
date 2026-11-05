package no.nav.tiltak.historikk

import com.diffplug.selfie.coroutines.expectSelfie
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

class ApplicationTest : FunSpec({
    val jsonPrettyPrint = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    test("OpenAPI er tilgjengelig uten autentisering og beskriver kun historikk-endepunktet") {
        lateinit var openApiSpec: String
        withTestApplication {
            val response = client.get("/openapi.json")

            response.status shouldBe HttpStatusCode.OK
            response.contentType()?.withoutParameters() shouldBe ContentType.Application.Json

            val document = Json.parseToJsonElement(response.bodyAsText())
            openApiSpec = jsonPrettyPrint.encodeToString(document)
        }
        expectSelfie(openApiSpec).toMatchDisk()
    }

    test("Historikk krever autentisering") {
        withTestApplication {
            client.post("/api/v1/historikk").status shouldBe HttpStatusCode.Unauthorized
        }
    }

    context("liveness") {
        test("should respond with 200 OK") {
            withTestApplication {
                val response = client.get("/internal/liveness")

                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    context("readiness") {
        test("should respond with 200 OK") {
            withTestApplication {
                val response = client.get("/internal/readiness")

                response.status shouldBe HttpStatusCode.OK
            }
        }
    }
})
