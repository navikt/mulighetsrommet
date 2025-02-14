package no.nav.tiltak.historikk

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import no.nav.amt.model.AmtDeltakerV1Dto
import no.nav.mulighetsrommet.arena.ArenaDeltakerDbo
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.model.*
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.tiltak.historikk.clients.Avtale
import no.nav.tiltak.historikk.clients.GetAvtalerForPersonResponse
import no.nav.tiltak.historikk.clients.GraphqlResponse
import no.nav.tiltak.historikk.repositories.DeltakerRepository
import no.nav.tiltak.historikk.repositories.GruppetiltakRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

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
        beforeAny {
            inititalizeData(database)
        }

        test("unauthorized når token mangler") {
            val mockEngine = mockTiltakDatadeling()

            withTestApplication(oauth, mockEngine) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val response = client.post("/api/v1/historikk") {
                    contentType(ContentType.Application.Json)
                    setBody(TiltakshistorikkRequest(identer = listOf(NorskIdent("12345678910")), maxAgeYears = null))
                }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("tom historikk når det ikke finnes noen deltakelser for gitt ident") {
            val mockEngine = mockTiltakDatadeling()

            withTestApplication(oauth, mockEngine) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val response = client.post("/api/v1/historikk") {
                    bearerAuth(oauth.issueToken().serialize())
                    contentType(ContentType.Application.Json)
                    setBody(TiltakshistorikkRequest(identer = listOf(NorskIdent("22345623456")), maxAgeYears = null))
                }

                response.status shouldBe HttpStatusCode.OK

                response.body<TiltakshistorikkResponse>().historikk.shouldBeEmpty()
            }
        }

        test("Henter enkeltplassdeltakelser fra Arena når de er avsluttet før cutoff-dato") {
            val avtaleId = UUID.fromString("9dea48c1-d494-4664-9427-bdb20a6f265f")
            val mockEngine = mockTiltakDatadeling(
                response = GraphqlResponse(
                    data = GetAvtalerForPersonResponse(
                        avtalerForPerson = listOf(
                            Avtale(
                                avtaleId = avtaleId,
                                avtaleNr = 1,
                                deltakerFnr = NorskIdent("12345678910"),
                                bedriftNr = Organisasjonsnummer("123456789"),
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
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val response = client.post("/api/v1/historikk") {
                    bearerAuth(oauth.issueToken().serialize())
                    contentType(ContentType.Application.Json)
                    setBody(TiltakshistorikkRequest(identer = listOf(NorskIdent("12345678910")), maxAgeYears = null))
                }

                response.status shouldBe HttpStatusCode.OK

                response.body<TiltakshistorikkResponse>().historikk shouldContainExactlyInAnyOrder listOf(
                    Tiltakshistorikk.ArenaDeltakelse(
                        norskIdent = NorskIdent("12345678910"),
                        id = UUID.fromString("05fae1e4-4dcb-4b29-a8e6-7f6b6b52d617"),
                        arenaTiltakskode = "ARBTREN",
                        status = ArenaDeltakerStatus.GJENNOMFORES,
                        startDato = LocalDate.of(2023, 1, 1),
                        sluttDato = LocalDate.of(2023, 1, 31),
                        beskrivelse = "Arbeidstrening hos Fretex",
                        arrangor = Tiltakshistorikk.Arrangor(Organisasjonsnummer("123123123")),
                    ),
                    Tiltakshistorikk.ArbeidsgiverAvtale(
                        norskIdent = NorskIdent("12345678910"),
                        startDato = LocalDate.of(2024, 1, 1),
                        sluttDato = LocalDate.of(2024, 12, 31),
                        id = avtaleId,
                        avtaleId = avtaleId,
                        tiltakstype = Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.ARBEIDSTRENING,
                        status = ArbeidsgiverAvtaleStatus.GJENNOMFORES,
                        arbeidsgiver = Tiltakshistorikk.Arbeidsgiver(Organisasjonsnummer("123456789")),
                    ),
                    Tiltakshistorikk.ArenaDeltakelse(
                        norskIdent = NorskIdent("12345678910"),
                        id = UUID.fromString("ddb13a2b-cd65-432d-965c-9167938a26a4"),
                        arenaTiltakskode = "AMO",
                        status = ArenaDeltakerStatus.GJENNOMFORES,
                        startDato = LocalDate.of(2024, 2, 1),
                        sluttDato = LocalDate.of(2024, 2, 29),
                        beskrivelse = "Enkelt-AMO hos Joblearn",
                        arrangor = Tiltakshistorikk.Arrangor(Organisasjonsnummer("123123123")),
                    ),
                    Tiltakshistorikk.GruppetiltakDeltakelse(
                        norskIdent = NorskIdent("12345678910"),
                        id = UUID.fromString("6d54228f-534f-4b4b-9160-65eae26a3b06"),
                        startDato = null,
                        sluttDato = null,
                        status = DeltakerStatus(
                            type = DeltakerStatus.Type.VENTER_PA_OPPSTART,
                            aarsak = null,
                            opprettetDato = LocalDateTime.of(2002, 3, 1, 0, 0),
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

        test("Filtrerer vekk enkeltplassdeltakelser fra Arena når deltakelsene har sluttdato etter cutoff-dato") {
            val avtaleId = UUID.fromString("9dea48c1-d494-4664-9427-bdb20a6f265f")
            val mockEngine = mockTiltakDatadeling(
                response = GraphqlResponse(
                    data = GetAvtalerForPersonResponse(
                        avtalerForPerson = listOf(
                            Avtale(
                                avtaleId = avtaleId,
                                avtaleNr = 1,
                                deltakerFnr = NorskIdent("12345678910"),
                                bedriftNr = Organisasjonsnummer("123456789"),
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
                    Avtale.Tiltakstype.MENTOR to LocalDate.of(2025, 1, 31),
                ),
            )
            withTestApplication(oauth, mockEngine, config) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val response = client.post("/api/v1/historikk") {
                    bearerAuth(oauth.issueToken().serialize())
                    contentType(ContentType.Application.Json)
                    setBody(TiltakshistorikkRequest(identer = listOf(NorskIdent("12345678910")), maxAgeYears = null))
                }

                response.status shouldBe HttpStatusCode.OK

                val historikk = response.body<TiltakshistorikkResponse>().historikk.map { it.id }

                historikk shouldContainExactlyInAnyOrder listOf(
                    avtaleId,
                    UUID.fromString("4bf76cc3-ade9-45ef-b22b-5c4d3ceee185"),
                    UUID.fromString("ddb13a2b-cd65-432d-965c-9167938a26a4"),
                    UUID.fromString("6d54228f-534f-4b4b-9160-65eae26a3b06"),
                )
            }
        }

        test("filtrerer vekk historikk som er eldre enn maxAgeYears") {
            val mockEngine = mockTiltakDatadeling(
                response = GraphqlResponse(
                    data = GetAvtalerForPersonResponse(
                        avtalerForPerson = listOf(
                            Avtale(
                                avtaleId = UUID.randomUUID(),
                                avtaleNr = 1,
                                deltakerFnr = NorskIdent("12345678910"),
                                bedriftNr = Organisasjonsnummer("123456789"),
                                tiltakstype = Avtale.Tiltakstype.ARBEIDSTRENING,
                                startDato = LocalDate.of(2000, 1, 1),
                                sluttDato = LocalDate.of(2000, 12, 31),
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
                    Avtale.Tiltakstype.ARBEIDSTRENING to LocalDate.of(2025, 2, 1),
                ),
            )
            withTestApplication(oauth, mockEngine, config) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val response = client.post("/api/v1/historikk") {
                    bearerAuth(oauth.issueToken().serialize())
                    contentType(ContentType.Application.Json)
                    setBody(TiltakshistorikkRequest(identer = listOf(NorskIdent("12345678910")), maxAgeYears = 15))
                }

                response.status shouldBe HttpStatusCode.OK

                val historikk = response.body<TiltakshistorikkResponse>().historikk.map { it.id }

                historikk shouldContainExactlyInAnyOrder listOf(
                    UUID.fromString("ddb13a2b-cd65-432d-965c-9167938a26a4"),
                    UUID.fromString("05fae1e4-4dcb-4b29-a8e6-7f6b6b52d617"),
                )
            }
        }
    }
})

private fun mockTiltakDatadeling(
    response: GraphqlResponse<GetAvtalerForPersonResponse> = GraphqlResponse(
        data = GetAvtalerForPersonResponse(avtalerForPerson = listOf()),
    ),
): MockEngine {
    return createMockEngine {
        post("/tiltak-datadeling/graphql") {
            val serializer = GraphqlResponse.serializer(GetAvtalerForPersonResponse.serializer())
            respondJson(response, serializer)
        }
    }
}

private fun inititalizeData(database: FlywayDatabaseTestListener) {
    val gruppetiltak = GruppetiltakRepository(database.db)
    val deltakere = DeltakerRepository(database.db)

    val tiltak = TiltaksgjennomforingEksternV1Dto(
        id = UUID.fromString("566b89b0-4ed0-43cf-84a8-39085428f7e6"),
        tiltakstype = TiltaksgjennomforingEksternV1Dto.Tiltakstype(
            id = UUID.fromString("af6f4034-08da-4bd4-8735-ffd883e8aab7"),
            navn = "Gruppe AMO",
            arenaKode = "GRUPPEAMO",
            tiltakskode = Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
        ),
        navn = "Gruppe AMO",
        virksomhetsnummer = "123123123",
        startDato = LocalDate.now(),
        sluttDato = null,
        status = GjennomforingStatus.GJENNOMFORES,
        oppstart = GjennomforingOppstartstype.FELLES,
        tilgjengeligForArrangorFraOgMedDato = null,
        apentForPamelding = true,
        antallPlasser = 10,
    )
    gruppetiltak.upsert(tiltak)

    val arbeidstrening = ArenaDeltakerDbo(
        id = UUID.fromString("05fae1e4-4dcb-4b29-a8e6-7f6b6b52d617"),
        norskIdent = NorskIdent("12345678910"),
        arenaTiltakskode = "ARBTREN",
        status = ArenaDeltakerStatus.GJENNOMFORES,
        startDato = LocalDateTime.of(2023, 1, 1, 0, 0, 0),
        sluttDato = LocalDateTime.of(2023, 1, 31, 0, 0, 0),
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

    val enkeltAMO = ArenaDeltakerDbo(
        id = UUID.fromString("ddb13a2b-cd65-432d-965c-9167938a26a4"),
        norskIdent = NorskIdent("12345678910"),
        arenaTiltakskode = "AMO",
        status = ArenaDeltakerStatus.GJENNOMFORES,
        startDato = LocalDateTime.of(2024, 2, 1, 0, 0, 0),
        sluttDato = LocalDateTime.of(2024, 2, 29, 0, 0, 0),
        beskrivelse = "Enkelt-AMO hos Joblearn",
        arrangorOrganisasjonsnummer = Organisasjonsnummer("123123123"),
        registrertIArenaDato = LocalDateTime.of(2024, 1, 1, 0, 0, 0),
    )
    deltakere.upsertArenaDeltaker(enkeltAMO)

    val deltakelsesdato = LocalDateTime.of(2002, 3, 1, 0, 0, 0)
    val amtDeltaker = AmtDeltakerV1Dto(
        id = UUID.fromString("6d54228f-534f-4b4b-9160-65eae26a3b06"),
        gjennomforingId = tiltak.id,
        personIdent = "12345678910",
        startDato = null,
        sluttDato = null,
        status = DeltakerStatus(
            type = DeltakerStatus.Type.VENTER_PA_OPPSTART,
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
