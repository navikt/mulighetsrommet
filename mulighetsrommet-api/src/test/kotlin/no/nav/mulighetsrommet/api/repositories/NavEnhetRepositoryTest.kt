package no.nav.mulighetsrommet.api.repositories

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener

class NavEnhetRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    beforeEach {
        database.db.clean()
        database.db.migrate()
    }

    context("Synkronisering av enheter") {
        val enheter = NavEnhetRepository(database.db)

        test("CRUD") {
            val overordnetEnhet = NavEnhetDbo(
                enhetNr = "1000",
                navn = "Enhet X",
                status = NavEnhetStatus.AKTIV,
                type = Norg2Type.LOKAL,
                overordnetEnhet = null,
            )
            val enhetSomSkalSlettes1 = overordnetEnhet.copy(enhetNr = "1", overordnetEnhet = overordnetEnhet.enhetNr)
            val enhetSomSkalSlettes2 = overordnetEnhet.copy(enhetNr = "2", overordnetEnhet = overordnetEnhet.enhetNr)
            val enhetSomSkalSlettes3 = overordnetEnhet.copy(enhetNr = "3", overordnetEnhet = overordnetEnhet.enhetNr)
            val enhetSomSkalBeholdes1 = overordnetEnhet.copy(enhetNr = "4", overordnetEnhet = overordnetEnhet.enhetNr)
            val enhetSomSkalBeholdes2 = overordnetEnhet.copy(enhetNr = "5", overordnetEnhet = overordnetEnhet.enhetNr)

            enheter.upsert(overordnetEnhet).shouldBeRight()
            enheter.upsert(enhetSomSkalSlettes1).shouldBeRight()
            enheter.upsert(enhetSomSkalSlettes2).shouldBeRight()
            enheter.upsert(enhetSomSkalSlettes3).shouldBeRight()
            enheter.upsert(enhetSomSkalBeholdes1).shouldBeRight()
            enheter.upsert(enhetSomSkalBeholdes2).shouldBeRight()

            enheter.getAll() shouldContainExactlyInAnyOrder listOf(
                overordnetEnhet,
                enhetSomSkalSlettes1,
                enhetSomSkalSlettes2,
                enhetSomSkalSlettes3,
                enhetSomSkalBeholdes1,
                enhetSomSkalBeholdes2,
            )

            enheter.deleteWhereEnhetsnummer(listOf("1", "2", "3"))

            enheter.getAll() shouldContainExactlyInAnyOrder listOf(
                overordnetEnhet,
                enhetSomSkalBeholdes1,
                enhetSomSkalBeholdes2,
            )
        }
    }
})
