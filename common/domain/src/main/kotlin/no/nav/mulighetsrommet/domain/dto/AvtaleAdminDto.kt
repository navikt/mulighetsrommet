package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class AvtaleAdminDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tiltakstype: Tiltakstype,
    val navn: String,
    val avtalenummer: String?,
    val leverandor: Leverandor,
    val leverandorUnderenheter: List<Leverandor>,
    val leverandorKontaktperson: VirksomhetKontaktperson?,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate,
    val navRegion: NavEnhet?,
    val avtaletype: Avtaletype,
    val avtalestatus: Avtalestatus,
    val prisbetingelser: String?,
    val ansvarlig: Avtaleansvarlig?,
    val url: String?,
    val antallPlasser: Int?,
    val navEnheter: List<NavEnhet>,
    val opphav: ArenaMigrering.Opphav,
) {
    @Serializable
    data class Tiltakstype(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val arenaKode: String,
    )

    @Serializable
    data class Leverandor(
        val organisasjonsnummer: String,
        val navn: String? = null,
    )

    @Serializable
    data class Avtaleansvarlig(
        val navident: String? = null,
        val navn: String? = null,
    )
}
