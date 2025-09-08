package no.nav.mulighetsrommet.api.arrangor.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.*

@Serializable
data class ArrangorKontaktperson(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val arrangorId: UUID,
    val navn: String,
    val beskrivelse: String?,
    val telefon: String?,
    val epost: String,
    val ansvarligFor: List<Ansvar>,
) {
    @Serializable
    enum class Ansvar {
        AVTALE,
        GJENNOMFORING,
        OKONOMI,
    }
}
