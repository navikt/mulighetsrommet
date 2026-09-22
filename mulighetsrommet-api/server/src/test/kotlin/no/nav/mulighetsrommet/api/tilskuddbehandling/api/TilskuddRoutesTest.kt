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
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TilskuddFixtures
import no.nav.mulighetsrommet.api.getAnsattClaims
import no.nav.mulighetsrommet.api.mockKontoregisterOrganisasjon
import no.nav.mulighetsrommet.api.withTestApplication
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.ktor.createMockEngine
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
    val ansatt = NavAnsattFixture.DonaldDuck

    val domain = MulighetsrommetTestDomain(
        ansatte = listOf(ansatt),
        gjennomforinger = listOf(GjennomforingFixtures.EnkelAmo),
    )

    beforeEach {
        domain.initialize(database.api)
        database.api.transaction {
            queries.tilskuddBehandling.upsert(TilskuddFixtures.Behandling)
        }
    }

    afterEach {
        database.truncateAll()
    }

    fun appConfig() = ApplicationConfigTest.copy(
        auth = createAuthConfig(
            oauth,
            roles = setOf(generellRolle, saksbehandlerOkonomiRolle),
        ),
        engine = createMockEngine {
            mockKontoregisterOrganisasjon()
        },
    )

    test("henter tilskudd med handlinger") {
        withTestApplication(appConfig()) {
            val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, saksbehandlerOkonomiRolle))

            val response = client.get("/api/tiltaksadministrasjon/tilskudd/${TilskuddFixtures.TilskuddVedtakInnvilgelse.tilskuddId}") {
                bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
            }

            response.status shouldBe HttpStatusCode.OK

            val body = response.body<TilskuddDto>()
            body.tilskudd.id shouldBe TilskuddFixtures.TilskuddVedtakInnvilgelse.tilskuddId
            body.handlinger shouldBe setOf(TilskuddHandling.OPPHOR)
        }
    }
})
