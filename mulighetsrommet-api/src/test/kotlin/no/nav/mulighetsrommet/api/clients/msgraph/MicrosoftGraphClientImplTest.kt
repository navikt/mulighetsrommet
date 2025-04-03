package no.nav.mulighetsrommet.api.clients.msgraph

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType
import java.util.*

class MicrosoftGraphClientImplTest : FunSpec({

    fun createClient(engine: MockEngine) = MicrosoftGraphClient(engine, "https://ms-graph.com") { "token" }

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

        client.getNavAnsatt(id, AccessType.M2M) shouldBe AzureAdNavAnsatt(
            azureId = id,
            navIdent = NavIdent("DD123456"),
            fornavn = "Donald",
            etternavn = "Duck",
            hovedenhetKode = NavEnhetNummer("0400"),
            hovedenhetNavn = "Andeby",
            mobilnummer = "12345678",
            epost = "donald.duck@nav.no",
        )
    }

    test("should get member groups as AdGruppe") {
        val id = UUID.randomUUID()

        val group = MsGraphGroup(UUID.randomUUID(), displayName = "TEST")

        val engine = createMockEngine {
            get("/v1.0/users/$id/transitiveMemberOf/microsoft.graph.group") {
                respondJson(GetMemberGroupsResponse(listOf(group)))
            }
        }

        val client = createClient(engine)

        client.getMemberGroups(id, AccessType.M2M) shouldBe listOf(AdGruppe(group.id, group.displayName))
    }
})
