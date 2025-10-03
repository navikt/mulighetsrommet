package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.getOrElse
import arrow.core.toNonEmptySetOrNull
import io.ktor.http.*
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerClient
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.norg2.NorgError
import no.nav.mulighetsrommet.api.clients.pdl.*
import no.nav.mulighetsrommet.api.navenhet.NavEnhetDto
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.utbetaling.pdl.HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery
import no.nav.mulighetsrommet.api.utbetaling.pdl.PdlPerson
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType
import java.time.LocalDate
import java.util.*

class PersonaliaService(
    private val hentPersonOgGeografiskTilknytningQuery: HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery,
    private val norg2Client: Norg2Client,
    private val amtDeltakerClient: AmtDeltakerClient,
    private val navEnhetService: NavEnhetService,
) {
    suspend fun getPersonalia(deltakerIds: List<UUID>): Map<UUID, DeltakerPersonalia> {
        return amtDeltakerClient.hentPersonalia(deltakerIds)
            .map { amtList ->
                val pdlData = getPersonerMedGeografiskEnhet(amtList.map { NorskIdent(it.personident) })
                amtList.map { amtPersonalia ->
                    val norskIdent = NorskIdent(amtPersonalia.personident)
                    val (pdlPerson, geografiskEnhet) = pdlData[norskIdent] ?: (null to null)

                    if (pdlPerson?.gradering != PdlGradering.UGRADERT) {
                        DeltakerPersonalia(
                            deltakerId = amtPersonalia.deltakerId,
                            norskIdent = norskIdent,
                            navn = "Adressebeskyttet",
                            oppfolgingEnhet = null,
                            geografiskEnhet = null,
                            region = null,
                            foedselsdato = null,
                        )
                    } else if (amtPersonalia.erSkjermet) {
                        DeltakerPersonalia(
                            deltakerId = amtPersonalia.deltakerId,
                            norskIdent = norskIdent,
                            navn = "Skjermet",
                            oppfolgingEnhet = null,
                            geografiskEnhet = null,
                            region = null,
                            foedselsdato = null,
                        )
                    } else {
                        val geografiskEnhetDto = geografiskEnhet?.navEnhetNummer()?.let {
                            navEnhetService.hentEnhet(it)
                        }
                        DeltakerPersonalia(
                            deltakerId = amtPersonalia.deltakerId,
                            norskIdent = norskIdent,
                            navn = pdlPerson.navn ?: "Ukjent navn",
                            oppfolgingEnhet = amtPersonalia.navEnhetsnummer?.let { enhet ->
                                navEnhetService.hentEnhet(NavEnhetNummer(enhet))
                            },
                            geografiskEnhet = geografiskEnhetDto,
                            region = geografiskEnhetDto?.overordnetEnhet?.let {
                                navEnhetService.hentEnhet(it)
                            },
                            foedselsdato = pdlPerson.foedselsdato,
                        )
                    }
                }
            }
            .getOrElse {
                throw StatusException(
                    status = HttpStatusCode.InternalServerError,
                    detail = "Klarte ikke hente personalia fra amt-deltaker error: $it",
                )
            }
            .associateBy { it.deltakerId }
    }

    private suspend fun getPersonerMedGeografiskEnhet(identer: List<NorskIdent>): Map<NorskIdent, Pair<PdlPerson, GeografiskTilknytning?>> {
        val pdlIdenter = identer
            .map { ident -> PdlIdent(ident.value) }
            .toNonEmptySetOrNull()
            ?: return emptyMap()

        val pdlPersonData = hentPersonOgGeografiskTilknytningQuery.hentPersonOgGeografiskTilknytningBolk(pdlIdenter, AccessType.M2M)
            .getOrElse {
                throw StatusException(
                    status = HttpStatusCode.InternalServerError,
                    detail = "Klarte ikke hente informasjon om personer og geografisk tilknytning",
                )
            }

        return pdlPersonData.mapKeys { (pdlIdent, _) -> NorskIdent(pdlIdent.value) }
    }

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

data class DeltakerPersonalia(
    val deltakerId: UUID,
    val norskIdent: NorskIdent,
    val navn: String,
    val oppfolgingEnhet: NavEnhetDto?,
    val geografiskEnhet: NavEnhetDto?,
    val region: NavEnhetDto?,
    val foedselsdato: LocalDate?,
)
