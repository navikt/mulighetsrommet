package no.nav.mulighetsrommet.api.gjennomforing.service

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.mockk
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.amo.OpplaringKategorisering
import no.nav.mulighetsrommet.api.amo.OpplaringKategoriseringRequest
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.BransjeFixtures
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixtures
import no.nav.mulighetsrommet.api.fixtures.ForerkortFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.KurstypeFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.PrismodellFixtures
import no.nav.mulighetsrommet.api.fixtures.UtdanningFixtures
import no.nav.mulighetsrommet.api.janzz.Sertifisering
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeFeature
import no.nav.mulighetsrommet.api.tiltakstype.service.TiltakstypeService
import no.nav.mulighetsrommet.api.totrinnskontroll.TotrinnskontrollService
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollBesluttelse
import no.nav.mulighetsrommet.api.utbetaling.model.Deltakelsesmengde
import no.nav.mulighetsrommet.api.vedtak.Opplaeringtilskudd
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.NorskIdentHasher
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class GjennomforingEnkeltplassServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
        prismodeller = listOf(PrismodellFixtures.AnnenAvtaltPris.copy(totalbelop = 1000)),
        gjennomforinger = listOf(GjennomforingFixtures.EnkelAmo),
    )

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    fun createService(
        features: Map<Tiltakskode, Set<TiltakstypeFeature>> = mapOf(),
    ): GjennomforingEnkeltplassService {
        return GjennomforingEnkeltplassService(
            config = GjennomforingEnkeltplassService.Config(TEST_GJENNOMFORING_V2_TOPIC),
            db = database.db,
            personaliaService = mockk(),
            tiltakstyper = TiltakstypeService(TiltakstypeService.Config(features), database.db),
            totrinnskontroll = TotrinnskontrollService(""),
        )
    }

    val norskIdent = NorskIdent("12345678910")

    val migrert = mapOf(Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to setOf(TiltakstypeFeature.MIGRERT))

    val opprettetAv = NavAnsattFixture.DonaldDuck.navIdent
    val besluttetAv = NavAnsattFixture.MikkeMus.navIdent

    fun createEnkeltplass(
        kategorisering: OpplaringKategoriseringRequest? = null,
    ) = UpsertGjennomforingEnkeltplass(
        id = UUID.randomUUID(),
        tiltakskode = Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        arrangorId = GjennomforingFixtures.EnkelAmo.arrangorId,
        startDato = LocalDate.of(2025, 1, 1),
        sluttDato = LocalDate.of(2025, 6, 1),
        status = GjennomforingStatusType.GJENNOMFORES,
        ansvarligEnhet = GjennomforingFixtures.EnkelAmo.ansvarligEnhet!!,
        prismodell = UpsertGjennomforingEnkeltplass.Prismodell.Anskaffelse(
            totalbelop = 1000,
        ),
        kategorisering = kategorisering,
    )

    context("opprettUtkast") {
        val service = createService()

        test("oppretter enkeltplass uten å sende økonomi til godkjenning") {
            val upsert = createEnkeltplass()

            service.opprettUtkast(upsert).shouldBeRight()

            service.get(upsert.id).shouldNotBeNull().should { (gjennomforing, okonomi) ->
                gjennomforing.id shouldBe upsert.id
                gjennomforing.status shouldBe GjennomforingStatusType.GJENNOMFORES
                okonomi.shouldBeNull()
            }
        }

        test("oppdaterer ikke eksisterende enkeltplass") {
            val upsert = createEnkeltplass()
            service.opprettUtkast(upsert).shouldBeRight()

            val oppdatertPris = UpsertGjennomforingEnkeltplass.Prismodell.Anskaffelse(
                totalbelop = 999,
            )
            val (gjennomforing) = service.opprettUtkast(upsert.copy(prismodell = oppdatertPris)).shouldBeRight()

            gjennomforing.prismodell.shouldBeTypeOf<Prismodell.AnnenAvtaltPris>().should {
                it.totalbelop shouldBe 1000
            }
        }

        test("lagrer kategorisering amo") {
            val kategorisering = OpplaringKategoriseringRequest(
                bransjeId = BransjeFixtures.byggOgAnlegg.id,
                forerkort = setOf(ForerkortFixtures.B, ForerkortFixtures.BE).map { it.id },
                sertifiseringer = setOf(Sertifisering(konseptId = 1234, label = "Truckførerkurs")),
            )
            val gjennomforing = createEnkeltplass(kategorisering)

            service.opprettUtkast(gjennomforing).shouldBeRight()

            database.run {
                queries.opplaringKategorisering.getGjennomforingKategorisering(gjennomforing.id).shouldBe(
                    OpplaringKategorisering(
                        kurstype = KurstypeFixtures.bransjeOgYrkesrettet,
                        bransje = BransjeFixtures.byggOgAnlegg,
                        forerkort = setOf(ForerkortFixtures.B, ForerkortFixtures.BE),
                        sertifiseringer = setOf(Sertifisering(konseptId = 1234, label = "Truckførerkurs")),
                        norskprove = false,
                    ),
                )
            }
        }

        test("lagrer kategorisering norskopplaering, grunnleggende ferdigheter og FOV") {
            val request = OpplaringKategoriseringRequest(
                kurstypeId = KurstypeFixtures.fov.id,
            )
            val gjennomforing =
                createEnkeltplass(request).copy(tiltakskode = Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV)

            service.opprettUtkast(
                gjennomforing,
            ).shouldBeRight()

            database.run {
                queries.opplaringKategorisering.getGjennomforingKategorisering(gjennomforing.id).shouldBe(
                    OpplaringKategorisering(kurstype = KurstypeFixtures.fov, norskprove = false),
                )
            }
        }

        test("lagrer kategorisering fag og yrke") {
            val request = OpplaringKategoriseringRequest(
                utdanningsprogramId = UtdanningFixtures.UtdanningsProgram.byggOgAnlegg.id,
                larefag = listOf(UtdanningFixtures.Utdanninger.fjellOgBergverksfaget.id),
            )
            val gjennomforing = createEnkeltplass(request)

            service.opprettUtkast(gjennomforing).shouldBeRight()

            database.run {
                val utdanningslop = queries.gjennomforing.getUtdanningslop(gjennomforing.id).shouldNotBeNull()
                utdanningslop.utdanningsprogram.id.shouldBe(request.utdanningsprogramId)
                utdanningslop.utdanninger.map { it.id }.shouldContainExactly(request.larefag?.first())
            }
        }

        test("publiseres til kafka når gjennomføring opprettes") {
            val upsert = createEnkeltplass()

            service.opprettUtkast(upsert).shouldBeRight()

            database.run {
                queries.kafkaProducerRecord.getRecords(10).shouldHaveSize(1).should { (first) ->
                    first.topic shouldBe TEST_GJENNOMFORING_V2_TOPIC
                    first.key shouldBe upsert.id.toString().toByteArray()
                    first.value.decodeToString()
                        .let { Json.decodeFromString<TiltaksgjennomforingV2Dto>(it) }
                        .shouldBeTypeOf<TiltaksgjennomforingV2Dto.Enkeltplass>()
                }
            }
        }

        test("publiserer ikke til kafka når gjennomføring allerede eksisterer") {
            val upsert = createEnkeltplass()
            service.opprettUtkast(upsert).shouldBeRight()

            service.opprettUtkast(upsert.copy(status = GjennomforingStatusType.AVSLUTTET)).shouldBeRight()

            database.run {
                queries.kafkaProducerRecord.getRecords(10).shouldHaveSize(1)
            }
        }
    }

    context("soktInn") {
        val service = createService()

        test("oppretter enkeltplass og sender økonomi til godkjenning") {
            val upsert = createEnkeltplass()

            val (_, okonomi) = service.soktInn(upsert, opprettetAv).shouldBeRight()

            okonomi.shouldNotBeNull().should {
                it.behandletAv shouldBe opprettetAv
                it.besluttetAv.shouldBeNull()
                it.besluttelse.shouldBeNull()
            }
        }

        test("beholder referanse til samme prismodell ved oppdatering av økonomi") {
            val upsert = createEnkeltplass()

            val enkeltplass1 = service.soktInn(upsert, opprettetAv).shouldBeRight()

            enkeltplass1.gjennomforing.prismodell.shouldBeTypeOf<Prismodell.AnnenAvtaltPris>()

            val prismodell = UpsertGjennomforingEnkeltplass.Prismodell.TilskuddTilOpplaering(
                tilskudd = mapOf(Opplaeringtilskudd.Kode.SKOLEPENGER to 100),
                tilleggsopplysninger = null,
            )
            val enkeltplass2 = service
                .soktInn(upsert.copy(prismodell = prismodell), opprettetAv)
                .shouldBeRight()

            enkeltplass2.gjennomforing.prismodell.shouldBeTypeOf<Prismodell.TilskuddTilOpplaering>().should {
                it.id shouldBe enkeltplass1.gjennomforing.prismodell.id
            }
        }

        test("sender økonomi til godkjenning på nytt etter at økonomi er satt på vent") {
            val upsert = createEnkeltplass()
            service.soktInn(upsert, opprettetAv).shouldBeRight()
            service.settOkonomiPaVent(upsert.id, besluttetAv, forklaring = "Feil prisbetingelser").shouldBeRight()

            val prismodell = UpsertGjennomforingEnkeltplass.Prismodell.TilskuddTilOpplaering(
                tilskudd = mapOf(Opplaeringtilskudd.Kode.SKOLEPENGER to 100),
                tilleggsopplysninger = null,
            )
            val (gjennomforing, okonomi) = service
                .soktInn(upsert.copy(prismodell = prismodell), opprettetAv)
                .shouldBeRight()

            gjennomforing.prismodell.shouldBeTypeOf<Prismodell.TilskuddTilOpplaering>().should {
                it.tilskudd shouldBe prismodell.tilskudd
            }

            okonomi.shouldNotBeNull().should {
                it.besluttetAv.shouldBeNull()
                it.besluttelse.shouldBeNull()
            }
        }

        test("gjør ingenting dersom økonomi allerede er GODKJENT") {
            val upsert = createEnkeltplass()
            service.soktInn(upsert, opprettetAv).shouldBeRight()
            service.settOkonomiGodkjent(upsert.id, besluttetAv).shouldBeRight()

            val prismodell = UpsertGjennomforingEnkeltplass.Prismodell.TilskuddTilOpplaering(
                tilskudd = mapOf(Opplaeringtilskudd.Kode.SKOLEPENGER to 100),
                tilleggsopplysninger = null,
            )
            val (gjennomforing, okonomi) = service
                .soktInn(upsert.copy(prismodell = prismodell), opprettetAv)
                .shouldBeRight()

            gjennomforing.prismodell.shouldBeTypeOf<Prismodell.AnnenAvtaltPris>().should {
                it.totalbelop shouldBe 1000
            }

            okonomi.shouldNotBeNull().should {
                it.besluttelse shouldBe TotrinnskontrollBesluttelse.GODKJENT
                it.besluttetAv shouldBe besluttetAv
            }
        }

        test("økonomi godkjennes automatisk av Tiltaksadministrasjon ved IngenKostnader") {
            val prismodell = UpsertGjennomforingEnkeltplass.Prismodell.IngenKostnader(
                aarsak = Prismodell.IngenKostnader.Aarsak.OPPLAERINGEN_ER_KOSTNADSFRI,
                tilleggsopplysninger = null,
            )
            val upsert = createEnkeltplass().copy(prismodell = prismodell)

            val (_, okonomi) = service.soktInn(upsert, opprettetAv).shouldBeRight()

            okonomi.shouldNotBeNull().should {
                it.behandletAv shouldBe opprettetAv
                it.besluttetAv shouldBe Tiltaksadministrasjon
                it.besluttelse shouldBe TotrinnskontrollBesluttelse.GODKJENT
            }
        }

        test("publiseres til kafka når gjennomføring opprettes") {
            val upsert = createEnkeltplass()

            service.soktInn(upsert, opprettetAv).shouldBeRight()

            database.run {
                queries.kafkaProducerRecord.getRecords(10, listOf(TEST_GJENNOMFORING_V2_TOPIC)).shouldHaveSize(1)
                    .should { (first) ->
                        first.key shouldBe upsert.id.toString().toByteArray()
                        first.value.decodeToString()
                            .let { Json.decodeFromString<TiltaksgjennomforingV2Dto>(it) }
                            .shouldBeTypeOf<TiltaksgjennomforingV2Dto.Enkeltplass>()
                    }
            }
        }
    }

    context("synkroniserFraArena") {
        val service = createService()

        test("oppretter enkeltplass uten å sende økonomi til godkjenning") {
            val upsert = createEnkeltplass()

            service.synkroniserFraArena(upsert).shouldBeRight()

            service.get(upsert.id).shouldNotBeNull().okonomi.shouldBeNull()
        }

        test("publiseres til kafka når gjennomføring opprettes") {
            val upsert = createEnkeltplass()

            service.synkroniserFraArena(upsert).shouldBeRight()

            database.run {
                queries.kafkaProducerRecord.getRecords(10).shouldHaveSize(1).should { (first) ->
                    first.topic shouldBe TEST_GJENNOMFORING_V2_TOPIC
                    first.key shouldBe upsert.id.toString().toByteArray()
                    first.value.decodeToString()
                        .let { Json.decodeFromString<TiltaksgjennomforingV2Dto>(it) }
                        .shouldBeTypeOf<TiltaksgjennomforingV2Dto.Enkeltplass>()
                }
            }
        }

        test("publiserer ikke til kafka når ingenting er endret") {
            val upsert = createEnkeltplass().copy(navn = "Enkeltplass AMO")
            service.synkroniserFraArena(upsert).shouldBeRight()

            service.synkroniserFraArena(upsert).shouldBeRight()

            database.run {
                queries.kafkaProducerRecord.getRecords(10).shouldHaveSize(1)
            }
        }
    }

    context("behandling av økonomi for enkeltplasser") {
        val service = createService()

        test("godkjenner økonomi og setter besluttelse til GODKJENT") {
            val upsert = createEnkeltplass()

            service.soktInn(upsert, NavIdent("B123456")).shouldBeRight()

            val (_, okonomi) = service.settOkonomiGodkjent(upsert.id, besluttetAv).shouldBeRight()

            okonomi.shouldNotBeNull().should {
                it.besluttelse shouldBe TotrinnskontrollBesluttelse.GODKJENT
                it.besluttetAv shouldBe besluttetAv
            }
        }

        test("returnerer feil når behandletAv og besluttetAv er samme person") {
            val upsert = createEnkeltplass()
            service.soktInn(upsert, opprettetAv).shouldBeRight()

            service.settOkonomiGodkjent(upsert.id, opprettetAv)
                .shouldBeLeft()
                .first().detail shouldBe "Du kan ikke beslutte noe du selv har behandlet"
        }

        test("kan godkjenne enkeltplass etter avvisning") {
            val upsert = createEnkeltplass()
            service.soktInn(upsert, opprettetAv).shouldBeRight()

            service.settOkonomiPaVent(
                upsert.id,
                besluttetAv,
                forklaring = "Feil prisbetingelser",
            ).shouldBeRight()

            val (_, okonomi) = service.settOkonomiGodkjent(upsert.id, besluttetAv).shouldBeRight()

            okonomi.shouldNotBeNull().should {
                it.besluttelse shouldBe TotrinnskontrollBesluttelse.GODKJENT
                it.forklaring shouldBe null
            }
        }

        test("setter økonomi på vent og setter besluttelse til AVVIST") {
            val upsert = createEnkeltplass()
            service.soktInn(upsert, opprettetAv).shouldBeRight()

            val (_, okonomi) = service.settOkonomiPaVent(upsert.id, besluttetAv, forklaring = "Feil").shouldBeRight()

            okonomi.shouldNotBeNull().should {
                it.besluttelse shouldBe TotrinnskontrollBesluttelse.AVVIST
                it.besluttetAv shouldBe besluttetAv
                it.forklaring shouldBe "Feil"
            }
        }

        test("returnerer feil når enkeltplass allerede er behandlet") {
            val upsert = createEnkeltplass()
            service.soktInn(upsert, opprettetAv).shouldBeRight()

            service.settOkonomiGodkjent(upsert.id, besluttetAv).shouldBeRight()

            service.settOkonomiGodkjent(upsert.id, besluttetAv)
                .shouldBeLeft()
                .first().detail shouldBe "Totrinnskontrollen er allerede godkjent"

            service.settOkonomiPaVent(upsert.id, besluttetAv, forklaring = "Angret")
                .shouldBeLeft()
                .first().detail shouldBe "Totrinnskontrollen er allerede godkjent"
        }
    }

    context("updateFromDeltaker") {
        val service = createService()

        test("lagrer hash av norsk ident i fritekstsøk for aktiv deltaker") {
            val deltaker = DeltakerFixtures.createDeltaker(
                gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                status = DeltakerStatusType.DELTAR,
            )

            service.updateFromDeltaker(deltaker, norskIdent)

            database.run {
                queries.gjennomforing.getAll(search = "12345678910").items.shouldBeEmpty()
                queries.gjennomforing.getAll(
                    search = NorskIdentHasher.hashIfNorskIdent("12345678910"),
                ).items.shouldHaveSize(1).first().id shouldBe GjennomforingFixtures.EnkelAmo.id
            }
        }

        test("lagrer ikke norsk ident i fritekstsøk når deltaker er FEILREGISTRERT") {
            val deltaker = DeltakerFixtures.createDeltaker(
                gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                status = DeltakerStatusType.FEILREGISTRERT,
            )

            service.updateFromDeltaker(deltaker, norskIdent)

            database.run {
                queries.gjennomforing.getAll(
                    search = NorskIdentHasher.hashIfNorskIdent("12345678910"),
                ).items.shouldBeEmpty()
            }
        }

        context("validering av én deltaker per enkeltplass") {
            test("godtar deltakeren som allerede er tilknyttet gjennomføringen") {
                val eksisterendeDeltaker = DeltakerFixtures.createDeltakerDbo(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                )
                MulighetsrommetTestDomain(
                    deltakere = listOf(eksisterendeDeltaker),
                ).initialize(database.db)

                val deltaker = DeltakerFixtures.createDeltaker(
                    id = eksisterendeDeltaker.id,
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                )

                service.updateFromDeltaker(deltaker, norskIdent)
            }

            test("kaster exception når gjennomføringen allerede har en annen deltaker") {
                val annenDeltaker = DeltakerFixtures.createDeltakerDbo(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                )
                MulighetsrommetTestDomain(
                    deltakere = listOf(annenDeltaker),
                ).initialize(database.db)

                val nyDeltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                )

                shouldThrow<IllegalStateException> {
                    service.updateFromDeltaker(nyDeltaker, norskIdent)
                }.message shouldBe "Enkeltplass med id=${GjennomforingFixtures.EnkelAmo.id} har allerede en annen deltaker"
            }

            test("ignorerer annen deltaker som er FEILREGISTRERT uten å kaste exception") {
                val eksisterendeDeltaker = DeltakerFixtures.createDeltakerDbo(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                )
                MulighetsrommetTestDomain(
                    deltakere = listOf(eksisterendeDeltaker),
                ).initialize(database.db)

                val feilregistrertDeltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.FEILREGISTRERT,
                )

                val (gjennomforing) = service.updateFromDeltaker(feilregistrertDeltaker, norskIdent)

                gjennomforing.startDato shouldBe GjennomforingFixtures.EnkelAmo.startDato
                gjennomforing.sluttDato shouldBe GjennomforingFixtures.EnkelAmo.sluttDato
            }
        }

        context("når tiltakstype ikke er migrert") {
            test("oppdaterer ikke gjennomføring") {
                val deltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                    startDato = LocalDate.of(2026, 1, 1),
                    sluttDato = LocalDate.of(2026, 6, 1),
                )

                val (gjennomforing) = service.updateFromDeltaker(deltaker, norskIdent)

                gjennomforing.startDato shouldBe GjennomforingFixtures.EnkelAmo.startDato
                gjennomforing.sluttDato shouldBe GjennomforingFixtures.EnkelAmo.sluttDato
                gjennomforing.status shouldBe GjennomforingFixtures.EnkelAmo.status
            }

            test("publiserer ikke til kafka") {
                val deltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                )

                service.updateFromDeltaker(deltaker, norskIdent)

                database.run {
                    queries.kafkaProducerRecord.getRecords(10, listOf(TEST_GJENNOMFORING_V2_TOPIC)).shouldBeEmpty()
                }
            }
        }

        context("når tiltakstype er migrert") {
            test("oppdaterer gjennomføring med datoer og status fra deltaker") {
                val startDato = LocalDate.of(2025, 3, 1)
                val sluttDato = LocalDate.of(2025, 6, 1)

                val deltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                    startDato = startDato,
                    sluttDato = sluttDato,
                )

                val (gjennomforing) = createService(migrert).updateFromDeltaker(deltaker, norskIdent)

                gjennomforing.startDato shouldBe startDato
                gjennomforing.sluttDato shouldBe sluttDato
                gjennomforing.status shouldBe GjennomforingStatusType.GJENNOMFORES
            }

            test("bruker gjennomføringens startdato når deltaker ikke har startdato") {
                val deltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.VENTER_PA_OPPSTART,
                    startDato = null,
                )

                val (gjennomforing) = createService(migrert).updateFromDeltaker(deltaker, norskIdent)

                gjennomforing.startDato shouldBe GjennomforingFixtures.EnkelAmo.startDato
            }

            test("setter status AVBRUTT når deltaker er FEILREGISTRERT") {
                val deltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.FEILREGISTRERT,
                )

                val (gjennomforing) = createService(migrert).updateFromDeltaker(deltaker, norskIdent)

                gjennomforing.status shouldBe GjennomforingStatusType.AVBRUTT
            }

            test("setter status AVSLUTTET når deltaker er FULLFORT") {
                val deltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.FULLFORT,
                )

                val (gjennomforing) = createService(migrert).updateFromDeltaker(deltaker, norskIdent)

                gjennomforing.status shouldBe GjennomforingStatusType.AVSLUTTET
            }

            test("bruker deltakelsesprosent fra siste deltakelsesmengde") {
                val deltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                ).copy(
                    deltakelsesmengder = listOf(
                        Deltakelsesmengde(
                            gyldigFra = LocalDate.of(2025, 1, 1),
                            deltakelsesprosent = 50.0,
                        ),
                        Deltakelsesmengde(
                            gyldigFra = LocalDate.of(2025, 3, 1),
                            deltakelsesprosent = 75.0,
                        ),
                    ),
                )

                val (gjennomforing) = createService(migrert).updateFromDeltaker(deltaker, norskIdent)

                gjennomforing.deltidsprosent shouldBe 75.0
            }

            test("bruker 100 prosent som standardverdi når deltaker ikke har deltakelsesmengder") {
                val deltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                ).copy(deltakelsesmengder = emptyList())

                val (gjennomforing) = createService(migrert).updateFromDeltaker(deltaker, norskIdent)

                gjennomforing.deltidsprosent shouldBe 100.0
            }

            test("persisterer oppdatert gjennomføring i databasen og publiserer til kafka") {
                val startDato = LocalDate.of(2025, 3, 1)
                val sluttDato = LocalDate.of(2025, 6, 1)

                val deltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                    startDato = startDato,
                    sluttDato = sluttDato,
                )

                val (gjennomforing) = createService(migrert).updateFromDeltaker(deltaker, norskIdent)

                gjennomforing.startDato shouldBe startDato
                gjennomforing.sluttDato shouldBe sluttDato
                gjennomforing.status shouldBe GjennomforingStatusType.GJENNOMFORES

                database.run {
                    queries.kafkaProducerRecord.getRecords(10, listOf(TEST_GJENNOMFORING_V2_TOPIC))
                        .shouldHaveSize(1).first().key.decodeToString()
                        .shouldBe(GjennomforingFixtures.EnkelAmo.id.toString())
                }
            }

            test("publiserer ikke gjennomføring til kafka hvis deltaker er uendret") {
                val deltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                )

                val service = createService(migrert)

                service.updateFromDeltaker(deltaker, norskIdent)

                database.run {
                    queries.kafkaProducerRecord.getRecords(10, listOf(TEST_GJENNOMFORING_V2_TOPIC))
                        .shouldHaveSize(1).first().key.decodeToString()
                        .shouldBe(GjennomforingFixtures.EnkelAmo.id.toString())
                }

                service.updateFromDeltaker(deltaker, norskIdent)

                database.run {
                    queries.kafkaProducerRecord.getRecords(10, listOf(TEST_GJENNOMFORING_V2_TOPIC)).shouldHaveSize(1)
                }
            }
        }

        context("relast av deltaker") {
            val tidligereEndretTidspunkt = LocalDateTime.of(2025, 1, 1, 12, 0, 0)
            val nyereEndretTidspunkt = LocalDateTime.of(2025, 6, 1, 12, 0, 0)

            test("prosesserer når deltaker-eventet er samme eller nyere enn lagret") {
                val lagretDeltaker = DeltakerFixtures.createDeltakerDbo(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    endretTidspunkt = tidligereEndretTidspunkt,
                )
                MulighetsrommetTestDomain(
                    deltakere = listOf(lagretDeltaker),
                ).initialize(database.db)

                val service = createService(migrert)

                val deltakerAvbrutt = DeltakerFixtures.createDeltaker(
                    id = lagretDeltaker.id,
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.AVBRUTT,
                    endretTidspunkt = nyereEndretTidspunkt,
                )
                val (gjennomforing) = service.updateFromDeltaker(deltakerAvbrutt, norskIdent)
                gjennomforing.status shouldBe GjennomforingStatusType.AVBRUTT

                val deltakerDeltar = DeltakerFixtures.createDeltaker(
                    id = lagretDeltaker.id,
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                    endretTidspunkt = nyereEndretTidspunkt,
                )
                val (gjennomforing2) = service.updateFromDeltaker(deltakerDeltar, norskIdent)
                gjennomforing2.status shouldBe GjennomforingStatusType.GJENNOMFORES
            }

            test("hopper over når deltaker-eventet er eldre enn lagret") {
                val lagretDeltaker = DeltakerFixtures.createDeltakerDbo(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    endretTidspunkt = nyereEndretTidspunkt,
                )
                MulighetsrommetTestDomain(
                    deltakere = listOf(lagretDeltaker),
                ).initialize(database.db)

                val deltaker = DeltakerFixtures.createDeltaker(
                    id = lagretDeltaker.id,
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.AVBRUTT,
                    endretTidspunkt = tidligereEndretTidspunkt,
                )

                val (gjennomforing) = createService(migrert).updateFromDeltaker(deltaker, norskIdent)

                gjennomforing.status shouldBe GjennomforingStatusType.GJENNOMFORES
            }
        }
    }
})
