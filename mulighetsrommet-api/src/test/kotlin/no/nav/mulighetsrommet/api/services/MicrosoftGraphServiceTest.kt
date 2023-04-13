package no.nav.mulighetsrommet.api.services

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.msgraph.AnsattDataDTO
import no.nav.mulighetsrommet.api.clients.msgraph.MicrosoftGraphClient
import org.assertj.core.api.Assertions.assertThat
import java.util.*

class MicrosoftGraphServiceTest : FunSpec({
    val navAnsattAzureId = UUID.randomUUID()

    context("Hent ansattdata for nav-ansatt") {
        test("Når man kaller hentAnsattData for en nav ansatts azureId får man svar og repeterende forespørsler kommer fra cache") {
            val mockResponse = AnsattDataDTO(hovedenhetKode = "2990", hovedenhetNavn = "IT-Avdelingen", fornavn = "Bertil", etternavn = "Betabruker", navident = "B123456")
            val mockAccessToken = "123"

            val client: MicrosoftGraphClient = mockk()
            coEvery {
                client.hentAnsattdata(mockAccessToken, navAnsattAzureId)
            } returns mockResponse

            val service = MicrosoftGraphService(client)
            val result = service.hentAnsattData(mockAccessToken, navAnsattAzureId)

            service.hentAnsattData(mockAccessToken, navAnsattAzureId)
            service.hentAnsattData(mockAccessToken, navAnsattAzureId)
            service.hentAnsattData(mockAccessToken, navAnsattAzureId)

            assertThat(result.hovedenhetKode).isEqualTo("2990")
            assertThat(result.hovedenhetNavn).isEqualTo("IT-Avdelingen")
            assertThat(result.fornavn).isEqualTo("Bertil")
            assertThat(result.etternavn).isEqualTo("Betabruker")
            assertThat(result.navident).isEqualTo("B123456")
            coVerify(exactly = 1) {
                client.hentAnsattdata(mockAccessToken, navAnsattAzureId)
            }
        }

        test("Når man kaller hentAnsattData og ikke finner bruker skal det kastes en feil") {

            val client: MicrosoftGraphClient = mockk()
            coEvery {
                client.hentAnsattdata("123", UUID.randomUUID())
            } throws RuntimeException("Klarte ikke hente bruker")

            val service = MicrosoftGraphService(client)

            shouldThrow<RuntimeException> {
                service.hentAnsattData("123", navAnsattAzureId)
            }
        }
    }
})
