package no.nav.mulighetsrommet.domain.arena

import kotlinx.serialization.Serializable

@Serializable
data class ArenaSak (
    val SAK_ID: Int?,
    val SAKSKODE: String?,
    val REG_DATO: String?,
    val REG_USER: String?,
    val MOD_DATO: String?,
    val MOD_USER: String?,
    val TABELLNAVNALIAS: String?,
    val OBJEKT_ID: Int?,
    val AAR: Int?,
    val LOPENRSAK: Int?,
    val DATO_AVSLUTTET: String?,
    val SAKSTATUSKODE: String?,
    val ARKIVNOKKEL: String?,
    val AETATENHET_ARKIV: String?,
    val ARKIVHENVISNING: String?,
    val BRUKERID_ANSVARLIG: String?,
    val AETATENHET_ANSVARLIG: String?,
    val OBJEKT_KODE: String?,
    val STATUS_ENDRET: String?,
    val PARTISJON: Int?,
    val ER_UTLAND: String?
)
