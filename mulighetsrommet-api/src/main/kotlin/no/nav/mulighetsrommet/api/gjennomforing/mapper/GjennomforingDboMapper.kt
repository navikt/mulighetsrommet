package no.nav.mulighetsrommet.api.gjennomforing.mapper

import no.nav.mulighetsrommet.api.gjennomforing.api.EstimertVentetid
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingKontaktpersonDto
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingRequest
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
        estimertVentetidVerdi = gjennomforing.estimertVentetid?.verdi,
        estimertVentetidEnhet = gjennomforing.estimertVentetid?.enhet,
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
        estimertVentetidVerdi = request.estimertVentetid?.verdi,
        estimertVentetidEnhet = request.estimertVentetid?.enhet,
        tilgjengeligForArrangorDato = request.tilgjengeligForArrangorDato,
        amoKategorisering = request.amoKategorisering,
        utdanningslop = request.utdanningslop,
    )

    fun toGjennomforingRequest(gjennomforing: Gjennomforing) = GjennomforingRequest(
        id = gjennomforing.id,
        navn = gjennomforing.navn,
        tiltakstypeId = gjennomforing.tiltakstype.id,
        avtaleId = requireNotNull(gjennomforing.avtaleId),
        startDato = gjennomforing.startDato,
        sluttDato = gjennomforing.sluttDato,
        antallPlasser = gjennomforing.antallPlasser,
        arrangorId = gjennomforing.arrangor.id,
        arrangorKontaktpersoner = gjennomforing.arrangor.kontaktpersoner.map { it.id },
        administratorer = gjennomforing.administratorer.map { it.navIdent },
        navEnheter = gjennomforing.kontorstruktur.flatMap { listOf(it.region.enhetsnummer) + it.kontorer.map { it.enhetsnummer } }.toSet(),
        oppstart = gjennomforing.oppstart,
        kontaktpersoner = gjennomforing.kontaktpersoner.map {
            GjennomforingKontaktpersonDto(
                navIdent = it.navIdent,
                beskrivelse = it.beskrivelse,
            )
        },
        stedForGjennomforing = gjennomforing.stedForGjennomforing,
        faneinnhold = gjennomforing.faneinnhold,
        beskrivelse = gjennomforing.beskrivelse,
        deltidsprosent = gjennomforing.deltidsprosent,
        estimertVentetid = gjennomforing.estimertVentetid?.verdi?.let {
            EstimertVentetid(
                verdi = gjennomforing.estimertVentetid.verdi,
                enhet = gjennomforing.estimertVentetid.enhet,
            )
        },
        tilgjengeligForArrangorDato = gjennomforing.tilgjengeligForArrangorDato,
        amoKategorisering = gjennomforing.amoKategorisering,
        utdanningslop = gjennomforing.utdanningslop?.toDbo(),
    )
}
