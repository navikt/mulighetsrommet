package no.nav.mulighetsrommet.api.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.domain.dto.Innsatsgruppe

class SanityFiltersKtTest : FunSpec({
    context("utledInnsatsgrupper") {
        test("utledInnsatsgrupper for standard innsats skal returnere korrekt liste med innsatsgrupper") {
            val innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS
            val result = utledInnsatsgrupper(innsatsgruppe)
            result shouldBe listOf(Innsatsgruppe.STANDARD_INNSATS)
        }

        test("utledInnsatsgrupper for situasjonsbestemt innsats skal returnere korrekt liste med innsatsgrupper") {
            val innsatsgruppe = Innsatsgruppe.SITUASJONSBESTEMT_INNSATS
            val result = utledInnsatsgrupper(innsatsgruppe)
            result shouldBe listOf(Innsatsgruppe.STANDARD_INNSATS, Innsatsgruppe.SITUASJONSBESTEMT_INNSATS)
        }

        test("utledInnsatsgrupper for spesielt tilpasset innsats skal returnere korrekt liste med innsatsgrupper") {
            val innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS
            val result = utledInnsatsgrupper(innsatsgruppe)
            result shouldBe listOf(
                Innsatsgruppe.STANDARD_INNSATS,
                Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
                Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            )
        }

        test("utledInnsatsgrupper for varig tilpasset innsats skal returnere korrekt liste med innsatsgrupper") {
            val innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS
            val result = utledInnsatsgrupper(innsatsgruppe)
            result shouldBe listOf(
                Innsatsgruppe.STANDARD_INNSATS,
                Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
                Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
                Innsatsgruppe.VARIG_TILPASSET_INNSATS,
            )
        }
    }
})
