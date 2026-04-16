package no.nav.mulighetsrommet.api.utbetaling.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
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
    ): Map<UUID, Either<AvvistGrunn, Personalia>> {
        val amtPersonalia = amtDeltakerClient.hentPersonalia(deltakerIds)
            .getOrElse {
                throw StatusException(
                    status = HttpStatusCode.InternalServerError,
                    detail = "Klarte ikke hente personalia fra amt-deltaker error: $it",
                )
            }

        val tilgangerByDeltakerId = sjekkTilgangTilPerson(amtPersonalia, accessType)

        return amtPersonalia.associate { p ->
            when (val avvistGrunn = tilgangerByDeltakerId[p.deltakerId]) {
                null ->
                    p.deltakerId to
                        Personalia(
                            norskIdent = p.norskIdent,
                            navn = p.navn,
                            oppfolgingEnhet = p.oppfolgingEnhet?.let { navEnhetService.hentEnhet(it) },
                            erSkjermet = p.erSkjermet,
                            adressebeskyttelse = p.adressebeskyttelse,
                        ).right()

                else -> p.deltakerId to avvistGrunn.left()
            }
        }
    }

    suspend fun getPersonaliaMedGeografiskEnhet(
        deltakerIds: List<UUID>,
        accessType: AccessType,
    ): Map<UUID, Either<AvvistGrunn, PersonaliaMedGeografiskEnhet>> {
        val personalia = getPersonalia(deltakerIds, accessType)
        val pdlData = getPersonerMedGeografiskEnhet(
            personalia
                .map { it.value }
                .filter { it.getOrNull()?.norskIdent != null }
                .map { requireNotNull(it.getOrNull()?.norskIdent) },
        )

        return personalia
            .mapValues { (_, e) ->
                e.map { p ->
                    val norskIdent = p.norskIdent

                    val (_, geografiskEnhet) = norskIdent.let { pdlData[norskIdent] } ?: (null to null)

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

data class Personalia(
    val norskIdent: NorskIdent,
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
