package no.nav.mulighetsrommet.api.fixtures

import io.kotest.assertions.arrow.core.shouldBeRight
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.repositories.NavAnsattRepository
import no.nav.mulighetsrommet.api.repositories.NavEnhetRepository
import no.nav.mulighetsrommet.database.FlywayDatabaseAdapter
import java.util.*

data class MulighetsrommetTestDomain(
    val enhet: NavEnhetDbo = NavEnhetDbo(
        navn = "IT",
        enhetsnummer = "2990",
        status = NavEnhetStatus.AKTIV,
        type = Norg2Type.DIR,
        overordnetEnhet = null,
    ),
    val ansatt1: NavAnsattDbo = NavAnsattDbo(
        navIdent = "DD1",
        fornavn = "Donald",
        etternavn = "Duck",
        hovedenhet = "2990",
        azureId = UUID.randomUUID(),
        fraAdGruppe = UUID.randomUUID(),
        mobilnummer = "12345678",
        epost = "test@test.no",
    ),
    val ansatt2: NavAnsattDbo = NavAnsattDbo(
        navIdent = "DD2",
        fornavn = "Dolly",
        etternavn = "Duck",
        hovedenhet = "2990",
        azureId = UUID.randomUUID(),
        fraAdGruppe = UUID.randomUUID(),
        mobilnummer = "48243214",
        epost = "test@testesen.no",
    ),
) {
    fun initialize(database: FlywayDatabaseAdapter) {
        database.clean()
        database.migrate()

        val enheter = NavEnhetRepository(database)
        enheter.upsert(enhet).shouldBeRight()

        val ansatte = NavAnsattRepository(database)
        ansatte.upsert(ansatt1).shouldBeRight()
        ansatte.upsert(ansatt2).shouldBeRight()
    }
}
