package no.nav.mulighetsrommet.api.gjennomforing

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.kafka.SisteTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.AvbruttAarsak
import no.nav.mulighetsrommet.model.GjennomforingStatus
import no.nav.mulighetsrommet.model.NavIdent
import java.time.LocalDate
import java.time.LocalDateTime

class GjennomforingServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val tiltaksgjennomforingKafkaProducer: SisteTiltaksgjennomforingerV1KafkaProducer = mockk(relaxed = true)
    val validator = mockk<GjennomforingValidator>()

    fun createService(): GjennomforingService = GjennomforingService(
        db = database.db,
        gjennomforingKafkaProducer = tiltaksgjennomforingKafkaProducer,
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

            every { validator.validate(gjennomforing.toDbo(), any()) } returns listOf(
                FieldError("navn", "Dårlig navn"),
            ).left()

            service.upsert(gjennomforing, bertilNavIdent).shouldBeLeft(
                listOf(FieldError("navn", "Dårlig navn")),
            )
        }

        test("Hvis publish kaster rulles upsert tilbake") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1Request

            every { tiltaksgjennomforingKafkaProducer.publish(any()) } throws Exception()

            shouldThrow<Throwable> {
                service.upsert(gjennomforing, bertilNavIdent)
            }

            service.get(gjennomforing.id) shouldBe null
        }

        test("Hvis ingen endring publish'er vi ikke igjen") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1Request

            every { tiltaksgjennomforingKafkaProducer.publish(any()) } returns Unit

            service.upsert(gjennomforing, bertilNavIdent)
            service.upsert(gjennomforing, bertilNavIdent)

            verify(exactly = 1) { tiltaksgjennomforingKafkaProducer.publish(any()) }
        }

        test("lagrer gjennomføring når ingen feil oppstår") {
            val gjennomforing = GjennomforingFixtures.Oppfolging1Request

            every { tiltaksgjennomforingKafkaProducer.publish(any()) } returns Unit

            service.upsert(gjennomforing, bertilNavIdent).shouldBeRight()

            service.get(gjennomforing.id) shouldNotBe null
        }
    }

    context("Administrator-notification") {
        val service = createService()

        test("Ingen administrator-notification hvis administratorer er samme som opprettet") {
            val navIdent = NavAnsattFixture.ansatt1.navIdent

            val gjennomforing = GjennomforingFixtures.Oppfolging1Request.copy(
                administratorer = listOf(navIdent),
                navEnheter = setOf("2990"),
            )
            service.upsert(gjennomforing, navIdent).shouldBeRight()

            database.assertTable("user_notification").isEmpty
        }

        test("Bare nye administratorer får notifikasjon når man endrer gjennomføring") {
            val identAnsatt1 = NavAnsattFixture.ansatt1.navIdent
            val identAnsatt2 = NavAnsattFixture.ansatt2.navIdent

            val gjennomforing = GjennomforingFixtures.Oppfolging1Request.copy(
                administratorer = listOf(identAnsatt2, identAnsatt1),
                navEnheter = setOf("2990"),
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
            MulighetsrommetTestDomain(gjennomforinger = listOf(gjennomforing)).initialize(database.db)

            every { tiltaksgjennomforingKafkaProducer.publish(any()) } returns Unit

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

            verify(exactly = 1) {
                tiltaksgjennomforingKafkaProducer.publish(match { it.id == gjennomforing.id })
            }

            database.run {
                queries.endringshistorikk.getEndringshistorikk(DocumentClass.GJENNOMFORING, gjennomforing.id)
                    .shouldNotBeNull().entries.shouldHaveSize(1).first().should {
                        it.operation shouldBe "Gjennomføringen ble avbrutt"
                    }
            }
        }

        test("avsluttes ikke hvis publish feiler") {
            val gjennomforing = GjennomforingFixtures.AFT1.copy(
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = null,
            )
            MulighetsrommetTestDomain(gjennomforinger = listOf(gjennomforing)).initialize(database.db)

            every { tiltaksgjennomforingKafkaProducer.publish(any()) } throws Exception()

            val service = createService()

            shouldThrow<Throwable> {
                service.setAvsluttet(
                    gjennomforing.id,
                    LocalDateTime.now(),
                    AvbruttAarsak.Feilregistrering,
                    bertilNavIdent,
                )
            }

            service.get(gjennomforing.id).shouldNotBeNull().should {
                it.status.status shouldBe GjennomforingStatus.GJENNOMFORES
            }
        }
    }
})
