package no.nav.mulighetsrommet.api.repositories

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
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
            )

            ansatte.upsert(ansatt).shouldBeRight()
            ansatte.upsert(ansatt2).shouldBeRight()

            ansatte.getByAzureIdAndAdGruppe(ansatt.azureId, adGruppe1) shouldBeRight ansatt
            ansatte.getByNavIdentAndAdGruppe(ansatt.navIdent, adGruppe1) shouldBeRight ansatt
            ansatte.getByAzureIdAndAdGruppe(ansatt2.azureId, adGruppe2) shouldBeRight ansatt2
            ansatte.getByNavIdentAndAdGruppe(ansatt2.navIdent, adGruppe2) shouldBeRight ansatt2

            ansatte.deleteByAzureId(ansatt.azureId).shouldBeRight()

            ansatte.getByAzureIdAndAdGruppe(ansatt.azureId, adGruppe1) shouldBeRight null
            ansatte.getByNavIdentAndAdGruppe(ansatt.navIdent, adGruppe1) shouldBeRight null
            ansatte.getByAzureIdAndAdGruppe(ansatt2.azureId, adGruppe2) shouldBeRight null
            ansatte.getByNavIdentAndAdGruppe(ansatt2.navIdent, adGruppe2) shouldBeRight null
        }
    }
})
