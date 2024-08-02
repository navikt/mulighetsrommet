package no.nav.mulighetsrommet.arena.adapter.models.arena

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("PropertyName")
interface ArenaTiltakdeltakelse {
    val PERSON_ID: Int
    val TILTAKGJENNOMFORING_ID: Int
    val DELTAKERSTATUSKODE: ArenaTiltakdeltakerStatus
    val DATO_FRA: String?
    val DATO_TIL: String?
    val REG_DATO: String
}

@Suppress("PropertyName")
@Serializable
data class ArenaTiltakdeltaker(
    val TILTAKDELTAKER_ID: Int,
    override val PERSON_ID: Int,
    override val TILTAKGJENNOMFORING_ID: Int,
    override val DELTAKERSTATUSKODE: ArenaTiltakdeltakerStatus,
    override val DATO_FRA: String?,
    override val DATO_TIL: String?,
    override val REG_DATO: String,
) : ArenaTiltakdeltakelse

@Suppress("PropertyName")
@Serializable
data class ArenaHistTiltakdeltaker(
    val HIST_TILTAKDELTAKER_ID: Int,
    override val PERSON_ID: Int,
    override val TILTAKGJENNOMFORING_ID: Int,
    override val DELTAKERSTATUSKODE: ArenaTiltakdeltakerStatus,
    override val DATO_FRA: String?,
    override val DATO_TIL: String?,
    override val REG_DATO: String,
) : ArenaTiltakdeltakelse

@Serializable
enum class ArenaTiltakdeltakerStatus {
    @SerialName("AVSLAG")
    AVSLAG,

    @SerialName("IKKAKTUELL")
    IKKE_AKTUELL,

    @SerialName("NEITAKK")
    TAKKET_NEI_TIL_TILBUD,

    @SerialName("TILBUD")
    TILBUD,

    @SerialName("JATAKK")
    TAKKET_JA_TIL_TILBUD,

    @SerialName("INFOMOETE")
    INFORMASJONSMOTE,

    @SerialName("AKTUELL")
    AKTUELL,

    @SerialName("VENTELISTE")
    VENTELISTE,

    @SerialName("GJENN")
    GJENNOMFORES,

    @SerialName("DELAVB")
    DELTAKELSE_AVBRUTT,

    @SerialName("GJENN_AVB")
    GJENNOMFORING_AVBRUTT,

    @SerialName("GJENN_AVL")
    GJENNOMFORING_AVLYST,

    @SerialName("FULLF")
    FULLFORT,

    @SerialName("IKKEM")
    IKKE_MOTT,

    @SerialName("FEILREG")
    FEILREGISTRERT,
}
