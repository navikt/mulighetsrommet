package no.nav.mulighetsrommet.api.services

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import io.mockk.mockk
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixture
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import java.time.LocalDate
import java.util.*

class TiltaksgjennomforingServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val sanityTiltaksgjennomforingService: SanityTiltaksgjennomforingService = mockk(relaxed = true)
    val virksomhetService: VirksomhetService = mockk(relaxed = true)
    val avtaleFixtures = AvtaleFixtures(database)

    val avtaleId = UUID.randomUUID()

    beforeEach {
        database.db.clean()
        database.db.migrate()

        val tiltakstypeRepository = TiltakstypeRepository(database.db)
        tiltakstypeRepository.upsert(TiltakstypeFixtures.Oppfolging)

        val avtaleRepository = AvtaleRepository(database.db)
        avtaleRepository.upsert(
            avtaleFixtures.createAvtaleForTiltakstype(
                id = avtaleId,
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
            ),
        )
    }

    context("Slette gjennomforing") {
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val deltagerRepository = DeltakerRepository(database.db)
        val tiltaksgjennomforingService = TiltaksgjennomforingService(
            tiltaksgjennomforingRepository,
            deltagerRepository,
            sanityTiltaksgjennomforingService,
            virksomhetService,
        )

        test("Man skal ikke få slette dersom avtalen ikke finnes") {
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

            tiltaksgjennomforingService.delete(gjennomforingMedSlutt.id, currentDate = currentDate).shouldBeLeft().should {
                it.status shouldBe HttpStatusCode.BadRequest
            }
            tiltaksgjennomforingService.delete(gjennomforingUtenSlutt.id, currentDate = currentDate).shouldBeLeft().should {
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

        test("Man skal ikke få slette dersom det finnes deltagere koblet til avtalen") {
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

        test("Skal få slette avtale hvis alle sjekkene er ok") {
            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1.copy(avtaleId = avtaleId)
            tiltaksgjennomforingRepository.upsert(gjennomforing)

            tiltaksgjennomforingService.delete(gjennomforing.id).shouldBeRight().should {
                it shouldBe 1
            }
        }
    }
})
