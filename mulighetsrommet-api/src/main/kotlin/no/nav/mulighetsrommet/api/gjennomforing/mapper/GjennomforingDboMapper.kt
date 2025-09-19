package no.nav.mulighetsrommet.api.gjennomforing.mapper

import no.nav.mulighetsrommet.api.gjennomforing.GjennomforingRequest
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingKontaktpersonDbo
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.model.GjennomforingStatusType

object GjennomforingDboMapper {
    fun fromGjennomforing(gjennomforing: Gjennomforing) = GjennomforingDbo(
        id = gjennomforing.id,
        navn = gjennomforing.navn,
        tiltakstypeId = gjennomforing.tiltakstype.id,
        arrangorId = gjennomforing.arrangor.id,
        arrangorKontaktpersoner = gjennomforing.arrangor.kontaktpersoner.map { it.id },
        startDato = gjennomforing.startDato,
        sluttDato = gjennomforing.sluttDato,
        status = gjennomforing.status.type,
        antallPlasser = gjennomforing.antallPlasser,
        avtaleId = gjennomforing.avtaleId ?: gjennomforing.id,
        administratorer = gjennomforing.administratorer.map { it.navIdent },
        navEnheter = gjennomforing.kontorstruktur
            .flatMap { (region, kontorer) ->
                kontorer.map { kontor -> kontor.enhetsnummer } + region.enhetsnummer
            }
            .toSet(),
        oppstart = gjennomforing.oppstart,
        kontaktpersoner = gjennomforing.kontaktpersoner.map {
            GjennomforingKontaktpersonDbo(
                navIdent = it.navIdent,
                beskrivelse = it.beskrivelse,
            )
        },
        stedForGjennomforing = gjennomforing.stedForGjennomforing,
        faneinnhold = gjennomforing.faneinnhold,
        beskrivelse = gjennomforing.beskrivelse,
        deltidsprosent = gjennomforing.deltidsprosent,
        estimertVentetid = gjennomforing.estimertVentetid,
        tilgjengeligForArrangorDato = gjennomforing.tilgjengeligForArrangorDato,
        amoKategorisering = gjennomforing.amoKategorisering,
        utdanningslop = gjennomforing.utdanningslop?.toDbo(),
    )

    fun fromGjennomforingRequest(request: GjennomforingRequest, status: GjennomforingStatusType) = GjennomforingDbo(
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
        estimertVentetid = request.estimertVentetid,
        tilgjengeligForArrangorDato = request.tilgjengeligForArrangorDato,
        amoKategorisering = request.amoKategorisering,
        utdanningslop = request.utdanningslop,
    )
}
