package no.nav.tiltak.historikk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.ArbeidsgiverAvtaleStatus
import no.nav.mulighetsrommet.model.ArenaDeltakerStatus
import no.nav.mulighetsrommet.model.DeltakerStatusAarsakType
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
sealed class TiltakshistorikkV1Dto {
    /**
     * Id på deltakelse fra kildesystemet.
     *
     * MERK: Hvis kildesystemet er Arena så vil dette være en id som kun er kjent i `tiltakshistorikk`,
     * id fra Arena er tilgjengelig i feltet [TiltakshistorikkV1Dto.ArenaDeltakelse.arenaId].
     */
    abstract val id: UUID

    /**
     * Fødselsnummer til bruker.
     */
    abstract val norskIdent: NorskIdent

    /**
     * Hvilket kildesystem deltakelsen kommer fra.
     */
    abstract val opphav: Opphav

    /**
     * Startdato i tiltaket.
     */
    abstract val startDato: LocalDate?

    /**
     * Sluttdato i tiltaket.
     */
    abstract val sluttDato: LocalDate?

    /**
     * Beskrivende tittel/leslig navn for tiltaksdeltakelsen.
     *
     * Dette vises bl.a. til veileder i Modia og til bruker i aktivitetsplanen (for noen tiltak), og vil typisk være på
     * formatet "<tiltakstype> hos <arrangør>", f.eks. "Oppfølging hos Arrangør AS".
     *
     * Selve innholdet/oppbygning av tittelen kan variere mellom de forskjellige tiltakstypene og det kan komme
     * endringer i logikken på hvordan dette utledes.
     */
    abstract val tittel: String

    enum class Opphav {
        ARENA,
        TEAM_KOMET,
        TEAM_TILTAK,
    }

    @Serializable
    data class Virksomhet(
        val organisasjonsnummer: Organisasjonsnummer,
        /**
         * Navn på virksomhet vil stort sett være tilgjenglig, men i noen sjeldne tilfeller så kan det være
         * at dette er ukjent (typisk for eldre tiltaksdeltakelser).
         */
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
        /**
         * Navn på tiltaksgjennomføringen, enten fra Tiltaksadministrasjon eller fra Arena.
         *
         * MERK: Dette feltet inneholder fritekst og for eldre tiltaksdeltakelser fra Arena så kan det være persondata
         * i dette feltet. Vurder om dette trengs eller om f.eks. [TiltakshistorikkV1Dto.tittel] er mer egnet til
         * formålet (som f.eks. til visning i frontend).
         */
        val navn: String?,

        /**
         * Deltidsprosent kan være definert på gjennomføringen og vil i så fall gjelde for alle deltakelser
         * på tiltaket (med mindre en annen deltidsprosent er definert på deltakelsen).
         */
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
        val arenaId: Int,
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
    @SerialName("TeamKometDeltakelse")
    data class TeamKometDeltakelse(
        override val norskIdent: NorskIdent,
        @Serializable(with = LocalDateSerializer::class)
        override val startDato: LocalDate?,
        @Serializable(with = LocalDateSerializer::class)
        override val sluttDato: LocalDate?,
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val tittel: String,
        val status: Status,
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

        @Serializable
        data class Status(
            val type: DeltakerStatusType,
            val aarsak: DeltakerStatusAarsakType?,
            // TODO endre til `opprettetTidspunkt`, men avklare med konsumenter først
            @Serializable(with = LocalDateTimeSerializer::class)
            val opprettetDato: LocalDateTime,
        )
    }

    @Serializable
    @SerialName("TeamTiltakAvtale")
    data class TeamTiltakAvtale(
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
        val stillingsprosent: Float?,
        val dagerPerUke: Float?,
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
    @Serializable(with = UUIDSerializer::class)
    val tiltakstypeId: UUID,
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
    val arenaDeltakerId: Int,
    val norskIdent: NorskIdent,
    val status: ArenaDeltakerStatus,
    @Serializable(with = LocalDateTimeSerializer::class)
    val startDato: LocalDateTime?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val sluttDato: LocalDateTime?,
    val dagerPerUke: Double?,
    val deltidsprosent: Double?,
)
