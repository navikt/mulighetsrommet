package no.nav.mulighetsrommet.api.tiltakdokument.service

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.api.tiltakdokument.api.TiltakDokumentHandling
import no.nav.mulighetsrommet.api.tiltakdokument.api.TiltakDokumentRequest
import no.nav.mulighetsrommet.api.tiltakdokument.db.TiltakDokumentQueries.KontaktpersonDbo
import no.nav.mulighetsrommet.api.tiltakdokument.model.TiltakDokument
import no.nav.mulighetsrommet.api.validation.Validated
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID

class TiltakDokumentService(
    private val db: ApiDatabase,
    private val navAnsattService: NavAnsattService,
) {
    fun upsert(request: TiltakDokumentRequest): Validated<TiltakDokument> = TiltakDokumentValidator.validate(request).map {
        val navEnheter = (request.navRegioner + request.navKontorer + request.navAndreEnheter).toSet()
        db.transaction {
            queries.tiltakDokument.upsert(
                id = request.id,
                navn = request.navn.trim(),
                tiltakstypeId = request.tiltakstypeId,
                stedForGjennomforing = request.stedForGjennomforing,
                arrangorId = request.arrangorId,
                faneinnhold = request.faneinnhold,
                beskrivelse = request.beskrivelse,
                tiltaksnummer = null,
                sanityId = null,
            )
            queries.tiltakDokument.setAdministratorer(request.id, request.administratorer)
            queries.tiltakDokument.setNavEnheter(request.id, navEnheter)
            queries.tiltakDokument.setKontaktpersoner(
                request.id,
                request.kontaktpersoner.map { KontaktpersonDbo(it.navIdent, it.beskrivelse) }.toSet(),
            )
            if (request.arrangorId != null) {
                queries.tiltakDokument.setArrangorKontaktpersoner(
                    request.id,
                    request.arrangorKontaktpersoner,
                )
            } else {
                queries.tiltakDokument.setArrangorKontaktpersoner(request.id, emptySet())
            }
            queries.tiltakDokument.get(request.id)!!
        }
    }

    fun setPublisert(id: UUID, publisert: Boolean): Unit = db.transaction {
        queries.tiltakDokument.setPublisert(id, publisert)
    }

    fun getHandlinger(id: UUID, navIdent: NavIdent): Set<TiltakDokumentHandling> {
        get(id) ?: return emptySet()
        val ansatt = navAnsattService.getNavAnsattByNavIdent(navIdent) ?: return emptySet()
        return TiltakDokumentHandling.entries
            .filter { tilgangTilHandling(ansatt, it) }
            .toSet()
    }

    fun getAll(
        navEnheter: List<String> = emptyList(),
        tiltakstyper: List<Tiltakskode> = emptyList(),
    ): List<TiltakDokument> = db.session {
        val tiltakstypeIds = if (tiltakstyper.isNotEmpty()) {
            queries.tiltakstype.getAll(tiltakskoder = tiltakstyper.toSet()).map { it.id }
        } else {
            emptyList()
        }
        queries.tiltakDokument.getAll(
            navEnheter = navEnheter.map { no.nav.mulighetsrommet.model.NavEnhetNummer(it) },
            tiltakstyper = tiltakstypeIds,
        )
    }

    fun get(id: UUID): TiltakDokument? = db.session {
        queries.tiltakDokument.get(id)
    }

    companion object {
        private fun tilgangTilHandling(ansatt: NavAnsatt, handling: TiltakDokumentHandling): Boolean {
            val skrivGjennomforing = ansatt.hasGenerellRolle(Rolle.TILTAKSGJENNOMFORINGER_SKRIV)
            return when (handling) {
                TiltakDokumentHandling.PUBLISER -> skrivGjennomforing
                TiltakDokumentHandling.REDIGER -> skrivGjennomforing
                TiltakDokumentHandling.FORHANDSVIS_I_MODIA -> true
            }
        }
    }
}
