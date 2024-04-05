package no.nav.mulighetsrommet.api.services

import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotliquery.Query
import no.nav.mulighetsrommet.api.clients.AccessType
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClient
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.ArenaNavEnhet
import no.nav.mulighetsrommet.api.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.domain.dto.TiltakstypeEksternDto
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.repositories.*
import no.nav.mulighetsrommet.api.tasks.InitialLoadTiltaksgjennomforinger
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.*
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus
import no.nav.mulighetsrommet.domain.dto.Tiltakstypestatus
import no.nav.mulighetsrommet.kafka.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.kafka.producers.TiltakstypeKafkaProducer
import no.nav.mulighetsrommet.notifications.NotificationService
import no.nav.mulighetsrommet.notifications.NotificationType
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ArenaAdapterServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    context("tiltakstype") {
        val tiltakstype = TiltakstypeFixtures.Oppfolging

        afterEach {
            database.db.truncateAll()

            clearAllMocks()
        }

        test("CRUD") {
            val service = createArenaAdapterService(database.db)
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
            val tiltakstypeKafkaProducer = mockk<TiltakstypeKafkaProducer>(relaxed = true)
            val service = createArenaAdapterService(
                database.db,
                tiltakstypeKafkaProducer = tiltakstypeKafkaProducer,
            )

            service.upsertTiltakstype(tiltakstype)

            verify(exactly = 1) {
                tiltakstypeKafkaProducer.publish(
                    TiltakstypeEksternDto(
                        id = tiltakstype.id,
                        navn = tiltakstype.navn,
                        tiltakskode = Tiltakskode.fromArenaKode(tiltakstype.arenaKode)!!,
                        arenaKode = tiltakstype.arenaKode,
                        registrertIArenaDato = tiltakstype.registrertDatoIArena,
                        sistEndretIArenaDato = tiltakstype.sistEndretDatoIArena,
                        fraDato = tiltakstype.fraDato,
                        tilDato = tiltakstype.tilDato,
                        rettPaaTiltakspenger = tiltakstype.rettPaaTiltakspenger,
                        status = Tiltakstypestatus.Aktiv,
                        deltakerRegistreringInnhold = null,
                    ),
                )
            }

            service.removeTiltakstype(tiltakstype.id)

            verify(exactly = 1) {
                tiltakstypeKafkaProducer.retract(tiltakstype.id)
            }
        }

        test("should schedule initial load of gjennomføringer when name changes") {
            val initialLoadTiltaksgjennomforinger = mockk<InitialLoadTiltaksgjennomforinger>(relaxed = true)
            val service = createArenaAdapterService(
                database.db,
                initialLoadTiltaksgjennomforinger = initialLoadTiltaksgjennomforinger,
            )

            service.upsertTiltakstype(tiltakstype)

            verify(exactly = 0) {
                initialLoadTiltaksgjennomforinger.schedule(any(), any())
            }

            service.upsertTiltakstype(tiltakstype.copy(navn = "Nytt navn"))

            verify(exactly = 1) {
                initialLoadTiltaksgjennomforinger.schedule(
                    input = InitialLoadTiltaksgjennomforinger.Input(
                        tiltakstyper = listOf(Tiltakskode.OPPFOLGING),
                    ),
                    startTime = any(),
                )
            }
        }

        test("should not retract tiltakstype if it did not already exist") {
            val tiltakstypeKafkaProducer = mockk<TiltakstypeKafkaProducer>(relaxed = true)
            val service = createArenaAdapterService(
                database.db,
                tiltakstypeKafkaProducer = tiltakstypeKafkaProducer,
            )

            service.removeTiltakstype(UUID.randomUUID())

            verify(exactly = 0) { tiltakstypeKafkaProducer.retract(any()) }
        }
    }

    context("avtaler") {
        val avtale = ArenaAvtaleDbo(
            id = UUID.randomUUID(),
            navn = "Oppfølgingsavtale",
            tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
            avtalenummer = "2023#1000",
            arrangorOrganisasjonsnummer = "123456789",
            startDato = LocalDate.now(),
            sluttDato = LocalDate.now().plusYears(1),
            arenaAnsvarligEnhet = null,
            avtaletype = Avtaletype.Rammeavtale,
            avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
            prisbetingelser = "💸",
        )

        afterEach {
            database.db.truncateAll()

            clearAllMocks()
        }

        test("CRUD") {
            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(),
                gjennomforinger = listOf(),
            ).initialize(database.db)

            val service = createArenaAdapterService(database.db)
            service.upsertAvtale(avtale)

            database.assertThat("avtale").row()
                .value("id").isEqualTo(avtale.id)
                .value("navn").isEqualTo(avtale.navn)
                .value("tiltakstype_id").isEqualTo(avtale.tiltakstypeId)
                .value("avtalenummer").isEqualTo(avtale.avtalenummer)
                .value("arrangor_hovedenhet_id").isEqualTo(ArrangorFixtures.hovedenhet.id)
                .value("start_dato").isEqualTo(avtale.startDato)
                .value("slutt_dato").isEqualTo(avtale.sluttDato)
                .value("arena_ansvarlig_enhet").isEqualTo(avtale.arenaAnsvarligEnhet)
                .value("avtaletype").isEqualTo(avtale.avtaletype.name)
                .value("prisbetingelser").isEqualTo(avtale.prisbetingelser)
        }

        test("varsler administratorer basert på hovedenhet når avtale har endringer") {
            MulighetsrommetTestDomain(
                enheter = listOf(NavEnhetFixtures.IT),
                ansatte = listOf(
                    NavAnsattFixture.ansatt1.copy(hovedenhet = NavEnhetFixtures.IT.enhetsnummer),
                    NavAnsattFixture.ansatt2.copy(hovedenhet = NavEnhetFixtures.IT.enhetsnummer),
                ),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(),
            ).initialize(database.db)

            val notificationService = mockk<NotificationService>(relaxed = true)
            val service = createArenaAdapterService(
                database.db,
                notificationService = notificationService,
            )

            service.upsertAvtale(
                avtale.copy(
                    arenaAnsvarligEnhet = NavEnhetFixtures.IT.enhetsnummer,
                ),
            )

            verify(exactly = 1) {
                val expectedNotification: ScheduledNotification = match<ScheduledNotification> {
                    it.type == NotificationType.TASK && it.targets.containsAll(
                        listOf(NavAnsattFixture.ansatt1.navIdent, NavAnsattFixture.ansatt2.navIdent),
                    )
                }
                notificationService.scheduleNotification(expectedNotification, any())
            }
        }

        test("varsler administratorer basert på felles fylke når avtale har endringer") {
            MulighetsrommetTestDomain(
                enheter = listOf(
                    NavEnhetFixtures.IT,
                    NavEnhetFixtures.Oslo,
                    NavEnhetFixtures.Sagene,
                    NavEnhetFixtures.TiltakOslo,
                    NavEnhetFixtures.Innlandet,
                    NavEnhetFixtures.Gjovik,
                ),
                ansatte = listOf(
                    NavAnsattFixture.ansatt1.copy(hovedenhet = NavEnhetFixtures.Sagene.enhetsnummer),
                    NavAnsattFixture.ansatt2.copy(hovedenhet = NavEnhetFixtures.Gjovik.enhetsnummer),
                ),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(),
            ).initialize(database.db)

            val notificationService = mockk<NotificationService>(relaxed = true)
            val service = createArenaAdapterService(
                database.db,
                notificationService = notificationService,
            )

            service.upsertAvtale(
                avtale.copy(
                    arenaAnsvarligEnhet = NavEnhetFixtures.TiltakOslo.enhetsnummer,
                ),
            )

            verify(exactly = 1) {
                val expectedNotification: ScheduledNotification = match<ScheduledNotification> {
                    it.type == NotificationType.TASK && it.targets.containsAll(
                        listOf(NavAnsattFixture.ansatt1.navIdent),
                    )
                }
                notificationService.scheduleNotification(expectedNotification, any())
            }
        }

        test("varsler ikke administratorer når avtalen er avsluttet") {
            MulighetsrommetTestDomain(
                enheter = listOf(NavEnhetFixtures.IT),
                ansatte = listOf(
                    NavAnsattFixture.ansatt1.copy(hovedenhet = NavEnhetFixtures.IT.enhetsnummer),
                    NavAnsattFixture.ansatt2.copy(hovedenhet = NavEnhetFixtures.IT.enhetsnummer),
                ),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(),
            ).initialize(database.db)

            val notificationService = mockk<NotificationService>(relaxed = true)
            val service = createArenaAdapterService(
                database.db,
                notificationService = notificationService,
            )

            service.upsertAvtale(
                avtale.copy(
                    arenaAnsvarligEnhet = NavEnhetFixtures.IT.enhetsnummer,
                    sluttDato = LocalDate.now().minusDays(1),
                ),
            )

            verify(exactly = 0) {
                notificationService.scheduleNotification(any(), any())
            }
        }
    }

    context("tiltaksgjennomføring") {
        val gjennomforinger = TiltaksgjennomforingRepository(database.db)

        val tiltaksgjennomforing = ArenaTiltaksgjennomforingDbo(
            id = UUID.randomUUID(),
            navn = "Oppfølging",
            tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
            tiltaksnummer = "12345",
            arrangorOrganisasjonsnummer = "976663934",
            startDato = LocalDate.now(),
            sluttDato = LocalDate.now().plusYears(1),
            arenaAnsvarligEnhet = null,
            avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
            apentForInnsok = true,
            antallPlasser = null,
            oppstart = TiltaksgjennomforingOppstartstype.FELLES,
            avtaleId = null,
            deltidsprosent = 100.0,
        )

        beforeEach {
            MulighetsrommetTestDomain(
                enheter = listOf(NavEnhetFixtures.IT, NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(AvtaleFixtures.oppfolging),
            ).initialize(database.db)
        }

        afterEach {
            database.db.truncateAll()

            clearAllMocks()
        }

        test("CRUD") {
            val service = createArenaAdapterService(database.db)

            service.upsertTiltaksgjennomforing(tiltaksgjennomforing)

            database.assertThat("tiltaksgjennomforing").row()
                .value("id").isEqualTo(tiltaksgjennomforing.id)
                .value("navn").isEqualTo(tiltaksgjennomforing.navn)
                .value("tiltakstype_id").isEqualTo(TiltakstypeFixtures.Oppfolging.id)
                .value("tiltaksnummer").isEqualTo(tiltaksgjennomforing.tiltaksnummer)
                .value("arrangor_id").isEqualTo(ArrangorFixtures.underenhet1.id)
                .value("start_dato").isEqualTo(tiltaksgjennomforing.startDato)
                .value("slutt_dato").isEqualTo(tiltaksgjennomforing.sluttDato)
                .value("deltidsprosent").isEqualTo(tiltaksgjennomforing.deltidsprosent)
                .value("opphav").isEqualTo(ArenaMigrering.Opphav.ARENA.name)

            val updated = tiltaksgjennomforing.copy(navn = "Oppdatert arbeidstrening")
            service.upsertTiltaksgjennomforing(updated)

            database.assertThat("tiltaksgjennomforing").row()
                .value("navn").isEqualTo(updated.navn)

            service.removeTiltaksgjennomforing(updated.id)

            database.assertThat("tiltaksgjennomforing").isEmpty
        }

        test("should not retract from kafka if tiltak did not exist") {
            val tiltaksgjennomforingKafkaProducer = mockk<TiltaksgjennomforingKafkaProducer>(relaxed = true)
            val service = createArenaAdapterService(
                database.db,
                tiltaksgjennomforingKafkaProducer = tiltaksgjennomforingKafkaProducer,
            )

            service.removeTiltaksgjennomforing(UUID.randomUUID())

            verify(exactly = 0) { tiltaksgjennomforingKafkaProducer.retract(any()) }
        }

        test("should publish and retract gruppetiltak from kafka topic") {
            val tiltaksgjennomforingKafkaProducer = mockk<TiltaksgjennomforingKafkaProducer>(relaxed = true)
            val service = createArenaAdapterService(
                database.db,
                tiltaksgjennomforingKafkaProducer = tiltaksgjennomforingKafkaProducer,
            )

            service.upsertTiltaksgjennomforing(tiltaksgjennomforing)

            verify(exactly = 1) {
                tiltaksgjennomforingKafkaProducer.publish(
                    toTiltaksgjennomforingDto(
                        tiltaksgjennomforing,
                        TiltakstypeFixtures.Oppfolging,
                    ),
                )
            }

            service.removeTiltaksgjennomforing(tiltaksgjennomforing.id)

            verify(exactly = 1) {
                tiltaksgjennomforingKafkaProducer.retract(
                    tiltaksgjennomforing.id,
                )
            }
        }

        test("should only publish once for duplicated upserts") {
            val tiltaksgjennomforingKafkaProducer = mockk<TiltaksgjennomforingKafkaProducer>(relaxed = true)
            val service = createArenaAdapterService(
                database.db,
                tiltaksgjennomforingKafkaProducer = tiltaksgjennomforingKafkaProducer,
            )

            service.upsertTiltaksgjennomforing(tiltaksgjennomforing)
            service.upsertTiltaksgjennomforing(tiltaksgjennomforing)

            verify(exactly = 1) {
                tiltaksgjennomforingKafkaProducer.publish(
                    toTiltaksgjennomforingDto(
                        tiltaksgjennomforing,
                        TiltakstypeFixtures.Oppfolging,
                    ),
                )
            }
        }

        test("skal ikke overskrive opphav når gjennomføring allerede eksisterer") {
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1

            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(AvtaleFixtures.oppfolging),
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.db)

            val service = createArenaAdapterService(database.db)

            service.upsertTiltaksgjennomforing(
                ArenaTiltaksgjennomforingDbo(
                    id = gjennomforing.id,
                    navn = "Endret navn",
                    tiltakstypeId = gjennomforing.tiltakstypeId,
                    tiltaksnummer = "2024#1",
                    arrangorOrganisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                    startDato = gjennomforing.startDato,
                    sluttDato = gjennomforing.sluttDato,
                    arenaAnsvarligEnhet = null,
                    avslutningsstatus = Avslutningsstatus.AVSLUTTET,
                    apentForInnsok = gjennomforing.apentForInnsok,
                    antallPlasser = gjennomforing.antallPlasser,
                    avtaleId = gjennomforing.avtaleId,
                    oppstart = gjennomforing.oppstart,
                    deltidsprosent = gjennomforing.deltidsprosent,
                ),
            )

            gjennomforinger.get(gjennomforing.id).shouldNotBeNull().should {
                it.navn shouldBe "Endret navn"
                it.opphav shouldBe ArenaMigrering.Opphav.MR_ADMIN_FLATE
            }
        }

        test("skal oppdatere avbrutt_tidspunkt når den endres fra Arena") {
            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(AvtaleFixtures.oppfolging),
                gjennomforinger = listOf(),
            ).initialize(database.db)

            val service = createArenaAdapterService(database.db)

            // Upsert som har passert sluttdato, men med avslutningsstatus IKKE_AVSLUTTET
            val arenaGjennomforing = tiltaksgjennomforing.copy(
                startDato = LocalDate.now().minusDays(1),
                sluttDato = LocalDate.now().minusDays(1),
                avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
            )
            service.upsertTiltaksgjennomforing(arenaGjennomforing)
            // Verifiser status utledet fra datoer og ikke avslutningsstatus
            gjennomforinger.get(arenaGjennomforing.id)?.status shouldBe TiltaksgjennomforingStatus.AVSLUTTET

            // Verifiser at avbrutt_tidspunkt blir lagret
            service.upsertTiltaksgjennomforing(arenaGjennomforing.copy(avslutningsstatus = Avslutningsstatus.AVBRUTT))
            gjennomforinger.get(arenaGjennomforing.id)?.status shouldBe TiltaksgjennomforingStatus.AVBRUTT

            // Verifiser at man kan endre statusen
            service.upsertTiltaksgjennomforing(arenaGjennomforing.copy(avslutningsstatus = Avslutningsstatus.AVLYST))
            gjennomforinger.get(arenaGjennomforing.id)?.status shouldBe TiltaksgjennomforingStatus.AVLYST

            // Verifiser at man kan endre statusen
            service.upsertTiltaksgjennomforing(arenaGjennomforing.copy(avslutningsstatus = Avslutningsstatus.AVSLUTTET))
            gjennomforinger.get(arenaGjennomforing.id)?.status shouldBe TiltaksgjennomforingStatus.AVSLUTTET
        }

        test("skal bare oppdatere arena-felter når tiltakstype har endret eierskap") {
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                startDato = LocalDate.now(),
                sluttDato = LocalDate.now().plusDays(1),
            )

            MulighetsrommetTestDomain(
                enheter = listOf(
                    NavEnhetFixtures.IT,
                    NavEnhetFixtures.Innlandet,
                    NavEnhetFixtures.Gjovik,
                    NavEnhetFixtures.Oslo,
                    NavEnhetFixtures.TiltakOslo,
                ),
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(AvtaleFixtures.oppfolging),
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.db)

            val arenaDbo = ArenaTiltaksgjennomforingDbo(
                id = gjennomforing.id,
                navn = "Endet navn",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                tiltaksnummer = "2024#2024",
                arrangorOrganisasjonsnummer = ArrangorFixtures.underenhet2.organisasjonsnummer,
                startDato = LocalDate.of(2024, 1, 1),
                sluttDato = LocalDate.of(2024, 1, 1),
                arenaAnsvarligEnhet = NavEnhetFixtures.TiltakOslo.enhetsnummer,
                avslutningsstatus = Avslutningsstatus.AVSLUTTET,
                apentForInnsok = false,
                antallPlasser = 100,
                oppstart = TiltaksgjennomforingOppstartstype.FELLES,
                avtaleId = null,
                deltidsprosent = 1.0,
            )

            val service = createArenaAdapterService(
                database.db,
                migrerteTiltakstyper = listOf(Tiltakskode.OPPFOLGING),
            )

            service.upsertTiltaksgjennomforing(arenaDbo)

            gjennomforinger.get(gjennomforing.id).shouldNotBeNull().should {
                it.tiltaksnummer shouldBe "2024#2024"
                it.arenaAnsvarligEnhet shouldBe ArenaNavEnhet(navn = "NAV Tiltak Oslo", enhetsnummer = "0387")
                it.status shouldBe TiltaksgjennomforingStatus.GJENNOMFORES

                it.opphav shouldBe ArenaMigrering.Opphav.MR_ADMIN_FLATE
                it.avtaleId shouldBe gjennomforing.avtaleId
                it.navn shouldBe gjennomforing.navn
                it.arrangor.organisasjonsnummer shouldBe ArrangorFixtures.underenhet1.organisasjonsnummer
                it.startDato shouldBe gjennomforing.startDato
                it.sluttDato shouldBe gjennomforing.sluttDato
                it.apentForInnsok shouldBe gjennomforing.apentForInnsok
                it.antallPlasser shouldBe gjennomforing.antallPlasser
                it.oppstart shouldBe gjennomforing.oppstart
                it.deltidsprosent shouldBe gjennomforing.deltidsprosent
            }
        }

        test("skal ikke overskrive avbrutt_tidspunkt") {
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                startDato = LocalDate.now(),
                sluttDato = LocalDate.now().plusDays(1),
            )

            MulighetsrommetTestDomain(
                enheter = listOf(
                    NavEnhetFixtures.IT,
                    NavEnhetFixtures.Innlandet,
                    NavEnhetFixtures.Gjovik,
                    NavEnhetFixtures.Oslo,
                    NavEnhetFixtures.TiltakOslo,
                ),
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(AvtaleFixtures.oppfolging),
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.db)

            // Setter den til custom avbrutt tidspunkt for å sjekke at den ikke overskrives med en "fake" en
            val jan2023 = LocalDateTime.of(2023, 1, 1, 0, 0, 0)
            gjennomforinger.setAvbruttTidspunkt(gjennomforing.id, jan2023)

            val arenaDbo = ArenaTiltaksgjennomforingDbo(
                id = gjennomforing.id,
                navn = "Endet navn",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                tiltaksnummer = "2024#2024",
                arrangorOrganisasjonsnummer = ArrangorFixtures.underenhet2.organisasjonsnummer,
                startDato = LocalDate.of(2024, 1, 1),
                sluttDato = LocalDate.of(2024, 1, 1),
                arenaAnsvarligEnhet = NavEnhetFixtures.TiltakOslo.enhetsnummer,
                avslutningsstatus = Avslutningsstatus.AVLYST,
                apentForInnsok = false,
                antallPlasser = 100,
                oppstart = TiltaksgjennomforingOppstartstype.FELLES,
                avtaleId = null,
                deltidsprosent = 1.0,
            )

            val service = createArenaAdapterService(
                database.db,
                migrerteTiltakstyper = listOf(Tiltakskode.OPPFOLGING),
            )

            service.upsertTiltaksgjennomforing(arenaDbo)

            val avbruttTidspunkt =
                Query("select avbrutt_tidspunkt from tiltaksgjennomforing where id = '${gjennomforing.id}'")
                    .map { it.localDateTime("avbrutt_tidspunkt") }
                    .asSingle
                    .let { database.db.run(it) }

            avbruttTidspunkt shouldBe jan2023
        }

        test("should keep references to existing avtale when avtale is managed in Mulighetsrommet") {
            val tiltaksgjennomforingKafkaProducer = mockk<TiltaksgjennomforingKafkaProducer>(relaxed = true)
            val service = createArenaAdapterService(
                database.db,
                tiltaksgjennomforingKafkaProducer = tiltaksgjennomforingKafkaProducer,
            )

            forAll(row("VASV"), row("ARBFORB")) { arenaKode ->
                runBlocking {
                    val domain = MulighetsrommetTestDomain(
                        tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging.copy(arenaKode = arenaKode)),
                        avtaler = listOf(AvtaleFixtures.oppfolging),
                    )
                    domain.initialize(database.db)

                    val avtaleId = domain.avtaler[0].id

                    service.upsertTiltaksgjennomforing(tiltaksgjennomforing.copy(avtaleId = avtaleId)).shouldBeRight()
                    gjennomforinger.get(tiltaksgjennomforing.id).shouldNotBeNull().should {
                        it.avtaleId shouldBe avtaleId
                    }

                    service.upsertTiltaksgjennomforing(tiltaksgjennomforing.copy(avtaleId = null))
                    gjennomforinger.get(tiltaksgjennomforing.id).shouldNotBeNull().should {
                        it.avtaleId shouldBe avtaleId
                    }

                    verify(exactly = 1) {
                        tiltaksgjennomforingKafkaProducer.publish(any())
                    }
                }
            }
        }

        test("should overwrite references to existing avtale when avtale is managed in Arena") {
            val service = createArenaAdapterService(database.db)

            forAll(row("JOBBK"), row("GRUPPEAMO")) { arenaKode ->
                runBlocking {
                    val domain = MulighetsrommetTestDomain(
                        tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging.copy(arenaKode = arenaKode)),
                        avtaler = listOf(AvtaleFixtures.oppfolging),
                    )
                    domain.initialize(database.db)

                    val avtaleId = domain.avtaler[0].id

                    service.upsertTiltaksgjennomforing(tiltaksgjennomforing.copy(avtaleId = avtaleId))
                    gjennomforinger.get(tiltaksgjennomforing.id).shouldNotBeNull().should {
                        it.avtaleId shouldBe avtaleId
                    }

                    service.upsertTiltaksgjennomforing(tiltaksgjennomforing.copy(avtaleId = null))
                    gjennomforinger.get(tiltaksgjennomforing.id).shouldNotBeNull().should {
                        it.avtaleId shouldBe null
                    }
                }
            }
        }

        test("should update arrangør underenhet") {
            MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(AvtaleFixtures.oppfolging.copy(arrangorUnderenheter = emptyList())),
            ).initialize(database.db)

            val avtaler = AvtaleRepository(database.db)
            avtaler.get(AvtaleFixtures.oppfolging.id).shouldNotBeNull().arrangor.underenheter.shouldBeEmpty()

            val service = createArenaAdapterService(database.db)
            service.upsertTiltaksgjennomforing(tiltaksgjennomforing.copy(avtaleId = AvtaleFixtures.oppfolging.id))

            avtaler.get(AvtaleFixtures.oppfolging.id).shouldNotBeNull().arrangor.underenheter shouldBe listOf(
                AvtaleAdminDto.ArrangorUnderenhet(
                    id = ArrangorFixtures.underenhet1.id,
                    organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                    navn = ArrangorFixtures.underenhet1.navn,
                    slettet = false,
                ),
            )
        }

        test("varsler administratorer basert på hovedenhet når gjennomføring har endringer") {
            MulighetsrommetTestDomain(
                enheter = listOf(NavEnhetFixtures.IT),
                ansatte = listOf(
                    NavAnsattFixture.ansatt1.copy(hovedenhet = NavEnhetFixtures.IT.enhetsnummer),
                    NavAnsattFixture.ansatt2.copy(hovedenhet = NavEnhetFixtures.IT.enhetsnummer),
                ),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(AvtaleFixtures.oppfolging),
            ).initialize(database.db)

            val notificationService = mockk<NotificationService>(relaxed = true)
            val service = createArenaAdapterService(
                database.db,
                notificationService = notificationService,
            )

            service.upsertTiltaksgjennomforing(
                tiltaksgjennomforing.copy(
                    avtaleId = AvtaleFixtures.oppfolging.id,
                    arenaAnsvarligEnhet = NavEnhetFixtures.IT.enhetsnummer,
                ),
            )

            verify(exactly = 1) {
                val expectedNotification: ScheduledNotification = match<ScheduledNotification> {
                    it.type == NotificationType.TASK && it.targets.containsAll(
                        listOf(NavAnsattFixture.ansatt1.navIdent, NavAnsattFixture.ansatt2.navIdent),
                    )
                }
                notificationService.scheduleNotification(expectedNotification, any())
            }
        }

        test("varsler administratorer basert på felles fylke når gjennomføring har endringer") {
            MulighetsrommetTestDomain(
                enheter = listOf(
                    NavEnhetFixtures.IT,
                    NavEnhetFixtures.Oslo,
                    NavEnhetFixtures.Sagene,
                    NavEnhetFixtures.TiltakOslo,
                    NavEnhetFixtures.Innlandet,
                    NavEnhetFixtures.Gjovik,
                ),
                ansatte = listOf(
                    NavAnsattFixture.ansatt1.copy(hovedenhet = NavEnhetFixtures.Sagene.enhetsnummer),
                    NavAnsattFixture.ansatt2.copy(hovedenhet = NavEnhetFixtures.Gjovik.enhetsnummer),
                ),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(AvtaleFixtures.oppfolging),
            ).initialize(database.db)

            val notificationService = mockk<NotificationService>(relaxed = true)
            val service = createArenaAdapterService(
                database.db,
                notificationService = notificationService,
            )

            service.upsertTiltaksgjennomforing(
                tiltaksgjennomforing.copy(
                    avtaleId = AvtaleFixtures.oppfolging.id,
                    arenaAnsvarligEnhet = NavEnhetFixtures.TiltakOslo.enhetsnummer,
                ),
            )

            verify(exactly = 1) {
                val expectedNotification: ScheduledNotification = match<ScheduledNotification> {
                    it.type == NotificationType.TASK && it.targets.containsAll(
                        listOf(NavAnsattFixture.ansatt1.navIdent),
                    )
                }
                notificationService.scheduleNotification(expectedNotification, any())
            }
        }

        test("varsler ikke administratorer når gjennomføringen er avsluttet") {
            MulighetsrommetTestDomain(
                enheter = listOf(NavEnhetFixtures.IT),
                ansatte = listOf(
                    NavAnsattFixture.ansatt1.copy(hovedenhet = NavEnhetFixtures.IT.enhetsnummer),
                    NavAnsattFixture.ansatt2.copy(hovedenhet = NavEnhetFixtures.IT.enhetsnummer),
                ),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(AvtaleFixtures.oppfolging),
            ).initialize(database.db)

            val notificationService = mockk<NotificationService>(relaxed = true)
            val service = createArenaAdapterService(
                database.db,
                notificationService = notificationService,
            )

            service.upsertTiltaksgjennomforing(
                tiltaksgjennomforing.copy(
                    avtaleId = AvtaleFixtures.oppfolging.id,
                    arenaAnsvarligEnhet = NavEnhetFixtures.IT.enhetsnummer,
                    avslutningsstatus = Avslutningsstatus.AVBRUTT,
                ),
            )

            verify(exactly = 0) {
                notificationService.scheduleNotification(any(), any())
            }
        }
    }

    context("tiltakshistorikk") {
        val veilarboppfolgingClient: VeilarboppfolgingClient = mockk()
        val service = createArenaAdapterService(
            database.db,
            veilarboppfolgingClient = veilarboppfolgingClient,
        )

        val tiltakshistorikkGruppe = ArenaTiltakshistorikkDbo.Gruppetiltak(
            id = UUID.randomUUID(),
            tiltaksgjennomforingId = TiltaksgjennomforingFixtures.Oppfolging1.id,
            norskIdent = "12345678910",
            status = Deltakerstatus.VENTER,
            fraDato = LocalDateTime.of(2018, 12, 3, 0, 0),
            tilDato = LocalDateTime.of(2019, 12, 3, 0, 0),
            registrertIArenaDato = LocalDateTime.of(2018, 12, 3, 0, 0),
        )

        val tiltakstypeIndividuell = TiltakstypeDbo(
            id = UUID.randomUUID(),
            navn = "Høyere utdanning",
            arenaKode = "HOYEREUTD",
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

        beforeTest {
            val domain = MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging, tiltakstypeIndividuell),
                avtaler = listOf(AvtaleFixtures.oppfolging),
                gjennomforinger = listOf(TiltaksgjennomforingFixtures.Oppfolging1),
            )
            domain.initialize(database.db)

            coEvery {
                veilarboppfolgingClient.erBrukerUnderOppfolging(
                    "12345678910",
                    AccessType.M2M,
                )
            } returns true.right()
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

private fun createArenaAdapterService(
    db: Database,
    tiltakstypeKafkaProducer: TiltakstypeKafkaProducer = mockk(relaxed = true),
    tiltaksgjennomforingKafkaProducer: TiltaksgjennomforingKafkaProducer = mockk(relaxed = true),
    notificationService: NotificationService = mockk(relaxed = true),
    veilarboppfolgingClient: VeilarboppfolgingClient = mockk(),
    migrerteTiltakstyper: List<Tiltakskode> = listOf(),
    initialLoadTiltaksgjennomforinger: InitialLoadTiltaksgjennomforinger = mockk(relaxed = true),
) = ArenaAdapterService(
    db = db,
    navAnsatte = NavAnsattRepository(db),
    tiltakstyper = TiltakstypeRepository(db),
    avtaler = AvtaleRepository(db),
    tiltaksgjennomforinger = TiltaksgjennomforingRepository(db),
    tiltakshistorikk = TiltakshistorikkRepository(db),
    deltakere = DeltakerRepository(db),
    tiltaksgjennomforingKafkaProducer = tiltaksgjennomforingKafkaProducer,
    tiltakstypeKafkaProducer = tiltakstypeKafkaProducer,
    sanityTiltaksgjennomforingService = mockk(relaxed = true),
    arrangorService = mockk(relaxed = true),
    navEnhetService = NavEnhetService(NavEnhetRepository(db)),
    notificationService = notificationService,
    endringshistorikk = EndringshistorikkService(db),
    veilarboppfolgingClient = veilarboppfolgingClient,
    tiltakstypeService = TiltakstypeService(TiltakstypeRepository(db), migrerteTiltakstyper),
    initialLoadTiltaksgjennomforinger = initialLoadTiltaksgjennomforinger,
)

private fun toTiltaksgjennomforingDto(dbo: ArenaTiltaksgjennomforingDbo, tiltakstype: TiltakstypeDbo) = dbo.run {
    TiltaksgjennomforingDto(
        id = id,
        tiltakstype = TiltaksgjennomforingDto.Tiltakstype(
            id = tiltakstypeId,
            navn = tiltakstype.navn,
            arenaKode = tiltakstype.arenaKode,
        ),
        navn = navn,
        startDato = startDato,
        sluttDato = sluttDato,
        status = TiltaksgjennomforingStatus.GJENNOMFORES,
        oppstart = oppstart,
        virksomhetsnummer = arrangorOrganisasjonsnummer,
    )
}
