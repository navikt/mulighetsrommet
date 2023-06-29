package no.nav.mulighetsrommet.api.services

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.should
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.msgraph.AzureAdNavAnsatt
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

class NavAnsattServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val domain = MulighetsrommetTestDomain()

    beforeAny {
        domain.initialize(database.db)
    }

    fun toAzureAdNavAnsattDto(dbo: NavAnsattDbo) = AzureAdNavAnsatt(
        azureId = dbo.azureId,
        navIdent = dbo.navIdent,
        fornavn = dbo.fornavn,
        etternavn = dbo.etternavn,
        hovedenhetKode = domain.enhet.enhetsnummer,
        hovedenhetNavn = domain.enhet.navn,
        mobilnummer = dbo.mobilnummer,
        epost = dbo.epost,
    )

    val ansatt1 = toAzureAdNavAnsattDto(domain.ansatt1)
    val ansatt2 = toAzureAdNavAnsattDto(domain.ansatt2)

    val betabruker = AdGruppeNavAnsattRolleMapping(adGruppeId = UUID.randomUUID(), rolle = BETABRUKER)
    val kontaktperson = AdGruppeNavAnsattRolleMapping(adGruppeId = UUID.randomUUID(), rolle = KONTAKTPERSON)

    val msGraph = mockk<MicrosoftGraphService>()
    coEvery { msGraph.getNavAnsatteInGroup(betabruker.adGruppeId) } returns listOf(ansatt1, ansatt2)
    coEvery { msGraph.getNavAnsatteInGroup(kontaktperson.adGruppeId) } returns listOf(ansatt2)

    context("getNavAnsatteFromAzure") {

        test("should resolve all roles from the specified groups") {
            forAll(
                row(
                    listOf(betabruker),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, listOf(BETABRUKER)),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, listOf(BETABRUKER)),
                    ),
                ),
                row(
                    listOf(kontaktperson),
                    listOf(NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, listOf(KONTAKTPERSON))),
                ),
                row(
                    listOf(betabruker, kontaktperson),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, listOf(BETABRUKER)),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, listOf(BETABRUKER, KONTAKTPERSON)),
                    ),
                ),
            ) { roles, ansatteMedRoller ->
                runBlocking {
                    val service = NavAnsattService(
                        microsoftGraphService = msGraph,
                        ansatte = NavAnsattRepository(database.db),
                        roles = roles,
                    )

                    val resolvedAnsatte = service.getNavAnsatteFromAzure()

                    resolvedAnsatte shouldContainExactlyInAnyOrder ansatteMedRoller
                }
            }
        }
    }

    context("synchronizeNavAnsatteFromAzure") {
        val ansatte = NavAnsattRepository(database.db)

        test("should schedule nav_ansatt to be deleted when they are not in the list of ansatte to sync") {
            val today = LocalDate.now()
            val deletionDate = today.plusDays(1)

            forAll(
                row(
                    listOf(betabruker, kontaktperson),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, listOf(BETABRUKER)),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, listOf(BETABRUKER, KONTAKTPERSON)),
                    ),
                ),
                row(
                    listOf(betabruker),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, listOf(BETABRUKER)),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, listOf(BETABRUKER)),
                    ),
                ),
                row(
                    listOf(kontaktperson),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, listOf()).copy(
                            skalSlettesDato = deletionDate,
                        ),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, listOf(KONTAKTPERSON)),
                    ),
                ),
                row(
                    listOf(),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, listOf()).copy(
                            skalSlettesDato = deletionDate,
                        ),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, listOf()).copy(
                            skalSlettesDato = deletionDate,
                        ),
                    ),
                ),
            ) { roles, ansatteMedRoller ->
                runBlocking {
                    val service = NavAnsattService(
                        microsoftGraphService = msGraph,
                        ansatte = ansatte,
                        roles = roles,
                    )

                    service.synchronizeNavAnsatte(today, deletionDate).shouldBeRight()

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
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, listOf(BETABRUKER)),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, listOf(BETABRUKER, KONTAKTPERSON)),
                    ),
                ),
                row(
                    listOf(kontaktperson),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, listOf(KONTAKTPERSON)),
                    ),
                ),
                row(
                    listOf(),
                    listOf(),
                ),
            ) { roles, ansatteMedRoller ->
                runBlocking {
                    val service = NavAnsattService(
                        microsoftGraphService = msGraph,
                        ansatte = ansatte,
                        roles = roles,
                    )

                    service.synchronizeNavAnsatte(today, deletionDate = today).shouldBeRight()

                    ansatte.getAll().shouldBeRight().should {
                        it shouldContainExactlyInAnyOrder ansatteMedRoller
                    }
                }
            }
        }
    }
})
