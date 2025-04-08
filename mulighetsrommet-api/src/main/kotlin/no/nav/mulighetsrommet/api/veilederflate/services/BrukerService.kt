package no.nav.mulighetsrommet.api.veilederflate.services

import arrow.core.getOrElse
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.isoppfolgingstilfelle.IsoppfolgingstilfelleClient
import no.nav.mulighetsrommet.api.clients.isoppfolgingstilfelle.OppfolgingstilfelleError
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.clients.norg2.NorgError
import no.nav.mulighetsrommet.api.clients.oppfolging.ErUnderOppfolgingError
import no.nav.mulighetsrommet.api.clients.oppfolging.ManuellStatusDto
import no.nav.mulighetsrommet.api.clients.oppfolging.OppfolgingError
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClient
import no.nav.mulighetsrommet.api.clients.pdl.GeografiskTilknytningResponse
import no.nav.mulighetsrommet.api.clients.pdl.PdlError
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.clients.vedtak.InnsatsgruppeV2
import no.nav.mulighetsrommet.api.clients.vedtak.VedtakError
import no.nav.mulighetsrommet.api.clients.vedtak.VeilarbvedtaksstotteClient
import no.nav.mulighetsrommet.api.navenhet.NAV_EGNE_ANSATTE_TIL_FYLKE_MAP
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.veilederflate.pdl.HentBrukerPdlQuery
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.Innsatsgruppe
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType

class BrukerService(
    private val veilarboppfolgingClient: VeilarboppfolgingClient,
    private val veilarbvedtaksstotteClient: VeilarbvedtaksstotteClient,
    private val navEnhetService: NavEnhetService,
    private val norg2Client: Norg2Client,
    private val isoppfolgingstilfelleClient: IsoppfolgingstilfelleClient,
    private val brukerPdlQuery: HentBrukerPdlQuery,
) {
    suspend fun hentBrukerdata(fnr: NorskIdent, obo: AccessType.OBO): Brukerdata = coroutineScope {
        val deferredErUnderOppfolging = async { veilarboppfolgingClient.erBrukerUnderOppfolging(fnr, obo) }
        val deferredOppfolgingsenhet = async { veilarboppfolgingClient.hentOppfolgingsenhet(fnr, obo) }
        val deferredManuellStatus = async { veilarboppfolgingClient.hentManuellStatus(fnr, obo) }
        val deferredGjeldendeVedtak = async { veilarbvedtaksstotteClient.hentGjeldende14aVedtak(fnr, obo) }
        val deferredBruker = async { brukerPdlQuery.hentBruker(PdlIdent(fnr.value), obo) }
        val deferredErSykmeldtMedArbeidsgiver = async { isoppfolgingstilfelleClient.erSykmeldtMedArbeidsgiver(fnr) }

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

        val bruker = deferredBruker.await()
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

        val deferredBrukersGeografiskeEnhet = async { hentBrukersGeografiskeEnhet(bruker.geografiskTilknytningResponse) }

        val gjeldendeVedtak = deferredGjeldendeVedtak.await()
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

        val erSykmeldtMedArbeidsgiver = deferredErSykmeldtMedArbeidsgiver.await()
            .getOrElse {
                when (it) {
                    OppfolgingstilfelleError.Forbidden ->
                        throw StatusException(
                            HttpStatusCode.InternalServerError,
                            "Manglet tilgang til å hente oppfølgingstilfeller.",
                        )

                    OppfolgingstilfelleError.Error ->
                        throw StatusException(
                            HttpStatusCode.InternalServerError,
                            "Klarte ikke hente oppfølgingstilfeller.",
                        )
                }
            }

        val brukersGeografiskeEnhet = deferredBrukersGeografiskeEnhet.await()
        val brukersOppfolgingsenhet = oppfolgingsenhet?.enhetId?.let { navEnhetService.hentEnhet(it) }
        val enheter = getRelevanteEnheterForBruker(brukersGeografiskeEnhet, brukersOppfolgingsenhet)

        Brukerdata(
            fnr = fnr,
            innsatsgruppe = gjeldendeVedtak?.innsatsgruppe?.let { toInnsatsgruppe(it) },
            enheter = enheter,
            fornavn = bruker.fornavn,
            manuellStatus = manuellStatus,
            erUnderOppfolging = erUnderOppfolging,
            erSykmeldtMedArbeidsgiver = erSykmeldtMedArbeidsgiver,
            varsler = buildList {
                if (oppfolgingsenhetLokalOgUlik(brukersGeografiskeEnhet, brukersOppfolgingsenhet)) {
                    add(BrukerVarsel.LOKAL_OPPFOLGINGSENHET)
                }

                if (!erUnderOppfolging && gjeldendeVedtak?.innsatsgruppe != null) {
                    add(BrukerVarsel.BRUKER_IKKE_UNDER_OPPFOLGING)
                } else if (!erUnderOppfolging) {
                    add(BrukerVarsel.BRUKER_IKKE_UNDER_OPPFOLGING)
                } else if (gjeldendeVedtak?.innsatsgruppe == null) {
                    add(BrukerVarsel.BRUKER_UNDER_OPPFOLGING_MEN_MANGLER_14A_VEDTAK)
                }
            },
        )
    }

    private suspend fun hentBrukersGeografiskeEnhet(geografiskTilknytningResponse: GeografiskTilknytningResponse): NavEnhetDbo? {
        val norgResult = when (geografiskTilknytningResponse) {
            is GeografiskTilknytningResponse.GtBydel -> norg2Client.hentEnhetByGeografiskOmraade(geografiskTilknytningResponse.value)
            is GeografiskTilknytningResponse.GtKommune -> norg2Client.hentEnhetByGeografiskOmraade(geografiskTilknytningResponse.value)
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
        val erSykmeldtMedArbeidsgiver: Boolean,
        val varsler: List<BrukerVarsel>,
    )

    enum class BrukerVarsel {
        LOKAL_OPPFOLGINGSENHET,
        BRUKER_IKKE_UNDER_OPPFOLGING,
        BRUKER_UNDER_OPPFOLGING_MEN_MANGLER_14A_VEDTAK,
    }
}

private fun toInnsatsgruppe(innsatsgruppe: InnsatsgruppeV2): Innsatsgruppe {
    return when (innsatsgruppe) {
        InnsatsgruppeV2.GODE_MULIGHETER -> Innsatsgruppe.GODE_MULIGHETER
        InnsatsgruppeV2.TRENGER_VEILEDNING -> Innsatsgruppe.TRENGER_VEILEDNING
        InnsatsgruppeV2.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE -> Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE
        InnsatsgruppeV2.JOBBE_DELVIS -> Innsatsgruppe.JOBBE_DELVIS
        InnsatsgruppeV2.LITEN_MULIGHET_TIL_A_JOBBE -> Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE
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
        NAV_EGNE_ANSATTE_TIL_FYLKE_MAP.keys.contains(oppfolgingsenhet.enhetsnummer.value)
    ) {
        oppfolgingsenhet
    } else {
        null
    }
    return listOfNotNull(actualGeografiskEnhet, virtuellOppfolgingsenhet)
}
