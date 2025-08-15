package no.nav.mulighetsrommet.api.gjennomforing.mapper

import no.nav.mulighetsrommet.api.gjennomforing.GjennomforingRequest
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingKontaktpersonDbo
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
import no.nav.mulighetsrommet.model.GjennomforingStatus

object GjennomforingDboMapper {
    fun fromGjennomforingDto(dto: GjennomforingDto) = GjennomforingDbo(
        id = dto.id,
        navn = dto.navn,
        tiltakstypeId = dto.tiltakstype.id,
        arrangorId = dto.arrangor.id,
        arrangorKontaktpersoner = dto.arrangor.kontaktpersoner.map { it.id },
        startDato = dto.startDato,
        sluttDato = dto.sluttDato,
        status = dto.status.type,
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

    fun fromGjennomforingRequest(request: GjennomforingRequest, status: GjennomforingStatus) = GjennomforingDbo(
        id = request.id,
        navn = request.navn,
        tiltakstypeId = request.tiltakstypeId,
        avtaleId = request.avtaleId,
        startDato = request.startDato,
        sluttDato = request.sluttDato,
        status = status,
        antallPlasser = request.antallPlasser,
        arrangorId = request.arrangorId,
        arrangorKontaktpersoner = request.arrangorKontaktpersoner,
        administratorer = request.administratorer,
        navEnheter = request.navEnheter,
        oppstart = request.oppstart,
        kontaktpersoner = request.kontaktpersoner.map {
            GjennomforingKontaktpersonDbo(
                navIdent = it.navIdent,
                beskrivelse = it.beskrivelse,
            )
        },
        stedForGjennomforing = request.stedForGjennomforing,
        faneinnhold = request.faneinnhold,
        beskrivelse = request.beskrivelse,
        deltidsprosent = request.deltidsprosent,
        estimertVentetidVerdi = request.estimertVentetid?.verdi,
        estimertVentetidEnhet = request.estimertVentetid?.enhet,
        tilgjengeligForArrangorDato = request.tilgjengeligForArrangorDato,
        amoKategorisering = request.amoKategorisering,
        utdanningslop = request.utdanningslop,
    )
}
