package no.nav.mulighetsrommet.api.gjennomforing.mapper

import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingKontaktpersonDbo
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto

object GjennomforingDboMapper {
    fun toTiltaksgjennomforingDbo(dto: GjennomforingDto) = GjennomforingDbo(
        id = dto.id,
        navn = dto.navn,
        tiltakstypeId = dto.tiltakstype.id,
        arrangorId = dto.arrangor.id,
        arrangorKontaktpersoner = dto.arrangor.kontaktpersoner.map { it.id },
        startDato = dto.startDato,
        sluttDato = dto.sluttDato,
        antallPlasser = dto.antallPlasser,
        avtaleId = dto.avtaleId ?: dto.id,
        administratorer = dto.administratorer.map { it.navIdent },
        navEnheter = dto.kontorstruktur
            .flatMap { (region, kontorer) ->
                kontorer.map { kontor -> kontor.enhetsnummer } + region.enhetsnummer
            }
            .toSet(),
        oppstart = dto.oppstart,
        kontaktpersoner = dto.kontaktpersoner.map {
            GjennomforingKontaktpersonDbo(
                navIdent = it.navIdent,
                navEnheter = it.navEnheter,
                beskrivelse = it.beskrivelse,
            )
        },
        stedForGjennomforing = dto.stedForGjennomforing,
        faneinnhold = dto.faneinnhold,
        beskrivelse = dto.beskrivelse,
        deltidsprosent = dto.deltidsprosent,
        estimertVentetidVerdi = dto.estimertVentetid?.verdi,
        estimertVentetidEnhet = dto.estimertVentetid?.enhet,
        tilgjengeligForArrangorDato = dto.tilgjengeligForArrangorDato,
        amoKategorisering = dto.amoKategorisering,
        utdanningslop = dto.utdanningslop?.toDbo(),
    )
}
