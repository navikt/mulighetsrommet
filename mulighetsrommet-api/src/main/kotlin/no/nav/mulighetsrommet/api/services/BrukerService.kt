package no.nav.mulighetsrommet.api.services

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.clients.oppfolging.ManuellStatusDto
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClient
import no.nav.mulighetsrommet.api.clients.person.VeilarbpersonClient
import no.nav.mulighetsrommet.api.clients.vedtak.Innsatsgruppe
import no.nav.mulighetsrommet.api.clients.vedtak.VeilarbvedtaksstotteClient
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo

class BrukerService(
    private val veilarboppfolgingClient: VeilarboppfolgingClient,
    private val veilarbvedtaksstotteClient: VeilarbvedtaksstotteClient,
    private val veilarbpersonClient: VeilarbpersonClient,
    private val navEnhetService: NavEnhetService,
) {

    suspend fun hentBrukerdata(fnr: String, accessToken: String): Brukerdata {
        val oppfolgingsstatus = veilarboppfolgingClient.hentOppfolgingsstatus(fnr, accessToken)
        val manuellStatus = veilarboppfolgingClient.hentManuellStatus(fnr, accessToken)
        val sisteVedtak = veilarbvedtaksstotteClient.hentSiste14AVedtak(fnr, accessToken)
        val personInfo = veilarbpersonClient.hentPersonInfo(fnr, accessToken)

        val brukersOppfolgingsenhet = oppfolgingsstatus?.oppfolgingsenhet?.enhetId?.let {
            navEnhetService.hentEnhet(it)
        }

        val brukersGeografiskeEnhet = personInfo.geografiskEnhet?.enhetsnummer?.let {
            navEnhetService.hentEnhet(it)
        }

        return Brukerdata(
            fnr = fnr,
            innsatsgruppe = sisteVedtak?.innsatsgruppe,
            enheter = getRelevanteEnheterForBruker(brukersGeografiskeEnhet, brukersOppfolgingsenhet),
            servicegruppe = oppfolgingsstatus?.servicegruppe,
            fornavn = personInfo.fornavn,
            manuellStatus = manuellStatus,
            varsler = listOfNotNull(
                if (oppfolgingsenhetLokalOgUlik(brukersGeografiskeEnhet, brukersOppfolgingsenhet)) {
                    BrukerVarsel.LOKAL_OPPFOLGINGSENHET
                } else {
                    null
                },
                if (sisteVedtak?.innsatsgruppe == null && oppfolgingsstatus?.servicegruppe != null) {
                    BrukerVarsel.MANGLER_14A_VEDTAK
                } else {
                    null
                },
                if (sisteVedtak?.innsatsgruppe == null && oppfolgingsstatus?.servicegruppe == null) {
                    BrukerVarsel.MANGLER_INNSATSGRUPPE_OG_SERVICEGRUPPE
                } else {
                    null
                },
            ),
        )
    }

    @Serializable
    data class Brukerdata(
        val fnr: String,
        val innsatsgruppe: Innsatsgruppe?,
        val enheter: List<NavEnhetDbo>,
        val servicegruppe: String?,
        val fornavn: String?,
        val manuellStatus: ManuellStatusDto?,
        val varsler: List<BrukerVarsel>,
    )

    enum class BrukerVarsel {
        LOKAL_OPPFOLGINGSENHET,
        MANGLER_14A_VEDTAK,
        MANGLER_INNSATSGRUPPE_OG_SERVICEGRUPPE,
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

    val virtuellOppfolgingsenhet = if (oppfolgingsenhet != null && oppfolgingsenhet.type !in listOf(
            Norg2Type.FYLKE,
            Norg2Type.LOKAL,
        )
    ) {
        oppfolgingsenhet
    } else {
        null
    }
    return listOfNotNull(actualGeografiskEnhet, virtuellOppfolgingsenhet)
}
