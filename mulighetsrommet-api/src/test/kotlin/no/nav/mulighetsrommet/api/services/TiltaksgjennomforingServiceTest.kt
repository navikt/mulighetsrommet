package no.nav.mulighetsrommet.api.services

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import io.mockk.mockk
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import java.time.LocalDate
import java.util.*

class TiltaksgjennomforingServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val sanityTiltaksgjennomforingService: SanityTiltaksgjennomforingService = mockk(relaxed = true)
    val virksomhetService: VirksomhetService = mockk(relaxed = true)

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
            tiltaksgjennomforingRepository.upsert(gjennomforingMedSlutt).shouldBeRight()
            tiltaksgjennomforingRepository.upsert(gjennomforingUtenSlutt).shouldBeRight()

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
            tiltaksgjennomforingRepository.upsert(gjennomforing).shouldBeRight()

            tiltaksgjennomforingService.delete(gjennomforing.id).shouldBeLeft().should {
                it.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("Man skal ikke få slette dersom det finnes deltagere koblet til gjennomføringen") {
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtaleId,
                opphav = ArenaMigrering.Opphav.ARENA,
            )
            tiltaksgjennomforingRepository.upsert(gjennomforing).shouldBeRight()

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

            tiltaksgjennomforingService.delete(gjennomforing.id, currentDate = LocalDate.of(2023, 6, 16))
                .shouldBeRight().should {
                    it shouldBe 1
                }
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
            tiltaksgjennomforingRepository.upsert(gjennomforing).shouldBeRight()

            tiltaksgjennomforingService.avbrytGjennomforing(gjennomforing.id).shouldBeLeft().should {
                it.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("Man skal ikke få avbryte dersom det finnes deltagere koblet til gjennomføringen") {
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtaleId,
                opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
            )
            tiltaksgjennomforingRepository.upsert(gjennomforing).shouldBeRight()

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

            tiltaksgjennomforingService.avbrytGjennomforing(gjennomforing.id)
                .shouldBeRight().should {
                    it shouldBe 1
                }
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
        )

        test("Man skal ikke få lov og opprette dersom avtalen er avsluttet") {
            avtaleRepository.upsert(
                AvtaleFixtures.avtale1.copy(
                    id = avtaleId,
                    tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                    sluttDato = LocalDate.of(2022, 1, 1),
                ),
            )
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1.copy(avtaleId = avtaleId)
            tiltaksgjennomforingService.upsert(gjennomforing, LocalDate.of(2022, 2, 2)).shouldBeLeft().should {
                it.status shouldBe HttpStatusCode.BadRequest
            }
        }
    }
})
