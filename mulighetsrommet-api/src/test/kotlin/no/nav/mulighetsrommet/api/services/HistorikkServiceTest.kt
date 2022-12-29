package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createApiDatabaseTestSchema
import no.nav.mulighetsrommet.domain.models.*
import java.time.LocalDateTime
import java.util.*

class HistorikkServiceTest : FunSpec({
    testOrder = TestCaseOrder.Sequential

    val arrangorService: ArrangorService = mockk()

    val database = extension(FlywayDatabaseTestListener(createApiDatabaseTestSchema()))

    val tiltakstype = Tiltakstype(
        id = UUID.randomUUID(),
        navn = "Arbeidstrening",
        tiltakskode = "ARBTREN",
    )

    val tiltaksgjennomforing = Tiltaksgjennomforing(
        id = UUID.randomUUID(),
        navn = "Arbeidstrening",
        tiltakstypeId = tiltakstype.id,
        tiltaksnummer = "12345",
        virksomhetsnummer = "123456789",
        enhet = "2990"
    )

    val deltaker = Deltaker(
        id = UUID.randomUUID(),
        tiltaksgjennomforingId = tiltaksgjennomforing.id,
        norskIdent = "12345678910",
        status = Deltakerstatus.VENTER,
        fraDato = LocalDateTime.of(2018, 12, 3, 0, 0),
        tilDato = LocalDateTime.of(2019, 12, 3, 0, 0)
    )

    beforeSpec {
        val tiltakstypeRepository = TiltakstypeRepository(database.db)
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val deltakerRepository = DeltakerRepository(database.db)
        val service = ArenaService(tiltakstypeRepository, tiltaksgjennomforingRepository, deltakerRepository)

        service.upsert(tiltakstype)
        service.upsert(tiltaksgjennomforing)
        service.upsert(deltaker)
    }

    test("henter historikk for bruker basert på person id med arrangørnavn") {
        val bedriftsnavn = "Bedriftsnavn"
        coEvery { arrangorService.hentArrangornavn(any()) } returns bedriftsnavn

        val historikkService =
            HistorikkService(database.db, arrangorService)

        val forventetHistorikk = listOf(
            HistorikkForDeltakerDTO(
                id = deltaker.id,
                fraDato = LocalDateTime.of(2018, 12, 3, 0, 0),
                tilDato = LocalDateTime.of(2019, 12, 3, 0, 0),
                status = Deltakerstatus.VENTER,
                tiltaksnavn = "Arbeidstrening",
                tiltaksnummer = "12345",
                tiltakstype = "Arbeidstrening",
                arrangor = bedriftsnavn
            )
        )

        historikkService.hentHistorikkForBruker(
            "12345678910"
        ) shouldBe forventetHistorikk
    }
})
