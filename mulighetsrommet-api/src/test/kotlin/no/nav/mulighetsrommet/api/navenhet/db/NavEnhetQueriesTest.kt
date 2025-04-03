package no.nav.mulighetsrommet.api.navenhet.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.NavEnhetNummer
import org.intellij.lang.annotations.Language

class NavEnhetQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    fun createEnhet(
        enhet: NavEnhetNummer,
        overordnetEnhet: NavEnhetNummer?,
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
        database.runAndRollback { session ->
            val queries = NavEnhetQueries(session)

            val overordnetEnhet = createEnhet(NavEnhetNummer("1200"), null, Norg2Type.FYLKE)
            val underenhet1 = createEnhet(NavEnhetNummer("1111"), overordnetEnhet.enhetsnummer, Norg2Type.LOKAL)
            val underenhet2 = createEnhet(NavEnhetNummer("2222"), overordnetEnhet.enhetsnummer, Norg2Type.LOKAL)
            val underenhet3 = createEnhet(NavEnhetNummer("3333"), overordnetEnhet.enhetsnummer, Norg2Type.LOKAL)
            val underenhet4 = createEnhet(NavEnhetNummer("4444"), overordnetEnhet.enhetsnummer, Norg2Type.ALS)
            val underenhet5 = createEnhet(
                NavEnhetNummer("5555"),
                overordnetEnhet.enhetsnummer,
                Norg2Type.LOKAL,
                NavEnhetStatus.NEDLAGT,
            )

            queries.upsert(overordnetEnhet)
            queries.upsert(underenhet1)
            queries.upsert(underenhet2)
            queries.upsert(underenhet3)
            queries.upsert(underenhet4)
            queries.upsert(underenhet5)

            queries.getAll() shouldContainExactlyInAnyOrder listOf(
                overordnetEnhet,
                underenhet1,
                underenhet2,
                underenhet3,
                underenhet4,
                underenhet5,
            )

            queries.getAll(
                typer = listOf(
                    Norg2Type.FYLKE,
                    Norg2Type.ALS,
                ),
            ) shouldContainExactlyInAnyOrder listOf(
                overordnetEnhet,
                underenhet4,
            )

            queries.getAll(statuser = listOf(NavEnhetStatus.AKTIV)) shouldContainExactlyInAnyOrder listOf(
                overordnetEnhet,
                underenhet1,
                underenhet2,
                underenhet3,
                underenhet4,
            )

            queries.getAll(overordnetEnhet = overordnetEnhet.enhetsnummer) shouldContainExactlyInAnyOrder listOf(
                underenhet1,
                underenhet2,
                underenhet3,
                underenhet4,
                underenhet5,
            )

            queries.deleteWhereEnhetsnummer(
                listOf(
                    NavEnhetNummer("1111"),
                    NavEnhetNummer("2222"),
                    NavEnhetNummer("3333"),
                ),
            )

            queries.getAll() shouldContainExactlyInAnyOrder listOf(
                overordnetEnhet,
                underenhet4,
                underenhet5,
            )
        }
    }

    test("kostnadssted") {
        database.runAndRollback { session ->
            val queries = NavEnhetQueries(session)

            queries.upsert(createEnhet(enhet = NavEnhetNummer("0200"), type = Norg2Type.FYLKE, overordnetEnhet = null))
            queries.upsert(
                createEnhet(
                    enhet = NavEnhetNummer("0106"),
                    type = Norg2Type.LOKAL,
                    overordnetEnhet = NavEnhetNummer("0200"),
                ),
            )
            queries.upsert(
                createEnhet(
                    enhet = NavEnhetNummer("0101"),
                    type = Norg2Type.LOKAL,
                    overordnetEnhet = NavEnhetNummer("0200"),
                ),
            )
            queries.upsert(
                createEnhet(
                    enhet = NavEnhetNummer("0128"),
                    type = Norg2Type.LOKAL,
                    overordnetEnhet = NavEnhetNummer("0200"),
                ),
            )

            @Language("PostgreSQL")
            val setKostnadssteder = """
                insert into kostnadssted (enhetsnummer, region)
                values ('0106', '0200'), ('0101', '0200'), ('0200', '0200')
                on conflict do nothing;
            """.trimIndent()
            session.execute(queryOf(setKostnadssteder))

            queries.getKostnadssted(listOf(NavEnhetNummer("0200")))
                .map { it.enhetsnummer } shouldContainExactlyInAnyOrder listOf(
                NavEnhetNummer("0106"),
                NavEnhetNummer("0101"),
                NavEnhetNummer("0200"),
            )
        }
    }
})
