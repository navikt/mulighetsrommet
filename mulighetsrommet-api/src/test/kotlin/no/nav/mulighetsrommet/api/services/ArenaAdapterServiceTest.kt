package no.nav.mulighetsrommet.api.services

import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.api.producers.TiltakstypeKafkaProducer
import no.nav.mulighetsrommet.api.repositories.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.*
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus
import no.nav.mulighetsrommet.domain.dto.TiltakstypeDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ArenaAdapterServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    beforeEach {
        database.db.truncateAll()
    }

    val tiltakstype = TiltakstypeDbo(
        id = UUID.randomUUID(),
        navn = "Oppfølging",
        tiltakskode = "INDOPPFAG",
        rettPaaTiltakspenger = true,
        registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        sistEndretDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        fraDato = LocalDate.of(2023, 1, 11),
        tilDato = LocalDate.of(2023, 1, 12),
    )

    val avtale = ArenaAvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Arbeidstrening",
        tiltakstypeId = tiltakstype.id,
        avtalenummer = "2023#1000",
        leverandorOrganisasjonsnummer = "123456789",
        startDato = LocalDate.of(2022, 11, 11),
        sluttDato = LocalDate.of(2023, 11, 11),
        arenaAnsvarligEnhet = "2990",
        avtaletype = Avtaletype.Rammeavtale,
        avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
        prisbetingelser = "💸",
        opphav = ArenaMigrering.Opphav.ARENA,
    )

    val tiltaksgjennomforing = ArenaTiltaksgjennomforingDbo(
        id = UUID.randomUUID(),
        navn = "Arbeidstrening",
        tiltakstypeId = tiltakstype.id,
        tiltaksnummer = "12345",
        arrangorOrganisasjonsnummer = "123456789",
        startDato = LocalDate.of(2022, 11, 11),
        sluttDato = LocalDate.of(2023, 11, 11),
        arenaAnsvarligEnhet = "2990",
        avslutningsstatus = Avslutningsstatus.AVSLUTTET,
        tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.LEDIG,
        antallPlasser = null,
        oppstart = TiltaksgjennomforingOppstartstype.FELLES,
        opphav = ArenaMigrering.Opphav.ARENA,
        avtaleId = null,
    )

    val tiltakshistorikkGruppe = ArenaTiltakshistorikkDbo.Gruppetiltak(
        id = UUID.randomUUID(),
        tiltaksgjennomforingId = tiltaksgjennomforing.id,
        norskIdent = "12345678910",
        status = Deltakerstatus.VENTER,
        fraDato = LocalDateTime.of(2018, 12, 3, 0, 0),
        tilDato = LocalDateTime.of(2019, 12, 3, 0, 0),
        registrertIArenaDato = LocalDateTime.of(2018, 12, 3, 0, 0),
    )

    val tiltakstypeIndividuell = TiltakstypeDbo(
        id = UUID.randomUUID(),
        navn = "Høyere utdanning",
        tiltakskode = "HOYEREUTD",
        rettPaaTiltakspenger = true,
        registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        sistEndretDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        fraDato = LocalDate.of(2023, 1, 11),
        tilDato = LocalDate.of(2023, 1, 12),
    )

    val tiltakshistorikkIndividuell = ArenaTiltakshistorikkDbo.IndividueltTiltak(
        id = UUID.randomUUID(),
        norskIdent = "12345678910",
        status = Deltakerstatus.VENTER,
        fraDato = LocalDateTime.of(2018, 12, 3, 0, 0),
        tilDato = LocalDateTime.of(2019, 12, 3, 0, 0),
        registrertIArenaDato = LocalDateTime.of(2018, 12, 3, 0, 0),
        beskrivelse = "Utdanning",
        tiltakstypeId = tiltakstypeIndividuell.id,
        arrangorOrganisasjonsnummer = "12343",
    )

    val tiltaksgjennomforingDto = tiltaksgjennomforing.run {
        TiltaksgjennomforingDto(
            id = id,
            tiltakstype = TiltaksgjennomforingDto.Tiltakstype(
                id = tiltakstypeId,
                navn = tiltakstype.navn,
                arenaKode = tiltakstype.tiltakskode,
            ),
            navn = navn,
            startDato = startDato,
            sluttDato = sluttDato,
            status = Tiltaksgjennomforingsstatus.AVSLUTTET,
            oppstart = oppstart,
            virksomhetsnummer = arrangorOrganisasjonsnummer,
        )
    }

    context("tiltakstype") {
        val tiltakstypeKafkaProducer = mockk<TiltakstypeKafkaProducer>(relaxed = true)
        val service = ArenaAdapterService(
            tiltakstyper = TiltakstypeRepository(database.db),
            avtaler = AvtaleRepository(database.db),
            tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db),
            tiltakshistorikk = TiltakshistorikkRepository(database.db),
            deltakere = DeltakerRepository(database.db),
            tiltaksgjennomforingKafkaProducer = mockk(relaxed = true),
            tiltakstypeKafkaProducer = tiltakstypeKafkaProducer,
            sanityTiltaksgjennomforingService = mockk(relaxed = true),
            virksomhetService = mockk(relaxed = true),
            db = database.db,
        )

        afterTest {
            clearAllMocks()
        }

        test("CRUD") {
            service.upsertTiltakstype(tiltakstype)

            database.assertThat("tiltakstype").row()
                .value("id").isEqualTo(tiltakstype.id)
                .value("navn").isEqualTo(tiltakstype.navn)

            val updated = tiltakstype.copy(navn = "Arbeidsovertrening")
            service.upsertTiltakstype(updated)

            database.assertThat("tiltakstype").row()
                .value("navn").isEqualTo(updated.navn)

            service.removeTiltakstype(updated.id)

            database.assertThat("tiltakstype").isEmpty
        }

        test("should publish and retract tiltakstype from kafka topic") {
            service.upsertTiltakstype(tiltakstype)

            verify(exactly = 1) { tiltakstypeKafkaProducer.publish(TiltakstypeDto.from(tiltakstype)) }

            service.removeTiltakstype(tiltakstype.id)

            verify(exactly = 1) { tiltakstypeKafkaProducer.retract(tiltakstype.id) }
        }

        test("should not retract tiltakstype if it did not already exist") {
            service.removeTiltakstype(UUID.randomUUID())

            verify(exactly = 0) { tiltakstypeKafkaProducer.retract(any()) }
        }
    }

    context("avtaler") {
        val service = ArenaAdapterService(
            tiltakstyper = TiltakstypeRepository(database.db),
            avtaler = AvtaleRepository(database.db),
            tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db),
            tiltakshistorikk = TiltakshistorikkRepository(database.db),
            deltakere = DeltakerRepository(database.db),
            tiltaksgjennomforingKafkaProducer = mockk(relaxed = true),
            tiltakstypeKafkaProducer = mockk(relaxed = true),
            sanityTiltaksgjennomforingService = mockk(relaxed = true),
            virksomhetService = mockk(relaxed = true),
            db = database.db,
        )

        test("CRUD") {
            service.upsertTiltakstype(tiltakstype)

            service.upsertAvtale(avtale)
            database.assertThat("avtale").row()
                .value("id").isEqualTo(avtale.id)
                .value("navn").isEqualTo(avtale.navn)
                .value("tiltakstype_id").isEqualTo(avtale.tiltakstypeId)
                .value("avtalenummer").isEqualTo(avtale.avtalenummer)
                .value("leverandor_organisasjonsnummer").isEqualTo(avtale.leverandorOrganisasjonsnummer)
                .value("start_dato").isEqualTo(avtale.startDato)
                .value("slutt_dato").isEqualTo(avtale.sluttDato)
                .value("arena_ansvarlig_enhet").isEqualTo(avtale.arenaAnsvarligEnhet)
                .value("avtaletype").isEqualTo(avtale.avtaletype.name)
                .value("avslutningsstatus").isEqualTo(avtale.avslutningsstatus.name)
                .value("prisbetingelser").isEqualTo(avtale.prisbetingelser)

            val updated = tiltaksgjennomforing.copy(navn = "Arbeidsovertrening")
            service.upsertTiltaksgjennomforing(updated).getOrThrow()
            database.assertThat("tiltaksgjennomforing").row()
                .value("navn").isEqualTo(updated.navn)

            service.removeTiltaksgjennomforing(updated.id)
            database.assertThat("tiltaksgjennomforing").isEmpty
        }
    }

    context("tiltaksgjennomføring") {
        val tiltaksgjennomforingKafkaProducer = mockk<TiltaksgjennomforingKafkaProducer>(relaxed = true)
        val service = ArenaAdapterService(
            tiltakstyper = TiltakstypeRepository(database.db),
            avtaler = AvtaleRepository(database.db),
            tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db),
            tiltakshistorikk = TiltakshistorikkRepository(database.db),
            deltakere = DeltakerRepository(database.db),
            tiltaksgjennomforingKafkaProducer = tiltaksgjennomforingKafkaProducer,
            tiltakstypeKafkaProducer = mockk(relaxed = true),
            sanityTiltaksgjennomforingService = mockk(relaxed = true),
            virksomhetService = mockk(relaxed = true),
            db = database.db,
        )

        afterTest {
            clearAllMocks()
        }

        test("CRUD") {
            service.upsertTiltakstype(tiltakstype)

            service.upsertTiltaksgjennomforing(tiltaksgjennomforing)

            database.assertThat("tiltaksgjennomforing").row()
                .value("id").isEqualTo(tiltaksgjennomforing.id)
                .value("navn").isEqualTo(tiltaksgjennomforing.navn)
                .value("tiltakstype_id").isEqualTo(tiltakstype.id)
                .value("tiltaksnummer").isEqualTo(tiltaksgjennomforing.tiltaksnummer)
                .value("arrangor_organisasjonsnummer").isEqualTo(tiltaksgjennomforing.arrangorOrganisasjonsnummer)
                .value("start_dato").isEqualTo(tiltaksgjennomforing.startDato)
                .value("slutt_dato").isEqualTo(tiltaksgjennomforing.sluttDato)

            val updated = tiltaksgjennomforing.copy(navn = "Oppdatert arbeidstrening")
            service.upsertTiltaksgjennomforing(updated)

            database.assertThat("tiltaksgjennomforing").row()
                .value("navn").isEqualTo(updated.navn)

            service.removeTiltaksgjennomforing(updated.id)

            database.assertThat("tiltaksgjennomforing").isEmpty
        }

        test("should not retract from kafka if tiltak did not exist") {
            service.removeTiltaksgjennomforing(UUID.randomUUID())

            verify(exactly = 0) { tiltaksgjennomforingKafkaProducer.retract(any()) }
        }

        test("should publish and retract gruppetiltak from kafka topic") {
            service.upsertTiltakstype(tiltakstype)
            service.upsertTiltaksgjennomforing(tiltaksgjennomforing)

            verify(exactly = 1) {
                tiltaksgjennomforingKafkaProducer.publish(tiltaksgjennomforingDto)
            }

            service.removeTiltaksgjennomforing(tiltaksgjennomforing.id)

            verify(exactly = 1) {
                tiltaksgjennomforingKafkaProducer.retract(
                    tiltaksgjennomforing.id,
                )
            }
        }

        test("should keep references to existing avtale when avtale is managed in Mulighetsrommet") {
            forAll(row("VASV"), row("ARBFORB")) { tiltakskode ->
                runBlocking {
                    val type = tiltakstype.copy(tiltakskode = tiltakskode)

                    service.upsertTiltakstype(type)
                    service.upsertAvtale(avtale)

                    service.upsertTiltaksgjennomforing(tiltaksgjennomforing.copy(avtaleId = avtale.id))
                    database.assertThat("tiltaksgjennomforing").row()
                        .value("avtale_id").isEqualTo(avtale.id)

                    service.upsertTiltaksgjennomforing(tiltaksgjennomforing.copy(avtaleId = null))
                    database.assertThat("tiltaksgjennomforing").row()
                        .value("avtale_id").isEqualTo(avtale.id)
                }
            }
        }

        test("should overwrite references to existing avtale when avtale is managed in Arena") {
            forAll(row("JOBBK"), row("GRUPPEAMO")) { tiltakskode ->
                runBlocking {
                    val type = tiltakstype.copy(tiltakskode = tiltakskode)

                    service.upsertTiltakstype(type)
                    service.upsertAvtale(avtale)

                    service.upsertTiltaksgjennomforing(tiltaksgjennomforing.copy(avtaleId = avtale.id))
                    database.assertThat("tiltaksgjennomforing").row()
                        .value("avtale_id").isEqualTo(avtale.id)

                    service.upsertTiltaksgjennomforing(tiltaksgjennomforing.copy(avtaleId = null))
                    database.assertThat("tiltaksgjennomforing").row()
                        .value("avtale_id").isNull
                }
            }
        }
    }

    context("tiltakshistorikk") {
        val service = ArenaAdapterService(
            tiltakstyper = TiltakstypeRepository(database.db),
            avtaler = AvtaleRepository(database.db),
            tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db),
            tiltakshistorikk = TiltakshistorikkRepository(database.db),
            deltakere = DeltakerRepository(database.db),
            tiltaksgjennomforingKafkaProducer = mockk(relaxed = true),
            tiltakstypeKafkaProducer = mockk(relaxed = true),
            sanityTiltaksgjennomforingService = mockk(relaxed = true),
            virksomhetService = mockk(relaxed = true),
            db = database.db,
        )

        beforeTest {
            service.upsertTiltakstype(tiltakstype)
            service.upsertTiltaksgjennomforing(tiltaksgjennomforing)
        }

        test("CRUD gruppe") {
            service.upsertTiltakshistorikk(tiltakshistorikkGruppe)

            database.assertThat("tiltakshistorikk").row()
                .value("id").isEqualTo(tiltakshistorikkGruppe.id)
                .value("status").isEqualTo(tiltakshistorikkGruppe.status.name)
                .value("tiltaksgjennomforing_id").isEqualTo(tiltakshistorikkGruppe.tiltaksgjennomforingId)
                .value("beskrivelse").isNull
                .value("arrangor_organisasjonsnummer").isNull
                .value("tiltakstypeid").isNull

            val updated = tiltakshistorikkGruppe.copy(status = Deltakerstatus.DELTAR)
            service.upsertTiltakshistorikk(updated)

            database.assertThat("tiltakshistorikk").row()
                .value("status").isEqualTo(updated.status.name)

            service.removeTiltakshistorikk(updated.id)

            database.assertThat("tiltakshistorikk").isEmpty
        }

        test("CRUD individuell") {
            service.upsertTiltakstype(tiltakstypeIndividuell)
            service.upsertTiltakshistorikk(tiltakshistorikkIndividuell)

            database.assertThat("tiltakshistorikk").row()
                .value("id").isEqualTo(tiltakshistorikkIndividuell.id)
                .value("beskrivelse").isEqualTo(tiltakshistorikkIndividuell.beskrivelse)
                .value("arrangor_organisasjonsnummer")
                .isEqualTo(tiltakshistorikkIndividuell.arrangorOrganisasjonsnummer)
                .value("tiltakstypeid").isEqualTo(tiltakshistorikkIndividuell.tiltakstypeId)
                .value("tiltaksgjennomforing_id").isNull

            val updated = tiltakshistorikkIndividuell.copy(beskrivelse = "Ny beskrivelse")
            service.upsertTiltakshistorikk(updated)

            database.assertThat("tiltakshistorikk").row()
                .value("beskrivelse").isEqualTo("Ny beskrivelse")

            service.removeTiltakshistorikk(updated.id)

            database.assertThat("tiltakshistorikk").isEmpty
        }
    }
})
