package no.nav.mulighetsrommet.api.utbetaling

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.content.*
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.altinn.AltinnClient
import no.nav.mulighetsrommet.altinn.AltinnClient.AuthorizedParty
import no.nav.mulighetsrommet.altinn.AltinnClient.AuthorizedPartyType
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakerPersonaliaResponse
import no.nav.mulighetsrommet.api.clients.dokark.DokarkResponse
import no.nav.mulighetsrommet.api.clients.dokark.DokarkResponseDokument
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontonummerResponse
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
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.clamav.ScanResult
import no.nav.mulighetsrommet.clamav.Status
import no.nav.mulighetsrommet.ktor.MockEngineBuilder
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.tiltak.okonomi.BestillingStatusType
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

object ArrangorflateTestUtils {
    val identMedTilgang = NorskIdent("01010199988")
    val hovedenhet = ArrangorFixtures.hovedenhet
    val underenhet = ArrangorFixtures.underenhet1

    fun createTestDeltaker(): DeltakerDbo = DeltakerDbo(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        startDato = GjennomforingFixtures.AFT1.startDato,
        sluttDato = GjennomforingFixtures.AFT1.sluttDato,
        registrertDato = GjennomforingFixtures.AFT1.startDato,
        endretTidspunkt = LocalDateTime.now(),
        deltakelsesprosent = 100.0,
        deltakelsesmengder = listOf(),
        status = DeltakerStatus(
            type = DeltakerStatusType.DELTAR,
            aarsak = null,
            opprettetDato = LocalDateTime.now(),
        ),
    )

    fun createTestTilsagn(): TilsagnDbo = TilsagnDbo(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        periode = Periode(LocalDate.of(2024, 6, 1), LocalDate.of(2025, 1, 1)),
        lopenummer = 1,
        bestillingsnummer = "A-2025/1-1",
        kostnadssted = NavEnhetFixtures.Innlandet.enhetsnummer,
        beregning = TilsagnBeregningFri(
            input = TilsagnBeregningFri.Input(
                linjer = listOf(
                    TilsagnBeregningFri.InputLinje(
                        id = UUID.randomUUID(),
                        beskrivelse = "Beskrivelse",
                        belop = 1000,
                        antall = 1,
                    ),
                ),
                prisbetingelser = null,
            ),
            output = TilsagnBeregningFri.Output(1000),
        ),
        type = TilsagnType.TILSAGN,
        belopBrukt = 0,
        bestillingStatus = BestillingStatusType.AKTIV,
        kommentar = null,
    )

    fun createTestUtbetalingForhandsgodkjent(deltakerId: UUID): UtbetalingDbo = UtbetalingDbo(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        beregning = UtbetalingBeregningFastSatsPerTiltaksplassPerManed(
            input = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(
                periode = Periode.forMonthOf(LocalDate.of(2024, 8, 1)),
                sats = 20205,
                stengt = setOf(),
                deltakelser = setOf(
                    DeltakelseDeltakelsesprosentPerioder(
                        deltakelseId = deltakerId,
                        perioder = listOf(
                            DeltakelsesprosentPeriode(
                                periode = Periode(LocalDate.of(2024, 8, 1), LocalDate.of(2024, 8, 31)),
                                deltakelsesprosent = 100.0,
                            ),
                        ),
                    ),
                ),
            ),
            output = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
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
        tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        godkjentAvArrangorTidspunkt = null,
        status = UtbetalingStatusType.GENERERT,
    )

    fun createTestUtbetalingFri(): UtbetalingDbo = UtbetalingDbo(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        beregning = UtbetalingBeregningFri(
            input = UtbetalingBeregningFri.Input(5000),
            output = UtbetalingBeregningFri.Output(5000),
        ),
        kontonummer = Kontonummer("12312312312"),
        kid = null,
        periode = Periode.forMonthOf(LocalDate.of(2024, 8, 1)),
        innsender = null,
        beskrivelse = "Test utbetaling",
        tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        godkjentAvArrangorTidspunkt = null,
        status = UtbetalingStatusType.GENERERT,
    )

    fun createTestDomain(
        deltaker: DeltakerDbo = createTestDeltaker(),
        tilsagn: TilsagnDbo = createTestTilsagn(),
        utbetalinger: List<UtbetalingDbo> = listOf(createTestUtbetalingForhandsgodkjent(deltaker.id)),
    ): MulighetsrommetTestDomain = MulighetsrommetTestDomain(
        navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
        ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
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

    fun mockKontoregisterOrganisasjon(builder: MockEngineBuilder) {
        val path = Regex(""".*/kontoregister/api/v1/hent-kontonummer-for-organisasjon/.*""")
        builder.get(path) {
            respondJson(
                KontonummerResponse(
                    kontonr = "12345678901",
                    mottaker = "asdf",
                ),
            )
        }
    }

    fun mockClamAvScan(builder: MockEngineBuilder) {
        builder.post("/scan") {
            respondJson(listOf(ScanResult(Filename = "filnavn", Result = Status.OK)))
        }
    }

    fun mockAmtDeltaker(builder: MockEngineBuilder) {
        builder.post("/amt-deltaker/external/deltakere/personalia") {
            respondJson(emptyList<DeltakerPersonaliaResponse>())
        }
    }

    fun appConfig(
        oauth: MockOAuth2Server,
        engine: MockEngine = createMockEngine {
            mockAltinnAuthorizedParties(this)
            mockJournalpost(this)
            mockClamAvScan(this)
            mockAmtDeltaker(this)
            mockKontoregisterOrganisasjon(this)
        },
    ) = createTestApplicationConfig().copy(
        database = databaseConfig,
        auth = createAuthConfig(oauth, roles = setOf()),
        engine = engine,
    )

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
