package no.nav.mulighetsrommet.api.domain.opplaring

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class Bransje(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val kode: Kode,
    val navn: String,
) {
    enum class Kode {
        INGENIOR_OG_IKT_FAG,
        HELSE_PLEIE_OG_OMSORG,
        BARNE_OG_UNGDOMSARBEID,
        KONTORARBEID,
        BUTIKK_OG_SALGSARBEID,
        BYGG_OG_ANLEGG,
        INDUSTRIARBEID,
        REISELIV_SERVERING_OG_TRANSPORT,
        SERVICEYRKER_OG_ANNET_ARBEID,
        ANDRE_BRANSJER,
    }
}
