package no.nav.mulighetsrommet.domain.arena

import kotlinx.serialization.Serializable

@Serializable
data class ArenaTiltaksgjennomforing (
    val TILTAKGJENNOMFORING_ID: Int,
    val SAK_ID: Int,
    val TILTAKSKODE: String,
    val ANTALL_DELTAKERE: Int?,
    val ANTALL_VARIGHET: Int?,
    val DATO_FRA: String,
    val DATO_TIL: String,
    val FAGPLANKODE: String?,
    val MAALEENHET_VARIGHET: String?,
    val TEKST_FAGBESKRIVELSE: String?,
    val TEKST_KURSSTED: String?,
    val TEKST_MAALGRUPPE: String?,
    val STATUS_TREVERDIKODE_INNSOKNING: String?,
    val REG_DATO: String?,
    val REG_USER: String?,
    val MOD_DATO: String?,
    val MOD_USER: String?,
    val LOKALTNAVN: String,
    val TILTAKSTATUSKODE: String?,
    val PROSENT_DELTID: Double?,
    val KOMMENTAR: String?,
    val ARBGIV_ID_ARRANGOR: Int?,
    val PROFILELEMENT_ID_GEOGRAFI: Int?,
    val KLOKKETID_FREMMOTE: Int?,
    val DATO_FREMMOTE: String?,
    val BEGRUNNELSE_STATUS: String?,
    val AVTALE_ID: Int?,
    val AKTIVITET_ID: Int?,
    val DATO_INNSOKNINGSTART: String?,
    val GML_FRA_DATO: String?,
    val GML_TIL_DATO: String?,
    val AETAT_FREMMOTEREG: Int?,
    val AETAT_KONTERINGSSTED: String?,
    val OPPLAERINGNIVAAKODE: String?,
    val TILTAKGJENNOMFORING_ID_REL: Int?,
    val PROFILELEMENT_ID_OPPL_TILTAK: Int?,
    val DATO_OPPFOLGING_OK: String?,
    val PARTISJON: Int?,
    val MAALFORM_KRAVBREV: String?
)
