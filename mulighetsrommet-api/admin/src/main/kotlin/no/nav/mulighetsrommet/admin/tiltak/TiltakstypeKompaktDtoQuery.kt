package no.nav.mulighetsrommet.admin.tiltak

import no.nav.mulighetsrommet.api.domain.tiltak.SortDirection
import no.nav.mulighetsrommet.api.domain.tiltak.Tiltakstype
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeFeature
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeSortField
import no.nav.mulighetsrommet.model.TiltakstypeEgenskap

data class GetAllTiltakstypeKompakt(
    val sortField: TiltakstypeSortField = TiltakstypeSortField.NAVN,
    val sortDirection: SortDirection = SortDirection.ASC,
    val egenskaper: Set<TiltakstypeEgenskap> = setOf(),
)

class TiltakstypeKompaktQuery(
    private val tiltakstypeService: TiltakstypeService,
) {
    fun execute(query: GetAllTiltakstypeKompakt): List<TiltakstypeKompaktDto> {
        val enabled = tiltakstypeService
            .getTiltakskodeByFeatures(setOf(TiltakstypeFeature.VISES_I_TILTAKSADMINISTRASJON))
            .ifEmpty { return listOf() }

        val tiltakskoder = if (query.egenskaper.isNotEmpty()) {
            enabled
                .filter { kode -> kode.egenskaper.any { it in query.egenskaper } }
                .toSet()
                .ifEmpty { return listOf() }
        } else {
            enabled
        }

        val tiltakstyper = tiltakstypeService.getAll(
            tiltakskoder = tiltakskoder,
            sortField = query.sortField,
            sortDirection = query.sortDirection,
        )

        return tiltakstyper.map { it.toTiltakstypeKompaktDto() }
    }

    private fun Tiltakstype.toTiltakstypeKompaktDto(): TiltakstypeKompaktDto {
        val features = tiltakstypeService.getFeatures(tiltakskode)
        return TiltakstypeKompaktDto(
            id = id,
            navn = navn,
            tiltakskode = tiltakskode,
            gruppe = tiltakskode.gruppe?.tittel,
            features = features,
            egenskaper = tiltakskode.egenskaper,
        )
    }
}
