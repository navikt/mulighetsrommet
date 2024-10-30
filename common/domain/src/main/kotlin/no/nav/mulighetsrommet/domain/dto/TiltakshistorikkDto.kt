package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
sealed class Tiltakshistorikk {
    abstract val norskIdent: NorskIdent
    abstract val opphav: Opphav
    abstract val startDato: LocalDate?
    abstract val sluttDato: LocalDate?
    abstract val registrertTidspunkt: LocalDateTime

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
    data class Arbeidsgiver(
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
    @SerialName("ArenaDeltakelse")
    data class ArenaDeltakelse(
        override val norskIdent: NorskIdent,
        @Serializable(with = LocalDateSerializer::class)
        override val startDato: LocalDate?,
        @Serializable(with = LocalDateSerializer::class)
        override val sluttDato: LocalDate?,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val registrertTidspunkt: LocalDateTime,
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
    @SerialName("GruppetiltakDeltakelse")
    data class GruppetiltakDeltakelse(
        override val norskIdent: NorskIdent,
        @Serializable(with = LocalDateSerializer::class)
        override val startDato: LocalDate?,
        @Serializable(with = LocalDateSerializer::class)
        override val sluttDato: LocalDate?,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val registrertTidspunkt: LocalDateTime,
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val status: DeltakerStatus,
        val gjennomforing: Gjennomforing,
        val arrangor: Arrangor,
    ) : Tiltakshistorikk() {
        override val opphav = Opphav.TEAM_KOMET
    }

    @Serializable
    @SerialName("ArbeidsgiverAvtale")
    data class ArbeidsgiverAvtale(
        override val norskIdent: NorskIdent,
        @Serializable(with = LocalDateSerializer::class)
        override val startDato: LocalDate?,
        @Serializable(with = LocalDateSerializer::class)
        override val sluttDato: LocalDate?,
        @Serializable(with = LocalDateTimeSerializer::class)
        override val registrertTidspunkt: LocalDateTime,
        @Serializable(with = UUIDSerializer::class)
        val avtaleId: UUID,
        val tiltakstype: Tiltakstype,
        val status: ArbeidsgiverAvtaleStatus,
        val arbeidsgiver: Arbeidsgiver,
    ) : Tiltakshistorikk() {
        override val opphav = Opphav.TEAM_TILTAK

        enum class Tiltakstype {
            ARBEIDSTRENING,
            MIDLERTIDIG_LONNSTILSKUDD,
            VARIG_LONNSTILSKUDD,
            MENTOR,
            INKLUDERINGSTILSKUDD,
            SOMMERJOBB,
        }
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
    val meldinger: Set<TiltakshistorikkMelding>,
)

enum class TiltakshistorikkMelding {
    MANGLER_HISTORIKK_FRA_TEAM_TILTAK,
    HENTER_IKKE_HISTORIKK_FRA_TEAM_TILTAK,
}
