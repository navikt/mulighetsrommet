package no.nav.mulighetsrommet.domain.arena

import kotlinx.serialization.Serializable

@Serializable
data class ArenaTiltak (
    val TILTAKSNAVN: String,
    val TILTAKSGRUPPEKODE: String?,
    val REG_DATO: String?,
    val REG_USER: String?,
    val MOD_DATO: String?,
    val MOD_USER: String?,
    val TILTAKSKODE: String,
    val DATO_FRA: String,
    val DATO_TIL: String,
    val AVSNITT_ID_GENERELT: Int?,
    val STATUS_BASISYTELSE: String?,
    val ADMINISTRASJONKODE: String?,
    val STATUS_KOPI_TILSAGN: String?,
    val ARKIVNOKKEL: String?,
    val STATUS_ANSKAFFELSE: String?,
    val MAKS_ANT_PLASSER: Int?,
    val MAKS_ANT_SOKERE: Int?,
    val STATUS_FAST_ANT_PLASSER: String?,
    val STATUS_SJEKK_ANT_DELTAKERE: String?,
    val STATUS_KALKULATOR: String?,
    val RAMMEAVTALE: String?,
    val OPPLAERINGSGRUPPE: String?,
    val HANDLINGSPLAN: String?,
    val STATUS_SLUTTDATO: String?,
    val MAKS_PERIODE: String?,
    val STATUS_MELDEPLIKT: String?,
    val STATUS_VEDTAK: String?,
    val STATUS_IA_AVTALE: String?,
    val STATUS_TILLEGGSSTONADER: String?,
    val STATUS_UTDANNING: String?,
    val AUTOMATISK_TILSAGNSBREV: String?,
    val STATUS_BEGRUNNELSE_INNSOKT: String?,
    val STATUS_HENVISNING_BREV: String?,
    val STATUS_KOPIBREV: String?
)
