package no.nav.mulighetsrommet.api.gjennomforing.mapper

import no.nav.mulighetsrommet.api.avtale.model.fromPrismodell
import no.nav.mulighetsrommet.api.avtale.model.toDto
import no.nav.mulighetsrommet.api.gjennomforing.model.AvbrytelseDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtaleDetaljer
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtaleDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDetaljerDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplassDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingKontaktpersonDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingVeilederinfoDto
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.GjennomforingStatusType

object GjennomforingDtoMapper {
    fun fromGjennomforingAvtale(gjennomforing: GjennomforingAvtale, detaljer: GjennomforingAvtaleDetaljer) = GjennomforingDetaljerDto(
        tiltakstype = gjennomforing.tiltakstype,
        gjennomforing = GjennomforingAvtaleDto(
            id = gjennomforing.id,
            navn = gjennomforing.navn,
            lopenummer = gjennomforing.lopenummer,
            tiltaksnummer = gjennomforing.arena?.tiltaksnummer,
            arrangor = GjennomforingDto.ArrangorUnderenhet(
                id = gjennomforing.arrangor.id,
                organisasjonsnummer = gjennomforing.arrangor.organisasjonsnummer,
                navn = gjennomforing.arrangor.navn,
                slettet = gjennomforing.arrangor.slettet,
                kontaktpersoner = detaljer.arrangorKontaktpersoner.map { it.toArrangorKontaktpersonDto() },
            ),
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
            stengt = gjennomforing.stengt.map { it.toStengtPeriodeDto() },
            tilgjengeligForArrangorDato = detaljer.tilgjengeligForArrangorDato,
            administratorer = detaljer.administratorer.map { it.toAdministratorDto() },
            avbrytelse = detaljer.avbrytelse?.let { AvbrytelseDto(it.aarsaker, it.forklaring) },
        ),
        veilederinfo = GjennomforingVeilederinfoDto(
            kontorstruktur = detaljer.kontorstruktur,
            kontaktpersoner = detaljer.kontaktpersoner.map { it.toKontaktpersonDto() },
            oppmoteSted = detaljer.oppmoteSted,
            beskrivelse = detaljer.beskrivelse,
            faneinnhold = detaljer.faneinnhold,
            publisert = detaljer.publisert,
            estimertVentetid = detaljer.estimertVentetid?.toEstimertVentetidDto(),
        ),
        prismodell = fromPrismodell(gjennomforing.prismodell),
        amoKategorisering = detaljer.amoKategorisering?.toDto(gjennomforing.tiltakstype.tiltakskode),
        utdanningslop = detaljer.utdanningslop,
    )

    fun fromEnkeltplass(gjennomforing: GjennomforingEnkeltplass) = GjennomforingDetaljerDto(
        tiltakstype = gjennomforing.tiltakstype,
        gjennomforing = GjennomforingEnkeltplassDto(
            id = gjennomforing.id,
            navn = gjennomforing.tiltakstype.navn,
            lopenummer = gjennomforing.lopenummer,
            tiltaksnummer = gjennomforing.arena?.tiltaksnummer,
            arrangor = GjennomforingDto.ArrangorUnderenhet(
                id = gjennomforing.arrangor.id,
                organisasjonsnummer = gjennomforing.arrangor.organisasjonsnummer,
                navn = gjennomforing.arrangor.navn,
                slettet = gjennomforing.arrangor.slettet,
                // TODO: er kontaktperson hos arrangør relevant for enkeltplasser?
                kontaktpersoner = listOf(),
            ),
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

    fun fromGjennomforingStatus(status: GjennomforingStatusType): GjennomforingDto.Status {
        val variant = when (status) {
            GjennomforingStatusType.GJENNOMFORES -> DataElement.Status.Variant.SUCCESS
            GjennomforingStatusType.AVSLUTTET -> DataElement.Status.Variant.NEUTRAL
            GjennomforingStatusType.AVLYST, GjennomforingStatusType.AVBRUTT -> DataElement.Status.Variant.ERROR
        }
        val element = DataElement.Status(status.beskrivelse, variant, null)
        return GjennomforingDto.Status(status, element)
    }

    private fun GjennomforingAvtaleDetaljer.Administrator.toAdministratorDto(): GjennomforingDto.Administrator {
        return GjennomforingDto.Administrator(navIdent, navn)
    }

    private fun GjennomforingAvtaleDetaljer.GjennomforingKontaktperson.toKontaktpersonDto(): GjennomforingKontaktpersonDto {
        return GjennomforingKontaktpersonDto(
            navIdent = navIdent,
            navn = navn,
            epost = epost,
            mobilnummer = mobilnummer,
            hovedenhet = hovedenhet,
            beskrivelse = beskrivelse,
        )
    }

    private fun GjennomforingAvtale.StengtPeriode.toStengtPeriodeDto(): GjennomforingDto.StengtPeriode {
        return GjennomforingDto.StengtPeriode(
            id = id,
            start = start,
            slutt = slutt,
            beskrivelse = beskrivelse,
        )
    }

    private fun GjennomforingAvtaleDetaljer.ArrangorKontaktperson.toArrangorKontaktpersonDto(): GjennomforingDto.ArrangorKontaktperson {
        return GjennomforingDto.ArrangorKontaktperson(
            id = id,
            navn = navn,
            beskrivelse = beskrivelse,
            telefon = telefon,
            epost = epost,
        )
    }

    private fun GjennomforingAvtaleDetaljer.EstimertVentetid.toEstimertVentetidDto(): GjennomforingVeilederinfoDto.EstimertVentetid {
        return GjennomforingVeilederinfoDto.EstimertVentetid(verdi, enhet)
    }
}
