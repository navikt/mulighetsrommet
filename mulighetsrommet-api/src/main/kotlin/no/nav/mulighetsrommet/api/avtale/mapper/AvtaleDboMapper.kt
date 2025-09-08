package no.nav.mulighetsrommet.api.avtale.mapper

import no.nav.mulighetsrommet.api.avtale.AvtaleRequest
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.model.*
import no.nav.mulighetsrommet.model.AvtaleStatus
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID

object AvtaleDboMapper {
    fun fromAvtaleDto(dto: AvtaleDto) = AvtaleDbo(
        id = dto.id,
        navn = dto.navn,
        tiltakstypeId = dto.tiltakstype.id,
        avtalenummer = dto.avtalenummer,
        sakarkivNummer = dto.sakarkivNummer,
        arrangor = dto.arrangor?.id?.let {
            AvtaleDbo.Arrangor(
                hovedenhet = it,
                underenheter = dto.arrangor.underenheter.map { it.id },
                kontaktpersoner = dto.arrangor.kontaktpersoner.map { it.id },
            )
        },
        startDato = dto.startDato,
        sluttDato = dto.sluttDato,
        status = dto.status.type,
        navEnheter = dto.kontorstruktur.flatMap {
            it.kontorer.map { kontor -> kontor.enhetsnummer } + it.region.enhetsnummer
        }.toSet(),
        avtaletype = dto.avtaletype,
        administratorer = dto.administratorer.map { it.navIdent },
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = dto.personopplysninger,
        personvernBekreftet = dto.personvernBekreftet,
        amoKategorisering = dto.amoKategorisering,
        opsjonsmodell = dto.opsjonsmodell,
        utdanningslop = dto.utdanningslop?.toDbo(),
        prismodell = dto.prismodell.prismodell(),
        prisbetingelser = dto.prismodell.prisbetingelser(),
        satser = dto.prismodell.satser(),
    )

    fun fromAvtaleRequest(
        request: AvtaleRequest,
        arrangor: AvtaleDbo.Arrangor?,
        status: AvtaleStatus,
        tiltakstypeId: UUID,
    ): AvtaleDbo = AvtaleDbo(
        id = request.id,
        navn = request.navn,
        avtalenummer = request.avtalenummer,
        sakarkivNummer = request.sakarkivNummer,
        tiltakstypeId = tiltakstypeId,
        arrangor = arrangor,
        startDato = request.startDato,
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
        prismodell = request.prismodell.type,
        prisbetingelser = request.prismodell.prisbetingelser,
        satser = request.prismodell.satser.map {
            AvtaltSats(
                gjelderFra = it.gjelderFra,
                sats = it.pris,
            )
        },
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
            satser = dbo.satser.toDto(),
        ),
    )
}

fun AvtaleDto.PrismodellDto.prismodell(): Prismodell = when (this) {
    is AvtaleDto.PrismodellDto.AnnenAvtaltPris -> Prismodell.ANNEN_AVTALT_PRIS
    is AvtaleDto.PrismodellDto.AvtaltPrisPerManedsverk -> Prismodell.AVTALT_PRIS_PER_MANEDSVERK
    is AvtaleDto.PrismodellDto.AvtaltPrisPerUkesverk -> Prismodell.AVTALT_PRIS_PER_UKESVERK
    is AvtaleDto.PrismodellDto.ForhandsgodkjentPrisPerManedsverk -> Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK
    is AvtaleDto.PrismodellDto.AvtaltPrisPerTimeOppfolgingPerDeltaker -> Prismodell.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER
}

fun AvtaleDto.PrismodellDto.prisbetingelser(): String? = when (this) {
    is AvtaleDto.PrismodellDto.AnnenAvtaltPris -> prisbetingelser
    is AvtaleDto.PrismodellDto.AvtaltPrisPerManedsverk -> prisbetingelser
    is AvtaleDto.PrismodellDto.AvtaltPrisPerUkesverk -> prisbetingelser
    is AvtaleDto.PrismodellDto.AvtaltPrisPerTimeOppfolgingPerDeltaker -> prisbetingelser
    AvtaleDto.PrismodellDto.ForhandsgodkjentPrisPerManedsverk -> null
}

fun AvtaleDto.PrismodellDto.satser(): List<AvtaltSats> = when (this) {
    is AvtaleDto.PrismodellDto.AnnenAvtaltPris,
    is AvtaleDto.PrismodellDto.ForhandsgodkjentPrisPerManedsverk,
    -> emptyList()

    is AvtaleDto.PrismodellDto.AvtaltPrisPerManedsverk -> toAvtalteSatser(satser)
    is AvtaleDto.PrismodellDto.AvtaltPrisPerUkesverk -> toAvtalteSatser(satser)
    is AvtaleDto.PrismodellDto.AvtaltPrisPerTimeOppfolgingPerDeltaker -> toAvtalteSatser(satser)
}

private fun toAvtalteSatser(satser: List<AvtaltSatsDto>): List<AvtaltSats> = satser.map {
    AvtaltSats(
        gjelderFra = it.gjelderFra,
        sats = it.pris,
    )
}
