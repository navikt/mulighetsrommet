package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.mockk.mockk
import io.mockk.verifyAll
import no.nav.mulighetsrommet.api.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.api.producers.TiltakstypeKafkaProducer
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createApiDatabaseTestSchema
import no.nav.mulighetsrommet.domain.dbo.*
import no.nav.mulighetsrommet.domain.dto.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class KafkaSyncServiceTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseTestListener(createApiDatabaseTestSchema()))

    beforeContainer {
        database.db.clean()
        database.db.migrate()
    }

    val lastSuccessDate = LocalDate.of(2023, 2, 14)
    val today = LocalDate.of(2023, 2, 16)

    val tiltakstype = TiltakstypeDbo(
        id = UUID.randomUUID(),
        navn = "Oppfølging",
        tiltakskode = "INDOPPFAG",
        rettPaaTiltakspenger = true,
        registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        sistEndretDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        fraDato = LocalDate.of(2023, 1, 11),
        tilDato = LocalDate.of(2099, 1, 12)
    )

    fun createTiltaksgjennomforing(
        startDato: LocalDate = LocalDate.of(2023, 2, 15),
        sluttDato: LocalDate = LocalDate.of(2023, 11, 11),
        avslutningsstatus: Avslutningsstatus = Avslutningsstatus.AVSLUTTET
    ): TiltaksgjennomforingDbo {
        return TiltaksgjennomforingDbo(
            id = UUID.randomUUID(),
            navn = "Arbeidstrening",
            tiltakstypeId = tiltakstype.id,
            tiltaksnummer = "12345",
            virksomhetsnummer = "123456789",
            startDato = startDato,
            sluttDato = sluttDato,
            enhet = "2990",
            avslutningsstatus = avslutningsstatus
        )
    }

    fun TiltaksgjennomforingDbo.toDto(tiltaksgjennomforingsstatus: Tiltaksgjennomforingsstatus): TiltaksgjennomforingDto {
        return TiltaksgjennomforingDto(
            id = id,
            tiltakstype = TiltaksgjennomforingDto.Tiltakstype(
                id = tiltakstype.id,
                navn = tiltakstype.navn,
                arenaKode = tiltakstype.tiltakskode,
            ),
            navn = navn,
            virksomhetsnummer = virksomhetsnummer,
            startDato = startDato,
            sluttDato = sluttDato,
            status = tiltaksgjennomforingsstatus
        )
    }

    fun TiltakstypeDbo.toDto(tiltakstypestatus: Tiltakstypestatus): TiltakstypeDto {
        return TiltakstypeDto(
            id = id,
            navn = navn,
            arenaKode = tiltakskode,
            registrertIArenaDato = registrertDatoIArena,
            sistEndretIArenaDato = sistEndretDatoIArena,
            fraDato = fraDato,
            tilDato = tilDato,
            rettPaaTiltakspenger = rettPaaTiltakspenger,
            status = tiltakstypestatus
        )
    }

    context("oppdater statuser på tiltaksgjennomføringer") {
        val tiltakstypeRepository = TiltakstypeRepository(database.db)
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val tiltaksgjennomforingKafkaProducer = mockk<TiltaksgjennomforingKafkaProducer>(relaxed = true)
        val kafkaSyncService =
            KafkaSyncService(
                tiltaksgjennomforingRepository,
                tiltakstypeRepository,
                tiltaksgjennomforingKafkaProducer,
                mockk()
            )

        val startdatoInnenforMenAvsluttetStatus = createTiltaksgjennomforing()
        val startdatoInnenfor =
            createTiltaksgjennomforing(avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET)
        val sluttdatoInnenforMenAvbruttStatus = createTiltaksgjennomforing(
            startDato = lastSuccessDate,
            sluttDato = lastSuccessDate,
            avslutningsstatus = Avslutningsstatus.AVBRUTT
        )
        val sluttdatoInnenfor = createTiltaksgjennomforing(
            startDato = lastSuccessDate,
            sluttDato = lastSuccessDate,
            avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET
        )
        val datoerUtenfor = createTiltaksgjennomforing(
            startDato = lastSuccessDate,
            avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET
        )

        test("oppdater statuser på kafka på relevante tiltaksgjennomføringer") {
            tiltakstypeRepository.upsert(tiltakstype)

            tiltaksgjennomforingRepository.upsert(startdatoInnenforMenAvsluttetStatus)
            tiltaksgjennomforingRepository.upsert(startdatoInnenfor)
            tiltaksgjennomforingRepository.upsert(sluttdatoInnenforMenAvbruttStatus)
            tiltaksgjennomforingRepository.upsert(sluttdatoInnenfor)
            tiltaksgjennomforingRepository.upsert(datoerUtenfor)

            kafkaSyncService.oppdaterTiltaksgjennomforingsstatus(today, lastSuccessDate)

            verifyAll {
                tiltaksgjennomforingKafkaProducer.publish(startdatoInnenfor.toDto(Tiltaksgjennomforingsstatus.GJENNOMFORES))
                tiltaksgjennomforingKafkaProducer.publish(sluttdatoInnenfor.toDto(Tiltaksgjennomforingsstatus.AVSLUTTET))
            }
        }
    }

    context("oppdater statuser på tiltakstyper") {
        val tiltakstypeRepository = TiltakstypeRepository(database.db)
        val tiltakstypeKafkaProducer = mockk<TiltakstypeKafkaProducer>(relaxed = true)
        val kafkaSyncService =
            KafkaSyncService(mockk(), tiltakstypeRepository, mockk(), tiltakstypeKafkaProducer)

        val startdatoInnenfor =
            tiltakstype.copy(id = UUID.randomUUID(), fraDato = LocalDate.of(2023, 2, 15))
        val sluttdatoInnenfor =
            tiltakstype.copy(id = UUID.randomUUID(), fraDato = LocalDate.of(2023, 2, 13), tilDato = lastSuccessDate)

        test("oppdater statuser på kafka på relevante tiltakstyper") {
            tiltakstypeRepository.upsert(tiltakstype)
            tiltakstypeRepository.upsert(startdatoInnenfor)
            tiltakstypeRepository.upsert(sluttdatoInnenfor)

            kafkaSyncService.oppdaterTiltakstypestatus(today, lastSuccessDate)

            verifyAll {
                tiltakstypeKafkaProducer.publish(startdatoInnenfor.toDto(Tiltakstypestatus.Aktiv))
                tiltakstypeKafkaProducer.publish(sluttdatoInnenfor.toDto(Tiltakstypestatus.Avsluttet))
            }
        }
    }
})
