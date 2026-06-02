package no.nav.mulighetsrommet.api.amo.db

import no.nav.mulighetsrommet.api.amo.OpplaringKategorisering.InnholdElement
import no.nav.mulighetsrommet.api.janzz.Sertifisering
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import java.util.*

data class OpplaringKategoriseringDbo(
    val kurstypeId: UUID? = null,
    val bransjeId: UUID? = null,
    val forerkort: Set<UUID> = emptySet(),
    val innholdElementer: Set<InnholdElement> = emptySet(),
    val norskprove: Boolean? = null,
    val sertifiseringer: Set<Sertifisering> = emptySet(),
    val utdanningslop: UtdanningslopDbo? = null,
)
