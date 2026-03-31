package no.nav.mulighetsrommet.api.utbetaling.mapper

import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import java.time.LocalDateTime

object UtbetalingMapper {
    fun toNewUtbetaling(dbo: UtbetalingDbo, gjennomforing: GjennomforingAvtale): Utbetaling {
        return Utbetaling(
            id = dbo.id,
            status = dbo.status,
            periode = dbo.periode,
            utbetalesTidligstTidspunkt = dbo.utbetalesTidligstTidspunkt,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            betalingsinformasjon = dbo.betalingsinformasjon,
            tilskuddstype = dbo.tilskuddstype,
            valuta = dbo.valuta,
            beregning = dbo.beregning,
            tiltakstype = Utbetaling.Tiltakstype(
                gjennomforing.tiltakstype.navn,
                gjennomforing.tiltakstype.tiltakskode,
            ),
            gjennomforing = Utbetaling.Gjennomforing(
                id = gjennomforing.id,
                lopenummer = gjennomforing.lopenummer,
                navn = gjennomforing.navn,
                start = gjennomforing.startDato,
                slutt = gjennomforing.sluttDato,
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
