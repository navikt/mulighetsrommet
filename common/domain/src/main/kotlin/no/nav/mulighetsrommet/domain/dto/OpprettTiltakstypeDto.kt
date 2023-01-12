package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class OpprettTiltakstypeDto(
    val tiltakstypenavn: String,
    @Serializable(with = LocalDateSerializer::class)
    val fraDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val tilDato: LocalDate,
    val tiltaksgruppekode: Tiltaksgruppekode,
    val tiltakskode: String,
    val rettTilTiltakspenger: Boolean,
    val administrasjonskode: Administrasjonskode,
    val kopiAvTilsagnsbrev: Boolean,
    val harAnskaffelse: Boolean,
    val rammeavtale: Rammeavtale? = null,
    val opplaringsgruppe: Opplaringsgruppe,
    val handlingsplan: Handlingsplan,
    val harObligatoriskSluttdato: Boolean,
    val harStatusSluttdato: Boolean,
    val harStatusMeldeplikt: Boolean,
    val harStatusVedtak: Boolean,
    val harStatusIAAvtale: Boolean,
    val harStatusTilleggstonad: Boolean,
    val harStatusUtdanning: Boolean,
    val harAutomatiskTilsagnsbrev: Boolean,
    val harStatusBegrunnelseInnsok: Boolean,
    val harStatusHenvisningsbrev: Boolean,
    val harStatusKopibrev: Boolean
)

@Serializable
enum class Tiltaksgruppekode {
    AFT,
    AMB,
    ARBPRAKS,
    ARBRREHAB,
    ARBTREN,
    AVKLARING,
    BEHPSSAM,
    ETAB,
    FORSOK,
    LONNTILS,
    OPPFOLG,
    OPPL,
    TILRETTE,
    UTFAS,
    VARIGASV,
}

@Serializable
enum class Administrasjonskode {
    AMO, IND, INST
}

@Serializable
enum class Rammeavtale {
    SKAL, KAN, IKKE
}

@Serializable
enum class Opplaringsgruppe {
    AMO, UTD
}

@Serializable
enum class Handlingsplan {
    SOK, LAG, TIL
}
