package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.norg2.*
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.repositories.NavEnhetRepository
import no.nav.mulighetsrommet.slack.SlackNotifier

class NavEnheterSyncServiceTest : FunSpec({
    val norg2Client: Norg2Client = mockk()
    val enheter: NavEnhetRepository = mockk(relaxed = true)
    val slackNotifier: SlackNotifier = mockk(relaxed = true)
    val sanityClient = SanityClient(
        MockEngine {
            respondOk("Ok")
        },
        SanityClient.Config(projectId = "", dataset = "", apiVersion = "", token = ""),
    )
    val navEnheterSyncService = NavEnheterSyncService(norg2Client, sanityClient, enheter, slackNotifier)

    fun createEnhet(enhet: String, type: Norg2Type) = Norg2EnhetDto(
        enhetId = Math.random().toInt(),
        enhetNr = enhet,
        navn = "Enhet $enhet",
        status = Norg2EnhetStatus.AKTIV,
        type = type,
    )

    test("skal utlede ALS-, LOKAL- og FYLKE-enheter for synkronisering til sanity") {
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
                enhet = createEnhet("1300", Norg2Type.ALS),
                overordnetEnhet = null,
            ),
        )

        val tilSanity = navEnheterSyncService.utledEnheterTilSanity(mockEnheter)

        tilSanity.size shouldBe 3
        tilSanity[0]._id shouldBe "enhet.als.1300"
        tilSanity[1]._id shouldBe "enhet.fylke.1200"
        tilSanity[2]._id shouldBe "enhet.lokal.1001"
        tilSanity[2].fylke?._ref shouldBe "enhet.fylke.1200"
    }
})
