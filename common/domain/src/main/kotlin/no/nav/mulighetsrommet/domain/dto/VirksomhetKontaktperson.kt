package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.util.*

@Serializable
data class VirksomhetKontaktperson(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val organisasjonsnummer: String,
    val navn: String,
    val beskrivelse: String?,
    val telefon: String?,
    val epost: String,
)
