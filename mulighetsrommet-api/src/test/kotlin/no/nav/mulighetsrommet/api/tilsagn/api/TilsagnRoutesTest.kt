package no.nav.mulighetsrommet.api.tilsagn.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.EntraGroupNavAnsattRolleMapping
import no.nav.mulighetsrommet.api.clients.tilgangsmaskin.TilgangsmaskinRequest
import no.nav.mulighetsrommet.api.clients.tilgangsmaskin.TilgangsmaskinResponse
import no.nav.mulighetsrommet.api.createAuthConfig
import no.nav.mulighetsrommet.api.createTestApplicationConfig
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures
import no.nav.mulighetsrommet.api.fixtures.setTilsagnStatus
import no.nav.mulighetsrommet.api.getAnsattClaims
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.withTestApplication
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.UUID

class TilsagnRoutesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }

    val generellRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TILTAKADMINISTRASJON_GENERELL)
    val okonomiLesRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.OKONOMI_LES)

    val ansatt = NavAnsattFixture.DonaldDuck

    val deltaker = DeltakerFixtures.createDeltakerDbo(
        gjennomforingId = GjennomforingFixtures.AFT1.id,
    )
    val tilsagn = TilsagnFixtures.Tilsagn1.copy(
        deltakere = listOf(
            TilsagnDbo.Deltaker(
                deltakerId = deltaker.id,
                innholdAnnet = "Viktig innhold",
            ),
        ),
    )

    val domain = MulighetsrommetTestDomain(
        ansatte = listOf(ansatt, NavAnsattFixture.FetterAnton),
        avtaler = listOf(AvtaleFixtures.AFT),
        gjennomforinger = listOf(GjennomforingFixtures.AFT1),
        tilsagn = listOf(tilsagn),
        deltakere = listOf(deltaker),
    ) {
        setTilsagnStatus(tilsagn, TilsagnStatus.GODKJENT)
    }

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    fun appConfig() = createTestApplicationConfig().copy(
        auth = createAuthConfig(oauth, roles = setOf(okonomiLesRolle, generellRolle)),
        tilgangsmaskin = createTestApplicationConfig().tilgangsmaskin.copy(
            engine = MockEngine { request ->
                if (request.url.toString().endsWith("/api/v1/bulk/obo")) {
                    val jsonString = (request.body as TextContent).text
                    val requests = Json.decodeFromString<List<TilgangsmaskinRequest>>(jsonString)
                    val payload = TilgangsmaskinResponse(
                        requests.map { req ->
                            TilgangsmaskinResponse.Resultat(
                                brukerId = req.brukerId,
                                status = 403,
                            )
                        },
                    )
                    respond(
                        content = ByteReadChannel(Json.encodeToString(payload)),
                        status = HttpStatusCode.MultiStatus,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                } else {
                    error("Unexpected request: ${request.url}")
                }
            },
        ),
    )

    test("personalia blir skjermet i TilsagnDeltakerDto når man ikke har tilgang") {
        withTestApplication(appConfig()) {
            val navAnsattClaims = getAnsattClaims(ansatt, setOf(okonomiLesRolle, generellRolle))

            val response = client.get("/api/tiltaksadministrasjon/tilsagn/${tilsagn.id}") {
                bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
            }

            response.status shouldBe HttpStatusCode.OK
            val body = response.body<TilsagnDetaljerDto>()
            body.tilsagn.deltakere.first { it.deltakerId == deltaker.id } should {
                it.norskIdent.shouldBeNull()
                it.navn shouldBe "Skjermet"
                it.innholdAnnet.shouldBeNull()
            }
        }
    }
})
