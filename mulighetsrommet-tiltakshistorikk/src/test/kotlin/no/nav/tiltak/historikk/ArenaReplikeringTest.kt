package no.nav.tiltak.historikk

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.ArenaDeltakerStatus
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.plugins.TiltakshistorikkWriteRoles
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class ArenaReplikeringTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))
    val db by lazy { TiltakshistorikkDatabase(database.db) }
    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }

    beforeEach {
        db.session {
            queries.tiltakstype.upsert(TestFixtures.Tiltakstype.Arbeidstrening)
        }
    }

    afterEach {
        database.truncateAll()
    }

    test("replikerer og sletter Arena-deltaker og gjennomføring gjennom HTTP") {
        val gjennomforing = TestFixtures.ArenaGjennomforing.Arbeidstrening.copy(
            arrangorOrganisasjonsnummer = Organisasjonsnummer("111222333"),
            arenaRegDato = Instant.parse("2025-01-01T10:00:00.123456789Z"),
            arenaModDato = Instant.parse("2025-01-02T11:00:00.987654789Z"),
        )
        val deltaker = TiltakshistorikkArenaDeltaker(
            id = UUID.randomUUID(),
            arenaRegDato = Instant.parse("2025-01-01T10:00:00.123456789Z"),
            arenaModDato = Instant.parse("2025-01-02T11:00:00.987654789Z"),
            arenaGjennomforingId = gjennomforing.id,
            arenaDeltakerId = 1,
            norskIdent = NorskIdent("12345678910"),
            status = ArenaDeltakerStatus.GJENNOMFORES,
            startDato = LocalDate.of(2025, 1, 1),
            sluttDato = null,
            dagerPerUke = 5.0,
            deltidsprosent = 100.0,
        )
        val token = oauth.issueToken(claims = mapOf("roles" to TiltakshistorikkWriteRoles)).serialize()

        withTestApplication(oauth) {
            client.put("/api/v1/intern/arena/gjennomforing") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(gjennomforing)
            }.status shouldBe HttpStatusCode.OK

            db.session {
                queries.arenaGjennomforing.get(gjennomforing.id) shouldBe gjennomforing.copy(
                    arenaRegDato = Instant.parse("2025-01-01T10:00:00.123456Z"),
                    arenaModDato = Instant.parse("2025-01-02T11:00:00.987654Z"),
                )
            }

            client.put("/api/v1/intern/arena/deltaker") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(deltaker)
            }.status shouldBe HttpStatusCode.OK

            db.session {
                val historikk = queries.arenaDeltaker.getArenaHistorikk(listOf(deltaker.norskIdent))
                historikk.single().gjennomforing.navn shouldBe "Arbeidstrening hos Fretex"
                historikk.single().startDato shouldBe LocalDate.of(2025, 1, 1)
                historikk.single().arrangor.underenhet.organisasjonsnummer shouldBe Organisasjonsnummer("111222333")
                historikk.single().opprettetTidspunkt shouldBe Instant.parse("2025-01-01T10:00:00.123456Z")
                historikk.single().oppdatertTidspunkt shouldBe Instant.parse("2025-01-02T11:00:00.987654Z")
            }

            client.put("/api/v1/intern/arena/deltaker") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(deltaker.copy(startDato = LocalDate.of(2025, 2, 1)))
            }.status shouldBe HttpStatusCode.OK

            db.session {
                queries.arenaDeltaker.getArenaHistorikk(listOf(deltaker.norskIdent))
                    .single().startDato shouldBe LocalDate.of(2025, 2, 1)
            }

            client.delete("/api/v1/intern/arena/deltaker/${deltaker.id}") {
                bearerAuth(token)
            }.status shouldBe HttpStatusCode.OK

            db.session {
                queries.arenaDeltaker.getArenaHistorikk(listOf(deltaker.norskIdent)).shouldBeEmpty()
            }

            client.delete("/api/v1/intern/arena/gjennomforing/${gjennomforing.id}") {
                bearerAuth(token)
            }.status shouldBe HttpStatusCode.OK

            db.session {
                queries.arenaGjennomforing.get(gjennomforing.id).shouldBeNull()
            }
        }
    }

    test("avviser Arena-gjennomføring når arrangør ikke kan hentes fra Brreg") {
        val gjennomforing = TestFixtures.ArenaGjennomforing.Arbeidstrening
        val engine = MockEngine { respond("", HttpStatusCode.NotFound) }
        val token = oauth.issueToken(claims = mapOf("roles" to TiltakshistorikkWriteRoles)).serialize()

        withTestApplication(oauth, httpClientEngine = engine) {
            client.put("/api/v1/intern/arena/gjennomforing") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(gjennomforing)
            }.status shouldBe HttpStatusCode.BadRequest

            db.session {
                queries.virksomhet.get(gjennomforing.arrangorOrganisasjonsnummer).shouldBeNull()
                queries.arenaGjennomforing.get(gjennomforing.id).shouldBeNull()
            }
        }
    }
})
