package no.nav.tiltak.okonomi.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.brreg.BrregAdresse
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregHovedenhetDto
import no.nav.mulighetsrommet.brreg.SlettetBrregHovedenhetDto
import no.nav.tiltak.okonomi.AnnullerBestilling
import no.nav.tiltak.okonomi.OpprettBestilling
import no.nav.tiltak.okonomi.OpprettFaktura
import no.nav.tiltak.okonomi.db.OkonomiDatabase
import no.nav.tiltak.okonomi.model.Bestilling
import no.nav.tiltak.okonomi.model.BestillingStatusType
import no.nav.tiltak.okonomi.model.Faktura
import no.nav.tiltak.okonomi.model.OebsKontering
import no.nav.tiltak.okonomi.oebs.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OpprettBestillingError(message: String, cause: Throwable? = null) : Exception(message, cause)

class AnnullerBestillingError(message: String, cause: Throwable? = null) : Exception(message, cause)

class OpprettFakturaError(message: String, cause: Throwable? = null) : Exception(message, cause)

class OkonomiService(
    private val db: OkonomiDatabase,
    private val oebs: OebsTiltakApiClient,
    private val brreg: BrregClient,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    suspend fun opprettBestilling(
        opprettBestilling: OpprettBestilling,
    ): Either<OpprettBestillingError, Bestilling> = db.session {
        queries.bestilling.getByBestillingsnummer(opprettBestilling.bestillingsnummer)?.let {
            log.info("Bestilling ${opprettBestilling.bestillingsnummer} er allerede opprettet")
            return it.right()
        }

        val kontering = queries.kontering
            .getOebsKontering(
                tilskuddstype = opprettBestilling.tilskuddstype,
                tiltakskode = opprettBestilling.tiltakskode,
                periode = opprettBestilling.periode,
            )
            ?: return OpprettBestillingError("Kontering for bestilling ${opprettBestilling.bestillingsnummer} mangler").left()

        val bestilling = Bestilling.fromOpprettBestilling(opprettBestilling)

        return brreg.getHovedenhet(bestilling.arrangorHovedenhet)
            .mapLeft { OpprettBestillingError("Klarte ikke hente hovedenhet ${bestilling.arrangorHovedenhet} fra Brreg: $it") }
            .flatMap {
                when (it) {
                    is BrregHovedenhetDto -> it.right()

                    is SlettetBrregHovedenhetDto -> {
                        OpprettBestillingError("Hovedenhet med orgnr ${bestilling.arrangorHovedenhet} er slettet").left()
                    }
                }
            }
            .flatMap { hovedenhet ->
                getLeverandorAdresse(hovedenhet).map { adresse ->
                    OebsBestillingMelding.Selger(
                        organisasjonsNummer = hovedenhet.organisasjonsnummer.value,
                        organisasjonsNavn = hovedenhet.navn,
                        adresse = adresse,
                        bedriftsNummer = bestilling.arrangorUnderenhet.value,
                    )
                }
            }
            .flatMap { selger ->
                val melding = toOebsBestillingMelding(bestilling, kontering, selger)
                log.info("Sender bestilling ${bestilling.bestillingsnummer} til oebs")
                oebs.sendBestilling(melding).mapLeft {
                    OpprettBestillingError(
                        "Klarte ikke sende bestilling ${bestilling.bestillingsnummer} til oebs",
                        it,
                    )
                }
            }
            .map {
                log.info("Lagrer bestilling ${bestilling.bestillingsnummer}")
                queries.bestilling.insertBestilling(bestilling)
                bestilling
            }
            .onLeft {
                log.warn("Opprett bestilling ${bestilling.bestillingsnummer} feilet", it)
            }
    }

    suspend fun annullerBestilling(
        annullerBestilling: AnnullerBestilling,
    ): Either<AnnullerBestillingError, Bestilling> = db.session {
        val bestillingsnummer = annullerBestilling.bestillingsnummer
        val bestilling = queries.bestilling.getByBestillingsnummer(bestillingsnummer)
            ?: return AnnullerBestillingError("Bestilling $bestillingsnummer finnes ikke").left()

        if (bestilling.status == BestillingStatusType.ANNULLERT) {
            log.info("Bestilling $bestillingsnummer er allerede annullert")
            return bestilling.right()
        } else if (bestilling.status == BestillingStatusType.OPPGJORT) {
            return AnnullerBestillingError("Bestilling $bestillingsnummer kan ikke annulleres fordi den er oppgjort").left()
        } else if (queries.faktura.getByBestillingsnummer(bestillingsnummer).isNotEmpty()) {
            return AnnullerBestillingError("Bestilling $bestillingsnummer kan ikke annulleres fordi det finnes fakturaer for bestillingen").left()
        }

        val melding = toOebsAnnulleringMelding(bestilling, annullerBestilling)
        return oebs.sendAnnullering(melding)
            .mapLeft {
                AnnullerBestillingError("Klarte ikke annullere bestilling $bestillingsnummer hos oebs", it)
            }
            .map {
                log.info("Lagrer bestilling ${bestilling.bestillingsnummer} som annullert")
                queries.bestilling.setStatus(bestillingsnummer, BestillingStatusType.ANNULLERT)
                queries.bestilling.setAnnullering(
                    bestillingsnummer,
                    Bestilling.Totrinnskontroll(
                        behandletAv = annullerBestilling.behandletAv,
                        behandletTidspunkt = annullerBestilling.besluttetTidspunkt,
                        besluttetAv = annullerBestilling.besluttetAv,
                        besluttetTidspunkt = annullerBestilling.besluttetTidspunkt,
                    ),
                )
                checkNotNull(queries.bestilling.getByBestillingsnummer(bestillingsnummer))
            }
    }

    suspend fun opprettFaktura(
        opprettFaktura: OpprettFaktura,
    ): Either<OpprettFakturaError, Faktura> = db.session {
        val bestilling = queries.bestilling.getByBestillingsnummer(opprettFaktura.bestillingsnummer)
            ?: return OpprettFakturaError("Bestilling ${opprettFaktura.bestillingsnummer} finnes ikke").left()

        if (bestilling.status in listOf(BestillingStatusType.ANNULLERT, BestillingStatusType.OPPGJORT)) {
            return OpprettFakturaError("Faktura ${opprettFaktura.fakturanummer} kan ikke opprettes fordi bestilling ${opprettFaktura.bestillingsnummer} har status ${bestilling.status}").left()
        }

        val faktura = Faktura.fromOpprettFaktura(opprettFaktura, bestilling.linjer)

        val melding = toOebsFakturaMelding(bestilling, faktura, opprettFaktura.frigjorBestilling)
        return oebs.sendFaktura(melding)
            .mapLeft {
                OpprettFakturaError("Klarte ikke sende faktura ${faktura.fakturanummer} til oebs", it)
            }
            .map {
                log.info("Lagrer faktura ${faktura.fakturanummer}")
                queries.faktura.insertFaktura(faktura)

                if (opprettFaktura.frigjorBestilling) {
                    log.info("Setter bestilling ${bestilling.bestillingsnummer} til oppgjort")
                    queries.bestilling.setStatus(bestilling.bestillingsnummer, BestillingStatusType.OPPGJORT)
                }

                faktura
            }
    }
}

private fun getLeverandorAdresse(leverandor: BrregHovedenhetDto): Either<OpprettBestillingError, List<OebsBestillingMelding.Selger.Adresse>> {
    val adresse = leverandor.forretningsadresse?.let { toOebsAdresse(it) }.let { listOfNotNull(it) }

    return if (adresse.isNotEmpty()) {
        adresse.right()
    } else {
        OpprettBestillingError("Klarte ikke utlede adresse for leverandør ${leverandor.organisasjonsnummer.value}").left()
    }
}

private fun toOebsAdresse(it: BrregAdresse): OebsBestillingMelding.Selger.Adresse? {
    return OebsBestillingMelding.Selger.Adresse(
        gateNavn = it.adresse?.joinToString(separator = ", ") ?: return null,
        by = it.poststed ?: return null,
        postNummer = it.postnummer ?: return null,
        landsKode = it.landkode ?: return null,
    )
}

private fun toOebsBestillingMelding(
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

private fun toOebsAnnulleringMelding(
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

private fun toOebsFakturaMelding(
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
        valutaKode = "NOK",
        saksbehandler = faktura.behandletAv.part,
        bdmGodkjenner = faktura.besluttetAv.part,
        fakturaDato = faktura.besluttetTidspunkt.toLocalDate(),
        betalingsKanal = OebsBetalingskanal.BBAN,
        bankKontoNummer = faktura.kontonummer.value,
        kidNummer = faktura.kid?.value,
        bankNavn = null,
        bankLandKode = null,
        bicSwiftKode = null,
        // TODO: generer en beskrivende melding
        meldingTilLeverandor = null,
        beskrivelse = null,
        fakturaLinjer = linjer,
    )
}
