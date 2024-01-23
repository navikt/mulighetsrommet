package no.nav.mulighetsrommet.api.services

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.oppfolging.ManuellStatusDto
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClient
import no.nav.mulighetsrommet.api.clients.person.VeilarbpersonClient
import no.nav.mulighetsrommet.api.clients.vedtak.Innsatsgruppe
import no.nav.mulighetsrommet.api.clients.vedtak.VeilarbvedtaksstotteClient
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dto.EmbeddedNavEnhet

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
            oppfolgingsenhet = brukersOppfolgingsenhet?.toEmbeddedNavEnhet(),
            geografiskEnhet = brukersGeografiskeEnhet?.toEmbeddedNavEnhet(),
            servicegruppe = oppfolgingsstatus?.servicegruppe,
            fornavn = personInfo.fornavn,
            manuellStatus = manuellStatus,
        )
    }

    @Serializable
    data class Brukerdata(
        val fnr: String,
        val innsatsgruppe: Innsatsgruppe?,
        val oppfolgingsenhet: EmbeddedNavEnhet?,
        val geografiskEnhet: EmbeddedNavEnhet?,
        val servicegruppe: String?,
        val fornavn: String?,
        val manuellStatus: ManuellStatusDto?,
    )
}

fun NavEnhetDbo.toEmbeddedNavEnhet(): EmbeddedNavEnhet {
    return EmbeddedNavEnhet(
        enhetsnummer = enhetsnummer,
        navn = navn,
        type = type,
        overordnetEnhet = overordnetEnhet,
    )
}
