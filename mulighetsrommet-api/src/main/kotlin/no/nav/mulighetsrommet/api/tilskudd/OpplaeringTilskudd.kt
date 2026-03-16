package no.nav.mulighetsrommet.api.tilskudd

import java.util.UUID

data class OpplaeringTilskudd(
    val id: UUID,
    val navn: String,
    val kode: Kode,
) {
    enum class Kode {
        SKOLEPENGER,
        STUDIEREISE,
        EKSAMENSAVGIFT,
        SEMESTERAVGIFT,
        INTEGRERT_BOTILBUD,
    }
}
