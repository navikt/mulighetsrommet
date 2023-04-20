package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Response
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.fixtures.Norg2EnhetFixture
import no.nav.mulighetsrommet.api.repositories.EnhetRepository

class Norg2ServiceTest : FunSpec({
    val norg2Client: Norg2Client = mockk()
    val enheter: EnhetRepository = mockk(relaxed = true)
    val norg2Service = Norg2Service(norg2Client, enheter)

    test("Synkroniser enheter skal slette enheter som ikke tilfredstiller whitelist") {
        val mockEnheter = listOf(
            Norg2Response(
                enhet = Norg2EnhetFixture.enhet.copy(enhetId = 1, type = Norg2Type.AAREG),
                overordnetEnhet = "1200",
            ),
            Norg2Response(
                enhet = Norg2EnhetFixture.enhet.copy(enhetId = 2),
                overordnetEnhet = "1200",
            ),
            Norg2Response(
                enhet = Norg2EnhetFixture.enhet.copy(enhetId = 3),
                overordnetEnhet = "1400",
            ),
        )

        coEvery {
            norg2Client.hentEnheter()
        } returns mockEnheter

        val tilLagring = norg2Service.synkroniserEnheter()
        tilLagring.size shouldBe 2
        tilLagring[0].enhet.enhetId shouldBe 2
        tilLagring[0].overordnetEnhet shouldBe "1200"
        tilLagring[1].enhet.enhetId shouldBe 3
        tilLagring[1].overordnetEnhet shouldBe "1400"
    }
})
