package no.nav.mulighetsrommet.api.kostnadssted

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NavEnhetNummer

@Serializable
data class RegionKostnadssteder(
    val region: Region,
    val kostnadssteder: List<Kontor>,
) {
    @Serializable
    data class Region(
        val navn: String,
        val enhetsnummer: NavEnhetNummer,
    )

    @Serializable
    data class Kontor(
        val navn: String,
        val enhetsnummer: NavEnhetNummer,
    )

    companion object {
        fun fromKostnadssteder(kostnadssteder: List<Kostnadssted>): List<RegionKostnadssteder> {
            return kostnadssteder.groupBy { it.region }.entries
                .map { (region, kostnadssteder) ->
                    RegionKostnadssteder(
                        region = Region(region.navn, region.enhetsnummer),
                        kostnadssteder = kostnadssteder
                            .map { enhet ->
                                Kontor(enhet.navn, enhet.enhetsnummer)
                            }
                            .sortedBy { it.navn },
                    )
                }
                .sortedBy { it.region.navn }
        }
    }
}
