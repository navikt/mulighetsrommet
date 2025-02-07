package no.nav.mulighetsrommet.api.utbetaling

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import no.nav.mulighetsrommet.api.*
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattRolle
import no.nav.mulighetsrommet.api.utbetaling.OpprettManuellUtbetalingRequest.Periode
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.time.LocalDate
import java.util.*

class UtbetalingRoutesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        gjennomforinger = listOf(GjennomforingFixtures.Oppfolging1),
    )

    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
        domain.initialize(database.db)
    }

    afterSpec {
        oauth.shutdown()
    }

    val generellRolle = AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL)
    val avtaleSkrivRolle = AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), NavAnsattRolle.AVTALER_SKRIV)
    val gjennomforingerSkrivRolle =
        AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV)

    fun appConfig(
        engine: HttpClientEngine = CIO.create(),
    ) = createTestApplicationConfig().copy(
        database = databaseConfig,
        auth = createAuthConfig(oauth, roles = listOf(generellRolle, avtaleSkrivRolle, gjennomforingerSkrivRolle)),
        engine = engine,
    )

    test("Skal returnere 200 ok for korrekt request for manuell utbetaling (frimodell)") {
        withTestApplication(appConfig()) {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }

            val id = UUID.randomUUID()
            val response = client.post("/api/v1/intern/utbetaling/$id/opprett-utbetaling") {
                val claims = mapOf(
                    "NAVident" to "ABC123",
                    "groups" to listOf(
                        avtaleSkrivRolle.adGruppeId,
                        generellRolle.adGruppeId,
                        gjennomforingerSkrivRolle.adGruppeId
                    ),
                )
                bearerAuth(
                    oauth.issueToken(claims = claims).serialize(),
                )
                contentType(ContentType.Application.Json)
                setBody(
                    OpprettManuellUtbetalingRequest(
                        gjennomforingId = GjennomforingFixtures.Oppfolging1.id,
                        periode = Periode(
                            start = LocalDate.now(),
                            slutt = LocalDate.now().plusDays(1),
                        ),
                        beskrivelse = "Bla bla bla bla bla",
                        kontonummer = Kontonummer(value = "12345678910"),
                        kidNummer = null,
                        belop = 150
                    )
                )
            }
            println(response.bodyAsText())
            response.status shouldBe HttpStatusCode.OK
        }
    }
})
