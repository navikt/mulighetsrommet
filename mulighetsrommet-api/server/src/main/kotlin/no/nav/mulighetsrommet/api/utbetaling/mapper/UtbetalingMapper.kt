package no.nav.mulighetsrommet.api.utbetaling.mapper

import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregning
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.LocalDateTime
import java.util.UUID

object UtbetalingMapper {
    fun toNewUtbetaling(
        periode: Periode,
        gjennomforing: GjennomforingAvtale,
        beregning: UtbetalingBeregning,
    ): Utbetaling {
        return Utbetaling(
            id = UUID.randomUUID(),
            status = UtbetalingStatusType.GENERERT,
            periode = periode,
            utbetalesTidligstTidspunkt = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            betalingsinformasjon = null,
            tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
            valuta = beregning.output.pris.valuta,
            beregning = beregning,
            tiltakstype = Utbetaling.Tiltakstype(
                gjennomforing.tiltakstype.navn,
                gjennomforing.tiltakstype.tiltakskode,
            ),
            gjennomforing = Utbetaling.Gjennomforing(
                id = gjennomforing.id,
                lopenummer = gjennomforing.lopenummer,
            ),
            arrangor = Utbetaling.Arrangor(
                gjennomforing.arrangor.id,
                gjennomforing.arrangor.organisasjonsnummer,
                gjennomforing.arrangor.navn,
                gjennomforing.arrangor.slettet,
            ),
            korreksjon = null,
            innsending = null,
            begrunnelseMindreBetalt = null,
            avbruttBegrunnelse = null,
            avbruttTidspunkt = null,
            journalpostId = null,
            kommentar = null,
            blokkeringer = emptySet(),
        )
    }
}
