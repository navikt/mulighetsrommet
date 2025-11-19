package no.nav.tiltak.historikk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
sealed class TiltakshistorikkV1Dto {
    abstract val id: UUID
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
        val navn: String? = null,
    )

    @Serializable
    data class Arbeidsgiver(
        val organisasjonsnummer: String,
        val navn: String? = null,
    )

    @Serializable
    data class Gjennomforing(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String?,
        val deltidsprosent: Float? = null,
    )

    @Serializable
    @SerialName("ArenaDeltakelse")
    data class ArenaDeltakelse(
        override val norskIdent: NorskIdent,
        @Serializable(with = LocalDateSerializer::class)
        override val startDato: LocalDate?,
        @Serializable(with = LocalDateSerializer::class)
        override val sluttDato: LocalDate?,
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        val tiltakstype: Tiltakstype,
        val status: ArenaDeltakerStatus,
        val beskrivelse: String,
        val arrangor: Arrangor,
        val deltidsprosent: Float? = null,
        val dagerPerUke: Float? = null,
    ) : TiltakshistorikkV1Dto() {
        override val opphav = Opphav.ARENA

        @Serializable
        data class Tiltakstype(
            val tiltakskode: String,
            val navn: String?,
        )
    }

    @Serializable
    @SerialName("GruppetiltakDeltakelse")
    data class GruppetiltakDeltakelse(
        override val norskIdent: NorskIdent,
        @Serializable(with = LocalDateSerializer::class)
        override val startDato: LocalDate?,
        @Serializable(with = LocalDateSerializer::class)
        override val sluttDato: LocalDate?,
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        val status: DeltakerStatus,
        val tiltakstype: Tiltakstype,
        val gjennomforing: Gjennomforing,
        val arrangor: Arrangor,
        val deltidsprosent: Float? = null,
        val dagerPerUke: Float? = null,
    ) : TiltakshistorikkV1Dto() {
        override val opphav = Opphav.TEAM_KOMET

        @Serializable
        data class Tiltakstype(
            val tiltakskode: Tiltakskode,
            val navn: String?,
        )
    }

    @Serializable
    @SerialName("ArbeidsgiverAvtale")
    data class ArbeidsgiverAvtale(
        override val norskIdent: NorskIdent,
        @Serializable(with = LocalDateSerializer::class)
        override val startDato: LocalDate?,
        @Serializable(with = LocalDateSerializer::class)
        override val sluttDato: LocalDate?,
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        val tiltakstype: Tiltakstype,
        val status: ArbeidsgiverAvtaleStatus,
        val arbeidsgiver: Arbeidsgiver,
    ) : TiltakshistorikkV1Dto() {
        override val opphav = Opphav.TEAM_TILTAK

        @Serializable
        data class Tiltakstype(
            val tiltakskode: Tiltakskode,
            val navn: String?,
        )

        enum class Tiltakskode {
            ARBEIDSTRENING,
            MIDLERTIDIG_LONNSTILSKUDD,
            VARIG_LONNSTILSKUDD,
            MENTOR,
            INKLUDERINGSTILSKUDD,
            SOMMERJOBB,
            VARIG_TILRETTELAGT_ARBEID_ORDINAR,
        }
    }
}

@Serializable
data class TiltakshistorikkV1Request(
    val identer: List<NorskIdent>,
    val maxAgeYears: Int?,
)

@Serializable
data class TiltakshistorikkV1Response(
    val historikk: List<TiltakshistorikkV1Dto>,
    val meldinger: Set<TiltakshistorikkMelding>,
)

enum class TiltakshistorikkMelding {
    MANGLER_HISTORIKK_FRA_TEAM_TILTAK,
}

@Serializable
data class TiltakshistorikkArenaGjennomforing(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val arenaTiltakskode: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val arenaRegDato: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val arenaModDato: LocalDateTime,
    val navn: String,
    val arrangorOrganisasjonsnummer: Organisasjonsnummer,
    val deltidsprosent: Double,
)

@Serializable
data class TiltakshistorikkArenaDeltaker(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = LocalDateTimeSerializer::class)
    val arenaRegDato: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val arenaModDato: LocalDateTime,
    @Serializable(with = UUIDSerializer::class)
    val arenaGjennomforingId: UUID,
    val norskIdent: NorskIdent,
    val arenaTiltakskode: String,
    val status: ArenaDeltakerStatus,
    @Serializable(with = LocalDateTimeSerializer::class)
    val startDato: LocalDateTime?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val sluttDato: LocalDateTime?,
    val beskrivelse: String,
    val arrangorOrganisasjonsnummer: Organisasjonsnummer,
    val dagerPerUke: Double?,
    val deltidsprosent: Double?,
)

@Serializable
data class TiltakshistorikkArenaDeltakerGjennomforingId(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val arenaGjennomforingId: UUID,
)
