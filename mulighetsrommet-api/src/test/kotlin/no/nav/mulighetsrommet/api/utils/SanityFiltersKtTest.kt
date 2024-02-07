package no.nav.mulighetsrommet.api.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.clients.vedtak.Innsatsgruppe

class SanityFiltersKtTest : FunSpec({

    context("Filter for Groq-spørringer") {
        test("byggTiltakstypeFilter skal returnere tom streng når ingen tiltakstyper er valgt") {
            val tiltakstyper = emptyList<String>()
            val result = byggTiltakstypeFilter(tiltakstyper)
            result shouldBe ""
        }

        test("byggTiltakstypeFilter skal returnere korrekt Groq-uttrykk når minst en tiltakstype er valgt") {
            val tiltakstyper = listOf("1", "2", "3")
            val result = byggTiltakstypeFilter(tiltakstyper)
            result shouldBe "&& tiltakstype->_id in ['1', '2', '3']"
        }

        test("byggSokefilter skal returnere tom streng når søkestrengen er tom") {
            val sokestreng = ""
            val result = byggSokeFilter(sokestreng)
            result shouldBe ""
        }

        test("byggSokefilter skal returnere korrekt Groq-uttrykk når søkestrengen ikke er tom") {
            val sokestreng = "Oppfølging"
            val result = byggSokeFilter(sokestreng)
            result shouldBe "&& [tiltaksgjennomforingNavn, string(tiltaksnummer.current), tiltakstype->tiltakstypeNavn] match \"*Oppfølging*\""
        }

        test("byggInnsatsgruppeFilter skal returnere tom streng når ingen innsatsgrupper er valgt") {
            val innsatsgruppe = null
            val result = byggInnsatsgruppeFilter(innsatsgruppe)
            result shouldBe "&& tiltakstype->innsatsgruppe->nokkel in []"
        }

        test("byggInnsatsgruppeFilter skal returnere korrekt Groq-uttrykk når en innsatsgruppe er gitt") {
            val innsatsgruppe = "SITUASJONSBESTEMT_INNSATS"
            val result = byggInnsatsgruppeFilter(innsatsgruppe)
            result shouldBe "&& tiltakstype->innsatsgruppe->nokkel in ['STANDARD_INNSATS', 'SITUASJONSBESTEMT_INNSATS']"
        }

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
