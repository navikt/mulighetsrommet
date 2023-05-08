package no.nav.mulighetsrommet.api.clients.msgraph

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.domain.dto.AdGruppe
import no.nav.mulighetsrommet.api.domain.dto.NavAnsattDto
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import java.util.*

class MicrosoftGraphClientImplTest : FunSpec({

    test("should get an MsGraph user as a NavAnsatt") {
        val id = UUID.randomUUID()

        val engine = createMockEngine(
            "/v1.0/users/$id?\$select=id,streetAddress,city,givenName,surname,onPremisesSamAccountName" to {
                respondJson(
                    MsGraphUserDto(
                        id = id,
                        givenName = "Donald",
                        surname = "Duck",
                        onPremisesSamAccountName = "DD123456",
                        mail = "donald.duck@nav.no",
                        streetAddress = "0400",
                        city = "Andeby",
                    ),
                )
            },
        )

        val client = MicrosoftGraphClientImpl(engine, "https://ms-graph.com") { token ->
            token
        }

        client.getNavAnsatt("token", id) shouldBe NavAnsattDto(
            navident = "DD123456",
            fornavn = "Donald",
            etternavn = "Duck",
            hovedenhetKode = "0400",
            hovedenhetNavn = "Andeby",
        )
    }

    test("should get member groups as AdGruppe") {
        val id = UUID.randomUUID()

        val group = MsGraphGroup(UUID.randomUUID(), displayName = "TEST")

        val engine = createMockEngine(
            "/v1.0/users/$id/transitiveMemberOf" to {
                respondJson(GetMemberGroupsResponse(listOf(group)))
            },
        )

        val client = MicrosoftGraphClientImpl(engine, "https://ms-graph.com") { token ->
            token
        }

        client.getMemberGroups(id) shouldBe listOf(AdGruppe(group.id, group.displayName))
    }
})
