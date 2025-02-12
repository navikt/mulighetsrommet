package no.nav.tiltak.okonomi.model

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
    val linjer: List<Linje>,
) {
    data class Linje(
        val linjenummer: Int,
        val periode: Periode,
        val belop: Int,
    )
}

enum class BestillingStatusType {
    AKTIV,
    ANNULLERT,
    OPPGJORT,
}
