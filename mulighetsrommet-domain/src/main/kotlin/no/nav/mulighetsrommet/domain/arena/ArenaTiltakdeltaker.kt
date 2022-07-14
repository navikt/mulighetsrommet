package no.nav.mulighetsrommet.domain.arena

import kotlinx.serialization.Serializable

@Serializable
data class ArenaTiltakdeltaker (
    val TILTAKDELTAKER_ID: Int,
    val PERSON_ID: Int,
    val TILTAKGJENNOMFORING_ID: Int,
    val DELTAKERSTATUSKODE: String,
    val DELTAKERTYPEKODE: String?,
    val AARSAKVERDIKODE_STATUS: String?,
    val OPPMOTETYPEKODE: String?,
    val PRIORITET: String?,
    val BEGRUNNELSE_INNSOKT: String?,
    val BEGRUNNELSE_PRIORITERING: String?,
    val REG_DATO: String?,
    val REG_USER: String?,
    val MOD_DATO: String?,
    val MOD_USER: String?,
    val DATO_SVARFRIST: String?,
    val DATO_FRA: String,
    val DATO_TIL: String,
    val BEGRUNNELSE_STATUS: String?,
    val PROSENT_DELTID: Int?,
    val BRUKERID_STATUSENDRING: String?,
    val DATO_STATUSENDRING: String?,
    val AKTIVITET_ID: Int?,
    val BRUKERID_ENDRING_PRIORITERING: String?,
    val DATO_ENDRING_PRIORITERING: String?,
    val DOKUMENTKODE_SISTE_BREV: String?,
    val STATUS_INNSOK_PAKKE: String?,
    val STATUS_OPPTAK_PAKKE: String?,
    val OPPLYSNINGER_INNSOK: String?,
    val PARTISJON: Int?,
    val BEGRUNNELSE_BESTILLING: String?,
    val ANTALL_DAGER_PR_UKE: Int?
)
