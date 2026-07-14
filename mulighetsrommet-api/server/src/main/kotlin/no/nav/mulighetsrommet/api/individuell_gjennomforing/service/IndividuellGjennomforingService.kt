package no.nav.mulighetsrommet.api.individuell_gjennomforing.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.individuell_gjennomforing.api.IndividuellGjennomforingRequest
import no.nav.mulighetsrommet.api.individuell_gjennomforing.db.IndividuellGjennomforingQueries.KontaktpersonDbo
import no.nav.mulighetsrommet.api.individuell_gjennomforing.model.IndividuellGjennomforing
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID

class IndividuellGjennomforingService(private val db: ApiDatabase) {

    fun upsert(request: IndividuellGjennomforingRequest): Either<List<FieldError>, IndividuellGjennomforing> {
        val errors = buildList {
            if (request.navn.isBlank()) {
                add(FieldError(pointer = "/navn", detail = "Navn er påkrevd"))
            } else if (request.navn.length > 500) {
                add(FieldError(pointer = "/navn", detail = "Navn kan ikke være lengre enn 500 tegn"))
            }
            if (request.administratorer.isEmpty()) {
                add(FieldError(pointer = "/administratorer", detail = "Du må velge minst én administrator"))
            }
        }

        if (errors.isNotEmpty()) {
            return errors.left()
        }

        val navEnheter = (request.navRegioner + request.navKontorer + request.navAndreEnheter).toSet()

        return db.transaction {
            queries.individuellGjennomforing.upsert(
                id = request.id,
                navn = request.navn.trim(),
                tiltakstypeId = request.tiltakstypeId,
                stedForGjennomforing = request.stedForGjennomforing,
                arrangorId = request.arrangorId,
                faneinnhold = request.faneinnhold,
                beskrivelse = request.beskrivelse,
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
            queries.individuellGjennomforing.get(request.id)!!.right()
        }
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
}
