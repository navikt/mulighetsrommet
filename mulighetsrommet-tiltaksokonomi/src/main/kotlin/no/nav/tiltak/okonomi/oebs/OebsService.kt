package no.nav.tiltak.okonomi.oebs

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregHovedenhetDto
import no.nav.mulighetsrommet.brreg.SlettetBrregHovedenhetDto
import no.nav.tiltak.okonomi.OpprettBestilling
import no.nav.tiltak.okonomi.OpprettFaktura
import no.nav.tiltak.okonomi.db.OkonomiDatabase
import no.nav.tiltak.okonomi.model.Bestilling
import no.nav.tiltak.okonomi.model.BestillingStatusType
import no.nav.tiltak.okonomi.model.Faktura
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OpprettBestillingError(message: String, cause: Throwable? = null) : Exception(message, cause)

class AnnullerBestillingError(message: String, cause: Throwable? = null) : Exception(message, cause)

class OpprettFakturaError(message: String, cause: Throwable? = null) : Exception(message, cause)

class OebsService(
    private val db: OkonomiDatabase,
    private val oebs: OebsTiltakApiClient,
    private val brreg: BrregClient,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    suspend fun opprettBestilling(
        opprettBestilling: OpprettBestilling,
    ): Either<OpprettBestillingError, Bestilling> = db.session {
        queries.bestilling.getBestilling(opprettBestilling.bestillingsnummer)?.let {
            log.info("Bestilling ${opprettBestilling.bestillingsnummer} er allerede opprettet")
            return it.right()
        }

        val bestilling = Bestilling.fromOpprettBestilling(opprettBestilling, BestillingStatusType.AKTIV)

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
            .map { hovedenhet ->
                val linjer = bestilling.linjer.map { linje ->
                    OebsBestillingMelding.Linje(
                        linjeNummer = linje.linjenummer,
                        antall = linje.belop,
                        periode = linje.periode.start.monthValue,
                        startDato = linje.periode.start,
                        sluttDato = linje.periode.getLastDate(),
                    )
                }
                toOebsBestillingMelding(bestilling, hovedenhet, linjer)
            }
            .flatMap { melding ->
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
                queries.bestilling.createBestilling(bestilling)
                bestilling
            }
            .onLeft {
                log.warn("Klarte ikke sende bestilling ${bestilling.bestillingsnummer} til oebs", it)
            }
    }

    suspend fun annullerBestilling(
        bestillingsnummer: String,
    ): Either<AnnullerBestillingError, Bestilling> = db.session {
        val bestilling = queries.bestilling.getBestilling(bestillingsnummer)
            ?: return AnnullerBestillingError("Bestilling $bestillingsnummer finnes ikke").left()

        if (bestilling.status == BestillingStatusType.ANNULLERT) {
            return bestilling.right()
        } else if (bestilling.status != BestillingStatusType.AKTIV) {
            return AnnullerBestillingError("Kan ikke annullere bestilling $bestillingsnummer med status ${bestilling.status}").left()
        }

        val melding = OebsAnnulleringMelding(
            bestillingsNummer = bestillingsnummer,
            bestillingsType = OebsBestillingType.ANNULLER,
            selger = OebsAnnulleringMelding.Selger(
                organisasjonsNummer = bestilling.arrangorHovedenhet.value,
                bedriftsNummer = bestilling.arrangorUnderenhet.value,
            ),
        )

        return oebs.sendAnnullering(melding)
            .mapLeft {
                AnnullerBestillingError("Klarte ikke annullere bestilling $bestillingsnummer hos oebs", it)
            }
            .map {
                queries.bestilling.setStatus(bestillingsnummer, BestillingStatusType.ANNULLERT)
                checkNotNull(queries.bestilling.getBestilling(bestillingsnummer))
            }
    }

    suspend fun opprettFaktura(
        opprettFaktura: OpprettFaktura,
    ): Either<OpprettFakturaError, Faktura> = db.session {
        val bestilling = queries.bestilling.getBestilling(opprettFaktura.bestillingsnummer)
            ?: return OpprettFakturaError("Bestilling ${opprettFaktura.bestillingsnummer} mangler for faktura ${opprettFaktura.fakturanummer}").left()

        val faktura = Faktura.fromOpprettFaktura(opprettFaktura, bestilling.linjer)

        val melding = toOebsFakturaMelding(bestilling, faktura)

        return oebs.sendFaktura(melding)
            .mapLeft {
                OpprettFakturaError("Klarte ikke sende faktura ${faktura.fakturanummer} til oebs", it)
            }
            .map {
                queries.faktura.opprettFaktura(faktura)
                faktura
            }
    }
}

private fun toOebsBestillingMelding(
    bestilling: Bestilling,
    arrangorHovedenhet: BrregHovedenhetDto,
    linjer: List<OebsBestillingMelding.Linje>,
): OebsBestillingMelding {
    val selger = OebsBestillingMelding.Selger(
        organisasjonsNummer = arrangorHovedenhet.organisasjonsnummer.value,
        organisasjonsNavn = arrangorHovedenhet.navn,
        postAdresse = listOfNotNull(
            arrangorHovedenhet.postadresse?.let { adresse ->
                // TODO: hvordan håndtere manglende adresse? Feile, sette empty strings, eller sende null?
                OebsBestillingMelding.Selger.PostAdresse(
                    gateNavn = adresse.adresse?.joinToString(separator = ", ") ?: "",
                    by = adresse.poststed ?: "",
                    postNummer = adresse.postnummer ?: "",
                    landsKode = adresse.landkode ?: "",
                )
            },
        ),
        bedriftsNummer = bestilling.arrangorUnderenhet.value,
    )

    return OebsBestillingMelding(
        kilde = OebsKilde.TILTADM,
        bestillingsNummer = bestilling.bestillingsnummer,
        opprettelsesTidspunkt = bestilling.opprettetTidspunkt,
        bestillingsType = OebsBestillingType.NY,
        selger = selger,
        rammeavtaleNummer = bestilling.avtalenummer,
        totalSum = bestilling.belop,
        valutaKode = "NOK",
        saksbehandler = bestilling.opprettetAv.part,
        bdmGodkjenner = bestilling.opprettetAv.part,
        startDato = bestilling.periode.start,
        sluttDato = bestilling.periode.getLastDate(),
        bestillingsLinjer = linjer,
        statsregnskapsKonto = OebsKontering.TILTAK.statsregnskapskonto,
        artsKonto = OebsKonteringInfo.getArtskonto(bestilling.tiltakskode),
        kontor = bestilling.kostnadssted.value,
        tilsagnsAar = bestilling.periode.start.year,
    )
}

private fun toOebsFakturaMelding(
    bestilling: Bestilling,
    faktura: Faktura,
): OebsFakturaMelding {
    return OebsFakturaMelding(
        kilde = OebsKilde.TILTADM,
        fakturaNummer = faktura.fakturanummer,
        opprettelsesTidspunkt = faktura.opprettetTidspunkt,
        organisasjonsNummer = bestilling.arrangorHovedenhet.value,
        bedriftsNummer = bestilling.arrangorUnderenhet.value,
        totalSum = faktura.belop,
        valutaKode = "NOK",
        saksbehandler = faktura.opprettetAv.part,
        bdmGodkjenner = faktura.opprettetAv.part,
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
        fakturaLinjer = faktura.linjer.map {
            OebsFakturaMelding.Linje(
                bestillingsnummer = bestilling.bestillingsnummer,
                bestillingsLinjeNummer = it.linjenummer,
                antall = it.belop,
                // TODO: støtte i opprett bestilling, eller i egen handling? Burde nok lagres direkte på faktura.
                erSisteFaktura = false,
            )
        },
    )
}
