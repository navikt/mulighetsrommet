package no.nav.mulighetsrommet.api.individuell_gjennomforing.service

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.individuell_gjennomforing.api.IndividuellGjennomforingHandling
import no.nav.mulighetsrommet.api.individuell_gjennomforing.api.IndividuellGjennomforingRequest
import no.nav.mulighetsrommet.api.individuell_gjennomforing.db.IndividuellGjennomforingQueries.KontaktpersonDbo
import no.nav.mulighetsrommet.api.individuell_gjennomforing.model.IndividuellGjennomforing
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.api.validation.Validated
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID

class IndividuellGjennomforingService(
    private val db: ApiDatabase,
    private val navAnsattService: NavAnsattService,
) {
    fun upsert(request: IndividuellGjennomforingRequest): Validated<IndividuellGjennomforing> = IndividuellGjennomforingValidator.validate(request).map {
        val navEnheter = (request.navRegioner + request.navKontorer + request.navAndreEnheter).toSet()
        db.transaction {
            queries.individuellGjennomforing.upsert(
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
            queries.individuellGjennomforing.setAdministratorer(request.id, request.administratorer)
            queries.individuellGjennomforing.setNavEnheter(request.id, navEnheter)
            queries.individuellGjennomforing.setKontaktpersoner(
                request.id,
                request.kontaktpersoner.map { KontaktpersonDbo(it.navIdent, it.beskrivelse) }.toSet(),
            )
            if (request.arrangorId != null) {
                queries.individuellGjennomforing.setArrangorKontaktpersoner(
                    request.id,
                    request.arrangorKontaktpersoner,
                )
            } else {
                queries.individuellGjennomforing.setArrangorKontaktpersoner(request.id, emptySet())
            }
            queries.individuellGjennomforing.get(request.id)!!
        }
    }

    fun setPublisert(id: UUID, publisert: Boolean): Unit = db.transaction {
        queries.individuellGjennomforing.setPublisert(id, publisert)
    }

    fun getHandlinger(id: UUID, navIdent: NavIdent): Set<IndividuellGjennomforingHandling> {
        get(id) ?: return emptySet()
        val ansatt = navAnsattService.getNavAnsattByNavIdent(navIdent) ?: return emptySet()
        return IndividuellGjennomforingHandling.entries
            .filter { tilgangTilHandling(ansatt, it) }
            .toSet()
    }

    fun getAll(
        navEnheter: List<String> = emptyList(),
        tiltakstyper: List<Tiltakskode> = emptyList(),
    ): List<IndividuellGjennomforing> = db.session {
        val tiltakstypeIds = if (tiltakstyper.isNotEmpty()) {
            queries.tiltakstype.getAll(tiltakskoder = tiltakstyper.toSet()).map { it.id }
        } else {
            emptyList()
        }
        queries.individuellGjennomforing.getAll(
            navEnheter = navEnheter.map { no.nav.mulighetsrommet.model.NavEnhetNummer(it) },
            tiltakstyper = tiltakstypeIds,
        )
    }

    fun get(id: UUID): IndividuellGjennomforing? = db.session {
        queries.individuellGjennomforing.get(id)
    }

    companion object {
        private fun tilgangTilHandling(ansatt: NavAnsatt, handling: IndividuellGjennomforingHandling): Boolean {
            val skrivGjennomforing = ansatt.hasGenerellRolle(Rolle.TILTAKSGJENNOMFORINGER_SKRIV)
            return when (handling) {
                IndividuellGjennomforingHandling.PUBLISER -> skrivGjennomforing
                IndividuellGjennomforingHandling.REDIGER -> skrivGjennomforing
                IndividuellGjennomforingHandling.FORHANDSVIS_I_MODIA -> true
            }
        }
    }
}
