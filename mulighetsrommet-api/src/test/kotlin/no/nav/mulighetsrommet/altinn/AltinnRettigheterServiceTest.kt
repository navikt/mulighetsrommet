package no.nav.mulighetsrommet.altinn

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.mulighetsrommet.altinn.model.AltinnRessurs
import no.nav.mulighetsrommet.altinn.model.BedriftRettigheter
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.time.Duration

class AltinnRettigheterServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val norskIdent = NorskIdent("12345678901")

    val rettigheter1 = listOf(
        BedriftRettigheter(
            organisasjonsnummer = Organisasjonsnummer("123456789"),
            rettigheter = listOf(AltinnRessurs.TILTAK_ARRANGOR_BE_OM_UTBETALING),
        ),
    )

    val rettigheter2 = listOf(
        BedriftRettigheter(
            organisasjonsnummer = Organisasjonsnummer("123456789"),
            rettigheter = listOf(AltinnRessurs.TILTAK_ARRANGOR_BE_OM_UTBETALING),
        ),
        BedriftRettigheter(
            organisasjonsnummer = Organisasjonsnummer("123123123"),
            rettigheter = listOf(AltinnRessurs.TILTAK_ARRANGOR_BE_OM_UTBETALING),
        ),
    )

    fun createService(expiry: Duration, altinnClient: AltinnClient) = AltinnRettigheterService(
        db = database.db,
        altinnClient = altinnClient,
        config = AltinnRettigheterService.Config(
            rettighetExpiryDuration = expiry,
        ),
    )

    afterEach {
        database.run {
            queries.altinnRettigheter.deleteAll()
        }
    }

    test("henter rettigheter fra altinn med cache fra databasen") {
        val altinnClient = mockk<AltinnClient>()
        coEvery { altinnClient.hentRettigheter(norskIdent) } returns rettigheter1

        val service = createService(expiry = Duration.ofMinutes(1), altinnClient)

        service.getRettigheter(norskIdent) shouldBe rettigheter1
        coVerify(exactly = 1) { altinnClient.hentRettigheter(norskIdent) }

        coEvery { altinnClient.hentRettigheter(norskIdent) } returns rettigheter2

        service.getRettigheter(norskIdent) shouldBe rettigheter1
        coVerify(exactly = 1) { altinnClient.hentRettigheter(norskIdent) }
    }

    test("henter rettigheter på nytt fra altinn når expiry er utløpt") {
        val altinnClient = mockk<AltinnClient>()
        coEvery { altinnClient.hentRettigheter(norskIdent) } returns rettigheter1

        val service = createService(expiry = Duration.ofSeconds(0), altinnClient)

        service.getRettigheter(norskIdent) shouldBe rettigheter1
        coVerify(exactly = 1) { altinnClient.hentRettigheter(norskIdent) }

        coEvery { altinnClient.hentRettigheter(norskIdent) } returns rettigheter2

        service.getRettigheter(norskIdent) shouldBe rettigheter2
        coVerify(exactly = 2) { altinnClient.hentRettigheter(norskIdent) }
    }
})
