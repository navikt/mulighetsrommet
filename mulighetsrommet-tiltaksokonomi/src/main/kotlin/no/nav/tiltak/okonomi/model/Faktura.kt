package no.nav.tiltak.okonomi.model

import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.tiltak.okonomi.Bestillingsnummer
import no.nav.tiltak.okonomi.FakturaStatusType
import no.nav.tiltak.okonomi.Fakturanummer
import no.nav.tiltak.okonomi.GjorOppBestilling
import no.nav.tiltak.okonomi.OkonomiPart
import no.nav.tiltak.okonomi.OpprettFaktura
import no.nav.tiltak.okonomi.oebs.OebsBetalingskanal
import java.time.Instant

data class Faktura(
    val bestillingsnummer: Bestillingsnummer,
    val fakturanummer: Fakturanummer,
    val betalingsinformasjon: Betalingsinformasjon?,
    val belop: Int,
    val periode: Periode,
    val status: FakturaStatusType,
    val statusSistOppdatert: Instant,
    val behandletAv: OkonomiPart,
    val behandletTidspunkt: Instant,
    val besluttetAv: OkonomiPart,
    val besluttetTidspunkt: Instant,
    val linjer: List<Linje>,
    val beskrivelse: String?,
    val valuta: Valuta,
) {
    sealed interface Betalingsreferanse {
        @JvmInline
        value class KidNummer(
            val kid: Kid,
        ) : Betalingsreferanse

        @JvmInline
        value class MeldingTilMottaker(
            val melding: String,
        ) : Betalingsreferanse

        companion object {
            fun from(betalingsreferanse: OpprettFaktura.Betalingsreferanse): Betalingsreferanse = when (betalingsreferanse) {
                is OpprettFaktura.Betalingsreferanse.KidNummer -> KidNummer(betalingsreferanse.kid)
                is OpprettFaktura.Betalingsreferanse.MeldingTilMottaker -> MeldingTilMottaker(betalingsreferanse.melding)
            }
        }
    }
    data class Betalingsinformasjon(
        val betalingsKanal: OebsBetalingskanal,
        val kontonummer: Kontonummer?,
        val referanse: Betalingsreferanse?,
        val bankNavn: String?,
        val bankLandKode: String?,
        val bic: String?,
        val iban: String?,
    ) {
        companion object {
            fun from(betalingsinformasjon: OpprettFaktura.Betalingsinformasjon): Betalingsinformasjon {
                return when (betalingsinformasjon) {
                    is OpprettFaktura.Betalingsinformasjon.BBan -> Betalingsinformasjon(
                        betalingsKanal = OebsBetalingskanal.BBAN,
                        kontonummer = betalingsinformasjon.kontonummer,
                        referanse = Betalingsreferanse.from(betalingsinformasjon.referanse),
                        bankNavn = null,
                        bankLandKode = null,
                        bic = null,
                        iban = null,
                    )

                    is OpprettFaktura.Betalingsinformasjon.IBan -> Betalingsinformasjon(
                        betalingsKanal = OebsBetalingskanal.IBAN,
                        kontonummer = null,
                        referanse = null,
                        bankNavn = betalingsinformasjon.bankNavn,
                        bankLandKode = betalingsinformasjon.bankLandKode,
                        bic = betalingsinformasjon.bic,
                        iban = betalingsinformasjon.iban,
                    )
                }
            }
        }
    }

    fun erGjorOppFaktura(): Boolean {
        return fakturanummer == gjorOppFakturanummer(bestillingsnummer)
    }

    data class Linje(
        val linjenummer: Int,
        val periode: Periode,
        val belop: Int,
    )

    companion object {
        fun fromOpprettFaktura(
            faktura: OpprettFaktura,
            bestillingLinjer: List<Bestilling.Linje>,
        ): Faktura {
            val bestillingLinjerByMonth = bestillingLinjer.associateBy { it.periode.start.month }
            val perioder = divideBelopByMonthsInPeriode(faktura.periode, faktura.belop)
            return Faktura(
                bestillingsnummer = faktura.bestillingsnummer,
                fakturanummer = faktura.fakturanummer,
                betalingsinformasjon = Betalingsinformasjon.from(faktura.betalingsinformasjon),
                belop = faktura.belop,
                periode = faktura.periode,
                status = FakturaStatusType.SENDT,
                statusSistOppdatert = Instant.now(),
                behandletAv = faktura.behandletAv,
                behandletTidspunkt = faktura.behandletTidspunkt,
                besluttetAv = faktura.besluttetAv,
                besluttetTidspunkt = faktura.besluttetTidspunkt,
                linjer = perioder.map { (periode, belop) ->
                    val bestillingLinje = bestillingLinjerByMonth.getValue(periode.start.month)
                    Linje(
                        linjenummer = bestillingLinje.linjenummer,
                        periode = periode,
                        belop = belop,
                    )
                },
                beskrivelse = faktura.beskrivelse,
                valuta = faktura.valuta,
            )
        }

        fun fromGjorOppBestilling(
            gjorOppBestilling: GjorOppBestilling,
            bestilling: Bestilling,
        ): Faktura {
            val sisteBestillingLinje = bestilling.linjer.last()
            return Faktura(
                bestillingsnummer = bestilling.bestillingsnummer,
                fakturanummer = gjorOppFakturanummer(bestilling.bestillingsnummer),
                betalingsinformasjon = null,
                belop = 0,
                periode = sisteBestillingLinje.periode,
                status = FakturaStatusType.SENDT,
                statusSistOppdatert = Instant.now(),
                behandletAv = gjorOppBestilling.behandletAv,
                behandletTidspunkt = gjorOppBestilling.behandletTidspunkt,
                besluttetAv = gjorOppBestilling.besluttetAv,
                besluttetTidspunkt = gjorOppBestilling.besluttetTidspunkt,
                linjer = listOf(
                    Linje(
                        linjenummer = sisteBestillingLinje.linjenummer,
                        periode = sisteBestillingLinje.periode,
                        belop = 0,
                    ),
                ),
                beskrivelse = null,
                valuta = bestilling.valuta,
            )
        }
    }
}

fun gjorOppFakturanummer(bestillingsnummer: Bestillingsnummer): Fakturanummer = Fakturanummer("$bestillingsnummer-X")
