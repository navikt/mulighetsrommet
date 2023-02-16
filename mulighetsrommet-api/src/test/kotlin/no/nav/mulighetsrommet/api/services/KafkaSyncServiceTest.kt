package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
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

    afterEach {
        database.db.clean()
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

    fun TiltaksgjennomforingDbo.toAdmin(): TiltaksgjennomforingAdminDto {
        return TiltaksgjennomforingAdminDto(
            id = id,
            tiltakstype = TiltaksgjennomforingAdminDto.Tiltakstype(
                id = tiltakstype.id,
                navn = tiltakstype.navn,
                arenaKode = tiltakstype.tiltakskode,
            ),
            navn = navn,
            tiltaksnummer = tiltaksnummer,
            virksomhetsnummer = virksomhetsnummer,
            startDato = startDato,
            sluttDato = sluttDato,
            enhet = enhet,
            status = Tiltaksgjennomforingsstatus.AVSLUTTET
        )
    }


    context("tiltaksgjennomføring") {

        afterTest {
            clearAllMocks()
        }

        test("CRUD") {
            val tiltakstypeRepository = TiltakstypeRepository(database.db)
            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
            val tiltaksgjennomforingKafkaProducer = mockk<TiltaksgjennomforingKafkaProducer>(relaxed = true)
            val kafkaSyncService =
                KafkaSyncService(TiltaksgjennomforingRepository(database.db), tiltaksgjennomforingKafkaProducer)

            val tiltaksgjennomforingStartdatoInnenforMenAvsluttetStatus = createTiltaksgjennomforing()
            val tiltaksgjennomforingStartdatoInnenfor =
                createTiltaksgjennomforing(avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET)
            val tiltaksgjennomforingSluttdatoInnenforMenAvbruttStatus = createTiltaksgjennomforing(
                startDato = lastSuccessDate,
                sluttDato = today,
                avslutningsstatus = Avslutningsstatus.AVBRUTT
            )
            val tiltaksgjennomforingSluttdatoInnenfor = createTiltaksgjennomforing(
                startDato = lastSuccessDate,
                sluttDato = today,
                avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET
            )
            tiltakstypeRepository.upsert(tiltakstype)
            tiltaksgjennomforingRepository.upsert(tiltaksgjennomforingStartdatoInnenforMenAvsluttetStatus)
            tiltaksgjennomforingRepository.upsert(tiltaksgjennomforingStartdatoInnenfor)
            tiltaksgjennomforingRepository.upsert(tiltaksgjennomforingSluttdatoInnenforMenAvbruttStatus)
            tiltaksgjennomforingRepository.upsert(tiltaksgjennomforingSluttdatoInnenfor)

            kafkaSyncService.oppdaterTiltaksgjennomforingsstatus(today, lastSuccessDate)

            verify(exactly = 2) { tiltaksgjennomforingKafkaProducer.publish(any()) }
        }
    }
})


