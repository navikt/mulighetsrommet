package no.nav.mulighetsrommet.api.services

import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.msgraph.AzureAdNavAnsatt
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle.BETABRUKER
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle.KONTAKTPERSON
import no.nav.mulighetsrommet.api.domain.dto.NavAnsattDto
import no.nav.mulighetsrommet.api.repositories.NavAnsattRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import java.util.*

class NavAnsattServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val ansatt1 = AzureAdNavAnsatt(
        navIdent = "DD1",
        fornavn = "Donald",
        etternavn = "Duck",
        hovedenhetKode = "2990",
        hovedenhetNavn = "Andeby",
        azureId = UUID.randomUUID(),
        mobilnummer = "12345678",
        epost = "donald.duck@nav.no",
    )
    val ansatt2 = AzureAdNavAnsatt(
        navIdent = "DD2",
        fornavn = "Dolly",
        etternavn = "Duck",
        hovedenhetKode = "2990",
        hovedenhetNavn = "Andeby",
        azureId = UUID.randomUUID(),
        mobilnummer = "48243214",
        epost = "dolly.duck@nav.no",
    )

    context("getNavAnsatteInAdGroups") {
        val ansatte = NavAnsattRepository(database.db)

        val betabrukerGroup = AdGruppeNavAnsattRolleMapping(adGruppeId = UUID.randomUUID(), rolle = BETABRUKER)
        val kontaktpersonGroup = AdGruppeNavAnsattRolleMapping(adGruppeId = UUID.randomUUID(), rolle = KONTAKTPERSON)

        test("should resolve all roles from the specified groups") {
            val msGraph = mockk<MicrosoftGraphService>()
            coEvery { msGraph.getNavAnsatteInGroup(betabrukerGroup.adGruppeId) } returns listOf(ansatt1, ansatt2)
            coEvery { msGraph.getNavAnsatteInGroup(kontaktpersonGroup.adGruppeId) } returns listOf(ansatt2)

            val service = NavAnsattService(
                microsoftGraphService = msGraph,
                ansatte = ansatte,
            )

            forAll(
                row(
                    listOf(betabrukerGroup),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, listOf(BETABRUKER)),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, listOf(BETABRUKER)),
                    ),
                ),
                row(
                    listOf(kontaktpersonGroup),
                    listOf(NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, listOf(KONTAKTPERSON))),
                ),
                row(
                    listOf(betabrukerGroup, kontaktpersonGroup),
                    listOf(
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt1, listOf(BETABRUKER)),
                        NavAnsattDto.fromAzureAdNavAnsatt(ansatt2, listOf(BETABRUKER, KONTAKTPERSON)),
                    ),
                ),
            ) { groups, ansatteMedRoller ->
                runBlocking {
                    val resolvedAnsatte = service.getNavAnsatteWithRoles(groups)

                    resolvedAnsatte shouldContainExactlyInAnyOrder ansatteMedRoller
                }
            }
        }
    }
})
