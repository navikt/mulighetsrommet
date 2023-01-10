package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.mulighetsrommet.api.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.api.producers.TiltakstypeKafkaProducer
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakshistorikkRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createApiDatabaseTestSchema
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dbo.TiltakshistorikkDbo
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.domain.dto.Deltakerstatus
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.domain.dto.TiltakstypeDto
import java.time.LocalDate
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
        startDato = LocalDate.of(2022, 11, 11),
        sluttDato = LocalDate.of(2023, 11, 11),
        enhet = "2990"
    )

    val tiltakshistorikkGruppe = TiltakshistorikkDbo.Gruppetiltak(
        id = UUID.randomUUID(),
        tiltaksgjennomforingId = tiltaksgjennomforing.id,
        norskIdent = "12345678910",
        status = Deltakerstatus.VENTER,
        fraDato = LocalDateTime.now(),
        tilDato = LocalDateTime.now().plusYears(1)
    )

    val tiltakstypeIndividuell = TiltakstypeDbo(
        id = UUID.randomUUID(),
        navn = "Høyere utdanning",
        tiltakskode = "HOYEREUTD"
    )

    val tiltakshistorikkIndividuell = TiltakshistorikkDbo.IndividueltTiltak(
        id = UUID.randomUUID(),
        norskIdent = "12345678910",
        status = Deltakerstatus.VENTER,
        fraDato = LocalDateTime.of(2018, 12, 3, 0, 0),
        tilDato = LocalDateTime.of(2019, 12, 3, 0, 0),
        beskrivelse = "Utdanning",
        tiltakstypeId = tiltakstypeIndividuell.id,
        virksomhetsnummer = "12343",
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
            startDato = startDato,
            sluttDato = sluttDato,
            enhet = enhet
        )
    }

    context("tiltakstype") {
        val tiltakstypeKafkaProducer = mockk<TiltakstypeKafkaProducer>(relaxed = true)
        val service = ArenaService(
            TiltakstypeRepository(database.db),
            TiltaksgjennomforingRepository(database.db),
            TiltakshistorikkRepository(database.db),
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
        val tiltaksgjennomforingKafkaProducer =
            mockk<TiltaksgjennomforingKafkaProducer>(relaxed = true)
        val service = ArenaService(
            TiltakstypeRepository(database.db),
            TiltaksgjennomforingRepository(database.db),
            TiltakshistorikkRepository(database.db),
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
                .value("start_dato").isEqualTo(tiltaksgjennomforing.startDato)
                .value("slutt_dato").isEqualTo(tiltaksgjennomforing.sluttDato)

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
                    TiltaksgjennomforingDto.from(tiltaksgjennomforingDto)
                )
            }

            service.remove(tiltaksgjennomforing)

            verify(exactly = 1) {
                tiltaksgjennomforingKafkaProducer.retract(
                    tiltaksgjennomforing.id
                )
            }
        }

        test("should not publish other tiltak than gruppetilak") {
            service.upsert(tiltakstype.copy(tiltakskode = "MENTOR"))
            service.upsert(tiltaksgjennomforing)

            verify(exactly = 0) { tiltaksgjennomforingKafkaProducer.publish(any()) }

            service.remove(tiltaksgjennomforing)

            verify(exactly = 0) { tiltaksgjennomforingKafkaProducer.retract(any()) }
        }
    }

    context("tiltakshistorikk") {
        val service = ArenaService(
            TiltakstypeRepository(database.db),
            TiltaksgjennomforingRepository(database.db),
            TiltakshistorikkRepository(database.db),
            mockk(relaxed = true),
            mockk(relaxed = true)
        )

        beforeTest {
            service.upsert(tiltakstype)
            service.upsert(tiltaksgjennomforing)
        }

        test("CRUD gruppe") {
            service.upsert(tiltakshistorikkGruppe)

            database.assertThat("tiltakshistorikk").row()
                .value("id").isEqualTo(tiltakshistorikkGruppe.id)
                .value("status").isEqualTo(tiltakshistorikkGruppe.status.name)

            val updated = tiltakshistorikkGruppe.copy(status = Deltakerstatus.DELTAR)
            service.upsert(updated)

            database.assertThat("tiltakshistorikk").row()
                .value("status").isEqualTo(updated.status.name)

            service.remove(updated)

            database.assertThat("tiltakshistorikk").isEmpty
        }

        test("CRUD individuell") {
            service.upsert(tiltakstypeIndividuell)
            service.upsert(tiltakshistorikkIndividuell)

            database.assertThat("tiltakshistorikk").row()
                .value("id").isEqualTo(tiltakshistorikkIndividuell.id)
                .value("beskrivelse").isEqualTo(tiltakshistorikkIndividuell.beskrivelse)

            val updated = tiltakshistorikkIndividuell.copy(beskrivelse = "Ny beskrivelse")
            service.upsert(updated)

            database.assertThat("tiltakshistorikk").row()
                .value("beskrivelse").isEqualTo("Ny beskrivelse")

            service.remove(updated)

            database.assertThat("tiltakshistorikk").isEmpty
        }
    }
})
