package no.nav.mulighetsrommet.api.avtale.mapper

import no.nav.mulighetsrommet.api.avtale.AvtaleRequest
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.model.AvtaleStatus
import no.nav.mulighetsrommet.model.Periode
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
        },
        avtaletype = dto.avtaletype,
        antallPlasser = dto.antallPlasser,
        administratorer = dto.administratorer.map { it.navIdent },
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = dto.personopplysninger,
        personvernBekreftet = dto.personvernBekreftet,
        amoKategorisering = dto.amoKategorisering,
        opsjonsmodell = dto.opsjonsmodell,
        utdanningslop = dto.utdanningslop?.toDbo(),
        prismodell = dto.prismodell.prismodell(),
        prisbetingelser = when (dto.prismodell) {
            is AvtaleDto.PrismodellDto.AnnenAvtaltPris -> dto.prismodell.prisbetingelser
            is AvtaleDto.PrismodellDto.AvtaltPrisPerManedsverk -> dto.prismodell.prisbetingelser
            is AvtaleDto.PrismodellDto.AvtaltPrisPerUkesverk -> dto.prismodell.prisbetingelser
            AvtaleDto.PrismodellDto.ForhandsgodkjentPrisPerManedsverk -> null
        },
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
        antallPlasser = null,
        administratorer = request.administratorer,
        navEnheter = request.navEnheter,
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
                periode = Periode.fromInclusiveDates(it.periodeStart, it.periodeSlutt),
                sats = it.pris,
            )
        },
    )
}

fun AvtaleDto.PrismodellDto.prismodell(): Prismodell = when (this) {
    is AvtaleDto.PrismodellDto.AnnenAvtaltPris -> Prismodell.ANNEN_AVTALT_PRIS
    is AvtaleDto.PrismodellDto.AvtaltPrisPerManedsverk -> Prismodell.AVTALT_PRIS_PER_MANEDSVERK
    is AvtaleDto.PrismodellDto.AvtaltPrisPerUkesverk -> Prismodell.AVTALT_PRIS_PER_UKESVERK
    is AvtaleDto.PrismodellDto.ForhandsgodkjentPrisPerManedsverk -> Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK
}

fun AvtaleDto.PrismodellDto.satser(): List<AvtaltSats> = when (this) {
    is AvtaleDto.PrismodellDto.AnnenAvtaltPris,
    is AvtaleDto.PrismodellDto.ForhandsgodkjentPrisPerManedsverk,
    -> emptyList()
    is AvtaleDto.PrismodellDto.AvtaltPrisPerManedsverk ->
        this.satser.map {
            AvtaltSats(
                periode = Periode.fromInclusiveDates(it.periodeStart, it.periodeSlutt),
                sats = it.pris,
            )
        }
    is AvtaleDto.PrismodellDto.AvtaltPrisPerUkesverk ->
        this.satser.map {
            AvtaltSats(
                periode = Periode.fromInclusiveDates(it.periodeStart, it.periodeSlutt),
                sats = it.pris,
            )
        }
}
