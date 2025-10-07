package no.nav.mulighetsrommet.api.arrangorflate.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing.Tiltakstype
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class ArrangorflateGjennomforing(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val tiltakstype: ArrangorflateTiltakstype,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
)

@Serializable
data class ArrangorflateGjennomforingInfo(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
)
