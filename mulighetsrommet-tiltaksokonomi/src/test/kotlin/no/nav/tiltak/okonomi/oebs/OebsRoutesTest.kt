package no.nav.tiltak.okonomi.oebs

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.model.*
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.tiltak.okonomi.*
import no.nav.tiltak.okonomi.api.API_BASE_PATH
import no.nav.tiltak.okonomi.api.OebsBestillingKvittering
import no.nav.tiltak.okonomi.api.OebsFakturaKvittering
import no.nav.tiltak.okonomi.db.OkonomiDatabase
import no.nav.tiltak.okonomi.db.QueryContext
import no.nav.tiltak.okonomi.model.Bestilling
import no.nav.tiltak.okonomi.model.Faktura
import no.nav.tiltak.okonomi.plugins.AppRoles
import no.nav.tiltak.okonomi.withTestApplication
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.time.LocalDateTime

class OebsRoutesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    lateinit var db: OkonomiDatabase
    val bestilling = Bestilling.fromOpprettBestilling(
        OpprettBestilling(
            bestillingsnummer = "1",
            tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
            tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
            arrangor = OpprettBestilling.Arrangor(
                hovedenhet = Organisasjonsnummer("123456789"),
                underenhet = Organisasjonsnummer("234567891"),
            ),
            avtalenummer = null,
            belop = 1000,
            behandletAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
            behandletTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
            besluttetAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
            besluttetTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            kostnadssted = NavEnhetNummer("0400"),
        ),
    )
    val faktura = Faktura.fromOpprettFaktura(
        OpprettFaktura(
            fakturanummer = "1-1",
            bestillingsnummer = "1",
            betalingsinformasjon = OpprettFaktura.Betalingsinformasjon(
                kontonummer = Kontonummer("12345678901"),
                kid = null,
            ),
            belop = 1000,
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            behandletAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
            behandletTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
            besluttetAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
            besluttetTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
            gjorOppBestilling = false,
            beskrivelse = null,
        ),
        bestilling.linjer,
    )

    val oauth = MockOAuth2Server()
    lateinit var bearerAuth: String

    beforeSpec {
        db = OkonomiDatabase(database.db)
        db.session {
            queries.bestilling.insertBestilling(bestilling)
            queries.faktura.insertFaktura(faktura)
        }
        oauth.start()
        bearerAuth = oauth.issueToken(claims = mapOf("roles" to listOf(AppRoles.ACCESS_AS_APPLICATION, AppRoles.OEBS_API))).serialize()
    }

    afterSpec {
        oauth.shutdown()
    }

    fun ApplicationTestBuilder.createClient(): HttpClient {
        return createClient { install(ContentNegotiation) { json() } }
    }

    context("success gir status endring") {
        test("opprett-bestilling") {
            withTestApplication(oauth) {
                val client = createClient()

                val response = client.post("${API_BASE_PATH}/kvittering/bestilling") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(bearerAuth)
                    setBody(
                        listOf(
                            OebsBestillingKvittering(
                                bestillingsNummer = bestilling.bestillingsnummer,
                                opprettelsesTidspunkt = LocalDateTime.now(),
                            ),
                        ),
                    )
                }
                response.status shouldBe HttpStatusCode.OK
                db.session { queries.bestilling.getByBestillingsnummer(bestilling.bestillingsnummer) }
                    ?.status shouldBe BestillingStatusType.AKTIV
            }
        }

        test("annuller-bestilling") {
            withTestApplication(oauth) {
                val client = createClient()

                val response = client.post("${API_BASE_PATH}/kvittering/bestilling") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(bearerAuth)
                    setBody(
                        listOf(
                            OebsBestillingKvittering(
                                bestillingsNummer = bestilling.bestillingsnummer,
                                opprettelsesTidspunkt = LocalDateTime.now(),
                                annullert = "Y",
                            ),
                        ),
                    )
                }
                response.status shouldBe HttpStatusCode.OK
                db.session { queries.bestilling.getByBestillingsnummer(bestilling.bestillingsnummer) }
                    ?.status shouldBe BestillingStatusType.ANNULLERT
            }
        }

        test("faktura") {
            withTestApplication(oauth) {
                val client = createClient()

                val response = client.post("${API_BASE_PATH}/kvittering/faktura") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(bearerAuth)
                    setBody(
                        listOf(
                            OebsFakturaKvittering(
                                fakturaNummer = faktura.fakturanummer,
                                opprettelsesTidspunkt = LocalDateTime.now(),
                                statusBetalt = OebsFakturaKvittering.StatusBetalt.FulltBetalt,
                            ),
                        ),
                    )
                }
                response.status shouldBe HttpStatusCode.OK
                db.session { queries.faktura.getByFakturanummer(faktura.fakturanummer) }
                    ?.status shouldBe FakturaStatusType.FULLT_BETALT
            }
        }
    }

    test("bestilling først feil så godkjent") {
        withTestApplication(oauth) {
            val client = createClient()

            client.post("${API_BASE_PATH}/kvittering/bestilling") {
                contentType(ContentType.Application.Json)
                bearerAuth(bearerAuth)
                setBody(
                    listOf(
                        OebsBestillingKvittering(
                            bestillingsNummer = bestilling.bestillingsnummer,
                            opprettelsesTidspunkt = LocalDateTime.now(),
                            feilKode = "FEILKODE",
                        ),
                    ),
                )
            }.status shouldBe HttpStatusCode.OK
            db.session { queries.bestilling.getByBestillingsnummer(bestilling.bestillingsnummer) }
                ?.status shouldBe BestillingStatusType.FEILET

            client.post("${API_BASE_PATH}/kvittering/bestilling") {
                contentType(ContentType.Application.Json)
                bearerAuth(bearerAuth)
                setBody(
                    listOf(
                        OebsBestillingKvittering(
                            bestillingsNummer = bestilling.bestillingsnummer,
                            opprettelsesTidspunkt = LocalDateTime.now(),
                        ),
                    ),
                )
            }.status shouldBe HttpStatusCode.OK
            db.session { queries.bestilling.getByBestillingsnummer(bestilling.bestillingsnummer) }
                ?.status shouldBe BestillingStatusType.AKTIV
        }
    }

    test("faktura først feil så godkjent") {
        withTestApplication(oauth) {
            val client = createClient()

            client.post("${API_BASE_PATH}/kvittering/faktura") {
                contentType(ContentType.Application.Json)
                bearerAuth(bearerAuth)
                setBody(
                    listOf(
                        OebsFakturaKvittering(
                            fakturaNummer = faktura.fakturanummer,
                            opprettelsesTidspunkt = LocalDateTime.now(),
                            statusOpprettet = "Avvist",
                        ),
                    ),
                )
            }.status shouldBe HttpStatusCode.OK
            db.session { queries.faktura.getByFakturanummer(faktura.fakturanummer) }
                ?.status shouldBe FakturaStatusType.FEILET

            client.post("${API_BASE_PATH}/kvittering/faktura") {
                contentType(ContentType.Application.Json)
                bearerAuth(bearerAuth)
                setBody(
                    listOf(
                        OebsFakturaKvittering(
                            fakturaNummer = faktura.fakturanummer,
                            opprettelsesTidspunkt = LocalDateTime.now(),
                            statusOpprettet = "Suksess",
                            statusBetalt = OebsFakturaKvittering.StatusBetalt.DelvisBetalt,
                        ),
                    ),
                )
            }.status shouldBe HttpStatusCode.OK
            db.session { queries.faktura.getByFakturanummer(faktura.fakturanummer) }
                ?.status shouldBe FakturaStatusType.DELVIS_BETALT
        }
    }

    test("bad request ved manglende fakturaNummer") {
        withTestApplication(oauth) {
            val client = createClient()

            val response = client.post("${API_BASE_PATH}/kvittering/faktura") {
                contentType(ContentType.Application.Json)
                bearerAuth(bearerAuth)
                setBody(
                    """
                        { "statusOebs": "Godkjent", "opprettelsesTidspunkt": "2023-01-01 09:33:16" }
                    """.trimIndent(),
                )
            }
            response.status shouldBe HttpStatusCode.BadRequest
            val pd = response.body<ProblemDetail>()
            pd.status shouldBe 400
            pd.title shouldBe "Bad Request"
            pd.type shouldBe "bad-request"
        }
    }

    test("ikke not found når bestilling ikke finnes") {
        withTestApplication(oauth) {
            val client = createClient()

            val response = client.post("${API_BASE_PATH}/kvittering/bestilling") {
                contentType(ContentType.Application.Json)
                bearerAuth(bearerAuth)
                setBody(
                    """
                        [{
                            "bestillingsNummer": "999",
                            "statusOebs": "Godkjent",
                            "opprettelsesTidspunkt": "2023-01-01 09:33:16"
                        }]
                    """.trimIndent(),
                )
            }
            response.status shouldBe HttpStatusCode.OK
        }
    }

    test("ignoreUnknownKeys") {
        withTestApplication(oauth) {
            val client = createClient()

            val response = client.post("/api/v1/okonomi/kvittering/bestilling") {
                contentType(ContentType.Application.Json)
                bearerAuth(bearerAuth)
                setBody(
                    """
                        [{
                            "bestillingsNummer": "${bestilling.bestillingsnummer}",
                            "statusOebs": "Godkjent",
                            "opprettelsesTidspunkt": "2023-01-01 09:33:16",
                            "foo": "2023-01-01 09:33:16",
                            "bar": {
                                "baz": true
                            }
                        }]
                    """.trimIndent(),
                )
            }
            response.status shouldBe HttpStatusCode.OK
        }
    }

    test("kvittering json blir lagret") {
        val kvitteringJson = """
            {
                "bestillingsNummer": "${bestilling.bestillingsnummer}",
                "statusOebs": "Godkjent",
                "opprettelsesTidspunkt": "2023-01-01 09:33:16",
                "foo": "2023-01-01 09:33:16",
                "bar": {
                    "baz": true
                }
            }
        """.trimIndent()
        withTestApplication(oauth) {
            val client = createClient()

            client.post("${API_BASE_PATH}/kvittering/bestilling") {
                contentType(ContentType.Application.Json)
                bearerAuth(bearerAuth)
                setBody(kvitteringJson)
            }

            val json = Json.decodeFromString<JsonElement>(db.session { getLatestKvittering() })
            json.jsonObject["statusOebs"] shouldBe JsonPrimitive("Godkjent")
        }
    }

    test("manglende auth gir 401") {
        withTestApplication(oauth) {
            val client = createClient()

            client.post("${API_BASE_PATH}/kvittering/bestilling") {
                contentType(ContentType.Application.Json)
                setBody("")
            }.status shouldBe HttpStatusCode.Unauthorized

            client.post("${API_BASE_PATH}/kvittering/bestilling") {
                contentType(ContentType.Application.Json)
                bearerAuth(oauth.issueToken(claims = emptyMap()).serialize())
                setBody("")
            }.status shouldBe HttpStatusCode.Unauthorized

            client.post("${API_BASE_PATH}/kvittering/bestilling") {
                contentType(ContentType.Application.Json)
                bearerAuth(oauth.issueToken(claims = mapOf("roles" to listOf(AppRoles.ACCESS_AS_APPLICATION))).serialize())
                setBody("")
            }.status shouldBe HttpStatusCode.Unauthorized
        }
    }
})

private fun QueryContext.getLatestKvittering(): String {
    @Language("PostgreSQL")
    val query = """
        select * from oebs_kvittering order by id desc limit 1
    """.trimIndent()

    return session.requireSingle(queryOf(query)) { it.string("json") }
}
