package no.nav.mulighetsrommet.api.services

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattDbo
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.repositories.*
import no.nav.mulighetsrommet.api.routes.v1.responses.BadRequest
import no.nav.mulighetsrommet.api.routes.v1.responses.NotFound
import no.nav.mulighetsrommet.api.routes.v1.responses.ValidationError
import no.nav.mulighetsrommet.api.tiltaksgjennomforinger.TiltaksgjennomforingValidator
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus
import no.nav.mulighetsrommet.kafka.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.notifications.NotificationRepository
import java.time.LocalDate
import java.util.*

class TiltaksgjennomforingServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val tiltaksgjennomforingKafkaProducer: TiltaksgjennomforingKafkaProducer = mockk(relaxed = true)
    val virksomhetService: VirksomhetService = mockk(relaxed = true)
    val utkastRepository: UtkastRepository = mockk(relaxed = true)
    val validator = mockk<TiltaksgjennomforingValidator>()
    val avtaleId = AvtaleFixtures.oppfolging.id
    val domain = MulighetsrommetTestDomain()

    beforeEach {
        domain.initialize(database.db)

        every { validator.validate(any(), any()) } answers {
            firstArg<TiltaksgjennomforingDbo>().right()
        }

        coEvery { virksomhetService.getOrSyncVirksomhetFromBrreg(any()) } answers {
            VirksomhetDto(
                organisasjonsnummer = firstArg<String>(),
                navn = "Virksomhet",
                postnummer = null,
                poststed = null,
            ).right()
        }
    }

    afterEach {
        database.db.truncateAll()
        clearAllMocks()
    }

    context("Avbryte gjennomføring") {
        val avtaler = AvtaleRepository(database.db)
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val deltagerRepository = DeltakerRepository(database.db)
        val tiltaksgjennomforingService = TiltaksgjennomforingService(
            avtaler,
            tiltaksgjennomforingRepository,
            deltagerRepository,
            virksomhetService,
            utkastRepository,
            tiltaksgjennomforingKafkaProducer,
            NotificationRepository(database.db),
            validator,
            EndringshistorikkService(database.db),
            database.db,
        )

        test("Man skal ikke få avbryte dersom gjennomføringen ikke finnes") {
            tiltaksgjennomforingService.avbrytGjennomforing(UUID.randomUUID(), "B123456").shouldBeLeft(
                NotFound(message = "Gjennomføringen finnes ikke"),
            )
        }

        test("Man skal ikke få avbryte dersom opphav for gjennomføringen ikke er admin-flate") {
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtaleId,
            )
            tiltaksgjennomforingRepository.upsert(gjennomforing)
            tiltaksgjennomforingRepository.setOpphav(gjennomforing.id, ArenaMigrering.Opphav.ARENA)

            tiltaksgjennomforingService.avbrytGjennomforing(gjennomforing.id, "B123456").shouldBeLeft(
                BadRequest(message = "Gjennomføringen har opprinnelse fra Arena og kan ikke bli avbrutt i admin-flate."),
            )
        }

        test("Man skal ikke få avbryte dersom det finnes deltagere koblet til gjennomføringen") {
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtaleId,
                sluttDato = null,
            )
            tiltaksgjennomforingRepository.upsert(gjennomforing)
            tiltaksgjennomforingRepository.setOpphav(gjennomforing.id, ArenaMigrering.Opphav.MR_ADMIN_FLATE)

            val deltager = DeltakerFixture.Deltaker.copy(tiltaksgjennomforingId = gjennomforing.id)
            deltagerRepository.upsert(deltager)

            tiltaksgjennomforingService.avbrytGjennomforing(gjennomforing.id, "B123456").shouldBeLeft(
                BadRequest(message = "Gjennomføringen kan ikke avbrytes fordi den har 1 deltager(e) koblet til seg."),
            )
        }

        test("Skal få avbryte tiltaksgjennomføring hvis alle sjekkene er ok") {
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtaleId,
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = null,
            )
            tiltaksgjennomforingRepository.upsert(gjennomforing)

            tiltaksgjennomforingService.avbrytGjennomforing(gjennomforing.id, "B123456").shouldBeRight()
        }
    }

    context("Upsert gjennomføring") {
        val avtaler = AvtaleRepository(database.db)
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val deltagerRepository = DeltakerRepository(database.db)
        val avtaleRepository = AvtaleRepository(database.db)
        val tiltaksgjennomforingService = TiltaksgjennomforingService(
            avtaler,
            tiltaksgjennomforingRepository,
            deltagerRepository,
            virksomhetService,
            utkastRepository,
            tiltaksgjennomforingKafkaProducer,
            NotificationRepository(database.db),
            validator,
            EndringshistorikkService(database.db),
            database.db,
        )

        test("Man skal ikke få lov til å opprette gjennomføring dersom det oppstår valideringsfeil") {
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1Request

            every { validator.validate(gjennomforing.toDbo(), any()) } returns listOf(
                ValidationError("navn", "Dårlig navn"),
            ).left()

            avtaleRepository.upsert(AvtaleFixtures.oppfolging)

            tiltaksgjennomforingService.upsert(gjennomforing, "B123456").shouldBeLeft(
                listOf(ValidationError("navn", "Dårlig navn")),
            )
        }
    }

    context("Administrator-notification") {
        val avtaler = AvtaleRepository(database.db)
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val deltagerRepository = DeltakerRepository(database.db)
        val avtaleRepository = AvtaleRepository(database.db)
        val tiltaksgjennomforingService = TiltaksgjennomforingService(
            avtaler,
            tiltaksgjennomforingRepository,
            deltagerRepository,
            virksomhetService,
            utkastRepository,
            tiltaksgjennomforingKafkaProducer,
            NotificationRepository(database.db),
            validator,
            EndringshistorikkService(database.db),
            database.db,
        )
        val navAnsattRepository = NavAnsattRepository(database.db)

        test("Ingen administrator-notification hvis administratorer er samme som opprettet") {
            navAnsattRepository.upsert(
                NavAnsattDbo(
                    navIdent = "B123456",
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
            avtaleRepository.upsert(
                AvtaleFixtures.oppfolging.copy(
                    id = avtaleId,
                    tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                    sluttDato = LocalDate.of(2025, 1, 1),
                ),
            )
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1Request.copy(
                administratorer = listOf("B123456"),
                navEnheter = listOf("2990"),
            )
            tiltaksgjennomforingService.upsert(gjennomforing, "B123456").shouldBeRight()

            database.assertThat("user_notification").isEmpty
        }

        test("Bare nye administratorer får notifikasjon når man endrer gjennomføring") {
            navAnsattRepository.upsert(
                NavAnsattDbo(
                    navIdent = "B123456",
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
                    navIdent = "Z654321",
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
                    navIdent = "T654321",
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
            avtaleRepository.upsert(
                AvtaleFixtures.oppfolging.copy(
                    id = avtaleId,
                    tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                    sluttDato = LocalDate.of(2025, 1, 1),
                ),
            )

            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1Request.copy(
                administratorer = listOf("B123456"),
                navEnheter = listOf("2990"),
            )
            tiltaksgjennomforingService.upsert(gjennomforing, "B123456").shouldBeRight()

            database.assertThat("user_notification").isEmpty

            val endretGjennomforing = gjennomforing.copy(
                navn = "nytt navn",
                administratorer = listOf("Z654321", "T654321", "B123456"),
            )
            tiltaksgjennomforingService.upsert(endretGjennomforing, "B123456").shouldBeRight()

            database.assertThat("user_notification")
                .hasNumberOfRows(2)
                .column("user_id")
                .containsValues("Z654321", "T654321")
        }
    }

    context("transaction testing") {
        val avtaler = AvtaleRepository(database.db)
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val deltagerRepository = DeltakerRepository(database.db)
        val notificationRepository = spyk(NotificationRepository(database.db))
        val tiltaksgjennomforingService = TiltaksgjennomforingService(
            avtaler,
            tiltaksgjennomforingRepository,
            deltagerRepository,
            virksomhetService,
            utkastRepository,
            tiltaksgjennomforingKafkaProducer,
            notificationRepository,
            validator,
            EndringshistorikkService(database.db),
            database.db,
        )

        test("Hvis publish kaster rulles upsert tilbake") {
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1Request

            every { tiltaksgjennomforingKafkaProducer.publish(any()) } throws Exception()

            shouldThrow<Throwable> {
                tiltaksgjennomforingService.upsert(gjennomforing, "B123456")
            }

            tiltaksgjennomforingService.get(gjennomforing.id) shouldBe null
        }

        test("Hvis ingen endring publish'er vi ikke igjen") {
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1Request

            every { tiltaksgjennomforingKafkaProducer.publish(any()) } returns Unit

            tiltaksgjennomforingService.upsert(gjennomforing, "B123456")
            tiltaksgjennomforingService.upsert(gjennomforing, "B123456")

            verify(exactly = 1) { tiltaksgjennomforingKafkaProducer.publish(any()) }
        }

        test("Hvis is publish _ikke_ kaster blir upsert værende") {
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1Request

            every { tiltaksgjennomforingKafkaProducer.publish(any()) } returns Unit

            tiltaksgjennomforingService.upsert(gjennomforing, "B123456").shouldBeRight()

            tiltaksgjennomforingService.get(gjennomforing.id) shouldNotBe null
        }

        test("Hvis utkast kaster rulles upsert tilbake") {
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1Request

            every { utkastRepository.delete(any(), any()) } throws Exception()

            shouldThrow<Throwable> {
                tiltaksgjennomforingService.upsert(gjennomforing, "B123456")
            }

            tiltaksgjennomforingService.get(gjennomforing.id) shouldBe null
        }

        test("Hvis notification kaster rulles upsert tilbake") {
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1Request

            every { notificationRepository.insert(any(), any()) } throws Exception()

            shouldThrow<Throwable> {
                tiltaksgjennomforingService.upsert(gjennomforing, "B123456")
            }

            tiltaksgjennomforingService.get(gjennomforing.id) shouldBe null
        }

        test("Avbrytes ikke hvis publish feiler") {
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtaleId,
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = null,
            )
            tiltaksgjennomforingRepository.upsert(gjennomforing)

            every { tiltaksgjennomforingKafkaProducer.publish(any()) } throws Exception()

            shouldThrow<Throwable> { tiltaksgjennomforingService.avbrytGjennomforing(gjennomforing.id, "B123456") }

            tiltaksgjennomforingService.get(gjennomforing.id) should {
                it!!.status shouldBe Tiltaksgjennomforingsstatus.GJENNOMFORES
            }
        }
    }
})
