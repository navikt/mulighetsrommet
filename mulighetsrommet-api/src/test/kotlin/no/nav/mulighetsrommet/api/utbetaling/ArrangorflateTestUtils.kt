package no.nav.mulighetsrommet.api.utbetaling

import io.ktor.client.engine.mock.*
import io.ktor.http.content.*
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.altinn.AltinnClient
import no.nav.mulighetsrommet.altinn.AltinnClient.AuthorizedParty
import no.nav.mulighetsrommet.altinn.AltinnClient.AuthorizedPartyType
import no.nav.mulighetsrommet.api.clients.dokark.DokarkResponse
import no.nav.mulighetsrommet.api.clients.dokark.DokarkResponseDokument
import no.nav.mulighetsrommet.api.createAuthConfig
import no.nav.mulighetsrommet.api.createTestApplicationConfig
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerDbo
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelseManedsverk
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsePeriode
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsePerioder
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningForhandsgodkjent
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
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

/**
 * Shared test utilities for ArrangorflateService and ArrangorflateRoutes tests
 */
object ArrangorflateTestUtils {
    val identMedTilgang = NorskIdent("01010199988")
    val hovedenhet = ArrangorFixtures.hovedenhet
    val underenhet = ArrangorFixtures.underenhet1

    /**
     * Creates a test deltaker
     */
    fun createTestDeltaker(): DeltakerDbo = DeltakerDbo(
        id = UUID.randomUUID(),
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

    /**
     * Creates a test tilsagn
     */
    fun createTestTilsagn(): TilsagnDbo = TilsagnDbo(
        id = UUID.randomUUID(),
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

    /**
     * Creates a test utbetaling with a forhandsgodkjent beregning
     */
    fun createTestUtbetalingForhandsgodkjent(deltakerId: UUID): UtbetalingDbo = UtbetalingDbo(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        fristForGodkjenning = LocalDateTime.now(),
        beregning = UtbetalingBeregningForhandsgodkjent(
            input = UtbetalingBeregningForhandsgodkjent.Input(
                periode = Periode.forMonthOf(LocalDate.of(2024, 8, 1)),
                sats = 20205,
                stengt = setOf(),
                deltakelser = setOf(
                    DeltakelsePerioder(
                        deltakelseId = deltakerId,
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
                        deltakelseId = deltakerId,
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

    /**
     * Creates a test utbetaling with a fri beregning
     */
    fun createTestUtbetalingFri(): UtbetalingDbo = UtbetalingDbo(
        id = UUID.randomUUID(),
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

    /**
     * Creates a test domain with standard test data
     */
    fun createTestDomain(
        deltaker: DeltakerDbo = createTestDeltaker(),
        tilsagn: TilsagnDbo = createTestTilsagn(),
        utbetalinger: List<UtbetalingDbo> = listOf(createTestUtbetalingForhandsgodkjent(deltaker.id)),
    ): MulighetsrommetTestDomain = MulighetsrommetTestDomain(
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
        utbetalinger = utbetalinger,
    ) {
        setTilsagnStatus(tilsagn, TilsagnStatus.GODKJENT)
    }

    /**
     * Mocks the AltinnAuthorizedParties endpoint
     */
    fun mockAltinnAuthorizedParties(builder: MockEngineBuilder) {
        builder.post("/altinn/accessmanagement/api/v1/resourceowner/authorizedparties") {
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

    /**
     * Mocks the Journalpost endpoint
     */
    fun mockJournalpost(builder: MockEngineBuilder) {
        builder.post("/dokark/rest/journalpostapi/v1/journalpost") {
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

    /**
     * Creates a standard application config for tests
     */
    fun appConfig(
        oauth: MockOAuth2Server,
        engine: MockEngine = createMockEngine {
            mockAltinnAuthorizedParties(this)
            mockJournalpost(this)
        },
    ) = createTestApplicationConfig().copy(
        database = databaseConfig,
        auth = createAuthConfig(oauth, roles = emptyList()),
        engine = engine,
    )

    /**
     * Creates a mock engine with PDL responses
     */
    fun createPdlMockEngine() = createMockEngine {
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
}
