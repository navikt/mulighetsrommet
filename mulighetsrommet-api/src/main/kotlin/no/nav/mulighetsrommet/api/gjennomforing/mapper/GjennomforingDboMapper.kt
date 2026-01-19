package no.nav.mulighetsrommet.api.gjennomforing.mapper

import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingRequest
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingGruppetiltakDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingKontaktpersonDbo
import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import java.time.LocalDate
import java.util.UUID

object GjennomforingDboMapper {
    fun fromGjennomforingRequest(
        request: GjennomforingRequest,
        startDato: LocalDate,
        antallPlasser: Int,
        prismodellId: UUID,
        arrangorId: UUID,
        status: GjennomforingStatusType,
        oppstartstype: GjennomforingOppstartstype,
        pameldingType: GjennomforingPameldingType,
        amoKategorisering: AmoKategorisering?,
    ) = GjennomforingGruppetiltakDbo(
        id = request.id,
        navn = request.navn,
        tiltakstypeId = request.tiltakstypeId,
        arrangorId = arrangorId,
        arrangorKontaktpersoner = request.arrangorKontaktpersoner,
        startDato = startDato,
        sluttDato = request.sluttDato,
        status = status,
        antallPlasser = antallPlasser,
        avtaleId = request.avtaleId,
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
        prismodellId = prismodellId,
    )
}
