package no.nav.mulighetsrommet.api.delmedbruker

import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NorskIdent
import java.util.UUID

interface DelMedBrukerQueryHandler {
    fun insert(dbo: DelMedBrukerDbo, deltFraFylke: NavEnhetNummer?)

    fun getLast(norskIdent: NorskIdent, tiltakDokumentOrGjennomforingId: UUID): DelMedBrukerDto?

    fun getAll(norskIdent: NorskIdent): List<DelMedBrukerDto>
}
