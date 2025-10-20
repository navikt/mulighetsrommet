package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.tilsagn.api.KostnadsstedDto
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

data class InnsendingKompaktDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val periode: Periode,
    val kostnadssteder: List<KostnadsstedDto>,
    val belop: Int?,
    val arrangor: String,
    val tiltakstype: Utbetaling.Tiltakstype,
)
