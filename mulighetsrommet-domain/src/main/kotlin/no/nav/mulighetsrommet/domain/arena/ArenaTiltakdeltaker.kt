package no.nav.mulighetsrommet.domain.arena

import kotlinx.serialization.Serializable

@Serializable
data class ArenaTiltakdeltaker (
    val TILTAKDELTAKER_ID: Int,
    val PERSON_ID: Int,
    val TILTAKGJENNOMFORING_ID: Int,
    val DELTAKERSTATUSKODE: String,
    val DATO_FRA: String?,
    val DATO_TIL: String?,
)
