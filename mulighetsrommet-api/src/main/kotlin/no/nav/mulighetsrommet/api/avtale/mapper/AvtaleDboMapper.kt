package no.nav.mulighetsrommet.api.avtale.mapper

import no.nav.mulighetsrommet.api.avtale.api.AvtaleRequest
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
        prismodell = avtale.prismodell.prismodell(),
        prisbetingelser = avtale.prismodell.prisbetingelser(),
        satser = avtale.prismodell.satser(),
    )

    fun fromAvtaleRequest(
        request: AvtaleRequest,
        startDato: LocalDate,
        prismodell: PrismodellDbo,
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
        personopplysninger = request.personopplysninger,
        personvernBekreftet = request.personvernBekreftet,
        amoKategorisering = request.amoKategorisering,
        opsjonsmodell = request.opsjonsmodell,
        utdanningslop = request.utdanningslop,
        prismodell = prismodell.prismodell,
        prisbetingelser = prismodell.prisbetingelser,
        satser = prismodell.satser,
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
        personopplysninger = dbo.personopplysninger,
        personvernBekreftet = dbo.personvernBekreftet,
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

fun Avtale.PrismodellDto.prismodell(): Prismodell = when (this) {
    is Avtale.PrismodellDto.AnnenAvtaltPris -> Prismodell.ANNEN_AVTALT_PRIS
    is Avtale.PrismodellDto.AvtaltPrisPerManedsverk -> Prismodell.AVTALT_PRIS_PER_MANEDSVERK
    is Avtale.PrismodellDto.AvtaltPrisPerUkesverk -> Prismodell.AVTALT_PRIS_PER_UKESVERK
    is Avtale.PrismodellDto.ForhandsgodkjentPrisPerManedsverk -> Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK
    is Avtale.PrismodellDto.AvtaltPrisPerTimeOppfolgingPerDeltaker -> Prismodell.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER
}

fun Avtale.PrismodellDto.prisbetingelser(): String? = when (this) {
    is Avtale.PrismodellDto.AnnenAvtaltPris -> prisbetingelser
    is Avtale.PrismodellDto.AvtaltPrisPerManedsverk -> prisbetingelser
    is Avtale.PrismodellDto.AvtaltPrisPerUkesverk -> prisbetingelser
    is Avtale.PrismodellDto.AvtaltPrisPerTimeOppfolgingPerDeltaker -> prisbetingelser
    Avtale.PrismodellDto.ForhandsgodkjentPrisPerManedsverk -> null
}

fun Avtale.PrismodellDto.satser(): List<AvtaltSats> = when (this) {
    is Avtale.PrismodellDto.AnnenAvtaltPris,
    is Avtale.PrismodellDto.ForhandsgodkjentPrisPerManedsverk,
    -> emptyList()

    is Avtale.PrismodellDto.AvtaltPrisPerManedsverk -> toAvtalteSatser(satser)
    is Avtale.PrismodellDto.AvtaltPrisPerUkesverk -> toAvtalteSatser(satser)
    is Avtale.PrismodellDto.AvtaltPrisPerTimeOppfolgingPerDeltaker -> toAvtalteSatser(satser)
}

private fun toAvtalteSatser(satser: List<AvtaltSatsDto>): List<AvtaltSats> = satser.map {
    AvtaltSats(
        gjelderFra = it.gjelderFra,
        sats = it.pris,
    )
}
