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
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType
import java.util.UUID

class PersonaliaService(
    private val hentPersonOgGeografiskTilknytningQuery: HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery,
    private val norg2Client: Norg2Client,
    private val amtDeltakerClient: AmtDeltakerClient,
    private val tilgangsmaskinClient: TilgangsmaskinClient,
    private val navEnhetService: NavEnhetService,
) {
    suspend fun getPersonalia(
        deltakerIds: List<UUID>,
        accessType: AccessType,
    ): Map<UUID, Personalia> {
        val amtPersonalia = amtDeltakerClient.hentPersonalia(deltakerIds)
            .getOrElse {
                throw StatusException(
                    status = HttpStatusCode.InternalServerError,
                    detail = "Klarte ikke hente personalia fra amt-deltaker error: $it",
                )
            }

        val tilgangerByDeltakerId: Map<UUID, Boolean> = when (accessType) {
            is AccessType.OBO.AzureAd -> {
                when (NaisEnv.current()) {
                    NaisEnv.Local,
                    NaisEnv.DevGCP,
                    -> {
                        val identer = amtPersonalia.map { it.norskIdent }
                        val tilgangsmaskinResponse = tilgangsmaskinClient.bulk(identer, accessType)
                        amtPersonalia.associate { p ->
                            val harTilgang = requireNotNull(tilgangsmaskinResponse.resultater.find { it.brukerId == p.norskIdent.value }) {
                                "Fant ikke deltakerId: ${p.deltakerId} i respons fra tilgangsmaskin"
                            }
                                .harTilgang()
                            p.deltakerId to harTilgang
                        }
                    }

                    NaisEnv.ProdGCP -> amtPersonalia.associate {
                        it.deltakerId to (it.adressebeskyttelse == PdlGradering.UGRADERT && !it.erSkjermet)
                    }
                }
            }

            is AccessType.OBO.TokenX -> amtPersonalia.associate {
                it.deltakerId to (it.adressebeskyttelse == PdlGradering.UGRADERT && !it.erSkjermet)
            }

            is AccessType.M2M -> amtPersonalia.associate {
                it.deltakerId to true
            }
        }
        return amtPersonalia.associate { p ->
            if (requireNotNull(tilgangerByDeltakerId[p.deltakerId])) {
                p.deltakerId to
                    Personalia(
                        norskIdent = p.norskIdent,
                        navn = p.navn,
                        oppfolgingEnhet = p.oppfolgingEnhet?.let { navEnhetService.hentEnhet(it) },
                        erSkjermet = p.erSkjermet,
                        adressebeskyttelse = p.adressebeskyttelse,
                    )
            } else {
                val navn = when {
                    p.adressebeskyttelse != PdlGradering.UGRADERT -> "Adressebeskyttet"
                    else -> "Skjermet"
                }
                p.deltakerId to
                    Personalia(
                        norskIdent = null,
                        navn = navn,
                        oppfolgingEnhet = null,
                        erSkjermet = p.erSkjermet,
                        adressebeskyttelse = p.adressebeskyttelse,
                    )
            }
        }
    }

    suspend fun getPersonaliaMedGeografiskEnhet(
        deltakerIds: List<UUID>,
        accessType: AccessType,
    ): Map<UUID, PersonaliaMedGeografiskEnhet> {
        val personalia = getPersonalia(deltakerIds, accessType)
        val pdlData = getPersonerMedGeografiskEnhet(
            personalia
                .map { it.value }
                .filter { it.norskIdent != null }
                .map { requireNotNull(it.norskIdent) },
        )

        return personalia.mapValues { (_, p) ->
            val norskIdent = p.norskIdent

            val (_, geografiskEnhet) = norskIdent?.let { pdlData[norskIdent] } ?: (null to null)

            PersonaliaMedGeografiskEnhet(
                norskIdent = norskIdent,
                navn = p.navn,
                erSkjermet = p.erSkjermet,
                adressebeskyttelse = p.adressebeskyttelse,
                oppfolgingEnhet = p.oppfolgingEnhet,
                geografiskEnhet = geografiskEnhet?.navEnhetNummer()?.let { navEnhetService.hentEnhet(it) },
                region = p.oppfolgingEnhet?.overordnetEnhet?.let {
                    navEnhetService.hentEnhet(it)
                },
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

data class Personalia(
    val norskIdent: NorskIdent?,
    val navn: String,
    val erSkjermet: Boolean,
    val adressebeskyttelse: PdlGradering,
    val oppfolgingEnhet: NavEnhetDto?,
)

data class PersonaliaMedGeografiskEnhet(
    val norskIdent: NorskIdent?,
    val navn: String,
    val erSkjermet: Boolean,
    val adressebeskyttelse: PdlGradering,
    val oppfolgingEnhet: NavEnhetDto?,
    val geografiskEnhet: NavEnhetDto?,
    val region: NavEnhetDto?,
)
