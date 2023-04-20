package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
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
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate,
    val navEnhet: NavEnhet,
    val avtaletype: Avtaletype,
    val avtalestatus: Avtalestatus,
    val prisbetingelser: String?,
    val ansvarlig: String?,
    val url: String?,
    val antallPlasser: Int?,
) {
    @Serializable
    data class Tiltakstype(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val arenaKode: String,
    )

    @Serializable
    data class NavEnhet(
        val enhetsnummer: String,
        val navn: String? = null,
    )

    @Serializable
    data class Leverandor(
        val organisasjonsnummer: String,
        val navn: String? = null,
    )
}
