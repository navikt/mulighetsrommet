package no.nav.mulighetsrommet.api.gjennomforing.mapper

import no.nav.mulighetsrommet.api.gjennomforing.api.EstimertVentetid
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingKontaktpersonDto
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingRequest
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingVeilederinfoRequest
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingKontaktpersonDbo
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.navenhet.NavEnhetType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import java.time.LocalDate
import java.util.UUID

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
        oppmoteSted = gjennomforing.oppmoteSted,
        faneinnhold = gjennomforing.faneinnhold,
        beskrivelse = gjennomforing.beskrivelse,
        deltidsprosent = gjennomforing.deltidsprosent,
        estimertVentetidVerdi = gjennomforing.estimertVentetid?.verdi,
        estimertVentetidEnhet = gjennomforing.estimertVentetid?.enhet,
        tilgjengeligForArrangorDato = gjennomforing.tilgjengeligForArrangorDato,
        amoKategorisering = gjennomforing.amoKategorisering,
        utdanningslop = gjennomforing.utdanningslop?.toDbo(),
    )

    fun fromGjennomforingRequest(
        request: GjennomforingRequest,
        startDato: LocalDate,
        antallPlasser: Int,
        arrangorId: UUID,
        status: GjennomforingStatusType,
    ) = GjennomforingDbo(
        id = request.id,
        navn = request.navn,
        tiltakstypeId = request.tiltakstypeId,
        avtaleId = request.avtaleId,
        startDato = startDato,
        sluttDato = request.sluttDato,
        status = status,
        antallPlasser = antallPlasser,
        arrangorId = arrangorId,
        arrangorKontaktpersoner = request.arrangorKontaktpersoner,
        administratorer = request.administratorer,
        navEnheter =
        (request.veilederinformasjon.navRegioner + request.veilederinformasjon.navKontorer + request.veilederinformasjon.navAndreEnheter).toSet(),
        oppstart = request.oppstart,
        kontaktpersoner = request.kontaktpersoner.map {
            GjennomforingKontaktpersonDbo(
                navIdent = it.navIdent,
                beskrivelse = it.beskrivelse,
            )
        },
        stedForGjennomforing = request.stedForGjennomforing,
        oppmoteSted = request.oppmoteSted,
        faneinnhold = request.veilederinformasjon.faneinnhold,
        beskrivelse = request.veilederinformasjon.beskrivelse,
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
        veilederinformasjon = GjennomforingVeilederinfoRequest(
            navRegioner = gjennomforing.kontorstruktur.map { it.region.enhetsnummer },
            navKontorer = gjennomforing.kontorstruktur.flatMap {
                it.kontorer.filter { it.type == NavEnhetType.LOKAL }.map { it.enhetsnummer }
            },
            navAndreEnheter = gjennomforing.kontorstruktur.flatMap {
                it.kontorer.filter { it.type != NavEnhetType.LOKAL }.map { it.enhetsnummer }
            },
            faneinnhold = gjennomforing.faneinnhold,
            beskrivelse = gjennomforing.beskrivelse,
        ),
        administratorer = gjennomforing.administratorer.map { it.navIdent },
        oppstart = gjennomforing.oppstart,
        kontaktpersoner = gjennomforing.kontaktpersoner.map {
            GjennomforingKontaktpersonDto(
                navIdent = it.navIdent,
                beskrivelse = it.beskrivelse,
            )
        },
        stedForGjennomforing = gjennomforing.stedForGjennomforing,
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
        oppmoteSted = gjennomforing.oppmoteSted,
    )
}
