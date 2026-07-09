package no.nav.mulighetsrommet.api.domain.tiltak

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.Innsatsgruppe
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class Tiltakstype(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val innsatsgrupper: Set<Innsatsgruppe>,
    val tiltakskode: Tiltakskode,
    val arenakode: String?,
    @Serializable(with = UUIDSerializer::class)
    val sanityId: UUID?,
    val veilederinfo: Veilederinfo?,
    val deltakerinfo: Deltakerinfo?,
) {

    @Serializable
    data class Deltakerinfo(
        val ledetekst: String?,
        val innholdskoder: List<String>,
    )

    @Serializable
    data class Veilederinfo(
        val beskrivelse: String? = null,
        val faneinnhold: Faneinnhold? = null,
        val faglenker: List<
            @Serializable(with = UUIDSerializer::class)
            UUID,
            > = emptyList(),
        val kanKombineresMed: List<
            @Serializable(with = UUIDSerializer::class)
            UUID,
            > = emptyList(),
    )
}
