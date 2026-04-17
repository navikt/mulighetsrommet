package no.nav.mulighetsrommet.api.tilskuddbehandling.model

import kotlinx.serialization.Serializable

@Serializable
enum class TilskuddType {
    SKOLEPENGER,
    SEMESTERAVGIFT,
    EKSAMENSGEBYR,
}
