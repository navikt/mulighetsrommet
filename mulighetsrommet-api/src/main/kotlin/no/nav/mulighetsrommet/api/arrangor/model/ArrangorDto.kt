package no.nav.mulighetsrommet.api.arrangor.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

enum class ArrangorTil {
    AVTALE,
    TILTAKSGJENNOMFORING,
}

@Serializable
data class ArrangorDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val organisasjonsnummer: Organisasjonsnummer,
    val navn: String,
    val overordnetEnhet: Organisasjonsnummer? = null,
    val underenheter: List<ArrangorDto>? = null,
    @Serializable(with = LocalDateSerializer::class)
    val slettetDato: LocalDate? = null,
)
