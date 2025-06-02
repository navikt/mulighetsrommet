package no.nav.mulighetsrommet.api.gjennomforing.mapper

import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
import no.nav.mulighetsrommet.model.TiltaksgjennomforingEksternV1Dto

object TiltaksgjennomforingEksternMapper {
    fun toTiltaksgjennomforingV1Dto(dto: GjennomforingDto) = TiltaksgjennomforingEksternV1Dto(
        id = dto.id,
        tiltakstype = TiltaksgjennomforingEksternV1Dto.Tiltakstype(
            id = dto.tiltakstype.id,
            navn = dto.tiltakstype.navn,
            arenaKode = dto.tiltakstype.tiltakskode.toArenaKode(),
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
