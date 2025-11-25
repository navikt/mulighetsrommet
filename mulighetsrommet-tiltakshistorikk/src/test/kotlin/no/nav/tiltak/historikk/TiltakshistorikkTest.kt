package no.nav.tiltak.historikk

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.amt.model.AmtDeltakerV1Dto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.model.*
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.tiltak.historikk.clients.Avtale
import no.nav.tiltak.historikk.clients.GetAvtalerForPersonResponse
import no.nav.tiltak.historikk.clients.GraphqlResponse
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.kafka.consumers.toGjennomforingDbo
import no.nav.tiltak.historikk.plugins.ACCESS_AS_APPLICATION
import no.nav.tiltak.historikk.plugins.TiltakshistorikkRead
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

private val TEAM_TILTAK_ARBEIDSTRENING_ID: UUID = UUID.fromString("9dea48c1-d494-4664-9427-bdb20a6f265f")
private val ARENA_ARBEIDSTRENING_ID: UUID = UUID.fromString("05fae1e4-4dcb-4b29-a8e6-7f6b6b52d617")
private val ARENA_ENKEL_AMO_ID: UUID = UUID.fromString("ddb13a2b-cd65-432d-965c-9167938a26a4")
private val ARENA_MENTOR_ID: UUID = UUID.fromString("4bf76cc3-ade9-45ef-b22b-5c4d3ceee185")
private val TEAM_KOMET_GRUPPE_AMO_ID: UUID = UUID.fromString("6d54228f-534f-4b4b-9160-65eae26a3b06")

class TiltakshistorikkTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }

    context("tiltakshistorikk for bruker") {
        val db = TiltakshistorikkDatabase(database.db)

        beforeAny {
            inititalizeData(db)
        }

        test("unauthorized når token mangler") {
            val mockEngine = mockTiltakDatadeling()

            withTestApplication(oauth, mockEngine) {
                val response = client.post("/api/v1/historikk") {
                    contentType(ContentType.Application.Json)
                    setBody(TiltakshistorikkV1Request(identer = listOf(NorskIdent("12345678910")), maxAgeYears = null))
                }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("unauthorized når riktige roles mangler") {
            val mockEngine = mockTiltakDatadeling()

            withTestApplication(oauth, mockEngine) {
                val response = client.post("/api/v1/historikk") {
                    bearerAuth(oauth.issueToken(claims = mapOf("roles" to listOf(ACCESS_AS_APPLICATION))).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(TiltakshistorikkV1Request(identer = listOf(NorskIdent("12345678910")), maxAgeYears = null))
                }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("tom historikk når det ikke finnes noen deltakelser for gitt ident") {
            val mockEngine = mockTiltakDatadeling()

            withTestApplication(oauth, mockEngine) {
                val response = client.post("/api/v1/historikk") {
                    bearerAuth(oauth.issueToken(claims = tiltakshistorikkReadClaims()).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(TiltakshistorikkV1Request(identer = listOf(NorskIdent("22345623456")), maxAgeYears = null))
                }

                response.status shouldBe HttpStatusCode.OK

                response.body<TiltakshistorikkV1Response>().historikk.shouldBeEmpty()
            }
        }

        test("Henter enkeltplassdeltakelser fra Arena når de er avsluttet før cutoff-dato") {
            val mockEngine = mockTiltakDatadeling(
                response = GraphqlResponse(
                    data = GetAvtalerForPersonResponse(
                        avtalerForPerson = listOf(
                            Avtale(
                                avtaleId = TEAM_TILTAK_ARBEIDSTRENING_ID,
                                avtaleNr = 1,
                                deltakerFnr = NorskIdent("12345678910"),
                                bedriftNr = "876543210",
                                tiltakstype = Avtale.Tiltakstype.ARBEIDSTRENING,
                                startDato = LocalDate.of(2024, 1, 1),
                                sluttDato = LocalDate.of(2024, 12, 31),
                                avtaleStatus = Avtale.Status.GJENNOMFORES,
                                opprettetTidspunkt = ZonedDateTime.of(
                                    LocalDateTime.of(2023, 1, 1, 0, 0, 0),
                                    ZoneId.of("Europe/Oslo"),
                                ),
                                endretTidspunkt = ZonedDateTime.of(
                                    LocalDateTime.of(2023, 1, 1, 0, 0, 0),
                                    ZoneId.of("Europe/Oslo"),
                                ),
                            ),
                        ),
                    ),
                ),
            )

            val config = createTestApplicationConfig(oauth, mockEngine).copy(
                arbeidsgiverTiltakCutOffDatoMapping = mapOf(
                    Avtale.Tiltakstype.ARBEIDSTRENING to LocalDate.of(2024, 1, 1),
                ),
            )
            withTestApplication(oauth, mockEngine, config) {
                val response = client.post("/api/v1/historikk") {
                    bearerAuth(oauth.issueToken(claims = tiltakshistorikkReadClaims()).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(TiltakshistorikkV1Request(identer = listOf(NorskIdent("12345678910")), maxAgeYears = null))
                }

                response.status shouldBe HttpStatusCode.OK

                response.body<TiltakshistorikkV1Response>().historikk shouldContainExactlyInAnyOrder listOf(
                    TiltakshistorikkV1Dto.ArenaDeltakelse(
                        norskIdent = NorskIdent("12345678910"),
                        id = ARENA_ARBEIDSTRENING_ID,
                        status = ArenaDeltakerStatus.GJENNOMFORES,
                        startDato = LocalDate.of(2023, 1, 1),
                        sluttDato = LocalDate.of(2023, 1, 31),
                        tittel = "Arbeidstrening hos Arrangør",
                        tiltakstype = TiltakshistorikkV1Dto.ArenaDeltakelse.Tiltakstype(
                            tiltakskode = "ARBTREN",
                            navn = "Arbeidstrening",
                        ),
                        gjennomforing = TiltakshistorikkV1Dto.Gjennomforing(
                            id = TestFixtures.Gjennomforing.arenaArbeidstrening.id,
                            navn = "Arbeidstrening hos Fretex",
                            deltidsprosent = 80f,
                        ),
                        arrangor = TiltakshistorikkV1Dto.Arrangor(
                            hovedenhet = null,
                            underenhet = TiltakshistorikkV1Dto.Virksomhet(Organisasjonsnummer("987654321"), "Arrangør"),
                        ),
                        deltidsprosent = 100f,
                        dagerPerUke = 5f,
                    ),
                    TiltakshistorikkV1Dto.ArenaDeltakelse(
                        norskIdent = NorskIdent("12345678910"),
                        id = UUID.fromString("4bf76cc3-ade9-45ef-b22b-5c4d3ceee185"),
                        status = ArenaDeltakerStatus.GJENNOMFORES,
                        startDato = LocalDate.of(2024, 2, 1),
                        sluttDato = LocalDate.of(2024, 2, 29),
                        tittel = "Mentor hos Arrangør",
                        tiltakstype = TiltakshistorikkV1Dto.ArenaDeltakelse.Tiltakstype(
                            tiltakskode = "MENTOR",
                            navn = "Mentor",
                        ),
                        gjennomforing = TiltakshistorikkV1Dto.Gjennomforing(
                            id = TestFixtures.Gjennomforing.arenaMentor.id,
                            navn = "Mentortiltak hos Joblearn",
                            deltidsprosent = 100f,
                        ),
                        arrangor = TiltakshistorikkV1Dto.Arrangor(
                            hovedenhet = null,
                            underenhet = TiltakshistorikkV1Dto.Virksomhet(Organisasjonsnummer("987654321"), "Arrangør"),
                        ),
                        deltidsprosent = 100f,
                        dagerPerUke = 5f,
                    ),
                    TiltakshistorikkV1Dto.ArenaDeltakelse(
                        norskIdent = NorskIdent("12345678910"),
                        id = ARENA_ENKEL_AMO_ID,
                        status = ArenaDeltakerStatus.GJENNOMFORES,
                        startDato = LocalDate.of(2024, 2, 1),
                        sluttDato = LocalDate.of(2024, 2, 29),
                        tittel = "Arbeidsmarkedsopplæring (AMO) hos Arrangør",
                        tiltakstype = TiltakshistorikkV1Dto.ArenaDeltakelse.Tiltakstype(
                            tiltakskode = "AMO",
                            navn = "Arbeidsmarkedsopplæring (AMO)",
                        ),
                        gjennomforing = TiltakshistorikkV1Dto.Gjennomforing(
                            id = TestFixtures.Gjennomforing.arenaAmo.id,
                            navn = "Enkelt-AMO hos Joblearn",
                            deltidsprosent = 100f,
                        ),
                        arrangor = TiltakshistorikkV1Dto.Arrangor(
                            hovedenhet = null,
                            underenhet = TiltakshistorikkV1Dto.Virksomhet(Organisasjonsnummer("987654321"), "Arrangør"),
                        ),
                        deltidsprosent = 100f,
                        dagerPerUke = 5f,
                    ),
                    TiltakshistorikkV1Dto.ArbeidsgiverAvtale(
                        norskIdent = NorskIdent("12345678910"),
                        startDato = LocalDate.of(2024, 1, 1),
                        sluttDato = LocalDate.of(2024, 12, 31),
                        id = TEAM_TILTAK_ARBEIDSTRENING_ID,
                        tittel = "Arbeidstrening hos Arbeidsgiver",
                        tiltakstype = TiltakshistorikkV1Dto.ArbeidsgiverAvtale.Tiltakstype(
                            tiltakskode = TiltakshistorikkV1Dto.ArbeidsgiverAvtale.Tiltakskode.ARBEIDSTRENING,
                            navn = "Arbeidstrening",
                        ),
                        status = ArbeidsgiverAvtaleStatus.GJENNOMFORES,
                        arbeidsgiver = TiltakshistorikkV1Dto.Virksomhet(
                            Organisasjonsnummer("876543210"),
                            "Arbeidsgiver",
                        ),
                    ),
                    TiltakshistorikkV1Dto.GruppetiltakDeltakelse(
                        norskIdent = NorskIdent("12345678910"),
                        id = TEAM_KOMET_GRUPPE_AMO_ID,
                        startDato = null,
                        sluttDato = null,
                        tittel = "Arbeidsmarkedsopplæring (gruppe) hos Arrangør",
                        status = DeltakerStatus(
                            type = DeltakerStatusType.VENTER_PA_OPPSTART,
                            aarsak = null,
                            opprettetDato = LocalDateTime.of(2002, 3, 1, 0, 0),
                        ),
                        tiltakstype = TiltakshistorikkV1Dto.GruppetiltakDeltakelse.Tiltakstype(
                            tiltakskode = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
                            navn = "Arbeidsmarkedsopplæring (gruppe)",
                        ),
                        gjennomforing = TiltakshistorikkV1Dto.Gjennomforing(
                            id = TestFixtures.Gjennomforing.gruppeAmo.id,
                            navn = "Gruppe AMO",
                            deltidsprosent = 80f,
                        ),
                        arrangor = TiltakshistorikkV1Dto.Arrangor(
                            hovedenhet = null,
                            underenhet = TiltakshistorikkV1Dto.Virksomhet(Organisasjonsnummer("987654321"), "Arrangør"),
                        ),
                        deltidsprosent = 100f,
                        dagerPerUke = 5f,
                    ),
                )
            }
        }

        test("Filtrerer vekk enkeltplassdeltakelser fra Arena når deltakelsene har sluttdato etter cutoff-dato") {
            val mockEngine = mockTiltakDatadeling(
                response = GraphqlResponse(
                    data = GetAvtalerForPersonResponse(
                        avtalerForPerson = listOf(
                            Avtale(
                                avtaleId = TEAM_TILTAK_ARBEIDSTRENING_ID,
                                avtaleNr = 1,
                                deltakerFnr = NorskIdent("12345678910"),
                                bedriftNr = "876543210",
                                tiltakstype = Avtale.Tiltakstype.ARBEIDSTRENING,
                                startDato = LocalDate.of(2024, 1, 1),
                                sluttDato = LocalDate.of(2024, 12, 31),
                                avtaleStatus = Avtale.Status.GJENNOMFORES,
                                opprettetTidspunkt = ZonedDateTime.of(
                                    LocalDateTime.of(2023, 1, 1, 0, 0, 0),
                                    ZoneId.of("Europe/Oslo"),
                                ),
                                endretTidspunkt = ZonedDateTime.of(
                                    LocalDateTime.of(2023, 1, 1, 0, 0, 0),
                                    ZoneId.of("Europe/Oslo"),
                                ),
                            ),
                        ),
                    ),
                ),
            )

            val config = createTestApplicationConfig(oauth, mockEngine).copy(
                arbeidsgiverTiltakCutOffDatoMapping = mapOf(
                    Avtale.Tiltakstype.ARBEIDSTRENING to LocalDate.of(2023, 1, 31),
                    Avtale.Tiltakstype.MENTOR to LocalDate.of(2024, 1, 31),
                ),
            )
            withTestApplication(oauth, mockEngine, config) {
                val response = client.post("/api/v1/historikk") {
                    bearerAuth(oauth.issueToken(claims = tiltakshistorikkReadClaims()).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(TiltakshistorikkV1Request(identer = listOf(NorskIdent("12345678910")), maxAgeYears = null))
                }

                response.status shouldBe HttpStatusCode.OK

                val historikk = response.body<TiltakshistorikkV1Response>().historikk.map { it.id }

                historikk shouldContainExactlyInAnyOrder listOf(
                    TEAM_TILTAK_ARBEIDSTRENING_ID,
                    ARENA_ENKEL_AMO_ID,
                    TEAM_KOMET_GRUPPE_AMO_ID,
                )
            }
        }

        test("filtrerer vekk historikk som er eldre enn maxAgeYears") {
            val mockEngine = mockTiltakDatadeling(
                response = GraphqlResponse(
                    data = GetAvtalerForPersonResponse(
                        avtalerForPerson = listOf(),
                    ),
                ),
            )

            withTestApplication(oauth, mockEngine) {
                val response = client.post("/api/v1/historikk") {
                    bearerAuth(oauth.issueToken(claims = tiltakshistorikkReadClaims()).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(TiltakshistorikkV1Request(identer = listOf(NorskIdent("12345678910")), maxAgeYears = 15))
                }

                response.status shouldBe HttpStatusCode.OK

                val historikk = response.body<TiltakshistorikkV1Response>().historikk.map { it.id }

                historikk shouldContainExactlyInAnyOrder listOf(
                    ARENA_ENKEL_AMO_ID,
                    ARENA_MENTOR_ID,
                    ARENA_ARBEIDSTRENING_ID,
                )
            }
        }
    }
})

private fun tiltakshistorikkReadClaims(): Map<String, List<String>> {
    return mapOf("roles" to TiltakshistorikkRead.requiredRoles)
}

private fun mockTiltakDatadeling(
    response: GraphqlResponse<GetAvtalerForPersonResponse> = GraphqlResponse(
        data = GetAvtalerForPersonResponse(avtalerForPerson = listOf()),
    ),
): MockEngine {
    return createMockEngine {
        post("/tiltak-datadeling/graphql") { _: HttpRequestData ->
            val serializer = GraphqlResponse.serializer(GetAvtalerForPersonResponse.serializer())
            respondJson(response, serializer)
        }
    }
}

private fun inititalizeData(db: TiltakshistorikkDatabase) = db.session {
    val arrangor = TestFixtures.Virksomhet.arrangor
    queries.virksomhet.upsert(arrangor)

    val arbeidsgiver = TestFixtures.Virksomhet.arbeidsgiver
    queries.virksomhet.upsert(arbeidsgiver)

    val arenaArbeidstrening = TestFixtures.Gjennomforing.arenaArbeidstrening
    queries.arenaGjennomforing.upsert(arenaArbeidstrening)

    val arbeidstrening = TiltakshistorikkArenaDeltaker(
        id = ARENA_ARBEIDSTRENING_ID,
        arenaGjennomforingId = arenaArbeidstrening.id,
        norskIdent = NorskIdent("12345678910"),
        status = ArenaDeltakerStatus.GJENNOMFORES,
        startDato = LocalDateTime.of(2023, 1, 1, 0, 0, 0),
        sluttDato = LocalDateTime.of(2023, 1, 31, 0, 0, 0),
        arenaRegDato = LocalDateTime.of(2024, 1, 1, 0, 0, 0),
        arenaModDato = LocalDateTime.of(2024, 1, 1, 0, 0, 0),
        dagerPerUke = 5.0,
        deltidsprosent = 100.0,
    )
    queries.arenaDeltaker.upsertArenaDeltaker(arbeidstrening)

    val arenaMentor = TestFixtures.Gjennomforing.arenaMentor
    queries.arenaGjennomforing.upsert(arenaMentor)

    val mentor = TiltakshistorikkArenaDeltaker(
        id = ARENA_MENTOR_ID,
        arenaGjennomforingId = arenaMentor.id,
        norskIdent = NorskIdent("12345678910"),
        status = ArenaDeltakerStatus.GJENNOMFORES,
        startDato = LocalDateTime.of(2024, 2, 1, 0, 0, 0),
        sluttDato = LocalDateTime.of(2024, 2, 29, 0, 0, 0),
        arenaRegDato = LocalDateTime.of(2024, 1, 1, 0, 0, 0),
        arenaModDato = LocalDateTime.of(2024, 1, 1, 0, 0, 0),
        dagerPerUke = 5.0,
        deltidsprosent = 100.0,
    )
    queries.arenaDeltaker.upsertArenaDeltaker(mentor)

    val arenaAmo = TestFixtures.Gjennomforing.arenaAmo
    queries.arenaGjennomforing.upsert(arenaAmo)

    val enkeltAMO = TiltakshistorikkArenaDeltaker(
        id = ARENA_ENKEL_AMO_ID,
        arenaGjennomforingId = arenaAmo.id,
        norskIdent = NorskIdent("12345678910"),
        status = ArenaDeltakerStatus.GJENNOMFORES,
        startDato = LocalDateTime.of(2024, 2, 1, 0, 0, 0),
        sluttDato = LocalDateTime.of(2024, 2, 29, 0, 0, 0),
        arenaRegDato = LocalDateTime.of(2024, 1, 1, 0, 0, 0),
        arenaModDato = LocalDateTime.of(2024, 1, 1, 0, 0, 0),
        dagerPerUke = 5.0,
        deltidsprosent = 100.0,
    )
    queries.arenaDeltaker.upsertArenaDeltaker(enkeltAMO)

    val tiltak = TestFixtures.Gjennomforing.gruppeAmo
    queries.gjennomforing.upsert(tiltak.toGjennomforingDbo())

    val amtDeltaker = AmtDeltakerV1Dto(
        id = TEAM_KOMET_GRUPPE_AMO_ID,
        gjennomforingId = tiltak.id,
        personIdent = "12345678910",
        startDato = null,
        sluttDato = null,
        status = DeltakerStatus(
            type = DeltakerStatusType.VENTER_PA_OPPSTART,
            aarsak = null,
            opprettetDato = LocalDateTime.of(2002, 3, 1, 0, 0, 0),
        ),
        registrertDato = LocalDateTime.of(2002, 3, 1, 0, 0, 0),
        endretDato = LocalDateTime.of(2002, 3, 1, 0, 0, 0),
        dagerPerUke = 5f,
        prosentStilling = 100f,
        deltakelsesmengder = listOf(),
    )
    queries.kometDeltaker.upsertKometDeltaker(amtDeltaker)
}
