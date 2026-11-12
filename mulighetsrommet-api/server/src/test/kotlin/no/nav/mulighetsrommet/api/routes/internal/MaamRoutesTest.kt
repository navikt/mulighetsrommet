package no.nav.mulighetsrommet.api.routes.internal

import arrow.core.left
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.mulighetsrommet.admin.navansatt.EntraGroupNavAnsattRolleMapping
import no.nav.mulighetsrommet.api.ApplicationConfigTest
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateUtbetaling
import no.nav.mulighetsrommet.api.arrangorflate.service.ArrangorflateUtbetalingService
import no.nav.mulighetsrommet.api.brukerutbetaling.BrukerUtbetalingService
import no.nav.mulighetsrommet.api.createAuthConfig
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.getAnsattClaims
import no.nav.mulighetsrommet.api.withTestApplication
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.maam.dto.DeleteKafkaRecordRequest
import no.nav.mulighetsrommet.maam.dto.DeleteScheduledTaskRequest
import no.nav.mulighetsrommet.maam.dto.ExecutedTaskResponse
import no.nav.mulighetsrommet.validation.FieldError
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.koin.ktor.ext.getKoin
import java.util.UUID

class MaamRoutesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val ansatt = NavAnsattFixture.DonaldDuck
    val domain = MulighetsrommetTestDomain(
        ansatte = listOf(ansatt),
    )

    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
        domain.initialize(database.api)
    }

    afterSpec {
        oauth.shutdown()
    }

    val generellRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TILTAKADMINISTRASJON_GENERELL)
    val teamMulighetsrommetRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TEAM_MULIGHETSROMMET)

    fun appConfig() = ApplicationConfigTest.copy(
        auth = createAuthConfig(
            oauth,
            roles = setOf(generellRolle, teamMulighetsrommetRolle),
        ),
    )

    test("sletting krever rollen TEAM_MULIGHETSROMMET") {
        withTestApplication(appConfig()) {
            forAll(
                row(generellRolle, HttpStatusCode.Forbidden),
                row(teamMulighetsrommetRolle, HttpStatusCode.Conflict),
            ) { rolle, status ->
                val token = oauth.issueToken(claims = getAnsattClaims(ansatt, setOf(rolle))).serialize()
                client.delete("/api/intern/maam/kafka/failed-records") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(DeleteKafkaRecordRequest(id = 0, topic = "missing"))
                }.status shouldBe status

                client.delete("/api/intern/maam/tasks/scheduled") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(DeleteScheduledTaskRequest(taskName = "missing", taskInstance = "missing"))
                }.status shouldBe status
            }
        }
    }

    test("maam er bare tilgjengelig med når man har rollen TEAM_MULIGHETSROMMET") {
        withTestApplication(appConfig()) {
            forAll(
                row(generellRolle, HttpStatusCode.Forbidden),
                row(teamMulighetsrommetRolle, HttpStatusCode.OK),
            ) { rolle, status ->
                val claims = getAnsattClaims(ansatt, setOf(rolle))

                val response = client.get("/api/intern/maam/kafka/consumers") {
                    bearerAuth(oauth.issueToken(claims = claims).serialize())
                    contentType(ContentType.Application.Json)
                }

                response.status shouldBe status
            }
        }
    }

    test("start av task krever rollen TEAM_MULIGHETSROMMET") {
        withTestApplication(appConfig()) {
            forAll(
                row(generellRolle, HttpStatusCode.Forbidden),
                row(teamMulighetsrommetRolle, HttpStatusCode.Accepted),
            ) { rolle, status ->
                val token = oauth.issueToken(claims = getAnsattClaims(ansatt, setOf(rolle))).serialize()
                client.post("/api/intern/maam/tasks/sync-gjennomforing-avtale-fts") {
                    bearerAuth(token)
                }.status shouldBe status
            }
        }
    }

    test("Hel Ved-testutbetaling som ikke er tillatt returnerer 403 med forklaring") {
        val service = mockk<BrukerUtbetalingService>()
        every { service.produceTilskuddUtbetalingTest() } returns null

        withTestApplication(appConfig(), additionalConfiguration = {
            getKoin().declare(service)
        }) {
            val token = oauth.issueToken(
                claims = getAnsattClaims(ansatt, setOf(teamMulighetsrommetRolle)),
            ).serialize()

            val response = client.post("/api/intern/maam/tasks/hel-ved-utbetaling") {
                bearerAuth(token)
            }

            response.status shouldBe HttpStatusCode.Forbidden
            response.body<ExecutedTaskResponse>().message shouldBe "Hel Ved-testutbetaling er ikke tillatt i produksjon"
        }
    }

    test("regenerering med valideringsfeil returnerer 400 med forklaring") {
        val service = mockk<ArrangorflateUtbetalingService>()
        val utbetaling = mockk<ArrangorflateUtbetaling>()
        val utbetalingId = UUID.randomUUID()
        every { service.getOrError(utbetalingId) } returns utbetaling
        coEvery { service.regenererUtbetaling(utbetaling) } returns
            listOf(FieldError.of("Utbetalingen kan ikke regenereres")).left()

        withTestApplication(appConfig(), additionalConfiguration = {
            getKoin().declare(service)
        }) {
            val token = oauth.issueToken(
                claims = getAnsattClaims(ansatt, setOf(teamMulighetsrommetRolle)),
            ).serialize()

            val response = client.post("/api/intern/maam/tasks/regenerer-utbetaling") {
                bearerAuth(token)
                setBody(RegenererUtbetalingRequest(utbetalingId))
            }

            response.status shouldBe HttpStatusCode.BadRequest
            response.body<ExecutedTaskResponse>().message shouldBe
                "Utbetalingen kan ikke regenereres: Utbetalingen kan ikke regenereres"
        }
    }
})
