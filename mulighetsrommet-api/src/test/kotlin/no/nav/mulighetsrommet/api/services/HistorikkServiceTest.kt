package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.arena.VeilarbarenaClient
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseListener
import no.nav.mulighetsrommet.database.kotest.extensions.createApiDatabaseTestSchema
import no.nav.mulighetsrommet.domain.models.*
import java.time.LocalDateTime
import java.util.*

class HistorikkServiceTest : FunSpec({
    testOrder = TestCaseOrder.Sequential

    val arrangorService: ArrangorService = mockk()
    val veilarbarenaClient: VeilarbarenaClient = mockk()

    val listener = extension(FlywayDatabaseListener(createApiDatabaseTestSchema()))

    beforeSpec {
        val tiltakstypeRepository = TiltakstypeRepository(listener.db)
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(listener.db)
        val deltakerRepository = DeltakerRepository(listener.db)
        val service = ArenaService(tiltakstypeRepository, tiltaksgjennomforingRepository, deltakerRepository)

        val tiltakstype = Tiltakstype(
            id = UUID.randomUUID(),
            navn = "Arbeidstrening",
            tiltakskode = "ARBTREN",
        )

        val tiltaksgjennomforing = Tiltaksgjennomforing(
            id = UUID.randomUUID(),
            navn = "Arbeidstrening",
            tiltakstypeId = tiltakstype.id,
            tiltaksnummer = "12345"
        )

        val deltaker = Deltaker(
            id = UUID.randomUUID(),
            tiltaksgjennomforingId = tiltaksgjennomforing.id,
            fnr = "12345678910",
            status = Deltakerstatus.VENTER,
            fraDato = LocalDateTime.now(),
            tilDato = LocalDateTime.now().plusYears(1),
            virksomhetsnr = "123456789"
        )

        service.createOrUpdate(tiltakstype)
        service.createOrUpdate(tiltaksgjennomforing)
        service.createOrUpdate(deltaker)
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
            ""
        ) shouldBe forventetHistorikk
    }
})
