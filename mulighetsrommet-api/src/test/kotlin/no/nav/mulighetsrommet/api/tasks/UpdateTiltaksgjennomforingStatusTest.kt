package no.nav.mulighetsrommet.api.tasks

import io.kotest.core.spec.style.FunSpec
import io.mockk.mockk
import io.mockk.verifyAll
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus
import no.nav.mulighetsrommet.kafka.producers.TiltaksgjennomforingKafkaProducer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class UpdateTiltaksgjennomforingStatusTest : FunSpec({

    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val lastSuccessDate = LocalDate.of(2023, 2, 14)
    val today = LocalDate.of(2023, 2, 16)

    val tiltakstype = TiltakstypeFixtures.Oppfolging.copy(
        fraDato = LocalDate.of(2023, 1, 11),
        tilDato = LocalDate.now().plusYears(1),
    )

    context("oppdater statuser på tiltaksgjennomføringer") {
        val tiltaksgjennomforingKafkaProducer = mockk<TiltaksgjennomforingKafkaProducer>(relaxed = true)
        val task = UpdateTiltaksgjennomforingStatus(
            mockk(),
            TiltaksgjennomforingRepository(database.db),
            tiltaksgjennomforingKafkaProducer,
        )

        fun TiltaksgjennomforingDbo.toDto(status: TiltaksgjennomforingStatus): TiltaksgjennomforingDto {
            return TiltaksgjennomforingDto(
                id = id,
                tiltakstype = TiltaksgjennomforingDto.Tiltakstype(
                    id = tiltakstype.id,
                    navn = tiltakstype.navn,
                    arenaKode = tiltakstype.arenaKode,
                ),
                navn = navn,
                virksomhetsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                startDato = startDato,
                sluttDato = sluttDato,
                status = status,
                oppstart = oppstart,
            )
        }

        val startdatoInnenforMenAvsluttetStatus = TiltaksgjennomforingFixtures.Oppfolging1.copy(
            id = UUID.randomUUID(),
            startDato = LocalDate.of(2023, 2, 15),
            sluttDato = LocalDate.now().plusYears(1),
        )
        val startdatoInnenfor = TiltaksgjennomforingFixtures.Oppfolging1.copy(
            id = UUID.randomUUID(),
            startDato = LocalDate.of(2023, 2, 15),
            sluttDato = LocalDate.now().plusYears(1),
        )
        val sluttdatoInnenforMenAvbruttStatus = TiltaksgjennomforingFixtures.Oppfolging1.copy(
            id = UUID.randomUUID(),
            startDato = lastSuccessDate,
            sluttDato = lastSuccessDate,
        )
        val sluttdatoInnenfor = TiltaksgjennomforingFixtures.Oppfolging1.copy(
            id = UUID.randomUUID(),
            startDato = lastSuccessDate,
            sluttDato = lastSuccessDate,
        )
        val datoerUtenfor = TiltaksgjennomforingFixtures.Oppfolging1.copy(
            id = UUID.randomUUID(),
            startDato = lastSuccessDate,
            sluttDato = LocalDate.now().plusYears(1),
        )
        val domain = MulighetsrommetTestDomain(
            tiltakstyper = listOf(tiltakstype),
            avtaler = listOf(AvtaleFixtures.oppfolging),
            gjennomforinger = listOf(
                startdatoInnenforMenAvsluttetStatus,
                startdatoInnenfor,
                sluttdatoInnenforMenAvbruttStatus,
                sluttdatoInnenfor,
                datoerUtenfor,
            ),
        )

        test("oppdater statuser på kafka på relevante tiltaksgjennomføringer") {
            domain.initialize(database.db)

            val gjennomforinger = TiltaksgjennomforingRepository(database.db)
            gjennomforinger.setAvbruttTidspunkt(startdatoInnenforMenAvsluttetStatus.id, LocalDateTime.now())
            gjennomforinger.setAvbruttTidspunkt(sluttdatoInnenforMenAvbruttStatus.id, LocalDateTime.now())

            task.oppdaterTiltaksgjennomforingStatus(today, lastSuccessDate)

            verifyAll {
                tiltaksgjennomforingKafkaProducer.publish(startdatoInnenfor.toDto(TiltaksgjennomforingStatus.GJENNOMFORES))
                tiltaksgjennomforingKafkaProducer.publish(sluttdatoInnenfor.toDto(TiltaksgjennomforingStatus.AVSLUTTET))
            }
        }
    }
})
