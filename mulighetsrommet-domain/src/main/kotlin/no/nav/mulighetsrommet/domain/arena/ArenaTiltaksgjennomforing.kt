package no.nav.mulighetsrommet.domain.arena

import kotlinx.serialization.Serializable

@Serializable
data class ArenaTiltaksgjennomforing(
    val TILTAKGJENNOMFORING_ID: Int,
    val SAK_ID: Int,
    val TILTAKSKODE: String,
    val DATO_FRA: String?,
    val DATO_TIL: String?,
    val LOKALTNAVN: String?,
    val ARBGIV_ID_ARRANGOR: Int?,
    val STATUS_TREVERDIKODE_INNSOKNING: JaNeiStatus?,
    val ANTALL_DELTAKERE: Int?
)
