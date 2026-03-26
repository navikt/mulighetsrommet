package no.nav.mulighetsrommet.api.utbetaling.service

import arrow.core.getOrElse
import arrow.core.toNonEmptySetOrNull
import io.ktor.http.HttpStatusCode
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerClient
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.norg2.NorgError
import no.nav.mulighetsrommet.api.clients.pdl.GeografiskTilknytning
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.clients.tilgangsmaskin.TilgangsmaskinClient
import no.nav.mulighetsrommet.api.navenhet.NavEnhetDto
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.utbetaling.pdl.HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery
import no.nav.mulighetsrommet.api.utbetaling.pdl.PdlPerson
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType
import java.util.UUID

class PersonaliaService(
    private val hentPersonOgGeografiskTilknytningQuery: HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery,
    private val norg2Client: Norg2Client,
    private val amtDeltakerClient: AmtDeltakerClient,
    private val navEnhetService: NavEnhetService,
    private val tilgansmaskinClient: TilgangsmaskinClient,
) {
    suspend fun getPersonaliaMedGeografiskEnhet(
        deltakerIds: List<UUID>,
        obo: AccessType.OBO,
    ): Map<UUID, PersonaliaMedGeografiskEnhet> {
        return amtDeltakerClient.hentPersonalia(deltakerIds)
            .map { amtList ->
                val pdlData = getPersonerMedGeografiskEnhet(amtList.map { it.norskIdent })
                amtList.associate { amtPersonalia ->
                    val norskIdent = amtPersonalia.norskIdent
                    val access = tilgansmaskinClient.komplett(norskIdent, obo)

                    if (access) {
                        val (_, geografiskEnhet) = pdlData[norskIdent] ?: (null to null)

                        val geografiskEnhetDto = geografiskEnhet?.navEnhetNummer()?.let {
                            navEnhetService.hentEnhet(it)
                        }

                        val oppfolgingEnhet = amtPersonalia.oppfolgingEnhet?.let {
                            navEnhetService.hentEnhet(it)
                        }

                        amtPersonalia.deltakerId to
                            PersonaliaMedGeografiskEnhet(
                                norskIdent = norskIdent,
                                navn = amtPersonalia.navn,
                                oppfolgingEnhet = oppfolgingEnhet,
                                geografiskEnhet = geografiskEnhetDto,
                                region = oppfolgingEnhet?.overordnetEnhet?.let {
                                    navEnhetService.hentEnhet(it)
                                },
                            )
                    } else {
                        val skjermetNavn = when {
                            amtPersonalia.adressebeskyttelse != PdlGradering.UGRADERT -> "Adressebeskyttet"
                            amtPersonalia.erSkjermet -> "Skjermet"
                            else -> "Skjermet"
                        }

                        amtPersonalia.deltakerId to
                            PersonaliaMedGeografiskEnhet(
                                norskIdent = null,
                                navn = skjermetNavn,
                                oppfolgingEnhet = null,
                                geografiskEnhet = null,
                                region = null,
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

data class PersonaliaMedGeografiskEnhet(
    val norskIdent: NorskIdent?,
    val navn: String,
    val oppfolgingEnhet: NavEnhetDto?,
    val geografiskEnhet: NavEnhetDto?,
    val region: NavEnhetDto?,
)
