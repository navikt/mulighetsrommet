package no.nav.mulighetsrommet.api.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.clients.vedtak.Innsatsgruppe

class SanityFiltersKtTest : FunSpec({
    context("utledInnsatsgrupper") {
        test("utledInnsatsgrupper for standard innsats skal returnere korrekt liste med innsatsgrupper") {
            val innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS.name
            val result = utledInnsatsgrupper(innsatsgruppe)
            result shouldBe listOf(Innsatsgruppe.STANDARD_INNSATS.name)
        }

        test("utledInnsatsgrupper for situasjonsbestemt innsats skal returnere korrekt liste med innsatsgrupper") {
            val innsatsgruppe = Innsatsgruppe.SITUASJONSBESTEMT_INNSATS.name
            val result = utledInnsatsgrupper(innsatsgruppe)
            result shouldBe listOf(Innsatsgruppe.STANDARD_INNSATS.name, Innsatsgruppe.SITUASJONSBESTEMT_INNSATS.name)
        }

        test("utledInnsatsgrupper for spesielt tilpasset innsats skal returnere korrekt liste med innsatsgrupper") {
            val innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS.name
            val result = utledInnsatsgrupper(innsatsgruppe)
            result shouldBe listOf(
                Innsatsgruppe.STANDARD_INNSATS.name,
                Innsatsgruppe.SITUASJONSBESTEMT_INNSATS.name,
                Innsatsgruppe.SPESIELT_TILPASSET_INNSATS.name,
            )
        }

        test("utledInnsatsgrupper for varig tilpasset innsats skal returnere korrekt liste med innsatsgrupper") {
            val innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS.name
            val result = utledInnsatsgrupper(innsatsgruppe)
            result shouldBe listOf(
                Innsatsgruppe.STANDARD_INNSATS.name,
                Innsatsgruppe.SITUASJONSBESTEMT_INNSATS.name,
                Innsatsgruppe.SPESIELT_TILPASSET_INNSATS.name,
                Innsatsgruppe.VARIG_TILPASSET_INNSATS.name,
            )
        }
    }
})
