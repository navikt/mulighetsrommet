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
import no.nav.mulighetsrommet.admin.endringshistorikk.EndringshistorikkType
import no.nav.mulighetsrommet.admin.opplaring.OpplaringKategoriseringDetaljer
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeService
import no.nav.mulighetsrommet.api.amo.OpplaringKategoriseringRequest
import no.nav.mulighetsrommet.api.domain.deltaker.Deltakelsesmengde
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.domain.opplaring.Sertifisering
import no.nav.mulighetsrommet.api.domain.tiltak.Prismodell
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeFeature
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollStatus
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
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
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.FieldError
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
        utdanningsprogram = listOf(UtdanningFixtures.Utdanningsprogrammer.byggOgAnlegg),
    )

    beforeEach {
        domain.initialize(database.api)
    }

    afterEach {
        database.truncateAll()
    }

    fun createService(
        features: Map<Tiltakskode, Set<TiltakstypeFeature>> = mapOf(),
    ): GjennomforingEnkeltplassService {
        return GjennomforingEnkeltplassService(
            db = database.api,
            personaliaService = mockk(),
            tiltakstyper = TiltakstypeService(TiltakstypeService.Config(features), database.admin),
        )
    }

    val norskIdent = NorskIdent("12345678910")

    val migrert = mapOf(Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to setOf(TiltakstypeFeature.MIGRERT))

    val opprettetAv = NavAnsattFixture.DonaldDuck.navIdent
    val besluttetAv = NavAnsattFixture.MikkeMus.navIdent

    fun createRequest(
        kategorisering: OpplaringKategoriseringRequest? = null,
    ) = UpsertEnkeltplass(
        id = UUID.randomUUID(),
        tiltakskode = Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        arrangorId = GjennomforingFixtures.EnkelAmo.arrangorId,
        ansvarligEnhet = GjennomforingFixtures.EnkelAmo.ansvarligEnhet!!,
        prismodell = UpsertEnkeltplass.Prismodell.Anskaffelse(1000),
        kategorisering = kategorisering,
    )

    fun behandling(opprettetAv: NavIdent): TotrinnskontrollBehandling {
        return TotrinnskontrollBehandling(UUID.randomUUID(), opprettetAv)
    }

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

            val oppdatertPris = UpsertEnkeltplass.Prismodell.Anskaffelse(999)
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
                queries.opplaering.get(utkast.id).shouldBe(
                    OpplaringKategoriseringDetaljer(
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
            val utkast = createRequest(request).copy(
                tiltakskode = Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
            )

            service.opprettUtkast(utkast, opprettetAv).shouldBeRight()

            database.run {
                queries.opplaering.get(utkast.id).shouldBe(
                    OpplaringKategoriseringDetaljer(
                        kurstype = KurstypeFixtures.fov,
                        norskprove = false,
                    ),
                )
            }
        }

        test("lagrer kategorisering fag og yrke") {
            val request = OpplaringKategoriseringRequest(
                utdanningsprogramId = UtdanningFixtures.Utdanningsprogrammer.byggOgAnlegg.id,
                larefag = listOf(UtdanningFixtures.Utdanninger.fjellOgBergverksfaget.id),
            )
            val utkast = createRequest(request)

            service.opprettUtkast(utkast, opprettetAv).shouldBeRight()

            database.run {
                val utdanningslop = queries.opplaering.get(utkast.id)?.utdanningslop.shouldNotBeNull()
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

            val (_, okonomi) = service.soktInn(soktInn, behandling(opprettetAv)).shouldBeRight()

            okonomi.shouldNotBeNull().should {
                it.status shouldBe TotrinnskontrollStatus.TIL_BEHANDLING
                it.behandletAv shouldBe opprettetAv
                it.besluttetAv.shouldBeNull()
            }
        }

        test("beholder referanse til samme prismodell ved oppdatering av økonomi") {
            val soktInn = createRequest()

            val enkeltplass1 = service.soktInn(soktInn, behandling(opprettetAv)).shouldBeRight()

            enkeltplass1.gjennomforing.prismodell.shouldBeTypeOf<Prismodell.AnnenAvtaltPris>()

            val prismodell = UpsertEnkeltplass.Prismodell.TilskuddTilOpplaering(
                tilskudd = mapOf(Opplaeringtilskudd.Kode.SKOLEPENGER to 100),
                tilleggsopplysninger = null,
            )
            val enkeltplass2 = service
                .soktInn(soktInn.copy(prismodell = prismodell), behandling(opprettetAv))
                .shouldBeRight()

            enkeltplass2.gjennomforing.prismodell.shouldBeTypeOf<Prismodell.TilskuddTilOpplaering>().should {
                it.id shouldBe enkeltplass1.gjennomforing.prismodell.id
            }
        }

        test("sender økonomi til godkjenning på nytt etter at økonomi er satt på vent") {
            val soktInn = createRequest()
            service.soktInn(soktInn, behandling(opprettetAv)).shouldBeRight()
            service.settOkonomiPaVent(soktInn.id, besluttetAv, forklaring = "Feil prisbetingelser").shouldBeRight()

            val prismodell = UpsertEnkeltplass.Prismodell.TilskuddTilOpplaering(
                tilskudd = mapOf(Opplaeringtilskudd.Kode.SKOLEPENGER to 100),
                tilleggsopplysninger = null,
            )
            val (gjennomforing, okonomi) = service
                .soktInn(soktInn.copy(prismodell = prismodell), behandling(opprettetAv))
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
            service.soktInn(soktInn, behandling(opprettetAv)).shouldBeRight()
            service.settOkonomiGodkjent(soktInn.id, besluttetAv).shouldBeRight()

            val prismodell = UpsertEnkeltplass.Prismodell.TilskuddTilOpplaering(
                tilskudd = mapOf(Opplaeringtilskudd.Kode.SKOLEPENGER to 100),
                tilleggsopplysninger = null,
            )
            val (gjennomforing, okonomi) = service
                .soktInn(soktInn.copy(prismodell = prismodell), behandling(opprettetAv))
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

            service.soktInn(soktInn, behandling(opprettetAv)).shouldBeRight()

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

            service.soktInn(soktInn, behandling(opprettetAv)).shouldBeRight()
                .should { (gjennomforing) ->
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

            service.soktInn(soktInn, behandling(opprettetAv)).shouldBeRight()
                .should { (gjennomforing) ->
                    gjennomforing.startDato shouldBe startDato
                    gjennomforing.sluttDato shouldBe sluttDato
                    gjennomforing.deltidsprosent shouldBe 60.0
                }
        }
    }

    context("synkroniserFraArena") {
        val service = createService()

        fun createUpsert(
            id: UUID = UUID.randomUUID(),
            navn: String? = null,
        ) = UpsertArenaEnkeltplass(
            id = id,
            tiltakskode = Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
            arrangorId = GjennomforingFixtures.EnkelAmo.arrangorId,
            startDato = LocalDate.of(2025, 1, 1),
            sluttDato = LocalDate.of(2025, 6, 1),
            status = GjennomforingStatusType.GJENNOMFORES,
            ansvarligEnhet = GjennomforingFixtures.EnkelAmo.ansvarligEnhet!!,
            prismodell = UpsertEnkeltplass.Prismodell.Anskaffelse(1000),
            navn = navn,
        )

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

            service.soktInn(soktInn, behandling(opprettetAv)).shouldBeRight()

            val (_, okonomi) = service.settOkonomiGodkjent(soktInn.id, besluttetAv).shouldBeRight()

            okonomi.shouldNotBeNull().should {
                it.status shouldBe TotrinnskontrollStatus.GODKJENT
                it.besluttetAv shouldBe besluttetAv
            }
        }

        test("returnerer feil når behandletAv og besluttetAv er samme person") {
            val soktInn = createRequest()
            service.soktInn(soktInn, behandling(opprettetAv)).shouldBeRight()

            service.settOkonomiGodkjent(soktInn.id, opprettetAv)
                .shouldBeLeft()
                .first().detail shouldBe "Du kan ikke beslutte noe du selv har behandlet"
        }

        test("kan sette økonomi på vent") {
            val soktInn = createRequest()
            service.soktInn(soktInn, behandling(opprettetAv)).shouldBeRight()

            val (_, okonomi) = service.settOkonomiPaVent(soktInn.id, besluttetAv, forklaring = "Feil").shouldBeRight()

            okonomi.shouldNotBeNull().should {
                it.status shouldBe TotrinnskontrollStatus.SATT_PA_VENT
                it.besluttetAv shouldBe besluttetAv
                it.forklaring shouldBe "Feil"
            }
        }

        test("kan godkjenne enkeltplass når den er satt på vent") {
            val soktInn = createRequest()
            service.soktInn(soktInn, behandling(opprettetAv)).shouldBeRight()

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
            service.soktInn(soktInn, behandling(opprettetAv)).shouldBeRight()

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
                val eksisterendeDeltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                )
                MulighetsrommetTestDomain(
                    deltakere = listOf(eksisterendeDeltaker),
                ).initialize(database.api)

                val deltaker = DeltakerFixtures.createDeltaker(
                    id = eksisterendeDeltaker.id,
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                )

                service.updateFromDeltaker(deltaker, norskIdent)
            }

            test("kaster exception når gjennomføringen allerede har en annen deltaker") {
                val annenDeltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                )
                MulighetsrommetTestDomain(
                    deltakere = listOf(annenDeltaker),
                ).initialize(database.api)

                val nyDeltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                )

                shouldThrow<IllegalStateException> {
                    service.updateFromDeltaker(nyDeltaker, norskIdent)
                }.message shouldBe "Enkeltplass med id=${GjennomforingFixtures.EnkelAmo.id} har allerede en annen deltaker"
            }

            test("ignorerer annen deltaker som er FEILREGISTRERT uten å kaste exception") {
                val eksisterendeDeltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                )
                MulighetsrommetTestDomain(
                    deltakere = listOf(eksisterendeDeltaker),
                ).initialize(database.api)

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

        context("endrePrisinformasjon") {
            val service = createService()

            test("returnerer gammel okonomi og oppretter ny TIL_BEHANDLING når gammel økonomi er TIL_BEHANDLING") {
                val soktInn = createRequest()
                val forsteBehandling = behandling(opprettetAv)
                service.soktInn(soktInn, forsteBehandling).shouldBeRight().should { enkeltplass ->
                    enkeltplass.gjennomforing.prismodell.shouldBeTypeOf<Prismodell.AnnenAvtaltPris>().totalbelop shouldBe 1000
                    enkeltplass.okonomi.shouldNotBeNull().should {
                        it.id shouldBe forsteBehandling.id
                        it.status shouldBe TotrinnskontrollStatus.TIL_BEHANDLING
                        it.behandletAv shouldBe opprettetAv
                    }
                }

                database.run {
                    queries.kafkaProducerRecord.getRecords(10, listOf(TEST_GJENNOMFORING_V2_TOPIC)).shouldHaveSize(1)
                }

                val andreBehandling = behandling(opprettetAv)
                service.endrePrisinformasjon(
                    soktInn.id,
                    UpsertEnkeltplass.Prismodell.Anskaffelse(5000),
                    andreBehandling,
                ).shouldBeRight().should { enkeltplass ->
                    enkeltplass.gjennomforing.prismodell.shouldBeTypeOf<Prismodell.AnnenAvtaltPris>().totalbelop shouldBe 5000
                    enkeltplass.okonomi.shouldNotBeNull().should {
                        it.id shouldBe andreBehandling.id
                        it.status shouldBe TotrinnskontrollStatus.TIL_BEHANDLING
                        it.behandletAv shouldBe opprettetAv
                    }
                }

                database.run {
                    queries.totrinnskontroll.getById(forsteBehandling.id).status shouldBe TotrinnskontrollStatus.RETURNERT
                    queries.enkeltplassPrisendring.getByGjennomforingId(soktInn.id).shouldBeNull()
                    queries.kafkaProducerRecord.getRecords(10, listOf(TEST_GJENNOMFORING_V2_TOPIC)).shouldHaveSize(2)
                }
            }

            test("returnerer gammel okonomi og oppretter ny TIL_BEHANDLING når gammel økonomi er SATT_PA_VENT") {
                val soktInn = createRequest()

                val forsteBehandling = behandling(opprettetAv)
                service.soktInn(soktInn, forsteBehandling).shouldBeRight()

                service.settOkonomiPaVent(
                    soktInn.id,
                    besluttetAv,
                    "Trenger mer info",
                ).shouldBeRight().should { enkeltplass ->
                    enkeltplass.okonomi.shouldNotBeNull().should {
                        it.id shouldBe forsteBehandling.id
                        it.status shouldBe TotrinnskontrollStatus.SATT_PA_VENT
                    }
                }

                val andreBehandling = behandling(opprettetAv)
                service.endrePrisinformasjon(
                    soktInn.id,
                    UpsertEnkeltplass.Prismodell.Anskaffelse(5000),
                    andreBehandling,
                ).shouldBeRight().should { enkeltplass ->
                    enkeltplass.gjennomforing.prismodell.shouldBeTypeOf<Prismodell.AnnenAvtaltPris>().totalbelop shouldBe 5000
                    enkeltplass.okonomi.shouldNotBeNull().should {
                        it.id shouldBe andreBehandling.id
                        it.status shouldBe TotrinnskontrollStatus.TIL_BEHANDLING
                        it.behandletAv shouldBe opprettetAv
                    }
                }

                database.run {
                    queries.totrinnskontroll.getById(forsteBehandling.id).status shouldBe TotrinnskontrollStatus.RETURNERT
                    queries.enkeltplassPrisendring.getByGjennomforingId(soktInn.id).shouldBeNull()
                }
            }

            test("returnerer ny okonomi TIL_BEHANDLING ved påfølgende endrePrisinformasjon") {
                val soktInn = createRequest()
                service.soktInn(soktInn, behandling(opprettetAv)).shouldBeRight()

                val andreBehandling = behandling(opprettetAv)
                service.endrePrisinformasjon(
                    soktInn.id,
                    UpsertEnkeltplass.Prismodell.Anskaffelse(5000),
                    andreBehandling,
                ).shouldBeRight()

                val tredjeBehandling = behandling(opprettetAv)
                service.endrePrisinformasjon(
                    soktInn.id,
                    UpsertEnkeltplass.Prismodell.Anskaffelse(6000),
                    tredjeBehandling,
                ).shouldBeRight().should { enkeltplass ->
                    enkeltplass.gjennomforing.prismodell.shouldBeTypeOf<Prismodell.AnnenAvtaltPris>().totalbelop shouldBe 6000
                    enkeltplass.okonomi.shouldNotBeNull().should {
                        it.id shouldBe tredjeBehandling.id
                        it.status shouldBe TotrinnskontrollStatus.TIL_BEHANDLING
                    }
                }

                database.run {
                    queries.totrinnskontroll.getById(andreBehandling.id).status shouldBe TotrinnskontrollStatus.RETURNERT
                }
            }

            test("er idempotent og ignorerer kall med samme behandling.id") {
                val soktInn = createRequest()
                service.soktInn(soktInn, behandling(opprettetAv)).shouldBeRight()

                val behandling = behandling(opprettetAv)
                service.endrePrisinformasjon(
                    soktInn.id,
                    UpsertEnkeltplass.Prismodell.Anskaffelse(5000),
                    behandling,
                ).shouldBeRight().should { enkeltplass ->
                    enkeltplass.gjennomforing.prismodell.shouldBeTypeOf<Prismodell.AnnenAvtaltPris>().totalbelop shouldBe 5000
                }

                service.endrePrisinformasjon(
                    soktInn.id,
                    UpsertEnkeltplass.Prismodell.Anskaffelse(9999),
                    behandling,
                ).shouldBeRight().should { enkeltplass ->
                    enkeltplass.gjennomforing.prismodell.shouldBeTypeOf<Prismodell.AnnenAvtaltPris>().totalbelop shouldBe 5000
                }
            }

            test("oppretter prisendring når økonomi er GODKJENT") {
                val soktInn = createRequest()
                service.soktInn(soktInn, behandling(opprettetAv)).shouldBeRight()
                service.settOkonomiGodkjent(soktInn.id, besluttetAv).shouldBeRight()

                service.endrePrisinformasjon(
                    soktInn.id,
                    UpsertEnkeltplass.Prismodell.Anskaffelse(3000),
                    behandling(opprettetAv),
                ).shouldBeRight()

                service.get(soktInn.id).shouldNotBeNull().should { (gjennomforing, _) ->
                    gjennomforing.prismodell.shouldBeTypeOf<Prismodell.AnnenAvtaltPris>().totalbelop shouldBe 1000
                }

                database.run {
                    queries.enkeltplassPrisendring.getByGjennomforingId(soktInn.id).shouldNotBeNull()
                }
            }

            test("avviser eksisterende prisendring ved ny prisendring mens økonomi er GODKJENT") {
                val soktInn = createRequest()
                service.soktInn(soktInn, behandling(opprettetAv)).shouldBeRight()
                service.settOkonomiGodkjent(soktInn.id, besluttetAv).shouldBeRight()

                val forsteBehandling = behandling(opprettetAv)
                service.endrePrisinformasjon(
                    soktInn.id,
                    UpsertEnkeltplass.Prismodell.Anskaffelse(3000),
                    forsteBehandling,
                ).shouldBeRight()

                val andreBehandling = behandling(opprettetAv)
                service.endrePrisinformasjon(
                    soktInn.id,
                    UpsertEnkeltplass.Prismodell.Anskaffelse(4000),
                    andreBehandling,
                ).shouldBeRight()

                database.run {
                    val pending = queries.enkeltplassPrisendring.getByGjennomforingId(soktInn.id).shouldNotBeNull()
                    pending.totrinnskontrollId shouldBe andreBehandling.id
                }
            }

            test("returnerer feil økonomi er RETURNERT") {
                val soktInn = createRequest()
                val enkeltplass = service.soktInn(soktInn, behandling(opprettetAv)).shouldBeRight()

                // Simuler ugyldig tilstand - tjenesten tillater foreløpig ikke å sette status RETURNERT (kun SATT_PA_VENT)
                database.run {
                    enkeltplass.okonomi!!.returner(besluttetAv).onRight { queries.totrinnskontroll.upsert(it) }
                }

                service.endrePrisinformasjon(
                    soktInn.id,
                    UpsertEnkeltplass.Prismodell.Anskaffelse(5000),
                    behandling(opprettetAv),
                ) shouldBeLeft listOf(FieldError.of("Kan ikke endre prismodell på en enkeltplass med returnert økonomi"))
            }

            test("kaster feil dersom enkeltplass ikke er søkt inn") {
                val utkast = createRequest()
                service.opprettUtkast(utkast, opprettetAv).shouldBeRight()

                shouldThrow<IllegalStateException> {
                    service.endrePrisinformasjon(
                        utkast.id,
                        UpsertEnkeltplass.Prismodell.Anskaffelse(5000),
                        behandling(opprettetAv),
                    )
                }
            }

            test("oppretter prisendring med status TIL_BEHANDLING når økonomi er GODKJENT") {
                val soktInn = createRequest()
                service.soktInn(soktInn, behandling(opprettetAv)).shouldBeRight()
                service.settOkonomiGodkjent(soktInn.id, besluttetAv).shouldBeRight()

                val behandling = behandling(opprettetAv)
                service.endrePrisinformasjon(
                    soktInn.id,
                    UpsertEnkeltplass.Prismodell.Anskaffelse(3000),
                    behandling,
                ).shouldBeRight()

                database.run {
                    queries.totrinnskontroll.getById(behandling.id).should {
                        it.entityId shouldBe soktInn.id
                        it.type shouldBe TotrinnskontrollType.ENKELTPLASS_PRISENDRING
                        it.status shouldBe TotrinnskontrollStatus.TIL_BEHANDLING
                        it.behandletAv shouldBe opprettetAv
                    }
                }
            }

            test("setter eksisterende prisendring til RETURNERT ved ny prisendring når økonomi er GODKJENT") {
                val soktInn = createRequest()
                service.soktInn(soktInn, behandling(opprettetAv)).shouldBeRight()
                service.settOkonomiGodkjent(soktInn.id, besluttetAv).shouldBeRight()

                val forsteBehandling = behandling(opprettetAv)
                service.endrePrisinformasjon(
                    soktInn.id,
                    UpsertEnkeltplass.Prismodell.Anskaffelse(3000),
                    forsteBehandling,
                ).shouldBeRight()

                val andreBehandling = behandling(opprettetAv)
                service.endrePrisinformasjon(
                    soktInn.id,
                    UpsertEnkeltplass.Prismodell.Anskaffelse(4000),
                    andreBehandling,
                ).shouldBeRight()

                database.run {
                    queries.totrinnskontroll.getById(forsteBehandling.id).should {
                        it.status shouldBe TotrinnskontrollStatus.RETURNERT
                    }
                    queries.totrinnskontroll.getById(andreBehandling.id).should {
                        it.status shouldBe TotrinnskontrollStatus.TIL_BEHANDLING
                    }
                }
            }

            test("setter eksisterende prisendring som er SATT_PA_VENT til RETURNERT ved ny prisendring når økonomi er GODKJENT") {
                val soktInn = createRequest()
                service.soktInn(soktInn, behandling(opprettetAv)).shouldBeRight()
                service.settOkonomiGodkjent(soktInn.id, besluttetAv).shouldBeRight()

                val forsteBehandling = behandling(opprettetAv)
                service.endrePrisinformasjon(
                    soktInn.id,
                    UpsertEnkeltplass.Prismodell.Anskaffelse(3000),
                    forsteBehandling,
                ).shouldBeRight()

                service.settOkonomiPaVent(soktInn.id, besluttetAv, forklaring = "Trenger mer info").shouldBeRight()

                val andreBehandling = behandling(opprettetAv)
                service.endrePrisinformasjon(
                    soktInn.id,
                    UpsertEnkeltplass.Prismodell.Anskaffelse(4000),
                    andreBehandling,
                ).shouldBeRight()

                database.run {
                    queries.totrinnskontroll.getById(forsteBehandling.id).should {
                        it.status shouldBe TotrinnskontrollStatus.RETURNERT
                    }
                    queries.totrinnskontroll.getById(andreBehandling.id).should {
                        it.status shouldBe TotrinnskontrollStatus.TIL_BEHANDLING
                    }
                }
            }
        }

        context("behandling av prisendring for enkeltplasser") {
            val service = createService()

            test("settOkonomiGodkjent godkjenner prisendring og oppdaterer prismodell") {
                val soktInn = createRequest()
                service.soktInn(soktInn, behandling(opprettetAv)).shouldBeRight()
                service.settOkonomiGodkjent(soktInn.id, besluttetAv).shouldBeRight()

                service.endrePrisinformasjon(
                    soktInn.id,
                    UpsertEnkeltplass.Prismodell.Anskaffelse(5000),
                    behandling(opprettetAv),
                ).shouldBeRight()

                database.run {
                    queries.kafkaProducerRecord.getRecords(100, listOf(TEST_GJENNOMFORING_V2_TOPIC)).shouldHaveSize(1)
                }

                service.settOkonomiGodkjent(soktInn.id, besluttetAv).shouldBeRight()

                service.get(soktInn.id).shouldNotBeNull().should { (gjennomforing, _) ->
                    gjennomforing.prismodell.shouldBeTypeOf<Prismodell.AnnenAvtaltPris>().totalbelop shouldBe 5000
                }

                database.run {
                    queries.enkeltplassPrisendring.getByGjennomforingId(soktInn.id).shouldBeNull()

                    queries.kafkaProducerRecord.getRecords(100, listOf(TEST_GJENNOMFORING_V2_TOPIC)).shouldHaveSize(2)
                }
            }

            test("settOkonomiGodkjent setter prisendring-totrinnskontroll til GODKJENT") {
                val soktInn = createRequest()
                service.soktInn(soktInn, behandling(opprettetAv)).shouldBeRight()
                service.settOkonomiGodkjent(soktInn.id, besluttetAv).shouldBeRight()

                val behandling = behandling(opprettetAv)
                service.endrePrisinformasjon(
                    soktInn.id,
                    UpsertEnkeltplass.Prismodell.Anskaffelse(5000),
                    behandling,
                ).shouldBeRight()

                service.settOkonomiGodkjent(soktInn.id, besluttetAv).shouldBeRight()

                database.run {
                    queries.totrinnskontroll.getById(behandling.id).should {
                        it.status shouldBe TotrinnskontrollStatus.GODKJENT
                        it.besluttetAv shouldBe besluttetAv
                    }
                }
            }

            test("settOkonomiGodkjent godkjenner prisendring som er satt på vent") {
                val soktInn = createRequest()
                service.soktInn(soktInn, behandling(opprettetAv)).shouldBeRight()
                service.settOkonomiGodkjent(soktInn.id, besluttetAv).shouldBeRight()

                service.endrePrisinformasjon(
                    soktInn.id,
                    UpsertEnkeltplass.Prismodell.Anskaffelse(5000),
                    behandling(opprettetAv),
                ).shouldBeRight()

                service.settOkonomiPaVent(soktInn.id, besluttetAv, forklaring = "Trenger mer info").shouldBeRight()

                service.settOkonomiGodkjent(soktInn.id, besluttetAv).shouldBeRight()

                service.get(soktInn.id).shouldNotBeNull().should { (gjennomforing, _) ->
                    gjennomforing.prismodell.shouldBeTypeOf<Prismodell.AnnenAvtaltPris>().totalbelop shouldBe 5000
                }

                database.run {
                    queries.enkeltplassPrisendring.getByGjennomforingId(soktInn.id).shouldBeNull()
                }
            }

            test("settOkonomiPaVent setter prisendring på vent") {
                val soktInn = createRequest()
                service.soktInn(soktInn, behandling(opprettetAv)).shouldBeRight()
                service.settOkonomiGodkjent(soktInn.id, besluttetAv).shouldBeRight()

                val behandling = behandling(opprettetAv)
                service.endrePrisinformasjon(
                    soktInn.id,
                    UpsertEnkeltplass.Prismodell.Anskaffelse(5000),
                    behandling,
                ).shouldBeRight()

                service.settOkonomiPaVent(soktInn.id, besluttetAv, forklaring = "Trenger mer info").shouldBeRight()

                database.run {
                    queries.totrinnskontroll.getById(behandling.id).should {
                        it.status shouldBe TotrinnskontrollStatus.SATT_PA_VENT
                    }
                }
            }

            test("settOkonomiGodkjent returnerer feil dersom ingen okonomi finnes") {
                val utkast = createRequest()
                service.opprettUtkast(utkast, opprettetAv).shouldBeRight()

                service.settOkonomiGodkjent(utkast.id, besluttetAv)
                    .shouldBeLeft()
                    .first().detail shouldBe "Økonomi har ikke blitt sendt til godkjenning"
            }

            test("settOkonomiPaVent returnerer feil dersom ingen okonomi finnes") {
                val utkast = createRequest()
                service.opprettUtkast(utkast, opprettetAv).shouldBeRight()

                service.settOkonomiPaVent(utkast.id, besluttetAv, forklaring = null)
                    .shouldBeLeft()
                    .first().detail shouldBe "Økonomi har ikke blitt sendt til godkjenning"
            }
        }

        context("relast av deltaker") {
            val tidligereEndretTidspunkt = LocalDateTime.of(2025, 1, 1, 12, 0, 0)
            val nyereEndretTidspunkt = LocalDateTime.of(2025, 6, 1, 12, 0, 0)

            test("prosesserer når deltaker-eventet er samme eller nyere enn lagret") {
                val lagretDeltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    endretTidspunkt = tidligereEndretTidspunkt,
                )
                MulighetsrommetTestDomain(
                    deltakere = listOf(lagretDeltaker),
                ).initialize(database.api)

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
                val lagretDeltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    endretTidspunkt = nyereEndretTidspunkt,
                )
                MulighetsrommetTestDomain(
                    deltakere = listOf(lagretDeltaker),
                ).initialize(database.api)

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
