package no.nav.mulighetsrommet.api.tasks

import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyAll
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dto.TiltakstypeEksternDto
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.domain.dto.TiltakstypeStatus
import no.nav.mulighetsrommet.kafka.producers.TiltakstypeKafkaProducer
import java.time.LocalDate
import java.util.*

class UpdateTiltakstypeStatusTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val lastSuccessDate = LocalDate.of(2023, 2, 14)
    val today = LocalDate.of(2023, 2, 16)

    val tiltakstype = TiltakstypeFixtures.Oppfolging.copy(
        startDato = LocalDate.of(2023, 1, 11),
        sluttDato = LocalDate.now().plusYears(1),
    )

    context("oppdater statuser på tiltakstyper") {
        val tiltakstypeKafkaProducer = mockk<TiltakstypeKafkaProducer>(relaxed = true)
        val kafkaSyncService = UpdateTiltakstypeStatus(
            mockk(),
            tiltakstypeRepository = TiltakstypeRepository(database.db),
            tiltakstypeKafkaProducer = tiltakstypeKafkaProducer,
        )

        afterEach {
            clearAllMocks()
        }

        fun TiltakstypeDbo.toDto(tiltakstypestatus: TiltakstypeStatus): TiltakstypeEksternDto {
            return TiltakstypeEksternDto(
                id = id,
                navn = navn,
                tiltakskode = Tiltakskode.fromArenaKode(arenaKode)!!,
                arenaKode = arenaKode,
                startDato = startDato,
                sluttDato = sluttDato,
                rettPaaTiltakspenger = rettPaaTiltakspenger,
                status = tiltakstypestatus,
                deltakerRegistreringInnhold = null,
                innsatsgrupper = emptySet(),
            )
        }

        val startdatoInnenfor = tiltakstype.copy(
            id = UUID.randomUUID(),
            arenaKode = "AVKLARAG",
            startDato = LocalDate.of(2023, 2, 15),
        )
        val sluttdatoInnenfor = tiltakstype.copy(
            id = UUID.randomUUID(),
            arenaKode = "GRUPPEAMO",
            startDato = LocalDate.of(2023, 2, 13),
            sluttDato = lastSuccessDate,
        )

        test("oppdater statuser på relevante tiltakstyper") {
            MulighetsrommetTestDomain(
                tiltakstyper = listOf(tiltakstype, startdatoInnenfor, sluttdatoInnenfor),
                avtaler = listOf(),
                gjennomforinger = listOf(),
            ).initialize(database.db)

            kafkaSyncService.oppdaterTiltakstypestatus(today, lastSuccessDate)

            verifyAll {
                tiltakstypeKafkaProducer.publish(
                    startdatoInnenfor.toDto(TiltakstypeStatus.AKTIV),
                )
                tiltakstypeKafkaProducer.publish(
                    sluttdatoInnenfor.toDto(TiltakstypeStatus.AVSLUTTET),
                )
            }
        }

        test("oppdater ikke status på individuelle tiltakstyper") {
            MulighetsrommetTestDomain(
                tiltakstyper = listOf(
                    tiltakstype,
                    startdatoInnenfor.copy(arenaKode = "ARBTREN"),
                    sluttdatoInnenfor.copy(arenaKode = "ENKELAMO"),
                ),
                avtaler = listOf(),
                gjennomforinger = listOf(),
            ).initialize(database.db)

            kafkaSyncService.oppdaterTiltakstypestatus(today, lastSuccessDate)

            verify(exactly = 0) { tiltakstypeKafkaProducer.publish(any()) }
        }
    }
})
