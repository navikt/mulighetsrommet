package no.nav.mulighetsrommet.api.gjennomforing.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeTypeOf
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import no.nav.mulighetsrommet.admin.aarsakerbegrunnelse.AarsakerOgBegrunnelseRequest
import no.nav.mulighetsrommet.admin.gjennomforing.GjennomforingDetaljerRequest
import no.nav.mulighetsrommet.admin.gjennomforing.GjennomforingHandling
import no.nav.mulighetsrommet.admin.navansatt.EntraGroupNavAnsattRolleMapping
import no.nav.mulighetsrommet.admin.testing.GjennomforingFixtures.createGjennomforingRequest
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeService
import no.nav.mulighetsrommet.api.ApplicationConfigTest
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.clients.tilgangsmaskin.TilgangsmaskinResponse
import no.nav.mulighetsrommet.api.createAuthConfig
import no.nav.mulighetsrommet.api.domain.gjennomforing.AvbrytGjennomforingAarsak
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingAvtaleStatus
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.domain.testing.fixture.ArrangorFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.AvtaleFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.DeltakerFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavEnhetFixtures
import no.nav.mulighetsrommet.api.domain.tiltak.Prismodell
import no.nav.mulighetsrommet.api.domain.tiltak.PrismodellType
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeFeature
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.getAnsattClaims
import no.nav.mulighetsrommet.api.mockAmtDeltakerPersonalia
import no.nav.mulighetsrommet.api.mockPdlEmptyResult
import no.nav.mulighetsrommet.api.mockTilgangsmaskinenForbidden
import no.nav.mulighetsrommet.api.persistence.testing.fixture.GjennomforingDboFixtures
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.withTestApplication
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.validation.FieldError
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class GjennomforingRoutesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }

    val generellRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TILTAKADMINISTRASJON_GENERELL)
    val gjennomforingSkrivRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TILTAKSGJENNOMFORINGER_SKRIV)
    val enkeltplassSaksbehandler =
        EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS)

    fun appConfig() = ApplicationConfigTest.copy(
        auth = createAuthConfig(oauth, roles = setOf(generellRolle, gjennomforingSkrivRolle, enkeltplassSaksbehandler)),
    )

    val ansatt = NavAnsattFixture.DonaldDuck

    context("les gjennomføringer") {
        val domain = MulighetsrommetTestDomain(
            navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Oslo),
            ansatte = listOf(ansatt),
            arrangorer = listOf(),
        )

        beforeEach {
            domain.initialize(database.api)
        }

        afterEach {
            database.truncateAll()
        }

        test("401 Unauthorized for uautentisert kall") {
            withTestApplication(appConfig()) {
                val response = client.post("/api/tiltaksadministrasjon/gjennomforinger") {
                    contentType(ContentType.Application.Json)
                    setBody(GetGjennomforingerRequest())
                }
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("403 Forbidden når bruker ikke har generell tilgang") {
            withTestApplication(appConfig()) {
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(gjennomforingSkrivRolle))

                val response = client.post("/api/tiltaksadministrasjon/gjennomforinger") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(GetGjennomforingerRequest())
                }

                response.status shouldBe HttpStatusCode.Forbidden
                response.body<JsonObject>()["missingRoles"]!!.jsonArray
                    .map { it.jsonPrimitive.content }
                    .toSet() shouldBe setOf(Rolle.TILTAKADMINISTRASJON_GENERELL.visningsnavn)
            }
        }

        test("200 OK med generell tilgang") {
            withTestApplication(appConfig()) {
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle))

                val response = client.post("/api/tiltaksadministrasjon/gjennomforinger") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(GetGjennomforingerRequest())
                }

                response.status shouldBe HttpStatusCode.OK
            }
        }

        test("200 OK for excel nedlasting med request body") {
            withTestApplication(appConfig()) {
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle))

                val response = client.post("/api/tiltaksadministrasjon/gjennomforinger/excel") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        GetGjennomforingerRequest(
                            search = "donald",
                            statuser = listOf(GjennomforingStatusType.GJENNOMFORES),
                        ),
                    )
                }

                response.status shouldBe HttpStatusCode.OK
                response.contentType() shouldBe ContentType.Application.Xlsx
            }
        }
    }

    context("opprett gjennomføring") {
        val avtale = AvtaleFixtures.oppfolging.copy(
            veilederinfo = AvtaleFixtures.oppfolging.veilederinfo.copy(
                navEnheter = setOf(
                    NavEnhetFixtures.Oslo.enhetsnummer,
                    NavEnhetFixtures.Sagene.enhetsnummer,
                ),
            ),
        )

        val domain = MulighetsrommetTestDomain(
            navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Oslo, NavEnhetFixtures.Sagene),
            ansatte = listOf(ansatt),
            arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
            avtaler = listOf(avtale),
        )

        beforeEach {
            domain.initialize(database.api)
        }

        afterEach {
            database.truncateAll()
        }

        test("403 Forbidden når bruker mangler generell tilgang") {
            withTestApplication(appConfig()) {
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(gjennomforingSkrivRolle))

                val response = client.put("/api/tiltaksadministrasjon/gjennomforinger") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(createGjennomforingRequest(avtale))
                }

                response.status shouldBe HttpStatusCode.Forbidden
                response.body<JsonObject>()["missingRoles"]!!.jsonArray
                    .map { it.jsonPrimitive.content }
                    .toSet() shouldBe setOf(Rolle.TILTAKADMINISTRASJON_GENERELL.visningsnavn)
            }
        }

        test("403 Forbidden når bruker ikke har skrivetilgang til gjennomføringer") {
            withTestApplication(appConfig()) {
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle))

                val response = client.put("/api/tiltaksadministrasjon/gjennomforinger") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                }

                response.status shouldBe HttpStatusCode.Forbidden
                response.body<JsonObject>()["missingRoles"]!!.jsonArray
                    .map { it.jsonPrimitive.content }
                    .toSet() shouldBe setOf(Rolle.TILTAKSGJENNOMFORINGER_SKRIV.visningsnavn)
            }
        }

        test("400 Bad Request når gjennomføringen er ugyldig") {
            withTestApplication(appConfig()) {
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, gjennomforingSkrivRolle))

                val response = client.put("/api/tiltaksadministrasjon/gjennomforinger") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        createGjennomforingRequest(
                            avtale,
                            administratorer = setOf(),
                            navRegioner = setOf(NavEnhetFixtures.Oslo.enhetsnummer),
                            navKontorer = setOf(NavEnhetFixtures.Sagene.enhetsnummer),
                        ),
                    )
                }

                response.status shouldBe HttpStatusCode.BadRequest
                response.body<ValidationError>().errors shouldBe listOf(
                    FieldError.of(
                        "Du må velge minst én administrator",
                        GjennomforingDetaljerRequest::administratorer,
                    ),
                )
            }
        }

        test("200 OK når bruker har skrivetilgang til gjennomføringer") {
            withTestApplication(appConfig()) {
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, gjennomforingSkrivRolle))

                val response = client.put("/api/tiltaksadministrasjon/gjennomforinger") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        createGjennomforingRequest(
                            avtale,
                            navRegioner = setOf(NavEnhetFixtures.Oslo.enhetsnummer),
                            navKontorer = setOf(NavEnhetFixtures.Sagene.enhetsnummer),
                        ),
                    )
                }

                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    context("avbryt gjennomføring") {
        val aktivGjennomforingId = UUID.randomUUID()
        val avbruttGjennomforingId = UUID.randomUUID()

        val domain = MulighetsrommetTestDomain(
            ansatte = listOf(ansatt),
            avtaler = listOf(AvtaleFixtures.oppfolging),
            gjennomforinger = listOf(
                GjennomforingDboFixtures.Oppfolging1.copy(
                    id = aktivGjennomforingId,
                    startDato = LocalDate.now(),
                    sluttDato = LocalDate.now().plusDays(10),
                    status = GjennomforingStatusType.GJENNOMFORES,
                ),
                GjennomforingDboFixtures.Oppfolging1.copy(
                    id = avbruttGjennomforingId,
                ),
            ),
        ) {
            queries.gjennomforing.setStatus(
                id = avbruttGjennomforingId,
                status = GjennomforingStatusType.AVBRUTT,
                sluttDato = LocalDate.now(),
                aarsaker = listOf(AvbrytGjennomforingAarsak.FEILREGISTRERING),
                forklaring = null,
            )
        }

        beforeEach {
            domain.initialize(database.api)
        }

        afterEach {
            database.truncateAll()
        }

        test("bad request når årsak mangler") {
            withTestApplication(appConfig()) {
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, gjennomforingSkrivRolle))

                val response = client
                    .put("/api/tiltaksadministrasjon/gjennomforinger/$aktivGjennomforingId/avbryt") {
                        bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                        contentType(ContentType.Application.Json)
                        setBody(
                            AarsakerOgBegrunnelseRequest<AvbrytGjennomforingAarsak>(
                                aarsaker = emptyList(),
                                begrunnelse = null,
                            ),
                        )
                    }

                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("bad request når beskrivelse mangler for Annen årsak") {
            withTestApplication(appConfig()) {
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, gjennomforingSkrivRolle))

                val response = client
                    .put("/api/tiltaksadministrasjon/gjennomforinger/$aktivGjennomforingId/avbryt") {
                        bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                        contentType(ContentType.Application.Json)
                        setBody(
                            AarsakerOgBegrunnelseRequest(
                                aarsaker = listOf(AvbrytGjennomforingAarsak.ANNET),
                                begrunnelse = null,
                            ),
                        )
                    }

                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("bad request når gjennomføring allerede er avbrutt") {
            withTestApplication(appConfig()) {
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, gjennomforingSkrivRolle))

                val response = client
                    .put("/api/tiltaksadministrasjon/gjennomforinger/$avbruttGjennomforingId/avbryt") {
                        bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                        contentType(ContentType.Application.Json)
                        setBody(
                            AarsakerOgBegrunnelseRequest(
                                aarsaker = listOf(AvbrytGjennomforingAarsak.FEILREGISTRERING),
                                begrunnelse = null,
                            ),
                        )
                    }

                response.status shouldBe HttpStatusCode.BadRequest
                response.body<ValidationError>().errors shouldBe listOf(
                    FieldError("/", "Gjennomføringen er allerede avbrutt"),
                )
            }
        }

        test("avbryter gjennomføring") {
            withTestApplication(appConfig()) {
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, gjennomforingSkrivRolle))

                val response = client.put("/api/tiltaksadministrasjon/gjennomforinger/$aktivGjennomforingId/avbryt") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        AarsakerOgBegrunnelseRequest(
                            aarsaker = listOf(AvbrytGjennomforingAarsak.FEILREGISTRERING),
                            begrunnelse = null,
                        ),
                    )
                }

                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText().shouldBeEmpty()

                database.run {
                    queries.gjennomforing.getGjennomforingAvtaleOrError(aktivGjennomforingId).should {
                        it.status shouldBe GjennomforingAvtaleStatus.Avbrutt(
                            aarsaker = listOf(AvbrytGjennomforingAarsak.FEILREGISTRERING),
                            forklaring = null,
                        )
                    }
                }
            }
        }
    }

    context("manuell migrering fra Arena") {
        val arena = GjennomforingDboFixtures.ArenaEnkelAmo.copy(
            prismodellId = null,
            ansvarligEnhet = null,
            arenaTiltaksnummer = Tiltaksnummer("2026#42"),
            arenaAnsvarligEnhet = "0400",
        )
        val base = "/api/tiltaksadministrasjon/enkeltplasser"

        val config = appConfig().copy(
            tiltakstyper = TiltakstypeService.Config(
                mapOf(Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to setOf(TiltakstypeFeature.MIGRERT)),
            ),
        )

        beforeEach {
            MulighetsrommetTestDomain(
                gjennomforinger = listOf(arena),
                deltakere = listOf(
                    DeltakerFixtures.createDeltaker(
                        gjennomforingId = arena.id,
                        status = DeltakerStatusType.DELTAR,
                        startDato = LocalDate.of(2026, 1, 1),
                        sluttDato = null,
                        endretTidspunkt = Instant.parse("2026-01-02T10:00:00Z"),
                    ),
                ),
            ).initialize(database.api)
        }

        afterEach {
            database.truncateAll()
        }

        test("generell tilgang gir ingen enkeltplasshandlinger") {
            withTestApplication(config) {
                val token = oauth.issueToken(claims = getAnsattClaims(ansatt, setOf(generellRolle))).serialize()

                val response = client.get("$base/handlinger") { bearerAuth(token) }

                response.status shouldBe HttpStatusCode.OK
                response.body<Set<GjennomforingHandling>>() shouldBe emptySet()
            }
        }

        test("saksbehandlertilgang gir migreringshandling når en støttet tiltakstype er migrert") {
            withTestApplication(config) {
                val token = oauth
                    .issueToken(claims = getAnsattClaims(ansatt, setOf(generellRolle, enkeltplassSaksbehandler)))
                    .serialize()

                val response = client.get("$base/handlinger") { bearerAuth(token) }

                response.status shouldBe HttpStatusCode.OK
                response.body<Set<GjennomforingHandling>>() shouldBe setOf(GjennomforingHandling.MIGRER_ENKELTPLASS)
            }
        }

        test("oppslag og migrering krever saksbehandlertilgang til enkeltplasser") {
            withTestApplication(config) {
                val token = oauth.issueToken(claims = getAnsattClaims(ansatt, setOf(generellRolle))).serialize()
                client.post("$base/migrering/sok") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(FinnArenaEnkeltplassRequest(search = "2026/42"))
                }.status shouldBe HttpStatusCode.Forbidden

                client.post("$base/${arena.id}/migrer") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(
                        MigrerEnkeltplassRequest(
                            type = PrismodellType.ANSKAFFET_ENKELTPLASS,
                            totalbelop = 4200,
                        ),
                    )
                }.status shouldBe HttpStatusCode.Forbidden
            }
        }

        test("avvist persontilgang ved identitetssøk mappes til forbudt i ruten") {
            val errorConfig = config.copy(
                engine = createMockEngine { mockTilgangsmaskinenForbidden("AVVIST_FORTROLIG_ADRESSE") },
            )
            withTestApplication(errorConfig) {
                val token = oauth
                    .issueToken(claims = getAnsattClaims(ansatt, setOf(generellRolle, enkeltplassSaksbehandler)))
                    .serialize()

                val response = client.post("$base/migrering/sok") {
                    bearerAuth(token)
                    setBody(FinnArenaEnkeltplassRequest(search = "12345678901"))
                }

                response.status shouldBe HttpStatusCode.Forbidden
                response.bodyAsText() shouldContain "mangler-tilgang-til-person"
            }
        }

        test("migrering mapper tilskuddsbeløp fra HTTP-forespørselen") {
            val tilskuddConfig = config.copy(
                engine = createMockEngine {
                    mockAmtDeltakerPersonalia(PdlGradering.UGRADERT)
                    mockPdlEmptyResult()
                    post("/tilgangsmaskin/api/v1/bulk/obo") {
                        respondJson(
                            TilgangsmaskinResponse(listOf(TilgangsmaskinResponse.Resultat("12345678901", 204))),
                            HttpStatusCode.MultiStatus,
                        )
                    }
                },
            )
            withTestApplication(tilskuddConfig) {
                val token = oauth
                    .issueToken(claims = getAnsattClaims(ansatt, setOf(generellRolle, enkeltplassSaksbehandler)))
                    .serialize()

                val response = client.post("$base/${arena.id}/migrer") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(
                        MigrerEnkeltplassRequest(
                            type = PrismodellType.TILSKUDD_TIL_OPPLAERING,
                            tilskudd = mapOf(
                                Opplaeringtilskudd.Kode.SEMESTERAVGIFT to 1500,
                                Opplaeringtilskudd.Kode.SKOLEPENGER to 3000,
                            ),
                        ),
                    )
                }

                response.status shouldBe HttpStatusCode.OK
                response.body<MigrerEnkeltplassResponse>().id shouldBe arena.id
                database.run {
                    val gjennomforing = queries.gjennomforing.getGjennomforingEnkeltplassOrError(arena.id)
                    gjennomforing.prismodell.shouldBeTypeOf<Prismodell.TilskuddTilOpplaering>().tilskudd shouldBe mapOf(
                        Opplaeringtilskudd.Kode.SEMESTERAVGIFT to 1500.NOK,
                        Opplaeringtilskudd.Kode.SKOLEPENGER to 3000.NOK,
                    )
                }
            }
        }
    }
})
