package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
data class AvtaleAdminDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tiltakstype: Tiltakstype,
    val navn: String,
    val avtalenummer: String?,
    val leverandor: Leverandor,
    val leverandorUnderenheter: List<LeverandorUnderenhet>,
    val leverandorKontaktperson: VirksomhetKontaktperson?,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate,
    val navRegion: NavEnhet?,
    val avtaletype: Avtaletype,
    val avtalestatus: Avtalestatus,
    val prisbetingelser: String?,
    val administrator: Administrator?,
    val url: String?,
    val antallPlasser: Int?,
    val navEnheter: List<NavEnhet>,
    val opphav: ArenaMigrering.Opphav,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime,
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
        val navn: String?,
        val slettet: Boolean,
    )

    @Serializable
    data class LeverandorUnderenhet(
        val organisasjonsnummer: String,
        val navn: String,
    )

    @Serializable
    data class Administrator(
        val navIdent: String,
        val navn: String,
    )
}
