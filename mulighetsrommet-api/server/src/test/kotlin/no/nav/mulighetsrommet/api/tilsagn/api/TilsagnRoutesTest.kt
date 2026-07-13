package no.nav.mulighetsrommet.api.tilsagn.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import no.nav.mulighetsrommet.api.ApplicationConfigTest
import no.nav.mulighetsrommet.api.EntraGroupNavAnsattRolleMapping
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.createAuthConfig
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures
import no.nav.mulighetsrommet.api.fixtures.setTilsagnStatus
import no.nav.mulighetsrommet.api.getAnsattClaims
import no.nav.mulighetsrommet.api.mockAmtDeltakerPersonalia
import no.nav.mulighetsrommet.api.mockPdlEmptyResult
import no.nav.mulighetsrommet.api.mockTilgangsmaskinenForbidden
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.withTestApplication
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.ktor.createMockEngine
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

    fun appConfig(avvistGrunn: String) = ApplicationConfigTest.copy(
        auth = createAuthConfig(oauth, roles = setOf(okonomiLesRolle, generellRolle)),
        engine = createMockEngine {
            mockPdlEmptyResult()
            mockAmtDeltakerPersonalia(gradering = PdlGradering.UGRADERT)
            mockTilgangsmaskinenForbidden(avvistGrunn)
        },
    )

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
