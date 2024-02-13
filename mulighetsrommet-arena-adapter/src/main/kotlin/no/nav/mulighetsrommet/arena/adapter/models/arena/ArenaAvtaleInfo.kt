package no.nav.mulighetsrommet.arena.adapter.models.arena

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("PropertyName")
@Serializable
data class ArenaAvtaleInfo(
    val AVTALE_ID: Int,
    val AAR: Int,
    val LOPENRAVTALE: Int,
    val AVTALENAVN: String?,
    val ARKIVREF: String?,
    // Leverandør kan mangle for avtaler med status={PLAN,AVBRU}
    val ARBGIV_ID_LEVERANDOR: Int?,
    // Prisbetingelser kan mangle for avtaler med status={PLAN,AVSLU,AVBRU}
    val PRIS_BETBETINGELSER: String?,
    val DATO_FRA: String?,
    val DATO_TIL: String?,
    val TILTAKSKODE: String,
    val ORGENHET_ANSVARLIG: String,
    val BRUKER_ID_ANSVARLIG: String,
    val TEKST_ANDREOPPL: String?,
    val TEKST_FAGINNHOLD: String?,
    val TEKST_MAALGRUPPE: String?,
    val AVTALEKODE: Avtalekode,
    val AVTALESTATUSKODE: Avtalestatuskode,
    val STATUS_DATO_ENDRET: String?,
    val REG_DATO: String,
    val REG_USER: String,
    val MOD_DATO: String,
    val MOD_USER: String,
    val PROFILELEMENT_ID_OPPL_TILTAK: Int?,
)

@Serializable
enum class Avtalekode {
    @SerialName("AVT")
    Avtale,

    @SerialName("RAM")
    Rammeavtale,
}

@Serializable
enum class Avtalestatuskode {
    @SerialName("PLAN")
    Planlagt,

    @SerialName("GJENF")
    Gjennomforer,

    @SerialName("AVSLU")
    Avsluttet,

    @SerialName("AVBRU")
    Avbrutt,

    @SerialName("OVERF")
    Overfort,
}
