package no.nav.mulighetsrommet.api.gjennomforing.mapper

import no.nav.mulighetsrommet.admin.opplaring.OpplaringKategoriseringDetaljer
import no.nav.mulighetsrommet.admin.tiltak.toPrismodellDto
import no.nav.mulighetsrommet.admin.totrinnskontroll.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.gjennomforing.model.DeltakerDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtaleDetaljer
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtaleDetaljerDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtaleDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtaleStatus
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDtoArrangor
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplassDetaljerDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplassDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplassStatus
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingKontaktpersonDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingVeilederinfoDto
import no.nav.mulighetsrommet.model.DataElement

object GjennomforingDtoMapper {
    fun fromGjennomforingAvtale(
        gjennomforing: GjennomforingAvtale,
        detaljer: GjennomforingAvtaleDetaljer,
    ): GjennomforingAvtaleDetaljerDto {
        return GjennomforingAvtaleDetaljerDto(
            tiltakstype = gjennomforing.tiltakstype,
            gjennomforing = GjennomforingAvtaleDto(
                id = gjennomforing.id,
                navn = gjennomforing.navn,
                lopenummer = gjennomforing.lopenummer,
                tiltaksnummer = gjennomforing.arena?.tiltaksnummer,
                arrangor = GjennomforingDtoArrangor(
                    id = gjennomforing.arrangor.id,
                    organisasjonsnummer = gjennomforing.arrangor.organisasjonsnummer,
                    navn = gjennomforing.arrangor.navn,
                    slettet = gjennomforing.arrangor.slettet,
                    kontaktpersoner = detaljer.arrangorKontaktpersoner.map { it.toArrangorKontaktpersonDto() },
                ),
                startDato = gjennomforing.startDato,
                sluttDato = gjennomforing.sluttDato,
                status = fromGjennomforingAvtaleStatus(gjennomforing.status),
                antallPlasser = gjennomforing.antallPlasser,
                avtaleId = gjennomforing.avtaleId,
                oppstart = gjennomforing.oppstart,
                pameldingType = gjennomforing.pameldingType,
                apentForPamelding = gjennomforing.apentForPamelding,
                deltidsprosent = gjennomforing.deltidsprosent,
                stengt = gjennomforing.stengt.map { it.toStengtPeriodeDto() },
                tilgjengeligForArrangorDato = detaljer.tilgjengeligForArrangorDato,
                administratorer = detaljer.administratorer.map { it.toAdministratorDto() },
                avbrytelse = gjennomforing.status.toAvbrytelseDto(),
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
            prismodell = gjennomforing.prismodell.toPrismodellDto(),
            opplaring = detaljer.opplaringKategorisering,
        )
    }

    fun fromEnkeltplass(
        gjennomforing: GjennomforingEnkeltplass,
        okonomi: TotrinnskontrollDto?,
        prisendring: GjennomforingEnkeltplassDetaljerDto.Prisendring?,
        deltaker: DeltakerDto?,
        kategorisering: OpplaringKategoriseringDetaljer?,
    ): GjennomforingEnkeltplassDetaljerDto {
        return GjennomforingEnkeltplassDetaljerDto(
            tiltakstype = gjennomforing.tiltakstype,
            gjennomforing = GjennomforingEnkeltplassDto(
                id = gjennomforing.id,
                navn = gjennomforing.tiltakstype.navn,
                lopenummer = gjennomforing.lopenummer,
                tiltaksnummer = gjennomforing.arena?.tiltaksnummer,
                arrangor = GjennomforingDtoArrangor(
                    id = gjennomforing.arrangor.id,
                    organisasjonsnummer = gjennomforing.arrangor.organisasjonsnummer,
                    navn = gjennomforing.arrangor.navn,
                    slettet = gjennomforing.arrangor.slettet,
                ),
                startDato = gjennomforing.startDato,
                sluttDato = gjennomforing.sluttDato,
                status = deltaker?.status ?: fromEnkeltplassStatus(gjennomforing.status),
                ansvarligEnhet = gjennomforing.toAnsvarligEnhetDto(),
            ),
            prismodell = gjennomforing.prismodell.toPrismodellDto(),
            okonomi = okonomi,
            prisendring = prisendring,
            opplaring = kategorisering,
            deltaker = deltaker,
        )
    }

    fun fromGjennomforingAvtaleStatus(status: GjennomforingAvtaleStatus): DataElement.Status {
        val variant = when (status) {
            is GjennomforingAvtaleStatus.Gjennomfores -> DataElement.Status.Variant.SUCCESS
            is GjennomforingAvtaleStatus.Avsluttet -> DataElement.Status.Variant.NEUTRAL
            is GjennomforingAvtaleStatus.Avlyst, is GjennomforingAvtaleStatus.Avbrutt -> DataElement.Status.Variant.ERROR
        }
        return DataElement.Status(status.type.beskrivelse, variant, null)
    }

    fun fromEnkeltplassStatus(status: GjennomforingEnkeltplassStatus): DataElement.Status {
        val variant = when (status) {
            is GjennomforingEnkeltplassStatus.UtkastTilPamelding -> DataElement.Status.Variant.INFO

            is GjennomforingEnkeltplassStatus.SoktInn -> DataElement.Status.Variant.ALT_2

            is GjennomforingEnkeltplassStatus.VenterPaOppstart -> DataElement.Status.Variant.ALT_3

            is GjennomforingEnkeltplassStatus.Deltar -> DataElement.Status.Variant.BLANK

            is GjennomforingEnkeltplassStatus.Fullfort -> DataElement.Status.Variant.ALT_1

            is GjennomforingEnkeltplassStatus.IkkeAktuell,
            is GjennomforingEnkeltplassStatus.Avbrutt,
            is GjennomforingEnkeltplassStatus.AvbruttUtkast,
            is GjennomforingEnkeltplassStatus.Feilregistrert,
            -> DataElement.Status.Variant.NEUTRAL
        }
        return DataElement.Status(status.type.beskrivelse, variant, null)
    }

    private fun GjennomforingAvtaleStatus.toAvbrytelseDto(): GjennomforingAvtaleDto.AvbrytelseDto? = when (this) {
        is GjennomforingAvtaleStatus.Gjennomfores, is GjennomforingAvtaleStatus.Avsluttet -> null
        is GjennomforingAvtaleStatus.Avlyst -> GjennomforingAvtaleDto.AvbrytelseDto(aarsaker, forklaring)
        is GjennomforingAvtaleStatus.Avbrutt -> GjennomforingAvtaleDto.AvbrytelseDto(aarsaker, forklaring)
    }

    private fun GjennomforingAvtaleDetaljer.Administrator.toAdministratorDto(): GjennomforingAvtaleDto.Administrator {
        return GjennomforingAvtaleDto.Administrator(navIdent, navn)
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

    private fun GjennomforingAvtale.StengtPeriode.toStengtPeriodeDto(): GjennomforingAvtaleDto.StengtPeriode {
        return GjennomforingAvtaleDto.StengtPeriode(
            id = id,
            start = start,
            slutt = slutt,
            beskrivelse = beskrivelse,
        )
    }

    private fun GjennomforingAvtaleDetaljer.ArrangorKontaktperson.toArrangorKontaktpersonDto(): GjennomforingDtoArrangor.Kontaktperson {
        return GjennomforingDtoArrangor.Kontaktperson(
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

    private fun GjennomforingEnkeltplass.toAnsvarligEnhetDto(): GjennomforingEnkeltplassDto.AnsvarligEnhet = GjennomforingEnkeltplassDto.AnsvarligEnhet(
        enhetsnummer = ansvarligEnhet.enhetsnummer,
        navn = ansvarligEnhet.navn,
    )
}
