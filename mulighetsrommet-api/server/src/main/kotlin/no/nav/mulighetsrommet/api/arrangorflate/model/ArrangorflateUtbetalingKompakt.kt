package no.nav.mulighetsrommet.api.arrangorflate.model

import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateArrangorDto
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateGjennomforingDto
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflatePris
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateTiltakstypeDto
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingTypeDto
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
    val pris: ArrangorflatePris,
    val godkjentPris: ValutaBelop?,
)
