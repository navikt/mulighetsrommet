package no.nav.mulighetsrommet.api.tasks

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.should
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle.BETABRUKER
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle.KONTAKTPERSON
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.repositories.NavAnsattRepository
import no.nav.mulighetsrommet.api.services.AdGruppeNavAnsattRolleMapping
import no.nav.mulighetsrommet.api.services.NavAnsattService
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import java.time.LocalDate
import java.util.*

class SynchronizeNavAnsatteTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val domain = MulighetsrommetTestDomain()

    beforeAny {
        domain.initialize(database.db)
    }

    context("synchronizeNavAnsatte") {
        val betabruker = AdGruppeNavAnsattRolleMapping(adGruppeId = UUID.randomUUID(), rolle = BETABRUKER)
        val kontaktperson = AdGruppeNavAnsattRolleMapping(adGruppeId = UUID.randomUUID(), rolle = KONTAKTPERSON)

        val service = mockk<NavAnsattService>()
        coEvery { service.getNavAnsatteWithRoles(listOf()) } returns listOf()
        coEvery { service.getNavAnsatteWithRoles(listOf(betabruker)) } returns listOf(
            domain.ansatt1.copy(roller = listOf(BETABRUKER)),
            domain.ansatt2.copy(roller = listOf(BETABRUKER)),
        )
        coEvery { service.getNavAnsatteWithRoles(listOf(kontaktperson)) } returns listOf(
            domain.ansatt2.copy(roller = listOf(KONTAKTPERSON)),
        )
        coEvery { service.getNavAnsatteWithRoles(listOf(betabruker, kontaktperson)) } returns listOf(
            domain.ansatt1.copy(roller = listOf(BETABRUKER)),
            domain.ansatt2.copy(roller = listOf(BETABRUKER, KONTAKTPERSON)),
        )

        val ansatte = NavAnsattRepository(database.db)

        val task = SynchronizeNavAnsatte(
            config = SynchronizeNavAnsatte.Config(disabled = true),
            navAnsattService = service,
            ansatte = ansatte,
            slack = mockk(),
        )

        test("should schedule nav_ansatt to be deleted when they are not in the list of ansatte to sync") {
            val today = LocalDate.now()
            val deletionDate = today.plusDays(1)

            forAll(
                row(
                    listOf(betabruker, kontaktperson),
                    listOf(
                        domain.ansatt1.copy(roller = listOf(BETABRUKER)),
                        domain.ansatt2.copy(roller = listOf(BETABRUKER, KONTAKTPERSON)),
                    ),
                ),
                row(
                    listOf(betabruker),
                    listOf(
                        domain.ansatt1.copy(roller = listOf(BETABRUKER)),
                        domain.ansatt2.copy(roller = listOf(BETABRUKER)),
                    ),
                ),
                row(
                    listOf(kontaktperson),
                    listOf(
                        domain.ansatt1.copy(
                            roller = listOf(),
                            skalSlettesDato = deletionDate,
                        ),
                        domain.ansatt2.copy(roller = listOf(KONTAKTPERSON)),
                    ),
                ),
                row(
                    listOf(),
                    listOf(
                        domain.ansatt1.copy(
                            roller = listOf(),
                            skalSlettesDato = deletionDate,
                        ),
                        domain.ansatt2.copy(
                            roller = listOf(),
                            skalSlettesDato = deletionDate,
                        ),
                    ),
                ),
            ) { grupper, ansatteMedRoller ->
                runBlocking {
                    task.synchronizeNavAnsatte(grupper, today, deletionDate).shouldBeRight()

                    ansatte.getAll().shouldBeRight().should {
                        it shouldContainExactlyInAnyOrder ansatteMedRoller
                    }
                }
            }
        }

        test("should delete nav_ansatt when their deletion date matches the provided deletion date") {
            val today = LocalDate.now()

            forAll(
                row(
                    listOf(betabruker, kontaktperson),
                    listOf(
                        domain.ansatt1.copy(roller = listOf(BETABRUKER)),
                        domain.ansatt2.copy(roller = listOf(BETABRUKER, KONTAKTPERSON)),
                    ),
                ),
                row(
                    listOf(kontaktperson),
                    listOf(
                        domain.ansatt2.copy(roller = listOf(KONTAKTPERSON)),
                    ),
                ),
                row(
                    listOf(),
                    listOf(),
                ),
            ) { grupper, ansatteMedRoller ->
                runBlocking {
                    task.synchronizeNavAnsatte(grupper, today, today).shouldBeRight()

                    ansatte.getAll().shouldBeRight().should {
                        it shouldContainExactlyInAnyOrder ansatteMedRoller
                    }
                }
            }
        }
    }
})
