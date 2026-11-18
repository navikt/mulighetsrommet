package no.nav.mulighetsrommet.admin.gjennomforing

import no.nav.mulighetsrommet.api.contracts.gjennomforing.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.api.contracts.gjennomforing.TiltaksgjennomforingV2Dto.Arrangor
import no.nav.mulighetsrommet.api.contracts.gjennomforing.TiltaksgjennomforingV2Dto.Enkeltplass
import no.nav.mulighetsrommet.api.contracts.gjennomforing.TiltaksgjennomforingV2Dto.Gruppe
import no.nav.mulighetsrommet.api.domain.gjennomforing.Gjennomforing
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingArena
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingAvtale
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingAvtaleDetaljer
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingAvtaleStatus
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingEnkeltplassStatus
import no.nav.mulighetsrommet.api.domain.tiltak.Prismodell
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype

object TiltaksgjennomforingV2Mapper {
    fun fromGjennomforingAvtale(
        gjennomforing: GjennomforingAvtale,
        detaljer: GjennomforingAvtaleDetaljer,
    ): TiltaksgjennomforingV2Dto {
        return Gruppe(
            id = gjennomforing.id,
            lopenummer = gjennomforing.lopenummer,
            opprettetTidspunkt = gjennomforing.opprettetTidspunkt,
            oppdatertTidspunkt = gjennomforing.oppdatertTidspunkt,
            tiltakskode = gjennomforing.tiltakstype.tiltakskode,
            arrangor = gjennomforing.arrangor.toArrangor(),
            navn = gjennomforing.navn,
            startDato = gjennomforing.startDato,
            sluttDato = gjennomforing.sluttDato,
            status = gjennomforing.status.toTiltaksgjennomforingV2Status(),
            oppstart = gjennomforing.oppstart,
            antallPlasser = gjennomforing.antallPlasser,
            deltidsprosent = gjennomforing.deltidsprosent,
            pameldingType = gjennomforing.pameldingType,
            apentForPamelding = gjennomforing.apentForPamelding,
            oppmoteSted = detaljer.oppmoteSted,
            tilgjengeligForArrangorFraOgMedDato = detaljer.tilgjengeligForArrangorDato,
        )
    }

    fun fromGjennomforingEnkeltplass(gjennomforing: GjennomforingEnkeltplass): TiltaksgjennomforingV2Dto {
        val prismodell = gjennomforing.prismodell
        return Enkeltplass(
            id = gjennomforing.id,
            lopenummer = gjennomforing.lopenummer,
            opprettetTidspunkt = gjennomforing.opprettetTidspunkt,
            oppdatertTidspunkt = gjennomforing.oppdatertTidspunkt,
            tiltakskode = gjennomforing.tiltakstype.tiltakskode,
            arrangor = gjennomforing.arrangor.toArrangor(),
            status = gjennomforing.status.toTiltaksgjennomforingV2Status(),
            oppstart = gjennomforing.oppstart,
            pameldingType = gjennomforing.pameldingType,
            prisinformasjon = when (prismodell) {
                is Prismodell.AnskaffetEnkeltplass -> TiltaksgjennomforingV2Dto.EnkeltplassPrisinformasjon.Anskaffelse(
                    pris = prismodell.totalbelop.belop,
                )

                is Prismodell.IngenKostnader -> TiltaksgjennomforingV2Dto.EnkeltplassPrisinformasjon.IngenKostnader(
                    tilleggsopplysninger = prismodell.prisbetingelser,
                )

                is Prismodell.TilskuddTilOpplaering -> TiltaksgjennomforingV2Dto.EnkeltplassPrisinformasjon.Tilskudd(
                    tilskudd = prismodell.tilskudd.mapValues { it.value.belop },
                    tilleggsopplysninger = prismodell.prisbetingelser,
                )

                is Prismodell.AnnenAvtaltPris,
                is Prismodell.AvtaltPrisPerBenyttetPlassPerHeleUke,
                is Prismodell.AvtaltPrisPerBenyttetPlassPerManed,
                is Prismodell.AvtaltPrisPerBenyttetPlassPerUke,
                is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker,
                is Prismodell.FastSatsPerAvtaltPlassPerManed,
                is Prismodell.FastSatsPerBenyttetPlassPerManed,
                -> error("uventet prismodell ${prismodell.type} for enkeltplass ${gjennomforing.id}")
            },
        )
    }

    fun fromGjennomforingArena(gjennomforing: GjennomforingArena): TiltaksgjennomforingV2Dto {
        return when (gjennomforing.oppstart) {
            GjennomforingOppstartstype.LOPENDE, GjennomforingOppstartstype.FELLES -> Gruppe(
                id = gjennomforing.id,
                lopenummer = gjennomforing.lopenummer,
                opprettetTidspunkt = gjennomforing.opprettetTidspunkt,
                oppdatertTidspunkt = gjennomforing.oppdatertTidspunkt,
                tiltakskode = gjennomforing.tiltakstype.tiltakskode,
                arrangor = gjennomforing.arrangor.toArrangor(),
                navn = gjennomforing.navn,
                startDato = gjennomforing.startDato,
                sluttDato = gjennomforing.sluttDato,
                status = gjennomforing.status.toTiltaksgjennomforingV2Status(),
                oppstart = gjennomforing.oppstart,
                antallPlasser = gjennomforing.antallPlasser,
                deltidsprosent = gjennomforing.deltidsprosent,
                pameldingType = gjennomforing.pameldingType,
                tilgjengeligForArrangorFraOgMedDato = null,
                apentForPamelding = false,
                oppmoteSted = null,
            )

            GjennomforingOppstartstype.ENKELTPLASS -> Enkeltplass(
                id = gjennomforing.id,
                lopenummer = gjennomforing.lopenummer,
                opprettetTidspunkt = gjennomforing.opprettetTidspunkt,
                oppdatertTidspunkt = gjennomforing.oppdatertTidspunkt,
                tiltakskode = gjennomforing.tiltakstype.tiltakskode,
                arrangor = gjennomforing.arrangor.toArrangor(),
                status = gjennomforing.status.toTiltaksgjennomforingV2Status(),
                oppstart = gjennomforing.oppstart,
                pameldingType = gjennomforing.pameldingType,
                prisinformasjon = null,
            )
        }
    }

    private fun Gjennomforing.ArrangorUnderenhet.toArrangor(): Arrangor = Arrangor(
        organisasjonsnummer = organisasjonsnummer,
    )

    private fun GjennomforingAvtaleStatus.toTiltaksgjennomforingV2Status() = when (this) {
        GjennomforingAvtaleStatus.Gjennomfores -> TiltaksgjennomforingV2Dto.Status.GJENNOMFORES
        GjennomforingAvtaleStatus.Avsluttet -> TiltaksgjennomforingV2Dto.Status.AVSLUTTET
        is GjennomforingAvtaleStatus.Avbrutt -> TiltaksgjennomforingV2Dto.Status.AVBRUTT
        is GjennomforingAvtaleStatus.Avlyst -> TiltaksgjennomforingV2Dto.Status.AVLYST
    }

    private fun GjennomforingEnkeltplassStatus.toTiltaksgjennomforingV2Status() = when (this) {
        is GjennomforingEnkeltplassStatus.UtkastTilPamelding,
        is GjennomforingEnkeltplassStatus.SoktInn,
        is GjennomforingEnkeltplassStatus.VenterPaOppstart,
        is GjennomforingEnkeltplassStatus.Deltar,
        -> TiltaksgjennomforingV2Dto.Status.GJENNOMFORES

        is GjennomforingEnkeltplassStatus.Fullfort -> TiltaksgjennomforingV2Dto.Status.AVSLUTTET

        is GjennomforingEnkeltplassStatus.IkkeAktuell,
        is GjennomforingEnkeltplassStatus.Avbrutt,
        is GjennomforingEnkeltplassStatus.AvbruttUtkast,
        is GjennomforingEnkeltplassStatus.Feilregistrert,
        -> TiltaksgjennomforingV2Dto.Status.AVBRUTT
    }
}
