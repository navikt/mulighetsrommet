package no.nav.mulighetsrommet.api.application.tiltak

import no.nav.mulighetsrommet.api.domain.tiltak.Tiltakstype
import no.nav.mulighetsrommet.model.DeltakerRegistreringInnholdDto
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.Innholdselement
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.TiltakstypeV3Dto
import java.util.UUID

interface TiltakstypeQueryHandler {
    fun getAll(
        tiltakskoder: Set<Tiltakskode> = emptySet(),
        sortField: TiltakstypeSortField = TiltakstypeSortField.NAVN,
        sortDirection: SortDirection = SortDirection.ASC,
    ): List<Tiltakstype>

    fun getByTiltakskode(tiltakskode: Tiltakskode): Tiltakstype

    fun getVeilederinfo(id: UUID): TiltakstypeVeilderinfo?

    fun getEksternTiltakstype(id: UUID): TiltakstypeV3Dto?

    fun getAllInnholdselementer(): List<Innholdselement>

    fun getDeltakerregistreringInnhold(id: UUID): DeltakerRegistreringInnholdDto?

    fun upsertRedaksjoneltInnhold(id: UUID, beskrivelse: String?, faneinnhold: Faneinnhold?)

    fun upsertDeltakerRegistreringInnhold(id: UUID, ledetekst: String?, innholdskoder: List<String>)

    fun setFaglenker(id: UUID, lenker: List<UUID>)

    fun setKanKombineresMed(id: UUID, kombineresmedIds: List<UUID>)

    fun getNamesReferencingLenke(lenkeId: UUID): List<String>
}
