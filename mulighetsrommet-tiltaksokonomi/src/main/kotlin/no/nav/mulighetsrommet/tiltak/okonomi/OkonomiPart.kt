package no.nav.mulighetsrommet.tiltak.okonomi

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.tiltak.okonomi.oebs.Kilde

@Serializable
sealed class OkonomiPart(val part: String) {

    @Serializable
    data class NavAnsatt(val navIdent: NavIdent) : OkonomiPart(navIdent.value)

    @Serializable
    data class System(val kilde: Kilde) : OkonomiPart(kilde.name)

    companion object {
        fun fromString(value: String): OkonomiPart {
            return try {
                System(Kilde.valueOf(value))
            } catch (e: IllegalArgumentException) {
                NavAnsatt(NavIdent(value))
            }
        }
    }
}
