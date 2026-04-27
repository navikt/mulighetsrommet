package no.nav.mulighetsrommet.api.utbetaling.service

import arrow.core.getOrElse
import arrow.core.toNonEmptySetOrNull
import io.ktor.http.HttpStatusCode
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerClient
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerPersonalia
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.norg2.NorgError
import no.nav.mulighetsrommet.api.clients.pdl.GeografiskTilknytning
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.clients.tilgangsmaskin.TilgangsmaskinClient
import no.nav.mulighetsrommet.api.clients.tilgangsmaskin.TilgangsmaskinResult
import no.nav.mulighetsrommet.api.navenhet.NavEnhetDto
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.utbetaling.pdl.HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery
import no.nav.mulighetsrommet.api.utbetaling.pdl.PdlPerson
import no.nav.mulighetsrommet.api.utbetaling.service.AvvistGrunn.AVVIST_FORTROLIG_ADRESSE
import no.nav.mulighetsrommet.api.utbetaling.service.AvvistGrunn.AVVIST_SKJERMING
import no.nav.mulighetsrommet.api.utbetaling.service.AvvistGrunn.AVVIST_STRENGT_FORTROLIG_ADRESSE
import no.nav.mulighetsrommet.api.utbetaling.service.AvvistGrunn.AVVIST_STRENGT_FORTROLIG_UTLAND
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
        val pdlData = getPersonerMedGeografiskEnhet(amtPersonalia.map { it.norskIdent })

        val tilgangerByDeltakerId = sjekkTilgangTilPerson(amtPersonalia, accessType)

        return amtPersonalia.associate { p ->
            when (val avvistGrunn = tilgangerByDeltakerId[p.deltakerId]) {
                null -> {
                    val norskIdent = p.norskIdent

                    val (_, geografiskEnhet) = norskIdent.let { pdlData[norskIdent] } ?: (null to null)
                    val oppfolgingEnhet = p.oppfolgingEnhet?.let { navEnhetService.hentEnhet(it) }

                    p.deltakerId to
                        Personalia.MedTilgang(
                            norskIdent = p.norskIdent,
                            navn = p.navn,
                            oppfolgingEnhet = oppfolgingEnhet,
                            erSkjermet = p.erSkjermet,
                            adressebeskyttelse = p.adressebeskyttelse,
                            geografiskEnhet = geografiskEnhet?.navEnhetNummer()?.let { navEnhetService.hentEnhet(it) },
                            region = oppfolgingEnhet?.overordnetEnhet?.let {
                                navEnhetService.hentEnhet(it)
                            },
                        )
                }

                else -> p.deltakerId to Personalia.Avvist(avvistGrunn)
            }
        }
    }

    suspend fun sjekkTilgangTilPerson(amtPersonalia: Set<AmtDeltakerPersonalia>, accessType: AccessType): Map<UUID, AvvistGrunn?> {
        return when (accessType) {
            is AccessType.OBO.AzureAd -> {
                when (NaisEnv.current()) {
                    NaisEnv.Local,
                    NaisEnv.DevGCP,
                    -> {
                        val identer = amtPersonalia.map { it.norskIdent }
                        val tilgangsmaskinResult = tilgangsmaskinClient.bulk(identer, accessType)

                        amtPersonalia.associate { p ->
                            val resultat = requireNotNull(tilgangsmaskinResult.resultater.find { it.brukerId == p.norskIdent.value }) {
                                "Fant ikke deltaker i respons fra tilgangsmaskin"
                            }

                            p.deltakerId to AvvistGrunn.fromTilgangsmaskinResultat(resultat)
                        }
                    }

                    NaisEnv.ProdGCP -> amtPersonalia.associate {
                        val grunn = when (it.adressebeskyttelse) {
                            PdlGradering.FORTROLIG -> AVVIST_FORTROLIG_ADRESSE

                            PdlGradering.STRENGT_FORTROLIG -> AVVIST_STRENGT_FORTROLIG_ADRESSE

                            PdlGradering.STRENGT_FORTROLIG_UTLAND -> AVVIST_STRENGT_FORTROLIG_UTLAND

                            PdlGradering.UGRADERT -> when (it.erSkjermet) {
                                true -> AVVIST_SKJERMING
                                false -> null
                            }
                        }
                        it.deltakerId to grunn
                    }
                }
            }

            is AccessType.OBO.TokenX -> amtPersonalia.associate {
                val grunn = when (it.adressebeskyttelse) {
                    PdlGradering.FORTROLIG -> AVVIST_FORTROLIG_ADRESSE
                    PdlGradering.STRENGT_FORTROLIG -> AVVIST_STRENGT_FORTROLIG_ADRESSE
                    PdlGradering.STRENGT_FORTROLIG_UTLAND -> AVVIST_STRENGT_FORTROLIG_UTLAND
                    PdlGradering.UGRADERT -> null
                }
                it.deltakerId to grunn
            }

            is AccessType.M2M -> amtPersonalia.associate {
                it.deltakerId to null
            }
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

sealed class Personalia {
    abstract val norskIdent: NorskIdent?
    abstract val navn: String?
    abstract val oppfolgingEnhet: NavEnhetDto?
    abstract val geografiskEnhet: NavEnhetDto?
    abstract val region: NavEnhetDto?

    data class MedTilgang(
        override val norskIdent: NorskIdent,
        override val navn: String,
        val erSkjermet: Boolean,
        val adressebeskyttelse: PdlGradering,
        override val oppfolgingEnhet: NavEnhetDto?,
        override val geografiskEnhet: NavEnhetDto?,
        override val region: NavEnhetDto?,
    ) : Personalia()

    data class Avvist(val grunn: AvvistGrunn) : Personalia() {
        override val norskIdent = null
        override val navn = null
        override val oppfolgingEnhet = null
        override val geografiskEnhet = null
        override val region = null
    }
}

enum class AvvistGrunn {
    AVVIST_STRENGT_FORTROLIG_ADRESSE,
    AVVIST_STRENGT_FORTROLIG_UTLAND,
    AVVIST_FORTROLIG_ADRESSE,
    AVVIST_SKJERMING,
    AVVIST_HABILITET,
    AVVIST_VERGE,
    AVVIST_GEOGRAFISK,
    ;

    companion object {
        fun fromTilgangsmaskinResultat(resultat: TilgangsmaskinResult.Resultat): AvvistGrunn? {
            return when (resultat) {
                is TilgangsmaskinResult.Resultat.Innvilget -> null
                is TilgangsmaskinResult.Resultat.Avvist -> AvvistGrunn.valueOf(resultat.grunn.name)
            }
        }
    }
}
