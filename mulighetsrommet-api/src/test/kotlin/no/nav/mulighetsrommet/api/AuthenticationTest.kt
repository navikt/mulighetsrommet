package no.nav.mulighetsrommet.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle
import no.nav.mulighetsrommet.api.plugins.AppRoles
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.*

class AuthenticationTest : FunSpec({

    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }

    fun Application.configureTestAuthentationRoutes() {
        routing {
            authenticate(AuthProvider.AZURE_AD_NAV_IDENT.name) {
                get("AZURE_AD_NAV_IDENT") { call.respond(HttpStatusCode.OK) }
            }

            authenticate(AuthProvider.AZURE_AD_TEAM_MULIGHETSROMMET.name) {
                get("AZURE_AD_TEAM_MULIGHETSROMMET") { call.respond(HttpStatusCode.OK) }
            }

            authenticate(AuthProvider.AZURE_AD_DEFAULT_APP.name) {
                get("AZURE_AD_DEFAULT_APP") { call.respond(HttpStatusCode.OK) }
            }

            authenticate(AuthProvider.AZURE_AD_TILTAKSGJENNOMFORING_APP.name) {
                get("AZURE_AD_TILTAKSGJENNOMFORING_APP") { call.respond(HttpStatusCode.OK) }
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
            auth = createAuthConfig(oauth, roles = listOf()),
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

    test("verify provider AZURE_AD_TEAM_MULIGHETSROMMET") {
        val wrongRole = AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), NavAnsattRolle.KONTAKTPERSON)
        val correctRole = AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), NavAnsattRolle.TEAM_MULIGHETSROMMET)

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
        val requestWithoutGroup = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken(claims = mapOf(Pair("NAVident", "ABC123"))).serialize())
        }
        val requestWithWrongGroup = { request: HttpRequestBuilder ->
            val claims = mapOf(
                "NAVident" to "ABC123",
                "groups" to listOf(wrongRole.adGruppeId),
            )
            request.bearerAuth(oauth.issueToken(claims = claims).serialize())
        }
        val requestWithCorrectGroup = { request: HttpRequestBuilder ->
            val claims = mapOf(
                "NAVident" to "ABC123",
                "groups" to listOf(correctRole.adGruppeId),
            )
            request.bearerAuth(oauth.issueToken(claims = claims).serialize())
        }

        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = listOf(correctRole)),
        )
        withTestApplication(config, additionalConfiguration = Application::configureTestAuthentationRoutes) {
            forAll(
                row(requestWithoutBearerToken, HttpStatusCode.Unauthorized),
                row(requestWithWrongAudience, HttpStatusCode.Unauthorized),
                row(requestWithWrongIssuer, HttpStatusCode.Unauthorized),
                row(requestWithoutNAVident, HttpStatusCode.Unauthorized),
                row(requestWithoutGroup, HttpStatusCode.Unauthorized),
                row(requestWithWrongGroup, HttpStatusCode.Unauthorized),
                row(requestWithCorrectGroup, HttpStatusCode.OK),
            ) { buildRequest, responseStatusCode ->
                val response = client.get("/AZURE_AD_TEAM_MULIGHETSROMMET") { buildRequest(this) }

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
            auth = createAuthConfig(oauth, roles = listOf()),
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
            auth = createAuthConfig(oauth, roles = listOf()),
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
})
