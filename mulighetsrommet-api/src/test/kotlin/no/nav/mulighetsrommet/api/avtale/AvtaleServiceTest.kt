package no.nav.mulighetsrommet.api.avtale

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.mulighetsrommet.api.Queries
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.clients.brreg.BrregClient
import no.nav.mulighetsrommet.api.clients.brreg.BrregError
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.gjennomforing.task.InitialLoadTiltaksgjennomforinger
import no.nav.mulighetsrommet.api.responses.BadRequest
import no.nav.mulighetsrommet.api.responses.NotFound
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.services.EndringshistorikkService
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dto.AvbruttAarsak
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.notifications.NotificationRepository
import no.nav.mulighetsrommet.utils.toUUID
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.mulighetsrommet.api.clients.brreg.BrregClient as BrregClient1

class AvtaleServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val validator = mockk<AvtaleValidator>()

    val domain = MulighetsrommetTestDomain()

    beforeEach {
        domain.initialize(database.db)

        every { validator.validate(any(), any()) } answers {
            firstArg<AvtaleDbo>().right()
        }
    }

    afterEach {
        database.truncateAll()
    }

    val bertilNavIdent = NavIdent("B123456")

    fun createAvtaleService(
        brregClient: BrregClient = mockk(relaxed = true),
        gjennomforingPublisher: InitialLoadTiltaksgjennomforinger = mockk(relaxed = true),
    ) = AvtaleService(
        database.db,
        ArrangorService(database.db, brregClient),
        NotificationRepository(database.db),
        validator,
        EndringshistorikkService(database.db),
        gjennomforingPublisher,
    )

    context("Upsert avtale") {
        val brregClient = mockk<BrregClient1>()
        val gjennomforingPublisher = mockk<InitialLoadTiltaksgjennomforinger>(relaxed = true)
        val avtaleService = createAvtaleService(brregClient, gjennomforingPublisher)

        test("får ikke opprette avtale dersom det oppstår valideringsfeil") {
            val request = AvtaleFixtures.avtaleRequest

            every { validator.validate(any(), any()) } returns listOf(
                ValidationError("navn", "Dårlig navn"),
            ).left()

            avtaleService.upsert(request, bertilNavIdent).shouldBeLeft(
                listOf(ValidationError("navn", "Dårlig navn")),
            )
        }

        test("får ikke opprette avtale dersom virksomhet ikke finnes i Brreg") {
            val request = AvtaleFixtures.avtaleRequest.copy(
                arrangorOrganisasjonsnummer = Organisasjonsnummer("888777435"),
                arrangorUnderenheter = listOf(),
            )

            coEvery { brregClient.getBrregVirksomhet(Organisasjonsnummer("888777435")) } returns BrregError.NotFound.left()

            avtaleService.upsert(request, bertilNavIdent).shouldBeLeft(
                listOf(
                    ValidationError(
                        "arrangorOrganisasjonsnummer",
                        "Tiltaksarrangøren finnes ikke i Brønnøysundregistrene",
                    ),
                ),
            )
        }

        test("skedulerer publisering av gjennomføringer tilhørende avtalen") {
            val request = AvtaleFixtures.avtaleRequest

            avtaleService.upsert(request, bertilNavIdent)

            verify {
                gjennomforingPublisher.schedule(
                    InitialLoadTiltaksgjennomforinger.Input(avtaleId = request.id),
                    any(),
                    any(),
                )
            }
        }
    }

    context("Avbryte avtale") {
        val avtaleService = createAvtaleService()

        test("Man skal ikke få avbryte dersom avtalen ikke finnes") {
            val avtaleIdSomIkkeFinnes = "3c9f3d26-50ec-45a7-a7b2-c2d8a3653945".toUUID()

            avtaleService.avbrytAvtale(avtaleIdSomIkkeFinnes, bertilNavIdent, AvbruttAarsak.Feilregistrering)
                .shouldBeLeft(NotFound("Avtalen finnes ikke"))
        }

        test("Man skal ikke få avbryte, men få en melding dersom avtalen allerede er avsluttet") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                navn = "Avtale som eksisterer",
                startDato = LocalDate.of(2023, 5, 1),
                sluttDato = LocalDate.of(2023, 6, 1),
            )

            database.run { Queries.avtale.upsert(avtale) }

            avtaleService.avbrytAvtale(avtale.id, bertilNavIdent, AvbruttAarsak.Feilregistrering).shouldBeLeft(
                BadRequest(message = "Avtalen er allerede avsluttet og kan derfor ikke avbrytes."),
            )
        }

        test("Man skal ikke få avbryte, men få en melding dersom det finnes aktive gjennomføringer koblet til avtalen") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                id = UUID.randomUUID(),
                navn = "Avtale som eksisterer",
                startDato = LocalDate.of(2024, 5, 17),
                sluttDato = LocalDate.of(2025, 7, 1),
            )
            val oppfolging1 = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtale.id,
                startDato = LocalDate.of(2023, 5, 1),
                sluttDato = null,
            )
            val oppfolging2 = TiltaksgjennomforingFixtures.Oppfolging2.copy(
                avtaleId = avtale.id,
                startDato = LocalDate.of(2023, 5, 1),
                sluttDato = null,
            )

            database.run {
                Queries.avtale.upsert(avtale)
                Queries.gjennomforing.upsert(oppfolging1)
                Queries.gjennomforing.upsert(oppfolging2)
            }

            avtaleService.avbrytAvtale(avtale.id, bertilNavIdent, AvbruttAarsak.Feilregistrering).shouldBeLeft(
                BadRequest("Avtalen har 2 aktive gjennomføringer og kan derfor ikke avbrytes."),
            )
        }

        test("Man skal få avbryte dersom det ikke finnes aktive gjennomføringer koblet til avtalen") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                id = UUID.randomUUID(),
                navn = "Avtale som eksisterer",
                startDato = LocalDate.now().minusDays(1),
                sluttDato = LocalDate.now().plusMonths(1),
            )
            val oppfolging1 = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtale.id,
                startDato = LocalDate.now().minusDays(1),
                sluttDato = LocalDate.now().minusDays(1),
            )

            database.run {
                Queries.avtale.upsert(avtale)
                Queries.gjennomforing.upsert(oppfolging1)
                Queries.gjennomforing.setAvsluttet(oppfolging1.id, LocalDateTime.now(), null)
            }

            avtaleService.avbrytAvtale(avtale.id, bertilNavIdent, AvbruttAarsak.Feilregistrering).shouldBeRight()
        }

        test("Skal få avbryte avtale hvis alle sjekkene er ok") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                startDato = LocalDate.now().minusDays(1),
                sluttDato = LocalDate.now().plusMonths(1),
            )

            database.run {
                Queries.avtale.upsert(avtale)
            }

            avtaleService.avbrytAvtale(avtale.id, bertilNavIdent, AvbruttAarsak.Feilregistrering)
        }
    }

    context("Administrator-notification") {
        val avtaleService = createAvtaleService()

        test("Ingen administrator-notification hvis administrator er samme som opprettet") {
            val identAnsatt1 = NavAnsattFixture.ansatt1.navIdent

            val avtale = AvtaleFixtures.avtaleRequest.copy(administratorer = listOf(identAnsatt1))
            avtaleService.upsert(avtale, identAnsatt1)

            database.assertThat("user_notification").isEmpty
        }

        test("Bare nye administratorer får notification når man endrer gjennomføring") {
            val identAnsatt1 = NavAnsattFixture.ansatt1.navIdent
            val identAnsatt2 = NavAnsattFixture.ansatt2.navIdent

            val avtale = AvtaleFixtures.avtaleRequest.copy(administratorer = listOf(identAnsatt2))
            avtaleService.upsert(avtale, identAnsatt1)

            database.assertThat("user_notification")
                .hasNumberOfRows(1)
                .column("user_id")
                .containsValues(identAnsatt2.value)
        }
    }
})
