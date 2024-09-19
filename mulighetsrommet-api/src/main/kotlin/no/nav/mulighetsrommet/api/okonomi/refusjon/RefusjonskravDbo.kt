package no.nav.mulighetsrommet.api.okonomi.refusjon

import no.nav.mulighetsrommet.api.okonomi.prismodell.Prismodell
import java.time.LocalDate
import java.util.*

data class RefusjonskravDbo(
    val id: UUID,
    val tiltaksgjennomforingId: UUID,
    val arrangorId: UUID,
    val periode: LocalDate,
    val beregning: Prismodell.RefusjonskravBeregning,
)
