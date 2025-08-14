package no.nav.mulighetsrommet.api.utbetaling

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import no.nav.amt.model.Melding
import no.nav.mulighetsrommet.api.arrangorflate.ArrangorFlateService
import no.nav.mulighetsrommet.api.arrangorflate.DeltakerOgPeriode
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrFlateBeregning
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrFlateTilsagnFilter
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrFlateUtbetalingStatus
import no.nav.mulighetsrommet.api.arrangorflate.harFeilSluttDato
import no.nav.mulighetsrommet.api.arrangorflate.harOverlappendePeriode
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.api.clients.pdl.PdlClient
import no.nav.mulighetsrommet.api.clients.pdl.mockPdlClient
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixtures
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerForslag
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerManedsverkMedDeltakelsesmengder
import no.nav.mulighetsrommet.api.utbetaling.pdl.HentAdressebeskyttetPersonBolkPdlQuery
import no.nav.mulighetsrommet.api.utbetaling.pdl.HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ArrangorflateServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val deltaker = ArrangorflateTestUtils.createTestDeltaker()
    val tilsagn = ArrangorflateTestUtils.createTestTilsagn()
    val utbetaling = ArrangorflateTestUtils.createTestUtbetalingForhandsgodkjent(deltaker.id)
    val friUtbetaling = ArrangorflateTestUtils.createTestUtbetalingFri()
    val kontoregisterOrganisasjon = mockk<KontoregisterOrganisasjonClient>(relaxed = true)

    val domain = ArrangorflateTestUtils.createTestDomain(
        deltaker = deltaker,
        tilsagn = tilsagn,
        utbetalinger = listOf(utbetaling, friUtbetaling),
    )

    lateinit var pdlClient: PdlClient
    lateinit var personService: PersonService
    lateinit var arrangorflateService: ArrangorFlateService

    fun getUtbetalingDto(id: UUID): Utbetaling = database.db.session {
        return requireNotNull(queries.utbetaling.get(id))
    }

    fun verifyForhandsgodkjentBeregning(
        beregning: ArrFlateBeregning.PrisPerManedsverkMedDeltakelsesmengder,
        expectedBelop: Int,
        expectedManedsverk: Double,
        expectedDeltakelserCount: Int,
    ) {
        beregning.antallManedsverk shouldBe expectedManedsverk
        beregning.belop shouldBe expectedBelop
        beregning.deltakelser shouldHaveSize expectedDeltakelserCount
    }

    beforeEach {
        domain.initialize(database.db)
        pdlClient = mockPdlClient(ArrangorflateTestUtils.createPdlMockEngine())
        personService = PersonService(
            hentPersonOgGeografiskTilknytningQuery = HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery(pdlClient),
            hentPersonQuery = HentAdressebeskyttetPersonBolkPdlQuery(pdlClient),
            norg2Client = mockk(),
        )
        arrangorflateService = ArrangorFlateService(database.db, personService, kontoregisterOrganisasjon)
    }

    afterEach {
        database.truncateAll()
    }

    test("getUtbetalinger should return list of compact utbetalinger for arrangor") {
        val result = arrangorflateService.getUtbetalinger(ArrangorflateTestUtils.underenhet.organisasjonsnummer)

        (result.aktive.size + result.historiske.size) shouldBe 2
        result.aktive.any { it.id == utbetaling.id } shouldBe true
        result.aktive.any { it.id == friUtbetaling.id } shouldBe true

        val forsteUtbetaling = result.aktive.first { it.id == utbetaling.id }
        forsteUtbetaling.belop shouldBe 10000
        forsteUtbetaling.status shouldBe ArrFlateUtbetalingStatus.KLAR_FOR_GODKJENNING

        val andreUtbetaling = result.aktive.first { it.id == friUtbetaling.id }
        andreUtbetaling.belop shouldBe 5000
        andreUtbetaling.status shouldBe ArrFlateUtbetalingStatus.KLAR_FOR_GODKJENNING
    }

    test("getUtbetaling should return utbetaling by ID") {
        val result = arrangorflateService.getUtbetaling(utbetaling.id)

        result.shouldNotBeNull()
        result.id shouldBe utbetaling.id

        result.beregning.shouldBeInstanceOf<UtbetalingBeregningPrisPerManedsverkMedDeltakelsesmengder>().should {
            it.output.belop shouldBe 10000
        }
    }

    test("getTilsagn should return arrangorflateTilsagn by ID") {
        val result = arrangorflateService.getTilsagn(tilsagn.id)

        result.shouldNotBeNull()
        result.id shouldBe tilsagn.id
        result.status shouldBe TilsagnStatus.GODKJENT
    }

    test("getTilsagnByOrgnr should return list of tilsagn for arrangor") {
        val result = arrangorflateService.getTilsagn(
            filter = ArrFlateTilsagnFilter(),
            ArrangorflateTestUtils.underenhet.organisasjonsnummer,
        )

        result shouldHaveSize 1
        result[0].id shouldBe tilsagn.id
        result[0].status shouldBe TilsagnStatus.GODKJENT
    }

    test("getArrangorflateTilsagnTilUtbetaling should return tilsagn for given gjennomforing and period") {
        val u = arrangorflateService.getUtbetaling(utbetaling.id)!!
        val result = arrangorflateService.getArrangorflateTilsagnTilUtbetaling(
            u.copy(periode = Periode(LocalDate.of(2024, 7, 1), LocalDate.of(2024, 8, 1))),
        )

        result shouldHaveSize 1
        result[0].id shouldBe tilsagn.id
    }

    test("getAdvarsler should return empty list when no advarsler exists") {
        val utbetalingDto = getUtbetalingDto(utbetaling.id)
        val result = arrangorflateService.getAdvarsler(utbetalingDto)
        result shouldHaveSize 0
    }

    test("mapUtbetalingToArrFlateUtbetaling should have status VENTER_PA_ENDRING") {
        // Setup deltakerforslag
        database.db.session {
            queries.deltakerForslag.upsert(
                DeltakerForslag(
                    id = UUID.randomUUID(),
                    deltakerId = deltaker.id,
                    endring = Melding.Forslag.Endring.Deltakelsesmengde(
                        deltakelsesprosent = 80,
                        gyldigFra = LocalDate.of(2024, 8, 15),
                    ),
                    status = DeltakerForslag.Status.VENTER_PA_SVAR,
                ),
            )
        }
        val result = arrangorflateService.toArrFlateUtbetaling(arrangorflateService.getUtbetaling(utbetaling.id)!!)

        result.id shouldBe utbetaling.id
        result.status shouldBe ArrFlateUtbetalingStatus.KREVER_ENDRING

        result.beregning.shouldBeInstanceOf<ArrFlateBeregning.PrisPerManedsverkMedDeltakelsesmengder> {
            verifyForhandsgodkjentBeregning(it, 10000, 1.0, 1)
        }
    }

    test("mapUtbetalingToArrFlateUtbetaling should have status KLAR_FOR_GODKJENNING") {
        val result = arrangorflateService.toArrFlateUtbetaling(arrangorflateService.getUtbetaling(utbetaling.id)!!)
        result.id shouldBe utbetaling.id
        result.status shouldBe ArrFlateUtbetalingStatus.KLAR_FOR_GODKJENNING
    }

    test("mapUtbetalingToArrFlateUtbetaling should convert a fri utbetaling") {
        val result = arrangorflateService.toArrFlateUtbetaling(arrangorflateService.getUtbetaling(friUtbetaling.id)!!)

        result.id shouldBe friUtbetaling.id
        result.status shouldBe ArrFlateUtbetalingStatus.KLAR_FOR_GODKJENNING
        result.beregning.shouldBeInstanceOf<ArrFlateBeregning.Fri> {
            it.belop shouldBe 5000
        }
    }

    test("toArrFlateUtbetaling should map successfully with kanViseBeregning = true for recently approved utbetaling") {
        val date = LocalDateTime.now()
        val godkjentAvArrangorUtbetaling = getUtbetalingDto(utbetaling.id).copy(godkjentAvArrangorTidspunkt = date.minusDays(1))
        val result = arrangorflateService.toArrFlateUtbetaling(godkjentAvArrangorUtbetaling, relativeDate = date)

        result.shouldNotBeNull()
        result.status shouldBe ArrFlateUtbetalingStatus.KLAR_FOR_GODKJENNING
        result.beregning.shouldBeInstanceOf<ArrFlateBeregning.PrisPerManedsverkMedDeltakelsesmengder> {
            it.deltakelser shouldHaveSize 1
        }
        result.kanViseBeregning shouldBe true
    }

    test("toArrFlateUtbetaling should map successfully with kanViseBeregning = false for 12 weeks old approved utbetaling") {
        val now = LocalDateTime.now()
        val godkjentAvArrangorUtbetaling = getUtbetalingDto(utbetaling.id).copy(godkjentAvArrangorTidspunkt = now.minusWeeks(12))
        val result = arrangorflateService.toArrFlateUtbetaling(godkjentAvArrangorUtbetaling, relativeDate = now)

        result.shouldNotBeNull()
        result.status shouldBe ArrFlateUtbetalingStatus.KLAR_FOR_GODKJENNING
        result.beregning.shouldBeInstanceOf<ArrFlateBeregning.PrisPerManedsverkMedDeltakelsesmengder> {
            it.deltakelser shouldHaveSize 1
            it.deltakelser[0].person.shouldBeNull()
        }
        result.kanViseBeregning shouldBe false
    }

    context("advarsler") {
        test("overlappende periode") {
            val d = DeltakerOgPeriode(
                id = UUID.randomUUID(),
                norskIdent = NorskIdent("01010199999"),
                periode = Periode.forMonthOf(LocalDate.of(2025, 8, 1)),
            )

            // Samme id gir ikke overlappende
            harOverlappendePeriode(
                d,
                listOf(d),
            ) shouldBe false

            // Annen norsk ident gir ikke overlappende
            harOverlappendePeriode(
                d,
                listOf(DeltakerOgPeriode(UUID.randomUUID(), NorskIdent("02020288888"), d.periode)),
            ) shouldBe false

            // Samme periode gir true
            harOverlappendePeriode(
                d,
                listOf(DeltakerOgPeriode(UUID.randomUUID(), d.norskIdent, d.periode)),
            ) shouldBe true

            // Tre med overlappende periode gir true
            harOverlappendePeriode(
                d,
                listOf(
                    DeltakerOgPeriode(UUID.randomUUID(), d.norskIdent, Periode(LocalDate.of(2025, 8, 2), LocalDate.of(2025, 9, 10))),
                    DeltakerOgPeriode(UUID.randomUUID(), d.norskIdent, Periode(LocalDate.of(2025, 7, 2), LocalDate.of(2025, 8, 2))),
                ),
            ) shouldBe true

            // Neste dag gir false
            harOverlappendePeriode(
                DeltakerOgPeriode(
                    d.id,
                    d.norskIdent,
                    Periode(LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 3)),
                ),
                listOf(DeltakerOgPeriode(UUID.randomUUID(), d.norskIdent, Periode(LocalDate.of(2025, 8, 3), LocalDate.of(2025, 8, 5)))),
            ) shouldBe false
        }

        test("feil slutt dato") {
            val today = LocalDate.of(2025, 1, 1)
            forAll(
                row(DeltakerStatusType.AVBRUTT_UTKAST, false),
                row(DeltakerStatusType.DELTAR, false),
                row(DeltakerStatusType.FEILREGISTRERT, false),
                row(DeltakerStatusType.IKKE_AKTUELL, false),
                row(DeltakerStatusType.KLADD, false),
                row(DeltakerStatusType.PABEGYNT_REGISTRERING, false),
                row(DeltakerStatusType.SOKT_INN, false),
                row(DeltakerStatusType.UTKAST_TIL_PAMELDING, false),
                row(DeltakerStatusType.VENTELISTE, false),
                row(DeltakerStatusType.VENTER_PA_OPPSTART, false),
                row(DeltakerStatusType.VURDERES, false),

                row(DeltakerStatusType.AVBRUTT, true),
                row(DeltakerStatusType.FULLFORT, true),
                row(DeltakerStatusType.HAR_SLUTTET, true),
            ) { status, expectedResult ->
                harFeilSluttDato(status, today.plusDays(1), today = today) shouldBe expectedResult
            }

            // I dag gir false
            harFeilSluttDato(DeltakerStatusType.HAR_SLUTTET, today, today) shouldBe false
            // I går gir false
            harFeilSluttDato(DeltakerStatusType.HAR_SLUTTET, today.minusDays(1), today) shouldBe false
            // Om et år gir true
            harFeilSluttDato(DeltakerStatusType.AVBRUTT, today.plusYears(1), today) shouldBe true
        }

        test("getFeilSluttDato") {
            val today = LocalDate.of(2025, 1, 1)
            val deltaker1 = DeltakerFixtures.createDeltaker(
                gjennomforingId = UUID.randomUUID(),
                startDato = today.minusMonths(1),
                sluttDato = today.plusMonths(1),
                statusType = DeltakerStatusType.HAR_SLUTTET,
            )
            val deltaker2 = DeltakerFixtures.createDeltaker(
                gjennomforingId = UUID.randomUUID(),
                startDato = today.minusMonths(1),
                sluttDato = today.plusMonths(1),
                statusType = DeltakerStatusType.DELTAR,
            )
            val feilSluttDato = arrangorflateService.getFeilSluttDato(listOf(deltaker1, deltaker2), today)
            feilSluttDato shouldHaveSize 1
            feilSluttDato[0].deltakerId shouldBe deltaker1.id
        }

        test("getOverlappendePerioder tre overlappende en ikke") {
            val deltakerPeriode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5))
            val utbetalingPeriode = Periode.forMonthOf(LocalDate.of(2024, 1, 1))
            val deltaker1 = DeltakerFixtures.createDeltaker(
                startDato = deltakerPeriode.start,
                sluttDato = deltakerPeriode.slutt,
                statusType = DeltakerStatusType.DELTAR,
            )
            val deltaker2 = DeltakerFixtures.createDeltaker(
                startDato = deltakerPeriode.start,
                sluttDato = deltakerPeriode.slutt,
                statusType = DeltakerStatusType.DELTAR,
            )
            val deltaker3 = DeltakerFixtures.createDeltaker(
                startDato = null,
                sluttDato = deltakerPeriode.slutt,
                statusType = DeltakerStatusType.DELTAR,
            )
            val deltaker4 = DeltakerFixtures.createDeltaker(
                startDato = deltakerPeriode.slutt.plusDays(1),
                sluttDato = null,
                statusType = DeltakerStatusType.DELTAR,
            )
            val overlappendePerioder = arrangorflateService.getOverlappendePerioder(
                listOf(deltaker1, deltaker2, deltaker3, deltaker4),
                utbetalingPeriode,
            )
            overlappendePerioder.map { it.deltakerId } shouldContainExactlyInAnyOrder listOf(
                deltaker1.id,
                deltaker2.id,
                deltaker3.id,
            )
        }
    }
})
