package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.norg2.*
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.repositories.NavEnhetRepository
import no.nav.mulighetsrommet.slack.SlackNotifier

class NavEnheterSyncServiceTest : FunSpec({
    val norg2Client: Norg2Client = mockk()
    val enheter: NavEnhetRepository = mockk(relaxed = true)
    val slackNotifier: SlackNotifier = mockk(relaxed = true)
    val sanityClient = SanityClient(
        MockEngine {
            respondOk("Ok")
        },
        SanityClient.Config(projectId = "", dataset = "", apiVersion = "", token = ""),
    )
    val navEnheterSyncService = NavEnheterSyncService(norg2Client, sanityClient, enheter, slackNotifier)

    test("Synkroniser enheter skal slette enheter som ikke tilfredstiller whitelist") {
        val norg2Enhet = Norg2EnhetDto(
            enhetId = Math.random().toInt(),
            enhetNr = "1000",
            navn = "Enhet X",
            status = Norg2EnhetStatus.AKTIV,
            type = Norg2Type.LOKAL,
        )

        val mockEnheter = listOf(
            Norg2Response(
                enhet = norg2Enhet.copy(enhetId = 1, type = Norg2Type.AAREG),
                overordnetEnhet = "1200",
            ),
            Norg2Response(
                enhet = norg2Enhet.copy(enhetId = 2),
                overordnetEnhet = "1200",
            ),
            Norg2Response(
                enhet = norg2Enhet.copy(enhetId = 3),
                overordnetEnhet = "1400",
            ),
        )

        coEvery {
            norg2Client.hentEnheter()
        } returns mockEnheter

        val tilLagring = navEnheterSyncService.synkroniserEnheter()
        tilLagring.size shouldBe 2
        tilLagring[0].enhet.enhetId shouldBe 2
        tilLagring[0].overordnetEnhet shouldBe "1200"
        tilLagring[1].enhet.enhetId shouldBe 3
        tilLagring[1].overordnetEnhet shouldBe "1400"
    }
})
