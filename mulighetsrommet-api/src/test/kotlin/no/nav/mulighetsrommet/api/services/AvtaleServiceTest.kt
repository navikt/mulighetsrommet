package no.nav.mulighetsrommet.api.services

import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import io.mockk.mockk
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.utils.toUUID
import java.time.LocalDate
import java.util.*

class AvtaleServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))
    val virksomhetService: VirksomhetService = mockk(relaxed = true)
    val domain = MulighetsrommetTestDomain()

    beforeEach {
        database.db.truncateAll()
        domain.initialize(database.db)
    }

    context("Slette avtale") {
        val avtaler = AvtaleRepository(database.db)
        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

        val avtaleService = AvtaleService(
            avtaler = avtaler,
            tiltaksgjennomforinger = tiltaksgjennomforinger,
            virksomhetService = virksomhetService,
        )

        test("Man skal ikke få slette dersom avtalen ikke finnes") {
            val avtaleIdSomIkkeFinnes = "3c9f3d26-50ec-45a7-a7b2-c2d8a3653945".toUUID()

            avtaleService.delete(avtaleIdSomIkkeFinnes).shouldBeLeft().should {
                it.status shouldBe HttpStatusCode.NotFound
            }
        }

        test("Man skal ikke få slette, men få en melding dersom dagens dato er mellom start- og sluttdato for avtalen") {
            val currentDate = LocalDate.of(2023, 6, 1)
            val avtale = AvtaleFixtures.avtale1.copy(
                navn = "Avtale som eksisterer",
                startDato = LocalDate.of(2023, 6, 1),
                sluttDato = LocalDate.of(2023, 7, 1),
            )
            avtaler.upsert(avtale).shouldBeRight()

            avtaleService.delete(avtale.id, currentDate = currentDate).shouldBeLeft().should {
                it.status shouldBe HttpStatusCode.BadRequest
                it.message shouldBe "Avtalen er aktiv og kan derfor ikke slettes."
            }
        }

        test("Man skal ikke få slette, men få en melding dersom opphav for avtalen ikke er admin-flate") {
            val currentDate = LocalDate.of(2023, 6, 1)
            val avtale = AvtaleFixtures.avtale1.copy(
                navn = "Avtale som eksisterer",
                startDato = LocalDate.of(2023, 6, 1),
                sluttDato = LocalDate.of(2023, 7, 1),
                opphav = ArenaMigrering.Opphav.ARENA,
            )
            avtaler.upsert(avtale).shouldBeRight()

            avtaleService.delete(avtale.id, currentDate = currentDate).shouldBeLeft().should {
                it.status shouldBe HttpStatusCode.BadRequest
                it.message shouldBe "Avtalen har opprinnelse fra Arena og kan ikke bli slettet i admin-flate."
            }
        }

        test("Man skal ikke få slette, men få en melding dersom det finnes gjennomføringer koblet til avtalen") {
            val currentDate = LocalDate.of(2023, 6, 1)
            val avtale = AvtaleFixtures.avtale1.copy(
                id = UUID.randomUUID(),
                navn = "Avtale som eksisterer",
                startDato = LocalDate.of(2024, 5, 17),
                sluttDato = LocalDate.of(2025, 7, 1),
            )
            val avtaleSomErUinteressant = AvtaleFixtures.avtale1.copy(
                id = UUID.randomUUID(),
                navn = "Avtale som vi ikke bryr oss om",
                startDato = LocalDate.of(2024, 5, 17),
                sluttDato = LocalDate.of(2025, 7, 1),
            )
            avtaler.upsert(avtale).shouldBeRight()
            avtaler.upsert(avtaleSomErUinteressant).shouldBeRight()
            val oppfolging = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtale.id,
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                startDato = LocalDate.of(2023, 5, 1),
                sluttDato = null,
            )
            val arbeidstrening = TiltaksgjennomforingFixtures.Arbeidstrening1.copy(
                avtaleId = avtale.id,
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                startDato = LocalDate.of(2023, 5, 1),
                sluttDato = null,
            )
            val oppfolging2 = TiltaksgjennomforingFixtures.Oppfolging2.copy(
                avtaleId = avtaleSomErUinteressant.id,
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                startDato = LocalDate.of(2023, 5, 1),
                sluttDato = null,
            )
            tiltaksgjennomforinger.upsert(oppfolging).shouldBeRight()
            tiltaksgjennomforinger.upsert(arbeidstrening).shouldBeRight()
            tiltaksgjennomforinger.upsert(oppfolging2).shouldBeRight()

            avtaleService.delete(avtale.id, currentDate = currentDate).shouldBeLeft().should {
                it.status shouldBe HttpStatusCode.BadRequest
                it.message shouldBe "Avtalen har 2 tiltaksgjennomføringer koblet til seg. Du må frikoble gjennomføringene før du kan slette avtalen."
            }
        }

        test("Skal få slette avtale hvis alle sjekkene er ok") {
            val currentDate = LocalDate.of(2023, 6, 1)

            val avtale = AvtaleFixtures.avtale1.copy(
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = LocalDate.of(2024, 7, 1),
            )
            avtaler.upsert(avtale).right()

            avtaleService.delete(avtale.id, currentDate = currentDate).shouldBeRight()
        }
    }

    context("Avbryte avtale") {
        val avtaleRepository = AvtaleRepository(database.db)
        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

        val avtaleService = AvtaleService(
            avtaler = avtaleRepository,
            tiltaksgjennomforinger = tiltaksgjennomforinger,
            virksomhetService = virksomhetService,
        )

        test("Man skal ikke få avbryte dersom avtalen ikke finnes") {
            val avtale = AvtaleFixtures.avtale1.copy(navn = "Avtale som eksisterer")
            val avtaleIdSomIkkeFinnes = "3c9f3d26-50ec-45a7-a7b2-c2d8a3653945".toUUID()
            avtaleRepository.upsert(avtale).shouldBeRight()

            avtaleService.avbrytAvtale(avtaleIdSomIkkeFinnes).shouldBeLeft().should {
                it.status shouldBe HttpStatusCode.NotFound
            }
        }

        test("Man skal ikke få avbryte, men få en melding dersom opphav for avtalen ikke er admin-flate") {
            val currentDate = LocalDate.of(2023, 6, 1)
            val avtale = AvtaleFixtures.avtale1.copy(
                navn = "Avtale som eksisterer",
                startDato = LocalDate.of(2023, 6, 1),
                sluttDato = LocalDate.of(2023, 7, 1),
                opphav = ArenaMigrering.Opphav.ARENA,
            )
            avtaleRepository.upsert(avtale).shouldBeRight()

            avtaleService.avbrytAvtale(avtale.id, currentDate = currentDate).shouldBeLeft().should {
                it.status shouldBe HttpStatusCode.BadRequest
                it.message shouldBe "Avtalen har opprinnelse fra Arena og kan ikke bli avbrutt fra admin-flate."
            }
        }

        test("Man skal ikke få avbryte, men få en melding dersom avtalen allerede er avsluttet") {
            val currentDate = LocalDate.of(2023, 7, 1)
            val avtale = AvtaleFixtures.avtale1.copy(
                navn = "Avtale som eksisterer",
                startDato = LocalDate.of(2023, 5, 1),
                sluttDato = LocalDate.of(2023, 6, 1),
                opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
            )
            avtaleRepository.upsert(avtale).shouldBeRight()

            avtaleService.avbrytAvtale(avtale.id, currentDate = currentDate).shouldBeLeft().should {
                it.status shouldBe HttpStatusCode.BadRequest
                it.message shouldBe "Avtalen er allerede avsluttet og kan derfor ikke avbrytes."
            }
        }

        test("Man skal ikke få avbryte, men få en melding dersom det finnes gjennomføringer koblet til avtalen") {
            val currentDate = LocalDate.of(2023, 6, 1)
            val avtale = AvtaleFixtures.avtale1.copy(
                id = UUID.randomUUID(),
                navn = "Avtale som eksisterer",
                startDato = LocalDate.of(2024, 5, 17),
                sluttDato = LocalDate.of(2025, 7, 1),
            )
            val avtaleSomErUinteressant = AvtaleFixtures.avtale1.copy(
                id = UUID.randomUUID(),
                navn = "Avtale som vi ikke bryr oss om",
                startDato = LocalDate.of(2024, 5, 17),
                sluttDato = LocalDate.of(2025, 7, 1),
            )
            avtaleRepository.upsert(avtale).shouldBeRight()
            avtaleRepository.upsert(avtaleSomErUinteressant).shouldBeRight()
            val oppfolging = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtale.id,
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                startDato = LocalDate.of(2023, 5, 1),
                sluttDato = null,
            )
            val arbeidstrening = TiltaksgjennomforingFixtures.Arbeidstrening1.copy(
                avtaleId = avtale.id,
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                startDato = LocalDate.of(2023, 5, 1),
                sluttDato = null,
            )
            val oppfolging2 = TiltaksgjennomforingFixtures.Oppfolging2.copy(
                avtaleId = avtaleSomErUinteressant.id,
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                startDato = LocalDate.of(2023, 5, 1),
                sluttDato = null,
            )
            tiltaksgjennomforinger.upsert(oppfolging).shouldBeRight()
            tiltaksgjennomforinger.upsert(arbeidstrening).shouldBeRight()
            tiltaksgjennomforinger.upsert(oppfolging2).shouldBeRight()

            avtaleService.avbrytAvtale(avtale.id, currentDate = currentDate).shouldBeLeft().should {
                it.status shouldBe HttpStatusCode.BadRequest
                it.message shouldBe "Avtalen har 2 tiltaksgjennomføringer koblet til seg. Du må frikoble gjennomføringene før du kan avbryte avtalen."
            }
        }

        test("Skal få avbryte avtale hvis alle sjekkene er ok") {
            val currentDate = LocalDate.of(2023, 6, 1)

            val avtale = AvtaleFixtures.avtale1.copy(
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = LocalDate.of(2024, 7, 1),
            )
            avtaleRepository.upsert(avtale).right()

            avtaleService.avbrytAvtale(avtale.id, currentDate = currentDate).shouldBeRight()
        }
    }
})
