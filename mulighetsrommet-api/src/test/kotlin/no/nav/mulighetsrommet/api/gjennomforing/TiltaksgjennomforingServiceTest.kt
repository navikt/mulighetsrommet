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
import no.nav.mulighetsrommet.api.endringshistorikk.EndretAv
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.kafka.SisteTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.domain.dto.AvbruttAarsak
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus
import java.time.LocalDate
import java.time.LocalDateTime

class TiltaksgjennomforingServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val tiltaksgjennomforingKafkaProducer: SisteTiltaksgjennomforingerV1KafkaProducer = mockk(relaxed = true)
    val validator = mockk<TiltaksgjennomforingValidator>()

    fun createService(): TiltaksgjennomforingService = TiltaksgjennomforingService(
        db = database.db,
        tiltaksgjennomforingKafkaProducer = tiltaksgjennomforingKafkaProducer,
        validator = validator,
        navAnsattService = mockk(relaxed = true),
    )

    val domain = MulighetsrommetTestDomain()

    beforeEach {
        domain.initialize(database.db)

        every { validator.validate(any(), any()) } answers {
            firstArg<TiltaksgjennomforingDbo>().right()
        }
    }

    afterEach {
        database.truncateAll()
        clearAllMocks()
    }

    val bertilNavIdent = NavIdent("B123456")

    context("Upsert gjennomføring") {
        val tiltaksgjennomforingService = createService()

        test("Man skal ikke få lov til å opprette gjennomføring dersom det oppstår valideringsfeil") {
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1Request

            every { validator.validate(gjennomforing.toDbo(), any()) } returns listOf(
                ValidationError("navn", "Dårlig navn"),
            ).left()

            tiltaksgjennomforingService.upsert(gjennomforing, bertilNavIdent).shouldBeLeft(
                listOf(ValidationError("navn", "Dårlig navn")),
            )
        }

        test("Hvis publish kaster rulles upsert tilbake") {
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1Request

            every { tiltaksgjennomforingKafkaProducer.publish(any()) } throws Exception()

            shouldThrow<Throwable> {
                tiltaksgjennomforingService.upsert(gjennomforing, bertilNavIdent)
            }

            tiltaksgjennomforingService.get(gjennomforing.id) shouldBe null
        }

        test("Hvis ingen endring publish'er vi ikke igjen") {
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1Request

            every { tiltaksgjennomforingKafkaProducer.publish(any()) } returns Unit

            tiltaksgjennomforingService.upsert(gjennomforing, bertilNavIdent)
            tiltaksgjennomforingService.upsert(gjennomforing, bertilNavIdent)

            verify(exactly = 1) { tiltaksgjennomforingKafkaProducer.publish(any()) }
        }

        test("lagrer gjennomføring når ingen feil oppstår") {
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1Request

            every { tiltaksgjennomforingKafkaProducer.publish(any()) } returns Unit

            tiltaksgjennomforingService.upsert(gjennomforing, bertilNavIdent).shouldBeRight()

            tiltaksgjennomforingService.get(gjennomforing.id) shouldNotBe null
        }
    }

    context("Administrator-notification") {
        val tiltaksgjennomforingService = createService()

        test("Ingen administrator-notification hvis administratorer er samme som opprettet") {
            val navIdent = NavAnsattFixture.ansatt1.navIdent

            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1Request.copy(
                administratorer = listOf(navIdent),
                navEnheter = listOf("2990"),
            )
            tiltaksgjennomforingService.upsert(gjennomforing, navIdent).shouldBeRight()

            database.assertThat("user_notification").isEmpty
        }

        test("Bare nye administratorer får notifikasjon når man endrer gjennomføring") {
            val identAnsatt1 = NavAnsattFixture.ansatt1.navIdent
            val identAnsatt2 = NavAnsattFixture.ansatt2.navIdent

            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1Request.copy(
                administratorer = listOf(identAnsatt2, identAnsatt1),
                navEnheter = listOf("2990"),
            )
            tiltaksgjennomforingService.upsert(gjennomforing, identAnsatt1).shouldBeRight()

            database.assertThat("user_notification")
                .hasNumberOfRows(1)
                .column("user_id")
                .containsValues(identAnsatt2.value)
        }
    }

    context("avslutte gjennomføring") {
        test("publiserer til kafka og skriver til endringshistorikken når gjennomføring avsluttes") {
            val gjennomforing = TiltaksgjennomforingFixtures.AFT1.copy(
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = null,
            )
            MulighetsrommetTestDomain(gjennomforinger = listOf(gjennomforing)).initialize(database.db)

            every { tiltaksgjennomforingKafkaProducer.publish(any()) } returns Unit

            val tiltaksgjennomforingService = createService()

            tiltaksgjennomforingService.setAvsluttet(
                gjennomforing.id,
                LocalDateTime.now(),
                AvbruttAarsak.Feilregistrering,
                EndretAv.NavAnsatt(bertilNavIdent),
            )

            tiltaksgjennomforingService.get(gjennomforing.id).shouldNotBeNull().should {
                it.status.status shouldBe TiltaksgjennomforingStatus.AVBRUTT
            }

            verify(exactly = 1) {
                tiltaksgjennomforingKafkaProducer.publish(match { it.id == gjennomforing.id })
            }

            database.run {
                Queries.endringshistorikk.getEndringshistorikk(DocumentClass.TILTAKSGJENNOMFORING, gjennomforing.id)
                    .shouldNotBeNull().entries.shouldHaveSize(1).first().should {
                        it.operation shouldBe "Gjennomføringen ble avbrutt"
                    }
            }
        }

        test("avsluttes ikke hvis publish feiler") {
            val gjennomforing = TiltaksgjennomforingFixtures.AFT1.copy(
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = null,
            )
            MulighetsrommetTestDomain(gjennomforinger = listOf(gjennomforing)).initialize(database.db)

            every { tiltaksgjennomforingKafkaProducer.publish(any()) } throws Exception()

            val tiltaksgjennomforingService = createService()

            shouldThrow<Throwable> {
                tiltaksgjennomforingService.setAvsluttet(
                    gjennomforing.id,
                    LocalDateTime.now(),
                    AvbruttAarsak.Feilregistrering,
                    EndretAv.NavAnsatt(bertilNavIdent),
                )
            }

            tiltaksgjennomforingService.get(gjennomforing.id).shouldNotBeNull().should {
                it.status.status shouldBe TiltaksgjennomforingStatus.GJENNOMFORES
            }
        }
    }
})
