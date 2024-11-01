package no.nav.mulighetsrommet.api.navenhet

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.norg2.*
import no.nav.mulighetsrommet.api.domain.dto.EnhetSlug
import no.nav.mulighetsrommet.api.domain.dto.FylkeRef
import no.nav.mulighetsrommet.api.domain.dto.SanityEnhet
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetRepository
import no.nav.mulighetsrommet.api.services.cms.SanityService
import no.nav.mulighetsrommet.slack.SlackNotifier

class NavEnheterSyncServiceTest : FunSpec({
    val norg2Client: Norg2Client = mockk()
    val enheter: NavEnhetRepository = mockk(relaxed = true)
    val slackNotifier: SlackNotifier = mockk(relaxed = true)
    val sanityService: SanityService = mockk(relaxed = true)
    val navEnheterSyncService = NavEnheterSyncService(norg2Client, sanityService, enheter, slackNotifier)

    fun createEnhet(enhet: String, type: Norg2Type) = Norg2EnhetDto(
        enhetId = Math.random().toInt(),
        enhetNr = enhet,
        navn = "Enhet $enhet",
        status = Norg2EnhetStatus.AKTIV,
        type = type,
    )

    test("skal utlede LOKAL- og FYLKE-enheter for synkronisering til sanity") {
        val mockEnheter = listOf(
            Norg2Response(
                enhet = createEnhet("1000", Norg2Type.AAREG),
                overordnetEnhet = "1200",
            ),
            Norg2Response(
                enhet = createEnhet("1001", Norg2Type.LOKAL),
                overordnetEnhet = "1200",
            ),
            Norg2Response(
                enhet = createEnhet("1200", Norg2Type.FYLKE),
                overordnetEnhet = null,
            ),
            Norg2Response(
                enhet = createEnhet("1291", Norg2Type.ALS),
                overordnetEnhet = null,
            ),
            Norg2Response(
                enhet = createEnhet("0387", Norg2Type.TILTAK),
                overordnetEnhet = null,
            ),
            Norg2Response(
                enhet = createEnhet("1002", Norg2Type.LOKAL),
                overordnetEnhet = "1200",
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
