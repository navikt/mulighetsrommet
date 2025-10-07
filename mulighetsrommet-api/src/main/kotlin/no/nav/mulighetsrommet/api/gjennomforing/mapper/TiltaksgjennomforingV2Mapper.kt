package no.nav.mulighetsrommet.api.gjennomforing.mapper

import no.nav.mulighetsrommet.api.gjennomforing.model.Enkeltplass
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import java.time.ZoneId

object TiltaksgjennomforingV2Mapper {
    fun fromGruppe(gruppe: Gjennomforing) = TiltaksgjennomforingV2Dto.Gruppe(
        id = gruppe.id,
        opprettetTidspunkt = gruppe.opprettetTidspunkt.atZone(ZoneId.systemDefault()).toInstant(),
        oppdatertTidspunkt = gruppe.oppdatertTidspunkt.atZone(ZoneId.systemDefault()).toInstant(),
        tiltakstype = TiltaksgjennomforingV2Dto.Tiltakstype(
            arenakode = gruppe.tiltakstype.tiltakskode.arenakode,
            tiltakskode = gruppe.tiltakstype.tiltakskode,
        ),
        arrangor = TiltaksgjennomforingV2Dto.Arrangor(
            organisasjonsnummer = gruppe.arrangor.organisasjonsnummer,
        ),
        navn = gruppe.navn,
        startDato = gruppe.startDato,
        sluttDato = gruppe.sluttDato,
        status = gruppe.status.type,
        oppstart = gruppe.oppstart,
        tilgjengeligForArrangorFraOgMedDato = gruppe.tilgjengeligForArrangorDato,
        apentForPamelding = gruppe.apentForPamelding,
        antallPlasser = gruppe.antallPlasser,
    )

    fun fromEnkeltplass(enkeltplass: Enkeltplass) = TiltaksgjennomforingV2Dto.Enkeltplass(
        id = enkeltplass.id,
        opprettetTidspunkt = enkeltplass.opprettetTidspunkt,
        oppdatertTidspunkt = enkeltplass.oppdatertTidspunkt,
        tiltakstype = TiltaksgjennomforingV2Dto.Tiltakstype(
            arenakode = enkeltplass.tiltakstype.tiltakskode.arenakode,
            tiltakskode = enkeltplass.tiltakstype.tiltakskode,
        ),
        arrangor = TiltaksgjennomforingV2Dto.Arrangor(
            organisasjonsnummer = enkeltplass.arrangor.organisasjonsnummer,
        ),
    )
}
