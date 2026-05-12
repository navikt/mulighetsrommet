package no.nav.mulighetsrommet.api.totrinnskontroll.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.Arena
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon

@Serializable
sealed class TotrinnskontrollAgent {
    @Serializable
    @SerialName("NAV_ANSATT")
    data class NavAnsatt(val navIdent: String) : TotrinnskontrollAgent()

    @Serializable
    @SerialName("SYSTEM")
    data class System(val system: String) : TotrinnskontrollAgent()

    @Serializable
    @SerialName("ARRANGOR")
    data object Arrangor : TotrinnskontrollAgent()
}

fun Agent.toAgentHendelse(): TotrinnskontrollAgent = when (this) {
    is NavIdent -> TotrinnskontrollAgent.NavAnsatt(value)

    Arena,
    Tiltaksadministrasjon,
    -> TotrinnskontrollAgent.System(toString())

    Arrangor -> TotrinnskontrollAgent.Arrangor
}
