package no.nav.mulighetsrommet.api.gjennomforing.mapper

import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingStatus
import no.nav.mulighetsrommet.model.DataElement

object GjennomforingDtoMapper {
    fun fromGjennomforing(gjennomforing: Gjennomforing) = GjennomforingDto(
        id = gjennomforing.id,
        tiltakstype = gjennomforing.tiltakstype,
        navn = gjennomforing.navn,
        tiltaksnummer = gjennomforing.tiltaksnummer,
        arrangor = gjennomforing.arrangor,
        startDato = gjennomforing.startDato,
        sluttDato = gjennomforing.sluttDato,
        arenaAnsvarligEnhet = gjennomforing.arenaAnsvarligEnhet,
        status = fromGjennomforingStatus(gjennomforing.status),
        apentForPamelding = gjennomforing.apentForPamelding,
        antallPlasser = gjennomforing.antallPlasser,
        avtaleId = gjennomforing.avtaleId,
        administratorer = gjennomforing.administratorer,
        kontorstruktur = gjennomforing.kontorstruktur,
        oppstart = gjennomforing.oppstart,
        opphav = gjennomforing.opphav,
        kontaktpersoner = gjennomforing.kontaktpersoner,
        stedForGjennomforing = gjennomforing.stedForGjennomforing,
        faneinnhold = gjennomforing.faneinnhold,
        beskrivelse = gjennomforing.beskrivelse,
        publisert = gjennomforing.publisert,
        deltidsprosent = gjennomforing.deltidsprosent,
        estimertVentetid = gjennomforing.estimertVentetid,
        tilgjengeligForArrangorDato = gjennomforing.tilgjengeligForArrangorDato,
        amoKategorisering = gjennomforing.amoKategorisering,
        utdanningslop = gjennomforing.utdanningslop,
        stengt = gjennomforing.stengt,
        oppmoteSted = gjennomforing.oppmoteSted,
    )

    private fun fromGjennomforingStatus(status: GjennomforingStatus): GjennomforingDto.Status {
        val variant = when (status) {
            GjennomforingStatus.Gjennomfores -> DataElement.Status.Variant.SUCCESS
            GjennomforingStatus.Avsluttet -> DataElement.Status.Variant.NEUTRAL
            is GjennomforingStatus.Avlyst, is GjennomforingStatus.Avbrutt -> DataElement.Status.Variant.ERROR
        }
        val description = when (status) {
            GjennomforingStatus.Gjennomfores, GjennomforingStatus.Avsluttet -> null
            is GjennomforingStatus.Avlyst -> status.forklaring
            is GjennomforingStatus.Avbrutt -> status.forklaring
        }
        val element = DataElement.Status(status.type.beskrivelse, variant, description)
        return GjennomforingDto.Status(status.type, element)
    }
}
