package no.nav.mulighetsrommet.domain.models

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.util.*

@Serializable
data class Tiltaksgjennomforing(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String?,
    @Serializable(with = UUIDSerializer::class)
    val tiltakstypeId: UUID,
    val tiltaksnummer: String,
    val virksomhetsnummer: String?
)


@Serializable
data class TiltaksgjennomforingMedTiltakstype(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String?,
    @Serializable(with = UUIDSerializer::class)
    val tiltakstypeId: UUID,
    val tiltaksnummer: String,
    val virksomhetsnummer: String?,
    val tiltakskode: String,
    val tiltakstypeNavn: String,
)
