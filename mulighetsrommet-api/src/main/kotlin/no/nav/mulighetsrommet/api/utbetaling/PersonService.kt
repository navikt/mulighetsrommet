package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.getOrElse
import arrow.core.toNonEmptySetOrNull
import io.ktor.http.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.norg2.NorgError
import no.nav.mulighetsrommet.api.clients.pdl.GeografiskTilknytning
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.clients.pdl.tilPersonNavn
import no.nav.mulighetsrommet.api.utbetaling.pdl.HentAdressebeskyttetPersonBolkPdlQuery
import no.nav.mulighetsrommet.api.utbetaling.pdl.HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery
import no.nav.mulighetsrommet.api.utbetaling.pdl.HentPersonBolkResponse
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.tokenprovider.AccessType
import java.time.LocalDate

class PersonService(
    private val hentPersonOgGeografiskTilknytningQuery: HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery,
    private val hentPersonQuery: HentAdressebeskyttetPersonBolkPdlQuery,
    private val norg2Client: Norg2Client,
) {
    suspend fun getPersoner(identer: List<NorskIdent>): List<Person> {
        val pdlIdenter = identer
            .map { ident -> PdlIdent(ident.value) }
            .toNonEmptySetOrNull()
            ?: return emptyList()

        val pdlPersonData = hentPersonQuery.hentPersonBolk(pdlIdenter)
            .getOrElse {
                throw StatusException(
                    status = HttpStatusCode.InternalServerError,
                    detail = "Klarte ikke hente informasjon om personer",
                )
            }

        return pdlPersonData
            .map { (ident, pdlPerson) ->
                val gradering = pdlPerson.adressebeskyttelse.firstOrNull()?.gradering ?: PdlGradering.UGRADERT
                when (gradering) {
                    PdlGradering.UGRADERT -> pdlPerson.toPerson(ident)
                    else -> addressebeskyttetPerson(ident)
                }
            }
    }

    suspend fun getPersonerMedGeografiskEnhet(identer: List<NorskIdent>): List<Pair<Person, NavEnhetNummer?>> {
        val pdlIdenter = identer
            .map { ident -> PdlIdent(ident.value) }
            .toNonEmptySetOrNull()
            ?: return emptyList()

        val pdlPersonData = hentPersonOgGeografiskTilknytningQuery.hentPersonOgGeografiskTilknytningBolk(pdlIdenter, AccessType.M2M)
            .getOrElse {
                throw StatusException(
                    status = HttpStatusCode.InternalServerError,
                    detail = "Klarte ikke hente informasjon om personer og geografisk tilknytning",
                )
            }

        return pdlPersonData
            .map { (ident, pair) ->
                val (pdlPerson, geografiskTilknytning) = pair
                val gradering = pdlPerson.adressebeskyttelse.firstOrNull()?.gradering ?: PdlGradering.UGRADERT
                when (gradering) {
                    PdlGradering.UGRADERT -> pdlPerson.toPerson(ident) to geografiskTilknytning?.navEnhetNummer()
                    else -> addressebeskyttetPerson(ident) to null
                }
            }
    }

    private fun HentPersonBolkResponse.Person.toPerson(
        ident: PdlIdent,
    ) = Person(
        norskIdent = NorskIdent(ident.value),
        navn = if (this.navn.isNotEmpty()) tilPersonNavn(this.navn) else "Ukjent",
        foedselsdato = if (this.foedselsdato.isNotEmpty()) this.foedselsdato.first().foedselsdato else null,
    )

    private fun addressebeskyttetPerson(ident: PdlIdent) = Person(
        norskIdent = NorskIdent(ident.value),
        navn = "Adressebeskyttet",
        foedselsdato = null,
    )

    private suspend fun GeografiskTilknytning.navEnhetNummer(): NavEnhetNummer? {
        val norgEnhet = when (this) {
            is GeografiskTilknytning.GtBydel -> norg2Client.hentEnhetByGeografiskOmraade(
                this.value,
            )

            is GeografiskTilknytning.GtKommune -> norg2Client.hentEnhetByGeografiskOmraade(
                this.value,
            )

            else -> return null
        }

        return norgEnhet
            .map { it.enhetNr }
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
}

@Serializable
data class Person(
    val norskIdent: NorskIdent,
    val navn: String,
    @Serializable(with = LocalDateSerializer::class)
    val foedselsdato: LocalDate?,
)
