package no.nav.mulighetsrommet.api.repositories

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.utils.NavAnsattFilter
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.utils.getOrThrow
import java.util.*

class NavAnsattRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    context("NavAnsattRepository") {
        val enheter = NavEnhetRepository(database.db)
        val ansatte = NavAnsattRepository(database.db)

        beforeAny {
            val enhet = NavEnhetDbo(
                enhetsnummer = "1000",
                navn = "Andeby",
                status = NavEnhetStatus.AKTIV,
                type = Norg2Type.LOKAL,
                overordnetEnhet = null,
            )
            enheter.upsert(enhet).shouldBeRight()
        }

        test("CRUD") {
            val adGruppe1 = UUID.randomUUID()
            val adGruppe2 = UUID.randomUUID()
            val azureId = UUID.randomUUID()

            val ansatt = NavAnsattDbo(
                azureId = azureId,
                navIdent = "DD123456",
                fornavn = "Donald",
                etternavn = "Duck",
                hovedenhet = "1000",
                fraAdGruppe = adGruppe1,
                mobilnummer = "12345678",
                epost = "test@test.no",
                rolle = NavAnsattRolle.BETABRUKER,
            )

            val ansatt2 = NavAnsattDbo(
                azureId = azureId,
                navIdent = "DD123456",
                fornavn = "Donald",
                etternavn = "Duck",
                hovedenhet = "1000",
                fraAdGruppe = adGruppe2,
                mobilnummer = "12345678",
                epost = "test@test.no",
                rolle = NavAnsattRolle.KONTAKTPERSON,
            )

            ansatte.upsert(ansatt).shouldBeRight()
            ansatte.upsert(ansatt2).shouldBeRight()

            ansatte.getByAzureIdAndAdGruppe(ansatt.azureId, NavAnsattRolle.BETABRUKER) shouldBeRight ansatt
            ansatte.getByNavIdentAndRolle(ansatt.navIdent, NavAnsattRolle.BETABRUKER) shouldBeRight ansatt
            ansatte.getByAzureIdAndAdGruppe(ansatt2.azureId, NavAnsattRolle.KONTAKTPERSON) shouldBeRight ansatt2
            ansatte.getByNavIdentAndRolle(ansatt2.navIdent, NavAnsattRolle.KONTAKTPERSON) shouldBeRight ansatt2

            ansatte.deleteByAzureId(ansatt.azureId).shouldBeRight()

            ansatte.getByAzureIdAndAdGruppe(ansatt.azureId, NavAnsattRolle.BETABRUKER) shouldBeRight null
            ansatte.getByNavIdentAndRolle(ansatt.navIdent, NavAnsattRolle.BETABRUKER) shouldBeRight null
            ansatte.getByAzureIdAndAdGruppe(ansatt2.azureId, NavAnsattRolle.KONTAKTPERSON) shouldBeRight null
            ansatte.getByNavIdentAndRolle(ansatt2.navIdent, NavAnsattRolle.KONTAKTPERSON) shouldBeRight null
        }

        test("Skal hente alle ansatte for en gitt ad-gruppe") {
            val adGruppe1 = UUID.randomUUID()
            val adGruppe2 = UUID.randomUUID()
            val azureId = UUID.randomUUID()

            val ansatt = NavAnsattDbo(
                azureId = azureId,
                navIdent = "DD123456",
                fornavn = "Donald",
                etternavn = "Duck",
                hovedenhet = "1000",
                fraAdGruppe = adGruppe1,
                mobilnummer = "12345678",
                epost = "test@test.no",
                rolle = NavAnsattRolle.BETABRUKER,
            )

            val ansatt2 = NavAnsattDbo(
                azureId = azureId,
                navIdent = "DD123456",
                fornavn = "Donald",
                etternavn = "Duck",
                hovedenhet = "1000",
                fraAdGruppe = adGruppe2,
                mobilnummer = "12345678",
                epost = "test@test.no",
                rolle = NavAnsattRolle.KONTAKTPERSON,
            )

            ansatte.upsert(ansatt).shouldBeRight()
            ansatte.upsert(ansatt2).shouldBeRight()

            val result =
                ansatte.getAll(filter = NavAnsattFilter(roller = listOf(NavAnsattRolle.KONTAKTPERSON))).getOrThrow()
            result.size shouldBe 1
        }
    }
})
