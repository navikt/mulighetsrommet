package no.nav.mulighetsrommet.api.avtale.mapper

import PersonvernDbo
import no.nav.mulighetsrommet.api.avtale.api.AvtaleRequest
import no.nav.mulighetsrommet.api.avtale.api.PersonvernRequest
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.db.PrismodellDbo
import no.nav.mulighetsrommet.api.avtale.model.*
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDate
import java.util.*

object AvtaleDboMapper {
    fun fromAvtale(avtale: Avtale) = AvtaleDbo(
        id = avtale.id,
        navn = avtale.navn,
        tiltakstypeId = avtale.tiltakstype.id,
        avtalenummer = avtale.avtalenummer,
        sakarkivNummer = avtale.sakarkivNummer,
        arrangor = avtale.arrangor?.id?.let {
            AvtaleDbo.Arrangor(
                hovedenhet = it,
                underenheter = avtale.arrangor.underenheter.map { it.id },
                kontaktpersoner = avtale.arrangor.kontaktpersoner.map { it.id },
            )
        },
        startDato = avtale.startDato,
        sluttDato = avtale.sluttDato,
        status = avtale.status.type,
        navEnheter = avtale.kontorstruktur.flatMap {
            it.kontorer.map { kontor -> kontor.enhetsnummer } + it.region.enhetsnummer
        }.toSet(),
        avtaletype = avtale.avtaletype,
        administratorer = avtale.administratorer.map { it.navIdent },
        beskrivelse = avtale.beskrivelse,
        faneinnhold = avtale.faneinnhold,
        personopplysninger = avtale.personopplysninger,
        personvernBekreftet = avtale.personvernBekreftet,
        amoKategorisering = avtale.amoKategorisering,
        opsjonsmodell = avtale.opsjonsmodell,
        utdanningslop = avtale.utdanningslop?.toDbo(),
        prismodell = avtale.prismodell.type,
        prisbetingelser = avtale.prismodell.prisbetingelser(),
        satser = avtale.prismodell.satser(),
    )

    fun fromAvtaleRequest(
        request: AvtaleRequest,
        startDato: LocalDate,
        prismodellDbo: PrismodellDbo,
        arrangor: AvtaleDbo.Arrangor?,
        status: AvtaleStatusType,
        tiltakstypeId: UUID,
    ): AvtaleDbo = AvtaleDbo(
        id = request.id,
        navn = request.navn,
        avtalenummer = request.avtalenummer,
        sakarkivNummer = request.sakarkivNummer,
        tiltakstypeId = tiltakstypeId,
        arrangor = arrangor,
        startDato = startDato,
        sluttDato = request.sluttDato,
        status = status,
        avtaletype = request.avtaletype,
        administratorer = request.administratorer,
        navEnheter = request.navEnheter.toSet(),
        beskrivelse = request.beskrivelse,
        faneinnhold = request.faneinnhold,
        personopplysninger = request.personvern.personopplysninger,
        personvernBekreftet = request.personvern.personvernBekreftet,
        amoKategorisering = request.amoKategorisering,
        opsjonsmodell = request.opsjonsmodell,
        utdanningslop = request.utdanningslop,
        prismodell = prismodellDbo.prismodell,
        prisbetingelser = prismodellDbo.prisbetingelser,
        satser = prismodellDbo.satser,
    )

    fun toAvtaleRequest(dbo: AvtaleDbo, arrangor: AvtaleRequest.Arrangor?, tiltakskode: Tiltakskode) = AvtaleRequest(
        id = dbo.id,
        navn = dbo.navn,
        avtalenummer = dbo.avtalenummer,
        sakarkivNummer = dbo.sakarkivNummer,
        tiltakskode = tiltakskode,
        arrangor = arrangor,
        startDato = dbo.startDato,
        sluttDato = dbo.sluttDato,
        avtaletype = dbo.avtaletype,
        administratorer = dbo.administratorer,
        navEnheter = dbo.navEnheter.toList(),
        beskrivelse = dbo.beskrivelse,
        faneinnhold = dbo.faneinnhold,
        personvern = PersonvernRequest(
            personopplysninger = dbo.personopplysninger,
            personvernBekreftet = dbo.personvernBekreftet,
        ),
        amoKategorisering = dbo.amoKategorisering,
        opsjonsmodell = dbo.opsjonsmodell,
        utdanningslop = dbo.utdanningslop,
        prismodell = PrismodellRequest(
            type = dbo.prismodell,
            prisbetingelser = dbo.prisbetingelser,
            satser = dbo.satser.map {
                AvtaltSatsRequest(
                    pris = it.sats,
                    valuta = "NOK",
                    gjelderFra = it.gjelderFra,
                )
            },
        ),
    )
}

fun Prismodell.prisbetingelser(): String? = when (this) {
    is Prismodell.AnnenAvtaltPris -> prisbetingelser
    is Prismodell.AvtaltPrisPerManedsverk -> prisbetingelser
    is Prismodell.AvtaltPrisPerUkesverk -> prisbetingelser
    is Prismodell.AvtaltPrisPerHeleUkesverk -> prisbetingelser
    is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker -> prisbetingelser
    Prismodell.ForhandsgodkjentPrisPerManedsverk -> null
}

fun Prismodell.satser(): List<AvtaltSats> = when (this) {
    is Prismodell.AnnenAvtaltPris,
    is Prismodell.ForhandsgodkjentPrisPerManedsverk,
    -> emptyList()

    is Prismodell.AvtaltPrisPerManedsverk -> toAvtalteSatser(satser)
    is Prismodell.AvtaltPrisPerUkesverk -> toAvtalteSatser(satser)
    is Prismodell.AvtaltPrisPerHeleUkesverk -> toAvtalteSatser(satser)
    is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker -> toAvtalteSatser(satser)
}

private fun toAvtalteSatser(satser: List<AvtaltSatsDto>): List<AvtaltSats> = satser.map {
    AvtaltSats(
        gjelderFra = it.gjelderFra,
        sats = it.pris,
    )
}

fun PersonvernRequest.toDbo(): PersonvernDbo = PersonvernDbo(
    personvernBekreftet = personvernBekreftet,
    personopplysninger = personopplysninger,
)
