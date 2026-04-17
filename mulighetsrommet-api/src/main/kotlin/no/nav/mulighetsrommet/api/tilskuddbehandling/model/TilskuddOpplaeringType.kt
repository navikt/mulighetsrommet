package no.nav.mulighetsrommet.api.tilskuddbehandling.model

import kotlinx.serialization.Serializable

@Serializable
enum class TilskuddOpplaeringType {
    SKOLEPENGER,
    STUDIEREISE,
    EKSAMENSAVGIFT,
    SEMESTERAVGIFT,
    INTEGRERT_BOTILBUD,
}
