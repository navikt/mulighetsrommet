package no.nav.mulighetsrommet.tiltakshistorikk

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dbo.ArenaDeltakerDbo
import no.nav.mulighetsrommet.domain.dbo.ArenaDeltakerStatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.*
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerStatus
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerV1Dto
import no.nav.mulighetsrommet.tiltakshistorikk.repositories.DeltakerRepository
import no.nav.mulighetsrommet.tiltakshistorikk.repositories.GruppetiltakRepository
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TiltakshistorikkTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }

    context("tiltakshistorikk for bruker") {
        beforeAny {
            inititalizeData(database)
        }

        test("unauthroized når token mangler") {
            withTestApplication(oauth) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val response = client.post("/api/v1/historikk") {
                    contentType(ContentType.Application.Json)
                    setBody(TiltakshistorikkRequest(identer = listOf(NorskIdent("12345678910"))))
                }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("tom historikk når det ikke finnes noen deltakelser for gitt ident") {
            withTestApplication(oauth) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val response = client.post("/api/v1/historikk") {
                    bearerAuth(oauth.issueToken().serialize())
                    contentType(ContentType.Application.Json)
                    setBody(TiltakshistorikkRequest(identer = listOf(NorskIdent("12345123456"))))
                }

                response.status shouldBe HttpStatusCode.OK

                response.body<TiltakshistorikkResponse>().historikk.shouldBeEmpty()
            }
        }

        test("samlet historikk når det finnes deltakelser for gitt ident") {
            withTestApplication(oauth) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val response = client.post("/api/v1/historikk") {
                    bearerAuth(oauth.issueToken().serialize())
                    contentType(ContentType.Application.Json)
                    setBody(TiltakshistorikkRequest(identer = listOf(NorskIdent("12345678910"))))
                }

                response.status shouldBe HttpStatusCode.OK

                response.body<TiltakshistorikkResponse>().historikk shouldContainExactlyInAnyOrder listOf(
                    Tiltakshistorikk.ArenaDeltakelse(
                        norskIdent = NorskIdent("12345678910"),
                        id = UUID.fromString("4bf76cc3-ade9-45ef-b22b-5c4d3ceee185"),
                        arenaTiltakskode = "MENTOR",
                        status = ArenaDeltakerStatus.GJENNOMFORES,
                        startDato = LocalDate.of(2024, 2, 1),
                        sluttDato = LocalDate.of(2024, 2, 29),
                        beskrivelse = "Mentortiltak hos Joblearn",
                        arrangor = Tiltakshistorikk.Arrangor(Organisasjonsnummer("123123123")),
                    ),
                    Tiltakshistorikk.ArenaDeltakelse(
                        norskIdent = NorskIdent("12345678910"),
                        id = UUID.fromString("05fae1e4-4dcb-4b29-a8e6-7f6b6b52d617"),
                        arenaTiltakskode = "ARBTREN",
                        status = ArenaDeltakerStatus.GJENNOMFORES,
                        startDato = LocalDate.of(2024, 1, 1),
                        sluttDato = LocalDate.of(2024, 1, 31),
                        beskrivelse = "Arbeidstrening hos Fretex",
                        arrangor = Tiltakshistorikk.Arrangor(Organisasjonsnummer("123123123")),
                    ),
                    Tiltakshistorikk.GruppetiltakDeltakelse(
                        norskIdent = NorskIdent("12345678910"),
                        id = UUID.fromString("6d54228f-534f-4b4b-9160-65eae26a3b06"),
                        startDato = null,
                        sluttDato = null,
                        status = AmtDeltakerStatus(
                            type = AmtDeltakerStatus.Type.VENTER_PA_OPPSTART,
                            aarsak = null,
                            opprettetDato = LocalDateTime.of(2023, 3, 1, 0, 0),
                        ),
                        gjennomforing = Tiltakshistorikk.Gjennomforing(
                            id = UUID.fromString("566b89b0-4ed0-43cf-84a8-39085428f7e6"),
                            navn = "Gruppe AMO",
                            tiltakskode = Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
                        ),
                        arrangor = Tiltakshistorikk.Arrangor(Organisasjonsnummer("123123123")),
                    ),
                )
            }
        }
    }
})

private fun inititalizeData(database: FlywayDatabaseTestListener) {
    val gruppetiltak = GruppetiltakRepository(database.db)
    val deltakere = DeltakerRepository(database.db)

    val tiltak = TiltaksgjennomforingV1Dto(
        id = UUID.fromString("566b89b0-4ed0-43cf-84a8-39085428f7e6"),
        tiltakstype = TiltaksgjennomforingV1Dto.Tiltakstype(
            id = UUID.fromString("af6f4034-08da-4bd4-8735-ffd883e8aab7"),
            navn = "Gruppe AMO",
            arenaKode = "GRUPPEAMO",
            tiltakskode = Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
        ),
        navn = "Gruppe AMO",
        virksomhetsnummer = "123123123",
        startDato = LocalDate.now(),
        sluttDato = null,
        status = TiltaksgjennomforingStatus.GJENNOMFORES,
        oppstart = TiltaksgjennomforingOppstartstype.FELLES,
        tilgjengeligForArrangorFraOgMedDato = null,
    )
    gruppetiltak.upsert(tiltak)

    val arbeidstrening = ArenaDeltakerDbo(
        id = UUID.fromString("05fae1e4-4dcb-4b29-a8e6-7f6b6b52d617"),
        norskIdent = NorskIdent("12345678910"),
        arenaTiltakskode = "ARBTREN",
        status = ArenaDeltakerStatus.GJENNOMFORES,
        startDato = LocalDateTime.of(2024, 1, 1, 0, 0, 0),
        sluttDato = LocalDateTime.of(2024, 1, 31, 0, 0, 0),
        beskrivelse = "Arbeidstrening hos Fretex",
        arrangorOrganisasjonsnummer = Organisasjonsnummer("123123123"),
        registrertIArenaDato = LocalDateTime.of(2024, 1, 1, 0, 0, 0),
    )
    deltakere.upsertArenaDeltaker(arbeidstrening)

    val mentor = ArenaDeltakerDbo(
        id = UUID.fromString("4bf76cc3-ade9-45ef-b22b-5c4d3ceee185"),
        norskIdent = NorskIdent("12345678910"),
        arenaTiltakskode = "MENTOR",
        status = ArenaDeltakerStatus.GJENNOMFORES,
        startDato = LocalDateTime.of(2024, 2, 1, 0, 0, 0),
        sluttDato = LocalDateTime.of(2024, 2, 29, 0, 0, 0),
        beskrivelse = "Mentortiltak hos Joblearn",
        arrangorOrganisasjonsnummer = Organisasjonsnummer("123123123"),
        registrertIArenaDato = LocalDateTime.of(2024, 1, 1, 0, 0, 0),
    )
    deltakere.upsertArenaDeltaker(mentor)

    val deltakelsesdato = LocalDateTime.of(2023, 3, 1, 0, 0, 0)
    val amtDeltaker = AmtDeltakerV1Dto(
        id = UUID.fromString("6d54228f-534f-4b4b-9160-65eae26a3b06"),
        gjennomforingId = tiltak.id,
        personIdent = "12345678910",
        startDato = null,
        sluttDato = null,
        status = AmtDeltakerStatus(
            type = AmtDeltakerStatus.Type.VENTER_PA_OPPSTART,
            aarsak = null,
            opprettetDato = deltakelsesdato,
        ),
        registrertDato = deltakelsesdato,
        endretDato = deltakelsesdato,
        dagerPerUke = 2.5f,
        prosentStilling = null,
    )
    deltakere.upsertKometDeltaker(amtDeltaker)
}
