package no.nav.mulighetsrommet.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.authenticate
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.*

class NavAnsattAuthorizationTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val oauth = MockOAuth2Server()

    val domain = MulighetsrommetTestDomain(
        ansatte = listOf(NavAnsattFixture.DonaldDuck),
        navEnheter = listOf(NavEnhetFixtures.Innlandet),
        arrangorer = listOf(),
        avtaler = listOf(),
    )

    beforeSpec {
        oauth.start()
        domain.initialize(database.db)
    }

    afterSpec {
        oauth.shutdown()
    }

    val teamMulighetsrommet = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TEAM_MULIGHETSROMMET)
    val generell = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TILTAKADMINISTRASJON_GENERELL)
    val saksbehandlerOkonomi = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.SAKSBEHANDLER_OKONOMI)
    val attestantUtbetaling = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.ATTESTANT_UTBETALING)

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
                row(setOf(), HttpStatusCode.Unauthorized),
                row(setOf(generell), HttpStatusCode.Forbidden),
                row(setOf(teamMulighetsrommet), HttpStatusCode.OK),
            ) { roles, responseStatusCode ->
                val request = oauth.createRequestWithAnsattClaims(NavAnsattFixture.DonaldDuck, roles)
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
                    authorize(allOf = setOf(Rolle.TILTAKADMINISTRASJON_GENERELL, Rolle.TEAM_MULIGHETSROMMET)) {
                        get("multiple") { call.respond(HttpStatusCode.OK) }
                    }
                }
            }
        }) {
            forAll(
                row(setOf(teamMulighetsrommet), HttpStatusCode.Forbidden),
                row(setOf(teamMulighetsrommet, generell), HttpStatusCode.OK),
            ) { roles, responseStatusCode ->
                val request = oauth.createRequestWithAnsattClaims(NavAnsattFixture.DonaldDuck, roles)
                client.get("/multiple", request).status shouldBe responseStatusCode
            }
        }
    }

    test("user needs at least one of roles to access route with multiple authorized roles") {
        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = setOf(teamMulighetsrommet, generell, saksbehandlerOkonomi)),
        )

        withTestApplication(config, additionalConfiguration = {
            routing {
                authenticate(AuthProvider.NAV_ANSATT_WITH_ROLES) {
                    authorize(anyOf = setOf(Rolle.TILTAKADMINISTRASJON_GENERELL, Rolle.TEAM_MULIGHETSROMMET)) {
                        get("multiple") { call.respond(HttpStatusCode.OK) }
                    }
                }
            }
        }) {
            forAll(
                row(setOf(teamMulighetsrommet), HttpStatusCode.OK),
                row(setOf(teamMulighetsrommet, generell), HttpStatusCode.OK),
                row(setOf(generell), HttpStatusCode.OK),
                row(setOf(saksbehandlerOkonomi), HttpStatusCode.Forbidden),
            ) { roles, responseStatusCode ->
                val request = oauth.createRequestWithAnsattClaims(NavAnsattFixture.DonaldDuck, roles)
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
                row(setOf(teamMulighetsrommet), HttpStatusCode.Forbidden),
                row(setOf(teamMulighetsrommet, generell), HttpStatusCode.Forbidden),
                row(setOf(teamMulighetsrommet, generell, saksbehandlerOkonomi), HttpStatusCode.OK),
            ) { roles, responseStatusCode ->
                val request = oauth.createRequestWithAnsattClaims(NavAnsattFixture.DonaldDuck, roles)
                client.get("/very/nested/route", request).status shouldBe responseStatusCode
            }
        }
    }

    test("user needs required roles to access route with nested authorization blocks") {
        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = setOf(teamMulighetsrommet, generell, saksbehandlerOkonomi, attestantUtbetaling)),
        )

        withTestApplication(config, additionalConfiguration = {
            routing {
                authenticate(AuthProvider.NAV_ANSATT_WITH_ROLES) {
                    authorize(allOf = setOf(Rolle.TEAM_MULIGHETSROMMET, Rolle.TILTAKADMINISTRASJON_GENERELL)) {
                        authorize(anyOf = setOf(Rolle.SAKSBEHANDLER_OKONOMI, Rolle.ATTESTANT_UTBETALING)) {
                            get("route") { call.respond(HttpStatusCode.OK) }
                        }
                    }
                }
            }
        }) {
            forAll(
                row(setOf(teamMulighetsrommet), HttpStatusCode.Forbidden),
                row(setOf(teamMulighetsrommet, generell), HttpStatusCode.Forbidden),
                row(setOf(teamMulighetsrommet, generell, saksbehandlerOkonomi), HttpStatusCode.OK),
                row(setOf(teamMulighetsrommet, generell, attestantUtbetaling), HttpStatusCode.OK),
                row(setOf(attestantUtbetaling, generell), HttpStatusCode.Forbidden),
            ) { roles, responseStatusCode ->
                val request = oauth.createRequestWithAnsattClaims(NavAnsattFixture.DonaldDuck, roles)
                client.get("/route", request).status shouldBe responseStatusCode
            }
        }
    }
})
