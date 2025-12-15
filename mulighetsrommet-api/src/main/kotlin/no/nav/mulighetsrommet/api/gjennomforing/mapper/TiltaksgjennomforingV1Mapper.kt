package no.nav.mulighetsrommet.api.gjennomforing.mapper

import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingGruppetiltak
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV1Dto

object TiltaksgjennomforingV1Mapper {
    fun fromGjennomforing(gjennomforing: GjennomforingGruppetiltak) = TiltaksgjennomforingV1Dto(
        id = gjennomforing.id,
        tiltakstype = TiltaksgjennomforingV1Dto.Tiltakstype(
            id = gjennomforing.tiltakstype.id,
            navn = gjennomforing.tiltakstype.navn,
            arenaKode = gjennomforing.tiltakstype.tiltakskode.arenakode,
            tiltakskode = gjennomforing.tiltakstype.tiltakskode,
        ),
        navn = gjennomforing.navn,
        startDato = gjennomforing.startDato,
        sluttDato = gjennomforing.sluttDato,
        status = gjennomforing.status.type,
        virksomhetsnummer = gjennomforing.arrangor.organisasjonsnummer.value,
        oppstart = gjennomforing.oppstart,
        tilgjengeligForArrangorFraOgMedDato = gjennomforing.tilgjengeligForArrangorDato,
        apentForPamelding = gjennomforing.apentForPamelding,
        antallPlasser = gjennomforing.antallPlasser,
        deltidsprosent = gjennomforing.deltidsprosent,
        opprettetTidspunkt = gjennomforing.opprettetTidspunkt,
        oppdatertTidspunkt = gjennomforing.oppdatertTidspunkt,
        oppmoteSted = gjennomforing.oppmoteSted,
    )
}
