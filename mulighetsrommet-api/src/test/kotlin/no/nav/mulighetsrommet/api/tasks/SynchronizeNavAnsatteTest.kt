package no.nav.mulighetsrommet.api.tasks

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.msgraph.MicrosoftGraphClient
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle.BETABRUKER
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle.KONTAKTPERSON
import no.nav.mulighetsrommet.api.domain.dto.NavAnsattDto
import java.util.*

class SynchronizeNavAnsatteTest : FunSpec({
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
        test("should resolve all roles from the provided groups") {
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
})
