package no.nav.mulighetsrommet.api.services

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.msgraph.AzureAdNavAnsatt
import no.nav.mulighetsrommet.api.clients.msgraph.MicrosoftGraphClient
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType
import java.util.*

class MicrosoftGraphServiceTest : FunSpec({
    val navAnsattAzureId = UUID.randomUUID()

    context("Hent ansattdata for nav-ansatt") {
        test("Når man kaller hentAnsattData for en nav ansatts azureId får man svar og repeterende forespørsler kommer fra cache") {
            val mockResponse = AzureAdNavAnsatt(
                azureId = UUID.randomUUID(),
                hovedenhetKode = "2990",
                hovedenhetNavn = "IT-Avdelingen",
                fornavn = "Bertil",
                etternavn = "Bengtson",
                navIdent = NavIdent("B123456"),
                mobilnummer = "12345678",
                epost = "test@test.no",
            )

            val client: MicrosoftGraphClient = mockk()
            coEvery {
                client.getNavAnsatt(navAnsattAzureId, AccessType.M2M)
            } returns mockResponse

            val service = MicrosoftGraphService(client)
            val result = service.getNavAnsatt(navAnsattAzureId, AccessType.M2M)

            service.getNavAnsatt(navAnsattAzureId, AccessType.M2M)
            service.getNavAnsatt(navAnsattAzureId, AccessType.M2M)
            service.getNavAnsatt(navAnsattAzureId, AccessType.M2M)

            result shouldBe mockResponse
            coVerify(exactly = 1) {
                client.getNavAnsatt(navAnsattAzureId, AccessType.M2M)
            }
        }

        test("Når man kaller hentAnsattData og ikke finner bruker skal det kastes en feil") {
            val client: MicrosoftGraphClient = mockk()
            coEvery { client.getNavAnsatt(navAnsattAzureId, AccessType.M2M) } throws RuntimeException("Klarte ikke hente bruker")

            val service = MicrosoftGraphService(client)

            shouldThrow<RuntimeException> {
                service.getNavAnsatt(navAnsattAzureId, AccessType.M2M)
            }
        }
    }
})
