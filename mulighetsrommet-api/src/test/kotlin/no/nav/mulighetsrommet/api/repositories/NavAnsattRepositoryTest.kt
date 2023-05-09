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
            val ansatt = NavAnsattDbo(
                oid = UUID.randomUUID(),
                navIdent = "DD123456",
                fornavn = "Donald",
                etternavn = "Duck",
                hovedenhet = "1000",
            )

            ansatte.upsert(ansatt).shouldBeRight()

            ansatte.getByObjectId(ansatt.oid) shouldBeRight ansatt
            ansatte.getByNavIdent(ansatt.navIdent) shouldBeRight ansatt

            ansatte.deleteByObjectId(ansatt.oid).shouldBeRight()

            ansatte.getByObjectId(ansatt.oid) shouldBeRight null
            ansatte.getByNavIdent(ansatt.navIdent) shouldBeRight null
        }
    }
})
