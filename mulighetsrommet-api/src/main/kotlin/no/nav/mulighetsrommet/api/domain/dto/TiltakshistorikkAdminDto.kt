package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
data class TiltakshistorikkAdminDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = LocalDateTimeSerializer::class)
    val fraDato: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val tilDato: LocalDateTime? = null,
    val status: Deltakerstatus,
    val tiltaksnavn: String?,
    val tiltakstype: String,
    val arrangor: Arrangor?,
) {
    @Serializable
    data class Arrangor(
        val organisasjonsnummer: Organisasjonsnummer,
        val navn: String?,
    )
}
