package no.nav.mulighetsrommet.arena.adapter.models.arena

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dto.JaNeiStatus

@Suppress("PropertyName")
@Serializable
data class ArenaTiltak(
    val TILTAKSNAVN: String,
    val TILTAKSGRUPPEKODE: String,
    val TILTAKSKODE: String,
    val REG_DATO: String,
    val MOD_DATO: String,
    val DATO_FRA: String,
    val DATO_TIL: String,
    val STATUS_BASISYTELSE: JaNeiStatus,
    val ADMINISTRASJONKODE: Administrasjonskode,
    val STATUS_KOPI_TILSAGN: JaNeiStatus,
    val STATUS_ANSKAFFELSE: JaNeiStatus,
    val MAKS_ANT_PLASSER: Int?,
    val MAKS_ANT_SOKERE: Int?,
    val STATUS_FAST_ANT_PLASSER: JaNeiStatus?,
    val STATUS_SJEKK_ANT_DELTAKERE: JaNeiStatus?,
    val STATUS_KALKULATOR: JaNeiStatus,
    val RAMMEAVTALE: Rammeavtale?,
    val OPPLAERINGSGRUPPE: String?,
    val HANDLINGSPLAN: Handlingsplan?,
    val STATUS_SLUTTDATO: JaNeiStatus,
    val MAKS_PERIODE: Int?,
    val STATUS_MELDEPLIKT: JaNeiStatus?,
    val STATUS_VEDTAK: JaNeiStatus,
    val STATUS_IA_AVTALE: JaNeiStatus,
    val STATUS_TILLEGGSSTONADER: JaNeiStatus,
    val STATUS_UTDANNING: JaNeiStatus,
    val AUTOMATISK_TILSAGNSBREV: JaNeiStatus,
    val STATUS_BEGRUNNELSE_INNSOKT: JaNeiStatus,
    val STATUS_HENVISNING_BREV: JaNeiStatus,
    val STATUS_KOPIBREV: JaNeiStatus,
)
