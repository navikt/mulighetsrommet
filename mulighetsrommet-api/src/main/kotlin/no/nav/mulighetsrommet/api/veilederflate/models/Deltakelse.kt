package no.nav.mulighetsrommet.api.veilederflate.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class Deltakelse {
    abstract val id: UUID
    abstract val eierskap: DeltakelseEierskap
    abstract val tittel: String
    abstract val tiltakstypeNavn: String
    abstract val innsoktDato: LocalDate?
    abstract val sistEndretDato: LocalDate?
    abstract val periode: DeltakelsePeriode
    abstract val tilstand: DeltakelseTilstand
    abstract val status: DeltakelseStatus
}

@Serializable
enum class DeltakelseEierskap {
    ARENA,
    TEAM_KOMET,
    TEAM_TILTAK,
}

@Serializable
enum class DeltakelseTilstand {
    UTKAST,
    KLADD,
    AKTIV,
    AVSLUTTET,
}

@Serializable
data class DeltakelseStatus(
    val type: DataElement.Status,
    val aarsak: String?,
)

@Serializable
data class DeltakelsePeriode(
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
)

@Serializable
data class DeltakelseArena(
    @Serializable(with = UUIDSerializer::class)
    override val id: UUID,
    override val eierskap: DeltakelseEierskap,
    override val tilstand: DeltakelseTilstand,
    override val tittel: String,
    override val tiltakstypeNavn: String,
    @Serializable(with = LocalDateSerializer::class)
    override val innsoktDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    override val sistEndretDato: LocalDate?,
    override val periode: DeltakelsePeriode,
    override val status: DeltakelseStatus,
) : Deltakelse()

@Serializable
data class DeltakelseGruppetiltak(
    @Serializable(with = UUIDSerializer::class)
    override val id: UUID,
    override val eierskap: DeltakelseEierskap,
    override val tittel: String,
    override val tiltakstypeNavn: String,
    @Serializable(with = LocalDateSerializer::class)
    override val innsoktDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    override val sistEndretDato: LocalDate?,
    override val periode: DeltakelsePeriode,
    override val tilstand: DeltakelseTilstand,
    override val status: DeltakelseStatus,
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
) : Deltakelse()

@Serializable
data class DeltakelseArbeidsgiverAvtale(
    @Serializable(with = UUIDSerializer::class)
    override val id: UUID,
    override val eierskap: DeltakelseEierskap,
    override val tittel: String,
    override val tiltakstypeNavn: String,
    @Serializable(with = LocalDateSerializer::class)
    override val innsoktDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    override val sistEndretDato: LocalDate?,
    override val periode: DeltakelsePeriode,
    override val tilstand: DeltakelseTilstand,
    override val status: DeltakelseStatus,
) : Deltakelse()
