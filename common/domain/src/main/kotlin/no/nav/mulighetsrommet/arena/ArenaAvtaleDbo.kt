package no.nav.mulighetsrommet.arena

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class ArenaAvtaleDbo(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    @Serializable(with = UUIDSerializer::class)
    val tiltakstypeId: UUID,
    val avtalenummer: String?,
    val arrangorOrganisasjonsnummer: String,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val arenaAnsvarligEnhet: String?,
    val avtaletype: Avtaletype,
    val avslutningsstatus: Avslutningsstatus,
    val prisbetingelser: String?,
)
