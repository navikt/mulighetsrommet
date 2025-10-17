package no.nav.mulighetsrommet.api.gjennomforing.service

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.mockk
import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Innlandet
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Oslo
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Sagene
import no.nav.mulighetsrommet.api.gjennomforing.model.AvbrytGjennomforingAarsak
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingStatus
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV1Dto
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import java.time.LocalDate
import java.time.LocalDateTime

const val TEST_GJENNOMFORING_V1_TOPIC = "gjennomforing-v1"
const val TEST_GJENNOMFORING_V2_TOPIC = "gjennomforing-v2"

class GjennomforingServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    fun createService(): GjennomforingService = GjennomforingService(
        config = GjennomforingService.Config(TEST_GJENNOMFORING_V1_TOPIC, TEST_GJENNOMFORING_V2_TOPIC),
        db = database.db,
        navAnsattService = mockk(relaxed = true),
    )

    val domain = MulighetsrommetTestDomain(
        navEnheter = listOf(Innlandet, Oslo, Sagene, Gjovik),
    )

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    val bertilNavIdent = NavIdent("B123456")

    context("valideringsfeil ved opprettelse av gjennomføring") {
        test("får ikke opprettet gjennomføring som allerede er avsluttet") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1Request.copy(
                startDato = LocalDate.of(2023, 1, 1),
                sluttDato = LocalDate.of(2023, 1, 1),
            )

            val service = createService()

            service.upsert(gjennomforing, bertilNavIdent, today = LocalDate.of(2023, 1, 2))
                .shouldBeLeft().shouldContainAll(
                    listOf(FieldError("/navn", "Du kan ikke opprette en gjennomføring som er avsluttet")),
                )
        }
    }

    context("opprettelse av gjennomføring") {
        val service = createService()

        test("oppretting av gjennomføring blir lagret som et utgående kafka-record") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1Request

            service.upsert(gjennomforing, bertilNavIdent).shouldBeRight()

            database.run {
                shouldHaveKafkaProducerRecords(TEST_GJENNOMFORING_V1_TOPIC, 1).should { (record) ->
                    record.key shouldBe gjennomforing.id.toString().toByteArray()
                    Json.decodeFromString<TiltaksgjennomforingV1Dto>(record.value.decodeToString()).id shouldBe gjennomforing.id
                }

                shouldHaveKafkaProducerRecords(TEST_GJENNOMFORING_V2_TOPIC, 1).should { (record) ->
                    record.key shouldBe gjennomforing.id.toString().toByteArray()
                    val deserialized = Json.decodeFromString<TiltaksgjennomforingV2Dto>(record.value.decodeToString())
                    deserialized should beInstanceOf<TiltaksgjennomforingV2Dto.Gruppe>()
                    deserialized.id shouldBe gjennomforing.id
                }
            }
        }

        test("navEnheter uten fylke blir filtrert vekk") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1Request

            createService().upsert(
                gjennomforing.copy(
                    veilederinformasjon = gjennomforing.veilederinformasjon.copy(
                        navEnheter = listOf(
                            Innlandet.enhetsnummer,
                            Gjovik.enhetsnummer,
                            Sagene.enhetsnummer,
                        ),
                    ),
                ),
                bertilNavIdent,
            ).shouldBeRight().should {
                it.kontorstruktur.shouldHaveSize(1)
                it.kontorstruktur[0].kontorer.shouldHaveSize(1)
                it.kontorstruktur[0].kontorer[0].enhetsnummer shouldBe Gjovik.enhetsnummer
                it.kontorstruktur[0].region.enhetsnummer shouldBe Innlandet.enhetsnummer
            }
        }

        test("lagrer ikke duplikater som utgående kafka-records") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1Request

            service.upsert(gjennomforing, bertilNavIdent)
            service.upsert(gjennomforing, bertilNavIdent)

            database.run {
                shouldHaveKafkaProducerRecords(TEST_GJENNOMFORING_V1_TOPIC, 1)
                shouldHaveKafkaProducerRecords(TEST_GJENNOMFORING_V2_TOPIC, 1)
            }
        }

        test("Ingen administrator-notification hvis administratorer er samme som opprettet") {
            val navIdent = NavAnsattFixture.DonaldDuck.navIdent

            val gjennomforing = GjennomforingFixtures.Oppfolging1Request.copy(
                administratorer = listOf(navIdent),
            )
            service.upsert(gjennomforing, navIdent).shouldBeRight()

            database.assertTable("user_notification").isEmpty
        }

        test("Bare nye administratorer får notifikasjon når man endrer gjennomføring") {
            val identAnsatt1 = NavAnsattFixture.DonaldDuck.navIdent
            val identAnsatt2 = NavAnsattFixture.MikkeMus.navIdent

            val gjennomforing = GjennomforingFixtures.Oppfolging1Request.copy(
                administratorer = listOf(identAnsatt2, identAnsatt1),
            )
            service.upsert(gjennomforing, identAnsatt1).shouldBeRight()

            database.assertTable("user_notification")
                .hasNumberOfRows(1)
                .column("user_id")
                .containsValues(identAnsatt2.value)
        }
    }

    context("avbryte gjennomføring") {
        val service = createService()

        test("blir valideringsfeil hvis gjennomføringen ikke er aktiv") {
            val gjennomforing = GjennomforingFixtures.AFT1.copy(
                status = GjennomforingStatusType.AVSLUTTET,
            )

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.db)

            service.avbrytGjennomforing(
                gjennomforing.id,
                tidspunkt = LocalDateTime.now(),
                aarsakerOgForklaring = AarsakerOgForklaringRequest(
                    listOf(AvbrytGjennomforingAarsak.FEILREGISTRERING),
                    null,
                ),
                avbruttAv = bertilNavIdent,
            ).shouldBeLeft(
                listOf(FieldError.root("Gjennomføringen er allerede avsluttet")),
            )
        }

        test("blir valideringsfeil hvis gjennomføringen forsøkes avbrytes etter at sluttdato er passert") {
            val gjennomforing = GjennomforingFixtures.AFT1.copy(
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = LocalDate.of(2023, 7, 1),
                status = GjennomforingStatusType.AVSLUTTET,
            )

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.db)

            service.avbrytGjennomforing(
                gjennomforing.id,
                tidspunkt = LocalDate.of(2023, 7, 2).atStartOfDay(),
                aarsakerOgForklaring = AarsakerOgForklaringRequest(
                    listOf(AvbrytGjennomforingAarsak.FEILREGISTRERING),
                    null,
                ),
                avbruttAv = bertilNavIdent,
            ).shouldBeLeft(
                listOf(FieldError.root("Gjennomføringen er allerede avsluttet")),
            )
        }

        test("stenger gjennomføring, publiserer til kafka og skriver til endringshistorikken når gjennomføring avbrytes") {
            val gjennomforing = GjennomforingFixtures.AFT1.copy(
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = null,
            )

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.db)

            service.avbrytGjennomforing(
                gjennomforing.id,
                tidspunkt = LocalDate.of(2023, 7, 1).atStartOfDay(),
                aarsakerOgForklaring = AarsakerOgForklaringRequest(
                    listOf(AvbrytGjennomforingAarsak.FEILREGISTRERING),
                    null,
                ),
                avbruttAv = bertilNavIdent,
            ).shouldBeRight().should {
                it.status.shouldBeTypeOf<GjennomforingStatus.Avbrutt>()
                it.publisert shouldBe false
                it.apentForPamelding shouldBe false
            }

            database.run {
                shouldHaveKafkaProducerRecords(TEST_GJENNOMFORING_V1_TOPIC, 1).should { (record) ->
                    val decoded = Json.decodeFromString<TiltaksgjennomforingV1Dto>(record.value.decodeToString())
                    decoded.id shouldBe gjennomforing.id
                    decoded.apentForPamelding shouldBe false
                }
                shouldHaveKafkaProducerRecords(TEST_GJENNOMFORING_V2_TOPIC, 1).should { (record) ->
                    val decoded = Json.decodeFromString<TiltaksgjennomforingV2Dto>(record.value.decodeToString())
                    val gruppe = decoded.shouldBeInstanceOf<TiltaksgjennomforingV2Dto.Gruppe>()
                    gruppe.id shouldBe gjennomforing.id
                    gruppe.apentForPamelding shouldBe false
                }

                queries.endringshistorikk.getEndringshistorikk(DocumentClass.GJENNOMFORING, gjennomforing.id)
                    .shouldNotBeNull().entries.shouldHaveSize(1).first().should {
                        it.operation shouldBe "Gjennomføringen ble avbrutt"
                    }
            }
        }

        test("stenger gjennomføring og får status avlyst når gjennomføring avbrytes før start") {
            val gjennomforing = GjennomforingFixtures.AFT1.copy(
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = null,
            )

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.db)

            service.avbrytGjennomforing(
                gjennomforing.id,
                tidspunkt = LocalDate.of(2023, 6, 1).atStartOfDay(),
                aarsakerOgForklaring = AarsakerOgForklaringRequest(
                    listOf(AvbrytGjennomforingAarsak.FEILREGISTRERING),
                    null,
                ),
                avbruttAv = bertilNavIdent,
            )

            service.get(gjennomforing.id).shouldNotBeNull().should {
                it.status.shouldBeTypeOf<GjennomforingStatus.Avlyst>()
                it.publisert shouldBe false
                it.apentForPamelding shouldBe false
            }
        }
    }

    context("avslutte gjennomføring") {
        val service = createService()

        test("stenger gjennomføring, publiserer til kafka og skriver til endringshistorikken når gjennomføring avsluttes") {
            val gjennomforing = GjennomforingFixtures.AFT1.copy(
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = LocalDate.of(2023, 7, 1),
            )

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.db)

            service.avsluttGjennomforing(
                gjennomforing.id,
                LocalDate.of(2023, 7, 2).atStartOfDay(),
                bertilNavIdent,
            ) should {
                it.status.shouldBeTypeOf<GjennomforingStatus.Avsluttet>()
                it.publisert shouldBe false
                it.apentForPamelding shouldBe false
            }

            database.run {
                shouldHaveKafkaProducerRecords(TEST_GJENNOMFORING_V1_TOPIC, 1).should { (record) ->
                    val decoded = Json.decodeFromString<TiltaksgjennomforingV1Dto>(record.value.decodeToString())
                    decoded.id shouldBe gjennomforing.id
                    decoded.status shouldBe GjennomforingStatusType.AVSLUTTET
                }

                shouldHaveKafkaProducerRecords(TEST_GJENNOMFORING_V2_TOPIC, 1).should { (record) ->
                    val decoded = Json.decodeFromString<TiltaksgjennomforingV2Dto>(record.value.decodeToString())
                    val gruppe = decoded.shouldBeInstanceOf<TiltaksgjennomforingV2Dto.Gruppe>()
                    gruppe.id shouldBe gjennomforing.id
                    gruppe.status shouldBe GjennomforingStatusType.AVSLUTTET
                }

                queries.endringshistorikk.getEndringshistorikk(DocumentClass.GJENNOMFORING, gjennomforing.id)
                    .shouldNotBeNull().entries.shouldHaveSize(1).first().should {
                        it.operation shouldBe "Gjennomføringen ble avsluttet"
                    }
            }
        }
    }

    context("tilgjengelig for arrangør") {
        test("dato for tilgjengelig for arrangør blir publisert til kafka") {
            val tilgjengeligForArrangorDato = LocalDate.now().plusDays(1)
            val startDato = LocalDate.now().plusWeeks(1)

            val gjennomforing = GjennomforingFixtures.AFT1.copy(startDato = startDato)

            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.db)

            val service = createService()

            service.setTilgjengeligForArrangorDato(
                id = gjennomforing.id,
                tilgjengeligForArrangorDato = tilgjengeligForArrangorDato,
                navIdent = bertilNavIdent,
            ).shouldBeRight()

            database.run {
                shouldHaveKafkaProducerRecords(TEST_GJENNOMFORING_V1_TOPIC, 1).should { (record) ->
                    val decoded = Json.decodeFromString<TiltaksgjennomforingV1Dto>(record.value.decodeToString())
                    decoded.tilgjengeligForArrangorFraOgMedDato shouldBe tilgjengeligForArrangorDato
                }

                shouldHaveKafkaProducerRecords(TEST_GJENNOMFORING_V2_TOPIC, 1).should { (record) ->
                    val decoded = Json.decodeFromString<TiltaksgjennomforingV2Dto>(record.value.decodeToString())
                    val gruppe = decoded.shouldBeInstanceOf<TiltaksgjennomforingV2Dto.Gruppe>()
                    gruppe.tilgjengeligForArrangorFraOgMedDato shouldBe tilgjengeligForArrangorDato
                }
            }
        }
    }
})

private fun QueryContext.shouldHaveKafkaProducerRecords(topic: String, size: Int): List<StoredProducerRecord> {
    return queries.kafkaProducerRecord.getRecords(100, listOf(topic)).shouldHaveSize(size)
}
