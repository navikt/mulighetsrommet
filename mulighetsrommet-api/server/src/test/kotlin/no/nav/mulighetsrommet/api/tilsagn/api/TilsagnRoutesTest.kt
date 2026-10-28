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
import no.nav.mulighetsrommet.admin.navansatt.EntraGroupNavAnsattRolleMapping
import no.nav.mulighetsrommet.api.ApplicationConfigTest
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.createAuthConfig
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.domain.testing.fixture.AvtaleFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.DeltakerFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavEnhetFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
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
import no.nav.mulighetsrommet.model.DataElement
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
    val annenGjennomforing = GjennomforingFixtures.AFT1.copy(id = UUID.randomUUID())

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
        gjennomforinger = listOf(GjennomforingFixtures.AFT1, annenGjennomforing),
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
        auth = createAuthConfig(oauth, roles = setOf(okonomiLesRolle, generellRolle)),
        engine = createMockEngine {
            mockPdlEmptyResult()
            mockAmtDeltakerPersonalia(gradering = PdlGradering.UGRADERT)
            mockTilgangsmaskinenForbidden(avvistGrunn)
        },
    )

    test("filtrer tilsagn på kostnadssteder sammen med eksisterende filtre") {
        database.api.session {
            queries.tilsagn.upsert(
                TilsagnFixtures.Tilsagn2.copy(kostnadssted = NavEnhetFixtures.Gjovik.enhetsnummer),
            )
        }

        withTestApplication(appConfig("AVVIST_SKJERMING")) {
            val navAnsattClaims = getAnsattClaims(ansatt, setOf(okonomiLesRolle, generellRolle))
            val token = oauth.issueToken(claims = navAnsattClaims).serialize()

            val cases = listOf(
                "" to listOf("A-2025/1-1", "A-2025/1-2"),
                "kostnadssteder=0400" to listOf("A-2025/1-1"),
                "kostnadssteder=0502" to listOf("A-2025/1-2"),
                "kostnadssteder=0400&kostnadssteder=0502" to listOf("A-2025/1-1", "A-2025/1-2"),
                "kostnadssteder=0400&kostnadssteder=0400" to listOf("A-2025/1-1"),
                "kostnadssteder=9999" to emptyList(),
                "kostnadssteder=0400&statuser=GODKJENT&gjennomforingId=${GjennomforingFixtures.AFT1.id}" to listOf("A-2025/1-1"),
                "kostnadssteder=0502&statuser=GODKJENT" to emptyList(),
                "kostnadssteder=0400&gjennomforingId=${UUID.randomUUID()}" to emptyList(),
            )

            cases.forEach { (query, expected) ->
                val response = client.get("/api/tiltaksadministrasjon/tilsagn?$query") {
                    bearerAuth(token)
                }
                response.status shouldBe HttpStatusCode.OK
                val body = response.body<PaginatedDataDrivenTableResponse>()
                body.pagination.totalCount shouldBe expected.size
                body.table.rows.map { it.cells["bestillingsnummer"] }.toSet() shouldBe expected.map { DataElement.text(it) }.toSet()
            }

            val invalidResponse = client.get("/api/tiltaksadministrasjon/tilsagn?kostnadssteder=invalid") {
                bearerAuth(token)
            }
            invalidResponse.status shouldBe HttpStatusCode.BadRequest
        }
    }

    test("kostnadssteder inkluderer alle tilsagn for gjennomføringen uavhengig av filtre og paginering") {
        database.api.session {
            queries.enhet.save(NavEnhetFixtures.Oslo)
            queries.tilsagn.upsert(
                TilsagnFixtures.Tilsagn2.copy(kostnadssted = NavEnhetFixtures.Gjovik.enhetsnummer),
            )
            queries.tilsagn.upsert(TilsagnFixtures.createTilsagn(lopenummer = 3))
            queries.tilsagn.upsert(
                TilsagnFixtures.createTilsagn(gjennomforingId = annenGjennomforing.id, lopenummer = 4)
                    .copy(kostnadssted = NavEnhetFixtures.Oslo.enhetsnummer),
            )
        }

        withTestApplication(appConfig("AVVIST_SKJERMING")) {
            val navAnsattClaims = getAnsattClaims(ansatt, setOf(okonomiLesRolle, generellRolle))
            val token = oauth.issueToken(claims = navAnsattClaims).serialize()
            val forventedeKostnadssteder = listOf(
                NavEnhetFixtures.Innlandet.enhetsnummer,
                NavEnhetFixtures.Gjovik.enhetsnummer,
            )

            val cases = listOf(
                "size=1&page=1" to 1,
                "size=1&page=2" to 1,
                "statuser=GODKJENT" to 1,
                "kostnadssteder=0502" to 1,
                "kostnadssteder=9999" to 0,
            )
            cases.forEach { (query, expectedRows) ->
                val response = client.get(
                    "/api/tiltaksadministrasjon/tilsagn?gjennomforingId=${GjennomforingFixtures.AFT1.id}&$query",
                ) {
                    bearerAuth(token)
                }
                response.status shouldBe HttpStatusCode.OK
                val body = response.body<PaginatedDataDrivenTableResponse>()
                body.kostnadssteder shouldBe forventedeKostnadssteder
                body.table.rows.size shouldBe expectedRows
            }

            listOf(
                annenGjennomforing.id to listOf(NavEnhetFixtures.Oslo.enhetsnummer),
                UUID.randomUUID() to emptyList(),
            ).forEach { (gjennomforingId, expected) ->
                val response = client.get("/api/tiltaksadministrasjon/tilsagn?gjennomforingId=$gjennomforingId") {
                    bearerAuth(token)
                }
                response.status shouldBe HttpStatusCode.OK
                response.body<PaginatedDataDrivenTableResponse>().kostnadssteder shouldBe expected
            }
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
