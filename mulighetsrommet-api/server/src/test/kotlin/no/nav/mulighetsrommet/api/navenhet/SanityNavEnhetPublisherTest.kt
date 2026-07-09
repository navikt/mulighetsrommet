package no.nav.mulighetsrommet.api.navenhet

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhet
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetType
import no.nav.mulighetsrommet.api.sanity.EnhetSlug
import no.nav.mulighetsrommet.api.sanity.FylkeRef
import no.nav.mulighetsrommet.api.sanity.SanityEnhet
import no.nav.mulighetsrommet.model.NavEnhetNummer

class SanityNavEnhetPublisherTest : FunSpec({

    val publisher = SanityNavEnhetPublisher(
        sanityService = mockk(relaxed = true),
        slackNotifier = mockk(relaxed = true),
    )

    fun createEnhet(enhet: NavEnhetNummer, type: NavEnhetType, overordnetEnhet: NavEnhetNummer? = null) = NavEnhet(
        enhetsnummer = enhet,
        navn = "Enhet $enhet",
        status = NavEnhetStatus.AKTIV,
        type = type,
        overordnetEnhet = overordnetEnhet,
    )

    test("skal utlede LOKAL- og FYLKE-enheter for synkronisering til sanity") {
        val mockEnheter = listOf(
            createEnhet(NavEnhetNummer("1000"), NavEnhetType.AAREG, NavEnhetNummer("1200")),
            createEnhet(NavEnhetNummer("1001"), NavEnhetType.LOKAL, NavEnhetNummer("1200")),
            createEnhet(NavEnhetNummer("1200"), NavEnhetType.FYLKE),
            createEnhet(NavEnhetNummer("1291"), NavEnhetType.ALS),
            createEnhet(NavEnhetNummer("0387"), NavEnhetType.TILTAK),
            createEnhet(NavEnhetNummer("1002"), NavEnhetType.LOKAL, NavEnhetNummer("1200")),
        )

        val tilSanity = publisher.utledEnheterTilSanity(mockEnheter)

        tilSanity shouldBe listOf(
            SanityEnhet(
                _id = "enhet.fylke.1200",
                navn = "Enhet 1200",
                type = "Fylke",
                nummer = EnhetSlug(current = "1200"),
                status = "Aktiv",
                fylke = null,
            ),
            SanityEnhet(
                _id = "enhet.lokal.1001",
                navn = "Enhet 1001",
                type = "Lokal",
                nummer = EnhetSlug(current = "1001"),
                status = "Aktiv",
                fylke = FylkeRef(_ref = "enhet.fylke.1200", _key = "1200"),
            ),
            SanityEnhet(
                _id = "enhet.lokal.1002",
                navn = "Enhet 1002",
                type = "Lokal",
                nummer = EnhetSlug(current = "1002"),
                status = "Aktiv",
                fylke = FylkeRef(_ref = "enhet.fylke.1200", _key = "1200"),
            ),
        )
    }
})
