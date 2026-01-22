package no.nav.tiltak.okonomi.oebs

import no.nav.tiltak.okonomi.AnnullerBestilling
import no.nav.tiltak.okonomi.model.Bestilling
import no.nav.tiltak.okonomi.model.Faktura
import no.nav.tiltak.okonomi.model.OebsKontering

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
            valutaKode = bestilling.valuta.name,
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

        return OebsFakturaMelding(
            kilde = OebsKilde.TILTADM,
            fakturaNummer = faktura.fakturanummer,
            opprettelsesTidspunkt = faktura.besluttetTidspunkt,
            organisasjonsNummer = bestilling.arrangorHovedenhet.value,
            bedriftsNummer = bestilling.arrangorUnderenhet.value,
            totalSum = faktura.belop,
            saksbehandler = faktura.behandletAv.part,
            bdmGodkjenner = faktura.besluttetAv.part,
            fakturaDato = faktura.besluttetTidspunkt.toLocalDate(),
            betalingsKanal = faktura.betalingsinformasjon?.betalingsKanal ?: OebsBetalingskanal.BBAN,
            bankKontoNummer = faktura.betalingsinformasjon?.kontonummer?.value ?: faktura.betalingsinformasjon?.iban,
            kidNummer = faktura.betalingsinformasjon?.kid?.value,
            bankNavn = faktura.betalingsinformasjon?.bankNavn,
            bankLandKode = faktura.betalingsinformasjon?.bankLandKode,
            bicSwiftKode = faktura.betalingsinformasjon?.bic,
            valutaKode = faktura.valuta.name,
            meldingTilLeverandor = faktura.beskrivelse.takeIf { faktura.betalingsinformasjon?.kid == null },
            beskrivelse = faktura.beskrivelse,
            fakturaLinjer = linjer,
        )
    }
}
