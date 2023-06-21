package no.nav.mulighetsrommet.domain.dbo

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.domain.dto.VirksomhetKontaktperson
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class AvtaleDbo(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    @Serializable(with = UUIDSerializer::class)
    val tiltakstypeId: UUID,
    val avtalenummer: String? = null,
    val leverandorOrganisasjonsnummer: String,
    val leverandorUnderenheter: List<String>,
    val leverandorKontaktperson: VirksomhetKontaktperson? = null,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate,
    val arenaAnsvarligEnhet: String?,
    val navRegion: String?,
    val navEnheter: List<String>,
    val avtaletype: Avtaletype,
    val avslutningsstatus: Avslutningsstatus,
    val opphav: ArenaMigrering.Opphav,
    val prisbetingelser: String? = null,
    val antallPlasser: Int? = null,
    val url: String? = null,
    val ansvarlige: List<String> = emptyList(),
)
