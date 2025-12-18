package no.nav.mulighetsrommet.api.gjennomforing.mapper

import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingRequest
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingKontaktpersonDbo
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
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
        oppmoteSted = gjennomforing.oppmoteSted,
        faneinnhold = gjennomforing.faneinnhold,
        beskrivelse = gjennomforing.beskrivelse,
        deltidsprosent = gjennomforing.deltidsprosent,
        estimertVentetidVerdi = gjennomforing.estimertVentetid?.verdi,
        estimertVentetidEnhet = gjennomforing.estimertVentetid?.enhet,
        tilgjengeligForArrangorDato = gjennomforing.tilgjengeligForArrangorDato,
        amoKategorisering = gjennomforing.amoKategorisering,
        utdanningslop = gjennomforing.utdanningslop?.toDbo(),
        pameldingType = gjennomforing.pameldingType,
    )

    fun fromGjennomforingRequest(
        request: GjennomforingRequest,
        startDato: LocalDate,
        antallPlasser: Int,
        arrangorId: UUID,
        status: GjennomforingStatusType,
        oppstartstype: GjennomforingOppstartstype,
        pameldingType: GjennomforingPameldingType,
        amoKategorisering: AmoKategorisering?,
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
        oppstart = oppstartstype,
        kontaktpersoner = request.kontaktpersoner.map {
            GjennomforingKontaktpersonDbo(
                navIdent = it.navIdent,
                beskrivelse = it.beskrivelse,
            )
        },
        oppmoteSted = request.oppmoteSted?.ifBlank { null },
        faneinnhold = request.veilederinformasjon.faneinnhold,
        beskrivelse = request.veilederinformasjon.beskrivelse,
        deltidsprosent = request.deltidsprosent,
        estimertVentetidVerdi = request.estimertVentetid?.verdi,
        estimertVentetidEnhet = request.estimertVentetid?.enhet,
        tilgjengeligForArrangorDato = request.tilgjengeligForArrangorDato,
        amoKategorisering = amoKategorisering,
        utdanningslop = request.utdanningslop,
        pameldingType = pameldingType,
    )
}
