package no.nav.mulighetsrommet.api.services

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.msgraph.AzureAdNavAnsatt
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle.BETABRUKER
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle.KONTAKTPERSON
import no.nav.mulighetsrommet.api.domain.dto.AdGruppe
import no.nav.mulighetsrommet.api.domain.dto.NavAnsattDto
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.repositories.NavAnsattRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import java.time.LocalDate
import java.util.*

class NavAnsattServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val domain = MulighetsrommetTestDomain()

    beforeEach {
        database.db.truncateAll()
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

    val ansatt1 = toAzureAdNavAnsattDto(NavAnsattFixture.ansatt1)
    val ansatt2 = toAzureAdNavAnsattDto(NavAnsattFixture.ansatt2)

    val betabruker = AdGruppeNavAnsattRolleMapping(adGruppeId = UUID.randomUUID(), rolle = BETABRUKER)
    val kontaktperson = AdGruppeNavAnsattRolleMapping(adGruppeId = UUID.randomUUID(), rolle = KONTAKTPERSON)

    val msGraph = mockk<MicrosoftGraphService>()
    coEvery { msGraph.getNavAnsatteInGroup(betabruker.adGruppeId) } returns listOf(ansatt1, ansatt2)
    coEvery { msGraph.getNavAnsatteInGroup(kontaktperson.adGruppeId) } returns listOf(ansatt2)

    val sanityAnsattService = mockk<SanityAnsattService>()

    context("getNavAnsattFromAzure") {
        test("should get NavAnsatt with roles filtered by the configured roles") {
            val service = NavAnsattService(
                microsoftGraphService = msGraph,
                ansatte = NavAnsattRepository(database.db),
                roles = listOf(betabruker),
                sanityAnsattService = sanityAnsattService,
            )

            val azureId = UUID.randomUUID()

            coEvery { msGraph.getNavAnsatt(azureId) } returns ansatt1
            coEvery { msGraph.getNavAnsattAdGrupper(azureId) } returns listOf(
                AdGruppe(id = betabruker.adGruppeId, navn = "Betabruker"),
                AdGruppe(
                    id = UUID.randomUUID(),
                    navn = "Tilfeldig AD-gruppe som ikke har en innvirkning på den ansattes roller",
                ),
            )

            service.getNavAnsattFromAzure(azureId) shouldBe NavAnsattDto.fromAzureAdNavAnsatt(
                ansatt1,
                setOf(BETABRUKER),
            )
        }

        test("should fail when the requested NavAnsatt does not have any of the configured roles") {
            val service = NavAnsattService(
                microsoftGraphService = msGraph,
                ansatte = NavAnsattRepository(database.db),
                roles = listOf(kontaktperson),
                sanityAnsattService = sanityAnsattService,
            )

            val azureId = UUID.randomUUID()

            coEvery { msGraph.getNavAnsatt(azureId) } returns ansatt1
            coEvery { msGraph.getNavAnsattAdGrupper(azureId) } returns listOf(
                AdGruppe(id = betabruker.adGruppeId, navn = "Betabruker"),
            )

            shouldThrow<IllegalStateException> {
                service.getNavAnsattFromAzure(azureId)
            }
        }
    }

    context("getNavAnsatteFromAzure") {
        test("should resolve all roles from the specified groups") {
            forAll(
                row(
                    listOf(betabruker),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf(BETABRUKER)),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(BETABRUKER)),
                    ),
                ),
                row(
                    listOf(kontaktperson),
                    listOf(NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(KONTAKTPERSON))),
                ),
                row(
                    listOf(betabruker, kontaktperson),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf(BETABRUKER)),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(BETABRUKER, KONTAKTPERSON)),
                    ),
                ),
            ) { roles, ansatteMedRoller ->
                runBlocking {
                    val service = NavAnsattService(
                        microsoftGraphService = msGraph,
                        ansatte = NavAnsattRepository(database.db),
                        roles = roles,
                        sanityAnsattService = sanityAnsattService,
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
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf(BETABRUKER)),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(BETABRUKER, KONTAKTPERSON)),
                    ),
                ),
                row(
                    listOf(betabruker),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf(BETABRUKER)),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(BETABRUKER)),
                    ),
                ),
                row(
                    listOf(kontaktperson),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf()).copy(
                            skalSlettesDato = deletionDate,
                        ),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(KONTAKTPERSON)),
                    ),
                ),
                row(
                    listOf(),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf()).copy(
                            skalSlettesDato = deletionDate,
                        ),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf()).copy(
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
                        sanityAnsattService = sanityAnsattService,
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
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, setOf(BETABRUKER)),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(BETABRUKER, KONTAKTPERSON)),
                    ),
                ),
                row(
                    listOf(kontaktperson),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, setOf(KONTAKTPERSON)),
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
                        sanityAnsattService = sanityAnsattService,
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
