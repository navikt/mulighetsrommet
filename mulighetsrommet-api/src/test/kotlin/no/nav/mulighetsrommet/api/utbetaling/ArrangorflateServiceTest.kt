package no.nav.mulighetsrommet.api.utbetaling

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import kotlinx.coroutines.runBlocking
import no.nav.amt.model.Melding
import no.nav.mulighetsrommet.api.arrangorflate.ArrangorFlateService
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrFlateUtbetalingStatus
import no.nav.mulighetsrommet.api.arrangorflate.api.Beregning
import no.nav.mulighetsrommet.api.clients.pdl.mockPdlClient
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerForslag
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningForhandsgodkjent
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Periode
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.time.LocalDate
import java.util.*

class ArrangorflateServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))
    val oauth = MockOAuth2Server()

    // Create test data using utility methods
    val deltaker = ArrangorflateTestUtils.createTestDeltaker()
    val tilsagn = ArrangorflateTestUtils.createTestTilsagn()
    val utbetaling = ArrangorflateTestUtils.createTestUtbetalingForhandsgodkjent(deltaker.id)
    val friUtbetaling = ArrangorflateTestUtils.createTestUtbetalingFri()

    // Set up domain with test data
    val domain = ArrangorflateTestUtils.createTestDomain(
        deltaker = deltaker,
        tilsagn = tilsagn,
        utbetalinger = listOf(utbetaling, friUtbetaling),
    )

    // PDL client and service will be initialized in beforeEach
    lateinit var pdlClient: no.nav.mulighetsrommet.api.clients.pdl.PdlClient
    lateinit var query: HentAdressebeskyttetPersonBolkPdlQuery
    lateinit var arrangorflateService: ArrangorFlateService

    beforeSpec {
        oauth.start()
    }

    beforeEach {
        domain.initialize(database.db)
        pdlClient = mockPdlClient(ArrangorflateTestUtils.createPdlMockEngine())
        query = HentAdressebeskyttetPersonBolkPdlQuery(pdlClient)
        arrangorflateService = ArrangorFlateService(query, database.db)
    }

    afterEach {
        database.truncateAll()
    }

    afterSpec {
        oauth.shutdown()
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
        result.beregning should beInstanceOf<UtbetalingBeregningForhandsgodkjent>()
        (result.beregning as UtbetalingBeregningForhandsgodkjent).output.belop shouldBe 10000
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
        val utbetalingDto = database.db.session {
            queries.utbetaling.get(utbetaling.id)
        }
        utbetalingDto.shouldNotBeNull()

        val result = arrangorflateService.getRelevanteForslag(utbetalingDto)

        result shouldHaveSize 0
    }

    test("toArrFlateUtbetaling should convert a forhandsgodkjent utbetaling") {
        runBlocking {
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

            val utbetalingDto = database.db.session {
                queries.utbetaling.get(utbetaling.id)
            }
            utbetalingDto.shouldNotBeNull()

            val result = arrangorflateService.toArrFlateUtbetaling(utbetalingDto)

            result.id shouldBe utbetaling.id
            result.status shouldBe ArrFlateUtbetalingStatus.VENTER_PA_ENDRING
            result.beregning should beInstanceOf<Beregning.Forhandsgodkjent>()
            val beregning = result.beregning as Beregning.Forhandsgodkjent
            beregning.antallManedsverk shouldBe 1.0
            beregning.belop shouldBe 10000
            beregning.deltakelser shouldHaveSize 1
        }
    }

    test("toArrFlateUtbetaling should convert a fri utbetaling") {
        runBlocking {
            val utbetalingDto = database.db.session {
                queries.utbetaling.get(friUtbetaling.id)
            }
            utbetalingDto.shouldNotBeNull()

            val result = arrangorflateService.toArrFlateUtbetaling(utbetalingDto)

            result.id shouldBe friUtbetaling.id
            result.status shouldBe ArrFlateUtbetalingStatus.KLAR_FOR_GODKJENNING
            result.beregning should beInstanceOf<Beregning.Fri>()
            val beregning = result.beregning as Beregning.Fri
            beregning.belop shouldBe 5000
        }
    }
})
