package no.nav.mulighetsrommet.api.routes.internal

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.mulighetsrommet.api.*
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.*

class MaamRoutesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val ansatt = NavAnsattFixture.DonaldDuck
    val domain = MulighetsrommetTestDomain(
        ansatte = listOf(ansatt),
    )

    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
        domain.initialize(database.db)
    }

    afterSpec {
        oauth.shutdown()
    }

    val generellRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TILTAKADMINISTRASJON_GENERELL)
    val teamMulighetsrommetRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TEAM_MULIGHETSROMMET)

    fun appConfig() = createTestApplicationConfig().copy(
        auth = createAuthConfig(
            oauth,
            roles = setOf(generellRolle, teamMulighetsrommetRolle),
        ),
    )

    test("maam er bare tilgjengelig med når man har rollen TEAM_MULIGHETSROMMET") {
        withTestApplication(appConfig()) {
            forAll(
                row(generellRolle, HttpStatusCode.Forbidden),
                row(teamMulighetsrommetRolle, HttpStatusCode.OK),
            ) { rolle, status ->
                val claims = getAnsattClaims(ansatt, setOf(rolle))

                val response = client.get("/api/intern/maam/topics") {
                    bearerAuth(oauth.issueToken(claims = claims).serialize())
                    contentType(ContentType.Application.Json)
                }

                response.status shouldBe status
            }
        }
    }
})
