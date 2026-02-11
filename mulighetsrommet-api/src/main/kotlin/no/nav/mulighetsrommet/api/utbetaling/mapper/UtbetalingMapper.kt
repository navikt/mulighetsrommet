package no.nav.mulighetsrommet.api.utbetaling.mapper

import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import java.time.LocalDateTime

object UtbetalingMapper {
    fun toNewUtbetaling(dbo: UtbetalingDbo, gjennomforing: Gjennomforing): Utbetaling {
        return Utbetaling(
            id = dbo.id,
            status = dbo.status,
            periode = dbo.periode,
            godkjentAvArrangorTidspunkt = null,
            utbetalesTidligstTidspunkt = dbo.utbetalesTidligstTidspunkt,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            betalingsinformasjon = dbo.betalingsinformasjon,
            beskrivelse = dbo.beskrivelse,
            begrunnelseMindreBetalt = null,
            avbruttBegrunnelse = null,
            avbruttTidspunkt = null,
            journalpostId = null,
            tilskuddstype = dbo.tilskuddstype,
            innsender = null,
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
            valuta = dbo.valuta,
            beregning = dbo.beregning,
        )
    }
}
