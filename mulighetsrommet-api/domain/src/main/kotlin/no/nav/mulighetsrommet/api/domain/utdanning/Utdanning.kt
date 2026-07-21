package no.nav.mulighetsrommet.api.domain.utdanning

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class Utdanning(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID = UUID.randomUUID(),
    val programomradekode: String,
    val utdanningId: String,
    val navn: String,
    val sluttkompetanse: Sluttkompetanse?,
    val aktiv: Boolean,
    val utdanningstatus: Status,
    val utdanningslop: List<String>,
    val nusKoder: List<String>,
) {
    @Serializable
    enum class Sluttkompetanse {
        FAGBREV,
        SVENNEBREV,
        STUDIEKOMPETANSE,
        YRKESKOMPETANSE,
    }

    @Serializable
    enum class Status {
        GYLDIG,
        KOMMENDE,
        UTGAAENDE,
        UTGAATT,
    }
}
