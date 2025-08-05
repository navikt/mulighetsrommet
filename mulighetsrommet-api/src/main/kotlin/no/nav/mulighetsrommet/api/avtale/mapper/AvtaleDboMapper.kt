package no.nav.mulighetsrommet.api.avtale.mapper

import no.nav.mulighetsrommet.api.avtale.AvtaleRequest
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
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
        prisbetingelser = dto.prisbetingelser,
        antallPlasser = dto.antallPlasser,
        administratorer = dto.administratorer.map { it.navIdent },
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = dto.personopplysninger,
        personvernBekreftet = dto.personvernBekreftet,
        amoKategorisering = dto.amoKategorisering,
        opsjonsmodell = dto.opsjonsmodell,
        utdanningslop = dto.utdanningslop?.toDbo(),
        prismodell = dto.prismodell,
        satser = dto.satser.map {
            AvtaltSats(
                periode = Periode.fromInclusiveDates(it.periodeStart, it.periodeSlutt),
                sats = it.pris,
            )
        },
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
        prisbetingelser = request.prisbetingelser,
        navEnheter = request.navEnheter,
        beskrivelse = request.beskrivelse,
        faneinnhold = request.faneinnhold,
        personopplysninger = request.personopplysninger,
        personvernBekreftet = request.personvernBekreftet,
        amoKategorisering = request.amoKategorisering,
        opsjonsmodell = request.opsjonsmodell,
        utdanningslop = request.utdanningslop,
        prismodell = request.prismodell,
        satser = request.satser.map {
            AvtaltSats(
                periode = Periode.fromInclusiveDates(it.periodeStart, it.periodeSlutt),
                sats = it.pris,
            )
        },
    )
}
