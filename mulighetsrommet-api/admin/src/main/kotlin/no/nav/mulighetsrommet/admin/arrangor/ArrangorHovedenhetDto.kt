package no.nav.mulighetsrommet.admin.arrangor

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID

@Serializable
data class ArrangorHovedenhetDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val organisasjonsnummer: Organisasjonsnummer,
    val navn: String,
    val underenheter: List<ArrangorDto>,
    @Serializable(with = LocalDateSerializer::class)
    val slettetDato: LocalDate? = null,
)

fun ArrangorDto.medUnderenheter(underenheter: List<ArrangorDto>) = ArrangorHovedenhetDto(
    id = id,
    organisasjonsnummer = organisasjonsnummer,
    navn = navn,
    underenheter = underenheter,
    slettetDato = slettetDato,
)
