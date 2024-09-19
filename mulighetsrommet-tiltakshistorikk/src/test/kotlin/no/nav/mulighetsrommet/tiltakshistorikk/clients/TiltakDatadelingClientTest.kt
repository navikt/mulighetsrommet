package no.nav.mulighetsrommet.tiltakshistorikk.clients

import arrow.core.nonEmptyListOf
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.mulighetsrommet.tokenprovider.AccessType

class TiltakDatadelingClientTest : FunSpec({
    test("get avtaler for person") {
        val clientEngine = createMockEngine(
            "/graphql" to {
                respondJson(
                    """
                    {
                      "data": {
                        "avtalerForPerson": [
                          {
                            "avtaleId": "d0abcf43-f240-442f-913b-8865521c40b3",
                            "avtaleNr": 548,
                            "deltakerFnr": "07860198775",
                            "bedriftNr": "910825518",
                            "tiltakstype": "VARIG_LONNSTILSKUDD",
                            "startDato": "2023-01-16",
                            "sluttDato": "2025-04-01",
                            "avtaleStatus": "GJENNOMFORES",
                            "registrertTidspunkt": "2023-01-01T11:05:40.946+02:00"
                          },
                          {
                            "avtaleId": "32006ff3-76cb-4e15-b35e-3f049e4cdb0a",
                            "avtaleNr": 570,
                            "deltakerFnr": "07860198775",
                            "bedriftNr": "896929119",
                            "tiltakstype": "MIDLERTIDIG_LONNSTILSKUDD",
                            "startDato": null,
                            "sluttDato": null,
                            "avtaleStatus": "ANNULLERT",
                            "registrertTidspunkt": "2023-02-01T11:05:40.946+02:00"
                          },
                          {
                            "avtaleId": "67404e92-e4c4-4201-bee1-9b0f4b649b38",
                            "avtaleNr": 551,
                            "deltakerFnr": "07860198775",
                            "bedriftNr": "910825518",
                            "tiltakstype": "VARIG_LONNSTILSKUDD",
                            "startDato": "2023-01-24",
                            "sluttDato": "2023-02-05",
                            "avtaleStatus": "AVSLUTTET",
                            "registrertTidspunkt": "2023-01-01T11:05:40.946+02:00"
                          }
                        ]
                      }
                    }
                    """.trimIndent(),
                )
            },
        )

        val client = TiltakDatadelingClient(
            engine = clientEngine,
            baseUrl = "https://tiltak-datadeling.intern.dev.nav.no",
        ) { "token" }

        val result = client.getAvtalerForPerson(GraphqlRequest.GetAvtalerForPerson("07860198775"), AccessType.M2M)

        result.shouldBeRight().should {
            it.shouldHaveSize(3)
        }
    }

    test("returns error when server responds with an error") {
        val errors = nonEmptyListOf(
            GraphqlResponse.GraphqlError(
                message = "Syntax error",
                extensions = GraphqlResponse.Extensions(classification = "InvalidSyntax"),
            ),
        )
        val clientEngine = createMockEngine(
            "/graphql" to {
                respondJson(
                    JsonIgnoreUnknownKeys.encodeToString<GraphqlResponse<Nothing>>(
                        GraphqlResponse(errors = errors),
                    ),
                )
            },
        )

        val client = TiltakDatadelingClient(
            engine = clientEngine,
            baseUrl = "https://tiltak-datadeling.intern.dev.nav.no",
        ) { "token" }

        val result = client.getAvtalerForPerson(GraphqlRequest.GetAvtalerForPerson("07860198775"), AccessType.M2M)

        result.shouldBeLeft().should {
            it shouldBe TiltakDatadelingError.GraphqlError(errors)
        }
    }
})
