package no.nav.mulighetsrommet.api.clients.msgraph

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.utils.toUUID
import java.util.*

class MsGraphClientTest : FunSpec({

    fun createClient(engine: MockEngine) = MsGraphClient(engine, "https://ms-graph.com") { "token" }

    test("should get an MsGraph user as a NavAnsatt") {
        val id = UUID.randomUUID()

        val engine = createMockEngine {
            get("/v1.0/users/$id?\$select=id,streetAddress,city,givenName,surname,onPremisesSamAccountName,mail,mobilePhone") {
                respondJson(
                    MsGraphUserDto(
                        id = id,
                        givenName = "Donald",
                        surname = "Duck",
                        onPremisesSamAccountName = "DD123456",
                        mail = "donald.duck@nav.no",
                        streetAddress = "0400",
                        city = "Andeby",
                        mobilePhone = "12345678",
                    ),
                )
            }
        }

        val client = createClient(engine)

        client.getNavAnsatt(id, AccessType.M2M) shouldBe EntraNavAnsatt(
            entraObjectId = id,
            navIdent = NavIdent("DD123456"),
            fornavn = "Donald",
            etternavn = "Duck",
            hovedenhetKode = NavEnhetNummer("0400"),
            hovedenhetNavn = "Andeby",
            mobilnummer = "12345678",
            epost = "donald.duck@nav.no",
        )
    }

    test("should get member groups") {
        val id = UUID.randomUUID()

        val expectedGroupIds = listOf(
            "639e2806-4cc2-484c-a72a-51b4308c52a1".toUUID(),
            "a9fb2838-fd9f-4bbd-aa41-2cabc83b26ac".toUUID(),
        )

        val engine = createMockEngine {
            post("/v1.0/users/$id/getMemberGroups") {
                respondJson(
                    $$"""
                        {
                            "@odata.context": "https://graph.microsoft.com/v1.0/$metadata#Collection(Edm.String)",
                            "value": [
                                "639e2806-4cc2-484c-a72a-51b4308c52a1",
                                "a9fb2838-fd9f-4bbd-aa41-2cabc83b26ac"
                            ]
                        }
                    """.trimIndent(),
                )
            }
        }

        val client = createClient(engine)

        client.getMemberGroups(id, AccessType.M2M) shouldBe expectedGroupIds
    }
})
