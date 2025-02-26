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

    companion object {
        fun fromOpprettBestilling(bestilling: OpprettBestilling, status: BestillingStatusType): Bestilling {
            val perioder = divideBelopByMonthsInPeriode(bestilling.periode, bestilling.belop)
            return Bestilling(
                tiltakskode = bestilling.tiltakskode,
                arrangorHovedenhet = bestilling.arrangor.hovedenhet,
                arrangorUnderenhet = bestilling.arrangor.hovedenhet,
                kostnadssted = bestilling.kostnadssted,
                bestillingsnummer = bestilling.bestillingsnummer,
                avtalenummer = bestilling.avtalenummer,
                belop = bestilling.belop,
                periode = bestilling.periode,
                status = status,
                opprettetAv = bestilling.opprettetAv,
                opprettetTidspunkt = bestilling.opprettetTidspunkt,
                besluttetAv = bestilling.besluttetAv,
                besluttetTidspunkt = bestilling.besluttetTidspunkt,
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
