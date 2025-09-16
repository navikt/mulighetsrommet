package no.nav.mulighetsrommet.api.utbetaling.mapper

import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import java.time.LocalDateTime

object UtbetalingMapper {
    fun toUtbetaling(dbo: UtbetalingDbo, gjennomforing: Gjennomforing): Utbetaling {
        return Utbetaling(
            id = dbo.id,
            status = dbo.status,
            periode = dbo.periode,
            godkjentAvArrangorTidspunkt = null,
            createdAt = LocalDateTime.now(),
            betalingsinformasjon = Utbetaling.Betalingsinformasjon(dbo.kontonummer, null),
            beskrivelse = dbo.beskrivelse,
            begrunnelseMindreBetalt = null,
            journalpostId = null,
            tilskuddstype = dbo.tilskuddstype,
            innsender = null,
            tiltakstype = Utbetaling.Tiltakstype(
                gjennomforing.tiltakstype.navn,
                gjennomforing.tiltakstype.tiltakskode,
            ),
            gjennomforing = Utbetaling.Gjennomforing(
                gjennomforing.id,
                gjennomforing.navn,
            ),
            arrangor = Utbetaling.Arrangor(
                gjennomforing.arrangor.id,
                gjennomforing.arrangor.organisasjonsnummer,
                gjennomforing.arrangor.navn,
                gjennomforing.arrangor.slettet,
            ),
            beregning = dbo.beregning,
        )
    }
}
