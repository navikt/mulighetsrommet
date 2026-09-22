package no.nav.mulighetsrommet.api.tilskuddbehandling.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import no.nav.mulighetsrommet.admin.navansatt.EntraGroupNavAnsattRolleMapping
import no.nav.mulighetsrommet.api.ApplicationConfigTest
import no.nav.mulighetsrommet.api.createAuthConfig
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsattRolle
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TilskuddFixtures
import no.nav.mulighetsrommet.api.getAnsattClaims
import no.nav.mulighetsrommet.api.mockKontoregisterOrganisasjon
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddBehandling
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddVedtak
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatus
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingType.REGISTRERING
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.api.withTestApplication
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.UUID

class TilskuddRoutesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())
    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }

    val generellRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TILTAKADMINISTRASJON_GENERELL)
    val saksbehandlerOkonomiRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.SAKSBEHANDLER_OKONOMI)
    val teamMulighetsrommetRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TEAM_MULIGHETSROMMET)
    val ansattUtenTeamrolle = NavAnsattFixture.DonaldDuck
    val ansattMedTeamrolle = NavAnsattFixture.MikkeMus.medRoller(
        setOf(NavAnsattRolle.generell(Rolle.TEAM_MULIGHETSROMMET)),
    )

    fun appConfig() = ApplicationConfigTest.copy(
        auth = createAuthConfig(
            oauth,
            roles = setOf(generellRolle, saksbehandlerOkonomiRolle, teamMulighetsrommetRolle),
        ),
        engine = createMockEngine {
            mockKontoregisterOrganisasjon()
        },
    )

    fun initData(
        behandling: TilskuddBehandling,
    ) {
        MulighetsrommetTestDomain(
            ansatte = listOf(ansattMedTeamrolle, ansattUtenTeamrolle),
            gjennomforinger = listOf(GjennomforingFixtures.EnkelAmo),
        ).initialize(database.api)
        database.api.transaction {
            queries.tilskuddBehandling.upsert(behandling)
        }
    }

    fun lagBehandling(
        tilskuddVedtak: TilskuddVedtak = TilskuddFixtures.TilskuddVedtakInnvilgelse,
        tilskudd: List<TilskuddVedtak> = listOf(tilskuddVedtak),
    ): TilskuddBehandling {
        return TilskuddFixtures.Behandling.copy(
            status = TilskuddBehandlingStatus.FERDIG_BEHANDLET,
            type = REGISTRERING,
            tilskudd = tilskudd,
        )
    }

    beforeEach {
        database.truncateAll()
    }

    afterEach {
        database.truncateAll()
    }

    test("henter tilskudd med OPPHOR når siste vedtak er innvilget og ansatt har team-rolle") {
        initData(lagBehandling())

        withTestApplication(appConfig()) {
            val navAnsattClaims = getAnsattClaims(
                ansattMedTeamrolle,
                setOf(generellRolle, saksbehandlerOkonomiRolle, teamMulighetsrommetRolle),
            )

            val response = client.get("/api/tiltaksadministrasjon/tilskudd/${TilskuddFixtures.TilskuddVedtakInnvilgelse.tilskuddId}") {
                bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
            }

            response.status shouldBe HttpStatusCode.OK

            val body = response.body<TilskuddDto>()
            body.tilskudd.id shouldBe TilskuddFixtures.TilskuddVedtakInnvilgelse.tilskuddId
            body.handlinger shouldBe setOf(TilskuddHandling.OPPHOR)
        }
    }

    test("henter ingen handlinger når ansatt ikke har team-rolle") {
        initData(lagBehandling())

        withTestApplication(appConfig()) {
            val navAnsattClaims = getAnsattClaims(ansattUtenTeamrolle, setOf(generellRolle, saksbehandlerOkonomiRolle))

            val response = client.get("/api/tiltaksadministrasjon/tilskudd/${TilskuddFixtures.TilskuddVedtakInnvilgelse.tilskuddId}") {
                bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
            }

            response.status shouldBe HttpStatusCode.OK

            val body = response.body<TilskuddDto>()
            body.handlinger shouldBe emptySet<TilskuddHandling>()
        }
    }

    test("henter ingen handlinger når siste vedtak er opphørt") {
        initData(
            lagBehandling(
                tilskuddVedtak = TilskuddFixtures.TilskuddVedtakInnvilgelse.copy(
                    utbetalingBelop = ValutaBelop(0, Valuta.NOK),
                ),
            ),
        )

        withTestApplication(appConfig()) {
            val navAnsattClaims = getAnsattClaims(
                ansattMedTeamrolle,
                setOf(generellRolle, saksbehandlerOkonomiRolle, teamMulighetsrommetRolle),
            )

            val response = client.get("/api/tiltaksadministrasjon/tilskudd/${TilskuddFixtures.TilskuddVedtakInnvilgelse.tilskuddId}") {
                bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
            }

            response.status shouldBe HttpStatusCode.OK

            val body = response.body<TilskuddDto>()
            body.handlinger shouldBe emptySet<TilskuddHandling>()
        }
    }

    test("henter ingen handlinger når siste vedtak ikke er innvilget") {
        initData(
            lagBehandling(
                tilskuddVedtak = TilskuddFixtures.TilskuddVedtakInnvilgelse.copy(
                    vedtakResultat = VedtakResultat.AVSLAG,
                ),
            ),
        )

        withTestApplication(appConfig()) {
            val navAnsattClaims = getAnsattClaims(
                ansattMedTeamrolle,
                setOf(generellRolle, saksbehandlerOkonomiRolle, teamMulighetsrommetRolle),
            )

            val response = client.get("/api/tiltaksadministrasjon/tilskudd/${TilskuddFixtures.TilskuddVedtakInnvilgelse.tilskuddId}") {
                bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
            }

            response.status shouldBe HttpStatusCode.OK

            val body = response.body<TilskuddDto>()
            body.handlinger shouldBe emptySet<TilskuddHandling>()
        }
    }

    test("bruker siste vedtak når tidligere vedtak er innvilget") {
        val tilskuddId = UUID.randomUUID()
        val tidligereVedtak = TilskuddFixtures.TilskuddVedtakInnvilgelse.copy(
            id = UUID.randomUUID(),
            tilskuddId = tilskuddId,
        )
        val sisteVedtak = TilskuddFixtures.TilskuddVedtakAvslag.copy(
            id = UUID.randomUUID(),
            tilskuddId = tilskuddId,
        )

        initData(
            lagBehandling(
                tilskudd = listOf(
                    tidligereVedtak,
                    sisteVedtak,
                ),
            ),
        )

        withTestApplication(appConfig()) {
            val navAnsattClaims = getAnsattClaims(
                ansattMedTeamrolle,
                setOf(generellRolle, saksbehandlerOkonomiRolle, teamMulighetsrommetRolle),
            )

            val response = client.get("/api/tiltaksadministrasjon/tilskudd/$tilskuddId") {
                bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
            }

            response.status shouldBe HttpStatusCode.OK

            val body = response.body<TilskuddDto>()
            body.handlinger shouldBe emptySet<TilskuddHandling>()
        }
    }
})
