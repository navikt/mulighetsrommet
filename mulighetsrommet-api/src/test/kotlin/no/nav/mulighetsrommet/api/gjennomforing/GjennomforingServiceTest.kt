package no.nav.mulighetsrommet.api.gjennomforing

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.*
import java.time.LocalDate
import java.time.LocalDateTime

private const val PRODUCER_TOPIC = "siste-tiltaksgjennomforinger-topic"

class GjennomforingServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val validator = mockk<GjennomforingValidator>()

    fun createService(): GjennomforingService = GjennomforingService(
        config = GjennomforingService.Config(PRODUCER_TOPIC),
        db = database.db,
        validator = validator,
        navAnsattService = mockk(relaxed = true),
    )

    val domain = MulighetsrommetTestDomain()

    beforeEach {
        domain.initialize(database.db)

        every { validator.validate(any(), any()) } answers {
            firstArg<GjennomforingDbo>().right()
        }
    }

    afterEach {
        database.truncateAll()
        clearAllMocks()
    }

    val bertilNavIdent = NavIdent("B123456")

    context("Upsert gjennomføring") {
        val service = createService()

        test("Man skal ikke få lov til å opprette gjennomføring dersom det oppstår valideringsfeil") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1Request

            every { validator.validate(any(), any()) } returns listOf(
                FieldError("navn", "Dårlig navn"),
            ).left()

            service.upsert(gjennomforing, bertilNavIdent).shouldBeLeft(
                listOf(FieldError("navn", "Dårlig navn")),
            )
        }

        test("oppretting av gjennomføring blir lagret som et utgående kafka-record") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1Request

            service.upsert(gjennomforing, bertilNavIdent)

            database.run {
                val record = queries.kafkaProducerRecord.getRecords(10).shouldHaveSize(1).first()
                record.topic shouldBe PRODUCER_TOPIC
                record.key shouldBe gjennomforing.id.toString().toByteArray()

                val decoded = Json.decodeFromString<TiltaksgjennomforingEksternV1Dto>(record.value.decodeToString())
                decoded.id shouldBe gjennomforing.id
            }
        }

        test("lagrer ikke duplikater som utgående kafka-records") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1Request

            service.upsert(gjennomforing, bertilNavIdent)
            service.upsert(gjennomforing, bertilNavIdent)

            database.run {
                queries.kafkaProducerRecord.getRecords(10).shouldHaveSize(1)
            }
        }
    }

    context("Administrator-notification") {
        val service = createService()

        test("Ingen administrator-notification hvis administratorer er samme som opprettet") {
            val navIdent = NavAnsattFixture.DonaldDuck.navIdent

            val gjennomforing = GjennomforingFixtures.Oppfolging1Request.copy(
                administratorer = listOf(navIdent),
                navEnheter = setOf(NavEnhetNummer("2990")),
            )
            service.upsert(gjennomforing, navIdent).shouldBeRight()

            database.assertTable("user_notification").isEmpty
        }

        test("Bare nye administratorer får notifikasjon når man endrer gjennomføring") {
            val identAnsatt1 = NavAnsattFixture.DonaldDuck.navIdent
            val identAnsatt2 = NavAnsattFixture.MikkeMus.navIdent

            val gjennomforing = GjennomforingFixtures.Oppfolging1Request.copy(
                administratorer = listOf(identAnsatt2, identAnsatt1),
                navEnheter = setOf(NavEnhetNummer("2990")),
            )
            service.upsert(gjennomforing, identAnsatt1).shouldBeRight()

            database.assertTable("user_notification")
                .hasNumberOfRows(1)
                .column("user_id")
                .containsValues(identAnsatt2.value)
        }
    }

    context("avslutte gjennomføring") {
        test("publiserer til kafka og skriver til endringshistorikken når gjennomføring avsluttes") {
            val gjennomforing = GjennomforingFixtures.AFT1.copy(
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = null,
            )
            MulighetsrommetTestDomain(
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.db)

            val service = createService()

            service.setAvsluttet(
                gjennomforing.id,
                LocalDateTime.now(),
                AvbruttAarsak.Feilregistrering,
                bertilNavIdent,
            )

            service.get(gjennomforing.id).shouldNotBeNull().should {
                it.status.status shouldBe GjennomforingStatus.AVBRUTT
            }

            database.run {
                val record = queries.kafkaProducerRecord.getRecords(10).shouldHaveSize(1).first()
                record.key shouldBe gjennomforing.id.toString().toByteArray()

                queries.endringshistorikk.getEndringshistorikk(DocumentClass.GJENNOMFORING, gjennomforing.id)
                    .shouldNotBeNull().entries.shouldHaveSize(1).first().should {
                        it.operation shouldBe "Gjennomføringen ble avbrutt"
                    }
            }
        }
    }
})
