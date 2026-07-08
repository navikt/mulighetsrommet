package no.nav.mulighetsrommet.api.navenhet.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhet
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetType
import no.nav.mulighetsrommet.api.kostnadssted.Kostnadssted
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.NavEnhetNummer
import org.intellij.lang.annotations.Language

class KostnadsstedQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

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
            queries.enhet.save(
                createEnhet(
                    enhet = NavEnhetNummer("0200"),
                    type = NavEnhetType.FYLKE,
                    overordnetEnhet = null,
                ),
            )
            queries.enhet.save(
                createEnhet(
                    enhet = NavEnhetNummer("0106"),
                    type = NavEnhetType.LOKAL,
                    overordnetEnhet = NavEnhetNummer("0200"),
                ),
            )
            queries.enhet.save(
                createEnhet(
                    enhet = NavEnhetNummer("0101"),
                    type = NavEnhetType.LOKAL,
                    overordnetEnhet = NavEnhetNummer("0200"),
                ),
            )
            queries.enhet.save(
                createEnhet(
                    enhet = NavEnhetNummer("0128"),
                    type = NavEnhetType.LOKAL,
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

            queries.kostnadssted.getAll(listOf(NavEnhetNummer("0200"))) shouldContainExactlyInAnyOrder listOf(
                Kostnadssted(
                    navn = "Enhet 0106",
                    enhetsnummer = NavEnhetNummer("0106"),
                    region = Kostnadssted.Region(
                        navn = "Enhet 0200",
                        enhetsnummer = NavEnhetNummer("0200"),
                    ),
                ),
                Kostnadssted(
                    navn = "Enhet 0101",
                    enhetsnummer = NavEnhetNummer("0101"),
                    region = Kostnadssted.Region(
                        navn = "Enhet 0200",
                        enhetsnummer = NavEnhetNummer("0200"),
                    ),
                ),
                Kostnadssted(
                    navn = "Enhet 0200",
                    enhetsnummer = NavEnhetNummer("0200"),
                    region = Kostnadssted.Region(
                        navn = "Enhet 0200",
                        enhetsnummer = NavEnhetNummer("0200"),
                    ),
                ),
            )
        }
    }
})
