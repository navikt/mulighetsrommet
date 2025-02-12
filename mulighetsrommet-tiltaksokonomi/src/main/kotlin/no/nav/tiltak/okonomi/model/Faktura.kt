package no.nav.tiltak.okonomi.model

import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.okonomi.OkonomiPart
import java.time.LocalDateTime

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
    val linjer: List<Linje>,
) {
    data class Linje(
        val linjenummer: Int,
        val periode: Periode,
        val belop: Int,
    )
}

enum class FakturaStatusType {
    UTBETALT,
}
