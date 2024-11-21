package no.nav.mulighetsrommet.api.navenhet.db

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import org.intellij.lang.annotations.Language

class NavEnhetRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    beforeEach {
        database.db.truncateAll()
    }

    fun createEnhet(
        enhet: String,
        overordnetEnhet: String?,
        type: Norg2Type,
        status: NavEnhetStatus = NavEnhetStatus.AKTIV,
    ) = NavEnhetDbo(
        enhetsnummer = enhet,
        navn = "Enhet $enhet",
        status = status,
        type = type,
        overordnetEnhet = overordnetEnhet,
    )

    test("CRUD") {
        val enheter = NavEnhetRepository(database.db)

        val overordnetEnhet = createEnhet("1200", null, Norg2Type.FYLKE)
        val underenhet1 = createEnhet("1", overordnetEnhet.enhetsnummer, Norg2Type.LOKAL)
        val underenhet2 = createEnhet("2", overordnetEnhet.enhetsnummer, Norg2Type.LOKAL)
        val underenhet3 = createEnhet("3", overordnetEnhet.enhetsnummer, Norg2Type.LOKAL)
        val underenhet4 = createEnhet("4", overordnetEnhet.enhetsnummer, Norg2Type.ALS)
        val underenhet5 = createEnhet("5", overordnetEnhet.enhetsnummer, Norg2Type.LOKAL, NavEnhetStatus.NEDLAGT)

        enheter.upsert(overordnetEnhet).shouldBeRight()
        enheter.upsert(underenhet1).shouldBeRight()
        enheter.upsert(underenhet2).shouldBeRight()
        enheter.upsert(underenhet3).shouldBeRight()
        enheter.upsert(underenhet4).shouldBeRight()
        enheter.upsert(underenhet5).shouldBeRight()

        enheter.getAll() shouldContainExactlyInAnyOrder listOf(
            overordnetEnhet,
            underenhet1,
            underenhet2,
            underenhet3,
            underenhet4,
            underenhet5,
        )

        enheter.getAll(typer = listOf(Norg2Type.FYLKE, Norg2Type.ALS)) shouldContainExactlyInAnyOrder listOf(
            overordnetEnhet,
            underenhet4,
        )

        enheter.getAll(statuser = listOf(NavEnhetStatus.AKTIV)) shouldContainExactlyInAnyOrder listOf(
            overordnetEnhet,
            underenhet1,
            underenhet2,
            underenhet3,
            underenhet4,
        )

        enheter.getAll(overordnetEnhet = overordnetEnhet.enhetsnummer) shouldContainExactlyInAnyOrder listOf(
            underenhet1,
            underenhet2,
            underenhet3,
            underenhet4,
            underenhet5,
        )

        enheter.deleteWhereEnhetsnummer(listOf("1", "2", "3"))

        enheter.getAll() shouldContainExactlyInAnyOrder listOf(
            overordnetEnhet,
            underenhet4,
            underenhet5,
        )
    }

    test("kostnadssted") {
        val enheter = NavEnhetRepository(database.db)
        enheter.upsert(createEnhet(enhet = "0200", type = Norg2Type.FYLKE, overordnetEnhet = null)).shouldBeRight()
        enheter.upsert(createEnhet(enhet = "0106", type = Norg2Type.LOKAL, overordnetEnhet = "0200")).shouldBeRight()
        enheter.upsert(createEnhet(enhet = "0101", type = Norg2Type.LOKAL, overordnetEnhet = "0200")).shouldBeRight()
        enheter.upsert(createEnhet(enhet = "0128", type = Norg2Type.LOKAL, overordnetEnhet = "0200")).shouldBeRight()

        @Language("PostgreSQL")
        val query = """
            insert into kostnadssted (enhetsnummer, region)
            values
            ('0106', '0200'),
            ('0101', '0200'),
            ('0200', '0200');
        """.trimIndent()
        database.db.run(queryOf(query).asExecute)

        enheter
            .getKostnadssted(listOf("0200"))
            .map { it.enhetsnummer } shouldContainExactlyInAnyOrder listOf(
            "0106",
            "0101",
            "0200",
        )
    }
})
