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

    test("CRUD") {
        val enheter = NavEnhetRepository(database.db)

        val overordnetEnhet = NavEnhetDbo(
            enhetsnummer = "1000",
            navn = "Enhet X",
            status = NavEnhetStatus.AKTIV,
            type = Norg2Type.LOKAL,
            overordnetEnhet = null,
        )
        val enhetSomSkalSlettes1 = overordnetEnhet.copy(
            enhetsnummer = "1",
            overordnetEnhet = overordnetEnhet.enhetsnummer,
        )
        val enhetSomSkalSlettes2 = overordnetEnhet.copy(
            enhetsnummer = "2",
            overordnetEnhet = overordnetEnhet.enhetsnummer,
        )
        val enhetSomSkalSlettes3 = overordnetEnhet.copy(
            enhetsnummer = "3",
            overordnetEnhet = overordnetEnhet.enhetsnummer,
        )
        val enhetSomSkalBeholdes1 = overordnetEnhet.copy(
            enhetsnummer = "4",
            overordnetEnhet = overordnetEnhet.enhetsnummer,
        )
        val enhetSomSkalBeholdes2 = overordnetEnhet.copy(
            enhetsnummer = "5",
            overordnetEnhet = overordnetEnhet.enhetsnummer,
        )

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
})
