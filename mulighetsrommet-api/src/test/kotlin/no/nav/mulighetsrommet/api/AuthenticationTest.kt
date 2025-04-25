package no.nav.mulighetsrommet.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.msgraph.GetMemberGroupsResponse
import no.nav.mulighetsrommet.api.clients.msgraph.MsGraphGroup
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.plugins.AppRoles
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.authenticate
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.intellij.lang.annotations.Language
import java.util.*

class AuthenticationTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }

    fun Application.configureTestAuthentationRoutes() {
        routing {
            authenticate(AuthProvider.AZURE_AD_NAV_IDENT) {
                get("AZURE_AD_NAV_IDENT") { call.respond(HttpStatusCode.OK) }
            }

            authenticate(AuthProvider.NAV_ANSATT_WITH_ROLES) {
                get("NAV_ANSATT_WITH_ROLES") { call.respond(HttpStatusCode.OK) }
            }

            authenticate(AuthProvider.AZURE_AD_DEFAULT_APP) {
                get("AZURE_AD_DEFAULT_APP") { call.respond(HttpStatusCode.OK) }
            }

            authenticate(AuthProvider.AZURE_AD_TILTAKSGJENNOMFORING_APP) {
                get("AZURE_AD_TILTAKSGJENNOMFORING_APP") { call.respond(HttpStatusCode.OK) }
            }

            authenticate(AuthProvider.TOKEN_X_ARRANGOR_FLATE) {
                get("TOKEN_X_ARRANGOR_FLATE") { call.respond(HttpStatusCode.OK) }
            }
        }
    }

    test("verify provider AZURE_AD_NAV_IDENT") {
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
        withTestApplication(config, additionalConfiguration = Application::configureTestAuthentationRoutes) {
            forAll(
                row(requestWithoutBearerToken, HttpStatusCode.Unauthorized),
                row(requestWithWrongAudience, HttpStatusCode.Unauthorized),
                row(requestWithWrongIssuer, HttpStatusCode.Unauthorized),
                row(requestWithoutNAVident, HttpStatusCode.Unauthorized),
                row(requestWithNAVident, HttpStatusCode.OK),
            ) { buildRequest, responseStatusCode ->
                val response = client.get("/AZURE_AD_NAV_IDENT") { buildRequest(this) }

                response.status shouldBe responseStatusCode
            }
        }
    }

    test("verify provider NAV_ANSATT_WITH_ROLES") {
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
        val userWithoutRoles = UUID.randomUUID().toString()
        val requestWithWrongGroup = { request: HttpRequestBuilder ->
            val claims = mapOf(
                "NAVident" to "ABC123",
                "oid" to userWithoutRoles,
            )
            request.bearerAuth(oauth.issueToken(claims = claims).serialize())
        }
        val userWithRoles = UUID.randomUUID().toString()
        val requestWithCorrectGroup = { request: HttpRequestBuilder ->
            val claims = mapOf(
                "NAVident" to "ABC123",
                "oid" to userWithRoles,
            )
            request.bearerAuth(oauth.issueToken(claims = claims).serialize())
        }

        val roles = setOf(AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TEAM_MULIGHETSROMMET))

        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = roles),
            engine = createMockEngine {
                get(".*/users/$userWithoutRoles/transitiveMemberOf/microsoft.graph.group.*".toRegex()) {
                    respondJson(
                        GetMemberGroupsResponse(listOf()),
                    )
                }
                get(".*/users/$userWithRoles/transitiveMemberOf/microsoft.graph.group.*".toRegex()) {
                    respondJson(
                        GetMemberGroupsResponse(
                            roles.map { MsGraphGroup(id = it.adGruppeId, displayName = it.rolle.name) },
                        ),
                    )
                }
            },
        )
        withTestApplication(config, additionalConfiguration = Application::configureTestAuthentationRoutes) {
            forAll(
                row(requestWithoutBearerToken, HttpStatusCode.Unauthorized),
                row(requestWithWrongAudience, HttpStatusCode.Unauthorized),
                row(requestWithWrongIssuer, HttpStatusCode.Unauthorized),
                row(requestWithoutNAVident, HttpStatusCode.Unauthorized),
                row(requestWithoutOid, HttpStatusCode.Unauthorized),
                row(requestWithWrongGroup, HttpStatusCode.Unauthorized),
                row(requestWithCorrectGroup, HttpStatusCode.OK),
            ) { buildRequest, responseStatusCode ->
                val response = client.get("/NAV_ANSATT_WITH_ROLES") { buildRequest(this) }

                response.status shouldBe responseStatusCode
            }
        }
    }

    test("verify provider AZURE_AD_DEFAULT_APP") {
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

        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = setOf()),
        )
        withTestApplication(config, additionalConfiguration = Application::configureTestAuthentationRoutes) {
            forAll(
                row(requestWithoutBearerToken, HttpStatusCode.Unauthorized),
                row(requestWithWrongAudience, HttpStatusCode.Unauthorized),
                row(requestWithWrongIssuer, HttpStatusCode.Unauthorized),
                row(requestWithoutClaimAccessAsApplication, HttpStatusCode.Unauthorized),
                row(requestWithClaimAccessAsApplication, HttpStatusCode.OK),
            ) { buildRequest, responseStatusCode ->
                val response = client.get("/AZURE_AD_DEFAULT_APP") { buildRequest(this) }

                response.status shouldBe responseStatusCode
            }
        }
    }

    test("verify provider AZURE_AD_TILTAKSGJENNOMFORING_APP") {
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
            val claims = mapOf("roles" to listOf(AppRoles.ACCESS_AS_APPLICATION, AppRoles.READ_TILTAKSGJENNOMFORING))
            request.bearerAuth(oauth.issueToken(claims = claims).serialize())
        }

        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = setOf()),
        )
        withTestApplication(config, additionalConfiguration = Application::configureTestAuthentationRoutes) {
            forAll(
                row(requestWithoutBearerToken, HttpStatusCode.Unauthorized),
                row(requestWithWrongAudience, HttpStatusCode.Unauthorized),
                row(requestWithWrongIssuer, HttpStatusCode.Unauthorized),
                row(requestWithoutClaimAccessAsApplication, HttpStatusCode.Unauthorized),
                row(requestWithClaimAccessAsApplication, HttpStatusCode.Unauthorized),
                row(requestWithClaimsReadTiltaksgjennomforing, HttpStatusCode.OK),
            ) { buildRequest, responseStatusCode ->
                val response = client.get("/AZURE_AD_TILTAKSGJENNOMFORING_APP") { buildRequest(this) }

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
        val requestWithPidWithoutRettighet = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken(claims = mapOf(Pair("pid", "21830348931"))).serialize())
        }
        val requestWithPidWithRettighet = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken(claims = mapOf(Pair("pid", personMedRettighet))).serialize())
        }

        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = setOf()),
        )
        withTestApplication(config, additionalConfiguration = Application::configureTestAuthentationRoutes) {
            forAll(
                row(requestWithoutBearerToken, HttpStatusCode.Unauthorized),
                row(requestWithWrongAudience, HttpStatusCode.Unauthorized),
                row(requestWithWrongIssuer, HttpStatusCode.Unauthorized),
                row(requestWithoutPid, HttpStatusCode.Unauthorized),
                row(requestWithPidWithoutRettighet, HttpStatusCode.Unauthorized),
                row(requestWithPidWithRettighet, HttpStatusCode.OK),
            ) { buildRequest, responseStatusCode ->
                val response = client.get("/TOKEN_X_ARRANGOR_FLATE") { buildRequest(this) }

                response.status shouldBe responseStatusCode
            }
        }
    }
})
