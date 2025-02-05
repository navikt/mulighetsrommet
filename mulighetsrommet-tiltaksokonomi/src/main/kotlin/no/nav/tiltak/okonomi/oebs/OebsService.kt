package no.nav.tiltak.okonomi.oebs

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregHovedenhetDto
import no.nav.mulighetsrommet.brreg.SlettetBrregHovedenhetDto
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.tiltak.okonomi.api.OpprettBestilling
import no.nav.tiltak.okonomi.api.OpprettFaktura
import no.nav.tiltak.okonomi.db.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.collections.set
import kotlin.math.floor

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
        bestilling: OpprettBestilling,
    ): Either<OpprettBestillingError, Bestilling> = db.session {
        queries.bestilling.getBestilling(bestilling.bestillingsnummer)?.let {
            log.info("Bestilling ${bestilling.bestillingsnummer} er allerede opprettet")
            return it.right()
        }

        val perioder = divideBelopByMonthsInPeriode(bestilling.periode, bestilling.belop)
        val dbo = Bestilling(
            tiltakskode = bestilling.tiltakskode,
            arrangorHovedenhet = bestilling.arrangor.hovedenhet,
            arrangorUnderenhet = bestilling.arrangor.hovedenhet,
            kostnadssted = bestilling.kostnadssted,
            bestillingsnummer = bestilling.bestillingsnummer,
            avtalenummer = bestilling.avtalenummer,
            belop = bestilling.belop,
            periode = bestilling.periode,
            status = BestillingStatusType.AKTIV,
            opprettetAv = bestilling.opprettetAv,
            opprettetTidspunkt = bestilling.opprettetTidspunkt,
            besluttetAv = bestilling.besluttetAv,
            besluttetTidspunkt = bestilling.besluttetTidspunkt,
            linjer = perioder.mapIndexed { index, (periode, belop) ->
                LinjeDbo(
                    linjenummer = (index + 1),
                    periode = periode,
                    belop = belop,
                )
            },
        )

        return brreg.getHovedenhet(bestilling.arrangor.hovedenhet)
            .mapLeft { OpprettBestillingError("Klarte ikke hente hovedenhet ${bestilling.arrangor.hovedenhet} fra Brreg: $it") }
            .flatMap {
                when (it) {
                    is BrregHovedenhetDto -> it.right()

                    is SlettetBrregHovedenhetDto -> {
                        OpprettBestillingError("Hovedenhet med orgnr ${bestilling.arrangor.hovedenhet} er slettet").left()
                    }
                }
            }
            .map { hovedenhet ->
                val linjer = dbo.linjer.map { linje ->
                    OebsBestillingMelding.Linje(
                        linjeNummer = linje.linjenummer,
                        antall = linje.belop,
                        periode = linje.periode.start.monthValue,
                        startDato = linje.periode.start,
                        sluttDato = linje.periode.getLastDate(),
                    )
                }
                toOebsBestillingMelding(dbo, hovedenhet, linjer)
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
                queries.bestilling.createBestilling(dbo)
                dbo
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
        faktura: OpprettFaktura,
    ): Either<OpprettFakturaError, Faktura> = db.session {
        val bestilling = queries.bestilling.getBestilling(faktura.bestillingsnummer)
            ?: return OpprettFakturaError("Bestilling ${faktura.bestillingsnummer} mangler for faktura ${faktura.fakturanummer}").left()

        val bestillingLinjerByMonth = bestilling.linjer.associateBy { it.periode.start.month }
        val perioder = divideBelopByMonthsInPeriode(faktura.periode, faktura.belop)
        val dbo = Faktura(
            bestillingsnummer = faktura.bestillingsnummer,
            fakturanummer = faktura.fakturanummer,
            kontonummer = faktura.betalingsinformasjon.kontonummer,
            kid = faktura.betalingsinformasjon.kid,
            belop = faktura.belop,
            periode = faktura.periode,
            status = FakturaStatusType.UTBETALT,
            opprettetAv = faktura.opprettetAv,
            opprettetTidspunkt = faktura.opprettetTidspunkt,
            besluttetAv = faktura.opprettetAv,
            besluttetTidspunkt = faktura.opprettetTidspunkt,
            linjer = perioder.map { (periode, belop) ->
                val bestillingLinje = bestillingLinjerByMonth.getValue(periode.start.month)
                LinjeDbo(
                    linjenummer = bestillingLinje.linjenummer,
                    periode = periode,
                    belop = belop,
                )
            },
        )

        val melding = toOebsFakturaMelding(bestilling, dbo)

        return oebs.sendFaktura(melding)
            .mapLeft {
                OpprettFakturaError("Klarte ikke sende faktura ${faktura.fakturanummer} til oebs", it)
            }
            .map {
                queries.faktura.opprettFaktura(dbo)
                dbo
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
        kilde = Kilde.TILTADM,
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
        artsKonto = getOebsArtskonto(bestilling.tiltakskode),
        kontor = bestilling.kostnadssted.value,
        tilsagnsAar = bestilling.periode.start.year,
    )
}

private fun toOebsFakturaMelding(
    bestilling: Bestilling,
    faktura: Faktura,
): OebsFakturaMelding {
    return OebsFakturaMelding(
        kilde = Kilde.TILTADM,
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

fun divideBelopByMonthsInPeriode(bestillingsperiode: Periode, belop: Int): List<Pair<Periode, Int>> {
    val monthlyPeriods = bestillingsperiode.splitByMonth()

    val belopPerDay = belop.toDouble() / bestillingsperiode.getDurationInDays()

    val belopByMonth = monthlyPeriods
        .associateWith { floor(belopPerDay * it.getDurationInDays()).toInt() }
        .toSortedMap()

    val remainder = belop - belopByMonth.values.sum()
    if (remainder > 0) {
        val firstPeriod = monthlyPeriods.first()
        belopByMonth[firstPeriod] = belopByMonth.getValue(firstPeriod) + remainder
    }

    return belopByMonth.toList()
}

private fun getOebsArtskonto(tiltakskode: Tiltakskode): String {
    val kontering = when (tiltakskode) {
        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING -> OebsKonteringInfo.ARBEIDSFORBEREDENDE_TRENING
        Tiltakskode.ARBEIDSRETTET_REHABILITERING -> OebsKonteringInfo.ARBEIDSRETTET_REHABILITERING
        Tiltakskode.AVKLARING -> OebsKonteringInfo.AVKLARING
        Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK -> OebsKonteringInfo.DIGITALT_OPPFOLGINGSTILTAK
        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING -> OebsKonteringInfo.GRUPPE_ARBEIDSMARKEDSOPPLAERING
        Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING -> OebsKonteringInfo.GRUPPE_FAG_OG_YRKESOPPLAERING
        Tiltakskode.JOBBKLUBB -> OebsKonteringInfo.JOBBKLUBB
        Tiltakskode.OPPFOLGING -> OebsKonteringInfo.OPPFOLGING
        Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET -> OebsKonteringInfo.VARIG_TILRETTELAGT_ARBEID_SKJERMET
    }

    return kontering.artskonto
}
