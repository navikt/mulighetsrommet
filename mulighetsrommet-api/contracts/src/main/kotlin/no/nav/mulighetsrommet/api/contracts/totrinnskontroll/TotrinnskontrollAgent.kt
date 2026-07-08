package no.nav.mulighetsrommet.api.contracts.totrinnskontroll

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
