package no.nav.mulighetsrommet.api.gjennomforing.mapper

import no.nav.mulighetsrommet.api.gjennomforing.model.ArenaGjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.AvtaleGjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.EnkeltplassGjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto.Arrangor
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto.Enkeltplass
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto.Gruppe
import no.nav.mulighetsrommet.model.Tiltakskoder
import java.time.ZoneId.systemDefault

object TiltaksgjennomforingV2Mapper {
    fun fromGjennomforing(gjennomforing: Gjennomforing): TiltaksgjennomforingV2Dto = when (gjennomforing) {
        is AvtaleGjennomforing -> Gruppe(
            id = gjennomforing.id,
            opprettetTidspunkt = gjennomforing.opprettetTidspunkt.atZone(systemDefault()).toInstant(),
            oppdatertTidspunkt = gjennomforing.oppdatertTidspunkt.atZone(systemDefault()).toInstant(),
            tiltakskode = gjennomforing.tiltakstype.tiltakskode,
            arrangor = Arrangor(
                organisasjonsnummer = gjennomforing.arrangor.organisasjonsnummer,
            ),
            navn = gjennomforing.navn,
            startDato = gjennomforing.startDato,
            sluttDato = gjennomforing.sluttDato,
            status = gjennomforing.status.type,
            oppstart = gjennomforing.oppstart,
            tilgjengeligForArrangorFraOgMedDato = gjennomforing.tilgjengeligForArrangorDato,
            apentForPamelding = gjennomforing.apentForPamelding,
            antallPlasser = gjennomforing.antallPlasser,
            deltidsprosent = gjennomforing.deltidsprosent,
            oppmoteSted = gjennomforing.oppmoteSted,
            pameldingType = gjennomforing.pameldingType,
        )

        is EnkeltplassGjennomforing -> Enkeltplass(
            id = gjennomforing.id,
            opprettetTidspunkt = gjennomforing.opprettetTidspunkt,
            oppdatertTidspunkt = gjennomforing.oppdatertTidspunkt,
            tiltakskode = gjennomforing.tiltakstype.tiltakskode,
            arrangor = Arrangor(
                organisasjonsnummer = gjennomforing.arrangor.organisasjonsnummer,
            ),
        )

        is ArenaGjennomforing -> if (Tiltakskoder.isEnkeltplassTiltak(gjennomforing.tiltakstype.tiltakskode)) {
            Enkeltplass(
                id = gjennomforing.id,
                opprettetTidspunkt = gjennomforing.opprettetTidspunkt,
                oppdatertTidspunkt = gjennomforing.oppdatertTidspunkt,
                tiltakskode = gjennomforing.tiltakstype.tiltakskode,
                arrangor = Arrangor(
                    organisasjonsnummer = gjennomforing.arrangor.organisasjonsnummer,
                ),
            )
        } else {
            Gruppe(
                id = gjennomforing.id,
                opprettetTidspunkt = gjennomforing.opprettetTidspunkt.atZone(systemDefault()).toInstant(),
                oppdatertTidspunkt = gjennomforing.oppdatertTidspunkt.atZone(systemDefault()).toInstant(),
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
        }
    }
}
