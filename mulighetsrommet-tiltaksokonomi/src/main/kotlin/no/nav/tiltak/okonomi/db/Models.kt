package no.nav.tiltak.okonomi.db

import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.okonomi.OkonomiPart
import java.time.LocalDateTime

data class Bestilling(
    val tiltakskode: Tiltakskode,
    val arrangorHovedenhet: Organisasjonsnummer,
    val arrangorUnderenhet: Organisasjonsnummer,
    val kostnadssted: NavEnhetNummer,
    val bestillingsnummer: String,
    val avtalenummer: String?,
    val belop: Int,
    val periode: Periode,
    val status: BestillingStatusType,
    val opprettetAv: OkonomiPart,
    val opprettetTidspunkt: LocalDateTime,
    val besluttetAv: OkonomiPart,
    val besluttetTidspunkt: LocalDateTime,
    val linjer: List<LinjeDbo>,
)

enum class BestillingStatusType {
    AKTIV,
    ANNULLERT,
    OPPGJORT,
}

data class Faktura(
    val bestillingsnummer: String,
    val fakturanummer: String,
    val kontonummer: Kontonummer,
    val kid: Kid?,
    val belop: Int,
    val periode: Periode,
    val status: FakturaStatusType,
    val opprettetAv: OkonomiPart,
    val opprettetTidspunkt: LocalDateTime,
    val besluttetAv: OkonomiPart,
    val besluttetTidspunkt: LocalDateTime,
    val linjer: List<LinjeDbo>,
)

enum class FakturaStatusType {
    UTBETALT,
}

data class LinjeDbo(
    val linjenummer: Int,
    val periode: Periode,
    val belop: Int,
)
