package no.nav.mulighetsrommet.api.services

import arrow.core.getOrElse
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.AccessType
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.clients.norg2.NorgError
import no.nav.mulighetsrommet.api.clients.oppfolging.ErUnderOppfolgingError
import no.nav.mulighetsrommet.api.clients.oppfolging.ManuellStatusDto
import no.nav.mulighetsrommet.api.clients.oppfolging.OppfolgingError
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClient
import no.nav.mulighetsrommet.api.clients.pdl.GeografiskTilknytning
import no.nav.mulighetsrommet.api.clients.pdl.PdlClient
import no.nav.mulighetsrommet.api.clients.pdl.PdlError
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.clients.vedtak.VedtakDto
import no.nav.mulighetsrommet.api.clients.vedtak.VedtakError
import no.nav.mulighetsrommet.api.clients.vedtak.VeilarbvedtaksstotteClient
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.domain.dto.Innsatsgruppe
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.ktor.exception.StatusException

class BrukerService(
    private val veilarboppfolgingClient: VeilarboppfolgingClient,
    private val veilarbvedtaksstotteClient: VeilarbvedtaksstotteClient,
    private val navEnhetService: NavEnhetService,
    private val pdlClient: PdlClient,
    private val norg2Client: Norg2Client,
) {
    suspend fun hentBrukerdata(fnr: NorskIdent, obo: AccessType.OBO): Brukerdata = coroutineScope {
        val deferredErUnderOppfolging = async { veilarboppfolgingClient.erBrukerUnderOppfolging(fnr, obo) }
        val deferredOppfolgingsenhet = async { veilarboppfolgingClient.hentOppfolgingsenhet(fnr, obo) }
        val deferredManuellStatus = async { veilarboppfolgingClient.hentManuellStatus(fnr, obo) }
        val deferredSisteVedtak = async { veilarbvedtaksstotteClient.hentSiste14AVedtak(fnr, obo) }
        val deferredPdlPerson = async { pdlClient.hentPerson(PdlIdent(fnr.value), obo) }
        val deferredBrukersGeografiskeEnhet = async { hentBrukersGeografiskeEnhet(fnr, obo) }

        val erUnderOppfolging = deferredErUnderOppfolging.await()
            .getOrElse {
                when (it) {
                    ErUnderOppfolgingError.Forbidden -> throw StatusException(
                        HttpStatusCode.Forbidden,
                        "Manglet tilgang til å hente oppfølgingsstatus.",
                    )

                    ErUnderOppfolgingError.Error -> throw StatusException(
                        HttpStatusCode.InternalServerError,
                        "Klarte ikke hente oppfølgingsstatus.",
                    )
                }
            }

        val oppfolgingsenhet = deferredOppfolgingsenhet.await()
            .getOrElse {
                when (it) {
                    OppfolgingError.Forbidden -> throw StatusException(
                        HttpStatusCode.Forbidden,
                        "Manglet tilgang til å hente oppfølgingsenhet.",
                    )

                    OppfolgingError.Error -> throw StatusException(
                        HttpStatusCode.InternalServerError,
                        "Klarte ikke hente oppfølgingsenhet.",
                    )

                    OppfolgingError.NotFound -> null
                }
            }

        val manuellStatus = deferredManuellStatus.await()
            .getOrElse {
                when (it) {
                    OppfolgingError.Forbidden -> throw StatusException(
                        HttpStatusCode.Forbidden,
                        "Manglet tilgang til å hente hente manuell status.",
                    )

                    OppfolgingError.Error -> throw StatusException(
                        HttpStatusCode.InternalServerError,
                        "Klarte ikke hente hente manuell status.",
                    )

                    OppfolgingError.NotFound -> throw StatusException(
                        HttpStatusCode.InternalServerError,
                        "Fant ikke manuell status.",
                    )
                }
            }

        val pdlPerson = deferredPdlPerson.await()
            .getOrElse {
                when (it) {
                    PdlError.Error -> throw StatusException(
                        HttpStatusCode.InternalServerError,
                        "Feil ved henting av personinfo fra Pdl.",
                    )

                    PdlError.NotFound -> throw StatusException(
                        HttpStatusCode.InternalServerError,
                        "Fant ikke person i Pdl.",
                    )
                }
            }

        val sisteVedtak = deferredSisteVedtak.await()
            .getOrElse {
                when (it) {
                    VedtakError.Forbidden -> throw StatusException(
                        HttpStatusCode.Forbidden,
                        "Mangler tilgang til å hente §14a-vedtak.",
                    )

                    VedtakError.Error -> throw StatusException(
                        HttpStatusCode.InternalServerError,
                        "Klarte ikke hente §14a-vedtak.",
                    )

                    VedtakError.NotFound -> null
                }
            }

        val brukersGeografiskeEnhet = deferredBrukersGeografiskeEnhet.await()

        val brukersOppfolgingsenhet = oppfolgingsenhet?.enhetId?.let {
            navEnhetService.hentEnhet(it)
        }

        val enheter = getRelevanteEnheterForBruker(brukersGeografiskeEnhet, brukersOppfolgingsenhet)

        Brukerdata(
            fnr = fnr,
            innsatsgruppe = sisteVedtak?.innsatsgruppe?.let { toInnsatsgruppe(it) },
            enheter = enheter,
            fornavn = pdlPerson.navn.firstOrNull()?.fornavn,
            manuellStatus = manuellStatus,
            erUnderOppfolging = erUnderOppfolging,
            varsler = buildList {
                if (oppfolgingsenhetLokalOgUlik(brukersGeografiskeEnhet, brukersOppfolgingsenhet)) {
                    add(BrukerVarsel.LOKAL_OPPFOLGINGSENHET)
                }

                if (!erUnderOppfolging && sisteVedtak?.innsatsgruppe != null) {
                    add(BrukerVarsel.BRUKER_IKKE_UNDER_OPPFOLGING)
                } else if (!erUnderOppfolging) {
                    add(BrukerVarsel.BRUKER_IKKE_UNDER_OPPFOLGING)
                } else if (sisteVedtak?.innsatsgruppe == null) {
                    add(BrukerVarsel.BRUKER_UNDER_OPPFOLGING_MEN_MANGLER_14A_VEDTAK)
                }
            },
        )
    }

    private suspend fun hentBrukersGeografiskeEnhet(fnr: NorskIdent, obo: AccessType.OBO): NavEnhetDbo? {
        val geografiskTilknytning = pdlClient.hentGeografiskTilknytning(PdlIdent(fnr.value), obo)
            .getOrElse {
                when (it) {
                    PdlError.Error -> {
                        throw StatusException(
                            HttpStatusCode.InternalServerError,
                            "Klarte ikke hente geografisk tilknytning fra Pdl.",
                        )
                    }

                    PdlError.NotFound -> null
                }
            }

        val norgResult = when (geografiskTilknytning) {
            is GeografiskTilknytning.GtBydel -> norg2Client.hentEnhetByGeografiskOmraade(geografiskTilknytning.value)
            is GeografiskTilknytning.GtKommune -> norg2Client.hentEnhetByGeografiskOmraade(geografiskTilknytning.value)
            else -> return null
        }

        return norgResult
            .map { navEnhetService.hentEnhet(it.enhetNr) }
            .getOrElse {
                when (it) {
                    NorgError.NotFound -> null
                    NorgError.Error -> throw StatusException(
                        HttpStatusCode.InternalServerError,
                        "Fant ikke nav enhet til geografisk tilknytning.",
                    )
                }
            }
    }

    @Serializable
    data class Brukerdata(
        val fnr: NorskIdent,
        val innsatsgruppe: Innsatsgruppe?,
        val enheter: List<NavEnhetDbo>,
        val fornavn: String?,
        val manuellStatus: ManuellStatusDto,
        val erUnderOppfolging: Boolean,
        val varsler: List<BrukerVarsel>,
    )

    enum class BrukerVarsel {
        LOKAL_OPPFOLGINGSENHET,
        BRUKER_IKKE_UNDER_OPPFOLGING,
        BRUKER_UNDER_OPPFOLGING_MEN_MANGLER_14A_VEDTAK,
    }
}

private fun toInnsatsgruppe(innsatsgruppe: VedtakDto.Innsatsgruppe): Innsatsgruppe {
    return when (innsatsgruppe) {
        VedtakDto.Innsatsgruppe.STANDARD_INNSATS -> Innsatsgruppe.STANDARD_INNSATS
        VedtakDto.Innsatsgruppe.SITUASJONSBESTEMT_INNSATS -> Innsatsgruppe.SITUASJONSBESTEMT_INNSATS
        VedtakDto.Innsatsgruppe.SPESIELT_TILPASSET_INNSATS -> Innsatsgruppe.SPESIELT_TILPASSET_INNSATS
        // TODO: benytt verdi for GRADERT_VARIG_TILPASSET_INNSATS når ny 14a-løsning er lansert nasjonalt
        VedtakDto.Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS, VedtakDto.Innsatsgruppe.VARIG_TILPASSET_INNSATS -> Innsatsgruppe.VARIG_TILPASSET_INNSATS
    }
}

fun oppfolgingsenhetLokalOgUlik(
    geografiskEnhet: NavEnhetDbo?,
    oppfolgingsenhet: NavEnhetDbo?,
): Boolean {
    return oppfolgingsenhet?.type == Norg2Type.LOKAL && oppfolgingsenhet.enhetsnummer != geografiskEnhet?.enhetsnummer
}

fun getRelevanteEnheterForBruker(
    geografiskEnhet: NavEnhetDbo?,
    oppfolgingsenhet: NavEnhetDbo?,
): List<NavEnhetDbo> {
    val actualGeografiskEnhet = if (oppfolgingsenhet?.type == Norg2Type.LOKAL) {
        oppfolgingsenhet
    } else {
        geografiskEnhet
    }

    val virtuellOppfolgingsenhet = if (
        oppfolgingsenhet != null &&
        NAV_EGNE_ANSATTE_TIL_FYLKE_MAP.keys.contains(oppfolgingsenhet.enhetsnummer)
    ) {
        oppfolgingsenhet
    } else {
        null
    }
    return listOfNotNull(actualGeografiskEnhet, virtuellOppfolgingsenhet)
}
