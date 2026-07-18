package no.nav.mulighetsrommet.admin.tiltakdokument

import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID

interface TiltakDokumentAdminQueries {
    fun getTiltakDokumentDto(id: UUID): TiltakDokumentDto?

    fun getAllKompaktDto(
        navEnheter: List<NavEnhetNummer> = emptyList(),
        tiltakstyper: List<Tiltakskode> = emptyList(),
        publisert: Boolean? = null,
    ): List<TiltakDokumentKompaktDto>

    fun setPublisert(id: UUID, publisert: Boolean)
}
