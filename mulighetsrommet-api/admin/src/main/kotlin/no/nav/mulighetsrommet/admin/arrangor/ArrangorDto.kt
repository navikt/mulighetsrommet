package no.nav.mulighetsrommet.admin.arrangor

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID

enum class ArrangorKobling {
    AVTALE,
    TILTAKSGJENNOMFORING,
}

@Serializable
data class ArrangorDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val organisasjonsnummer: Organisasjonsnummer,
    val navn: String,
    @Serializable(with = LocalDateSerializer::class)
    val slettetDato: LocalDate? = null,
)

fun Arrangor.toDto() = ArrangorDto(
    id = id,
    organisasjonsnummer = organisasjonsnummer,
    navn = navn,
    slettetDato = slettetDato,
)
