package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.mockk.mockk
import io.mockk.verifyAll
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.domain.dto.TiltakstypeEksternDto
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus
import no.nav.mulighetsrommet.domain.dto.Tiltakstypestatus
import no.nav.mulighetsrommet.kafka.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.kafka.producers.TiltakstypeKafkaProducer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class KafkaSyncServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val lastSuccessDate = LocalDate.of(2023, 2, 14)
    val today = LocalDate.of(2023, 2, 16)

    val tiltakstype = TiltakstypeFixtures.Oppfolging.copy(
        fraDato = LocalDate.of(2023, 1, 11),
        tilDato = LocalDate.now().plusYears(1),
    )

    context("oppdater statuser på tiltaksgjennomføringer") {
        val (kafkaSyncService, _, tiltaksgjennomforingKafkaProducer) = createService(database.db)

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

            kafkaSyncService.oppdaterTiltaksgjennomforingStatus(today, lastSuccessDate)

            verifyAll {
                tiltaksgjennomforingKafkaProducer.publish(startdatoInnenfor.toDto(TiltaksgjennomforingStatus.GJENNOMFORES))
                tiltaksgjennomforingKafkaProducer.publish(sluttdatoInnenfor.toDto(TiltaksgjennomforingStatus.AVSLUTTET))
            }
        }
    }

    context("oppdater statuser på tiltakstyper") {
        val (kafkaSyncService, tiltakstypeKafkaProducer) = createService(database.db)

        fun TiltakstypeDbo.toDto(tiltakstypestatus: Tiltakstypestatus): TiltakstypeEksternDto {
            return TiltakstypeEksternDto(
                id = id,
                navn = navn,
                arenaKode = arenaKode,
                tiltakskode = Tiltakskode.fromArenaKode(arenaKode)!!,
                registrertIArenaDato = registrertDatoIArena,
                sistEndretIArenaDato = sistEndretDatoIArena,
                fraDato = fraDato,
                tilDato = tilDato,
                rettPaaTiltakspenger = rettPaaTiltakspenger,
                status = tiltakstypestatus,
                deltakerRegistreringInnhold = null,
            )
        }

        val startdatoInnenfor = tiltakstype.copy(
            id = UUID.randomUUID(),
            arenaKode = "AVKLARAG",
            fraDato = LocalDate.of(2023, 2, 15),
        )
        val sluttdatoInnenfor = tiltakstype.copy(
            id = UUID.randomUUID(),
            arenaKode = "GRUPPEAMO",
            fraDato = LocalDate.of(2023, 2, 13),
            tilDato = lastSuccessDate,
        )
        val domain = MulighetsrommetTestDomain(
            tiltakstyper = listOf(tiltakstype, startdatoInnenfor, sluttdatoInnenfor),
            avtaler = listOf(),
            gjennomforinger = listOf(),
        )

        test("oppdater statuser på kafka på relevante tiltakstyper") {
            domain.initialize(database.db)

            kafkaSyncService.oppdaterTiltakstypestatus(today, lastSuccessDate)

            verifyAll {
                tiltakstypeKafkaProducer.publish(
                    startdatoInnenfor.toDto(Tiltakstypestatus.Aktiv),
                )
                tiltakstypeKafkaProducer.publish(
                    sluttdatoInnenfor.toDto(Tiltakstypestatus.Avsluttet),
                )
            }
        }
    }
})

private fun createService(db: Database): Triple<KafkaSyncService, TiltakstypeKafkaProducer, TiltaksgjennomforingKafkaProducer> {
    val tiltakstypeKafkaProducer = mockk<TiltakstypeKafkaProducer>(relaxed = true)
    val tiltaksgjennomforingKafkaProducer = mockk<TiltaksgjennomforingKafkaProducer>(relaxed = true)
    val kafkaSyncService = KafkaSyncService(
        tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(db),
        tiltakstypeRepository = TiltakstypeRepository(db),
        tiltaksgjennomforingKafkaProducer = tiltaksgjennomforingKafkaProducer,
        tiltakstypeKafkaProducer = tiltakstypeKafkaProducer,
    )
    return Triple(kafkaSyncService, tiltakstypeKafkaProducer, tiltaksgjennomforingKafkaProducer)
}
