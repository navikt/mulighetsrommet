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
import no.nav.mulighetsrommet.api.clients.AccessType
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClient
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.domain.dto.TiltakstypeEksternDto
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.repositories.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.*
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus.AVSLUTTET
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus.IKKE_AVSLUTTET
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus
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
        val tiltakstypeKafkaProducer = mockk<TiltakstypeKafkaProducer>(relaxed = true)
        val service = ArenaAdapterService(
            db = database.db,
            navAnsatte = NavAnsattRepository(database.db),
            tiltakstyper = TiltakstypeRepository(database.db),
            avtaler = AvtaleRepository(database.db),
            tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db),
            tiltakshistorikk = TiltakshistorikkRepository(database.db),
            deltakere = DeltakerRepository(database.db),
            tiltaksgjennomforingKafkaProducer = mockk(relaxed = true),
            tiltakstypeKafkaProducer = tiltakstypeKafkaProducer,
            sanityTiltaksgjennomforingService = mockk(relaxed = true),
            virksomhetService = mockk(relaxed = true),
            navEnhetService = mockk(relaxed = true),
            notificationService = mockk(relaxed = true),
            endringshistorikk = EndringshistorikkService(database.db),
            veilarboppfolgingClient = mockk<VeilarboppfolgingClient>(),
        )

        val tiltakstype = TiltakstypeFixtures.Oppfolging

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
            service.upsertTiltakstype(tiltakstype).shouldBeRight()

            verify(exactly = 1) {
                tiltakstypeKafkaProducer.publish(
                    TiltakstypeEksternDto(
                        id = tiltakstype.id,
                        navn = tiltakstype.navn,
                        arenaKode = tiltakstype.tiltakskode,
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

            verify(exactly = 1) { tiltakstypeKafkaProducer.retract(tiltakstype.id) }
        }

        test("should not retract tiltakstype if it did not already exist") {
            service.removeTiltakstype(UUID.randomUUID())

            verify(exactly = 0) { tiltakstypeKafkaProducer.retract(any()) }
        }
    }

    context("avtaler") {
        val notificationService = mockk<NotificationService>(relaxed = true)
        val navEnheter = NavEnhetRepository(database.db)
        val service = ArenaAdapterService(
            db = database.db,
            navAnsatte = NavAnsattRepository(database.db),
            tiltakstyper = TiltakstypeRepository(database.db),
            avtaler = AvtaleRepository(database.db),
            tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db),
            tiltakshistorikk = TiltakshistorikkRepository(database.db),
            deltakere = DeltakerRepository(database.db),
            tiltaksgjennomforingKafkaProducer = mockk(relaxed = true),
            tiltakstypeKafkaProducer = mockk(relaxed = true),
            sanityTiltaksgjennomforingService = mockk(relaxed = true),
            virksomhetService = mockk(relaxed = true),
            navEnhetService = NavEnhetService(navEnheter),
            notificationService = notificationService,
            endringshistorikk = EndringshistorikkService(database.db),
            veilarboppfolgingClient = mockk<VeilarboppfolgingClient>(),
        )

        val avtale = ArenaAvtaleDbo(
            id = UUID.randomUUID(),
            navn = "Oppfølgingsavtale",
            tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
            avtalenummer = "2023#1000",
            leverandorOrganisasjonsnummer = "123456789",
            startDato = LocalDate.now(),
            sluttDato = LocalDate.now().plusYears(1),
            arenaAnsvarligEnhet = null,
            avtaletype = Avtaletype.Rammeavtale,
            avslutningsstatus = IKKE_AVSLUTTET,
            prisbetingelser = "💸",
        )

        afterEach {
            clearAllMocks()
        }

        test("CRUD") {
            val domain = MulighetsrommetTestDomain(
                virksomheter = listOf(VirksomhetFixtures.hovedenhet),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(),
                gjennomforinger = listOf(),
            )
            domain.initialize(database.db)

            service.upsertAvtale(avtale)
            database.assertThat("avtale").row()
                .value("id").isEqualTo(avtale.id)
                .value("navn").isEqualTo(avtale.navn)
                .value("tiltakstype_id").isEqualTo(avtale.tiltakstypeId)
                .value("avtalenummer").isEqualTo(avtale.avtalenummer)
                .value("leverandor_virksomhet_id").isEqualTo(VirksomhetFixtures.hovedenhet.id)
                .value("start_dato").isEqualTo(avtale.startDato)
                .value("slutt_dato").isEqualTo(avtale.sluttDato)
                .value("arena_ansvarlig_enhet").isEqualTo(avtale.arenaAnsvarligEnhet)
                .value("avtaletype").isEqualTo(avtale.avtaletype.name)
                .value("avslutningsstatus").isEqualTo(avtale.avslutningsstatus.name)
                .value("prisbetingelser").isEqualTo(avtale.prisbetingelser)
        }

        test("varsler administratorer basert på hovedenhet når avtale har endringer") {
            val domain = MulighetsrommetTestDomain(
                enheter = listOf(NavEnhetFixtures.IT),
                ansatte = listOf(
                    NavAnsattFixture.ansatt1.copy(hovedenhet = NavEnhetFixtures.IT.enhetsnummer),
                    NavAnsattFixture.ansatt2.copy(hovedenhet = NavEnhetFixtures.IT.enhetsnummer),
                ),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(),
            )
            domain.initialize(database.db)

            service.upsertAvtale(
                avtale.copy(
                    arenaAnsvarligEnhet = NavEnhetFixtures.IT.enhetsnummer,
                ),
            )

            verify(exactly = 1) {
                val expectedNotification: ScheduledNotification = match<ScheduledNotification> {
                    it.type == NotificationType.TASK && it.targets.containsAll(
                        listOf(domain.ansatte[0].navIdent, domain.ansatte[1].navIdent),
                    )
                }
                notificationService.scheduleNotification(expectedNotification, any())
            }
        }

        test("varsler administratorer basert på felles fylke når avtale har endringer") {
            val domain = MulighetsrommetTestDomain(
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
            )
            domain.initialize(database.db)

            service.upsertAvtale(
                avtale.copy(
                    arenaAnsvarligEnhet = NavEnhetFixtures.TiltakOslo.enhetsnummer,
                ),
            )

            verify(exactly = 1) {
                val expectedNotification: ScheduledNotification = match<ScheduledNotification> {
                    it.type == NotificationType.TASK && it.targets.containsAll(
                        listOf(domain.ansatte[0].navIdent),
                    )
                }
                notificationService.scheduleNotification(expectedNotification, any())
            }
        }

        test("varsler ikke administratorer når avtalen er avsluttet") {
            val domain = MulighetsrommetTestDomain(
                enheter = listOf(NavEnhetFixtures.IT),
                ansatte = listOf(
                    NavAnsattFixture.ansatt1.copy(hovedenhet = NavEnhetFixtures.IT.enhetsnummer),
                    NavAnsattFixture.ansatt2.copy(hovedenhet = NavEnhetFixtures.IT.enhetsnummer),
                ),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(),
            )
            domain.initialize(database.db)

            service.upsertAvtale(
                avtale.copy(
                    arenaAnsvarligEnhet = NavEnhetFixtures.IT.enhetsnummer,
                    avslutningsstatus = AVSLUTTET,
                ),
            )

            verify(exactly = 0) {
                notificationService.scheduleNotification(any(), any())
            }
        }
    }

    context("tiltaksgjennomføring") {
        val tiltaksgjennomforingKafkaProducer = mockk<TiltaksgjennomforingKafkaProducer>(relaxed = true)
        val notificationService = mockk<NotificationService>(relaxed = true)
        val gjennomforinger = TiltaksgjennomforingRepository(database.db)
        val service = ArenaAdapterService(
            db = database.db,
            navAnsatte = NavAnsattRepository(database.db),
            tiltakstyper = TiltakstypeRepository(database.db),
            avtaler = AvtaleRepository(database.db),
            tiltaksgjennomforinger = gjennomforinger,
            tiltakshistorikk = TiltakshistorikkRepository(database.db),
            deltakere = DeltakerRepository(database.db),
            tiltaksgjennomforingKafkaProducer = tiltaksgjennomforingKafkaProducer,
            tiltakstypeKafkaProducer = mockk(relaxed = true),
            sanityTiltaksgjennomforingService = mockk(relaxed = true),
            virksomhetService = mockk(relaxed = true),
            navEnhetService = NavEnhetService(NavEnhetRepository(database.db)),
            notificationService = notificationService,
            endringshistorikk = EndringshistorikkService(database.db),
            veilarboppfolgingClient = mockk<VeilarboppfolgingClient>(),
        )

        val tiltaksgjennomforing = ArenaTiltaksgjennomforingDbo(
            id = UUID.randomUUID(),
            navn = "Oppfølging",
            tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
            tiltaksnummer = "12345",
            arrangorOrganisasjonsnummer = "976663934",
            startDato = LocalDate.now(),
            sluttDato = LocalDate.now().plusYears(1),
            arenaAnsvarligEnhet = null,
            avslutningsstatus = IKKE_AVSLUTTET,
            apentForInnsok = true,
            antallPlasser = null,
            oppstart = TiltaksgjennomforingOppstartstype.FELLES,
            avtaleId = null,
            deltidsprosent = 100.0,
        )

        afterEach {
            clearAllMocks()
        }

        test("CRUD") {
            val domain = MulighetsrommetTestDomain(
                enheter = listOf(NavEnhetFixtures.IT),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(AvtaleFixtures.oppfolging),
            )
            domain.initialize(database.db)

            service.upsertTiltaksgjennomforing(tiltaksgjennomforing)

            database.assertThat("tiltaksgjennomforing").row()
                .value("id").isEqualTo(tiltaksgjennomforing.id)
                .value("navn").isEqualTo(tiltaksgjennomforing.navn)
                .value("tiltakstype_id").isEqualTo(TiltakstypeFixtures.Oppfolging.id)
                .value("tiltaksnummer").isEqualTo(tiltaksgjennomforing.tiltaksnummer)
                .value("arrangor_virksomhet_id").isEqualTo(VirksomhetFixtures.underenhet1.id)
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
            service.removeTiltaksgjennomforing(UUID.randomUUID())

            verify(exactly = 0) { tiltaksgjennomforingKafkaProducer.retract(any()) }
        }

        test("should publish and retract gruppetiltak from kafka topic") {
            service.upsertTiltakstype(TiltakstypeFixtures.Oppfolging)
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
            service.upsertTiltakstype(TiltakstypeFixtures.Oppfolging)
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

        test("should not overwrite opphav when gjennomforing already exists") {
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1

            MulighetsrommetTestDomain(
                virksomheter = listOf(VirksomhetFixtures.hovedenhet, VirksomhetFixtures.underenhet1),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(AvtaleFixtures.oppfolging),
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.db)

            service.upsertTiltaksgjennomforing(
                toArenaTiltaksgjennomforingDbo(
                    gjennomforing.copy(navn = "Endret navn"),
                    VirksomhetFixtures.underenhet1.organisasjonsnummer,
                    AVSLUTTET,
                    "2024#1",
                ),
            )

            gjennomforinger.get(gjennomforing.id).shouldNotBeNull().should {
                it.navn shouldBe "Endret navn"
                it.opphav shouldBe ArenaMigrering.Opphav.MR_ADMIN_FLATE
            }
        }

        test("should keep references to existing avtale when avtale is managed in Mulighetsrommet") {
            forAll(row("VASV"), row("ARBFORB")) { tiltakskode ->
                runBlocking {
                    val domain = MulighetsrommetTestDomain(
                        tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging.copy(tiltakskode = tiltakskode)),
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
            forAll(row("JOBBK"), row("GRUPPEAMO")) { tiltakskode ->
                runBlocking {
                    val domain = MulighetsrommetTestDomain(
                        tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging.copy(tiltakskode = tiltakskode)),
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

        test("should update avtale underleverandor") {
            val domain = MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(AvtaleFixtures.oppfolging.copy(leverandorUnderenheter = emptyList())),
            )
            domain.initialize(database.db)

            val avtaler = AvtaleRepository(database.db)

            avtaler.get(AvtaleFixtures.oppfolging.id).shouldNotBeNull().leverandor.underenheter.shouldBeEmpty()

            service.upsertTiltaksgjennomforing(tiltaksgjennomforing.copy(avtaleId = AvtaleFixtures.oppfolging.id))

            avtaler.get(AvtaleFixtures.oppfolging.id).shouldNotBeNull().leverandor.underenheter shouldBe listOf(
                AvtaleAdminDto.LeverandorUnderenhet(
                    id = VirksomhetFixtures.underenhet1.id,
                    organisasjonsnummer = VirksomhetFixtures.underenhet1.organisasjonsnummer,
                    navn = VirksomhetFixtures.underenhet1.navn,
                    slettet = false,
                ),
            )
        }

        test("varsler administratorer basert på hovedenhet når gjennomføring har endringer") {
            val domain = MulighetsrommetTestDomain(
                enheter = listOf(NavEnhetFixtures.IT),
                ansatte = listOf(
                    NavAnsattFixture.ansatt1.copy(hovedenhet = NavEnhetFixtures.IT.enhetsnummer),
                    NavAnsattFixture.ansatt2.copy(hovedenhet = NavEnhetFixtures.IT.enhetsnummer),
                ),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(AvtaleFixtures.oppfolging),
            )
            domain.initialize(database.db)

            service.upsertTiltaksgjennomforing(
                tiltaksgjennomforing.copy(
                    avtaleId = AvtaleFixtures.oppfolging.id,
                    arenaAnsvarligEnhet = NavEnhetFixtures.IT.enhetsnummer,
                ),
            )

            verify(exactly = 1) {
                val expectedNotification: ScheduledNotification = match<ScheduledNotification> {
                    it.type == NotificationType.TASK && it.targets.containsAll(
                        listOf(domain.ansatte[0].navIdent, domain.ansatte[1].navIdent),
                    )
                }
                notificationService.scheduleNotification(expectedNotification, any())
            }
        }

        test("varsler administratorer basert på felles fylke når gjennomføring har endringer") {
            val domain = MulighetsrommetTestDomain(
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
            )
            domain.initialize(database.db)

            service.upsertTiltaksgjennomforing(
                tiltaksgjennomforing.copy(
                    avtaleId = AvtaleFixtures.oppfolging.id,
                    arenaAnsvarligEnhet = NavEnhetFixtures.TiltakOslo.enhetsnummer,
                ),
            )

            verify(exactly = 1) {
                val expectedNotification: ScheduledNotification = match<ScheduledNotification> {
                    it.type == NotificationType.TASK && it.targets.containsAll(
                        listOf(domain.ansatte[0].navIdent),
                    )
                }
                notificationService.scheduleNotification(expectedNotification, any())
            }
        }

        test("varsler ikke administratorer når gjennomføringen er avsluttet") {
            val domain = MulighetsrommetTestDomain(
                enheter = listOf(NavEnhetFixtures.IT),
                ansatte = listOf(
                    NavAnsattFixture.ansatt1.copy(hovedenhet = NavEnhetFixtures.IT.enhetsnummer),
                    NavAnsattFixture.ansatt2.copy(hovedenhet = NavEnhetFixtures.IT.enhetsnummer),
                ),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(AvtaleFixtures.oppfolging),
            )
            domain.initialize(database.db)

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

        val service = ArenaAdapterService(
            db = database.db,
            navAnsatte = NavAnsattRepository(database.db),
            tiltakstyper = TiltakstypeRepository(database.db),
            avtaler = AvtaleRepository(database.db),
            tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db),
            tiltakshistorikk = TiltakshistorikkRepository(database.db),
            deltakere = DeltakerRepository(database.db),
            tiltaksgjennomforingKafkaProducer = mockk(relaxed = true),
            tiltakstypeKafkaProducer = mockk(relaxed = true),
            sanityTiltaksgjennomforingService = mockk(relaxed = true),
            virksomhetService = mockk(relaxed = true),
            navEnhetService = NavEnhetService(NavEnhetRepository(database.db)),
            notificationService = mockk(relaxed = true),
            endringshistorikk = EndringshistorikkService(database.db),
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

private fun toTiltaksgjennomforingDto(dbo: ArenaTiltaksgjennomforingDbo, tiltakstype: TiltakstypeDbo) = dbo.run {
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
        status = Tiltaksgjennomforingsstatus.GJENNOMFORES,
        oppstart = oppstart,
        virksomhetsnummer = arrangorOrganisasjonsnummer,
    )
}

fun toArenaTiltaksgjennomforingDbo(
    dbo: TiltaksgjennomforingDbo,
    organiasjonsnummer: String,
    avslutningsstatus: Avslutningsstatus,
    tiltaksnummer: String,
) = dbo.run {
    ArenaTiltaksgjennomforingDbo(
        id = id,
        navn = navn,
        tiltakstypeId = tiltakstypeId,
        tiltaksnummer = tiltaksnummer,
        arrangorOrganisasjonsnummer = organiasjonsnummer,
        startDato = startDato,
        sluttDato = sluttDato,
        arenaAnsvarligEnhet = null,
        avslutningsstatus = avslutningsstatus,
        apentForInnsok = apentForInnsok,
        antallPlasser = antallPlasser,
        avtaleId = avtaleId,
        oppstart = oppstart,
        deltidsprosent = deltidsprosent,
    )
}
