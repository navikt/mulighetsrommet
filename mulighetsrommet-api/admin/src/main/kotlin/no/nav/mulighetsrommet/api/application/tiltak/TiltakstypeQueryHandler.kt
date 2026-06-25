package no.nav.mulighetsrommet.api.application.tiltak

import no.nav.mulighetsrommet.model.DeltakerRegistreringInnholdDto
import no.nav.mulighetsrommet.model.Innholdselement
import no.nav.mulighetsrommet.model.TiltakstypeV3Dto
import java.util.UUID

interface TiltakstypeQueryHandler {
    fun getVeilederinfo(id: UUID): TiltakstypeVeilderinfo?

    fun getEksternTiltakstype(id: UUID): TiltakstypeV3Dto?

    fun getAllInnholdselementer(): List<Innholdselement>

    fun getDeltakerregistreringInnhold(id: UUID): DeltakerRegistreringInnholdDto?

    fun getNamesReferencingLenke(lenkeId: UUID): List<String>
}
