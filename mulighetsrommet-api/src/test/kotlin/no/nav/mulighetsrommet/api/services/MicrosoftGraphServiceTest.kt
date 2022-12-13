package no.nav.mulighetsrommet.api.services

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.msgraph.MicrosoftGraphClient
import no.nav.mulighetsrommet.api.domain.MSGraphBrukerdata
import org.assertj.core.api.Assertions.assertThat
import java.util.*

class MicrosoftGraphServiceTest : FunSpec({
    val navAnsattAzureId = UUID.randomUUID()

    context("Hent hovedenhet for nav-ansatt") {
        test("Når man kaller hentHovedenhet for en nav ansatts azureId får man svar og repeterende forespørsler kommer fra cache") {
            val mockResponse = MSGraphBrukerdata(hovedenhetKode = "2990", hovedenhetNavn = "IT-Avdelingen")

            val client: MicrosoftGraphClient = mockk()
            coEvery {
                client.hentHovedenhetForBruker(navAnsattAzureId)
            } returns mockResponse

            val service = MicrosoftGraphService(client)
            val result = service.hentHovedEnhetForNavAnsatt(navAnsattAzureId)

            service.hentHovedEnhetForNavAnsatt(navAnsattAzureId)
            service.hentHovedEnhetForNavAnsatt(navAnsattAzureId)
            service.hentHovedEnhetForNavAnsatt(navAnsattAzureId)

            assertThat(result.hovedenhetKode).isEqualTo("2990")
            assertThat(result.hovedenhetNavn).isEqualTo("IT-Avdelingen")
            coVerify(exactly = 1) {
                client.hentHovedenhetForBruker(navAnsattAzureId)
            }
        }

        test("Når man kaller hentHovedenhet og ikke finner bruker skal det kastes en feil") {

            val client: MicrosoftGraphClient = mockk()
            coEvery {
                client.hentHovedenhetForBruker(UUID.randomUUID())
            } throws RuntimeException("Klarte ikke hente bruker")

            val service = MicrosoftGraphService(client)

            shouldThrow<RuntimeException> {
                service.hentHovedEnhetForNavAnsatt(navAnsattAzureId)
            }
        }
    }
})
