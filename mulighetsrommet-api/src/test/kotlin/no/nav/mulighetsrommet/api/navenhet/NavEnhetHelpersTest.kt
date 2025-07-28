package no.nav.mulighetsrommet.api.navenhet

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.NavEnhetNummer

class NavEnhetHelpersTest : FunSpec({

    test("buildNavRegioner skal gruppere enheter etter fylke og inkludere underenheter") {
        val enheter = listOf(
            NavEnhetDto("Fylke 1", NavEnhetNummer("0400"), NavEnhetType.FYLKE, null),
            NavEnhetDto("Lokal 1", NavEnhetNummer("0401"), NavEnhetType.LOKAL, NavEnhetNummer("0400")),
            NavEnhetDto("Lokal 2", NavEnhetNummer("0402"), NavEnhetType.LOKAL, NavEnhetNummer("0400")),
            NavEnhetDto("Kø 1", NavEnhetNummer("0499"), NavEnhetType.KO, NavEnhetNummer("0400")),
            NavEnhetDto("Fylke 2", NavEnhetNummer("0500"), NavEnhetType.FYLKE, null),
            NavEnhetDto("Lokal 3", NavEnhetNummer("0501"), NavEnhetType.LOKAL, NavEnhetNummer("0500")),
        )

        val result = NavEnhetHelpers.buildNavRegioner(enheter)

        result shouldBe listOf(
            NavRegionDto(
                enhetsnummer = NavEnhetNummer("0400"),
                navn = "Fylke 1",
                enheter = listOf(
                    NavRegionUnderenhetDto("Lokal 1", NavEnhetNummer("0401"), NavEnhetNummer("0400"), true),
                    NavRegionUnderenhetDto("Lokal 2", NavEnhetNummer("0402"), NavEnhetNummer("0400"), true),
                    NavRegionUnderenhetDto("Kø 1", NavEnhetNummer("0499"), NavEnhetNummer("0400"), false),
                ),
            ),
            NavRegionDto(
                enhetsnummer = NavEnhetNummer("0500"),
                navn = "Fylke 2",
                enheter = listOf(
                    NavRegionUnderenhetDto("Lokal 3", NavEnhetNummer("0501"), NavEnhetNummer("0500"), true),
                ),
            ),
        )
    }
})
