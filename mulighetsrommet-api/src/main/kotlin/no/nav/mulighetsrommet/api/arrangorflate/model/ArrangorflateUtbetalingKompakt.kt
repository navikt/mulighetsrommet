package no.nav.mulighetsrommet.api.arrangorflate.model

import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateArrangorDto
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateGjennomforingDto
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateTiltakstypeDto
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingType
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingTypeDto
import no.nav.mulighetsrommet.api.utbetaling.api.toDto
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ValutaBelop
import java.util.UUID

data class ArrangorflateUtbetalingKompakt(
    val id: UUID,
    val tiltakstype: ArrangorflateTiltakstypeDto,
    val gjennomforing: ArrangorflateGjennomforingDto,
    val arrangor: ArrangorflateArrangorDto,
    val type: UtbetalingTypeDto,
    val periode: Periode,
    val status: ArrangorflateUtbetalingStatus,
    val pris: ValutaBelop,
    val godkjentBelop: ValutaBelop?,
) {
    companion object {
        fun fromUtbetaling(utbetaling: Utbetaling, status: ArrangorflateUtbetalingStatus, godkjentBelop: ValutaBelop?) = ArrangorflateUtbetalingKompakt(
            id = utbetaling.id,
            status = status,
            tiltakstype = ArrangorflateTiltakstypeDto(
                navn = utbetaling.tiltakstype.navn,
                tiltakskode = utbetaling.tiltakstype.tiltakskode,
            ),
            gjennomforing = ArrangorflateGjennomforingDto(
                id = utbetaling.gjennomforing.id,
                lopenummer = utbetaling.gjennomforing.lopenummer,
                navn = utbetaling.gjennomforing.navn,
            ),
            arrangor = ArrangorflateArrangorDto(
                id = utbetaling.arrangor.id,
                organisasjonsnummer = utbetaling.arrangor.organisasjonsnummer,
                navn = utbetaling.arrangor.navn,
            ),
            periode = utbetaling.periode,
            pris = utbetaling.beregning.output.pris,
            godkjentBelop = godkjentBelop,
            type = UtbetalingType.from(utbetaling).toDto(),
        )
    }
}
