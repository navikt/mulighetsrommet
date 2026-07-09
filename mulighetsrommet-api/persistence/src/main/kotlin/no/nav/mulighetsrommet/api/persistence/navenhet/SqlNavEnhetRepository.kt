package no.nav.mulighetsrommet.api.persistence.navenhet

import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhet
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetRepository
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetType
import no.nav.mulighetsrommet.api.persistence.navenhet.db.NavEnhetQueries
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.model.NavEnhetNummer

class SqlNavEnhetRepository(private val db: Database) : NavEnhetRepository {
    override fun save(navEnhet: NavEnhet) {
        db.session { NavEnhetQueries(it).save(navEnhet) }
    }

    override fun get(enhetsnummer: NavEnhetNummer): NavEnhet? {
        return db.session { NavEnhetQueries(it).get(enhetsnummer) }
    }

    override fun getAll(
        statuser: List<NavEnhetStatus>?,
        typer: List<NavEnhetType>?,
        overordnetEnhet: NavEnhetNummer?,
    ): List<NavEnhet> {
        return db.session { NavEnhetQueries(it).getAll(statuser, typer, overordnetEnhet) }
    }

    override fun deleteWhereEnhetsnummer(enhetsnummerForSletting: List<NavEnhetNummer>) {
        db.session { NavEnhetQueries(it).deleteWhereEnhetsnummer(enhetsnummerForSletting) }
    }
}
