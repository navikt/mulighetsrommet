package no.nav.mulighetsrommet.api.utbetaling

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
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
import no.nav.mulighetsrommet.api.withTestApplication
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

class ArrangorflateRoutesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val identMedTilgang = NorskIdent("01010199988")

    val hovedenhet = ArrangorFixtures.hovedenhet
    val underenhet = ArrangorFixtures.underenhet1

    val deltaker = DeltakerDbo(
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

    val tilsagn = TilsagnDbo(
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

    val utbetaling = UtbetalingDbo(
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
                belop = 0,
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
        utbetalinger = listOf(utbetaling),
    ) {
        setTilsagnStatus(tilsagn, TilsagnStatus.GODKJENT)
    }

    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
    }

    beforeEach {
        domain.initialize(database.db)
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

    test("401 Unauthorized uten pid i claims") {
        withTestApplication(appConfig()) {
            val response = client.get("/api/v1/intern/arrangorflate/tilgang-arrangor") {
                bearerAuth(oauth.issueToken().serialize())
            }
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }
})
