package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dbo.ArenaDeltakerStatus
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerStatus
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
sealed class Tiltakshistorikk {
    abstract val norskIdent: NorskIdent
    abstract val opphav: Opphav
    abstract val startDato: LocalDate?
    abstract val sluttDato: LocalDate?

    enum class Opphav {
        ARENA,
        TEAM_KOMET,
        TEAM_TILTAK,
    }

    @Serializable
    data class Arrangor(
        val organisasjonsnummer: Organisasjonsnummer,
    )

    @Serializable
    data class Gjennomforing(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val tiltakskode: Tiltakskode,
    )

    @Serializable
    data class ArenaDeltakelse(
        override val norskIdent: NorskIdent,
        @Serializable(with = LocalDateSerializer::class)
        override val startDato: LocalDate?,
        @Serializable(with = LocalDateSerializer::class)
        override val sluttDato: LocalDate?,
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val arenaTiltakskode: String,
        val status: ArenaDeltakerStatus,
        val beskrivelse: String,
        val arrangor: Arrangor,
    ) : Tiltakshistorikk() {
        override val opphav = Opphav.ARENA
    }

    @Serializable
    data class GruppetiltakDeltakelse(
        override val norskIdent: NorskIdent,
        @Serializable(with = LocalDateSerializer::class)
        override val startDato: LocalDate?,
        @Serializable(with = LocalDateSerializer::class)
        override val sluttDato: LocalDate?,
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val status: AmtDeltakerStatus,
        val gjennomforing: Gjennomforing,
        val arrangor: Arrangor,
    ) : Tiltakshistorikk() {
        override val opphav = Opphav.TEAM_KOMET
    }

    // TODO: team tiltak
    @Serializable
    data class ArbeidsgiverAvtale(
        override val norskIdent: NorskIdent,
        @Serializable(with = LocalDateSerializer::class)
        override val startDato: LocalDate?,
        @Serializable(with = LocalDateSerializer::class)
        override val sluttDato: LocalDate?,
    ) : Tiltakshistorikk() {
        override val opphav = Opphav.TEAM_TILTAK
    }
}

@Serializable
data class TiltakshistorikkRequest(
    val identer: List<NorskIdent>,
    val maxAgeYears: Int?,
)

@Serializable
data class TiltakshistorikkResponse(
    val historikk: List<Tiltakshistorikk>,
)
