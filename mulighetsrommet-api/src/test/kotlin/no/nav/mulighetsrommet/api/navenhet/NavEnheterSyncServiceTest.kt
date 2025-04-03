package no.nav.mulighetsrommet.api.navenhet

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.norg2.Norg2EnhetDto
import no.nav.mulighetsrommet.api.clients.norg2.Norg2EnhetStatus
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Response
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.sanity.EnhetSlug
import no.nav.mulighetsrommet.api.sanity.FylkeRef
import no.nav.mulighetsrommet.api.sanity.SanityEnhet
import no.nav.mulighetsrommet.model.NavEnhetNummer

class NavEnheterSyncServiceTest : FunSpec({

    val navEnheterSyncService = NavEnheterSyncService(
        db = mockk(relaxed = true),
        norg2Client = mockk(relaxed = true),
        sanityService = mockk(relaxed = true),
        slackNotifier = mockk(relaxed = true),
    )

    fun createEnhet(enhet: NavEnhetNummer, type: Norg2Type) = Norg2EnhetDto(
        enhetId = Math.random().toInt(),
        enhetNr = enhet,
        navn = "Enhet $enhet",
        status = Norg2EnhetStatus.AKTIV,
        type = type,
    )

    test("skal utlede LOKAL- og FYLKE-enheter for synkronisering til sanity") {
        val mockEnheter = listOf(
            Norg2Response(
                enhet = createEnhet(NavEnhetNummer("1000"), Norg2Type.AAREG),
                overordnetEnhet = NavEnhetNummer("1200"),
            ),
            Norg2Response(
                enhet = createEnhet(NavEnhetNummer("1001"), Norg2Type.LOKAL),
                overordnetEnhet = NavEnhetNummer("1200"),
            ),
            Norg2Response(
                enhet = createEnhet(NavEnhetNummer("1200"), Norg2Type.FYLKE),
                overordnetEnhet = null,
            ),
            Norg2Response(
                enhet = createEnhet(NavEnhetNummer("1291"), Norg2Type.ALS),
                overordnetEnhet = null,
            ),
            Norg2Response(
                enhet = createEnhet(NavEnhetNummer("0387"), Norg2Type.TILTAK),
                overordnetEnhet = null,
            ),
            Norg2Response(
                enhet = createEnhet(NavEnhetNummer("1002"), Norg2Type.LOKAL),
                overordnetEnhet = NavEnhetNummer("1200"),
            ),
        )

        val tilSanity = navEnheterSyncService.utledEnheterTilSanity(mockEnheter)

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
