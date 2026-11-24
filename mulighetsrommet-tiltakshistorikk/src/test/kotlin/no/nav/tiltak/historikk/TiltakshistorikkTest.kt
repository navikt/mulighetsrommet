package no.nav.tiltak.historikk

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.ArenaDeltakerStatus
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.model.DeltakerHistorikkilde
import no.nav.tiltak.historikk.plugins.ACCESS_AS_APPLICATION
import no.nav.tiltak.historikk.plugins.TiltakshistorikkReadRoles
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

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
            withTestApplication(oauth) {
                val response = client.post("/api/v1/historikk") {
                    contentType(ContentType.Application.Json)
                    setBody(TiltakshistorikkV1Request(identer = listOf(NorskIdent("12345678910"))))
                }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("unauthorized når riktige roles mangler") {
            withTestApplication(oauth) {
                val response = client.post("/api/v1/historikk") {
                    bearerAuth(oauth.issueToken(claims = mapOf("roles" to listOf(ACCESS_AS_APPLICATION))).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(TiltakshistorikkV1Request(identer = listOf(NorskIdent("12345678910"))))
                }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("tom historikk når det ikke finnes noen deltakelser for gitt ident") {
            withTestApplication(oauth) {
                val response = client.post("/api/v1/historikk") {
                    bearerAuth(oauth.issueToken(claims = tiltakshistorikkReadClaims()).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(TiltakshistorikkV1Request(identer = listOf(NorskIdent("22345623456"))))
                }

                response.status shouldBe HttpStatusCode.OK

                response.body<TiltakshistorikkV1Response>().historikk.shouldBeEmpty()
            }
        }

        test("Henter arbeidsgiveravtaler fra Arena når de er avsluttet før cutoff-dato") {
            val config = createTestApplicationConfig(oauth).copy(
                arbeidsgiverTiltakCutOffDatoMapping = mapOf(
                    Tiltakskode.ARBEIDSTRENING to LocalDate.of(2024, 1, 1),
                ),
            )
            withTestApplication(oauth, config = config) {
                val response = client.post("/api/v1/historikk") {
                    bearerAuth(oauth.issueToken(claims = tiltakshistorikkReadClaims()).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(TiltakshistorikkV1Request(identer = listOf(NorskIdent("12345678910"))))
                }

                response.status shouldBe HttpStatusCode.OK

                response.body<TiltakshistorikkV1Response>().historikk shouldContainExactlyInAnyOrder listOf(
                    TiltakshistorikkV1Dto.ArenaDeltakelse(
                        norskIdent = NorskIdent("12345678910"),
                        id = ARENA_ARBEIDSTRENING_ID,
                        arenaId = 1,
                        status = ArenaDeltakerStatus.GJENNOMFORES,
                        startDato = LocalDate.of(2023, 1, 1),
                        sluttDato = LocalDate.of(2023, 1, 31),
                        opprettetTidspunkt = Instant.parse("2023-12-31T23:00:00Z"),
                        oppdatertTidspunkt = Instant.parse("2023-12-31T23:00:00Z"),
                        tittel = "Arbeidstrening hos Arrangør",
                        tiltakstype = TiltakshistorikkV1Dto.ArenaDeltakelse.Tiltakstype(
                            tiltakskode = "ARBTREN",
                            navn = "Arbeidstrening",
                        ),
                        gjennomforing = TiltakshistorikkV1Dto.Gjennomforing(
                            id = TestFixtures.ArenaGjennomforing.Arbeidstrening.id,
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
                        arenaId = 2,
                        status = ArenaDeltakerStatus.GJENNOMFORES,
                        startDato = LocalDate.of(2024, 2, 1),
                        sluttDato = LocalDate.of(2024, 2, 29),
                        opprettetTidspunkt = Instant.parse("2023-12-31T23:00:00Z"),
                        oppdatertTidspunkt = Instant.parse("2023-12-31T23:00:00Z"),
                        tittel = "Mentor hos Arrangør",
                        tiltakstype = TiltakshistorikkV1Dto.ArenaDeltakelse.Tiltakstype(
                            tiltakskode = "MENTOR",
                            navn = "Mentor",
                        ),
                        gjennomforing = TiltakshistorikkV1Dto.Gjennomforing(
                            id = TestFixtures.ArenaGjennomforing.Mentor.id,
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
                        arenaId = 3,
                        status = ArenaDeltakerStatus.GJENNOMFORES,
                        startDato = LocalDate.of(2024, 2, 1),
                        sluttDato = LocalDate.of(2024, 2, 29),
                        opprettetTidspunkt = Instant.parse("2023-12-31T23:00:00Z"),
                        oppdatertTidspunkt = Instant.parse("2023-12-31T23:00:00Z"),
                        tittel = "Arbeidsmarkedsopplæring (enkeltplass) hos Arrangør",
                        tiltakstype = TiltakshistorikkV1Dto.ArenaDeltakelse.Tiltakstype(
                            tiltakskode = "ENKELAMO",
                            navn = "Arbeidsmarkedsopplæring (enkeltplass)",
                        ),
                        gjennomforing = TiltakshistorikkV1Dto.Gjennomforing(
                            id = TestFixtures.ArenaGjennomforing.EnkelAmo.id,
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
                    TiltakshistorikkV1Dto.TeamTiltakAvtale(
                        norskIdent = NorskIdent("12345678910"),
                        startDato = LocalDate.of(2024, 1, 1),
                        sluttDato = LocalDate.of(2024, 12, 31),
                        opprettetTidspunkt = Instant.parse("2022-12-31T23:00:00Z"),
                        oppdatertTidspunkt = Instant.parse("2022-12-31T23:00:00Z"),
                        id = TEAM_TILTAK_ARBEIDSTRENING_ID,
                        tittel = "Arbeidstrening hos Arbeidsgiver",
                        tiltakstype = TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakstype(
                            tiltakskode = TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.ARBEIDSTRENING,
                            navn = "Arbeidstrening",
                        ),
                        status = TiltakshistorikkV1Dto.TeamTiltakAvtale.Status.GJENNOMFORES,
                        stillingsprosent = 77f,
                        dagerPerUke = 2.5f,
                        arbeidsgiver = TiltakshistorikkV1Dto.Virksomhet(
                            Organisasjonsnummer("876543210"),
                            "Arbeidsgiver",
                        ),
                    ),
                    TiltakshistorikkV1Dto.TeamKometDeltakelse(
                        norskIdent = NorskIdent("12345678910"),
                        id = TEAM_KOMET_GRUPPE_AMO_ID,
                        startDato = null,
                        sluttDato = null,
                        opprettetTidspunkt = Instant.parse("2002-02-28T23:00:00Z"),
                        oppdatertTidspunkt = Instant.parse("2002-02-28T23:00:00Z"),
                        tittel = "Arbeidsmarkedsopplæring (gruppe) hos Arrangør",
                        status = TiltakshistorikkV1Dto.TeamKometDeltakelse.Status(
                            type = DeltakerStatusType.VENTER_PA_OPPSTART,
                            aarsak = null,
                            opprettetTidspunkt = LocalDateTime.of(2002, 3, 1, 0, 0),
                        ),
                        tiltakstype = TiltakshistorikkV1Dto.TeamKometDeltakelse.Tiltakstype(
                            tiltakskode = TiltakshistorikkV1Dto.TeamKometDeltakelse.Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
                            navn = "Arbeidsmarkedsopplæring (gruppe)",
                        ),
                        gjennomforing = TiltakshistorikkV1Dto.Gjennomforing(
                            id = TestFixtures.Gjennomforing.GruppeAmo.id,
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

        test("Filtrerer vekk arbeidsgiveravtaler fra Arena når deltakelsene har sluttdato etter cutoff-dato") {
            val config = createTestApplicationConfig(oauth).copy(
                arbeidsgiverTiltakCutOffDatoMapping = mapOf(
                    Tiltakskode.ARBEIDSTRENING to LocalDate.of(2023, 1, 31),
                    Tiltakskode.MENTOR to LocalDate.of(2024, 1, 31),
                ),
            )
            withTestApplication(oauth, config = config) {
                val response = client.post("/api/v1/historikk") {
                    bearerAuth(oauth.issueToken(claims = tiltakshistorikkReadClaims()).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(TiltakshistorikkV1Request(identer = listOf(NorskIdent("12345678910"))))
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

        test("bruker appens kildevalg for enkeltplasser i historikk-endepunktet") {
            val kometDeltakerId = UUID.fromString("50717dbd-370a-4b54-a442-c0c3390266bd")
            db.session {
                queries.gjennomforing.upsert(TestFixtures.Gjennomforing.EnkelAmo)
                queries.kometDeltaker.upsertKometDeltaker(
                    TestFixtures.Deltaker.EnkelAmo.copy(
                        id = kometDeltakerId,
                        personIdent = "12345678910",
                    ),
                )
            }

            forAll(
                row(
                    createTestApplicationConfig(oauth).copy(
                        deltakerHistorikkilder = mapOf(
                            Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to DeltakerHistorikkilde.KOMET,
                        ),
                    ),
                    listOf(
                        ARENA_ARBEIDSTRENING_ID,
                        ARENA_MENTOR_ID,
                        TEAM_KOMET_GRUPPE_AMO_ID,
                        kometDeltakerId,
                    ),
                ),
                row(
                    createTestApplicationConfig(oauth).copy(
                        deltakerHistorikkilder = mapOf(
                            Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to DeltakerHistorikkilde.ARENA,
                        ),
                    ),
                    listOf(
                        ARENA_ARBEIDSTRENING_ID,
                        ARENA_MENTOR_ID,
                        TEAM_KOMET_GRUPPE_AMO_ID,
                        ARENA_ENKEL_AMO_ID,
                    ),
                ),
            ) { config, ids ->
                withTestApplication(oauth, config = config) {
                    val response = client.post("/api/v1/historikk") {
                        bearerAuth(oauth.issueToken(claims = tiltakshistorikkReadClaims()).serialize())
                        contentType(ContentType.Application.Json)
                        setBody(TiltakshistorikkV1Request(identer = listOf(NorskIdent("12345678910"))))
                    }

                    response.status shouldBe HttpStatusCode.OK
                    response.body<TiltakshistorikkV1Response>().historikk.map { it.id } shouldContainExactlyInAnyOrder ids
                }
            }
        }
    }
})

private fun tiltakshistorikkReadClaims(): Map<String, List<String>> {
    return mapOf("roles" to TiltakshistorikkReadRoles)
}

private fun inititalizeData(db: TiltakshistorikkDatabase) = db.session {
    val arrangor = TestFixtures.Virksomhet.Arrangor
    queries.virksomhet.upsert(arrangor)

    val arbeidsgiver = TestFixtures.Virksomhet.Arbeidsgiver
    queries.virksomhet.upsert(arbeidsgiver)

    val arbeidstrengingTiltakstype = TestFixtures.Tiltakstype.Arbeidstrening
    queries.tiltakstype.upsert(arbeidstrengingTiltakstype)

    val arenaArbeidstrening = TestFixtures.ArenaGjennomforing.Arbeidstrening
    queries.arenaGjennomforing.upsert(arenaArbeidstrening)

    val arbeidstrening = TestFixtures.ArenaDeltaker.Arbeidstrening.copy(
        id = ARENA_ARBEIDSTRENING_ID,
        arenaDeltakerId = 1,
        norskIdent = NorskIdent("12345678910"),
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2023, 1, 31),
        arenaRegDato = Instant.parse("2023-12-31T23:00:00Z"),
        arenaModDato = Instant.parse("2023-12-31T23:00:00Z"),
    )
    queries.arenaDeltaker.upsertArenaDeltaker(arbeidstrening)

    val mentorTiltakstype = TestFixtures.Tiltakstype.Mentor
    queries.tiltakstype.upsert(mentorTiltakstype)
    val arenaMentor = TestFixtures.ArenaGjennomforing.Mentor
    queries.arenaGjennomforing.upsert(arenaMentor)

    val mentor = TestFixtures.ArenaDeltaker.Arbeidstrening.copy(
        id = ARENA_MENTOR_ID,
        arenaGjennomforingId = arenaMentor.id,
        arenaDeltakerId = 2,
        norskIdent = NorskIdent("12345678910"),
        startDato = LocalDate.of(2024, 2, 1),
        sluttDato = LocalDate.of(2024, 2, 29),
        arenaRegDato = Instant.parse("2023-12-31T23:00:00Z"),
        arenaModDato = Instant.parse("2023-12-31T23:00:00Z"),
    )
    queries.arenaDeltaker.upsertArenaDeltaker(mentor)

    queries.tiltakstype.upsert(TestFixtures.Tiltakstype.EnkelAmo)

    val arenaEnkeltAmo = TestFixtures.ArenaGjennomforing.EnkelAmo
    queries.arenaGjennomforing.upsert(arenaEnkeltAmo)

    val enkeltAMO = TestFixtures.ArenaDeltaker.EnkelAmo.copy(
        id = ARENA_ENKEL_AMO_ID,
        arenaDeltakerId = 3,
        norskIdent = NorskIdent("12345678910"),
        startDato = LocalDate.of(2024, 2, 1),
        sluttDato = LocalDate.of(2024, 2, 29),
        arenaRegDato = Instant.parse("2023-12-31T23:00:00Z"),
        arenaModDato = Instant.parse("2023-12-31T23:00:00Z"),
    )
    queries.arenaDeltaker.upsertArenaDeltaker(enkeltAMO)

    val teamTiltakArbeidstrening = TestFixtures.Avtale.Arbeidstrening.copy(
        avtaleId = TEAM_TILTAK_ARBEIDSTRENING_ID,
        norskIdent = NorskIdent("12345678910"),
        stillingsprosent = 77f,
        dagerPerUke = 2.5f,
        opprettetTidspunkt = Instant.parse("2022-12-31T23:00:00Z"),
        oppdatertTidspunkt = Instant.parse("2022-12-31T23:00:00Z"),
    )
    queries.arbeidsgiverAvtale.upsert(teamTiltakArbeidstrening)

    val gruppeAmoTiltakstype = TestFixtures.Tiltakstype.GruppeAmo
    queries.tiltakstype.upsert(gruppeAmoTiltakstype)

    val gruppeAmoGjennomforing = TestFixtures.Gjennomforing.GruppeAmo
    queries.gjennomforing.upsert(gruppeAmoGjennomforing)

    val kometDeltaker = TestFixtures.Deltaker.GruppeAmo.copy(
        id = TEAM_KOMET_GRUPPE_AMO_ID,
        personIdent = "12345678910",
        statusOpprettetTidspunkt = LocalDateTime.of(2002, 3, 1, 0, 0, 0),
        opprettetTidspunkt = LocalDateTime.of(2002, 3, 1, 0, 0, 0),
        oppdatertTidspunkt = LocalDateTime.of(2002, 3, 1, 0, 0, 0),
        prosentStilling = 100f,
        dagerPerUke = 5f,
    )
    queries.kometDeltaker.upsertKometDeltaker(kometDeltaker)
}
