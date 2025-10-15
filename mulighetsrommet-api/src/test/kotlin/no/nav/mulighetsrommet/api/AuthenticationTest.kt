package no.nav.mulighetsrommet.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.plugins.AppRoles
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.IdPortenAmr
import no.nav.mulighetsrommet.api.plugins.authenticate
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.intellij.lang.annotations.Language
import java.util.*

class AuthenticationTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }

    test("verify provider NAV_ANSATT") {
        val requestWithoutBearerToken = { _: HttpRequestBuilder -> }
        val requestWithWrongAudience = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken(audience = "skatteetaten").serialize())
        }
        val requestWithWrongIssuer = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken(issuerId = "skatteetaten").serialize())
        }
        val requestWithoutNAVident = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken().serialize())
        }
        val requestWithNAVident = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken(claims = mapOf(Pair("NAVident", "ABC123"))).serialize())
        }

        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = setOf()),
        )
        withTestApplication(config, additionalConfiguration = {
            routing {
                authenticate(AuthProvider.NAV_ANSATT) {
                    get("NAV_ANSATT") { call.respond(HttpStatusCode.OK) }
                }
            }
        }) {
            forAll(
                row(requestWithoutBearerToken, HttpStatusCode.Unauthorized),
                row(requestWithWrongAudience, HttpStatusCode.Unauthorized),
                row(requestWithWrongIssuer, HttpStatusCode.Unauthorized),
                row(requestWithoutNAVident, HttpStatusCode.Unauthorized),
                row(requestWithNAVident, HttpStatusCode.OK),
            ) { buildRequest, responseStatusCode ->
                val response = client.get("/NAV_ANSATT") { buildRequest(this) }

                response.status shouldBe responseStatusCode
            }
        }
    }

    test("verify provider NAV_ANSATT_WITH_ROLES") {
        MulighetsrommetTestDomain(
            ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
            navEnheter = listOf(NavEnhetFixtures.Innlandet),
            arrangorer = listOf(),
            avtaler = listOf(),
        ).initialize(database.db)

        val rolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TEAM_MULIGHETSROMMET)

        val requestWithoutBearerToken = { _: HttpRequestBuilder -> }
        val requestWithWrongAudience = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken(audience = "skatteetaten").serialize())
        }
        val requestWithWrongIssuer = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken(issuerId = "skatteetaten").serialize())
        }
        val requestWithoutNAVident = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken().serialize())
        }
        val requestWithoutOid = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken(claims = mapOf(Pair("NAVident", "ABC123"))).serialize())
        }
        val requestWithoutSid = { request: HttpRequestBuilder ->
            val claims = mapOf(
                "NAVident" to "B123456",
                "oid" to UUID.randomUUID().toString(),
            )
            request.bearerAuth(oauth.issueToken(claims = claims).serialize())
        }
        val requestWithoutRoles = oauth.createRequestWithAnsattClaims(NavAnsattFixture.MikkeMus, roles = setOf())
        val requestWithRoles = oauth.createRequestWithAnsattClaims(NavAnsattFixture.DonaldDuck, roles = setOf(rolle))

        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = setOf(rolle)),
        )
        withTestApplication(config, additionalConfiguration = {
            routing {
                authenticate(AuthProvider.NAV_ANSATT_WITH_ROLES) {
                    get("NAV_ANSATT_WITH_ROLES") { call.respond(HttpStatusCode.OK) }
                }
            }
        }) {
            forAll(
                row(requestWithoutBearerToken, HttpStatusCode.Unauthorized),
                row(requestWithWrongAudience, HttpStatusCode.Unauthorized),
                row(requestWithWrongIssuer, HttpStatusCode.Unauthorized),
                row(requestWithoutNAVident, HttpStatusCode.Unauthorized),
                row(requestWithoutOid, HttpStatusCode.Unauthorized),
                row(requestWithoutSid, HttpStatusCode.Unauthorized),
                row(requestWithoutRoles, HttpStatusCode.Unauthorized),
                row(requestWithRoles, HttpStatusCode.OK),
            ) { buildRequest, responseStatusCode ->
                val response = client.get("/NAV_ANSATT_WITH_ROLES") { buildRequest(this) }

                response.status shouldBe responseStatusCode
            }
        }
    }

    test("verify provider NAIS_APP_ARENA_ADAPTER_ACCESS") {
        val requestWithoutBearerToken = { _: HttpRequestBuilder -> }
        val requestWithWrongAudience = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken(audience = "skatteetaten").serialize())
        }
        val requestWithWrongIssuer = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken(issuerId = "skatteetaten").serialize())
        }
        val requestWithoutClaimAccessAsApplication = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken().serialize())
        }
        val requestWithClaimAccessAsApplication = { request: HttpRequestBuilder ->
            val claims = mapOf("roles" to listOf(AppRoles.ACCESS_AS_APPLICATION))
            request.bearerAuth(oauth.issueToken(claims = claims).serialize())
        }
        val requestWithClaimsArenaAdapter = { request: HttpRequestBuilder ->
            val claims = mapOf("roles" to listOf(AppRoles.ACCESS_AS_APPLICATION, AppRoles.ARENA_ADAPTER))
            request.bearerAuth(oauth.issueToken(claims = claims).serialize())
        }

        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = setOf()),
        )
        withTestApplication(config, additionalConfiguration = {
            routing {
                authenticate(AuthProvider.NAIS_APP_ARENA_ADAPTER_ACCESS) {
                    get("NAIS_APP_ARENA_ADAPTER_ACCESS") { call.respond(HttpStatusCode.OK) }
                }
            }
        }) {
            forAll(
                row(requestWithoutBearerToken, HttpStatusCode.Unauthorized),
                row(requestWithWrongAudience, HttpStatusCode.Unauthorized),
                row(requestWithWrongIssuer, HttpStatusCode.Unauthorized),
                row(requestWithoutClaimAccessAsApplication, HttpStatusCode.Unauthorized),
                row(requestWithClaimAccessAsApplication, HttpStatusCode.Unauthorized),
                row(requestWithClaimsArenaAdapter, HttpStatusCode.OK),
            ) { buildRequest, responseStatusCode ->
                val response = client.get("/NAIS_APP_ARENA_ADAPTER_ACCESS") { buildRequest(this) }

                response.status shouldBe responseStatusCode
            }
        }
    }

    test("verify provider NAIS_APP_GJENNOMFORING_ACCESS") {
        val requestWithoutBearerToken = { _: HttpRequestBuilder -> }
        val requestWithWrongAudience = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken(audience = "skatteetaten").serialize())
        }
        val requestWithWrongIssuer = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken(issuerId = "skatteetaten").serialize())
        }
        val requestWithoutClaimAccessAsApplication = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken().serialize())
        }
        val requestWithClaimAccessAsApplication = { request: HttpRequestBuilder ->
            val claims = mapOf("roles" to listOf(AppRoles.ACCESS_AS_APPLICATION))
            request.bearerAuth(oauth.issueToken(claims = claims).serialize())
        }
        val requestWithClaimsReadTiltaksgjennomforing = { request: HttpRequestBuilder ->
            val claims = mapOf("roles" to listOf(AppRoles.ACCESS_AS_APPLICATION, AppRoles.READ_GJENNOMFORING))
            request.bearerAuth(oauth.issueToken(claims = claims).serialize())
        }

        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = setOf()),
        )
        withTestApplication(config, additionalConfiguration = {
            routing {
                authenticate(AuthProvider.NAIS_APP_GJENNOMFORING_ACCESS) {
                    get("NAIS_APP_GJENNOMFORING_ACCESS") { call.respond(HttpStatusCode.OK) }
                }
            }
        }) {
            forAll(
                row(requestWithoutBearerToken, HttpStatusCode.Unauthorized),
                row(requestWithWrongAudience, HttpStatusCode.Unauthorized),
                row(requestWithWrongIssuer, HttpStatusCode.Unauthorized),
                row(requestWithoutClaimAccessAsApplication, HttpStatusCode.Unauthorized),
                row(requestWithClaimAccessAsApplication, HttpStatusCode.Unauthorized),
                row(requestWithClaimsReadTiltaksgjennomforing, HttpStatusCode.OK),
            ) { buildRequest, responseStatusCode ->
                val response = client.get("/NAIS_APP_GJENNOMFORING_ACCESS") { buildRequest(this) }

                response.status shouldBe responseStatusCode
            }
        }
    }

    test("verify provider TOKEN_X_ARRANGOR_FLATE") {
        val personMedRettighet = "11830348931"

        database.run {
            @Language("PostgreSQL")
            val query = """
                insert into altinn_person_rettighet (norsk_ident, organisasjonsnummer, rettighet, expiry)
                values('$personMedRettighet', '123456789', 'TILTAK_ARRANGOR_BE_OM_UTBETALING', '3000-01-01') on conflict do nothing;
            """.trimIndent()
            it.execute(queryOf(query))
        }

        val requestWithoutBearerToken = { _: HttpRequestBuilder -> }
        val requestWithWrongAudience = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken(audience = "skatteetaten").serialize())
        }
        val requestWithWrongIssuer = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken(issuerId = "skatteetaten").serialize())
        }
        val requestWithoutPid = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken().serialize())
        }
        val requestWithoutAmr = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken(claims = mapOf("pid" to "21830348931")).serialize())
        }
        val requestWithPidAmrWithoutRettighet = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken(claims = mapOf("pid" to "21830348931", "amr" to IdPortenAmr.BankID.toString())).serialize())
        }
        val requestWithPidAmrWithRettighet = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken(claims = mapOf("pid" to personMedRettighet, "amr" to IdPortenAmr.BankID.toString())).serialize())
        }

        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = setOf()),
        )
        withTestApplication(config, additionalConfiguration = {
            routing {
                authenticate(AuthProvider.TOKEN_X_ARRANGOR_FLATE) {
                    get("TOKEN_X_ARRANGOR_FLATE") { call.respond(HttpStatusCode.OK) }
                }
            }
        }) {
            forAll(
                row(requestWithoutBearerToken, HttpStatusCode.Unauthorized),
                row(requestWithWrongAudience, HttpStatusCode.Unauthorized),
                row(requestWithWrongIssuer, HttpStatusCode.Unauthorized),
                row(requestWithoutPid, HttpStatusCode.Unauthorized),
                row(requestWithoutAmr, HttpStatusCode.Unauthorized),
                row(requestWithPidAmrWithoutRettighet, HttpStatusCode.Unauthorized),
                row(requestWithPidAmrWithRettighet, HttpStatusCode.OK),
            ) { buildRequest, responseStatusCode ->
                val response = client.get("/TOKEN_X_ARRANGOR_FLATE") { buildRequest(this) }

                response.status shouldBe responseStatusCode
            }
        }
    }
})
