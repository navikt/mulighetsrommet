package no.nav.tiltak.okonomi.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.brreg.BrregAdresse
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregHovedenhetDto
import no.nav.mulighetsrommet.brreg.SlettetBrregHovedenhetDto
import no.nav.tiltak.okonomi.*
import no.nav.tiltak.okonomi.db.OkonomiDatabase
import no.nav.tiltak.okonomi.db.QueryContext
import no.nav.tiltak.okonomi.model.Bestilling
import no.nav.tiltak.okonomi.model.Faktura
import no.nav.tiltak.okonomi.model.OebsKontering
import no.nav.tiltak.okonomi.oebs.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OpprettBestillingError(message: String, cause: Throwable? = null) : Exception(message, cause)

class AnnullerBestillingError(message: String, cause: Throwable? = null) : Exception(message, cause)

class OpprettFakturaError(message: String, cause: Throwable? = null) : Exception(message, cause)

class FrigjorBestillingError(message: String, cause: Throwable? = null) : Exception(message, cause)

class OkonomiService(
    private val db: OkonomiDatabase,
    private val oebs: OebsPoApClient,
    private val brreg: BrregClient,
    private val topics: KafkaTopics,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    suspend fun opprettBestilling(
        opprettBestilling: OpprettBestilling,
    ): Either<OpprettBestillingError, Bestilling> = db.transaction {
        val bestillingsnummer = opprettBestilling.bestillingsnummer

        queries.bestilling.getByBestillingsnummer(bestillingsnummer)?.let {
            log.info("Bestilling $bestillingsnummer er allerede opprettet")
            return publishBestilling(bestillingsnummer).right()
        }

        val kontering = queries.kontering
            .getOebsKontering(
                tilskuddstype = opprettBestilling.tilskuddstype,
                tiltakskode = opprettBestilling.tiltakskode,
                periode = opprettBestilling.periode,
            )
            ?: return OpprettBestillingError("Kontering for bestilling $bestillingsnummer mangler").left()

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
                log.info("Sender bestilling $bestillingsnummer til oebs")
                oebs.sendBestilling(melding).mapLeft {
                    OpprettBestillingError("Klarte ikke sende bestilling $bestillingsnummer til oebs", it)
                }
            }
            .map {
                log.info("Lagrer bestilling $bestillingsnummer")
                queries.bestilling.insertBestilling(bestilling)
                publishBestilling(bestillingsnummer)
            }
            .onLeft {
                log.warn("Opprett bestilling $bestillingsnummer feilet", it)
            }
    }

    suspend fun annullerBestilling(
        annullerBestilling: AnnullerBestilling,
    ): Either<AnnullerBestillingError, Bestilling> = db.transaction {
        val bestillingsnummer = annullerBestilling.bestillingsnummer

        val bestilling = queries.bestilling.getByBestillingsnummer(bestillingsnummer)
            ?: return AnnullerBestillingError("Bestilling $bestillingsnummer finnes ikke").left()

        if (bestilling.status == BestillingStatusType.ANNULLERT) {
            log.info("Bestilling $bestillingsnummer er allerede annullert")
            return publishBestilling(bestillingsnummer).right()
        } else if (bestilling.status == BestillingStatusType.FRIGJORT) {
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
                publishBestilling(bestillingsnummer)
            }
    }

    suspend fun opprettFaktura(
        opprettFaktura: OpprettFaktura,
    ): Either<OpprettFakturaError, Faktura> = db.transaction {
        val fakturanummer = opprettFaktura.fakturanummer

        queries.faktura.getByFakturanummer(fakturanummer)?.let {
            log.info("Faktura $fakturanummer er allerede opprettet")
            return publishFaktura(fakturanummer).right()
        }

        val bestillingsnummer = opprettFaktura.bestillingsnummer

        val bestilling = queries.bestilling.getByBestillingsnummer(bestillingsnummer)
            ?: return OpprettFakturaError("Bestilling $bestillingsnummer finnes ikke").left()

        if (bestilling.status in listOf(BestillingStatusType.ANNULLERT, BestillingStatusType.FRIGJORT)) {
            return OpprettFakturaError("Faktura $fakturanummer kan ikke opprettes fordi bestilling $bestillingsnummer har status ${bestilling.status}").left()
        }

        val faktura = Faktura.fromOpprettFaktura(opprettFaktura, bestilling.linjer)

        val melding = toOebsFakturaMelding(bestilling, faktura, erSisteFaktura = opprettFaktura.frigjorBestilling)
        return oebs.sendFaktura(melding)
            .mapLeft {
                OpprettFakturaError("Klarte ikke sende faktura $fakturanummer til oebs", it)
            }
            .map {
                log.info("Lagrer faktura $fakturanummer")
                queries.faktura.insertFaktura(faktura)

                if (opprettFaktura.frigjorBestilling) {
                    setBestillingFrigjort(bestillingsnummer)
                }

                publishFaktura(fakturanummer)
            }
    }

    /**
     * Siden OeBS ikke har noen frigjørings funksjonalitet er dette implementert som en faktura med erSisteFaktura = true
     */
    suspend fun frigjorBestilling(
        frigjorBestilling: FrigjorBestilling,
    ): Either<FrigjorBestillingError, Faktura> = db.session {
        val bestillingsnummer = frigjorBestilling.bestillingsnummer

        val bestilling = queries.bestilling.getByBestillingsnummer(bestillingsnummer)
            ?: return FrigjorBestillingError("Bestilling $bestillingsnummer finnes ikke").left()

        queries.faktura.getByFakturanummer(frigjorFakturanummer(bestillingsnummer))?.let {
            log.info("Bestilling $bestillingsnummer er allerede frigjort")
            return it.right()
        }

        // TODO: Fjern sjekk mot SENDT status når bestillinger blir satt som aktive
        if (bestilling.status !in listOf(BestillingStatusType.SENDT, BestillingStatusType.AKTIV)) {
            return FrigjorBestillingError("Bestilling $bestillingsnummer kan ikke frigjøres fordi den har status ${bestilling.status}").left()
        }

        val faktura = Faktura.fromFrigjorBestilling(frigjorBestilling, bestilling)

        val melding = toOebsFakturaMelding(bestilling, faktura, erSisteFaktura = true)
        return oebs.sendFaktura(melding)
            .mapLeft {
                FrigjorBestillingError("Klarte ikke sende faktura ${faktura.fakturanummer} til oebs", it)
            }
            .map {
                log.info("Lagrer frigjøringsfaktura ${faktura.fakturanummer}")
                queries.faktura.insertFaktura(faktura)

                setBestillingFrigjort(bestillingsnummer)

                faktura
            }
    }

    private fun QueryContext.setBestillingFrigjort(bestillingsnummer: String) {
        log.info("Setter bestilling $bestillingsnummer til frigjort")
        queries.bestilling.setStatus(bestillingsnummer, BestillingStatusType.FRIGJORT)
        publishBestilling(bestillingsnummer)
    }

    private fun QueryContext.publishBestilling(bestillingsnummer: String): Bestilling {
        val bestilling = checkNotNull(queries.bestilling.getByBestillingsnummer(bestillingsnummer))

        log.info("Lagrer status-melding for bestilling $bestillingsnummer")
        queries.kafkaProducerRecord.insertBestillingStatus(
            topics.bestillingStatus,
            BestillingStatus(
                bestillingsnummer = bestilling.bestillingsnummer,
                status = bestilling.status,
            ),
        )

        return bestilling
    }

    private fun QueryContext.publishFaktura(fakturanummer: String): Faktura {
        val faktura = checkNotNull(queries.faktura.getByFakturanummer(fakturanummer))

        log.info("Lagrer status-melding for faktura $fakturanummer")
        queries.kafkaProducerRecord.insertFakturaStatus(
            topics.fakturaStatus,
            FakturaStatus(
                fakturanummer = fakturanummer,
                status = faktura.status,
            ),
        )

        return faktura
    }
}

fun frigjorFakturanummer(bestillingsnummer: String): String = "$bestillingsnummer-X"

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
        bankKontoNummer = faktura.kontonummer?.value,
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
