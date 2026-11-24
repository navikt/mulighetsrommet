package no.nav.tiltak.historikk

import com.diffplug.selfie.coroutines.expectSelfie
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.model.ArrangorDeltakerHistorikkKilde
import no.nav.tiltak.historikk.plugins.ACCESS_AS_APPLICATION
import no.nav.tiltak.historikk.plugins.TiltakshistorikkReadRoles
import java.time.LocalDate

class TiltakshistorikkTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }

    afterEach {
        database.truncateAll()
    }

    val db by lazy { TiltakshistorikkDatabase(database.db) }
    val request = TiltakshistorikkV1Request(listOf(NorskIdent("10101010100")))
    val readClaims = mapOf("roles" to TiltakshistorikkReadRoles)

    context("tilgangskontroll") {
        test("unauthorized når token mangler") {
            withTestApplication(oauth) {
                val response = client.post("/api/v1/historikk") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("unauthorized når riktige roles mangler") {
            withTestApplication(oauth) {
                val response = client.post("/api/v1/historikk") {
                    bearerAuth(oauth.issueToken(claims = mapOf("roles" to listOf(ACCESS_AS_APPLICATION))).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }
    }

    context("historikkrespons") {
        beforeEach {
            db.session {
                queries.virksomhet.upsert(TestFixtures.Virksomhet.Arrangor)
                queries.virksomhet.upsert(TestFixtures.Virksomhet.Arbeidsgiver)
                queries.tiltakstype.upsert(TestFixtures.Tiltakstype.EnkelAmo)
                queries.tiltakstype.upsert(TestFixtures.Tiltakstype.GruppeAmo)
                queries.tiltakstype.upsert(TestFixtures.Tiltakstype.Arbeidstrening)
                queries.arenaGjennomforing.upsert(TestFixtures.ArenaGjennomforing.EnkelAmo)
                queries.gjennomforing.upsert(TestFixtures.Gjennomforing.GruppeAmo)
            }
        }

        test("returnerer tom historikk når forespurte identer ikke har deltakelser") {
            db.session {
                queries.kometDeltaker.upsertKometDeltaker(TestFixtures.Deltaker.GruppeAmo)
            }

            val responseBody = withTestApplication(oauth) {
                val response = client.post("/api/v1/historikk") {
                    bearerAuth(oauth.issueToken(claims = readClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(TiltakshistorikkV1Request(listOf(NorskIdent("22345623456"))))
                }

                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText().prettyPrintJson()
            }
            expectSelfie(responseBody).toMatchDisk()
        }

        test("returnerer Arena-deltakelse som JSON") {
            db.session {
                queries.arenaDeltaker.upsertArenaDeltaker(TestFixtures.ArenaDeltaker.EnkelAmo)
            }

            val responseBody = withTestApplication(oauth) {
                val response = client.post("/api/v1/historikk") {
                    bearerAuth(oauth.issueToken(claims = readClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText().prettyPrintJson()
            }
            expectSelfie(responseBody).toMatchDisk()
        }

        test("returnerer Komet-deltakelse som JSON") {
            db.session {
                queries.kometDeltaker.upsertKometDeltaker(TestFixtures.Deltaker.GruppeAmo)
            }

            val responseBody = withTestApplication(oauth) {
                val response = client.post("/api/v1/historikk") {
                    bearerAuth(oauth.issueToken(claims = readClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText().prettyPrintJson()
            }
            expectSelfie(responseBody).toMatchDisk()
        }

        test("returnerer Team Tiltak-avtale som JSON") {
            db.session {
                queries.arbeidsgiverAvtale.upsert(TestFixtures.Avtale.Arbeidstrening)
            }

            val config = createTestApplicationConfig(oauth).copy(
                arbeidsgiverTiltakCutoffDatoer = mapOf(
                    Tiltakskode.ARBEIDSTRENING to LocalDate.of(2024, 1, 1),
                ),
            )

            val responseBody = withTestApplication(oauth, config = config) {
                val response = client.post("/api/v1/historikk") {
                    bearerAuth(oauth.issueToken(claims = readClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText().prettyPrintJson()
            }
            expectSelfie(responseBody).toMatchDisk()
        }
    }

    context("konfigurert kildevalg") {
        beforeEach {
            db.session {
                queries.virksomhet.upsert(TestFixtures.Virksomhet.Arrangor)
                queries.virksomhet.upsert(TestFixtures.Virksomhet.Arbeidsgiver)
                queries.tiltakstype.upsert(TestFixtures.Tiltakstype.EnkelAmo)
                queries.tiltakstype.upsert(TestFixtures.Tiltakstype.Arbeidstrening)
                queries.arenaGjennomforing.upsert(TestFixtures.ArenaGjennomforing.EnkelAmo)
                queries.arenaGjennomforing.upsert(TestFixtures.ArenaGjennomforing.Arbeidstrening)
                queries.gjennomforing.upsert(TestFixtures.Gjennomforing.EnkelAmo)
            }
        }

        test("bruker appens konfigurasjon for å velge deltakere fra Arena") {
            db.session {
                queries.arenaDeltaker.upsertArenaDeltaker(TestFixtures.ArenaDeltaker.EnkelAmo)
                queries.kometDeltaker.upsertKometDeltaker(TestFixtures.Deltaker.EnkelAmo)
            }
            val config = createTestApplicationConfig(oauth).copy(
                arrangorTiltakHistorikkKilder = mapOf(
                    Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to ArrangorDeltakerHistorikkKilde.ARENA,
                ),
            )

            withTestApplication(oauth, config = config) {
                val response = client.post("/api/v1/historikk") {
                    bearerAuth(oauth.issueToken(claims = readClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                response.status shouldBe HttpStatusCode.OK
                response.body<TiltakshistorikkV1Response>().historikk shouldMatchExactlyIds listOf(
                    TestFixtures.ArenaDeltaker.EnkelAmo.id,
                )
            }
        }

        test("bruker appens konfigurasjon for å velge deltakere fra Komet") {
            db.session {
                queries.arenaDeltaker.upsertArenaDeltaker(TestFixtures.ArenaDeltaker.EnkelAmo)
                queries.kometDeltaker.upsertKometDeltaker(TestFixtures.Deltaker.EnkelAmo)
            }
            val config = createTestApplicationConfig(oauth).copy(
                arrangorTiltakHistorikkKilder = mapOf(
                    Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to ArrangorDeltakerHistorikkKilde.KOMET,
                ),
            )

            withTestApplication(oauth, config = config) {
                val response = client.post("/api/v1/historikk") {
                    bearerAuth(oauth.issueToken(claims = readClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                response.status shouldBe HttpStatusCode.OK
                response.body<TiltakshistorikkV1Response>().historikk shouldMatchExactlyIds listOf(
                    TestFixtures.Deltaker.EnkelAmo.id,
                )
            }
        }

        test("bruker appens cutoff-konfigurasjon for å velge deltakere fra Team Tiltak") {
            db.session {
                queries.arenaDeltaker.upsertArenaDeltaker(TestFixtures.ArenaDeltaker.Arbeidstrening)
                queries.arbeidsgiverAvtale.upsert(TestFixtures.Avtale.Arbeidstrening)
            }
            val config = createTestApplicationConfig(oauth).copy(
                arbeidsgiverTiltakCutoffDatoer = mapOf(
                    Tiltakskode.ARBEIDSTRENING to LocalDate.of(2024, 1, 1),
                ),
            )

            withTestApplication(oauth, config = config) {
                val response = client.post("/api/v1/historikk") {
                    bearerAuth(oauth.issueToken(claims = readClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                response.status shouldBe HttpStatusCode.OK
                response.body<TiltakshistorikkV1Response>().historikk shouldMatchExactlyIds listOf(
                    TestFixtures.Avtale.Arbeidstrening.avtaleId,
                )
            }
        }
    }
})
