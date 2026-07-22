package no.nav.mulighetsrommet.api.utbetaling.service

import arrow.core.getOrElse
import arrow.core.toNonEmptySetOrNull
import io.ktor.http.HttpStatusCode
import no.nav.mulighetsrommet.admin.navenhet.GetNavEnhet
import no.nav.mulighetsrommet.admin.navenhet.NavEnhetDto
import no.nav.mulighetsrommet.admin.navenhet.NavEnhetDtoQuery
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.norg2.NorgError
import no.nav.mulighetsrommet.api.clients.pdl.GeografiskTilknytning
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.clients.tilgangsmaskin.TilgangsmaskinClient
import no.nav.mulighetsrommet.api.clients.tilgangsmaskin.TilgangsmaskinResult
import no.nav.mulighetsrommet.api.deltaker.client.AmtDeltakerClient
import no.nav.mulighetsrommet.api.deltaker.client.AmtDeltakerPersonalia
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
    private val tilgangsmaskinClient: TilgangsmaskinClient,
    private val navEnhetDtoQuery: NavEnhetDtoQuery,
) {
    sealed class OnBehalfOf {
        data object Arrangor : OnBehalfOf()
        data class NavAnsatt(val token: AccessType.OBO.AzureAd) : OnBehalfOf()
        data object System : OnBehalfOf()
    }

    suspend fun getPersonalia(
        deltakerId: UUID,
        onBehalfOf: OnBehalfOf,
    ): Personalia = getPersonalia(listOf(deltakerId), onBehalfOf).find { it.deltakerId == deltakerId }
        ?: throw IllegalArgumentException("Kunne ikke hente personalia fra amt-deltaker med id: $deltakerId")

    suspend fun getPersonalia(
        deltakerIds: List<UUID>,
        onBehalfOf: OnBehalfOf,
    ): List<Personalia> {
        val amtPersonalia = amtDeltakerClient.hentPersonalia(deltakerIds)
            .getOrElse {
                throw StatusException(
                    status = HttpStatusCode.InternalServerError,
                    detail = "Klarte ikke hente personalia fra amt-deltaker error: $it",
                )
            }
        val pdlData = getPersonerMedGeografiskEnhet(amtPersonalia.map { it.norskIdent })

        val tilgangerByDeltakerId = tilgangTilPerson(amtPersonalia, onBehalfOf)

        return amtPersonalia.map { p ->
            val norskIdent = p.norskIdent

            val geografiskEnhet = pdlData[norskIdent]?.second?.navEnhetNummer()?.let {
                navEnhetDtoQuery.execute(GetNavEnhet(it))
            }
            val oppfolgingEnhet = p.oppfolgingEnhet?.let { navEnhetDtoQuery.execute(GetNavEnhet(it)) }
            val region = oppfolgingEnhet?.overordnetEnhet?.let {
                navEnhetDtoQuery.execute(GetNavEnhet(it))
            }
            val avvistGrunn = tilgangerByDeltakerId[p.deltakerId]

            Personalia(
                deltakerId = p.deltakerId,
                norskIdent = p.norskIdent,
                navn = p.navn,
                oppfolgingEnhet = oppfolgingEnhet,
                geografiskEnhet = geografiskEnhet,
                region = region,
                avvistGrunn = avvistGrunn,
                gradering = Gradering.from(p.adressebeskyttelse, avvistGrunn, p.erSkjermet),
            )
        }
    }

    private suspend fun tilgangTilPerson(amtPersonalia: Set<AmtDeltakerPersonalia>, onBehalfOf: OnBehalfOf): Map<UUID, AvvistGrunn?> {
        return when (onBehalfOf) {
            is OnBehalfOf.NavAnsatt -> {
                val identer = amtPersonalia.map { it.norskIdent }
                val tilgangsmaskinResult = tilgangsmaskinClient.bulk(identer, onBehalfOf.token)

                amtPersonalia.associate { p ->
                    val resultat = requireNotNull(tilgangsmaskinResult.resultater.find { it.brukerId == p.norskIdent.value }) {
                        "Fant ikke deltaker i respons fra tilgangsmaskin"
                    }

                    p.deltakerId to AvvistGrunn.fromTilgangsmaskinResultat(resultat)
                }
            }

            is OnBehalfOf.Arrangor -> amtPersonalia.associate {
                val grunn = when (it.adressebeskyttelse) {
                    PdlGradering.FORTROLIG -> AvvistGrunn.AVVIST_FORTROLIG_ADRESSE
                    PdlGradering.STRENGT_FORTROLIG -> AvvistGrunn.AVVIST_STRENGT_FORTROLIG_ADRESSE
                    PdlGradering.STRENGT_FORTROLIG_UTLAND -> AvvistGrunn.AVVIST_STRENGT_FORTROLIG_UTLAND
                    PdlGradering.UGRADERT -> null
                }
                it.deltakerId to grunn
            }

            is OnBehalfOf.System -> amtPersonalia.associate {
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
    val deltakerId: UUID,
    private val norskIdent: NorskIdent,
    private val navn: String,
    private val oppfolgingEnhet: NavEnhetDto?,
    private val geografiskEnhet: NavEnhetDto?,
    private val region: NavEnhetDto?,
    val gradering: Gradering,
    val avvistGrunn: AvvistGrunn?,
) {
    fun harTilgang(): Boolean = avvistGrunn == null

    fun navn(): String = if (harTilgang()) {
        navn
    } else {
        when (gradering) {
            Gradering.STRENGT_FORTROLIG_ADRESSE,
            Gradering.STRENGT_FORTROLIG_UTLAND,
            Gradering.FORTROLIG_ADRESSE,
            -> "Adressebeskyttet"

            Gradering.GEOGRAFISK,
            Gradering.SKJERMING,
            -> "Skjermet"

            Gradering.UGRADERT -> navn
        }
    }

    fun norskIdent(): NorskIdent? = if (harTilgang()) {
        norskIdent
    } else {
        null
    }

    fun oppfolgingEnhet(): NavEnhetDto? = oppfolgingEnhet

    fun geografiskEnhet(): NavEnhetDto? = if (harTilgang()) {
        geografiskEnhet
    } else {
        null
    }

    fun region(): NavEnhetDto? = if (harTilgang()) {
        region
    } else {
        null
    }
}

enum class Gradering {
    STRENGT_FORTROLIG_ADRESSE,
    STRENGT_FORTROLIG_UTLAND,
    FORTROLIG_ADRESSE,
    SKJERMING,
    UGRADERT,
    GEOGRAFISK,
    ;

    companion object {
        fun from(pdlGradering: PdlGradering, avvistGrunn: AvvistGrunn?, erSkjermet: Boolean?): Gradering {
            return when (pdlGradering) {
                PdlGradering.FORTROLIG -> FORTROLIG_ADRESSE

                PdlGradering.STRENGT_FORTROLIG -> STRENGT_FORTROLIG_ADRESSE

                PdlGradering.STRENGT_FORTROLIG_UTLAND -> STRENGT_FORTROLIG_UTLAND

                PdlGradering.UGRADERT -> {
                    if (erSkjermet == true) {
                        SKJERMING
                    } else {
                        when (avvistGrunn) {
                            AvvistGrunn.AVVIST_SKJERMING,
                            AvvistGrunn.AVVIST_HABILITET,
                            AvvistGrunn.AVVIST_VERGE,
                            AvvistGrunn.AVVIST_VERGEMAAL,
                            AvvistGrunn.AVVIST_AVDOED,
                            -> SKJERMING

                            AvvistGrunn.AVVIST_STRENGT_FORTROLIG_ADRESSE,
                            -> STRENGT_FORTROLIG_ADRESSE

                            AvvistGrunn.AVVIST_STRENGT_FORTROLIG_UTLAND,
                            -> STRENGT_FORTROLIG_UTLAND

                            AvvistGrunn.AVVIST_FORTROLIG_ADRESSE,
                            -> FORTROLIG_ADRESSE

                            AvvistGrunn.AVVIST_GEOGRAFISK,
                            AvvistGrunn.AVVIST_PERSON_UTLAND,
                            AvvistGrunn.AVVIST_UKJENT_BOSTED,
                            -> GEOGRAFISK

                            null -> UGRADERT
                        }
                    }
                }
            }
        }
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
    AVVIST_AVDOED,
    AVVIST_VERGEMAAL,
    AVVIST_PERSON_UTLAND,
    AVVIST_UKJENT_BOSTED,
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
