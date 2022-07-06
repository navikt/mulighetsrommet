package no.nav.mulighetsrommet.api.services

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.oppfolging.Oppfolgingsstatus
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClient

/**
 * TODO Ta i mot client for hhv. veilarbvedtaksstotte som argumenter til BrukerService
 */
class BrukerService(private val veilarboppfolgingClient: VeilarboppfolgingClient) {

    suspend fun hentBrukerdata(fnr: String, accessToken: String?): Brukerdata {
        val oppfolgingsenhet = hentOppfolgingsenhet(fnr, accessToken)

        return Brukerdata(
            fnr = fnr,
            oppfolgingsenhet = oppfolgingsenhet?.oppfolgingsenhet?.navn ?: "Oppfølgingsenhet ikke satt",
            innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS // TODO Ikke hardkode verdien for innsatsgruppe
        )
    }

    private suspend fun hentOppfolgingsenhet(fnr: String, accessToken: String?): Oppfolgingsstatus? {
        return veilarboppfolgingClient.hentOppfolgingsstatus(fnr, accessToken)
    }
}

@Serializable
data class Brukerdata(
    val fnr: String,
    val innsatsgruppe: Innsatsgruppe,
    val oppfolgingsenhet: String
)

enum class Innsatsgruppe {
    STANDARD_INNSATS, SITUASJONSBESTEMT_INNSATS, SPESIELT_TILPASSET_INNSATS, GRADERT_VARIG_TILPASSET_INNSATS, VARIG_TILPASSET_INNSATS
}
