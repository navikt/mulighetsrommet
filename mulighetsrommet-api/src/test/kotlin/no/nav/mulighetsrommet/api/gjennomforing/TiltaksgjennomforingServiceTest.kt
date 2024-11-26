package no.nav.mulighetsrommet.api.gjennomforing

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.gjennomforing.kafka.SisteTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattDbo
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattRepository
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.services.EndretAv
import no.nav.mulighetsrommet.api.services.EndringshistorikkService
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.dto.AvbruttAarsak
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus
import no.nav.mulighetsrommet.notifications.NotificationRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TiltaksgjennomforingServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val tiltaksgjennomforingKafkaProducer: SisteTiltaksgjennomforingerV1KafkaProducer = mockk(relaxed = true)
    val validator = mockk<TiltaksgjennomforingValidator>()
    val avtaleId = AvtaleFixtures.oppfolging.id

    fun createService(
        notifications: NotificationRepository = NotificationRepository(database.db),
    ) = TiltaksgjennomforingService(
        TiltaksgjennomforingRepository(database.db),
        tiltaksgjennomforingKafkaProducer,
        notifications,
        validator,
        EndringshistorikkService(database.db),
        mockk(relaxed = true),
        database.db,
    )

    val domain = MulighetsrommetTestDomain()

    beforeEach {
        domain.initialize(database.db)

        every { validator.validate(any(), any()) } answers {
            firstArg<TiltaksgjennomforingDbo>().right()
        }
    }

    afterEach {
        database.db.truncateAll()
        clearAllMocks()
    }

    val bertilNavIdent = NavIdent("B123456")

    context("Upsert gjennomføring") {
        val notifications = spyk(NotificationRepository(database.db))
        val tiltaksgjennomforingService = createService(notifications)

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

        test("Hvis notification kaster rulles upsert tilbake") {
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1Request

            every { notifications.insert(any(), any()) } throws Exception()

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
        val navAnsattRepository = NavAnsattRepository(database.db)

        test("Ingen administrator-notification hvis administratorer er samme som opprettet") {
            navAnsattRepository.upsert(
                NavAnsattDbo(
                    navIdent = bertilNavIdent,
                    fornavn = "Bertil",
                    etternavn = "Bengtson",
                    hovedenhet = "2990",
                    azureId = UUID.randomUUID(),
                    mobilnummer = null,
                    epost = "",
                    roller = emptySet(),
                    skalSlettesDato = null,
                ),
            )
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1Request.copy(
                administratorer = listOf(bertilNavIdent),
                navEnheter = listOf("2990"),
            )
            tiltaksgjennomforingService.upsert(gjennomforing, bertilNavIdent).shouldBeRight()

            database.assertThat("user_notification").isEmpty
        }

        test("Bare nye administratorer får notifikasjon når man endrer gjennomføring") {
            navAnsattRepository.upsert(
                NavAnsattDbo(
                    navIdent = bertilNavIdent,
                    fornavn = "Bertil",
                    etternavn = "Bengtson",
                    hovedenhet = "2990",
                    azureId = UUID.randomUUID(),
                    mobilnummer = null,
                    epost = "",
                    roller = emptySet(),
                    skalSlettesDato = null,
                ),
            )
            navAnsattRepository.upsert(
                NavAnsattDbo(
                    navIdent = NavIdent("Z654321"),
                    fornavn = "Znorre",
                    etternavn = "Znorrezon",
                    hovedenhet = "2990",
                    azureId = UUID.randomUUID(),
                    mobilnummer = null,
                    epost = "",
                    roller = emptySet(),
                    skalSlettesDato = null,
                ),
            )
            navAnsattRepository.upsert(
                NavAnsattDbo(
                    navIdent = NavIdent("T654321"),
                    fornavn = "Tuva",
                    etternavn = "Testpilot",
                    hovedenhet = "2990",
                    azureId = UUID.randomUUID(),
                    mobilnummer = null,
                    epost = "",
                    roller = emptySet(),
                    skalSlettesDato = null,
                ),
            )

            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1Request.copy(
                administratorer = listOf(bertilNavIdent),
                navEnheter = listOf("2990"),
            )
            tiltaksgjennomforingService.upsert(gjennomforing, bertilNavIdent).shouldBeRight()

            database.assertThat("user_notification").isEmpty

            val endretGjennomforing = gjennomforing.copy(
                navn = "nytt navn",
                administratorer = listOf(NavIdent("Z654321"), NavIdent("T654321"), bertilNavIdent),
            )
            tiltaksgjennomforingService.upsert(endretGjennomforing, bertilNavIdent).shouldBeRight()

            database.assertThat("user_notification")
                .hasNumberOfRows(2)
                .column("user_id")
                .containsValues("Z654321", "T654321")
        }
    }

    context("Avbryte gjennomføring") {
        val gjennomforinger = TiltaksgjennomforingRepository(database.db)
        val tiltaksgjennomforingService = createService()

        test("Avbrytes ikke hvis publish feiler") {
            val gjennomforing = TiltaksgjennomforingFixtures.AFT1.copy(
                avtaleId = avtaleId,
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = null,
            )
            gjennomforinger.upsert(gjennomforing)

            every { tiltaksgjennomforingKafkaProducer.publish(any()) } throws Exception()

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
