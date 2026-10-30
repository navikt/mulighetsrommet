package no.nav.mulighetsrommet.api.tilsagn.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.mulighetsrommet.admin.navansatt.EntraGroupNavAnsattRolleMapping
import no.nav.mulighetsrommet.api.ApplicationConfigTest
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.createAuthConfig
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.domain.testing.fixture.AvtaleFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.DeltakerFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures
import no.nav.mulighetsrommet.api.fixtures.setTilsagnStatus
import no.nav.mulighetsrommet.api.getAnsattClaims
import no.nav.mulighetsrommet.api.mockAmtDeltakerPersonalia
import no.nav.mulighetsrommet.api.mockPdlEmptyResult
import no.nav.mulighetsrommet.api.mockTilgangsmaskinenForbidden
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.withTestApplication
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.UUID

class TilsagnRoutesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }

    val generellRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TILTAKADMINISTRASJON_GENERELL)
    val okonomiLesRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.OKONOMI_LES)
    val saksbehandlerRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS)

    val ansatt = NavAnsattFixture.DonaldDuck

    val deltaker = DeltakerFixtures.createDeltaker(
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
        gjennomforinger = listOf(GjennomforingFixtures.AFT1, GjennomforingFixtures.EnkelAmo),
        tilsagn = listOf(tilsagn),
        deltakere = listOf(deltaker),
    ) {
        setTilsagnStatus(tilsagn, TilsagnStatus.GODKJENT)
    }

    beforeEach {
        domain.initialize(database.api)
    }

    afterEach {
        database.truncateAll()
    }

    fun appConfig(avvistGrunn: String) = ApplicationConfigTest.copy(
        auth = createAuthConfig(oauth, roles = setOf(okonomiLesRolle, generellRolle, saksbehandlerRolle)),
        engine = createMockEngine {
            mockPdlEmptyResult()
            mockAmtDeltakerPersonalia(gradering = PdlGradering.UGRADERT)
            mockTilgangsmaskinenForbidden(avvistGrunn)
        },
    )

    test("standardverdier avvises for manuelt opplæringstilskudd") {
        withTestApplication(appConfig("AVVIST_SKJERMING")) {
            val claims = getAnsattClaims(ansatt, setOf(generellRolle, saksbehandlerRolle))
            val request = TilsagnFixtures.TilsagnRequest1.copy(
                gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                type = TilsagnType.TILSKUDD_TIL_OPPLAERING,
            )
            val response = client.post("/api/tiltaksadministrasjon/tilsagn/defaults") {
                bearerAuth(oauth.issueToken(claims = claims).serialize())
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            response.status shouldBe HttpStatusCode.BadRequest
            response.body<ProblemDetail>().detail shouldBe "Tilskudd til opplæring kan ikke opprettes manuelt"
        }
    }

    test("opplæringstilskudd kan ikke opprettes manuelt") {
        withTestApplication(appConfig("AVVIST_SKJERMING")) {
            val claims = getAnsattClaims(ansatt, setOf(generellRolle, saksbehandlerRolle))
            val request = TilsagnFixtures.TilsagnRequest1.copy(
                id = UUID.randomUUID(),
                gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                type = TilsagnType.TILSKUDD_TIL_OPPLAERING,
            )
            val response = client.put("/api/tiltaksadministrasjon/tilsagn") {
                bearerAuth(oauth.issueToken(claims = claims).serialize())
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            response.status shouldBe HttpStatusCode.BadRequest
            response.body<ValidationError>().errors.first().detail shouldBe "Tilskudd til opplæring kan ikke opprettes manuelt"
        }
    }

    test("personalia blir skjermet i TilsagnDeltakerDto når man ikke har tilgang") {
        withTestApplication(appConfig("AVVIST_SKJERMING")) {
            val navAnsattClaims = getAnsattClaims(ansatt, setOf(okonomiLesRolle, generellRolle))

            val response = client.get("/api/tiltaksadministrasjon/tilsagn/${tilsagn.id}") {
                bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
            }

            response.status shouldBe HttpStatusCode.OK
            val body = response.body<TilsagnDetaljerDto>()
            body.deltakere.first { it.deltakerId == deltaker.id } should {
                it.norskIdent.shouldBeNull()
                it.navn shouldBe "Skjermet"
                it.innholdAnnet.shouldNotBeNull()
            }
        }
    }

    test("personalia viser 'Adressebeskyttet' i TilsagnDeltakerDto ved AVVIST_FORTROLIG_ADRESSE") {
        withTestApplication(appConfig("AVVIST_FORTROLIG_ADRESSE")) {
            val navAnsattClaims = getAnsattClaims(ansatt, setOf(okonomiLesRolle, generellRolle))

            val response = client.get("/api/tiltaksadministrasjon/tilsagn/${tilsagn.id}") {
                bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
            }

            response.status shouldBe HttpStatusCode.OK

            val body = response.body<TilsagnDetaljerDto>()
            body.deltakere.first { it.deltakerId == deltaker.id } should {
                it.norskIdent.shouldBeNull()
                it.navn shouldBe "Adressebeskyttet"
            }
        }
    }
})
