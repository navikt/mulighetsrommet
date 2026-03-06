package no.nav.mulighetsrommet.api.arrangorflate.model

import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.ValutaBelop
import java.time.LocalDate
import java.util.UUID

data class OpprettUtbetaling(
    val gjennomforingId: UUID,
    val periodeStart: LocalDate,
    val periodeSlutt: LocalDate,
    val kidNummer: Kid?,
    val pris: ValutaBelop,
    val vedlegg: List<Vedlegg>,
)
