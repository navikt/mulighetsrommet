package no.nav.tiltak.historikk

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.tiltak.historikk.plugins.AuthProvider
import no.nav.tiltak.historikk.plugins.authenticate
import java.util.UUID

class AuthenticationTest : FunSpec({
    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }

    context("verify provider TEAM_MULIGHETSROMMET") {
        val requestWithoutBearerToken = { _: HttpRequestBuilder -> }
        val requestWithWrongAudience = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken(audience = "skatteetaten").serialize())
        }
        val requestWithWrongIssuer = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken(issuerId = "skatteetaten").serialize())
        }
        val requestWithRoles = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken(claims = mapOf(Pair("roles", emptyList<String>()))).serialize())
        }
        val requestWithGroups = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken(claims = mapOf(Pair("groups", listOf(UUID.randomUUID(), UUID.randomUUID())))).serialize())
        }

        val requestWithTeamMulighetsrommetClaim = { request: HttpRequestBuilder ->
            request.bearerAuth(oauth.issueToken(claims = mapOf(Pair("groups", listOf(teamMulighetsrommetTestEntraAdGroupId)))).serialize())
        }

        val testRoute = "MAAM_ROUTE"

        test("no bearer -> unauthorized") {
            withTestApplication<Unit>(oauth = oauth, additionalConfiguration = {
                routing {
                    authenticate(AuthProvider.TEAM_MULIGHETSROMMET) {
                        get(testRoute) { call.respond(HttpStatusCode.OK) }
                    }
                }
            }) {

                val resp = client.get(testRoute) {
                    requestWithoutBearerToken(this)
                }
                resp.bodyAsText()
                resp.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("wrong audience -> unauthorized") {
            withTestApplication<Unit>(oauth = oauth, additionalConfiguration = {
                routing {
                    authenticate(AuthProvider.TEAM_MULIGHETSROMMET) {
                        get(testRoute) { call.respond(HttpStatusCode.OK) }
                    }
                }
            }) {

                val resp = client.get(testRoute) {
                    requestWithWrongAudience(this)
                }
                resp.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("wrong issuer -> unauthorized") {
            withTestApplication<Unit>(oauth = oauth, additionalConfiguration = {
                routing {
                    authenticate(AuthProvider.TEAM_MULIGHETSROMMET) {
                        get(testRoute) { call.respond(HttpStatusCode.OK) }
                    }
                }
            }) {

                val resp = client.get(testRoute) {
                    requestWithWrongIssuer(this)
                }
                resp.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("only roles claims -> unauthorized") {
            withTestApplication<Unit>(oauth = oauth, additionalConfiguration = {
                routing {
                    authenticate(AuthProvider.TEAM_MULIGHETSROMMET) {
                        get(testRoute) { call.respond(HttpStatusCode.OK) }
                    }
                }
            }) {

                val resp = client.get(testRoute) {
                    requestWithRoles(this)
                }
                resp.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("with wrong groups claims -> unauthorized") {
            withTestApplication<Unit>(oauth = oauth, additionalConfiguration = {
                routing {
                    authenticate(AuthProvider.TEAM_MULIGHETSROMMET) {
                        get(testRoute) { call.respond(HttpStatusCode.OK) }
                    }
                }
            }) {

                val resp = client.get(testRoute) {
                    requestWithGroups(this)
                }
                resp.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("with team mulighetsrommet group claim -> ok") {
            withTestApplication<Unit>(oauth = oauth, additionalConfiguration = {
                routing {
                    authenticate(AuthProvider.TEAM_MULIGHETSROMMET) {
                        get(testRoute) { call.respond(HttpStatusCode.OK) }
                    }
                }
            }) {

                val resp = client.get(testRoute) {
                    requestWithTeamMulighetsrommetClaim(this)
                }
                resp.status shouldBe HttpStatusCode.OK
            }
        }
    }
})
