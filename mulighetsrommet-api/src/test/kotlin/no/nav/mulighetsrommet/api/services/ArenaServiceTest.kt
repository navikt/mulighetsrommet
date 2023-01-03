package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.mulighetsrommet.api.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.api.producers.TiltakstypeKafkaProducer
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createApiDatabaseTestSchema
import no.nav.mulighetsrommet.domain.dbo.DeltakerDbo
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.domain.dto.Deltakerstatus
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.domain.dto.TiltakstypeDto
import java.time.LocalDateTime
import java.util.*

class ArenaServiceTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseTestListener(createApiDatabaseTestSchema()))

    beforeEach {
        database.db.migrate()
    }

    afterEach {
        database.db.clean()
    }

    val tiltakstype = TiltakstypeDbo(
        id = UUID.randomUUID(),
        navn = "Oppfølging",
        tiltakskode = "INDOPPFAG"
    )

    val tiltaksgjennomforing = TiltaksgjennomforingDbo(
        id = UUID.randomUUID(),
        navn = "Arbeidstrening",
        tiltakstypeId = tiltakstype.id,
        tiltaksnummer = "12345",
        virksomhetsnummer = "123456789",
        fraDato = LocalDateTime.of(2022, 11, 11, 0, 0),
        tilDato = LocalDateTime.of(2023, 11, 11, 0, 0),
        enhet = "2990"
    )

    val deltaker = DeltakerDbo(
        id = UUID.randomUUID(),
        tiltaksgjennomforingId = tiltaksgjennomforing.id,
        norskIdent = "12345678910",
        status = Deltakerstatus.VENTER,
        fraDato = LocalDateTime.now(),
        tilDato = LocalDateTime.now().plusYears(1)
    )

    val tiltaksgjennomforingDto = tiltaksgjennomforing.run {
        TiltaksgjennomforingAdminDto(
            id = id,
            tiltakstype = TiltakstypeDto(
                id = tiltakstypeId,
                navn = tiltakstype.navn,
                arenaKode = tiltakstype.tiltakskode
            ),
            navn = navn,
            tiltaksnummer = tiltaksnummer,
            virksomhetsnummer = virksomhetsnummer,
            fraDato = fraDato,
            tilDato = tilDato,
            enhet = enhet
        )
    }

    context("tiltakstype") {
        val tiltakstypeKafkaProducer = mockk<TiltakstypeKafkaProducer>(relaxed = true)
        val service = ArenaService(
            TiltakstypeRepository(database.db),
            TiltaksgjennomforingRepository(database.db),
            DeltakerRepository(database.db),
            mockk(relaxed = true),
            tiltakstypeKafkaProducer
        )

        afterTest {
            clearAllMocks()
        }

        test("CRUD") {
            service.upsert(tiltakstype)

            database.assertThat("tiltakstype").row()
                .value("id").isEqualTo(tiltakstype.id)
                .value("navn").isEqualTo(tiltakstype.navn)

            val updated = tiltakstype.copy(navn = "Arbeidsovertrening")
            service.upsert(updated)

            database.assertThat("tiltakstype").row()
                .value("navn").isEqualTo(updated.navn)

            service.remove(updated)

            database.assertThat("tiltakstype").isEmpty
        }

        test("should publish and retract tiltakstype from kafka topic") {
            service.upsert(tiltakstype)

            verify(exactly = 1) { tiltakstypeKafkaProducer.publish(TiltakstypeDto.from(tiltakstype)) }

            service.remove(tiltakstype)

            verify(exactly = 1) { tiltakstypeKafkaProducer.retract(tiltakstype.id) }
        }
    }

    context("tiltaksgjennomføring") {
        val tiltaksgjennomforingKafkaProducer = mockk<TiltaksgjennomforingKafkaProducer>(relaxed = true)
        val service = ArenaService(
            TiltakstypeRepository(database.db),
            TiltaksgjennomforingRepository(database.db),
            DeltakerRepository(database.db),
            tiltaksgjennomforingKafkaProducer,
            mockk(relaxed = true)
        )

        afterTest {
            clearAllMocks()
        }

        test("CRUD") {
            service.upsert(tiltakstype)

            service.upsert(tiltaksgjennomforing)

            database.assertThat("tiltaksgjennomforing").row()
                .value("id").isEqualTo(tiltaksgjennomforing.id)
                .value("navn").isEqualTo(tiltaksgjennomforing.navn)
                .value("tiltakstype_id").isEqualTo(tiltakstype.id)
                .value("tiltaksnummer").isEqualTo(tiltaksgjennomforing.tiltaksnummer)
                .value("virksomhetsnummer").isEqualTo(tiltaksgjennomforing.virksomhetsnummer)
                .value("fra_dato").isEqualTo(tiltaksgjennomforing.fraDato)
                .value("til_dato").isEqualTo(tiltaksgjennomforing.tilDato)

            val updated = tiltaksgjennomforing.copy(navn = "Oppdatert arbeidstrening")
            service.upsert(updated)

            database.assertThat("tiltaksgjennomforing").row()
                .value("navn").isEqualTo(updated.navn)

            service.remove(updated)

            database.assertThat("tiltaksgjennomforing").isEmpty
        }

        test("should publish and retract gruppetiltak from kafka topic") {
            service.upsert(tiltakstype)
            service.upsert(tiltaksgjennomforing)

            verify(exactly = 1) {
                tiltaksgjennomforingKafkaProducer.publish(
                    TiltaksgjennomforingDto.from(
                        tiltaksgjennomforingDto
                    )
                )
            }

            service.remove(tiltaksgjennomforing)

            verify(exactly = 1) { tiltaksgjennomforingKafkaProducer.retract(tiltaksgjennomforing.id) }
        }

        test("should not publish other tiltak than gruppetilak") {
            service.upsert(tiltakstype.copy(tiltakskode = "MENTOR"))
            service.upsert(tiltaksgjennomforing)

            verify(exactly = 0) { tiltaksgjennomforingKafkaProducer.publish(any()) }

            service.remove(tiltaksgjennomforing)

            verify(exactly = 0) { tiltaksgjennomforingKafkaProducer.retract(any()) }
        }
    }

    context("deltaker") {
        val service = ArenaService(
            TiltakstypeRepository(database.db),
            TiltaksgjennomforingRepository(database.db),
            DeltakerRepository(database.db),
            mockk(relaxed = true),
            mockk(relaxed = true)
        )

        beforeTest {
            service.upsert(tiltakstype)
            service.upsert(tiltaksgjennomforing)
        }

        test("CRUD") {
            service.upsert(deltaker)

            database.assertThat("deltaker").row()
                .value("id").isEqualTo(deltaker.id)
                .value("status").isEqualTo(deltaker.status.name)

            val updated = deltaker.copy(status = Deltakerstatus.DELTAR)
            service.upsert(updated)

            database.assertThat("deltaker").row()
                .value("status").isEqualTo(updated.status.name)

            service.remove(updated)

            database.assertThat("deltaker").isEmpty
        }
    }
})
