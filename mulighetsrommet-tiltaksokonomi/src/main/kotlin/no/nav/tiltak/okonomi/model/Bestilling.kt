package no.nav.tiltak.okonomi.model

import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.tiltak.okonomi.BestillingStatusType
import no.nav.tiltak.okonomi.Bestillingsnummer
import no.nav.tiltak.okonomi.OkonomiFagsystem
import no.nav.tiltak.okonomi.OkonomiPart
import no.nav.tiltak.okonomi.OpprettBestilling
import java.time.Instant

data class Bestilling(
    val tiltakskode: Tiltakskode,
    val arrangorHovedenhet: Organisasjonsnummer,
    val arrangorUnderenhet: Organisasjonsnummer,
    val kostnadssted: NavEnhetNummer,
    val bestillingsnummer: Bestillingsnummer,
    val fagsystem: OkonomiFagsystem,
    val avtalenummer: String?,
    val belop: Int,
    val periode: Periode,
    val status: BestillingStatusType,
    val statusSistOppdatert: Instant,
    val opprettelse: Totrinnskontroll,
    val annullering: Totrinnskontroll?,
    val linjer: List<Linje>,
    val valuta: Valuta,
) {
    data class Totrinnskontroll(
        val behandletAv: OkonomiPart,
        val behandletTidspunkt: Instant,
        val besluttetAv: OkonomiPart,
        val besluttetTidspunkt: Instant,
    )

    data class Linje(
        val linjenummer: Int,
        val periode: Periode,
        val belop: Int,
    )

    companion object {
        fun fromOpprettBestilling(
            fagsystem: OkonomiFagsystem,
            bestilling: OpprettBestilling,
            arrangorHovedenhet: Organisasjonsnummer,
        ): Bestilling {
            val perioder = divideBelopByMonthsInPeriode(bestilling.periode, bestilling.belop)
            return Bestilling(
                tiltakskode = bestilling.tiltakskode,
                arrangorHovedenhet = arrangorHovedenhet,
                arrangorUnderenhet = bestilling.arrangor.organisasjonsnummer,
                kostnadssted = bestilling.kostnadssted,
                bestillingsnummer = bestilling.bestillingsnummer,
                fagsystem = fagsystem,
                avtalenummer = bestilling.avtalenummer,
                belop = bestilling.belop,
                periode = bestilling.periode,
                status = BestillingStatusType.SENDT,
                statusSistOppdatert = Instant.now(),
                opprettelse = Totrinnskontroll(
                    behandletAv = bestilling.behandletAv,
                    behandletTidspunkt = bestilling.behandletTidspunkt,
                    besluttetAv = bestilling.besluttetAv,
                    besluttetTidspunkt = bestilling.besluttetTidspunkt,
                ),
                annullering = null,
                linjer = perioder.mapIndexed { index, (periode, belop) ->
                    Linje(
                        linjenummer = (index + 1),
                        periode = periode,
                        belop = belop,
                    )
                },
                valuta = bestilling.valuta,
            )
        }
    }
}
