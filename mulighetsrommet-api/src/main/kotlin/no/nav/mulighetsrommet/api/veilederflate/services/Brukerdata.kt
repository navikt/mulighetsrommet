package no.nav.mulighetsrommet.api.veilederflate.services

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.oppfolging.ManuellStatusDto
import no.nav.mulighetsrommet.api.navenhet.NavEnhetDto
import no.nav.mulighetsrommet.model.Innsatsgruppe
import no.nav.mulighetsrommet.model.NorskIdent

@Serializable
data class Brukerdata(
    val fnr: NorskIdent,
    val innsatsgruppe: Innsatsgruppe?,
    val enheter: List<NavEnhetDto>,
    val fornavn: String?,
    val manuellStatus: ManuellStatusDto,
    val erUnderOppfolging: Boolean,
    val erSykmeldtMedArbeidsgiver: Boolean,
    val varsler: List<BrukerdataVarsel>,
)

enum class BrukerdataVarsel {
    LOKAL_OPPFOLGINGSENHET,
    BRUKER_IKKE_UNDER_OPPFOLGING,
    BRUKER_UNDER_OPPFOLGING_MEN_MANGLER_14A_VEDTAK,
}
