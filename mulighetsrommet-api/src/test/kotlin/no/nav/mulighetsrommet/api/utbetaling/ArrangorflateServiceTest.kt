package no.nav.mulighetsrommet.api.utbetaling

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import no.nav.amt.model.Melding
import no.nav.mulighetsrommet.api.arrangorflate.ArrangorFlateService
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrFlateBeregning
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrFlateUtbetalingStatus
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.api.clients.pdl.PdlClient
import no.nav.mulighetsrommet.api.clients.pdl.mockPdlClient
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerForslag
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerManedsverkMedDeltakelsesmengder
import no.nav.mulighetsrommet.api.utbetaling.pdl.HentAdressebeskyttetPersonBolkPdlQuery
import no.nav.mulighetsrommet.api.utbetaling.pdl.HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
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

        result shouldHaveSize 2
        result.any { it.id == utbetaling.id } shouldBe true
        result.any { it.id == friUtbetaling.id } shouldBe true

        val forsteUtbetaling = result.first { it.id == utbetaling.id }
        forsteUtbetaling.belop shouldBe 10000
        forsteUtbetaling.status shouldBe ArrFlateUtbetalingStatus.KLAR_FOR_GODKJENNING

        val andreUtbetaling = result.first { it.id == friUtbetaling.id }
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
        result.status.status shouldBe TilsagnStatus.GODKJENT
    }

    test("getTilsagnByOrgnr should return list of tilsagn for arrangor") {
        val result = arrangorflateService.getTilsagnByOrgnr(ArrangorflateTestUtils.underenhet.organisasjonsnummer)

        result shouldHaveSize 1
        result[0].id shouldBe tilsagn.id
        result[0].status.status shouldBe TilsagnStatus.GODKJENT
    }

    test("getArrangorflateTilsagnTilUtbetaling should return tilsagn for given gjennomforing and period") {
        val periode = Periode(LocalDate.of(2024, 7, 1), LocalDate.of(2024, 8, 1))
        val result = arrangorflateService.getArrangorflateTilsagnTilUtbetaling(GjennomforingFixtures.AFT1.id, periode)

        result shouldHaveSize 1
        result[0].id shouldBe tilsagn.id
    }

    test("getRelevanteForslag should return empty list when no forslag exists") {
        val utbetalingDto = getUtbetalingDto(utbetaling.id)
        val result = arrangorflateService.getRelevanteForslag(utbetalingDto)
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
        result.status shouldBe ArrFlateUtbetalingStatus.VENTER_PA_ENDRING

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
})
