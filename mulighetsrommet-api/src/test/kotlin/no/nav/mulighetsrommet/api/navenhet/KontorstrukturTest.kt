package no.nav.mulighetsrommet.api.navenhet

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.NavEnhetNummer

class KontorstrukturTest : FunSpec({
    test("buildNavRegioner skal gruppere enheter etter fylke og inkludere underenheter") {
        val enheter = listOf(
            NavEnhetDto("Fylke 1", NavEnhetNummer("0400"), NavEnhetType.FYLKE, null),
            NavEnhetDto("Lokal 1", NavEnhetNummer("0401"), NavEnhetType.LOKAL, NavEnhetNummer("0400")),
            NavEnhetDto("Lokal 2", NavEnhetNummer("0402"), NavEnhetType.LOKAL, NavEnhetNummer("0400")),
            NavEnhetDto("Kø 1", NavEnhetNummer("0499"), NavEnhetType.KO, NavEnhetNummer("0400")),
            NavEnhetDto("Fylke 2", NavEnhetNummer("0500"), NavEnhetType.FYLKE, null),
            NavEnhetDto("Lokal 3", NavEnhetNummer("0501"), NavEnhetType.LOKAL, NavEnhetNummer("0500")),
        )

        val result = Kontorstruktur.fromNavEnheter(enheter)

        result shouldBe listOf(
            Kontorstruktur(
                region = Kontorstruktur.Region("Fylke 1", NavEnhetNummer("0400")),
                kontorer = listOf(
                    Kontorstruktur.Kontor("Lokal 1", NavEnhetNummer("0401"), Kontorstruktur.Kontortype.LOKAL),
                    Kontorstruktur.Kontor("Lokal 2", NavEnhetNummer("0402"), Kontorstruktur.Kontortype.LOKAL),
                    Kontorstruktur.Kontor("Kø 1", NavEnhetNummer("0499"), Kontorstruktur.Kontortype.SPESIALENHET),
                ),
            ),
            Kontorstruktur(
                region = Kontorstruktur.Region("Fylke 2", NavEnhetNummer("0500")),
                kontorer = listOf(
                    Kontorstruktur.Kontor("Lokal 3", NavEnhetNummer("0501"), Kontorstruktur.Kontortype.LOKAL),
                ),
            ),
        )
    }

    test("buildNavRegioner skal fjerne dupliserte underenheter og fylker") {
        val enheter = listOf(
            NavEnhetDto("Fylke 1", NavEnhetNummer("0400"), NavEnhetType.FYLKE, null),
            NavEnhetDto("Lokal 1", NavEnhetNummer("0401"), NavEnhetType.LOKAL, NavEnhetNummer("0400")),
            NavEnhetDto("Lokal 1", NavEnhetNummer("0401"), NavEnhetType.LOKAL, NavEnhetNummer("0400")),
            NavEnhetDto("Fylke 1", NavEnhetNummer("0400"), NavEnhetType.FYLKE, null),
            NavEnhetDto("Lokal 1", NavEnhetNummer("0401"), NavEnhetType.LOKAL, NavEnhetNummer("0400")),
        )

        val result = Kontorstruktur.fromNavEnheter(enheter)

        result shouldBe listOf(
            Kontorstruktur(
                region = Kontorstruktur.Region("Fylke 1", NavEnhetNummer("0400")),
                kontorer = listOf(
                    Kontorstruktur.Kontor("Lokal 1", NavEnhetNummer("0401"), Kontorstruktur.Kontortype.LOKAL),
                ),
            ),
        )
    }

    test("buildNavRegioner skal fjerne underenheter som mangler fylke") {
        val enheter = listOf(
            NavEnhetDto("Fylke 1", NavEnhetNummer("0400"), NavEnhetType.FYLKE, null),
            NavEnhetDto("Lokal 1", NavEnhetNummer("0401"), NavEnhetType.LOKAL, NavEnhetNummer("0400")),
            NavEnhetDto("Lokal 1", NavEnhetNummer("0401"), NavEnhetType.LOKAL, NavEnhetNummer("0400")),
            NavEnhetDto("Lokal 3", NavEnhetNummer("0401"), NavEnhetType.LOKAL, NavEnhetNummer("0300")),
        )

        val result = Kontorstruktur.fromNavEnheter(enheter)

        result shouldBe listOf(
            Kontorstruktur(
                region = Kontorstruktur.Region("Fylke 1", NavEnhetNummer("0400")),
                kontorer = listOf(
                    Kontorstruktur.Kontor("Lokal 1", NavEnhetNummer("0401"), Kontorstruktur.Kontortype.LOKAL),
                ),
            ),
        )
    }
})
