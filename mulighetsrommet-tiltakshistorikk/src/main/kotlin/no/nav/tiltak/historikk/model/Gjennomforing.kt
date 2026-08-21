package no.nav.tiltak.historikk.model

import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID

data class Gjennomforing(
    val id: UUID,
    val type: Type,
    val tiltakskode: Tiltakskode,
    val arrangorOrganisasjonsnummer: String,
    val navn: String?,
    val deltidsprosent: Double?,
) {
    enum class Type {
        GRUPPE,
        ENKELTPLASS,
    }
}
