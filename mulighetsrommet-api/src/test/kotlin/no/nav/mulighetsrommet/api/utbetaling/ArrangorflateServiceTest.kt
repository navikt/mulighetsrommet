package no.nav.mulighetsrommet.api.utbetaling

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.ktor.client.engine.mock.*
import io.ktor.http.content.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import no.nav.amt.model.Melding
import no.nav.mulighetsrommet.altinn.AltinnClient
import no.nav.mulighetsrommet.altinn.AltinnClient.AuthorizedParty
import no.nav.mulighetsrommet.altinn.AltinnClient.AuthorizedPartyType
import no.nav.mulighetsrommet.api.arrangorflate.ArrangorFlateService
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrFlateUtbetalingStatus
import no.nav.mulighetsrommet.api.arrangorflate.model.Beregning
import no.nav.mulighetsrommet.api.clients.dokark.DokarkResponse
import no.nav.mulighetsrommet.api.clients.dokark.DokarkResponseDokument
import no.nav.mulighetsrommet.api.clients.pdl.mockPdlClient
import no.nav.mulighetsrommet.api.createAuthConfig
import no.nav.mulighetsrommet.api.createTestApplicationConfig
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerDbo
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerForslag
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelseManedsverk
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsePeriode
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsePerioder
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningForhandsgodkjent
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.ktor.MockEngineBuilder
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ArrangorflateServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val identMedTilgang = NorskIdent("01010199988")

    val hovedenhet = ArrangorFixtures.hovedenhet
    val underenhet = ArrangorFixtures.underenhet1

    val deltakerId = UUID.randomUUID()
    val deltaker = DeltakerDbo(
        id = deltakerId,
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        startDato = GjennomforingFixtures.AFT1.startDato,
        sluttDato = GjennomforingFixtures.AFT1.sluttDato,
        registrertTidspunkt = GjennomforingFixtures.AFT1.startDato.atStartOfDay(),
        endretTidspunkt = LocalDateTime.now(),
        deltakelsesprosent = 100.0,
        deltakelsesmengder = listOf(),
        status = DeltakerStatus(
            type = DeltakerStatus.Type.DELTAR,
            aarsak = null,
            opprettetDato = LocalDateTime.now(),
        ),
    )

    val tilsagnId = UUID.randomUUID()
    val tilsagn = TilsagnDbo(
        id = tilsagnId,
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        periode = Periode(LocalDate.of(2024, 6, 1), LocalDate.of(2025, 1, 1)),
        lopenummer = 1,
        bestillingsnummer = "A-2025/1-1",
        kostnadssted = NavEnhetFixtures.Innlandet.enhetsnummer,
        beregning = TilsagnBeregningFri(
            input = TilsagnBeregningFri.Input(1000),
            output = TilsagnBeregningFri.Output(1000),
        ),
        arrangorId = ArrangorFixtures.underenhet1.id,
        type = TilsagnType.TILSAGN,
    )

    val utbetalingId = UUID.randomUUID()
    val utbetaling = UtbetalingDbo(
        id = utbetalingId,
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        fristForGodkjenning = LocalDateTime.now(),
        beregning = UtbetalingBeregningForhandsgodkjent(
            input = UtbetalingBeregningForhandsgodkjent.Input(
                periode = Periode.forMonthOf(LocalDate.of(2024, 8, 1)),
                sats = 20205,
                stengt = setOf(),
                deltakelser = setOf(
                    DeltakelsePerioder(
                        deltakelseId = deltaker.id,
                        perioder = listOf(
                            DeltakelsePeriode(
                                periode = Periode(LocalDate.of(2024, 8, 1), LocalDate.of(2024, 8, 31)),
                                deltakelsesprosent = 100.0,
                            ),
                        ),
                    ),
                ),
            ),
            output = UtbetalingBeregningForhandsgodkjent.Output(
                belop = 10000,
                deltakelser = setOf(
                    DeltakelseManedsverk(
                        deltakelseId = deltaker.id,
                        manedsverk = 1.0,
                    ),
                ),
            ),
        ),
        kontonummer = Kontonummer("12312312312"),
        kid = null,
        periode = Periode.forMonthOf(LocalDate.of(2024, 8, 1)),
        innsender = null,
        beskrivelse = null,
    )

    val friUtbetalingId = UUID.randomUUID()
    val friUtbetaling = UtbetalingDbo(
        id = friUtbetalingId,
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        fristForGodkjenning = LocalDateTime.now(),
        beregning = UtbetalingBeregningFri(
            input = UtbetalingBeregningFri.Input(
                belop = 5000,
            ),
            output = UtbetalingBeregningFri.Output(
                belop = 5000,
            ),
        ),
        kontonummer = Kontonummer("12312312312"),
        kid = null,
        periode = Periode.forMonthOf(LocalDate.of(2024, 8, 1)),
        innsender = null,
        beskrivelse = "Test utbetaling",
    )

    val oauth = MockOAuth2Server()
    val clientEngine = createMockEngine {
        post("/graphql") {
            respondJson(
                """
                {
                    "data": {
                        "hentPersonBolk": [
                            {
                                "ident": "${identMedTilgang.value}",
                                "person": {
                                    "navn": [
                                        {
                                            "fornavn": "Test",
                                            "mellomnavn": null,
                                            "etternavn": "Testersen"
                                        }
                                    ],
                                    "foedselsdato": [
                                        {
                                            "foedselsdato": "1990-01-01",
                                            "foedselsaar": 1990
                                        }
                                    ],
                                    "adressebeskyttelse": []
                                }
                            }
                        ]
                    }
                }
                """.trimIndent(),
            )
        }
    }

    val domain = MulighetsrommetTestDomain(
        navEnheter = listOf(NavEnhetFixtures.IT, NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
        ansatte = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
        tiltakstyper = listOf(TiltakstypeFixtures.AFT),
        avtaler = listOf(
            AvtaleFixtures.AFT.copy(
                arrangor = AvtaleFixtures.AFT.arrangor?.copy(
                    hovedenhet = hovedenhet.id,
                    underenheter = listOf(underenhet.id),
                ),
            ),
        ),
        gjennomforinger = listOf(GjennomforingFixtures.AFT1.copy(arrangorId = underenhet.id)),
        deltakere = listOf(deltaker),
        arrangorer = listOf(hovedenhet, underenhet),
        tilsagn = listOf(tilsagn),
        utbetalinger = listOf(utbetaling, friUtbetaling),
    ) {
        setTilsagnStatus(tilsagn, TilsagnStatus.GODKJENT)
    }

    // PDL client and service will be initialized in beforeEach
    lateinit var pdlClient: no.nav.mulighetsrommet.api.clients.pdl.PdlClient
    lateinit var query: HentAdressebeskyttetPersonBolkPdlQuery
    lateinit var arrangorflateService: ArrangorFlateService

    beforeSpec {
        oauth.start()
    }

    beforeEach {
        domain.initialize(database.db)
        pdlClient = mockPdlClient(clientEngine)
        query = HentAdressebeskyttetPersonBolkPdlQuery(pdlClient)
        arrangorflateService = ArrangorFlateService(query, database.db)
    }

    afterEach {
        database.truncateAll()
    }

    afterSpec {
        oauth.shutdown()
    }

    fun MockEngineBuilder.mockAltinnAuthorizedParties() {
        post("/altinn/accessmanagement/api/v1/resourceowner/authorizedparties") {
            val body = Json.decodeFromString<AltinnClient.AltinnRequest>(
                (it.body as TextContent).text,
            )
            if (body.value == identMedTilgang.value) {
                respondJson(
                    listOf(
                        AuthorizedParty(
                            organizationNumber = underenhet.organisasjonsnummer.value,
                            name = underenhet.navn,
                            type = AuthorizedPartyType.Organization,
                            authorizedResources = listOf("nav_tiltaksarrangor_be-om-utbetaling"),
                            subunits = emptyList(),
                        ),
                    ),
                )
            } else {
                respondJson(emptyList<AuthorizedParty>())
            }
        }
    }

    fun MockEngineBuilder.mockJournalpost() {
        post("/dokark/rest/journalpostapi/v1/journalpost") {
            respondJson(
                DokarkResponse(
                    journalpostId = "123",
                    journalstatus = "bra",
                    melding = null,
                    journalpostferdigstilt = true,
                    dokumenter = listOf(DokarkResponseDokument("123")),
                ),
            )
        }
    }

    fun appConfig(
        engine: MockEngine = createMockEngine {
            mockAltinnAuthorizedParties()
            mockJournalpost()
        },
    ) = createTestApplicationConfig().copy(
        database = databaseConfig,
        auth = createAuthConfig(oauth, roles = emptyList()),
        engine = engine,
    )

    test("getUtbetalinger should return list of compact utbetalinger for arrangor") {
        val result = arrangorflateService.getUtbetalinger(underenhet.organisasjonsnummer)

        result shouldHaveSize 2
        result.any { it.id == utbetalingId } shouldBe true
        result.any { it.id == friUtbetalingId } shouldBe true

        val forsteUtbetaling = result.first { it.id == utbetalingId }
        forsteUtbetaling.belop shouldBe 10000
        forsteUtbetaling.status shouldBe ArrFlateUtbetalingStatus.KLAR_FOR_GODKJENNING

        val andreUtbetaling = result.first { it.id == friUtbetalingId }
        andreUtbetaling.belop shouldBe 5000
        andreUtbetaling.status shouldBe ArrFlateUtbetalingStatus.KLAR_FOR_GODKJENNING
    }

    test("getUtbetaling should return utbetaling by ID") {
        val result = arrangorflateService.getUtbetaling(utbetalingId)

        result.shouldNotBeNull()
        result.id shouldBe utbetalingId
        result.beregning should beInstanceOf<UtbetalingBeregningForhandsgodkjent>()
        (result.beregning as UtbetalingBeregningForhandsgodkjent).output.belop shouldBe 10000
    }

    test("getTilsagn should return arrangorflateTilsagn by ID") {
        val result = arrangorflateService.getTilsagn(tilsagnId)

        result.shouldNotBeNull()
        result.id shouldBe tilsagnId
        result.status.status shouldBe TilsagnStatus.GODKJENT
    }

    test("getTilsagnByOrgnr should return list of tilsagn for arrangor") {
        val result = arrangorflateService.getTilsagnByOrgnr(underenhet.organisasjonsnummer)

        result shouldHaveSize 1
        result[0].id shouldBe tilsagnId
        result[0].status.status shouldBe TilsagnStatus.GODKJENT
    }

    test("getArrangorflateTilsagnTilUtbetaling should return tilsagn for given gjennomforing and period") {
        val periode = Periode(LocalDate.of(2024, 7, 1), LocalDate.of(2024, 8, 1))
        val result = arrangorflateService.getArrangorflateTilsagnTilUtbetaling(GjennomforingFixtures.AFT1.id, periode)

        result shouldHaveSize 1
        result[0].id shouldBe tilsagnId
    }

    test("getRelevanteForslag should return empty list when no forslag exists") {
        val utbetalingDto = database.db.session {
            queries.utbetaling.get(utbetalingId)
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
                        deltakerId = deltakerId,
                        endring = Melding.Forslag.Endring.Deltakelsesmengde(
                            deltakelsesprosent = 80,
                            gyldigFra = LocalDate.of(2024, 8, 15),
                        ),
                        status = DeltakerForslag.Status.VENTER_PA_SVAR,
                    ),
                )
            }

            val utbetalingDto = database.db.session {
                queries.utbetaling.get(utbetalingId)
            }
            utbetalingDto.shouldNotBeNull()

            val result = arrangorflateService.toArrFlateUtbetaling(utbetalingDto)

            result.id shouldBe utbetalingId
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
                queries.utbetaling.get(friUtbetalingId)
            }
            utbetalingDto.shouldNotBeNull()

            val result = arrangorflateService.toArrFlateUtbetaling(utbetalingDto)

            result.id shouldBe friUtbetalingId
            result.status shouldBe ArrFlateUtbetalingStatus.KLAR_FOR_GODKJENNING
            result.beregning should beInstanceOf<Beregning.Fri>()
            val beregning = result.beregning as Beregning.Fri
            beregning.belop shouldBe 5000
        }
    }
})
