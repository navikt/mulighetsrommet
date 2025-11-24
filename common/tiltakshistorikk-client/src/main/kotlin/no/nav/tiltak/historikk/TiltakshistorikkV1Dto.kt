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
    abstract val tittel: String

    enum class Opphav {
        ARENA,
        TEAM_KOMET,
        TEAM_TILTAK,
    }

    @Serializable
    data class Virksomhet(
        val organisasjonsnummer: Organisasjonsnummer,
        val navn: String?,
    )

    @Serializable
    data class Arrangor(
        /**
         * Hovedenhet/Juridisk enhet hos arrangør (fra brreg)
         */
        val hovedenhet: Virksomhet?,

        /**
         * Underenhet hos arrangør (fra brreg) som tiltaksgjennomføringen er registrert på.
         *
         * MERK: Dette kan også være en "utenlandsk arrangør" fra Arena. En slik arrangør er
         * representert med et fiktivt organisasjonsnummer som begynner på "1" (i stedet for "8" eller "9").
         */
        val underenhet: Virksomhet,
    )

    @Serializable
    data class Gjennomforing(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String?,
        val deltidsprosent: Float?,
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
        override val tittel: String,
        val status: ArenaDeltakerStatus,
        val tiltakstype: Tiltakstype,
        val gjennomforing: Gjennomforing,
        val arrangor: Arrangor,
        val deltidsprosent: Float?,
        val dagerPerUke: Float?,
    ) : TiltakshistorikkV1Dto() {
        override val opphav = Opphav.ARENA

        @Serializable
        data class Tiltakstype(
            val tiltakskode: String,
            val navn: String,
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
        override val tittel: String,
        val status: DeltakerStatus,
        val tiltakstype: Tiltakstype,
        val gjennomforing: Gjennomforing,
        val arrangor: Arrangor,
        val deltidsprosent: Float?,
        val dagerPerUke: Float?,
    ) : TiltakshistorikkV1Dto() {
        override val opphav = Opphav.TEAM_KOMET

        @Serializable
        data class Tiltakstype(
            val tiltakskode: Tiltakskode,
            val navn: String,
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
        override val tittel: String,
        val tiltakstype: Tiltakstype,
        val status: ArbeidsgiverAvtaleStatus,
        val arbeidsgiver: Virksomhet,
    ) : TiltakshistorikkV1Dto() {
        override val opphav = Opphav.TEAM_TILTAK

        @Serializable
        data class Tiltakstype(
            val tiltakskode: Tiltakskode,
            val navn: String,
        )

        enum class Tiltakskode {
            ARBEIDSTRENING,
            MIDLERTIDIG_LONNSTILSKUDD,
            VARIG_LONNSTILSKUDD,
            MENTOR,
            INKLUDERINGSTILSKUDD,
            SOMMERJOBB,
            VTAO,
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
    val status: ArenaDeltakerStatus,
    @Serializable(with = LocalDateTimeSerializer::class)
    val startDato: LocalDateTime?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val sluttDato: LocalDateTime?,
    val dagerPerUke: Double?,
    val deltidsprosent: Double?,
)
