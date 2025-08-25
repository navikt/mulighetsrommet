package no.nav.mulighetsrommet.api.veilederflate.services

import arrow.core.getOrElse
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.mulighetsrommet.api.clients.isoppfolgingstilfelle.IsoppfolgingstilfelleClient
import no.nav.mulighetsrommet.api.clients.isoppfolgingstilfelle.OppfolgingstilfelleError
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.norg2.NorgError
import no.nav.mulighetsrommet.api.clients.oppfolging.ErUnderOppfolgingError
import no.nav.mulighetsrommet.api.clients.oppfolging.OppfolgingError
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClient
import no.nav.mulighetsrommet.api.clients.pdl.GeografiskTilknytning
import no.nav.mulighetsrommet.api.clients.pdl.PdlError
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.clients.vedtak.InnsatsgruppeV2
import no.nav.mulighetsrommet.api.clients.vedtak.VedtakError
import no.nav.mulighetsrommet.api.clients.vedtak.VeilarbvedtaksstotteClient
import no.nav.mulighetsrommet.api.navenhet.NavEnhetDto
import no.nav.mulighetsrommet.api.navenhet.NavEnhetHelpers
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
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

        val deferredBrukersGeografiskeEnhet = async { hentBrukersGeografiskeEnhet(bruker.geografiskTilknytning) }

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
                if (erOppfolgingsenhetEnAnnenGeografiskEnhet(brukersGeografiskeEnhet, brukersOppfolgingsenhet)) {
                    add(BrukerdataVarsel.LOKAL_OPPFOLGINGSENHET)
                }

                if (!erUnderOppfolging) {
                    add(BrukerdataVarsel.BRUKER_IKKE_UNDER_OPPFOLGING)
                } else if (gjeldendeVedtak?.innsatsgruppe == null) {
                    add(BrukerdataVarsel.BRUKER_UNDER_OPPFOLGING_MEN_MANGLER_14A_VEDTAK)
                }
            },
        )
    }

    private suspend fun hentBrukersGeografiskeEnhet(geografiskTilknytning: GeografiskTilknytning): NavEnhetDto? {
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

fun erOppfolgingsenhetEnAnnenGeografiskEnhet(
    geografiskEnhet: NavEnhetDto?,
    oppfolgingsenhet: NavEnhetDto?,
): Boolean {
    return oppfolgingsenhet?.type != null &&
        NavEnhetHelpers.erGeografiskEnhet(oppfolgingsenhet.type) &&
        oppfolgingsenhet.enhetsnummer != geografiskEnhet?.enhetsnummer
}

fun getRelevanteEnheterForBruker(
    geografiskEnhet: NavEnhetDto?,
    oppfolgingsenhet: NavEnhetDto?,
): List<NavEnhetDto> {
    val actualGeografiskEnhet = oppfolgingsenhet
        ?.takeIf { NavEnhetHelpers.erGeografiskEnhet(it.type) }
        ?: geografiskEnhet

    val virtuellOppfolgingsenhet = oppfolgingsenhet
        ?.takeIf { NavEnhetHelpers.erSpesialenhetSomKanVelgesIModia(it.enhetsnummer) }

    return listOfNotNull(actualGeografiskEnhet, virtuellOppfolgingsenhet)
}
