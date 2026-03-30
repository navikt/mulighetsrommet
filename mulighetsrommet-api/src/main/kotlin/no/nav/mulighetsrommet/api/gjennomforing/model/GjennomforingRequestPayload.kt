package no.nav.mulighetsrommet.api.gjennomforing.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
sealed interface GjennomforingRequestPayload {
    val gjennomforingId: UUID

    @Serializable
    @SerialName("OpprettGjennomforing")
    data class OpprettGjennomforing(
        @Serializable(with = UUIDSerializer::class)
        override val gjennomforingId: UUID,
        val tiltakskode: Tiltakskode,
        val prisinformasjon: String,
        val organisasjonsnummer: String,
    ) : GjennomforingRequestPayload
}
