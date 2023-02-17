package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyAll
import no.nav.mulighetsrommet.api.producers.TiltaksgjennomforingKafkaProducer
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

    beforeEach {
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
        tilDato = LocalDate.of(2023, 1, 12)
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

    context("oppdater statuser på tiltaksgjennomføringer") {
        val tiltakstypeRepository = TiltakstypeRepository(database.db)
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val tiltaksgjennomforingKafkaProducer = mockk<TiltaksgjennomforingKafkaProducer>(relaxed = true)
        val kafkaSyncService =
            KafkaSyncService(tiltaksgjennomforingRepository, tiltaksgjennomforingKafkaProducer)

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
})
