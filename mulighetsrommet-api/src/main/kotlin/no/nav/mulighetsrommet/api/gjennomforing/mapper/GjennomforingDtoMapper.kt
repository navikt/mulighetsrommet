package no.nav.mulighetsrommet.api.gjennomforing.mapper

import no.nav.mulighetsrommet.api.avtale.model.fromPrismodell
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingGruppetiltak
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingStatus
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.GjennomforingStatusType

object GjennomforingDtoMapper {
    fun fromGjennomforing(gjennomforing: GjennomforingGruppetiltak) = GjennomforingDto(
        id = gjennomforing.id,
        tiltakstype = gjennomforing.tiltakstype,
        navn = gjennomforing.navn,
        lopenummer = gjennomforing.lopenummer,
        tiltaksnummer = gjennomforing.arena?.tiltaksnummer,
        arrangor = gjennomforing.arrangor,
        startDato = gjennomforing.startDato,
        sluttDato = gjennomforing.sluttDato,
        arenaAnsvarligEnhet = gjennomforing.arena?.ansvarligNavEnhet,
        status = fromGjennomforingStatus(gjennomforing.status, gjennomforing.avbrytelse),
        avbrytelse = gjennomforing.avbrytelse,
        apentForPamelding = gjennomforing.apentForPamelding,
        antallPlasser = gjennomforing.antallPlasser,
        avtaleId = gjennomforing.avtaleId,
        administratorer = gjennomforing.administratorer,
        kontorstruktur = gjennomforing.kontorstruktur,
        oppstart = gjennomforing.oppstart,
        opphav = gjennomforing.opphav,
        kontaktpersoner = gjennomforing.kontaktpersoner,
        oppmoteSted = gjennomforing.oppmoteSted,
        faneinnhold = gjennomforing.faneinnhold,
        beskrivelse = gjennomforing.beskrivelse,
        publisert = gjennomforing.publisert,
        deltidsprosent = gjennomforing.deltidsprosent,
        estimertVentetid = gjennomforing.estimertVentetid,
        tilgjengeligForArrangorDato = gjennomforing.tilgjengeligForArrangorDato,
        amoKategorisering = gjennomforing.amoKategorisering,
        utdanningslop = gjennomforing.utdanningslop,
        stengt = gjennomforing.stengt,
        prismodell = gjennomforing.prismodell?.let { fromPrismodell(it) },
        pameldingType = gjennomforing.pameldingType,
    )

    fun fromGjennomforingStatus(status: GjennomforingStatus): GjennomforingDto.Status {
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

    fun fromGjennomforingStatus(status: GjennomforingStatusType, avbrytelse: GjennomforingGruppetiltak.Avbrytelse?): GjennomforingDto.Status {
        val variant = when (status) {
            GjennomforingStatusType.GJENNOMFORES -> DataElement.Status.Variant.SUCCESS
            GjennomforingStatusType.AVSLUTTET -> DataElement.Status.Variant.NEUTRAL
            GjennomforingStatusType.AVLYST, GjennomforingStatusType.AVBRUTT -> DataElement.Status.Variant.ERROR
        }
        val description = when (status) {
            GjennomforingStatusType.GJENNOMFORES,
            GjennomforingStatusType.AVSLUTTET,
            -> null

            GjennomforingStatusType.AVLYST, GjennomforingStatusType.AVBRUTT,
            -> avbrytelse?.forklaring
        }

        avbrytelse?.forklaring
        val element = DataElement.Status(status.beskrivelse, variant, description)
        return GjennomforingDto.Status(status, element)
    }
}
