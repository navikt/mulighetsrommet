package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.api.producers.TiltakstypeKafkaProducer
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createApiDatabaseTestSchema
import no.nav.mulighetsrommet.domain.dbo.HistorikkDbo
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.domain.dto.Deltakerstatus
import no.nav.mulighetsrommet.domain.models.HistorikkForDeltakerDTO
import java.time.LocalDateTime
import java.util.*

class HistorikkServiceTest : FunSpec({
    testOrder = TestCaseOrder.Sequential

    val arrangorService: ArrangorService = mockk()

    val database =
        extension(FlywayDatabaseTestListener(createApiDatabaseTestSchema()))

    val tiltakstype = TiltakstypeDbo(
        id = UUID.randomUUID(),
        navn = "Arbeidstrening",
        tiltakskode = "ARBTREN"
    )

    val tiltaksgjennomforing = TiltaksgjennomforingDbo(
        id = UUID.randomUUID(),
        navn = "Arbeidstrening",
        tiltakstypeId = tiltakstype.id,
        tiltaksnummer = "12345",
        virksomhetsnummer = "123456789",
        enhet = "2990"
    )

    val deltaker = HistorikkDbo.Gruppetiltak(
        id = UUID.randomUUID(),
        tiltaksgjennomforingId = tiltaksgjennomforing.id,
        norskIdent = "12345678910",
        status = Deltakerstatus.VENTER,
        fraDato = LocalDateTime.of(2018, 12, 3, 0, 0),
        tilDato = LocalDateTime.of(2019, 12, 3, 0, 0)
    )

    val tiltakstypeIndividuell = TiltakstypeDbo(
        id = UUID.randomUUID(),
        navn = "Høyere utdanning",
        tiltakskode = "HOYEREUTD"
    )

    val deltakerIndividuell = HistorikkDbo.IndividueltTiltak(
        id = UUID.randomUUID(),
        norskIdent = "12345678910",
        status = Deltakerstatus.VENTER,
        fraDato = LocalDateTime.of(2018, 12, 3, 0, 0),
        tilDato = LocalDateTime.of(2019, 12, 3, 0, 0),
        beskrivelse = "Utdanning",
        tiltakstypeId = tiltakstypeIndividuell.id,
        virksomhetsnummer = "12343",
    )

    beforeSpec {
        val tiltakstypeRepository = TiltakstypeRepository(database.db)
        val tiltaksgjennomforingRepository =
            TiltaksgjennomforingRepository(database.db)
        val deltakerRepository = DeltakerRepository(database.db)
        val tiltaksgjennomforingKafkaProducer = mockk<TiltaksgjennomforingKafkaProducer>(relaxed = true)
        val tiltakstypeKafkaProducer = mockk<TiltakstypeKafkaProducer>(relaxed = true)
        val service = ArenaService(tiltakstypeRepository, tiltaksgjennomforingRepository, deltakerRepository, tiltaksgjennomforingKafkaProducer, tiltakstypeKafkaProducer)

        service.upsert(tiltakstype)
        service.upsert(tiltaksgjennomforing)
        service.upsert(deltaker)
        service.upsert(tiltakstypeIndividuell)
        service.upsert(deltakerIndividuell)
    }

    test("henter historikk for bruker basert på person id med arrangørnavn") {
        val bedriftsnavn = "Bedriftsnavn"
        val bedriftsnavn2 = "Bedriftsnavn 2"
        coEvery { arrangorService.hentArrangornavn("123456789") } returns bedriftsnavn
        coEvery { arrangorService.hentArrangornavn("12343") } returns bedriftsnavn2

        val historikkService =
            HistorikkService(database.db, arrangorService)

        val forventetHistorikk = listOf(
            HistorikkForDeltakerDTO(
                id = deltaker.id,
                fraDato = LocalDateTime.of(2018, 12, 3, 0, 0),
                tilDato = LocalDateTime.of(2019, 12, 3, 0, 0),
                status = Deltakerstatus.VENTER,
                tiltaksnavn = "Arbeidstrening",
                tiltakstype = "Arbeidstrening",
                arrangor = bedriftsnavn
            ),
            HistorikkForDeltakerDTO(
                id = deltakerIndividuell.id,
                fraDato = LocalDateTime.of(2018, 12, 3, 0, 0),
                tilDato = LocalDateTime.of(2019, 12, 3, 0, 0),
                status = Deltakerstatus.VENTER,
                tiltaksnavn = "Utdanning",
                tiltakstype = "Høyere utdanning",
                arrangor = bedriftsnavn2
            )
        )

        historikkService.hentHistorikkForBruker(
            "12345678910"
        ) shouldBe forventetHistorikk
    }
})
