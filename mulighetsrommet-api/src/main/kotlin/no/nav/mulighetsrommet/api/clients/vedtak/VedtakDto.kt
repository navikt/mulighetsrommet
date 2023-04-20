package no.nav.mulighetsrommet.api.clients.vedtak

import kotlinx.serialization.Serializable

@Serializable
data class VedtakDto(
    val innsatsgruppe: Innsatsgruppe?,
)

@Serializable
enum class Innsatsgruppe {
    STANDARD_INNSATS, SITUASJONSBESTEMT_INNSATS, SPESIELT_TILPASSET_INNSATS, GRADERT_VARIG_TILPASSET_INNSATS, VARIG_TILPASSET_INNSATS
}
