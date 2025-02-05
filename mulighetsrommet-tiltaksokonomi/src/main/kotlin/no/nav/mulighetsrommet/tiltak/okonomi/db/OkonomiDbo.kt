package no.nav.mulighetsrommet.tiltak.okonomi.db

import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.tiltak.okonomi.OkonomiPart
import java.time.LocalDateTime

data class BestillingDbo(
    val tiltakskode: Tiltakskode,
    val arrangorHovedenhet: Organisasjonsnummer,
    val arrangorUnderenhet: Organisasjonsnummer,
    val kostnadssted: NavEnhetNummer,
    val bestillingsnummer: String,
    val avtalenummer: String?,
    val belop: Int,
    val periode: Periode,
    val opprettetAv: OkonomiPart,
    val opprettetTidspunkt: LocalDateTime,
    val besluttetAv: OkonomiPart,
    val besluttetTidspunkt: LocalDateTime,
    val annullert: Boolean,
    val linjer: List<LinjeDbo>,
)

data class FakturaDbo(
    val bestillingsnummer: String,
    val fakturanummer: String,
    val kontonummer: Kontonummer,
    val kid: Kid?,
    val belop: Int,
    val periode: Periode,
    val opprettetAv: OkonomiPart,
    val opprettetTidspunkt: LocalDateTime,
    val besluttetAv: OkonomiPart,
    val besluttetTidspunkt: LocalDateTime,
    val linjer: List<LinjeDbo>,
)

data class LinjeDbo(
    val linjenummer: Int,
    val periode: Periode,
    val belop: Int,
)
