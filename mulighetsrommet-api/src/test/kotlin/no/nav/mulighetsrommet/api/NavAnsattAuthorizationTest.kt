package no.nav.mulighetsrommet.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.clients.msgraph.AdGruppe
import no.nav.mulighetsrommet.api.clients.msgraph.mockMsGraphGetMemberGroups
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.authenticate
import no.nav.mulighetsrommet.ktor.createMockEngine
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
        oid: UUID,
    ): (HttpRequestBuilder) -> Unit = { request: HttpRequestBuilder ->
        val claims = mapOf(
            "NAVident" to "ABC123",
            "oid" to oid.toString(),
            "sid" to UUID.randomUUID().toString(),
        )
        request.bearerAuth(oauth.issueToken(claims = claims).serialize())
    }

    val teamMulighetsrommet = AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TEAM_MULIGHETSROMMET)
    val generell = AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TILTAKADMINISTRASJON_GENERELL)
    val saksbehandlerOkonomi = AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), Rolle.SAKSBEHANDLER_OKONOMI)

    test("user needs the correct role to access route for authorized role") {
        val userWithoutRoles = UUID.randomUUID()
        val userWithWrongRole = UUID.randomUUID()
        val userWithRequiredRole = UUID.randomUUID()

        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = setOf(teamMulighetsrommet, generell)),
            engine = createMockEngine {
                mockMsGraphGetMemberGroups(userWithoutRoles) { listOf() }
                mockMsGraphGetMemberGroups(userWithWrongRole) { listOf(generell.toAdGruppe()) }
                mockMsGraphGetMemberGroups(userWithRequiredRole) { listOf(teamMulighetsrommet.toAdGruppe()) }
            },
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
                row(userWithoutRoles, HttpStatusCode.Unauthorized),
                row(userWithWrongRole, HttpStatusCode.Forbidden),
                row(userWithRequiredRole, HttpStatusCode.OK),
            ) { userId, responseStatusCode ->
                val request = createRequestWithUserClaims(userId)
                client.get("/route", request).status shouldBe responseStatusCode
            }
        }
    }

    test("user needs all roles to access route with multiple authorized roles") {
        val userWithoutEnoughRoles = UUID.randomUUID()
        val userWithRequiredRoles = UUID.randomUUID()

        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = setOf(teamMulighetsrommet, generell)),
            engine = createMockEngine {
                mockMsGraphGetMemberGroups(userWithoutEnoughRoles) { listOf(teamMulighetsrommet.toAdGruppe()) }
                mockMsGraphGetMemberGroups(userWithRequiredRoles) {
                    listOf(
                        teamMulighetsrommet.toAdGruppe(),
                        generell.toAdGruppe(),
                    )
                }
            },
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
                row(userWithoutEnoughRoles, HttpStatusCode.Forbidden),
                row(userWithRequiredRoles, HttpStatusCode.OK),
            ) { userId, responseStatusCode ->
                val request = createRequestWithUserClaims(userId)
                client.get("/multiple", request).status shouldBe responseStatusCode
            }
        }
    }

    test("user needs all roles to access route with nested authorization blocks") {
        val userWithOneRole = UUID.randomUUID()
        val userWithTwoRoles = UUID.randomUUID()
        val userWithAllRoles = UUID.randomUUID()

        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = setOf(teamMulighetsrommet, generell, saksbehandlerOkonomi)),
            engine = createMockEngine {
                mockMsGraphGetMemberGroups(userWithOneRole) { listOf(teamMulighetsrommet.toAdGruppe()) }
                mockMsGraphGetMemberGroups(userWithTwoRoles) {
                    listOf(
                        teamMulighetsrommet.toAdGruppe(),
                        generell.toAdGruppe(),
                    )
                }
                mockMsGraphGetMemberGroups(userWithAllRoles) {
                    listOf(
                        teamMulighetsrommet.toAdGruppe(),
                        generell.toAdGruppe(),
                        saksbehandlerOkonomi.toAdGruppe(),
                    )
                }
            },
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
                row(userWithOneRole, HttpStatusCode.Forbidden),
                row(userWithTwoRoles, HttpStatusCode.Forbidden),
                row(userWithAllRoles, HttpStatusCode.OK),
            ) { userId, responseStatusCode ->
                val request = createRequestWithUserClaims(userId)
                client.get("/very/nested/route", request).status shouldBe responseStatusCode
            }
        }
    }
})

private fun AdGruppeNavAnsattRolleMapping.toAdGruppe(): AdGruppe = AdGruppe(adGruppeId, rolle.name)
