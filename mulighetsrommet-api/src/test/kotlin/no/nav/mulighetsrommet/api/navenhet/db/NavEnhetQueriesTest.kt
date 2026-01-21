package no.nav.mulighetsrommet.api.navenhet.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.NavEnhetNummer

class NavEnhetQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

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

            queries.enhet.upsert(overordnetEnhet)
            queries.enhet.upsert(underenhet1)
            queries.enhet.upsert(underenhet2)
            queries.enhet.upsert(underenhet3)
            queries.enhet.upsert(underenhet4)
            queries.enhet.upsert(underenhet5)

            queries.enhet.getAll() shouldContainExactlyInAnyOrder listOf(
                overordnetEnhet,
                underenhet1,
                underenhet2,
                underenhet3,
                underenhet4,
                underenhet5,
            )

            queries.enhet.getAll(
                typer = listOf(
                    Norg2Type.FYLKE,
                    Norg2Type.ALS,
                ),
            ) shouldContainExactlyInAnyOrder listOf(
                overordnetEnhet,
                underenhet4,
            )

            queries.enhet.getAll(statuser = listOf(NavEnhetStatus.AKTIV)) shouldContainExactlyInAnyOrder listOf(
                overordnetEnhet,
                underenhet1,
                underenhet2,
                underenhet3,
                underenhet4,
            )

            queries.enhet.getAll(overordnetEnhet = overordnetEnhet.enhetsnummer) shouldContainExactlyInAnyOrder listOf(
                underenhet1,
                underenhet2,
                underenhet3,
                underenhet4,
                underenhet5,
            )

            queries.enhet.deleteWhereEnhetsnummer(
                listOf(
                    NavEnhetNummer("1111"),
                    NavEnhetNummer("2222"),
                    NavEnhetNummer("3333"),
                ),
            )

            queries.enhet.getAll() shouldContainExactlyInAnyOrder listOf(
                overordnetEnhet,
                underenhet4,
                underenhet5,
            )
        }
    }
})
