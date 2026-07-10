package no.nav.mulighetsrommet.api.domain.tiltak

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.UUID

sealed interface TiltakstypeEvent {
    val id: UUID
    val endretAv: NavIdent
    val endretTidspunkt: LocalDateTime
}

@Serializable
data class VeilederinfoOppdatert(
    @Serializable(with = UUIDSerializer::class) override val id: UUID,
    val tiltakskode: Tiltakskode,
    override val endretAv: NavIdent,
    @Serializable(with = LocalDateTimeSerializer::class) override val endretTidspunkt: LocalDateTime,
) : TiltakstypeEvent

@Serializable
data class DeltakerinfoOppdatert(
    @Serializable(with = UUIDSerializer::class) override val id: UUID,
    val tiltakskode: Tiltakskode,
    override val endretAv: NavIdent,
    @Serializable(with = LocalDateTimeSerializer::class) override val endretTidspunkt: LocalDateTime,
) : TiltakstypeEvent
