package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.api.producers.TiltakstypeKafkaProducer
import no.nav.mulighetsrommet.api.repositories.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.*
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo.Tilgjengelighetsstatus
import no.nav.mulighetsrommet.domain.dto.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ArenaAdapterServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    afterEach {
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

    val avtale = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Arbeidstrening",
        tiltakstypeId = tiltakstype.id,
        avtalenummer = "2023#1000",
        leverandorOrganisasjonsnummer = "123456789",
        leverandorUnderenheter = emptyList(),
        startDato = LocalDate.of(2022, 11, 11),
        sluttDato = LocalDate.of(2023, 11, 11),
        arenaAnsvarligEnhet = "2990",
        navRegion = null,
        avtaletype = Avtaletype.Rammeavtale,
        avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
        prisbetingelser = "💸",
        opphav = ArenaMigrering.Opphav.ARENA,
        navEnheter = emptyList(),
    )

    val tiltaksgjennomforing = TiltaksgjennomforingDbo(
        id = UUID.randomUUID(),
        navn = "Arbeidstrening",
        tiltakstypeId = tiltakstype.id,
        tiltaksnummer = "12345",
        virksomhetsnummer = "123456789",
        startDato = LocalDate.of(2022, 11, 11),
        sluttDato = LocalDate.of(2023, 11, 11),
        arenaAnsvarligEnhet = "2990",
        avslutningsstatus = Avslutningsstatus.AVSLUTTET,
        tilgjengelighet = Tilgjengelighetsstatus.Ledig,
        antallPlasser = null,
        ansvarlige = emptyList(),
        navEnheter = emptyList(),
        oppstart = TiltaksgjennomforingDbo.Oppstartstype.FELLES,
        opphav = ArenaMigrering.Opphav.ARENA,
    )

    val tiltakshistorikkGruppe = TiltakshistorikkDbo.Gruppetiltak(
        id = UUID.randomUUID(),
        tiltaksgjennomforingId = tiltaksgjennomforing.id,
        norskIdent = "12345678910",
        status = Deltakerstatus.VENTER,
        fraDato = LocalDateTime.now(),
        tilDato = LocalDateTime.now().plusYears(1),
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
            tiltakstype = TiltaksgjennomforingAdminDto.Tiltakstype(
                id = tiltakstypeId,
                navn = tiltakstype.navn,
                arenaKode = tiltakstype.tiltakskode,
            ),
            navn = navn,
            tiltaksnummer = tiltaksnummer,
            virksomhetsnummer = virksomhetsnummer,
            startDato = startDato,
            sluttDato = sluttDato,
            arenaAnsvarligEnhet = arenaAnsvarligEnhet,
            status = Tiltaksgjennomforingsstatus.AVSLUTTET,
            tilgjengelighet = Tilgjengelighetsstatus.Ledig,
            antallPlasser = null,
            ansvarlig = null,
            navEnheter = emptyList(),
            sanityId = null,
            oppstart = oppstart,
            opphav = ArenaMigrering.Opphav.ARENA,
            stengtFra = null,
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
        )

        test("CRUD") {
            service.upsertTiltakstype(tiltakstype)

            service.upsertAvtale(avtale).getOrThrow()
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
                .value("virksomhetsnummer").isEqualTo(tiltaksgjennomforing.virksomhetsnummer)
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
                tiltaksgjennomforingKafkaProducer.publish(
                    TiltaksgjennomforingDto.from(tiltaksgjennomforingDto),
                )
            }

            service.removeTiltaksgjennomforing(tiltaksgjennomforing.id)

            verify(exactly = 1) {
                tiltaksgjennomforingKafkaProducer.retract(
                    tiltaksgjennomforing.id,
                )
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
                .value("virksomhetsnummer").isNull
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
                .value("virksomhetsnummer").isEqualTo(tiltakshistorikkIndividuell.virksomhetsnummer)
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
