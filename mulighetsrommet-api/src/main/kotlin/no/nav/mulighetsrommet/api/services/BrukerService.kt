package no.nav.mulighetsrommet.api.services

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClient
import no.nav.mulighetsrommet.api.clients.person.VeilarbpersonClient
import no.nav.mulighetsrommet.api.clients.vedtak.VeilarbvedtaksstotteClient
import no.nav.mulighetsrommet.api.domain.Innsatsgruppe
import no.nav.mulighetsrommet.api.domain.Oppfolgingsenhet

class BrukerService(
    private val veilarboppfolgingClient: VeilarboppfolgingClient,
    private val veilarbvedtaksstotteClient: VeilarbvedtaksstotteClient,
    private val veilarbpersonClient: VeilarbpersonClient,
) {

    suspend fun hentBrukerdata(fnr: String, accessToken: String?): Brukerdata {
        val oppfolgingsenhet = veilarboppfolgingClient.hentOppfolgingsstatus(fnr, accessToken)
        val sisteVedtak = veilarbvedtaksstotteClient.hentSiste14AVedtak(fnr, accessToken)
        val personInfo = veilarbpersonClient.hentPersonInfo(fnr, accessToken)

        return Brukerdata(
            fnr = fnr,
            oppfolgingsenhet = oppfolgingsenhet?.oppfolgingsenhet,
            innsatsgruppe = sisteVedtak?.innsatsgruppe,
            fornavn = personInfo?.fornavn
        )
    }
}

@Serializable
data class Brukerdata(
    val fnr: String,
    val innsatsgruppe: Innsatsgruppe?,
    val oppfolgingsenhet: Oppfolgingsenhet?,
    val fornavn: String?,
)
