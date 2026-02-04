package no.nav.mulighetsrommet.api.gjennomforing.mapper

import no.nav.mulighetsrommet.api.avtale.model.fromPrismodell
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDetaljerDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplassDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingGruppeDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingGruppetiltak
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingStatus
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingVeilederinfoDto
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.GjennomforingStatusType

object GjennomforingDtoMapper {
    fun fromGjennomforing(gjennomforing: Gjennomforing) = when (gjennomforing) {
        is GjennomforingGruppetiltak -> fromGruppetiltak(gjennomforing)
        is GjennomforingEnkeltplass -> fromEnkeltplass(gjennomforing)
    }

    fun fromGruppetiltak(gjennomforing: GjennomforingGruppetiltak) = GjennomforingDetaljerDto(
        tiltakstype = gjennomforing.tiltakstype,
        gjennomforing = GjennomforingGruppeDto(
            id = gjennomforing.id,
            navn = gjennomforing.navn,
            lopenummer = gjennomforing.lopenummer,
            tiltaksnummer = gjennomforing.arena?.tiltaksnummer,
            arrangor = gjennomforing.arrangor,
            startDato = gjennomforing.startDato,
            sluttDato = gjennomforing.sluttDato,
            status = fromGjennomforingStatus(gjennomforing.status),
            antallPlasser = gjennomforing.antallPlasser,
            avtaleId = gjennomforing.avtaleId,
            oppstart = gjennomforing.oppstart,
            pameldingType = gjennomforing.pameldingType,
            apentForPamelding = gjennomforing.apentForPamelding,
            opphav = gjennomforing.opphav,
            deltidsprosent = gjennomforing.deltidsprosent,
            tilgjengeligForArrangorDato = gjennomforing.tilgjengeligForArrangorDato,
            administratorer = gjennomforing.administratorer,
            stengt = gjennomforing.stengt,
        ),
        veilederinfo = GjennomforingVeilederinfoDto(
            kontorstruktur = gjennomforing.kontorstruktur,
            kontaktpersoner = gjennomforing.kontaktpersoner,
            oppmoteSted = gjennomforing.oppmoteSted,
            faneinnhold = gjennomforing.faneinnhold,
            beskrivelse = gjennomforing.beskrivelse,
            publisert = gjennomforing.publisert,
            estimertVentetid = gjennomforing.estimertVentetid,
        ),
        prismodell = gjennomforing.prismodell?.let { fromPrismodell(it) },
        amoKategorisering = gjennomforing.amoKategorisering,
        utdanningslop = gjennomforing.utdanningslop,
    )

    fun fromEnkeltplass(gjennomforing: GjennomforingEnkeltplass) = GjennomforingDetaljerDto(
        tiltakstype = gjennomforing.tiltakstype,
        gjennomforing = GjennomforingEnkeltplassDto(
            id = gjennomforing.id,
            navn = gjennomforing.tiltakstype.navn,
            lopenummer = gjennomforing.lopenummer,
            tiltaksnummer = gjennomforing.arena?.tiltaksnummer,
            arrangor = gjennomforing.arrangor,
            startDato = gjennomforing.startDato,
            sluttDato = gjennomforing.sluttDato,
            status = fromGjennomforingStatus(gjennomforing.status),
            opphav = gjennomforing.opphav,
        ),
        veilederinfo = null,
        prismodell = null,
        amoKategorisering = null,
        utdanningslop = null,
    )

    private fun fromGjennomforingStatus(status: GjennomforingStatusType): GjennomforingDto.Status {
        val variant = when (status) {
            GjennomforingStatusType.GJENNOMFORES -> DataElement.Status.Variant.SUCCESS
            GjennomforingStatusType.AVSLUTTET -> DataElement.Status.Variant.NEUTRAL
            GjennomforingStatusType.AVLYST, GjennomforingStatusType.AVBRUTT -> DataElement.Status.Variant.ERROR
        }
        val element = DataElement.Status(status.beskrivelse, variant)
        return GjennomforingDto.Status(status, element)
    }

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
}
