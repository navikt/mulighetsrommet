package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.getOrElse
import arrow.core.toNonEmptySetOrNull
import io.ktor.http.*
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.norg2.NorgError
import no.nav.mulighetsrommet.api.clients.pdl.GeografiskTilknytning
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.clients.pdl.tilPersonNavn
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakerPerson
import no.nav.mulighetsrommet.api.utbetaling.pdl.HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery
import no.nav.mulighetsrommet.api.utbetaling.pdl.HentPersonBolkResponse
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType

class DeltakerService(
    private val db: ApiDatabase,
    private val pdlQuery: HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery,
    private val norg2Client: Norg2Client,
) {
    suspend fun getDeltakereForKostnadsfordeling(deltakerIdenter: List<NorskIdent>): Map<NorskIdent, DeltakerPerson> {
        val personer = getPersoner(deltakerIdenter)

        val deltakereForKostnadsfordeling = personer.map { (ident, pair) ->
            val (person, geografiskTilknytning) = pair
            val gradering = person.adressebeskyttelse.firstOrNull()?.gradering ?: PdlGradering.UGRADERT

            if (gradering == PdlGradering.UGRADERT) {
                val navEnhet = geografiskTilknytning?.let { hentEnhetForGeografiskTilknytning(it) }
                val region = navEnhet?.overordnetEnhet?.let { hentNavEnhet(it) }
                val navn = if (person.navn.isNotEmpty()) tilPersonNavn(person.navn) else "Ukjent"
                val foedselsdato = if (person.foedselsdato.isNotEmpty()) person.foedselsdato.first() else null

                DeltakerPerson(
                    norskIdent = NorskIdent(ident.value),
                    navn = navn,
                    foedselsdato = foedselsdato?.foedselsdato,
                    geografiskEnhet = navEnhet,
                    region = region,
                )
            } else {
                DeltakerPerson(
                    norskIdent = NorskIdent(ident.value),
                    navn = "Adressebeskyttet person",
                    foedselsdato = null,
                    geografiskEnhet = null,
                    region = null,
                )
            }
        }.associateBy { it.norskIdent }

        return deltakereForKostnadsfordeling
    }

    private suspend fun getPersoner(deltakerIdenter: List<NorskIdent>): Map<PdlIdent, Pair<HentPersonBolkResponse.Person, GeografiskTilknytning?>> {
        val identer = deltakerIdenter
            .map { ident -> PdlIdent(ident.value) }
            .toNonEmptySetOrNull()
            ?: return mapOf()

        return pdlQuery.hentPersonOgGeografiskTilknytningBolk(identer, AccessType.M2M).getOrElse {
            throw StatusException(
                status = HttpStatusCode.InternalServerError,
                detail = "Klarte ikke hente informasjon om personer og geografisk tilknytning",
            )
        }
    }

    private suspend fun hentEnhetForGeografiskTilknytning(geografiskTilknytning: GeografiskTilknytning): NavEnhetDbo? {
        val norgEnhet = when (geografiskTilknytning) {
            is GeografiskTilknytning.GtBydel -> norg2Client.hentEnhetByGeografiskOmraade(
                geografiskTilknytning.value,
            )

            is GeografiskTilknytning.GtKommune -> norg2Client.hentEnhetByGeografiskOmraade(
                geografiskTilknytning.value,
            )

            else -> return null
        }

        return norgEnhet
            .map { hentNavEnhet(it.enhetNr) }
            .getOrElse {
                when (it) {
                    NorgError.NotFound -> null
                    NorgError.Error -> throw StatusException(
                        HttpStatusCode.InternalServerError,
                        "Fant ikke navenhet til geografisk tilknytning.",
                    )
                }
            }
    }

    private fun hentNavEnhet(enhetsNummer: NavEnhetNummer) = db.session {
        queries.enhet.get(enhetsNummer)
    }
}
