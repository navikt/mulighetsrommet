package no.nav.mulighetsrommet.api.delmedbruker

import no.nav.mulighetsrommet.api.veilederflate.NavEnhetService
import no.nav.mulighetsrommet.api.veilederflate.VeilederflateDatabase
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.teamLogsInfo
import org.slf4j.LoggerFactory
import java.util.UUID

class DelMedBrukerService(
    private val db: VeilederflateDatabase,
    private val navEnhetService: NavEnhetService,
) {
    private val logger = LoggerFactory.getLogger(DelMedBrukerService::class.java)

    fun insertDelMedBruker(dbo: DelMedBrukerDbo): Unit = db.session {
        logger.teamLogsInfo(
            "Veileder (${dbo.navIdent}) deler tiltak med id: '${dbo.tiltakDokumentId ?: dbo.gjennomforingId}' med bruker (${dbo.norskIdent.value})",
        )

        val fylke = navEnhetService.hentOverordnetFylkesenhet(dbo.deltFraEnhet)

        queries.delMedBruker.insert(dbo, fylke?.enhetsnummer)
    }

    fun getLast(fnr: NorskIdent, tiltakDokumentOrGjennomforingId: UUID): DelMedBrukerDto? = db.session {
        queries.delMedBruker.getLast(fnr, tiltakDokumentOrGjennomforingId)
    }

    fun getAll(fnr: NorskIdent): List<DelMedBrukerDto> = db.session {
        queries.delMedBruker.getAll(fnr)
    }
}
