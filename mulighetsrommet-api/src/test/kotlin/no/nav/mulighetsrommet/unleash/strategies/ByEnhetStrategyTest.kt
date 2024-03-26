package no.nav.mulighetsrommet.unleash.strategies

import io.getunleash.UnleashContext
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.common.client.axsys.AxsysClient
import no.nav.common.client.axsys.AxsysEnhet
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.NavIdent

class ByEnhetStrategyTest : FunSpec({
    context("Unleash - ByEnhetStrategy") {
        val axsysClient = mockk<AxsysClient>(relaxed = true)
        coEvery { axsysClient.hentTilganger(NavIdent("N123456")) } returns mockEnheter()
        coEvery { axsysClient.hentTilganger(NavIdent("N666666")) } returns mockAvskruddeEnheter()
        val byEnhetStrategy = ByEnhetStrategy(axsysClient = axsysClient)

        test("Skal returnere false når ingen UnleashContext er sendt inn") {
            byEnhetStrategy.isEnabled(mutableMapOf()) shouldBe false
        }

        test("Skal returnere false når brukers enhet ikke finnes i liste med påskrudde enheter") {
            byEnhetStrategy.isEnabled(
                mutableMapOf(ByEnhetStrategy.VALGT_ENHET_PARAM to "987,345"),
                UnleashContext("N666666", "", "", emptyMap()),
            ) shouldBe false
        }

        test("Skal returnere true når brukers enhet finnes i listen over påskrudde enheter") {
            byEnhetStrategy.isEnabled(
                mutableMapOf(ByEnhetStrategy.VALGT_ENHET_PARAM to "123,456"),
                UnleashContext("N123456", "", "", emptyMap()),
            ) shouldBe true
        }
    }
})

private fun mockEnheter(): List<AxsysEnhet> {
    val enhet1 = AxsysEnhet()
    enhet1.navn = "Nav 123"
    enhet1.temaer = (listOf("OPP", "NED"))
    enhet1.enhetId = EnhetId("123")

    val enhet2 = AxsysEnhet()
    enhet2.navn = "Nav 345"
    enhet2.temaer = (listOf("NED"))
    enhet2.enhetId = EnhetId("345")

    val enhet3 = AxsysEnhet()
    enhet3.navn = "Nav 678"
    enhet3.temaer = (listOf("TIL_SIDEN"))
    enhet3.enhetId = EnhetId("678")

    return listOf(enhet1, enhet2, enhet3)
}

private fun mockAvskruddeEnheter(): List<AxsysEnhet> {
    val enhet2 = AxsysEnhet()
    enhet2.navn = "Nav 345"
    enhet2.temaer = (listOf("NED"))
    enhet2.enhetId = EnhetId("345")

    val enhet3 = AxsysEnhet()
    enhet3.navn = "Nav 678"
    enhet3.temaer = (listOf("TIL_SIDEN"))
    enhet3.enhetId = EnhetId("678")

    return listOf(enhet2, enhet3)
}
