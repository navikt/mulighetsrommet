package no.nav.mulighetsrommet.admin.tiltak

import no.nav.mulighetsrommet.model.DeltakerRegistreringInnholdDto
import no.nav.mulighetsrommet.model.Innholdselement
import java.util.UUID

interface TiltakstypeQueryHandler {
    fun getVeilederinfo(id: UUID): TiltakstypeVeilderinfo?

    fun getAllInnholdselementer(): List<Innholdselement>

    fun getDeltakerregistreringInnhold(id: UUID): DeltakerRegistreringInnholdDto?

    fun getNamesReferencingLenke(lenkeId: UUID): List<String>
}
