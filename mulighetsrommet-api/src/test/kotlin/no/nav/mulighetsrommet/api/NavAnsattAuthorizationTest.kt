package no.nav.mulighetsrommet.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.authenticate
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.*

class NavAnsattAuthorizationTest : FunSpec({
    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }

    fun createRequestWithUserClaims(
        roles: List<AdGruppeNavAnsattRolleMapping>,
    ): (HttpRequestBuilder) -> Unit = { request: HttpRequestBuilder ->
        val claims = mapOf(
            "NAVident" to "B123456",
            "oid" to UUID.randomUUID().toString(),
            "sid" to UUID.randomUUID().toString(),
            "groups" to roles.map { it.adGruppeId.toString() },
        )
        request.bearerAuth(oauth.issueToken(claims = claims).serialize())
    }

    val teamMulighetsrommet = AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TEAM_MULIGHETSROMMET)
    val generell = AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TILTAKADMINISTRASJON_GENERELL)
    val saksbehandlerOkonomi = AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), Rolle.SAKSBEHANDLER_OKONOMI)

    test("user needs the correct role to access route for authorized role") {
        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = setOf(teamMulighetsrommet, generell)),
        )

        withTestApplication(config, additionalConfiguration = {
            routing {
                authenticate(AuthProvider.NAV_ANSATT_WITH_ROLES) {
                    authorize(Rolle.TEAM_MULIGHETSROMMET) {
                        get("route") { call.respond(HttpStatusCode.OK) }
                    }
                }
            }
        }) {
            forAll(
                row(listOf(), HttpStatusCode.Unauthorized),
                row(listOf(generell), HttpStatusCode.Forbidden),
                row(listOf(teamMulighetsrommet), HttpStatusCode.OK),
            ) { roles, responseStatusCode ->
                val request = createRequestWithUserClaims(roles)
                client.get("/route", request).status shouldBe responseStatusCode
            }
        }
    }

    test("user needs all roles to access route with multiple authorized roles") {
        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = setOf(teamMulighetsrommet, generell)),
        )

        withTestApplication(config, additionalConfiguration = {
            routing {
                authenticate(AuthProvider.NAV_ANSATT_WITH_ROLES) {
                    authorize(Rolle.TILTAKADMINISTRASJON_GENERELL, Rolle.TEAM_MULIGHETSROMMET) {
                        get("multiple") { call.respond(HttpStatusCode.OK) }
                    }
                }
            }
        }) {
            forAll(
                row(listOf(teamMulighetsrommet), HttpStatusCode.Forbidden),
                row(listOf(teamMulighetsrommet, generell), HttpStatusCode.OK),
            ) { roles, responseStatusCode ->
                val request = createRequestWithUserClaims(roles)
                client.get("/multiple", request).status shouldBe responseStatusCode
            }
        }
    }

    test("user needs all roles to access route with nested authorization blocks") {
        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = setOf(teamMulighetsrommet, generell, saksbehandlerOkonomi)),
        )

        withTestApplication(config, additionalConfiguration = {
            routing {
                authenticate(AuthProvider.NAV_ANSATT_WITH_ROLES) {
                    authorize(Rolle.TEAM_MULIGHETSROMMET) {
                        authorize(Rolle.TILTAKADMINISTRASJON_GENERELL) {
                            route("very") {
                                route("nested") {
                                    authorize(Rolle.SAKSBEHANDLER_OKONOMI) {
                                        get("route") { call.respond(HttpStatusCode.OK) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }) {
            forAll(
                row(listOf(teamMulighetsrommet), HttpStatusCode.Forbidden),
                row(listOf(teamMulighetsrommet, generell), HttpStatusCode.Forbidden),
                row(listOf(teamMulighetsrommet, generell, saksbehandlerOkonomi), HttpStatusCode.OK),
            ) { roles, responseStatusCode ->
                val request = createRequestWithUserClaims(roles)
                client.get("/very/nested/route", request).status shouldBe responseStatusCode
            }
        }
    }
})
