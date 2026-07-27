package no.nav.mulighetsrommet.api.persistence.kostnadssted

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import no.nav.mulighetsrommet.admin.kostnadssted.Kostnadssted
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhet
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetType
import no.nav.mulighetsrommet.api.persistence.SqlAdminDatabaseTestListener
import no.nav.mulighetsrommet.model.NavEnhetNummer

class KostnadsstedQueriesTest : FunSpec({
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

    test("kostnadssteder utledes fra eget kodeverk og henter navn fra registrerte nav-enheter") {
        database.runAndRollback {
            repository.navEnhet.save(
                createEnhet(
                    enhet = NavEnhetNummer("0600"),
                    type = NavEnhetType.FYLKE,
                    overordnetEnhet = null,
                ),
            )
            repository.navEnhet.save(
                createEnhet(
                    enhet = NavEnhetNummer("0602"),
                    type = NavEnhetType.LOKAL,
                    overordnetEnhet = NavEnhetNummer("0600"),
                ),
            )
            repository.navEnhet.save(
                createEnhet(
                    enhet = NavEnhetNummer("0617"),
                    type = NavEnhetType.LOKAL,
                    overordnetEnhet = NavEnhetNummer("0600"),
                ),
            )

            queries.kostnadssted.getAll(listOf(NavEnhetNummer("0600"))) shouldContainExactlyInAnyOrder listOf(
                Kostnadssted(
                    navn = "Enhet 0600",
                    enhetsnummer = NavEnhetNummer("0600"),
                    region = Kostnadssted.Region(
                        navn = "Enhet 0600",
                        enhetsnummer = NavEnhetNummer("0600"),
                    ),
                ),
                Kostnadssted(
                    navn = "Enhet 0602",
                    enhetsnummer = NavEnhetNummer("0602"),
                    region = Kostnadssted.Region(
                        navn = "Enhet 0600",
                        enhetsnummer = NavEnhetNummer("0600"),
                    ),
                ),
                Kostnadssted(
                    navn = "Enhet 0617",
                    enhetsnummer = NavEnhetNummer("0617"),
                    region = Kostnadssted.Region(
                        navn = "Enhet 0600",
                        enhetsnummer = NavEnhetNummer("0600"),
                    ),
                ),
            )
        }
    }
})
