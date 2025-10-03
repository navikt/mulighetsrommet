package no.nav.mulighetsrommet.api.gjennomforing.mapper

import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV1Dto

object TiltaksgjennomforingV1Mapper {
    fun fromGjennomforing(dto: Gjennomforing) = TiltaksgjennomforingV1Dto(
        id = dto.id,
        tiltakstype = TiltaksgjennomforingV1Dto.Tiltakstype(
            id = dto.tiltakstype.id,
            navn = dto.tiltakstype.navn,
            arenaKode = dto.tiltakstype.tiltakskode.arenakode,
            tiltakskode = dto.tiltakstype.tiltakskode,
        ),
        navn = dto.navn,
        startDato = dto.startDato,
        sluttDato = dto.sluttDato,
        status = dto.status.type,
        virksomhetsnummer = dto.arrangor.organisasjonsnummer.value,
        oppstart = dto.oppstart,
        tilgjengeligForArrangorFraOgMedDato = dto.tilgjengeligForArrangorDato,
        apentForPamelding = dto.apentForPamelding,
        antallPlasser = dto.antallPlasser,
        opprettetTidspunkt = dto.opprettetTidspunkt,
        oppdatertTidspunkt = dto.oppdatertTidspunkt,
    )
}
