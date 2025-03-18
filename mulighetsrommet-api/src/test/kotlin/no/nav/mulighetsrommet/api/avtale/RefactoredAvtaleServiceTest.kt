package no.nav.mulighetsrommet.api.avtale

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.gjennomforing.task.InitialLoadGjennomforinger
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.test.TestBase
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregError
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.model.AvbruttAarsak
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.utils.toUUID
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Refactored version of AvtaleServiceTest using the new TestBase
 */
class RefactoredAvtaleServiceTest : TestBase() {
    // Mock validator
    private val validator = mockk<AvtaleValidator>()

    init {
        beforeEach {
            every { validator.validate(any(), any()) } answers {
                firstArg<AvtaleDbo>().right()
            }
        }

        afterEach {
            database.truncateAll()
        }

        // Helper function to create service instance
        fun createAvtaleService(
            brregClient: BrregClient = mockk(relaxed = true),
            gjennomforingPublisher: InitialLoadGjennomforinger = mockk(relaxed = true),
        ) = AvtaleService(
            database.db,
            ArrangorService(database.db, brregClient),
            validator,
            gjennomforingPublisher,
        )

        context("Upsert avtale") {
            val brregClient = mockk<BrregClient>()
            val gjennomforingPublisher = mockk<InitialLoadGjennomforinger>(relaxed = true)
            val avtaleService = createAvtaleService(brregClient, gjennomforingPublisher)

            test("får ikke opprette avtale dersom det oppstår valideringsfeil") {
                val request = AvtaleFixtures.avtaleRequest

                every { validator.validate(any(), any()) } returns listOf(
                    FieldError("navn", "Dårlig navn"),
                ).left()

                avtaleService.upsert(request, testNavIdent).shouldBeLeft(
                    listOf(FieldError("navn", "Dårlig navn")),
                )
            }

            test("får ikke opprette avtale dersom virksomhet ikke finnes i Brreg") {
                val request = AvtaleFixtures.avtaleRequest.copy(
                    arrangor = AvtaleFixtures.avtaleRequest.arrangor?.copy(
                        hovedenhet = Organisasjonsnummer("888777435"),
                        underenheter = listOf(),
                    ),
                )

                coEvery { brregClient.getBrregEnhet(Organisasjonsnummer("888777435")) } returns BrregError.NotFound.left()

                avtaleService.upsert(request, testNavIdent).shouldBeLeft(
                    listOf(
                        FieldError(
                            "/arrangor/hovedenhet",
                            "Tiltaksarrangøren finnes ikke i Brønnøysundregistrene",
                        ),
                    ),
                )
            }

            test("skedulerer publisering av gjennomføringer tilhørende avtalen") {
                val request = AvtaleFixtures.avtaleRequest

                avtaleService.upsert(request, testNavIdent)

                verify {
                    gjennomforingPublisher.schedule(
                        InitialLoadGjennomforinger.Input(avtaleId = request.id),
                        any(),
                        any(),
                    )
                }
            }
        }

        context("Administrator-notification") {
            val avtaleService = createAvtaleService()

            test("Ingen administrator-notification hvis administrator er samme som opprettet") {
                val identAnsatt1 = NavAnsattFixture.ansatt1.navIdent

                val avtale = AvtaleFixtures.avtaleRequest.copy(administratorer = listOf(identAnsatt1))
                avtaleService.upsert(avtale, identAnsatt1)

                database.run {
                    queries.notifications.getAll().shouldBeEmpty()
                }
            }

            test("Bare nye administratorer får notification når man endrer gjennomføring") {
                val identAnsatt1 = NavAnsattFixture.ansatt1.navIdent
                val identAnsatt2 = NavAnsattFixture.ansatt2.navIdent

                val avtale = AvtaleFixtures.avtaleRequest.copy(administratorer = listOf(identAnsatt2))
                avtaleService.upsert(avtale, identAnsatt1)

                database.run {
                    queries.notifications.getAll().shouldHaveSize(1).first().should {
                        it.user shouldBe identAnsatt2
                    }
                }
            }
        }
    }
}
