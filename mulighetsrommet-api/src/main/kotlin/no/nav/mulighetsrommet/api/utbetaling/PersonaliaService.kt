package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.getOrElse
import arrow.core.toNonEmptySetOrNull
import io.ktor.http.HttpStatusCode
import java.util.*
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerClient
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.norg2.NorgError
import no.nav.mulighetsrommet.api.clients.pdl.GeografiskTilknytning
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.navenhet.NavEnhetDto
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.utbetaling.pdl.HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery
import no.nav.mulighetsrommet.api.utbetaling.pdl.PdlPerson
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType

class PersonaliaService(
    private val hentPersonOgGeografiskTilknytningQuery: HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery,
    private val norg2Client: Norg2Client,
    private val amtDeltakerClient: AmtDeltakerClient,
    private val navEnhetService: NavEnhetService,
) {
    suspend fun getPersonaliaMedGeografiskEnhet(deltakerIds: Set<UUID>): Set<DeltakerPersonaliaMedGeografiskEnhet> {
        return amtDeltakerClient.hentPersonalia(deltakerIds)
            .map { amtList ->
                val pdlData = getPersonerMedGeografiskEnhet(amtList.map { it.norskIdent })
                amtList.map { amtPersonalia ->
                    val norskIdent = amtPersonalia.norskIdent
                    val (_, geografiskEnhet) = pdlData[norskIdent] ?: (null to null)

                    val geografiskEnhetDto = geografiskEnhet?.navEnhetNummer()?.let {
                        navEnhetService.hentEnhet(it)
                    }

                    val oppfolgingEnhet = amtPersonalia.oppfolgingEnhet?.let {
                        navEnhetService.hentEnhet(it)
                    }

                    if (amtPersonalia.erSkjermet || amtPersonalia.adressebeskyttelse != PdlGradering.UGRADERT) {
                        val skjermetNavn = when {
                            amtPersonalia.adressebeskyttelse != PdlGradering.UGRADERT -> "Adressebeskyttet"
                            else -> "Skjermet"
                        }
                        DeltakerPersonaliaMedGeografiskEnhet(
                            deltakerId = amtPersonalia.deltakerId,
                            norskIdent = null,
                            navn = skjermetNavn,
                            oppfolgingEnhet = null,
                            geografiskEnhet = null,
                            region = null,
                        )
                    } else {
                        DeltakerPersonaliaMedGeografiskEnhet(
                            deltakerId = amtPersonalia.deltakerId,
                            norskIdent = norskIdent,
                            navn = amtPersonalia.navn,
                            oppfolgingEnhet = oppfolgingEnhet,
                            geografiskEnhet = geografiskEnhetDto,
                            region = oppfolgingEnhet?.overordnetEnhet?.let {
                                navEnhetService.hentEnhet(it)
                            },
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
            .toSet()
    }

    private suspend fun getPersonerMedGeografiskEnhet(identer: List<NorskIdent>): Map<NorskIdent, Pair<PdlPerson, GeografiskTilknytning?>> {
        val pdlIdenter = identer
            .map { ident -> PdlIdent(ident.value) }
            .toNonEmptySetOrNull()
            ?: return emptyMap()

        val pdlPersonData = hentPersonOgGeografiskTilknytningQuery
            .hentPersonOgGeografiskTilknytningBolk(pdlIdenter, AccessType.M2M)
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

data class DeltakerPersonaliaMedGeografiskEnhet(
    val deltakerId: UUID,
    val norskIdent: NorskIdent?,
    val navn: String,
    val oppfolgingEnhet: NavEnhetDto?,
    val geografiskEnhet: NavEnhetDto?,
    val region: NavEnhetDto?,
)
