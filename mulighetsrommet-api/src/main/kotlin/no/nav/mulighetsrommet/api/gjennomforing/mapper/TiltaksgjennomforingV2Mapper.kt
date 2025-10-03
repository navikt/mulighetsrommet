package no.nav.mulighetsrommet.api.gjennomforing.mapper

import no.nav.mulighetsrommet.api.gjennomforing.model.Enkeltplass
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto

object TiltaksgjennomforingV2Mapper {
    fun fromEnkeltplass(enkeltplass: Enkeltplass) = TiltaksgjennomforingV2Dto(
        tiltakstype = TiltaksgjennomforingV2Dto.Tiltakstype(
            arenakode = enkeltplass.tiltakstype.tiltakskode.arenakode,
            tiltakskode = enkeltplass.tiltakstype.tiltakskode,
        ),
        arrangor = TiltaksgjennomforingV2Dto.Arrangor(
            organisasjonsnummer = enkeltplass.arrangor.organisasjonsnummer,
        ),
        gjennomforing = TiltaksgjennomforingV2Dto.Enkeltplass(
            id = enkeltplass.id,
            opprettetTidspunkt = enkeltplass.opprettetTidspunkt,
            oppdatertTidspunkt = enkeltplass.oppdatertTidspunkt,
        ),
    )
}
