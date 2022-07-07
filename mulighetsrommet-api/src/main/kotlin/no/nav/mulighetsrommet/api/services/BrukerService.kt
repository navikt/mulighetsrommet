package no.nav.mulighetsrommet.api.services

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClient
import no.nav.mulighetsrommet.api.clients.vedtak.VeilarbvedtaksstotteClient
import no.nav.mulighetsrommet.api.domain.Innsatsgruppe

class BrukerService(
    private val veilarboppfolgingClient: VeilarboppfolgingClient,
    private val veilarbvedtaksstotteClient: VeilarbvedtaksstotteClient
) {

    suspend fun hentBrukerdata(fnr: String, accessToken: String?): Brukerdata {
        val oppfolgingsenhet = veilarboppfolgingClient.hentOppfolgingsstatus(fnr, accessToken)
        val sisteVedtak = veilarbvedtaksstotteClient.hentSiste14AVedtak(fnr, accessToken)

        return Brukerdata(
            fnr = fnr,
            oppfolgingsenhet = oppfolgingsenhet?.oppfolgingsenhet?.navn ?: "Oppfølgingsenhet ikke satt",
            innsatsgruppe = sisteVedtak?.innsatsgruppe
        )
    }
}

@Serializable
data class Brukerdata(
    val fnr: String,
    val innsatsgruppe: Innsatsgruppe?,
    val oppfolgingsenhet: String
)
