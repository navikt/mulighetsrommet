package no.nav.mulighetsrommet.arena.adapter.models.arena

import kotlinx.serialization.Serializable

@Suppress("PropertyName")
@Serializable
data class ArenaHistTiltakdeltaker(
    val HIST_TILTAKDELTAKER_ID: Int,
    val PERSON_ID: Int,
    val TILTAKGJENNOMFORING_ID: Int,
    val DELTAKERSTATUSKODE: ArenaTiltakdeltakerStatus,
    val DATO_FRA: String?,
    val DATO_TIL: String?,
    val REG_DATO: String,
)
