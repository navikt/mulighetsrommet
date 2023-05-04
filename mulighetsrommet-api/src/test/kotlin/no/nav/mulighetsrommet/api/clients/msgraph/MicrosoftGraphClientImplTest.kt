package no.nav.mulighetsrommet.api.clients.msgraph

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.domain.dto.NavAnsattDto
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import java.util.*

class MicrosoftGraphClientImplTest : FunSpec({

    test("should get an MsGraph user as a NAV ansatt") {
        val id = UUID.randomUUID()

        val engine = createMockEngine(
            "/v1.0/users/$id?\$select=streetAddress,city,givenName,surname,onPremisesSamAccountName" to {
                respondJson(
                    MsGraphUserDto(
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
            fornavn = "Donald",
            etternavn = "Duck",
            navident = "DD123456",
            hovedenhetKode = "0400",
            hovedenhetNavn = "Andeby",
        )
    }
})
