package no.nav.mulighetsrommet.api.gjennomforing.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import no.nav.mulighetsrommet.api.createAuthConfig
import no.nav.mulighetsrommet.api.createTestApplicationConfig
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.gjennomforing.db.EnkeltplassArenaDataDbo
import no.nav.mulighetsrommet.api.plugins.AppRoles
import no.nav.mulighetsrommet.api.withTestApplication
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.TiltaksgjennomforingArenaDataDto
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.*

class GjennomforingPublicRoutesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }

    val domain = MulighetsrommetTestDomain(
        navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
        ansatte = listOf(NavAnsattFixture.DonaldDuck),
        arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
        avtaler = listOf(AvtaleFixtures.oppfolging),
        gjennomforinger = listOf(GjennomforingFixtures.Oppfolging1),
    )

    beforeAny {
        domain.initialize(database.db)

        database.run {
            queries.enkeltplass.upsert(EnkeltplassFixtures.EnkelAmo)
        }
    }

    fun appConfig() = createTestApplicationConfig().copy(
        auth = createAuthConfig(oauth, roles = setOf()),
    )

    context("getTiltaksgjennomforingV2") {
        val tiltakGruppeId = GjennomforingFixtures.Oppfolging1.id
        val tiltakEnkeltplassId = EnkeltplassFixtures.EnkelAmo.id

        test("401 når påkrevde claims mangler fra token") {
            withTestApplication(appConfig()) {
                val response = client.get("/api/v2/tiltaksgjennomforinger/$tiltakGruppeId") {
                    bearerAuth(
                        oauth.issueToken(claims = withApplicationRoles()).serialize(),
                    )
                }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("404 når gjennomføring ikke finnes") {
            withTestApplication(appConfig()) {
                val response = client.get("/api/v2/tiltaksgjennomforinger/${UUID.randomUUID()}") {
                    bearerAuth(
                        oauth.issueToken(claims = withApplicationRoles(AppRoles.READ_GJENNOMFORING)).serialize(),
                    )
                }

                response.status shouldBe HttpStatusCode.NotFound
            }
        }

        test("200 når gruppetiltak finnes") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val response = client.get("/api/v2/tiltaksgjennomforinger/$tiltakGruppeId") {
                    bearerAuth(
                        oauth.issueToken(claims = withApplicationRoles(AppRoles.READ_GJENNOMFORING)).serialize(),
                    )
                }

                response.status shouldBe HttpStatusCode.OK

                val gjennomforing = response.body<TiltaksgjennomforingV2Dto>()
                    .shouldBeTypeOf<TiltaksgjennomforingV2Dto.Gruppe>()

                gjennomforing.id shouldBe tiltakGruppeId
                gjennomforing.arrangor.organisasjonsnummer shouldBe ArrangorFixtures.underenhet1.organisasjonsnummer
            }
        }

        test("200 når enkeltplass finnes") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val response = client.get("/api/v2/tiltaksgjennomforinger/$tiltakEnkeltplassId") {
                    bearerAuth(
                        oauth.issueToken(claims = withApplicationRoles(AppRoles.READ_GJENNOMFORING)).serialize(),
                    )
                }

                response.status shouldBe HttpStatusCode.OK

                val gjennomforing = response.body<TiltaksgjennomforingV2Dto>()
                    .shouldBeTypeOf<TiltaksgjennomforingV2Dto.Enkeltplass>()

                gjennomforing.id shouldBe tiltakEnkeltplassId
                gjennomforing.arrangor.organisasjonsnummer shouldBe ArrangorFixtures.underenhet1.organisasjonsnummer
            }
        }
    }

    context("getTiltaksgjennomforingArenadata") {
        val tiltakGruppeId = GjennomforingFixtures.Oppfolging1.id
        val tiltakEnkeltplassId = EnkeltplassFixtures.EnkelAmo.id

        test("401 når påkrevde claims mangler fra token") {
            withTestApplication(appConfig()) {
                val response = client.get("/api/v1/tiltaksgjennomforinger/arenadata/$tiltakGruppeId") {
                    bearerAuth(
                        oauth.issueToken(claims = withApplicationRoles()).serialize(),
                    )
                }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("404 når gjennomføring ikke finnes") {
            withTestApplication(appConfig()) {
                val response = client.get("/api/v1/tiltaksgjennomforinger/arenadata/${UUID.randomUUID()}") {
                    bearerAuth(
                        oauth.issueToken(claims = withApplicationRoles(AppRoles.READ_GJENNOMFORING)).serialize(),
                    )
                }

                response.status shouldBe HttpStatusCode.NotFound
            }
        }

        test("201 når tiltaksnummer mangler fra guppetiltak") {
            withTestApplication(appConfig()) {
                val response = client.get("/api/v1/tiltaksgjennomforinger/arenadata/$tiltakGruppeId") {
                    bearerAuth(
                        oauth.issueToken(claims = withApplicationRoles(AppRoles.READ_GJENNOMFORING)).serialize(),
                    )
                }

                response.status shouldBe HttpStatusCode.NoContent
            }
        }

        test("200 når tiltaksnummer finnes på guppetiltak") {
            database.run {
                queries.gjennomforing.updateArenaData(tiltakGruppeId, "2024#123", null)
            }

            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val response = client.get("/api/v1/tiltaksgjennomforinger/arenadata/$tiltakGruppeId") {
                    bearerAuth(
                        oauth.issueToken(claims = withApplicationRoles(AppRoles.READ_GJENNOMFORING)).serialize(),
                    )
                }

                response.status shouldBe HttpStatusCode.OK
                response.body<TiltaksgjennomforingArenaDataDto>() shouldBe TiltaksgjennomforingArenaDataDto(
                    opprettetAar = 2024,
                    lopenr = 123,
                )
            }
        }

        test("204 når tiltaksnummer ikke finnes på enkeltplass") {
            withTestApplication(appConfig()) {
                val response = client.get("/api/v1/tiltaksgjennomforinger/arenadata/$tiltakEnkeltplassId") {
                    bearerAuth(
                        oauth.issueToken(claims = withApplicationRoles(AppRoles.READ_GJENNOMFORING)).serialize(),
                    )
                }

                response.status shouldBe HttpStatusCode.NoContent
            }
        }

        test("200 når tiltaksnummer finnes på enkeltplass") {
            database.run {
                queries.enkeltplass.setArenaData(
                    EnkeltplassArenaDataDbo(
                        id = tiltakEnkeltplassId,
                        tiltaksnummer = "2025#1",
                        navn = null,
                        startDato = null,
                        sluttDato = null,
                        status = null,
                        arenaAnsvarligEnhet = null,
                    ),
                )
            }

            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val response = client.get("/api/v1/tiltaksgjennomforinger/arenadata/$tiltakEnkeltplassId") {
                    bearerAuth(
                        oauth.issueToken(claims = withApplicationRoles(AppRoles.READ_GJENNOMFORING)).serialize(),
                    )
                }

                response.status shouldBe HttpStatusCode.OK
                response.body<TiltaksgjennomforingArenaDataDto>() shouldBe TiltaksgjennomforingArenaDataDto(
                    opprettetAar = 2025,
                    lopenr = 1,
                )
            }
        }
    }
})

private fun withApplicationRoles(roles: String? = null): Map<String, List<String>> = mapOf(
    "roles" to listOfNotNull(AppRoles.ACCESS_AS_APPLICATION, roles),
)
