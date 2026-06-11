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
import no.nav.mulighetsrommet.api.amo.db.OpplaringKategoriseringQueries
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeFeature
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkType
import no.nav.mulighetsrommet.api.fixtures.BransjeFixtures
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixtures
import no.nav.mulighetsrommet.api.fixtures.ForerkortFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.KurstypeFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.PrismodellFixtures
import no.nav.mulighetsrommet.api.fixtures.UtdanningFixtures
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.janzz.Sertifisering
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.tiltakstype.service.TiltakstypeService
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollStatus
import no.nav.mulighetsrommet.api.utbetaling.model.Deltakelsesmengde
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.NorskIdentHasher
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class GjennomforingEnkeltplassServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

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
            db = database.db,
            personaliaService = mockk(),
            tiltakstyper = TiltakstypeService(TiltakstypeService.Config(features), database.db),
        )
    }

    val norskIdent = NorskIdent("12345678910")

    val migrert = mapOf(Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to setOf(TiltakstypeFeature.MIGRERT))

    val opprettetAv = NavAnsattFixture.DonaldDuck.navIdent
    val besluttetAv = NavAnsattFixture.MikkeMus.navIdent

    fun createRequest(
        kategorisering: OpplaringKategoriseringRequest? = null,
    ) = EnkeltplassRequest(
        id = UUID.randomUUID(),
        tiltakskode = Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        arrangorId = GjennomforingFixtures.EnkelAmo.arrangorId,
        ansvarligEnhet = GjennomforingFixtures.EnkelAmo.ansvarligEnhet!!,
        prismodell = UpsertGjennomforingEnkeltplass.Prismodell.Anskaffelse(
            totalbelop = 1000,
        ),
        kategorisering = kategorisering,
    )

    fun createUpsert(
        id: UUID = UUID.randomUUID(),
        navn: String? = null,
    ) = UpsertGjennomforingEnkeltplass(
        id = id,
        tiltakskode = Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        arrangorId = GjennomforingFixtures.EnkelAmo.arrangorId,
        startDato = LocalDate.of(2025, 1, 1),
        sluttDato = LocalDate.of(2025, 6, 1),
        status = GjennomforingStatusType.GJENNOMFORES,
        ansvarligEnhet = GjennomforingFixtures.EnkelAmo.ansvarligEnhet!!,
        prismodell = UpsertGjennomforingEnkeltplass.Prismodell.Anskaffelse(
            totalbelop = 1000,
        ),
        navn = navn,
    )

    context("opprettUtkast") {
        val service = createService()

        test("oppretter enkeltplass uten å sende økonomi til godkjenning") {
            val utkast = createRequest()

            service.opprettUtkast(utkast, opprettetAv).shouldBeRight()

            service.get(utkast.id).shouldNotBeNull().should { (gjennomforing, okonomi) ->
                gjennomforing.id shouldBe utkast.id
                gjennomforing.status shouldBe GjennomforingStatusType.GJENNOMFORES
                okonomi.shouldBeNull()
            }
        }

        test("oppdaterer ikke eksisterende enkeltplass") {
            val utkast = createRequest()

            service.opprettUtkast(utkast, opprettetAv).shouldBeRight()

            val oppdatertPris = UpsertGjennomforingEnkeltplass.Prismodell.Anskaffelse(
                totalbelop = 999,
            )
            val (gjennomforing) = service.opprettUtkast(utkast.copy(prismodell = oppdatertPris), opprettetAv)
                .shouldBeRight()

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
            val utkast = createRequest(kategorisering)

            service.opprettUtkast(utkast, opprettetAv).shouldBeRight()

            database.run {
                context(this.session) {
                    OpplaringKategoriseringQueries.get(utkast.id).shouldBe(
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
        }

        test("lagrer kategorisering norskopplaering, grunnleggende ferdigheter og FOV") {
            val request = OpplaringKategoriseringRequest(
                kurstypeId = KurstypeFixtures.fov.id,
            )
            val utkast = createRequest(request).copy(
                tiltakskode = Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
            )

            service.opprettUtkast(utkast, opprettetAv).shouldBeRight()

            database.run {
                context(this.session) {
                    OpplaringKategoriseringQueries.get(utkast.id).shouldBe(
                        OpplaringKategorisering(kurstype = KurstypeFixtures.fov, norskprove = false),
                    )
                }
            }
        }

        test("lagrer kategorisering fag og yrke") {
            val request = OpplaringKategoriseringRequest(
                utdanningsprogramId = UtdanningFixtures.UtdanningsProgram.byggOgAnlegg.id,
                larefag = listOf(UtdanningFixtures.Utdanninger.fjellOgBergverksfaget.id),
            )
            val utkast = createRequest(request)

            service.opprettUtkast(utkast, opprettetAv).shouldBeRight()

            database.run {
                val utdanningslop = context(this.session) {
                    OpplaringKategoriseringQueries.get(utkast.id)?.utdanningslop.shouldNotBeNull()
                }
                utdanningslop.utdanningsprogram.id.shouldBe(request.utdanningsprogramId)
                utdanningslop.utdanninger.map { it.id }.shouldContainExactly(request.larefag?.first())
            }
        }

        test("publiseres til kafka når gjennomføring opprettes") {
            val utkast = createRequest()

            service.opprettUtkast(utkast, opprettetAv).shouldBeRight()

            database.run {
                queries.kafkaProducerRecord.getRecords(10).shouldHaveSize(1).should { (first) ->
                    first.topic shouldBe TEST_GJENNOMFORING_V2_TOPIC
                    first.key shouldBe utkast.id.toString().toByteArray()
                    first.value.decodeToString()
                        .let { Json.decodeFromString<TiltaksgjennomforingV2Dto>(it) }
                        .shouldBeTypeOf<TiltaksgjennomforingV2Dto.Enkeltplass>()
                }
            }
        }

        test("publiserer ikke til kafka når gjennomføring allerede eksisterer") {
            val utkast = createRequest()

            service.opprettUtkast(utkast, opprettetAv).shouldBeRight()

            service.opprettUtkast(utkast, opprettetAv).shouldBeRight()

            database.run {
                queries.kafkaProducerRecord.getRecords(10).shouldHaveSize(1)
            }
        }

        test("skriver til endringshistorikk når gjennomføring opprettes") {
            val utkast = createRequest()

            service.opprettUtkast(utkast, opprettetAv).shouldBeRight()

            database.run {
                queries.endringshistorikk.getEndringshistorikk(EndringshistorikkType.GJENNOMFORING, utkast.id)
                    .entries.shouldHaveSize(1).first().should {
                        it.operation shouldBe "Opprettet utkast"
                    }
            }
        }
    }

    context("soktInn") {
        val service = createService()

        test("oppretter enkeltplass og sender økonomi til godkjenning") {
            val soktInn = createRequest()

            val (_, okonomi) = service.soktInn(soktInn, opprettetAv).shouldBeRight()

            okonomi.shouldNotBeNull().should {
                it.status shouldBe TotrinnskontrollStatus.TIL_BEHANDLING
                it.behandletAv shouldBe opprettetAv
                it.besluttetAv.shouldBeNull()
            }
        }

        test("beholder referanse til samme prismodell ved oppdatering av økonomi") {
            val soktInn = createRequest()

            val enkeltplass1 = service.soktInn(soktInn, opprettetAv).shouldBeRight()

            enkeltplass1.gjennomforing.prismodell.shouldBeTypeOf<Prismodell.AnnenAvtaltPris>()

            val prismodell = UpsertGjennomforingEnkeltplass.Prismodell.TilskuddTilOpplaering(
                tilskudd = mapOf(Opplaeringtilskudd.Kode.SKOLEPENGER to 100),
                tilleggsopplysninger = null,
            )
            val enkeltplass2 = service
                .soktInn(soktInn.copy(prismodell = prismodell), opprettetAv)
                .shouldBeRight()

            enkeltplass2.gjennomforing.prismodell.shouldBeTypeOf<Prismodell.TilskuddTilOpplaering>().should {
                it.id shouldBe enkeltplass1.gjennomforing.prismodell.id
            }
        }

        test("sender økonomi til godkjenning på nytt etter at økonomi er satt på vent") {
            val soktInn = createRequest()
            service.soktInn(soktInn, opprettetAv).shouldBeRight()
            service.settOkonomiPaVent(soktInn.id, besluttetAv, forklaring = "Feil prisbetingelser").shouldBeRight()

            val prismodell = UpsertGjennomforingEnkeltplass.Prismodell.TilskuddTilOpplaering(
                tilskudd = mapOf(Opplaeringtilskudd.Kode.SKOLEPENGER to 100),
                tilleggsopplysninger = null,
            )
            val (gjennomforing, okonomi) = service
                .soktInn(soktInn.copy(prismodell = prismodell), opprettetAv)
                .shouldBeRight()

            gjennomforing.prismodell.shouldBeTypeOf<Prismodell.TilskuddTilOpplaering>().should {
                it.tilskudd shouldBe prismodell.tilskudd
            }

            okonomi.shouldNotBeNull().should {
                it.status shouldBe TotrinnskontrollStatus.TIL_BEHANDLING
                it.besluttetAv.shouldBeNull()
            }
        }

        test("gjør ingenting dersom økonomi allerede er GODKJENT") {
            val soktInn = createRequest()
            service.soktInn(soktInn, opprettetAv).shouldBeRight()
            service.settOkonomiGodkjent(soktInn.id, besluttetAv).shouldBeRight()

            val prismodell = UpsertGjennomforingEnkeltplass.Prismodell.TilskuddTilOpplaering(
                tilskudd = mapOf(Opplaeringtilskudd.Kode.SKOLEPENGER to 100),
                tilleggsopplysninger = null,
            )
            val (gjennomforing, okonomi) = service
                .soktInn(soktInn.copy(prismodell = prismodell), opprettetAv)
                .shouldBeRight()

            gjennomforing.prismodell.shouldBeTypeOf<Prismodell.AnnenAvtaltPris>().should {
                it.totalbelop shouldBe 1000
            }

            okonomi.shouldNotBeNull().should {
                it.status shouldBe TotrinnskontrollStatus.GODKJENT
                it.besluttetAv shouldBe besluttetAv
            }
        }

        test("publiseres til kafka når gjennomføring opprettes") {
            val soktInn = createRequest()

            service.soktInn(soktInn, opprettetAv).shouldBeRight()

            database.run {
                queries.kafkaProducerRecord.getRecords(10, listOf(TEST_GJENNOMFORING_V2_TOPIC)).shouldHaveSize(1)
                    .should { (first) ->
                        first.key shouldBe soktInn.id.toString().toByteArray()
                        first.value.decodeToString()
                            .let { Json.decodeFromString<TiltaksgjennomforingV2Dto>(it) }
                            .shouldBeTypeOf<TiltaksgjennomforingV2Dto.Enkeltplass>()
                    }
            }
        }

        test("bevarer datoer og status satt av updateFromDeltaker") {
            val service = createService(migrert)
            val soktInn = createRequest()

            service.soktInn(soktInn, opprettetAv).shouldBeRight().should { (gjennomforing) ->
                gjennomforing.startDato shouldBe null
                gjennomforing.sluttDato shouldBe null
                gjennomforing.deltidsprosent shouldBe 100.0
            }

            val startDato = LocalDate.of(2025, 3, 1)
            val sluttDato = LocalDate.of(2025, 9, 1)
            val deltaker = DeltakerFixtures.createDeltaker(
                id = UUID.randomUUID(),
                gjennomforingId = soktInn.id,
                status = DeltakerStatusType.DELTAR,
                startDato = startDato,
                sluttDato = sluttDato,
            ).copy(deltakelsesmengder = listOf(Deltakelsesmengde(gyldigFra = startDato, deltakelsesprosent = 60.0)))
            service.updateFromDeltaker(deltaker, NorskIdent("12345678910"))

            service.soktInn(soktInn, opprettetAv).shouldBeRight().should { (gjennomforing) ->
                gjennomforing.startDato shouldBe startDato
                gjennomforing.sluttDato shouldBe sluttDato
                gjennomforing.deltidsprosent shouldBe 60.0
            }
        }
    }

    context("synkroniserFraArena") {
        val service = createService()

        test("oppretter enkeltplass uten å sende økonomi til godkjenning") {
            val upsert = createUpsert()

            service.synkroniserFraArena(upsert).shouldBeRight()

            service.get(upsert.id).shouldNotBeNull().okonomi.shouldBeNull()
        }

        test("publiseres til kafka når gjennomføring opprettes") {
            val upsert = createUpsert()

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
            val upsert = createUpsert(navn = "Enkeltplass AMO")
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
            val soktInn = createRequest()

            service.soktInn(soktInn, NavIdent("B123456")).shouldBeRight()

            val (_, okonomi) = service.settOkonomiGodkjent(soktInn.id, besluttetAv).shouldBeRight()

            okonomi.shouldNotBeNull().should {
                it.status shouldBe TotrinnskontrollStatus.GODKJENT
                it.besluttetAv shouldBe besluttetAv
            }
        }

        test("returnerer feil når behandletAv og besluttetAv er samme person") {
            val soktInn = createRequest()
            service.soktInn(soktInn, opprettetAv).shouldBeRight()

            service.settOkonomiGodkjent(soktInn.id, opprettetAv)
                .shouldBeLeft()
                .first().detail shouldBe "Du kan ikke beslutte noe du selv har behandlet"
        }

        test("kan sette økonomi på vent") {
            val soktInn = createRequest()
            service.soktInn(soktInn, opprettetAv).shouldBeRight()

            val (_, okonomi) = service.settOkonomiPaVent(soktInn.id, besluttetAv, forklaring = "Feil").shouldBeRight()

            okonomi.shouldNotBeNull().should {
                it.status shouldBe TotrinnskontrollStatus.SATT_PA_VENT
                it.besluttetAv shouldBe besluttetAv
                it.forklaring shouldBe "Feil"
            }
        }

        test("kan godkjenne enkeltplass når den er satt på vent") {
            val soktInn = createRequest()
            service.soktInn(soktInn, opprettetAv).shouldBeRight()

            service.settOkonomiPaVent(
                soktInn.id,
                besluttetAv,
                forklaring = "Feil prisbetingelser",
            ).shouldBeRight()

            val (_, okonomi) = service.settOkonomiGodkjent(soktInn.id, besluttetAv).shouldBeRight()

            okonomi.shouldNotBeNull().should {
                it.status shouldBe TotrinnskontrollStatus.GODKJENT
                it.forklaring shouldBe null
            }
        }

        test("returnerer feil når enkeltplass allerede er behandlet") {
            val soktInn = createRequest()
            service.soktInn(soktInn, opprettetAv).shouldBeRight()

            service.settOkonomiGodkjent(soktInn.id, besluttetAv).shouldBeRight()

            service.settOkonomiGodkjent(soktInn.id, besluttetAv)
                .shouldBeLeft()
                .first().detail shouldBe "Totrinnskontrollen er allerede godkjent"

            service.settOkonomiPaVent(soktInn.id, besluttetAv, forklaring = "Angret")
                .shouldBeLeft()
                .first().detail shouldBe "Totrinnskontrollen er allerede godkjent"
        }
    }

    context("updateArenaData") {
        val service = createService()

        test("skriver til endringshistorikk når arena-data oppdateres") {
            val gjennomforingId = GjennomforingFixtures.EnkelAmo.id
            val arenadata = Gjennomforing.ArenaData(
                tiltaksnummer = Tiltaksnummer("2025/1"),
                ansvarligNavEnhet = "0400",
            )

            service.updateArenaData(gjennomforingId, arenadata)

            database.run {
                queries.endringshistorikk.getEndringshistorikk(EndringshistorikkType.GJENNOMFORING, gjennomforingId)
                    .entries.shouldHaveSize(1).first().should {
                        it.operation shouldBe "Oppdatert med tiltaksnummer fra Arena"
                    }
            }
        }

        test("skriver ikke til endringshistorikk når arena-data ikke er endret") {
            val gjennomforingId = GjennomforingFixtures.EnkelAmo.id
            val arenadata = Gjennomforing.ArenaData(
                tiltaksnummer = Tiltaksnummer("2025/1"),
                ansvarligNavEnhet = "0400",
            )

            service.updateArenaData(gjennomforingId, arenadata)
            service.updateArenaData(gjennomforingId, arenadata)

            database.run {
                queries.endringshistorikk.getEndringshistorikk(EndringshistorikkType.GJENNOMFORING, gjennomforingId)
                    .entries.shouldHaveSize(1)
            }
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
