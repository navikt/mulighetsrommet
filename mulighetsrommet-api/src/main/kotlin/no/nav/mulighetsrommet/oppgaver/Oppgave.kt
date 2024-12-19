package no.nav.mulighetsrommet.oppgaver

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.Tiltakskode
import java.util.*

enum class OppgaveType {
    TILSAGN_TIL_BESLUTNING,
    TILSAGN_TIL_ANNULLERING,
}

@Serializable
data class Oppgave(
    val type: OppgaveType,
    val title: String,
    val description: String? = null,
    val tiltakstype: Tiltakskode,
    val link: OppgaveLink? = null,
)

@Serializable
data class OppgaveLink(
    val linkText: String,
    val link: String,
)
