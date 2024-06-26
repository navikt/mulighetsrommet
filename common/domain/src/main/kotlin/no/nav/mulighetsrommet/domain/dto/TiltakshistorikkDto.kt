package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
data class TiltakshistorikkDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = LocalDateTimeSerializer::class)
    val startDato: LocalDateTime?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val sluttDato: LocalDateTime?,
    val status: Deltakerstatus,
    val tiltaksnavn: String,
    val arenaTiltakskode: String,
    val arrangorOrganisasjonsnummer: Organisasjonsnummer?,
)

@Serializable
data class TiltakshistorikkRequest(
    val identer: List<NorskIdent>,
)
