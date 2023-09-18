package no.nav.mulighetsrommet.api.services

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattDbo
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.NavAnsattRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.UtkastRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus
import no.nav.mulighetsrommet.notifications.NotificationRepository
import java.time.LocalDate
import java.util.*

class TiltaksgjennomforingServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val tiltaksgjennomforingKafkaProducer: TiltaksgjennomforingKafkaProducer = mockk(relaxed = true)
    val sanityTiltaksgjennomforingService: SanityTiltaksgjennomforingService = mockk(relaxed = true)
    val virksomhetService: VirksomhetService = mockk(relaxed = true)
    val notificationRepository: NotificationRepository = mockk(relaxed = true)
    val utkastRepository: UtkastRepository = mockk(relaxed = true)

    val avtaleId = AvtaleFixtures.avtale1.id
    val domain = MulighetsrommetTestDomain()

    beforeEach {
        database.db.truncateAll()
        domain.initialize(database.db)
    }

    context("Slette gjennomføring") {
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val deltagerRepository = DeltakerRepository(database.db)
        val avtaleRepository = AvtaleRepository(database.db)
        val tiltaksgjennomforingService = TiltaksgjennomforingService(
            tiltaksgjennomforingRepository,
            deltagerRepository,
            avtaleRepository,
            sanityTiltaksgjennomforingService,
            virksomhetService,
            utkastRepository,
            tiltaksgjennomforingKafkaProducer,
            notificationRepository,
            database.db,
        )

        test("Man skal ikke få slette dersom gjennomføringen ikke finnes") {
            tiltaksgjennomforingService.delete(UUID.randomUUID()).shouldBeLeft().should {
                it.status shouldBe HttpStatusCode.NotFound
            }
        }

        test("Man skal ikke få slette dersom dagens dato er mellom start- og sluttdato for gjennomoringen") {
            val currentDate = LocalDate.of(2023, 6, 1)
            val gjennomforingMedSlutt = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtaleId,
                startDato = LocalDate.of(2023, 1, 1),
                sluttDato = LocalDate.of(2024, 8, 1),
            )
            val gjennomforingUtenSlutt = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                id = UUID.randomUUID(),
                avtaleId = avtaleId,
                startDato = LocalDate.of(2023, 1, 1),
                sluttDato = null,
            )
            tiltaksgjennomforingRepository.upsert(gjennomforingMedSlutt)
            tiltaksgjennomforingRepository.upsert(gjennomforingUtenSlutt)

            tiltaksgjennomforingService.delete(gjennomforingMedSlutt.id, currentDate = currentDate).shouldBeLeft()
                .should {
                    it.status shouldBe HttpStatusCode.BadRequest
                }
            tiltaksgjennomforingService.delete(gjennomforingUtenSlutt.id, currentDate = currentDate).shouldBeLeft()
                .should {
                    it.status shouldBe HttpStatusCode.BadRequest
                }
        }

        test("Man skal ikke få slette dersom opphav for gjennomforingen ikke er admin-flate") {
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtaleId,
                opphav = ArenaMigrering.Opphav.ARENA,
            )
            tiltaksgjennomforingRepository.upsert(gjennomforing)

            tiltaksgjennomforingService.delete(gjennomforing.id).shouldBeLeft().should {
                it.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("Man skal ikke få slette dersom det finnes deltagere koblet til gjennomføringen") {
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtaleId,
                opphav = ArenaMigrering.Opphav.ARENA,
            )
            tiltaksgjennomforingRepository.upsert(gjennomforing)

            val deltager = DeltakerFixture.Deltaker.copy(tiltaksgjennomforingId = gjennomforing.id)
            deltagerRepository.upsert(deltager)

            tiltaksgjennomforingService.delete(gjennomforing.id).shouldBeLeft().should {
                it.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("Skal få slette tiltaksgjennomføring hvis alle sjekkene er ok") {
            val gjennomforing =
                TiltaksgjennomforingFixtures.Oppfolging1.copy(avtaleId = avtaleId, startDato = LocalDate.of(2023, 7, 1))
            tiltaksgjennomforingRepository.upsert(gjennomforing)

            tiltaksgjennomforingService.delete(gjennomforing.id, currentDate = LocalDate.of(2023, 6, 16)).shouldBeRight()
        }
    }

    context("Avbryte gjennomføring") {
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val deltagerRepository = DeltakerRepository(database.db)
        val avtaleRepository = AvtaleRepository(database.db)
        val tiltaksgjennomforingService = TiltaksgjennomforingService(
            tiltaksgjennomforingRepository,
            deltagerRepository,
            avtaleRepository,
            sanityTiltaksgjennomforingService,
            virksomhetService,
            utkastRepository,
            tiltaksgjennomforingKafkaProducer,
            notificationRepository,
            database.db,
        )

        test("Man skal ikke få avbryte dersom gjennomføringen ikke finnes") {
            tiltaksgjennomforingService.delete(UUID.randomUUID()).shouldBeLeft().should {
                it.status shouldBe HttpStatusCode.NotFound
            }
        }

        test("Man skal ikke få avbryte dersom opphav for gjennomføringen ikke er admin-flate") {
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtaleId,
                opphav = ArenaMigrering.Opphav.ARENA,
            )
            tiltaksgjennomforingRepository.upsert(gjennomforing)

            tiltaksgjennomforingService.avbrytGjennomforing(gjennomforing.id).shouldBeLeft().should {
                it.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("Man skal ikke få avbryte dersom det finnes deltagere koblet til gjennomføringen") {
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtaleId,
                opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
            )
            tiltaksgjennomforingRepository.upsert(gjennomforing)

            val deltager = DeltakerFixture.Deltaker.copy(tiltaksgjennomforingId = gjennomforing.id)
            deltagerRepository.upsert(deltager)

            tiltaksgjennomforingService.avbrytGjennomforing(gjennomforing.id).shouldBeLeft().should {
                it.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("Skal få avbryte tiltaksgjennomføring hvis alle sjekkene er ok") {
            val gjennomforing =
                TiltaksgjennomforingFixtures.Oppfolging1.copy(avtaleId = avtaleId, startDato = LocalDate.of(2023, 7, 1))
            tiltaksgjennomforingRepository.upsert(gjennomforing)

            tiltaksgjennomforingService.avbrytGjennomforing(gjennomforing.id).shouldBeRight()
        }
    }

    context("Put gjennomforing") {
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val deltagerRepository = DeltakerRepository(database.db)
        val avtaleRepository = AvtaleRepository(database.db)
        val tiltaksgjennomforingService = TiltaksgjennomforingService(
            tiltaksgjennomforingRepository,
            deltagerRepository,
            avtaleRepository,
            sanityTiltaksgjennomforingService,
            virksomhetService,
            utkastRepository,
            tiltaksgjennomforingKafkaProducer,
            notificationRepository,
            database.db,
        )

        test("Man skal ikke få lov og opprette dersom avtalen er avsluttet") {
            avtaleRepository.upsert(
                AvtaleFixtures.avtale1.copy(
                    id = avtaleId,
                    tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                    sluttDato = LocalDate.of(2022, 1, 1),
                ),
            )
            val gjennomforing = TiltaksgjennomforingFixtures.oppfolging1Request(avtaleId)
            tiltaksgjennomforingService.upsert(gjennomforing, "B123456", LocalDate.of(2022, 2, 2)).shouldBeLeft().should {
                it.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("Antall plasser må være større enn null") {
            avtaleRepository.upsert(
                AvtaleFixtures.avtale1.copy(
                    id = avtaleId,
                    tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                    sluttDato = LocalDate.of(2022, 1, 1),
                ),
            )
            val gjennomforing = TiltaksgjennomforingFixtures.oppfolging1Request(avtaleId).copy(antallPlasser = 0)
            tiltaksgjennomforingService.upsert(gjennomforing, "B123456", LocalDate.of(2022, 2, 2)).shouldBeLeft()
        }
    }

    context("Administrator-notification") {
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val deltagerRepository = DeltakerRepository(database.db)
        val avtaleRepository = AvtaleRepository(database.db)
        val tiltaksgjennomforingService = TiltaksgjennomforingService(
            tiltaksgjennomforingRepository,
            deltagerRepository,
            avtaleRepository,
            sanityTiltaksgjennomforingService,
            virksomhetService,
            utkastRepository,
            tiltaksgjennomforingKafkaProducer,
            notificationRepository,
            database.db,
        )
        val navAnsattRepository = NavAnsattRepository(database.db)

        test("Ingen administrator-notification hvis administrator er samme som opprettet") {
            navAnsattRepository.upsert(
                NavAnsattDbo(
                    navIdent = "B123456",
                    fornavn = "Bertil",
                    etternavn = "Betabruker",
                    hovedenhet = "2990",
                    azureId = UUID.randomUUID(),
                    mobilnummer = null,
                    epost = "",
                    roller = emptySet(),
                    skalSlettesDato = null,
                ),
            )
            avtaleRepository.upsert(
                AvtaleFixtures.avtale1.copy(
                    id = avtaleId,
                    tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                    sluttDato = LocalDate.of(2025, 1, 1),
                ),
            )
            val gjennomforing = TiltaksgjennomforingFixtures.oppfolging1Request(avtaleId)
                .copy(administrator = "B123456", navEnheter = listOf("2990"))
            tiltaksgjennomforingService.upsert(gjennomforing, "B123456", LocalDate.of(2023, 1, 1)).shouldBeRight()

            verify(exactly = 0) { notificationRepository.insert(any(), any()) }
        }

        test("Bare én administrator notification når man endrer gjennomforing") {
            navAnsattRepository.upsert(
                NavAnsattDbo(
                    navIdent = "B123456",
                    fornavn = "Bertil",
                    etternavn = "Betabruker",
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
                    fornavn = "Zorre",
                    etternavn = "Betabruker",
                    hovedenhet = "2990",
                    azureId = UUID.randomUUID(),
                    mobilnummer = null,
                    epost = "",
                    roller = emptySet(),
                    skalSlettesDato = null,
                ),
            )
            avtaleRepository.upsert(
                AvtaleFixtures.avtale1.copy(
                    id = avtaleId,
                    tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                    sluttDato = LocalDate.of(2025, 1, 1),
                ),
            )
            val gjennomforing = TiltaksgjennomforingFixtures.oppfolging1Request(avtaleId)
                .copy(administrator = "Z654321", navEnheter = listOf("2990"))

            tiltaksgjennomforingService.upsert(gjennomforing, "B123456", LocalDate.of(2023, 1, 1)).shouldBeRight()
            tiltaksgjennomforingService.upsert(gjennomforing.copy(navn = "nytt navn"), "B123456", LocalDate.of(2023, 1, 1)).shouldBeRight()

            verify(exactly = 1) { notificationRepository.insert(any(), any()) }
        }
    }

    context("transaction testing") {
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val deltagerRepository = DeltakerRepository(database.db)
        val avtaleRepository = AvtaleRepository(database.db)
        val tiltaksgjennomforingService = TiltaksgjennomforingService(
            tiltaksgjennomforingRepository,
            deltagerRepository,
            avtaleRepository,
            sanityTiltaksgjennomforingService,
            virksomhetService,
            utkastRepository,
            tiltaksgjennomforingKafkaProducer,
            notificationRepository,
            database.db,
        )

        test("Hvis publish kaster rulles upsert tilbake") {
            val gjennomforing = TiltaksgjennomforingFixtures.oppfolging1Request(avtaleId)

            every { tiltaksgjennomforingKafkaProducer.publish(any()) } throws Exception()

            shouldThrow<Throwable> { tiltaksgjennomforingService.upsert(gjennomforing, "B123456", LocalDate.of(2023, 1, 1)) }

            tiltaksgjennomforingService.get(gjennomforing.id) shouldBe null
        }

        test("Hvis is publish _ikke_ kaster blir upsert værende") {
            val gjennomforing = TiltaksgjennomforingFixtures.oppfolging1Request(avtaleId)

            every { tiltaksgjennomforingKafkaProducer.publish(any()) } returns Unit

            tiltaksgjennomforingService.upsert(gjennomforing, "B123456", LocalDate.of(2023, 1, 1)).shouldBeRight()

            tiltaksgjennomforingService.get(gjennomforing.id) shouldNotBe null
        }

        test("Hvis utkast kaster rulles upsert tilbake") {
            val gjennomforing = TiltaksgjennomforingFixtures.oppfolging1Request(avtaleId)

            every { utkastRepository.delete(any(), any()) } throws Exception()

            shouldThrow<Throwable> { tiltaksgjennomforingService.upsert(gjennomforing, "B123456", LocalDate.of(2023, 1, 1)) }

            tiltaksgjennomforingService.get(gjennomforing.id) shouldBe null
        }

        test("Hvis notification kaster rulles upsert tilbake") {
            val gjennomforing = TiltaksgjennomforingFixtures.oppfolging1Request(avtaleId)

            every { notificationRepository.insert(any(), any()) } throws Exception()

            shouldThrow<Throwable> { tiltaksgjennomforingService.upsert(gjennomforing, "B123456", LocalDate.of(2023, 1, 1)) }

            tiltaksgjennomforingService.get(gjennomforing.id) shouldBe null
        }

        test("Avbrytes ikke hvis publish feiler") {
            val gjennomforing =
                TiltaksgjennomforingFixtures.Oppfolging1.copy(avtaleId = avtaleId, startDato = LocalDate.of(2023, 7, 1))
            tiltaksgjennomforingRepository.upsert(gjennomforing)

            every { tiltaksgjennomforingKafkaProducer.publish(any()) } throws Exception()

            shouldThrow<Throwable> { tiltaksgjennomforingService.avbrytGjennomforing(gjennomforing.id) }

            tiltaksgjennomforingService.get(gjennomforing.id) should {
                it!!.status shouldNotBe Tiltaksgjennomforingsstatus.AVBRUTT
            }
        }

        test("Deletes ikke hvis retract feiler") {
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtaleId,
                startDato = LocalDate.now().plusDays(10),
            )
            tiltaksgjennomforingRepository.upsert(gjennomforing)

            every { tiltaksgjennomforingKafkaProducer.retract(any()) } throws Exception()

            shouldThrow<Throwable> { tiltaksgjennomforingService.delete(gjennomforing.id) }

            tiltaksgjennomforingService.get(gjennomforing.id) shouldNotBe null
        }

        test("Hvis sanity create kaster rulles upsert tilbake") {
            val gjennomforing = TiltaksgjennomforingFixtures.oppfolging1Request(avtaleId)

            coEvery { sanityTiltaksgjennomforingService.createOrPatchSanityTiltaksgjennomforing(any()) } throws Exception()

            shouldThrow<Throwable> { tiltaksgjennomforingService.upsert(gjennomforing, "B123456", LocalDate.of(2023, 1, 1)) }

            tiltaksgjennomforingService.get(gjennomforing.id) shouldBe null
        }
    }
})
