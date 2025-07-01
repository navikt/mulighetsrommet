package no.nav.tiltak.okonomi.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.brreg.*
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.tiltak.okonomi.*
import no.nav.tiltak.okonomi.api.OebsBestillingKvittering
import no.nav.tiltak.okonomi.api.OebsFakturaKvittering
import no.nav.tiltak.okonomi.db.OkonomiDatabase
import no.nav.tiltak.okonomi.db.QueryContext
import no.nav.tiltak.okonomi.model.Bestilling
import no.nav.tiltak.okonomi.model.Faktura
import no.nav.tiltak.okonomi.oebs.OebsBestillingMelding
import no.nav.tiltak.okonomi.oebs.OebsMeldingMapper
import no.nav.tiltak.okonomi.oebs.OebsPoApClient
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

        return getHovedenhet(opprettBestilling.arrangor)
            .flatMap { hovedenhet ->
                getLeverandorAdresse(hovedenhet).map { adresse ->
                    OebsBestillingMelding.Selger(
                        organisasjonsNummer = hovedenhet.organisasjonsnummer.value,
                        organisasjonsNavn = hovedenhet.navn,
                        adresse = adresse,
                        bedriftsNummer = opprettBestilling.arrangor.value,
                    )
                }
            }
            .flatMap { selger ->
                val bestilling = Bestilling.fromOpprettBestilling(
                    bestilling = opprettBestilling,
                    arrangorHovedenhet = Organisasjonsnummer(selger.organisasjonsNummer),
                )
                val melding = OebsMeldingMapper.toOebsBestillingMelding(bestilling, kontering, selger)
                log.info("Sender bestilling $bestillingsnummer til oebs")
                oebs.sendBestilling(melding)
                    .mapLeft {
                        OpprettBestillingError("Klarte ikke sende bestilling $bestillingsnummer til oebs", it)
                    }
                    .map { bestilling }
            }
            .map { bestilling ->
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
        val fakturaer = queries.faktura.getByBestillingsnummer(bestillingsnummer)

        if (bestilling.status in listOf(BestillingStatusType.ANNULLERT, BestillingStatusType.ANNULLERING_SENDT)) {
            log.info("Bestilling $bestillingsnummer er allerede annullert")
            return publishBestilling(bestillingsnummer).right()
        }
        if (fakturaer.isNotEmpty()) {
            return AnnullerBestillingError("Bestilling $bestillingsnummer kan ikke annulleres fordi det finnes fakturaer for bestillingen").left()
        }
        if (venterPaaKvittering(bestilling, fakturaer)) {
            return AnnullerBestillingError("Bestilling $bestillingsnummer kan ikke annulleres fordi vi venter på kvittering").left()
        }
        if (bestilling.status != BestillingStatusType.AKTIV) {
            return AnnullerBestillingError("Bestilling $bestillingsnummer kan ikke annulleres fordi den har status: ${bestilling.status}").left()
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
        val fakturaer = queries.faktura.getByBestillingsnummer(bestillingsnummer)

        if (venterPaaKvittering(bestilling, fakturaer)) {
            return OpprettFakturaError("Faktura $fakturanummer kan ikke opprettes fordi vi venter på kvittering").left()
        }
        if (bestilling.status != BestillingStatusType.AKTIV) {
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

        val fakturaer = queries.faktura.getByBestillingsnummer(bestillingsnummer)
        fakturaer.find { it.fakturanummer == gjorOppFakturanummer(bestillingsnummer) }?.let {
            log.info("Bestilling $bestillingsnummer er allerede oppgjort")
            return it.right()
        }

        if (venterPaaKvittering(bestilling, fakturaer)) {
            return GjorOppBestillingError("Bestilling $bestillingsnummer kan ikke gjøres opp fordi vi venter på kvittering").left()
        }
        if (bestilling.status != BestillingStatusType.AKTIV) {
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
        kvittering: OebsBestillingKvittering,
    ) = db.transaction {
        if (kvittering.isSuccess()) {
            if (kvittering.isAnnulleringKvittering()) {
                queries.bestilling.setStatus(bestilling.bestillingsnummer, BestillingStatusType.ANNULLERT)
            } else {
                queries.bestilling.setStatus(bestilling.bestillingsnummer, BestillingStatusType.AKTIV)
            }
            queries.bestilling.setFeilmelding(
                bestilling.bestillingsnummer,
                feilKode = null,
                feilMelding = null,
            )
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
            queries.faktura.setStatus(
                faktura.fakturanummer,
                requireNotNull(kvittering.statusBetalt?.toFakturaStatusType()) {
                    "Manglet statusBetalt på suksess faktura kvittering: ${faktura.fakturanummer}"
                },
            )
            queries.faktura.setFeilmelding(
                faktura.fakturanummer,
                feilKode = null,
                feilMelding = null,
            )
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

    private suspend fun getHovedenhet(organisasjonsnummer: Organisasjonsnummer): Either<OpprettBestillingError, BrregHovedenhetDto> {
        return brreg.getBrregEnhet(organisasjonsnummer)
            .mapLeft { error ->
                OpprettBestillingError("Klarte ikke utlede hovedenhet for $organisasjonsnummer fra Brreg: $error")
            }
            .flatMap { enhet ->
                when (enhet) {
                    is BrregHovedenhetDto -> enhet.overordnetEnhet?.let { getHovedenhet(it) } ?: enhet.right()
                    is BrregUnderenhetDto -> getHovedenhet(enhet.overordnetEnhet)
                    is SlettetBrregHovedenhetDto -> OpprettBestillingError("Hovedenhet med orgnr ${organisasjonsnummer.value} er slettet").left()
                    is SlettetBrregUnderenhetDto -> OpprettBestillingError("Underenhet med orgnr ${enhet.organisasjonsnummer.value} er slettet").left()
                }
            }
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
                        fakturaStatusSistOppdatert = faktura.fakturaStatusSistOppdatert,
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
    return when (it.landkode) {
        "NO" -> OebsBestillingMelding.Selger.Adresse(
            gateNavn = it.adresse?.joinToString(separator = ", ") ?: return null,
            by = it.poststed ?: return null,
            postNummer = it.postnummer ?: return null,
            landsKode = it.landkode ?: return null,
        )

        null -> null
        else -> OebsBestillingMelding.Selger.Adresse(
            gateNavn = it.adresse?.joinToString(separator = ", ") ?: return null,
            by = it.poststed ?: return null,
            postNummer = it.postnummer,
            landsKode = it.landkode ?: return null,
        )
    }
}

private fun venterPaaKvittering(bestilling: Bestilling, fakturaer: List<Faktura>): Boolean {
    when (bestilling.status) {
        BestillingStatusType.SENDT,
        BestillingStatusType.ANNULLERING_SENDT,
        -> return true

        BestillingStatusType.AKTIV,
        BestillingStatusType.ANNULLERT,
        BestillingStatusType.OPPGJORT,
        BestillingStatusType.FEILET,
        -> Unit
    }

    return fakturaer.any { it.status == FakturaStatusType.SENDT }
}
