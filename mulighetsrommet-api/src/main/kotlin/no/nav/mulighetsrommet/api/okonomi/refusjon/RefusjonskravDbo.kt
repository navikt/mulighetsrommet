package no.nav.mulighetsrommet.api.okonomi.refusjon

import no.nav.mulighetsrommet.api.okonomi.models.RefusjonKravBeregning
import java.time.LocalDate
import java.util.*

data class RefusjonskravDbo(
    val id: UUID,
    val gjennomforingId: UUID,
    val periodeStart: LocalDate,
    val periodeSlutt: LocalDate,
    val beregning: RefusjonKravBeregning,
)
