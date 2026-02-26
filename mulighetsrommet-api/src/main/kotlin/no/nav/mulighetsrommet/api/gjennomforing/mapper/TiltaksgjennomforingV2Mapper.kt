package no.nav.mulighetsrommet.api.gjennomforing.mapper

import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingArena
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtaleDetaljer
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto.Arrangor
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto.Enkeltplass
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto.Gruppe

object TiltaksgjennomforingV2Mapper {
    fun fromGjennomforingAvtale(
        gjennomforing: GjennomforingAvtale,
        detaljer: GjennomforingAvtaleDetaljer,
    ): TiltaksgjennomforingV2Dto {
        return Gruppe(
            id = gjennomforing.id,
            opprettetTidspunkt = gjennomforing.opprettetTidspunkt,
            oppdatertTidspunkt = gjennomforing.oppdatertTidspunkt,
            tiltakskode = gjennomforing.tiltakstype.tiltakskode,
            arrangor = Arrangor(
                organisasjonsnummer = gjennomforing.arrangor.organisasjonsnummer,
            ),
            navn = gjennomforing.navn,
            startDato = gjennomforing.startDato,
            sluttDato = gjennomforing.sluttDato,
            status = gjennomforing.status,
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
        return Enkeltplass(
            id = gjennomforing.id,
            opprettetTidspunkt = gjennomforing.opprettetTidspunkt,
            oppdatertTidspunkt = gjennomforing.oppdatertTidspunkt,
            tiltakskode = gjennomforing.tiltakstype.tiltakskode,
            arrangor = Arrangor(
                organisasjonsnummer = gjennomforing.arrangor.organisasjonsnummer,
            ),
        )
    }

    fun fromGjennomforingArena(gjennomforing: GjennomforingArena): TiltaksgjennomforingV2Dto {
        return when (gjennomforing.oppstart) {
            GjennomforingOppstartstype.LOPENDE, GjennomforingOppstartstype.FELLES -> Gruppe(
                id = gjennomforing.id,
                opprettetTidspunkt = gjennomforing.opprettetTidspunkt,
                oppdatertTidspunkt = gjennomforing.oppdatertTidspunkt,
                tiltakskode = gjennomforing.tiltakstype.tiltakskode,
                arrangor = Arrangor(
                    organisasjonsnummer = gjennomforing.arrangor.organisasjonsnummer,
                ),
                navn = gjennomforing.navn,
                startDato = gjennomforing.startDato,
                sluttDato = gjennomforing.sluttDato,
                status = gjennomforing.status,
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
                opprettetTidspunkt = gjennomforing.opprettetTidspunkt,
                oppdatertTidspunkt = gjennomforing.oppdatertTidspunkt,
                tiltakskode = gjennomforing.tiltakstype.tiltakskode,
                arrangor = Arrangor(
                    organisasjonsnummer = gjennomforing.arrangor.organisasjonsnummer,
                ),
            )
        }
    }
}
