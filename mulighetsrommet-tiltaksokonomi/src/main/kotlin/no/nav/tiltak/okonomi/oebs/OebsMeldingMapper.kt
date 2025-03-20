package no.nav.tiltak.okonomi.oebs

import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.AnnullerBestilling
import no.nav.tiltak.okonomi.model.Bestilling
import no.nav.tiltak.okonomi.model.Faktura
import no.nav.tiltak.okonomi.model.OebsKontering
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object OebsMeldingMapper {

    fun toOebsBestillingMelding(
        bestilling: Bestilling,
        kontering: OebsKontering,
        selger: OebsBestillingMelding.Selger,
    ): OebsBestillingMelding {
        val linjer = bestilling.linjer.map { linje ->
            OebsBestillingMelding.Linje(
                linjeNummer = linje.linjenummer,
                antall = linje.belop,
                pris = 1,
                periode = linje.periode.start.monthValue.toString().padStart(2, '0'),
                startDato = linje.periode.start,
                sluttDato = linje.periode.getLastInclusiveDate(),
            )
        }

        return OebsBestillingMelding(
            kilde = OebsKilde.TILTADM,
            bestillingsNummer = bestilling.bestillingsnummer,
            opprettelsesTidspunkt = bestilling.opprettelse.besluttetTidspunkt,
            bestillingsType = OebsBestillingType.NY,
            selger = selger,
            rammeavtaleNummer = bestilling.avtalenummer,
            totalSum = bestilling.belop,
            valutaKode = "NOK",
            saksbehandler = bestilling.opprettelse.behandletAv.part,
            bdmGodkjenner = bestilling.opprettelse.besluttetAv.part,
            startDato = bestilling.periode.start,
            sluttDato = bestilling.periode.getLastInclusiveDate(),
            bestillingsLinjer = linjer,
            statsregnskapsKonto = kontering.statligRegnskapskonto,
            artsKonto = kontering.statligArtskonto,
            kontor = bestilling.kostnadssted.value,
            tilsagnsAar = bestilling.periode.start.year,
        )
    }

    fun toOebsAnnulleringMelding(
        bestilling: Bestilling,
        annullerBestilling: AnnullerBestilling,
    ): OebsAnnulleringMelding {
        return OebsAnnulleringMelding(
            bestillingsNummer = bestilling.bestillingsnummer,
            opprettelsesTidspunkt = annullerBestilling.besluttetTidspunkt,
            kilde = OebsKilde.TILTADM,
            bestillingsType = OebsBestillingType.ANNULLER,
            selger = OebsAnnulleringMelding.Selger(
                organisasjonsNummer = bestilling.arrangorHovedenhet.value,
                bedriftsNummer = bestilling.arrangorUnderenhet.value,
            ),
        )
    }

    fun toOebsFakturaMelding(
        bestilling: Bestilling,
        faktura: Faktura,
        erSisteFaktura: Boolean,
    ): OebsFakturaMelding {
        val linjer = faktura.linjer.mapIndexed { index, linje ->
            OebsFakturaMelding.Linje(
                bestillingsNummer = bestilling.bestillingsnummer,
                bestillingsLinjeNummer = linje.linjenummer,
                antall = linje.belop,
                pris = 1,
                erSisteFaktura = erSisteFaktura && index == faktura.linjer.lastIndex,
            )
        }

        val beskrivelse = """
        |Utbetaling fra Nav
        |Tiltak: ${bestilling.tiltakskode}
        |Periode: ${formatPeriode(faktura.periode)}
        """.trimMargin()

        return OebsFakturaMelding(
            kilde = OebsKilde.TILTADM,
            fakturaNummer = faktura.fakturanummer,
            opprettelsesTidspunkt = faktura.besluttetTidspunkt,
            organisasjonsNummer = bestilling.arrangorHovedenhet.value,
            bedriftsNummer = bestilling.arrangorUnderenhet.value,
            totalSum = faktura.belop,
            valutaKode = "NOK",
            saksbehandler = faktura.behandletAv.part,
            bdmGodkjenner = faktura.besluttetAv.part,
            fakturaDato = faktura.besluttetTidspunkt.toLocalDate(),
            betalingsKanal = OebsBetalingskanal.BBAN,
            bankKontoNummer = faktura.kontonummer?.value,
            kidNummer = faktura.kid?.value,
            bankNavn = null,
            bankLandKode = null,
            bicSwiftKode = null,
            meldingTilLeverandor = beskrivelse.takeIf { faktura.kid == null },
            beskrivelse = beskrivelse,
            fakturaLinjer = linjer,
        )
    }
}

private fun formatPeriode(periode: Periode): String = "${formatDate(periode.start)} - ${formatDate(periode.getLastInclusiveDate())}"

private fun formatDate(localDate: LocalDate): String = localDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
