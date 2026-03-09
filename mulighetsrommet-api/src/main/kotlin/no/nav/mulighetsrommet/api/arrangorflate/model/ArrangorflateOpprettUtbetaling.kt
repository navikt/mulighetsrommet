package no.nav.mulighetsrommet.api.arrangorflate.model

import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ValutaBelop
import java.util.UUID

data class ArrangorflateOpprettUtbetaling(
    val gjennomforingId: UUID,
    val periode: Periode,
    val kidNummer: Kid?,
    val pris: ValutaBelop,
    val vedlegg: List<Vedlegg>,
)
