package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.arena.VeilarbarenaClient
import no.nav.mulighetsrommet.api.repositories.ArenaRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseListener
import no.nav.mulighetsrommet.database.kotest.extensions.createApiDatabaseTestSchema
import no.nav.mulighetsrommet.domain.adapter.AdapterSak
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltak
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltakdeltaker
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltaksgjennomforing
import no.nav.mulighetsrommet.domain.models.Deltakerstatus
import no.nav.mulighetsrommet.domain.models.HistorikkForDeltakerDTO
import java.time.LocalDateTime

class HistorikkServiceTest : FunSpec({
    testOrder = TestCaseOrder.Sequential

    val arrangorService: ArrangorService = mockk()
    val veilarbarenaClient: VeilarbarenaClient = mockk()

    val listener = extension(FlywayDatabaseListener(createApiDatabaseTestSchema()))

    beforeSpec {
        val arenaRepository = ArenaRepository(listener.db)

        val tiltakstype = AdapterTiltak(
            navn = "Arbeidstrening",
            innsatsgruppe = 1,
            tiltakskode = "ARBTREN",
            fraDato = LocalDateTime.now(),
            tilDato = LocalDateTime.now().plusYears(1)
        )

        val tiltaksgjennomforing = AdapterTiltaksgjennomforing(
            navn = "Arbeidstrening",
            arrangorId = 1,
            tiltakskode = "ARBTREN",
            id = 123,
            sakId = 123
        )

        val deltaker = AdapterTiltakdeltaker(
            id = 123,
            tiltaksgjennomforingId = 123,
            personId = 111,
            fraDato = LocalDateTime.of(2018, 12, 3, 0, 0),
            tilDato = LocalDateTime.of(2019, 12, 3, 0, 0),
            status = Deltakerstatus.VENTER
        )

        val sak = AdapterSak(
            id = 123,
            lopenummer = 3,
            aar = 2022
        )

        arenaRepository.upsertTiltakstype(tiltakstype)
        arenaRepository.upsertTiltaksgjennomforing(tiltaksgjennomforing)
        arenaRepository.upsertDeltaker(deltaker)
        arenaRepository.updateTiltaksgjennomforingWithSak(sak)
    }

    test("henter historikk for bruker basert på person id med arrangørnavn") {
        val bedriftsnavn = "Bedriftsnavn"
        coEvery { arrangorService.hentArrangornavn(1) } returns bedriftsnavn
        coEvery {
            veilarbarenaClient.hentPersonIdForFnr(
                any(),
                any()
            )
        } returns "111"

        val historikkService =
            HistorikkService(listener.db, veilarbarenaClient, arrangorService)

        val forventetHistorikk = listOf(
            HistorikkForDeltakerDTO(
                id = "1",
                fraDato = LocalDateTime.of(2018, 12, 3, 0, 0),
                tilDato = LocalDateTime.of(2019, 12, 3, 0, 0),
                status = Deltakerstatus.VENTER,
                tiltaksnavn = "Arbeidstrening",
                tiltaksnummer = "3",
                tiltakstype = "Arbeidstrening",
                arrangor = bedriftsnavn
            )
        )

        historikkService.hentHistorikkForBruker(
            "fnr",
            null
        ) shouldBe forventetHistorikk
    }
})
