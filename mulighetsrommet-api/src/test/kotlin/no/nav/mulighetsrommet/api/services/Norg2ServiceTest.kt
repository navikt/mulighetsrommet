package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.fixtures.Norg2EnhetFixture
import no.nav.mulighetsrommet.api.repositories.EnhetRepository

class Norg2ServiceTest : FunSpec({
    val norg2Client: Norg2Client = mockk()
    val enheter: EnhetRepository = mockk(relaxed = true)
    val norg2Service = Norg2Service(norg2Client, enheter)

    test("Synkroniser enheter skal slette enheter som ikke tilfredstiller whitelist") {
        val mockEnheter = listOf(
            Norg2EnhetFixture.enhet.copy(enhetId = 1, type = Norg2Type.AAREG),
            Norg2EnhetFixture.enhet.copy(enhetId = 2),
            Norg2EnhetFixture.enhet.copy(enhetId = 3)
        )

        coEvery {
            norg2Client.hentEnheter()
        } returns mockEnheter

        val tilLagring = norg2Service.synkroniserEnheter()
        tilLagring.size shouldBe 2
        tilLagring[0].enhetId shouldBe 2
        tilLagring[1].enhetId shouldBe 3
    }
})
