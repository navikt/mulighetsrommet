package no.nav.mulighetsrommet.api.arenaadapter

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotliquery.Query
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.gjennomforing.kafka.SisteTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.api.navenhet.db.ArenaNavEnhet
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.arena.ArenaAvtaleDbo
import no.nav.mulighetsrommet.arena.ArenaGjennomforingDbo
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.arena.Avslutningsstatus
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.AvbruttAarsak
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.GjennomforingStatus
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ArenaAdapterServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    fun createArenaAdapterService(
        gjennomforingKafkaProducer: SisteTiltaksgjennomforingerV1KafkaProducer = mockk(relaxed = true),
        sanityService: SanityService = mockk(relaxed = true),
    ) = ArenaAdapterService(
        db = database.db,
        gjennomforingKafkaProducer = gjennomforingKafkaProducer,
        sanityService = sanityService,
        arrangorService = mockk(relaxed = true),
    )

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
            database.truncateAll()

            clearAllMocks()
        }

        test("CRUD") {
            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(),
                gjennomforinger = listOf(),
            ).initialize(database.db)

            val service = createArenaAdapterService()
            service.upsertAvtale(avtale)

            database.assertTable("avtale").row()
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
    }

    context("tiltak i egen regi") {
        val gjennomforing = ArenaGjennomforingDbo(
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
            apentForPamelding = true,
            antallPlasser = null,
            avtaleId = null,
            deltidsprosent = 100.0,
        )

        beforeEach {
            MulighetsrommetTestDomain(
                navEnheter = listOf(NavEnhetFixtures.IT, NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
                tiltakstyper = listOf(TiltakstypeFixtures.IPS),
                avtaler = listOf(),
            ).initialize(database.db)
        }

        afterEach {
            database.truncateAll()

            clearAllMocks()
        }

        test("should not upsert egen regi-tiltak") {
            val service = createArenaAdapterService()

            service.upsertTiltaksgjennomforing(gjennomforing)

            database.assertTable("gjennomforing").isEmpty
        }

        test("should publish egen regi-tiltak to sanity") {
            val sanityService = mockk<SanityService>(relaxed = true)
            val service = createArenaAdapterService(
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
                sanityService = sanityService,
            )

            val sanityId = UUID.randomUUID()

            service.removeSanityTiltaksgjennomforing(sanityId)

            coVerify(exactly = 1) {
                sanityService.deleteSanityTiltaksgjennomforing(sanityId)
            }
        }

        test("should not publish egen regi-tiltak to kafka") {
            val gjennomforingKafkaProducer =
                mockk<SisteTiltaksgjennomforingerV1KafkaProducer>(relaxed = true)
            val service = createArenaAdapterService(
                gjennomforingKafkaProducer = gjennomforingKafkaProducer,
            )

            service.upsertTiltaksgjennomforing(gjennomforing)

            verify(exactly = 0) {
                gjennomforingKafkaProducer.publish(any())
            }
        }
    }

    context("gruppetiltak") {

        beforeEach {
            MulighetsrommetTestDomain(
                navEnheter = listOf(NavEnhetFixtures.IT, NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(AvtaleFixtures.oppfolging),
            ).initialize(database.db)
        }

        afterEach {
            database.truncateAll()

            clearAllMocks()
        }

        test("tillater ikke opprettelse av gjennomføringer fra Arena") {
            val service = createArenaAdapterService()

            val arenaGjennomforing = ArenaGjennomforingDbo(
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
                apentForPamelding = true,
                antallPlasser = null,
                avtaleId = null,
                deltidsprosent = 100.0,
            )

            val exception = assertThrows<IllegalArgumentException> {
                service.upsertTiltaksgjennomforing(arenaGjennomforing)
            }
            exception.message shouldBe "Alle gruppetiltak har blitt migrert. Forventet å finne gjennomføring i databasen."
        }

        test("skal bare oppdatere arena-felter når tiltakstype har endret eierskap") {
            val gjennomforing1 = GjennomforingFixtures.Oppfolging1.copy(
                startDato = LocalDate.now(),
                sluttDato = LocalDate.now().plusDays(1),
            )

            MulighetsrommetTestDomain(
                navEnheter = listOf(
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

            val arenaDbo = ArenaGjennomforingDbo(
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
                apentForPamelding = false,
                antallPlasser = 100,
                avtaleId = null,
                deltidsprosent = 1.0,
            )

            val service = createArenaAdapterService()

            service.upsertTiltaksgjennomforing(arenaDbo)

            database.run {
                queries.gjennomforing.get(gjennomforing1.id).shouldNotBeNull().should {
                    it.tiltaksnummer shouldBe "2024#2024"
                    it.arenaAnsvarligEnhet shouldBe ArenaNavEnhet(navn = "Nav Tiltak Oslo", enhetsnummer = "0387")
                    it.status.status shouldBe GjennomforingStatus.GJENNOMFORES
                    it.opphav shouldBe ArenaMigrering.Opphav.MR_ADMIN_FLATE
                    it.avtaleId shouldBe gjennomforing1.avtaleId
                    it.navn shouldBe gjennomforing1.navn
                    it.arrangor.organisasjonsnummer shouldBe ArrangorFixtures.underenhet1.organisasjonsnummer
                    it.startDato shouldBe gjennomforing1.startDato
                    it.sluttDato shouldBe gjennomforing1.sluttDato
                    it.antallPlasser shouldBe gjennomforing1.antallPlasser
                    it.oppstart shouldBe gjennomforing1.oppstart
                    it.deltidsprosent shouldBe gjennomforing1.deltidsprosent
                }
            }
        }

        test("skal ikke overskrive avsluttet_tidspunkt") {
            val gjennomforing1 = GjennomforingFixtures.Oppfolging1.copy(
                startDato = LocalDate.now(),
                sluttDato = LocalDate.now().plusDays(1),
            )

            MulighetsrommetTestDomain(
                navEnheter = listOf(
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
            database.run {
                queries.gjennomforing.setAvsluttet(
                    gjennomforing1.id,
                    jan2023,
                    AvbruttAarsak.EndringHosArrangor,
                )
            }

            val arenaDbo = ArenaGjennomforingDbo(
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
                apentForPamelding = false,
                antallPlasser = 100,
                avtaleId = null,
                deltidsprosent = 1.0,
            )

            val service = createArenaAdapterService()

            service.upsertTiltaksgjennomforing(arenaDbo)

            val avbrutt = database.run {
                session.single(Query("select avsluttet_tidspunkt, avbrutt_aarsak from gjennomforing where id = '${gjennomforing1.id}'")) {
                    it.localDateTime("avsluttet_tidspunkt") to it.string("avbrutt_aarsak")
                }
            }

            avbrutt shouldBe (jan2023 to "ENDRING_HOS_ARRANGOR")
        }

        test("skal publisere til kafka når det er endringer fra Arena") {
            val gjennomforing1 = GjennomforingFixtures.Oppfolging1

            MulighetsrommetTestDomain(
                navEnheter = listOf(NavEnhetFixtures.IT, NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(AvtaleFixtures.oppfolging),
                gjennomforinger = listOf(gjennomforing1),
            ).initialize(database.db)

            val gjennomforingKafkaProducer =
                mockk<SisteTiltaksgjennomforingerV1KafkaProducer>(relaxed = true)
            val service = createArenaAdapterService(
                gjennomforingKafkaProducer = gjennomforingKafkaProducer,
            )

            val arenaGjennomforing = ArenaGjennomforingDbo(
                id = gjennomforing1.id,
                navn = "Oppfølging",
                sanityId = null,
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                tiltaksnummer = "12345",
                arrangorOrganisasjonsnummer = "976663934",
                startDato = LocalDate.now(),
                sluttDato = LocalDate.now().plusYears(1),
                arenaAnsvarligEnhet = null,
                avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
                apentForPamelding = true,
                antallPlasser = null,
                avtaleId = null,
                deltidsprosent = 100.0,
            )

            service.upsertTiltaksgjennomforing(arenaGjennomforing)
            service.upsertTiltaksgjennomforing(arenaGjennomforing)

            // Verifiserer at duplikater ikke blir publisert
            verify(exactly = 1) {
                gjennomforingKafkaProducer.publish(
                    match {
                        it.navn == gjennomforing1.navn
                    },
                )
            }
        }
    }
})
