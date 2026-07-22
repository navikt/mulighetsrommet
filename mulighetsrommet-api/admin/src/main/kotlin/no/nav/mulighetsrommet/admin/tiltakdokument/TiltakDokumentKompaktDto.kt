package no.nav.mulighetsrommet.admin.tiltakdokument

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.admin.navenhet.Kontorstruktur
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class TiltakDokumentKompaktDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val tiltaksnummer: String?,
    val tiltakstype: Tiltakstype,
    val arrangor: Arrangor?,
    val publisert: Boolean,
    val kontorstruktur: List<Kontorstruktur>,
) {
    @Serializable
    data class Tiltakstype(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val tiltakskode: Tiltakskode,
    )

    @Serializable
    data class Arrangor(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val organisasjonsnummer: String,
    )
}
