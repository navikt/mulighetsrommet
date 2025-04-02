package no.nav.tiltak.okonomi.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.brreg.BrregAdresse
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregHovedenhetDto
import no.nav.mulighetsrommet.brreg.SlettetBrregHovedenhetDto
import no.nav.tiltak.okonomi.*
import no.nav.tiltak.okonomi.db.OkonomiDatabase
import no.nav.tiltak.okonomi.db.QueryContext
import no.nav.tiltak.okonomi.model.Bestilling
import no.nav.tiltak.okonomi.model.Faktura
import no.nav.tiltak.okonomi.oebs.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OpprettBestillingError(message: String, cause: Throwable? = null) : Exception(message, cause)

class AnnullerBestillingError(message: String, cause: Throwable? = null) : Exception(message, cause)

class OpprettFakturaError(message: String, cause: Throwable? = null) : Exception(message, cause)

class GjorOppBestillingError(message: String, cause: Throwable? = null) : Exception(message, cause)

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
                val melding = OebsMeldingMapper.toOebsBestillingMelding(bestilling, kontering, selger)
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

        if (bestilling.status in listOf(BestillingStatusType.ANNULLERT, BestillingStatusType.ANNULLERING_SENDT)) {
            log.info("Bestilling $bestillingsnummer er allerede annullert")
            return publishBestilling(bestillingsnummer).right()
        } else if (bestilling.status !in listOf(BestillingStatusType.SENDT, BestillingStatusType.AKTIV)) {
            // TODO: Fjern SENDT som valid status her når kvitteringer skal være mottatt
            return AnnullerBestillingError("Bestilling $bestillingsnummer kan ikke annulleres fordi den har status: ${bestilling.status}").left()
        } else if (queries.faktura.getByBestillingsnummer(bestillingsnummer).isNotEmpty()) {
            return AnnullerBestillingError("Bestilling $bestillingsnummer kan ikke annulleres fordi det finnes fakturaer for bestillingen").left()
        }

        val melding = OebsMeldingMapper.toOebsAnnulleringMelding(bestilling, annullerBestilling)
        return oebs.sendAnnullering(melding)
            .mapLeft {
                AnnullerBestillingError("Klarte ikke annullere bestilling $bestillingsnummer hos oebs", it)
            }
            .map {
                log.info("Lagrer bestilling ${bestilling.bestillingsnummer} som annullert")
                queries.bestilling.setStatus(bestillingsnummer, BestillingStatusType.ANNULLERING_SENDT)
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

        // TODO: Fjern SENDT som valid status her når kvitteringer skal være mottatt
        if (bestilling.status !in listOf(BestillingStatusType.SENDT, BestillingStatusType.AKTIV)) {
            return OpprettFakturaError("Faktura $fakturanummer kan ikke opprettes fordi bestilling $bestillingsnummer har status ${bestilling.status}").left()
        }

        val faktura = Faktura.fromOpprettFaktura(opprettFaktura, bestilling.linjer)

        val melding = OebsMeldingMapper.toOebsFakturaMelding(
            bestilling,
            faktura,
            erSisteFaktura = opprettFaktura.gjorOppBestilling,
        )
        return oebs.sendFaktura(melding)
            .mapLeft {
                OpprettFakturaError("Klarte ikke sende faktura $fakturanummer til oebs", it)
            }
            .map {
                log.info("Lagrer faktura $fakturanummer")
                queries.faktura.insertFaktura(faktura)

                if (opprettFaktura.gjorOppBestilling) {
                    setBestillingOppgjort(bestillingsnummer)
                }

                publishFaktura(fakturanummer)
            }
    }

    /**
     * Siden OeBS ikke har noen egen funksjon for å gjøre opp et tilsagn er dette implementert som en faktura med erSisteFaktura = true
     */
    suspend fun gjorOppBestilling(
        gjorOppBestilling: GjorOppBestilling,
    ): Either<GjorOppBestillingError, Faktura> = db.session {
        val bestillingsnummer = gjorOppBestilling.bestillingsnummer

        val bestilling = queries.bestilling.getByBestillingsnummer(bestillingsnummer)
            ?: return GjorOppBestillingError("Bestilling $bestillingsnummer finnes ikke").left()

        queries.faktura.getByFakturanummer(gjorOppFakturanummer(bestillingsnummer))?.let {
            log.info("Bestilling $bestillingsnummer er allerede oppgjort")
            return it.right()
        }

        // TODO: Fjern SENDT som valid status her når kvitteringer skal være mottatt
        if (bestilling.status !in listOf(BestillingStatusType.SENDT, BestillingStatusType.AKTIV)) {
            return GjorOppBestillingError("Bestilling $bestillingsnummer kan ikke gjøres opp fordi den har status ${bestilling.status}").left()
        }

        val faktura = Faktura.fromGjorOppBestilling(gjorOppBestilling, bestilling)

        val melding = OebsMeldingMapper.toOebsFakturaMelding(bestilling, faktura, erSisteFaktura = true)
        return oebs.sendFaktura(melding)
            .mapLeft {
                GjorOppBestillingError("Klarte ikke sende faktura ${faktura.fakturanummer} til oebs", it)
            }
            .map {
                log.info("Lagrer oppgjort-faktura ${faktura.fakturanummer}")
                queries.faktura.insertFaktura(faktura)

                setBestillingOppgjort(bestillingsnummer)

                faktura
            }
    }

    fun hentBestilling(bestillingsnummer: String): Bestilling? = db.session {
        queries.bestilling.getByBestillingsnummer(bestillingsnummer)
    }

    fun hentFaktura(fakturaNummer: String): Faktura? = db.session {
        queries.faktura.getByFakturanummer(fakturaNummer)
    }

    fun mottaBestillingKvittering(
        bestilling: Bestilling,
        kvittering: OebsOpprettBestillingKvittering,
    ) = db.transaction {
        if (kvittering.isSuccess()) {
            queries.bestilling.setStatus(bestilling.bestillingsnummer, BestillingStatusType.AKTIV)
        } else {
            queries.bestilling.setStatus(bestilling.bestillingsnummer, BestillingStatusType.FEILET)
            queries.bestilling.setFeilmelding(
                bestilling.bestillingsnummer,
                feilKode = kvittering.feilKode,
                feilMelding = kvittering.feilMelding,
            )
        }

        publishBestilling(bestilling.bestillingsnummer)
    }

    fun mottaAnnullerBestillingKvittering(
        bestilling: Bestilling,
        kvittering: OebsAnnullerBestillingKvittering,
    ) = db.transaction {
        if (kvittering.isSuccess()) {
            queries.bestilling.setStatus(bestilling.bestillingsnummer, BestillingStatusType.ANNULLERT)
        } else {
            queries.bestilling.setStatus(bestilling.bestillingsnummer, BestillingStatusType.FEILET)
            queries.bestilling.setFeilmelding(
                bestilling.bestillingsnummer,
                feilKode = kvittering.feilKode,
                feilMelding = kvittering.feilMelding,
            )
        }

        publishBestilling(bestilling.bestillingsnummer)
    }

    fun mottaFakturaKvittering(
        faktura: Faktura,
        kvittering: OebsFakturaKvittering,
    ) = db.transaction {
        if (kvittering.isSuccess()) {
            queries.faktura.setStatus(faktura.fakturanummer, FakturaStatusType.UTBETALT)
        } else {
            queries.faktura.setStatus(faktura.fakturanummer, FakturaStatusType.FEILET)
            queries.faktura.setFeilmelding(
                faktura.fakturanummer,
                feilKode = kvittering.feilKode,
                feilMelding = kvittering.feilMelding,
            )
        }

        publishFaktura(faktura.fakturanummer).right()
    }

    fun logKvittering(kvitteringJson: String) = db.session {
        queries.kvittering.insert(kvitteringJson)
    }

    private fun QueryContext.setBestillingOppgjort(bestillingsnummer: String) {
        log.info("Setter bestilling $bestillingsnummer til oppgjort")
        queries.bestilling.setStatus(bestillingsnummer, BestillingStatusType.OPPGJORT)
        publishBestilling(bestillingsnummer)
    }

    private fun QueryContext.publishBestilling(bestillingsnummer: String): Bestilling {
        val bestilling = checkNotNull(queries.bestilling.getByBestillingsnummer(bestillingsnummer))

        log.info("Lagrer status-melding for bestilling $bestillingsnummer")
        queries.kafkaProducerRecord.storeRecord(
            StoredProducerRecord(
                topics.bestillingStatus,
                bestillingsnummer.toByteArray(),
                Json.encodeToString(
                    BestillingStatus(
                        bestillingsnummer = bestilling.bestillingsnummer,
                        status = bestilling.status,
                    ),
                ).toByteArray(),
                null,
            ),
        )

        return bestilling
    }

    private fun QueryContext.publishFaktura(fakturanummer: String): Faktura {
        val faktura = checkNotNull(queries.faktura.getByFakturanummer(fakturanummer))

        log.info("Lagrer status-melding for faktura $fakturanummer")
        queries.kafkaProducerRecord.storeRecord(
            StoredProducerRecord(
                topics.fakturaStatus,
                fakturanummer.toByteArray(),
                Json.encodeToString(
                    FakturaStatus(
                        fakturanummer = fakturanummer,
                        status = faktura.status,
                    ),
                ).toByteArray(),
                null,
            ),
        )

        return faktura
    }
}

fun gjorOppFakturanummer(bestillingsnummer: String): String = "$bestillingsnummer-X"

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
