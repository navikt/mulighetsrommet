package no.nav.mulighetsrommet.api.persistence.navenhet.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhet
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetType
import no.nav.mulighetsrommet.api.persistence.SqlAdminDatabaseTestListener
import no.nav.mulighetsrommet.model.NavEnhetNummer

class NavEnhetQueriesTest : FunSpec({
    val database = extension(SqlAdminDatabaseTestListener())

    fun createEnhet(
        enhet: NavEnhetNummer,
        overordnetEnhet: NavEnhetNummer?,
        type: NavEnhetType,
        status: NavEnhetStatus = NavEnhetStatus.AKTIV,
    ) = NavEnhet(
        enhetsnummer = enhet,
        navn = "Enhet $enhet",
        status = status,
        type = type,
        overordnetEnhet = overordnetEnhet,
    )

    test("CRUD") {
        database.runAndRollback {
            val overordnetEnhet = createEnhet(NavEnhetNummer("1200"), null, NavEnhetType.FYLKE)
            val underenhet1 = createEnhet(NavEnhetNummer("1111"), overordnetEnhet.enhetsnummer, NavEnhetType.LOKAL)
            val underenhet2 = createEnhet(NavEnhetNummer("2222"), overordnetEnhet.enhetsnummer, NavEnhetType.LOKAL)
            val underenhet3 = createEnhet(NavEnhetNummer("3333"), overordnetEnhet.enhetsnummer, NavEnhetType.LOKAL)
            val underenhet4 = createEnhet(NavEnhetNummer("4444"), overordnetEnhet.enhetsnummer, NavEnhetType.ALS)
            val underenhet5 = createEnhet(
                NavEnhetNummer("5555"),
                overordnetEnhet.enhetsnummer,
                NavEnhetType.LOKAL,
                NavEnhetStatus.NEDLAGT,
            )

            repository.navEnhet.save(overordnetEnhet)
            repository.navEnhet.save(underenhet1)
            repository.navEnhet.save(underenhet2)
            repository.navEnhet.save(underenhet3)
            repository.navEnhet.save(underenhet4)
            repository.navEnhet.save(underenhet5)

            repository.navEnhet.getAll() shouldContainExactlyInAnyOrder listOf(
                overordnetEnhet,
                underenhet1,
                underenhet2,
                underenhet3,
                underenhet4,
                underenhet5,
            )

            repository.navEnhet.getAll(
                typer = listOf(
                    NavEnhetType.FYLKE,
                    NavEnhetType.ALS,
                ),
            ) shouldContainExactlyInAnyOrder listOf(
                overordnetEnhet,
                underenhet4,
            )

            repository.navEnhet.getAll(statuser = listOf(NavEnhetStatus.AKTIV)) shouldContainExactlyInAnyOrder listOf(
                overordnetEnhet,
                underenhet1,
                underenhet2,
                underenhet3,
                underenhet4,
            )

            repository.navEnhet.getAll(overordnetEnhet = overordnetEnhet.enhetsnummer) shouldContainExactlyInAnyOrder listOf(
                underenhet1,
                underenhet2,
                underenhet3,
                underenhet4,
                underenhet5,
            )

            repository.navEnhet.deleteWhereEnhetsnummer(
                listOf(
                    NavEnhetNummer("1111"),
                    NavEnhetNummer("2222"),
                    NavEnhetNummer("3333"),
                ),
            )

            repository.navEnhet.getAll() shouldContainExactlyInAnyOrder listOf(
                overordnetEnhet,
                underenhet4,
                underenhet5,
            )
        }
    }
})
