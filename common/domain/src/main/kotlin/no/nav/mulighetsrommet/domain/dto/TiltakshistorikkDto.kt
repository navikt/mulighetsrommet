package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.SerialName
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
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val status: AmtDeltakerStatus,
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
        @Serializable(with = UUIDSerializer::class)
        val avtaleId: UUID,
        val tiltakstype: Tiltakstype,
        val status: Status,
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

        enum class Status {
            /**
             * Tiltaket er påbegynt, men kan fortsatt mangle noe data som er påkrevd for at det skal kunne gjennomføres.
             * Kan anses som en "kladd".
             */
            PAABEGYNT,

            /**
             * Bl.a. når man mangler godkjenning av "controller", men kan muligens også være andre godkjenninger som
             * kreves.
             */
            MANGLER_GODKJENNING,

            /**
             * Status er basert på startdato for avtale. Kan anta at avtalen er klar, men startdato er i fremtiden.
             */
            KLAR_FOR_OPPSTART,

            /**
             * Avtale gjennomføres.
             * Bruker deltar på tiltaket.
             */
            GJENNOMFORES,

            /**
             * Avtale har blitt avsluttet.
             * Bruker har deltatt på tiltaket.
             */
            AVSLUTTET,

            /**
             * Avtale ble avbrutt.
             */
            AVBRUTT,

            /**
             * Tiltaket ble aldri noe av.
             */
            ANNULLERT,
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
