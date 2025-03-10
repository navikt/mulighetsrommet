package no.nav.tiltak.okonomi.model

import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.tiltak.okonomi.OkonomiPart
import no.nav.tiltak.okonomi.OpprettBestilling
import no.nav.tiltak.okonomi.helpers.divideBelopByMonthsInPeriode
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
    val opprettelse: Totrinnskontroll,
    val annullering: Totrinnskontroll?,
    val linjer: List<Linje>,
) {
    data class Totrinnskontroll(
        val behandletAv: OkonomiPart,
        val behandletTidspunkt: LocalDateTime,
        val besluttetAv: OkonomiPart,
        val besluttetTidspunkt: LocalDateTime,
    )

    data class Linje(
        val linjenummer: Int,
        val periode: Periode,
        val belop: Int,
    )

    companion object {
        fun fromOpprettBestilling(bestilling: OpprettBestilling): Bestilling {
            val perioder = divideBelopByMonthsInPeriode(bestilling.periode, bestilling.belop)
            return Bestilling(
                tiltakskode = bestilling.tiltakskode,
                arrangorHovedenhet = bestilling.arrangor.hovedenhet,
                arrangorUnderenhet = bestilling.arrangor.underenhet,
                kostnadssted = bestilling.kostnadssted,
                bestillingsnummer = bestilling.bestillingsnummer,
                avtalenummer = bestilling.avtalenummer,
                belop = bestilling.belop,
                periode = bestilling.periode,
                status = BestillingStatusType.BESTILT,
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
            )
        }
    }
}

enum class BestillingStatusType {
    BESTILT,
    AKTIV,
    ANNULLERT,
    OPPGJORT,
}
