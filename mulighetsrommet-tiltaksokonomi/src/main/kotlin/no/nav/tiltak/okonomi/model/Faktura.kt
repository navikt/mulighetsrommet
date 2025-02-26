package no.nav.tiltak.okonomi.model

import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.OkonomiPart
import no.nav.tiltak.okonomi.OpprettFaktura
import no.nav.tiltak.okonomi.helpers.divideBelopByMonthsInPeriode
import java.time.LocalDateTime

data class Faktura(
    val bestillingsnummer: String,
    val fakturanummer: String,
    val kontonummer: Kontonummer,
    val kid: Kid?,
    val belop: Int,
    val periode: Periode,
    val status: FakturaStatusType,
    val behandletAv: OkonomiPart,
    val behandletTidspunkt: LocalDateTime,
    val besluttetAv: OkonomiPart,
    val besluttetTidspunkt: LocalDateTime,
    val linjer: List<Linje>,
) {
    data class Linje(
        val linjenummer: Int,
        val periode: Periode,
        val belop: Int,
    )

    companion object {
        fun fromOpprettFaktura(faktura: OpprettFaktura, bestillingLinjer: List<Bestilling.Linje>): Faktura {
            val bestillingLinjerByMonth = bestillingLinjer.associateBy { it.periode.start.month }
            val perioder = divideBelopByMonthsInPeriode(faktura.periode, faktura.belop)
            return Faktura(
                bestillingsnummer = faktura.bestillingsnummer,
                fakturanummer = faktura.fakturanummer,
                kontonummer = faktura.betalingsinformasjon.kontonummer,
                kid = faktura.betalingsinformasjon.kid,
                belop = faktura.belop,
                periode = faktura.periode,
                status = FakturaStatusType.UTBETALT,
                behandletAv = faktura.behandletAv,
                behandletTidspunkt = faktura.behandletTidspunkt,
                besluttetAv = faktura.behandletAv,
                besluttetTidspunkt = faktura.behandletTidspunkt,
                linjer = perioder.map { (periode, belop) ->
                    val bestillingLinje = bestillingLinjerByMonth.getValue(periode.start.month)
                    Linje(
                        linjenummer = bestillingLinje.linjenummer,
                        periode = periode,
                        belop = belop,
                    )
                },
            )
        }
    }
}

enum class FakturaStatusType {
    UTBETALT,
}
