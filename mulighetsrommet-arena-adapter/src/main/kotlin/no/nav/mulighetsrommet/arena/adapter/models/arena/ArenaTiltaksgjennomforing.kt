package no.nav.mulighetsrommet.arena.adapter.models.arena

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.arena.JaNeiStatus
import no.nav.mulighetsrommet.serializers.FloatToIntSerializer

@Suppress("PropertyName")
@Serializable
data class ArenaTiltaksgjennomforing(
    val TILTAKGJENNOMFORING_ID: Int,
    val SAK_ID: Int,
    val TILTAKSKODE: String,
    val REG_DATO: String,
    val DATO_FRA: String?,
    val DATO_TIL: String?,
    val LOKALTNAVN: String?,
    val ARBGIV_ID_ARRANGOR: Int?,
    val STATUS_TREVERDIKODE_INNSOKNING: JaNeiStatus?,
    @Serializable(with = FloatToIntSerializer::class)
    val ANTALL_DELTAKERE: Int?,
    val TILTAKSTATUSKODE: String,
    val AVTALE_ID: Int?,
    val PROSENT_DELTID: Double,
    val EKSTERN_ID: String? = null,
)
