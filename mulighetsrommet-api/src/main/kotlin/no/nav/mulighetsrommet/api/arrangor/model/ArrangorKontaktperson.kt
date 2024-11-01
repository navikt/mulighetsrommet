package no.nav.mulighetsrommet.api.arrangor.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
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
    val ansvarligFor: List<AnsvarligFor>? = emptyList(),
) {
    @Serializable
    enum class AnsvarligFor {
        AVTALE,
        TILTAKSGJENNOMFORING,
        OKONOMI,
    }
}
