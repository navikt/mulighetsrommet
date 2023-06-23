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
import no.nav.mulighetsrommet.api.clients.msgraph.MicrosoftGraphClient
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle.BETABRUKER
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle.KONTAKTPERSON
import no.nav.mulighetsrommet.api.domain.dto.NavAnsattDto
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.repositories.NavAnsattRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import java.time.LocalDate
import java.util.*

class SynchronizeNavAnsatteTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val ansatt1 = NavAnsattDto(
        navident = "DD1",
        fornavn = "Donald",
        etternavn = "Duck",
        hovedenhetKode = "2990",
        hovedenhetNavn = "Andeby",
        azureId = UUID.randomUUID(),
        mobilnr = "12345678",
        epost = "donald.duck@nav.no",
    )
    val ansatt2 = NavAnsattDto(
        navident = "DD2",
        fornavn = "Dolly",
        etternavn = "Duck",
        hovedenhetKode = "2990",
        hovedenhetNavn = "Andeby",
        azureId = UUID.randomUUID(),
        mobilnr = "48243214",
        epost = "dolly.duck@nav.no",
    )

    context("resolveNavAnsatte") {
        test("should resolve all roles from the specified groups") {
            val betabrukerGroup = Group(adGruppe = UUID.randomUUID(), rolle = BETABRUKER)
            val kontaktpersonGroup = Group(adGruppe = UUID.randomUUID(), rolle = KONTAKTPERSON)

            val msGraph = mockk<MicrosoftGraphClient>()
            coEvery { msGraph.getGroupMembers(betabrukerGroup.adGruppe) } returns listOf(ansatt1, ansatt2)
            coEvery { msGraph.getGroupMembers(kontaktpersonGroup.adGruppe) } returns listOf(ansatt2)

            val ansatte = resolveNavAnsatte(listOf(betabrukerGroup, kontaktpersonGroup), msGraph)

            ansatte shouldContainExactlyInAnyOrder listOf(
                NavAnsattDbo.fromDto(ansatt1, listOf(BETABRUKER)),
                NavAnsattDbo.fromDto(ansatt2, listOf(BETABRUKER, KONTAKTPERSON)),
            )
        }
    }

    context("synchronizeNavAnsatte") {
        val domain = MulighetsrommetTestDomain(
            ansatt1 = NavAnsattDbo.fromDto(ansatt1),
            ansatt2 = NavAnsattDbo.fromDto(ansatt2),
        )
        domain.initialize(database.db)

        val betabrukerGroup = Group(adGruppe = UUID.randomUUID(), rolle = BETABRUKER)
        val kontaktpersonGroup = Group(adGruppe = UUID.randomUUID(), rolle = KONTAKTPERSON)

        val msGraph = mockk<MicrosoftGraphClient>()
        coEvery { msGraph.getGroupMembers(betabrukerGroup.adGruppe) } returns listOf(ansatt1, ansatt2)
        coEvery { msGraph.getGroupMembers(kontaktpersonGroup.adGruppe) } returns listOf(ansatt2)

        val ansatte = NavAnsattRepository(database.db)

        val task = SynchronizeNavAnsatte(
            config = SynchronizeNavAnsatte.Config(disabled = true),
            msGraphClient = msGraph,
            ansatte = ansatte,
            slack = mockk(),
        )

        test("should keep the nav_ansatt database table in sync with the specified groups") {
            forAll(
                row(
                    listOf(betabrukerGroup),
                    listOf(
                        NavAnsattDbo.fromDto(ansatt1, listOf(BETABRUKER)),
                        NavAnsattDbo.fromDto(ansatt2, listOf(BETABRUKER)),
                    ),
                ),
                row(
                    listOf(betabrukerGroup, kontaktpersonGroup),
                    listOf(
                        NavAnsattDbo.fromDto(ansatt1, listOf(BETABRUKER)),
                        NavAnsattDbo.fromDto(ansatt2, listOf(BETABRUKER, KONTAKTPERSON)),
                    ),
                ),
            ) { grupper, ansatteMedRoller ->
                runBlocking {
                    task.synchronizeNavAnsatte(grupper, LocalDate.now()).shouldBeRight()

                    ansatte.getAll().shouldBeRight().should {
                        it shouldContainExactlyInAnyOrder ansatteMedRoller
                    }
                }
            }
        }

        test("should schedule nav_ansatt to be deleted when they are not in the specified groups") {
            val skalSlettesDato = LocalDate.now().plusDays(1)

            forAll(
                row(
                    listOf(betabrukerGroup),
                    listOf(
                        NavAnsattDbo.fromDto(ansatt1, listOf(BETABRUKER)),
                        NavAnsattDbo.fromDto(ansatt2, listOf(BETABRUKER)),
                    ),
                ),
                row(
                    listOf(kontaktpersonGroup),
                    listOf(
                        NavAnsattDbo.fromDto(ansatt1, listOf(BETABRUKER)).copy(
                            skalSlettesDato = skalSlettesDato,
                        ),
                        NavAnsattDbo.fromDto(ansatt2, listOf(KONTAKTPERSON)),
                    ),
                ),
                row(
                    listOf(),
                    listOf(
                        NavAnsattDbo.fromDto(ansatt1, listOf(BETABRUKER)).copy(
                            skalSlettesDato = skalSlettesDato,
                        ),
                        NavAnsattDbo.fromDto(ansatt2, listOf(KONTAKTPERSON)).copy(
                            skalSlettesDato = skalSlettesDato,
                        ),
                    ),
                ),
            ) { grupper, ansatteMedRoller ->
                runBlocking {
                    task.synchronizeNavAnsatte(grupper, skalSlettesDato).shouldBeRight()

                    ansatte.getAll().shouldBeRight().should {
                        it shouldContainExactlyInAnyOrder ansatteMedRoller
                    }
                }
            }
        }

        test("should delete nav_ansatt when their deletion date matches the provided deletion date") {
            val skalSlettesDato = LocalDate.now()

            forAll(
                row(
                    listOf(betabrukerGroup, kontaktpersonGroup),
                    listOf(
                        NavAnsattDbo.fromDto(ansatt1, listOf(BETABRUKER)),
                        NavAnsattDbo.fromDto(ansatt2, listOf(BETABRUKER, KONTAKTPERSON)),
                    ),
                ),
                row(
                    listOf(kontaktpersonGroup),
                    listOf(
                        NavAnsattDbo.fromDto(ansatt2, listOf(KONTAKTPERSON)),
                    ),
                ),
                row(
                    listOf(),
                    listOf(),
                ),
            ) { grupper, ansatteMedRoller ->
                runBlocking {
                    task.synchronizeNavAnsatte(grupper, skalSlettesDato).shouldBeRight()

                    ansatte.getAll().shouldBeRight().should {
                        it shouldContainExactlyInAnyOrder ansatteMedRoller
                    }
                }
            }
        }
    }
})
