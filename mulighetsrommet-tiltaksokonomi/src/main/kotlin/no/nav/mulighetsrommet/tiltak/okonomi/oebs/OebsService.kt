package no.nav.mulighetsrommet.tiltak.okonomi.oebs

import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregHovedenhetDto
import no.nav.mulighetsrommet.brreg.SlettetBrregHovedenhetDto
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.tiltak.okonomi.oebs.OkonomiPart.NavAnsatt
import no.nav.mulighetsrommet.tiltak.okonomi.oebs.OkonomiPart.Tiltaksadministrasjon
import kotlin.math.floor

class OpprettOebsBestillingError(message: String) : Exception(message)

class OebsService(
    private val oebs: OebsTiltakApiClient,
    private val brreg: BrregClient,
) {
    suspend fun behandleBestilling(bestilling: OpprettOebsBestilling): OebsBestilling {
        brreg.getHovedenhet(bestilling.arrangor.hovedenhet)
            .flatMap {
                when (it) {
                    is BrregHovedenhetDto -> it.right()

                    is SlettetBrregHovedenhetDto -> {
                        OpprettOebsBestillingError("Hovedenhet med orgnr ${bestilling.arrangor.hovedenhet} er slettet").left()
                    }
                }
            }
            .map { toOebsBestillingMelding(bestilling, it) }
            .flatMap { oebs.sendBestilling(it) }
            .onLeft { throw Exception("Klarte ikke behandle bestilling ${bestilling.bestillingsnummer}: $it") }

        return OebsBestilling(
            bestillingsnummer = bestilling.bestillingsnummer,
            status = OebsBestilling.Status.BEHANDLET,
        )
    }

    suspend fun behandleAnnullering(annullering: AnnullerOebsBestilling) {
        val melding = OebsAnnulleringMelding(
            bestillingsNummer = annullering.bestillingsnummer,
            bestillingsType = OebsBestillingType.ANNULLER,
            selger = OebsAnnulleringMelding.Selger(
                organisasjonsNummer = annullering.arrangor.hovedenhet.value,
                bedriftsNummer = annullering.arrangor.underenhet.value,
            ),
        )

        oebs.sendAnnullering(melding)
    }
}

private fun toOebsBestillingMelding(
    bestilling: OpprettOebsBestilling,
    arrangorHovedenhet: BrregHovedenhetDto,
): OebsBestillingMelding {
    val selger = OebsBestillingMelding.Selger(
        organisasjonsNummer = arrangorHovedenhet.organisasjonsnummer.value,
        organisasjonsNavn = arrangorHovedenhet.navn,
        postAdresse = listOfNotNull(
            arrangorHovedenhet.postadresse?.let { adresse ->
                OebsBestillingMelding.Selger.PostAdresse(
                    gateNavn = adresse.adresse?.joinToString(separator = ", ") ?: "",
                    by = adresse.poststed ?: "",
                    postNummer = adresse.postnummer ?: "",
                    landsKode = adresse.landkode ?: "",
                )
            },
        ),
        bedriftsNummer = bestilling.arrangor.underenhet.value,
    )

    val linjer: List<OebsBestillingMelding.Linje> = splitBelopByMonthsInPeriode(bestilling.periode, bestilling.belop)

    return OebsBestillingMelding(
        bestillingsNummer = bestilling.bestillingsnummer,
        bestillingsType = OebsBestillingType.NY,
        rammeavtaleNummer = bestilling.avtalenummer,
        totalSum = bestilling.belop,
        saksbehandler = toOebsPart(bestilling.opprettetAv),
        bdmGodkjenner = toOebsPart(bestilling.opprettetAv),
        startDato = bestilling.periode.start,
        sluttDato = bestilling.periode.getLastDate(),
        valutaKode = "NOK",
        selger = selger,
        bestillingsLinjer = linjer,
        statsregnskapsKonto = OebsKontering.TILTAK.statsregnskapskonto,
        artsKonto = getOebsArtskonto(bestilling.tiltakskode),
        kontor = bestilling.kostnadssted.value,
        tilsagnsAar = bestilling.periode.start.year,
        opprettelsesTidspunkt = bestilling.opprettetTidspunkt,
        kilde = OebsBestillingMelding.Kilde.TILTADM,
    )
}

fun splitBelopByMonthsInPeriode(bestillingsperiode: Periode, belop: Int): List<OebsBestillingMelding.Linje> {
    val monthlyPeriods = bestillingsperiode.splitByMonth()

    val belopPerDay = belop.toDouble() / bestillingsperiode.getDurationInDays()

    val belopByMonth = monthlyPeriods
        .associateWith { floor(belopPerDay * it.getDurationInDays()).toInt() }
        .toMutableMap()

    val remainder = belop - belopByMonth.values.sum()
    if (remainder > 0) {
        val firstPeriod = monthlyPeriods.first()
        belopByMonth[firstPeriod] = belopByMonth.getValue(firstPeriod) + remainder
    }

    return monthlyPeriods.mapIndexed { index, periode ->
        OebsBestillingMelding.Linje(
            linjeNummer = (index + 1),
            antall = belopByMonth.getValue(periode),
            periode = periode.start.monthValue,
            startDato = periode.start,
            sluttDato = periode.getLastDate(),
        )
    }
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

fun toOebsPart(part: OkonomiPart) = when (part) {
    is NavAnsatt -> part.navIdent.value
    is Tiltaksadministrasjon -> OebsBestillingMelding.Kilde.TILTADM.name
}
