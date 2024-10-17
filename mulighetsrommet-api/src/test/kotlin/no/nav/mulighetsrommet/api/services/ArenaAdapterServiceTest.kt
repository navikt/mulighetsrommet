package no.nav.mulighetsrommet.api.services

import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotliquery.Query
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.ArenaNavEnhet
import no.nav.mulighetsrommet.api.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.api.domain.dto.AvtaleDto
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.repositories.*
import no.nav.mulighetsrommet.api.services.cms.SanityService
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.*
import no.nav.mulighetsrommet.domain.dto.AvbruttAarsak
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingEksternV1Dto
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus
import no.nav.mulighetsrommet.kafka.producers.SisteTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.notifications.NotificationService
import no.nav.mulighetsrommet.notifications.NotificationType
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ArenaAdapterServiceTest :
    FunSpec({
        val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

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
                        it.type == NotificationType.TASK &&
                            it.targets.containsAll(
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
                        it.type == NotificationType.TASK &&
                            it.targets.containsAll(listOf(NavAnsattFixture.ansatt1.navIdent))
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

        context("tiltak i egen regi") {
            val gjennomforing = ArenaTiltaksgjennomforingDbo(
                id = UUID.randomUUID(),
                sanityId = null,
                navn = "IPS",
                tiltakstypeId = TiltakstypeFixtures.IPS.id,
                tiltaksnummer = "12345",
                arrangorOrganisasjonsnummer = "976663934",
                startDato = LocalDate.now(),
                sluttDato = LocalDate.now().plusYears(1),
                arenaAnsvarligEnhet = null,
                avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
                apentForInnsok = true,
                antallPlasser = null,
                avtaleId = null,
                deltidsprosent = 100.0,
            )

            beforeEach {
                MulighetsrommetTestDomain(
                    enheter = listOf(NavEnhetFixtures.IT, NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
                    tiltakstyper = listOf(TiltakstypeFixtures.IPS),
                    avtaler = listOf(),
                ).initialize(database.db)
            }

            afterEach {
                database.db.truncateAll()

                clearAllMocks()
            }

            test("should not upsert egen regi-tiltak") {
                val service = createArenaAdapterService(database.db)

                service.upsertTiltaksgjennomforing(gjennomforing)

                database.assertThat("tiltaksgjennomforing").isEmpty
            }

            test("should publish egen regi-tiltak to sanity") {
                val sanityService = mockk<SanityService>(relaxed = true)
                val service = createArenaAdapterService(
                    database.db,
                    sanityService = sanityService,
                )

                service.upsertTiltaksgjennomforing(gjennomforing)

                coVerify(exactly = 1) {
                    sanityService.createOrPatchSanityTiltaksgjennomforing(gjennomforing, any())
                }
            }

            test("should delete egen regi-tiltak from sanity") {
                val sanityService = mockk<SanityService>(relaxed = true)
                val service = createArenaAdapterService(
                    database.db,
                    sanityService = sanityService,
                )

                val sanityId = UUID.randomUUID()

                service.removeSanityTiltaksgjennomforing(sanityId)

                coVerify(exactly = 1) {
                    sanityService.deleteSanityTiltaksgjennomforing(sanityId)
                }
            }

            test("should not publish egen regi-tiltak to kafka") {
                val tiltaksgjennomforingKafkaProducer = mockk<SisteTiltaksgjennomforingerV1KafkaProducer>(relaxed = true)
                val service = createArenaAdapterService(
                    database.db,
                    tiltaksgjennomforingKafkaProducer = tiltaksgjennomforingKafkaProducer,
                )

                service.upsertTiltaksgjennomforing(gjennomforing)

                verify(exactly = 0) {
                    tiltaksgjennomforingKafkaProducer.publish(any())
                }
            }
        }

        context("gruppetiltak") {
            val gjennomforinger = TiltaksgjennomforingRepository(database.db)

            val gjennomforing = ArenaTiltaksgjennomforingDbo(
                id = UUID.randomUUID(),
                navn = "Oppfølging",
                sanityId = null,
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                tiltaksnummer = "12345",
                arrangorOrganisasjonsnummer = "976663934",
                startDato = LocalDate.now(),
                sluttDato = LocalDate.now().plusYears(1),
                arenaAnsvarligEnhet = null,
                avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
                apentForInnsok = true,
                antallPlasser = null,
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

                service.upsertTiltaksgjennomforing(gjennomforing)

                database.assertThat("tiltaksgjennomforing").row()
                    .value("id").isEqualTo(gjennomforing.id)
                    .value("navn").isEqualTo(gjennomforing.navn)
                    .value("tiltakstype_id").isEqualTo(TiltakstypeFixtures.Oppfolging.id)
                    .value("tiltaksnummer").isEqualTo(gjennomforing.tiltaksnummer)
                    .value("arrangor_id").isEqualTo(ArrangorFixtures.underenhet1.id)
                    .value("start_dato").isEqualTo(gjennomforing.startDato)
                    .value("slutt_dato").isEqualTo(gjennomforing.sluttDato)
                    .value("deltidsprosent").isEqualTo(gjennomforing.deltidsprosent)
                    .value("opphav").isEqualTo(ArenaMigrering.Opphav.ARENA.name)

                val updated = gjennomforing.copy(navn = "Oppdatert arbeidstrening")
                service.upsertTiltaksgjennomforing(updated)

                database.assertThat("tiltaksgjennomforing").row()
                    .value("navn").isEqualTo(updated.navn)

                service.removeTiltaksgjennomforing(updated.id)

                database.assertThat("tiltaksgjennomforing").isEmpty
            }

            test("should publish gruppetiltak to sanity") {
                val sanityService = mockk<SanityService>(relaxed = true)
                val service = createArenaAdapterService(
                    database.db,
                    sanityService = sanityService,
                )

                service.upsertTiltaksgjennomforing(gjennomforing)

                coVerify(exactly = 0) {
                    sanityService.createOrPatchSanityTiltaksgjennomforing(any(), any())
                }
            }

            test("should not retract from kafka if tiltak did not exist") {
                val tiltaksgjennomforingKafkaProducer = mockk<SisteTiltaksgjennomforingerV1KafkaProducer>(relaxed = true)
                val service = createArenaAdapterService(
                    database.db,
                    tiltaksgjennomforingKafkaProducer = tiltaksgjennomforingKafkaProducer,
                )

                service.removeTiltaksgjennomforing(UUID.randomUUID())

                verify(exactly = 0) { tiltaksgjennomforingKafkaProducer.retract(any()) }
            }

            test("should publish and retract gruppetiltak from kafka topic") {
                val tiltaksgjennomforingKafkaProducer = mockk<SisteTiltaksgjennomforingerV1KafkaProducer>(relaxed = true)
                val service = createArenaAdapterService(
                    database.db,
                    tiltaksgjennomforingKafkaProducer = tiltaksgjennomforingKafkaProducer,
                )

                service.upsertTiltaksgjennomforing(gjennomforing)

                verify(exactly = 1) {
                    tiltaksgjennomforingKafkaProducer.publish(
                        toTiltaksgjennomforingDto(
                            gjennomforing,
                            TiltakstypeFixtures.Oppfolging,
                        ),
                    )
                }

                service.removeTiltaksgjennomforing(gjennomforing.id)

                verify(exactly = 1) {
                    tiltaksgjennomforingKafkaProducer.retract(
                        gjennomforing.id,
                    )
                }
            }

            test("should only publish once for duplicated upserts") {
                val tiltaksgjennomforingKafkaProducer = mockk<SisteTiltaksgjennomforingerV1KafkaProducer>(relaxed = true)
                val service = createArenaAdapterService(
                    database.db,
                    tiltaksgjennomforingKafkaProducer = tiltaksgjennomforingKafkaProducer,
                )

                service.upsertTiltaksgjennomforing(gjennomforing)
                service.upsertTiltaksgjennomforing(gjennomforing)

                verify(exactly = 1) {
                    tiltaksgjennomforingKafkaProducer.publish(
                        toTiltaksgjennomforingDto(
                            gjennomforing,
                            TiltakstypeFixtures.Oppfolging,
                        ),
                    )
                }
            }

            test("skal ikke overskrive opphav når gjennomføring allerede eksisterer") {
                val gjennomforing1 = TiltaksgjennomforingFixtures.Oppfolging1

                MulighetsrommetTestDomain(
                    arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                    tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                    avtaler = listOf(AvtaleFixtures.oppfolging),
                    gjennomforinger = listOf(gjennomforing1),
                ).initialize(database.db)

                val service = createArenaAdapterService(database.db)

                service.upsertTiltaksgjennomforing(
                    ArenaTiltaksgjennomforingDbo(
                        id = gjennomforing1.id,
                        sanityId = null,
                        navn = "Endret navn",
                        tiltakstypeId = gjennomforing1.tiltakstypeId,
                        tiltaksnummer = "2024#1",
                        arrangorOrganisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer.value,
                        startDato = gjennomforing1.startDato,
                        sluttDato = gjennomforing1.sluttDato,
                        arenaAnsvarligEnhet = null,
                        avslutningsstatus = Avslutningsstatus.AVSLUTTET,
                        apentForInnsok = gjennomforing1.apentForInnsok,
                        antallPlasser = gjennomforing1.antallPlasser,
                        avtaleId = gjennomforing1.avtaleId,
                        deltidsprosent = gjennomforing1.deltidsprosent,
                    ),
                )

                gjennomforinger.get(gjennomforing1.id).shouldNotBeNull().should {
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
                val arenaGjennomforing = gjennomforing.copy(
                    startDato = LocalDate.now().minusDays(1),
                    sluttDato = LocalDate.now().minusDays(1),
                    avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
                )
                service.upsertTiltaksgjennomforing(arenaGjennomforing)
                // Verifiser status utledet fra datoer og ikke avslutningsstatus
                gjennomforinger.get(arenaGjennomforing.id)?.status?.status shouldBe TiltaksgjennomforingStatus.AVSLUTTET

                // Verifiser at avbrutt_tidspunkt blir lagret
                service.upsertTiltaksgjennomforing(arenaGjennomforing.copy(avslutningsstatus = Avslutningsstatus.AVBRUTT))
                gjennomforinger.get(arenaGjennomforing.id)?.status?.status shouldBe TiltaksgjennomforingStatus.AVBRUTT

                // Verifiser at man kan endre statusen
                service.upsertTiltaksgjennomforing(arenaGjennomforing.copy(avslutningsstatus = Avslutningsstatus.AVLYST))
                gjennomforinger.get(arenaGjennomforing.id)?.status?.status shouldBe TiltaksgjennomforingStatus.AVLYST

                // Verifiser at man kan endre statusen
                service.upsertTiltaksgjennomforing(arenaGjennomforing.copy(avslutningsstatus = Avslutningsstatus.AVSLUTTET))
                gjennomforinger.get(arenaGjennomforing.id)?.status?.status shouldBe TiltaksgjennomforingStatus.AVSLUTTET
            }

            test("skal bare oppdatere arena-felter når tiltakstype har endret eierskap") {
                val gjennomforing1 = TiltaksgjennomforingFixtures.Oppfolging1.copy(
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
                    gjennomforinger = listOf(gjennomforing1),
                ).initialize(database.db)

                val arenaDbo = ArenaTiltaksgjennomforingDbo(
                    id = gjennomforing1.id,
                    sanityId = null,
                    navn = "Endet navn",
                    tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                    tiltaksnummer = "2024#2024",
                    arrangorOrganisasjonsnummer = ArrangorFixtures.underenhet2.organisasjonsnummer.value,
                    startDato = LocalDate.of(2024, 1, 1),
                    sluttDato = LocalDate.of(2024, 1, 1),
                    arenaAnsvarligEnhet = NavEnhetFixtures.TiltakOslo.enhetsnummer,
                    avslutningsstatus = Avslutningsstatus.AVSLUTTET,
                    apentForInnsok = false,
                    antallPlasser = 100,
                    avtaleId = null,
                    deltidsprosent = 1.0,
                )

                val service = createArenaAdapterService(
                    database.db,
                    migrerteTiltakstyper = listOf(Tiltakskode.OPPFOLGING),
                )

                service.upsertTiltaksgjennomforing(arenaDbo)

                gjennomforinger.get(gjennomforing1.id).shouldNotBeNull().should {
                    it.tiltaksnummer shouldBe "2024#2024"
                    it.arenaAnsvarligEnhet shouldBe ArenaNavEnhet(navn = "NAV Tiltak Oslo", enhetsnummer = "0387")
                    it.status.status shouldBe TiltaksgjennomforingStatus.GJENNOMFORES

                    it.opphav shouldBe ArenaMigrering.Opphav.MR_ADMIN_FLATE
                    it.avtaleId shouldBe gjennomforing1.avtaleId
                    it.navn shouldBe gjennomforing1.navn
                    it.arrangor.organisasjonsnummer shouldBe ArrangorFixtures.underenhet1.organisasjonsnummer
                    it.startDato shouldBe gjennomforing1.startDato
                    it.sluttDato shouldBe gjennomforing1.sluttDato
                    it.apentForInnsok shouldBe gjennomforing1.apentForInnsok
                    it.antallPlasser shouldBe gjennomforing1.antallPlasser
                    it.oppstart shouldBe gjennomforing1.oppstart
                    it.deltidsprosent shouldBe gjennomforing1.deltidsprosent
                }
            }

            test("skal ikke overskrive avbrutt_tidspunkt") {
                val gjennomforing1 = TiltaksgjennomforingFixtures.Oppfolging1.copy(
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
                    gjennomforinger = listOf(gjennomforing1),
                ).initialize(database.db)

                // Setter den til custom avbrutt tidspunkt for å sjekke at den ikke overskrives med en "fake" en
                val jan2023 = LocalDateTime.of(2023, 1, 1, 0, 0, 0)
                gjennomforinger.avbryt(gjennomforing1.id, jan2023, AvbruttAarsak.EndringHosArrangor)

                val arenaDbo = ArenaTiltaksgjennomforingDbo(
                    id = gjennomforing1.id,
                    sanityId = null,
                    navn = "Endet navn",
                    tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                    tiltaksnummer = "2024#2024",
                    arrangorOrganisasjonsnummer = ArrangorFixtures.underenhet2.organisasjonsnummer.value,
                    startDato = LocalDate.of(2024, 1, 1),
                    sluttDato = LocalDate.of(2024, 1, 1),
                    arenaAnsvarligEnhet = NavEnhetFixtures.TiltakOslo.enhetsnummer,
                    avslutningsstatus = Avslutningsstatus.AVLYST,
                    apentForInnsok = false,
                    antallPlasser = 100,
                    avtaleId = null,
                    deltidsprosent = 1.0,
                )

                val service = createArenaAdapterService(
                    database.db,
                    migrerteTiltakstyper = listOf(Tiltakskode.OPPFOLGING),
                )

                service.upsertTiltaksgjennomforing(arenaDbo)

                val avbruttTidspunkt =
                    Query("select avbrutt_tidspunkt, avbrutt_aarsak from tiltaksgjennomforing where id = '${gjennomforing1.id}'")
                        .map { it.localDateTime("avbrutt_tidspunkt") to it.string("avbrutt_aarsak") }
                        .asSingle
                        .let { database.db.run(it) }

                avbruttTidspunkt shouldBe (jan2023 to "ENDRING_HOS_ARRANGOR")
            }

            test("should keep references to existing avtale when avtale is managed in Mulighetsrommet") {
                val tiltaksgjennomforingKafkaProducer = mockk<SisteTiltaksgjennomforingerV1KafkaProducer>(relaxed = true)
                val service = createArenaAdapterService(
                    database.db,
                    tiltaksgjennomforingKafkaProducer = tiltaksgjennomforingKafkaProducer,
                )

                forAll(row(Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET), row(Tiltakskode.ARBEIDSFORBEREDENDE_TRENING)) { tiltakskode ->
                    runBlocking {
                        val domain = MulighetsrommetTestDomain(
                            tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging.copy(tiltakskode = tiltakskode)),
                            avtaler = listOf(AvtaleFixtures.oppfolging),
                        )
                        domain.initialize(database.db)

                        val avtaleId = domain.avtaler[0].id

                        service.upsertTiltaksgjennomforing(gjennomforing.copy(avtaleId = avtaleId))
                        gjennomforinger.get(gjennomforing.id).shouldNotBeNull().should {
                            it.avtaleId shouldBe avtaleId
                        }

                        service.upsertTiltaksgjennomforing(gjennomforing.copy(avtaleId = null))
                        gjennomforinger.get(gjennomforing.id).shouldNotBeNull().should {
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

                        service.upsertTiltaksgjennomforing(gjennomforing.copy(avtaleId = avtaleId))
                        gjennomforinger.get(gjennomforing.id).shouldNotBeNull().should {
                            it.avtaleId shouldBe avtaleId
                        }

                        service.upsertTiltaksgjennomforing(gjennomforing.copy(avtaleId = null))
                        gjennomforinger.get(gjennomforing.id).shouldNotBeNull().should {
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
                service.upsertTiltaksgjennomforing(gjennomforing.copy(avtaleId = AvtaleFixtures.oppfolging.id))

                avtaler.get(AvtaleFixtures.oppfolging.id).shouldNotBeNull().arrangor.underenheter shouldBe listOf(
                    AvtaleDto.ArrangorUnderenhet(
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
                    gjennomforing.copy(
                        avtaleId = AvtaleFixtures.oppfolging.id,
                        arenaAnsvarligEnhet = NavEnhetFixtures.IT.enhetsnummer,
                    ),
                )

                verify(exactly = 1) {
                    val expectedNotification: ScheduledNotification = match<ScheduledNotification> {
                        it.type == NotificationType.TASK &&
                            it.targets.containsAll(
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
                    gjennomforing.copy(
                        avtaleId = AvtaleFixtures.oppfolging.id,
                        arenaAnsvarligEnhet = NavEnhetFixtures.TiltakOslo.enhetsnummer,
                    ),
                )

                verify(exactly = 1) {
                    val expectedNotification: ScheduledNotification = match<ScheduledNotification> {
                        it.type == NotificationType.TASK &&
                            it.targets.containsAll(listOf(NavAnsattFixture.ansatt1.navIdent))
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
                    gjennomforing.copy(
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
    })

private fun createArenaAdapterService(
    db: Database,
    tiltaksgjennomforingKafkaProducer: SisteTiltaksgjennomforingerV1KafkaProducer = mockk(relaxed = true),
    sanityService: SanityService = mockk(relaxed = true),
    notificationService: NotificationService = mockk(relaxed = true),
    migrerteTiltakstyper: List<Tiltakskode> = listOf(),
) = ArenaAdapterService(
    db = db,
    navAnsatte = NavAnsattRepository(db),
    tiltakstyper = TiltakstypeRepository(db),
    avtaler = AvtaleRepository(db),
    tiltaksgjennomforinger = TiltaksgjennomforingRepository(db),
    deltakere = DeltakerRepository(db),
    tiltaksgjennomforingKafkaProducer = tiltaksgjennomforingKafkaProducer,
    sanityService = sanityService,
    arrangorService = mockk(relaxed = true),
    navEnhetService = NavEnhetService(NavEnhetRepository(db)),
    notificationService = notificationService,
    endringshistorikk = EndringshistorikkService(db),
    tiltakstypeService = TiltakstypeService(TiltakstypeRepository(db), migrerteTiltakstyper),
)

private fun toTiltaksgjennomforingDto(dbo: ArenaTiltaksgjennomforingDbo, tiltakstype: TiltakstypeDbo) = dbo.run {
    TiltaksgjennomforingEksternV1Dto(
        id = id,
        tiltakstype = TiltaksgjennomforingEksternV1Dto.Tiltakstype(
            id = tiltakstypeId,
            navn = tiltakstype.navn,
            arenaKode = tiltakstype.arenaKode,
            tiltakskode = tiltakstype.tiltakskode!!,
        ),
        navn = navn,
        startDato = startDato,
        sluttDato = sluttDato,
        status = TiltaksgjennomforingStatus.GJENNOMFORES,
        oppstart = TiltaksgjennomforingOppstartstype.LOPENDE,
        virksomhetsnummer = arrangorOrganisasjonsnummer,
        tilgjengeligForArrangorFraOgMedDato = null,
    )
}
