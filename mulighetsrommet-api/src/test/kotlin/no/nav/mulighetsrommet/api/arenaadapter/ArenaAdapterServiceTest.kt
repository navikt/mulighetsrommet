package no.nav.mulighetsrommet.api.arenaadapter

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingArenaService
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingAvtaleService
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingEnkeltplassService
import no.nav.mulighetsrommet.api.gjennomforing.service.TEST_GJENNOMFORING_V2_TOPIC
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeFeature
import no.nav.mulighetsrommet.arena.ArenaGjennomforingDbo
import no.nav.mulighetsrommet.arena.Avslutningsstatus
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import java.time.LocalDate
import java.util.UUID

class ArenaAdapterServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    fun createArenaAdapterService(
        sanityService: SanityService = mockk(relaxed = true),
        features: Map<Tiltakskode, Set<TiltakstypeFeature>> = mapOf(),
    ) = ArenaAdapterService(
        db = database.db,
        sanityService = sanityService,
        arrangorService = ArrangorService(database.db, mockk(relaxed = true), mockk(relaxed = true)),
        tiltakstypeService = TiltakstypeService(TiltakstypeService.Config(features), database.db),
        gjennomforingEnkeltplassService = GjennomforingEnkeltplassService(
            GjennomforingEnkeltplassService.Config(TEST_GJENNOMFORING_V2_TOPIC),
            database.db,
        ),
        gjennomforingAvtaleService = GjennomforingAvtaleService(
            GjennomforingAvtaleService.Config(TEST_GJENNOMFORING_V2_TOPIC),
            db = database.db,
            navAnsattService = mockk(),
        ),
        gjennomforingArenaService = GjennomforingArenaService(
            GjennomforingArenaService.Config(TEST_GJENNOMFORING_V2_TOPIC),
            database.db,
        ),
    )

    context("tiltak i egen regi") {
        val gjennomforing = ArenaGjennomforingDbo(
            id = UUID.randomUUID(),
            sanityId = null,
            navn = "IPS",
            arenaKode = TiltakstypeFixtures.IPS.arenaKode,
            tiltaksnummer = "2020#12345",
            arrangorOrganisasjonsnummer = "976663934",
            startDato = LocalDate.now(),
            sluttDato = LocalDate.now().plusYears(1),
            arenaAnsvarligEnhet = null,
            avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
            apentForPamelding = true,
            antallPlasser = 10,
            avtaleId = null,
            deltidsprosent = 100.0,
        )

        beforeEach {
            MulighetsrommetTestDomain(
                navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
                tiltakstyper = listOf(TiltakstypeFixtures.IPS),
            ).initialize(database.db)
        }

        afterEach {
            database.truncateAll()
        }

        test("should not upsert egen regi-tiltak") {
            val service = createArenaAdapterService()

            service.upsertTiltaksgjennomforing(gjennomforing)

            database.run {
                queries.gjennomforing.getAll().items.shouldBeEmpty()
            }
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
            val service = createArenaAdapterService()

            service.upsertTiltaksgjennomforing(gjennomforing)

            database.run {
                queries.kafkaProducerRecord.getRecords(10).shouldHaveSize(0)
            }
        }
    }

    context("gruppetiltak") {
        afterEach {
            database.truncateAll()
        }

        test("opprettes ikke når tiltakstype ikke er migrert") {
            MulighetsrommetTestDomain(
                navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(AvtaleFixtures.oppfolging),
            ).initialize(database.db)

            val service = createArenaAdapterService(
                features = mapOf(Tiltakskode.OPPFOLGING to setOf()),
            )

            val arenaGjennomforing = ArenaGjennomforingDbo(
                id = UUID.randomUUID(),
                navn = "Oppfølging",
                sanityId = null,
                arenaKode = TiltakstypeFixtures.Oppfolging.arenaKode,
                tiltaksnummer = "2020#12345",
                arrangorOrganisasjonsnummer = "976663934",
                startDato = LocalDate.now(),
                sluttDato = LocalDate.now().plusYears(1),
                arenaAnsvarligEnhet = null,
                avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
                apentForPamelding = true,
                antallPlasser = 10,
                avtaleId = null,
                deltidsprosent = 100.0,
            )

            val exception = shouldThrowExactly<IllegalArgumentException> {
                service.upsertTiltaksgjennomforing(arenaGjennomforing)
            }
            exception.message shouldBe "Forventet ikke å motta nye gjennomføringer for tiltakskode=INDOPPFAG fordi alle gruppetiltak skal være migrert"
        }

        test("oppdaterer arena-felter når tiltakstype er migrert") {
            val gjennomforing1 = GjennomforingFixtures.Oppfolging1.copy(
                startDato = LocalDate.now(),
                sluttDato = LocalDate.now().plusDays(1),
            )

            MulighetsrommetTestDomain(
                navEnheter = listOf(
                    NavEnhetFixtures.Innlandet,
                    NavEnhetFixtures.Gjovik,
                    NavEnhetFixtures.Oslo,
                    NavEnhetFixtures.TiltakOslo,
                ),
                arrangorer = listOf(
                    ArrangorFixtures.hovedenhet,
                    ArrangorFixtures.underenhet1,
                    ArrangorFixtures.underenhet2,
                ),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(AvtaleFixtures.oppfolging),
                gjennomforinger = listOf(gjennomforing1),
            ).initialize(database.db)

            val arenaDbo = ArenaGjennomforingDbo(
                id = gjennomforing1.id,
                sanityId = null,
                navn = "Endet navn",
                arenaKode = TiltakstypeFixtures.Oppfolging.arenaKode,
                tiltaksnummer = "2024#2024",
                arrangorOrganisasjonsnummer = ArrangorFixtures.underenhet2.organisasjonsnummer.value,
                startDato = LocalDate.of(2024, 1, 1),
                sluttDato = LocalDate.of(2024, 1, 1),
                arenaAnsvarligEnhet = NavEnhetFixtures.TiltakOslo.enhetsnummer.value,
                avslutningsstatus = Avslutningsstatus.AVSLUTTET,
                apentForPamelding = false,
                antallPlasser = 100,
                avtaleId = null,
                deltidsprosent = 1.0,
            )

            val service = createArenaAdapterService(
                features = mapOf(Tiltakskode.OPPFOLGING to setOf(TiltakstypeFeature.MIGRERT)),
            )

            service.upsertTiltaksgjennomforing(arenaDbo)

            database.run {
                queries.gjennomforing.getGjennomforingAvtaleOrError(gjennomforing1.id).should {
                    it.arena?.tiltaksnummer shouldBe Tiltaksnummer("2024#2024")
                    it.arena?.ansvarligNavEnhet shouldBe "0387"
                    it.status shouldBe GjennomforingStatusType.GJENNOMFORES
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

        test("skal publisere til kafka når det er endringer fra Arena") {
            val gjennomforing1 = GjennomforingFixtures.Oppfolging1

            MulighetsrommetTestDomain(
                navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(AvtaleFixtures.oppfolging),
                gjennomforinger = listOf(gjennomforing1),
            ).initialize(database.db)

            val service = createArenaAdapterService(
                features = mapOf(Tiltakskode.OPPFOLGING to setOf(TiltakstypeFeature.MIGRERT)),
            )

            val arenaGjennomforing = ArenaGjennomforingDbo(
                id = gjennomforing1.id,
                navn = "Oppfølging",
                sanityId = null,
                arenaKode = TiltakstypeFixtures.Oppfolging.arenaKode,
                tiltaksnummer = "2021#12345",
                arrangorOrganisasjonsnummer = "976663934",
                startDato = LocalDate.now(),
                sluttDato = LocalDate.now().plusYears(1),
                arenaAnsvarligEnhet = null,
                avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
                apentForPamelding = true,
                antallPlasser = 10,
                avtaleId = null,
                deltidsprosent = 100.0,
            )

            service.upsertTiltaksgjennomforing(arenaGjennomforing)

            database.run {
                queries.kafkaProducerRecord.getRecords(10).shouldHaveSize(1).should { (first) ->
                    first.topic shouldBe TEST_GJENNOMFORING_V2_TOPIC
                    first.key shouldBe gjennomforing1.id.toString().toByteArray()
                    Json.decodeFromString<TiltaksgjennomforingV2Dto>(first.value.decodeToString()).id shouldBe gjennomforing1.id
                }
            }

            // Ny upsert med samme payload
            service.upsertTiltaksgjennomforing(arenaGjennomforing)

            // Verifiser at ny upsert ikke produserer meldinger når payload er den samme
            database.run {
                queries.kafkaProducerRecord.getRecords(10).shouldHaveSize(1)
            }
        }
    }

    context("enkeltplasser") {
        beforeEach {
            MulighetsrommetTestDomain(
                navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
                tiltakstyper = listOf(TiltakstypeFixtures.EnkelAmo),
            ).initialize(database.db)
        }

        afterEach {
            database.truncateAll()
        }

        val arenaGjennomforing = ArenaGjennomforingDbo(
            id = UUID.randomUUID(),
            navn = "En enkeltplass",
            sanityId = null,
            arenaKode = TiltakstypeFixtures.EnkelAmo.arenaKode,
            tiltaksnummer = "2025#1",
            arrangorOrganisasjonsnummer = "976663934",
            startDato = LocalDate.now(),
            sluttDato = null,
            arenaAnsvarligEnhet = "0400",
            avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
            apentForPamelding = true,
            antallPlasser = 1,
            avtaleId = null,
            deltidsprosent = 100.0,
        )

        test("oppretter og endrer enkeltplasser fra Arena") {
            val service = createArenaAdapterService()

            service.upsertTiltaksgjennomforing(arenaGjennomforing)

            database.run {
                queries.gjennomforing.getGjennomforingEnkeltplassOrError(arenaGjennomforing.id).should {
                    it.tiltakstype.id shouldBe TiltakstypeFixtures.EnkelAmo.id
                    it.arrangor.organisasjonsnummer shouldBe Organisasjonsnummer("976663934")
                    it.navn shouldBe "En enkeltplass"
                    it.status shouldBe GjennomforingStatusType.GJENNOMFORES
                }
            }

            service.upsertTiltaksgjennomforing(
                arenaGjennomforing.copy(
                    arenaAnsvarligEnhet = "1000",
                    avslutningsstatus = Avslutningsstatus.AVSLUTTET,
                ),
            )

            database.run {
                queries.gjennomforing.getGjennomforingEnkeltplassOrError(arenaGjennomforing.id).should {
                    it.status shouldBe GjennomforingStatusType.AVSLUTTET
                    it.arena?.ansvarligNavEnhet shouldBe "1000"
                }
            }
        }

        test("oppretter enkeltplass som Arena-gjennomføring når sluttdato er før EnkeltplassSluttDatoCutoffDate") {
            val service = createArenaAdapterService()

            service.upsertTiltaksgjennomforing(
                arenaGjennomforing.copy(
                    startDato = LocalDate.of(2023, 1, 1),
                    sluttDato = LocalDate.of(2024, 1, 1),
                    avslutningsstatus = Avslutningsstatus.AVSLUTTET,
                ),
            )

            database.run {
                queries.gjennomforing.getGjennomforingArenaOrError(arenaGjennomforing.id).should {
                    it.tiltakstype.id shouldBe TiltakstypeFixtures.EnkelAmo.id
                    it.arrangor.organisasjonsnummer shouldBe Organisasjonsnummer("976663934")
                    it.navn shouldBe "En enkeltplass"
                    it.status shouldBe GjennomforingStatusType.AVSLUTTET
                }
            }
        }

        test("skal publisere til kafka kun når det er endringer fra Arena") {
            val service = createArenaAdapterService()

            service.upsertTiltaksgjennomforing(arenaGjennomforing)

            database.run {
                val record = queries.kafkaProducerRecord.getRecords(10).shouldHaveSize(1).first()
                record.topic shouldBe TEST_GJENNOMFORING_V2_TOPIC
                record.key shouldBe arenaGjennomforing.id.toString().toByteArray()

                val decoded = Json.decodeFromString<TiltaksgjennomforingV2Dto>(record.value.decodeToString())
                decoded.id shouldBe arenaGjennomforing.id
                decoded.tiltakskode shouldBe Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING
                decoded.arrangor.organisasjonsnummer shouldBe Organisasjonsnummer("976663934")
            }

            // Ny upsert med samme payload
            service.upsertTiltaksgjennomforing(arenaGjennomforing)

            // Verifiser at ny upsert ikke produserer meldinger når payload er den samme
            database.run {
                queries.kafkaProducerRecord.getRecords(10).shouldHaveSize(1)
            }

            // Ny upsert med ny data
            service.upsertTiltaksgjennomforing(arenaGjennomforing.copy(navn = "Nytt navn"))

            // Verifiser at upsert med ny data produserer meldinger
            database.run {
                queries.kafkaProducerRecord.getRecords(10).shouldHaveSize(2)
            }
        }

        test("tillater ikke opprettelse fra Arena når tiltakstype er migrert") {
            val service = createArenaAdapterService(
                features = mapOf(
                    Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to setOf(TiltakstypeFeature.MIGRERT),
                ),
            )

            val exception = shouldThrowExactly<IllegalStateException> {
                service.upsertTiltaksgjennomforing(arenaGjennomforing)
            }
            exception.message shouldBe "Tiltakstype tiltakskode=ENKELAMO er migrert, men gjennomføring fra Arena er ukjent"
        }

        test("oppdaterer bare arenadata når tiltakstype er migrert") {
            val gjennomforing = GjennomforingFixtures.EnkelAmo.copy(
                id = arenaGjennomforing.id,
                navn = "Gammelt navn",
            )
            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.db)

            val service = createArenaAdapterService(
                features = mapOf(
                    Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to setOf(TiltakstypeFeature.MIGRERT),
                ),
            )

            service.upsertTiltaksgjennomforing(arenaGjennomforing)

            database.run {
                queries.gjennomforing.getGjennomforingEnkeltplassOrError(arenaGjennomforing.id).should {
                    it.navn shouldBe "Gammelt navn"
                    it.arena?.tiltaksnummer shouldBe Tiltaksnummer("2025#1")
                    it.arena?.ansvarligNavEnhet shouldBe "0400"
                }
            }
        }
    }
})
