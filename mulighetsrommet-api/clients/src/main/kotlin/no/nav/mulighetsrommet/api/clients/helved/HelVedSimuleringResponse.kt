package no.nav.mulighetsrommet.api.clients.helved

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("@type")
sealed interface HelVedSimuleringResponse {
    @Serializable
    @SerialName("v1")
    data class V1(
        val oppsummeringer: List<OppsummeringForPeriode>,
        val detaljer: SimuleringDetaljer,
    ) : HelVedSimuleringResponse {
        @Serializable
        data class OppsummeringForPeriode(
            @Serializable(with = LocalDateSerializer::class)
            val fom: LocalDate,
            @Serializable(with = LocalDateSerializer::class)
            val tom: LocalDate,
            val tidligereUtbetalt: Int,
            val nyUtbetaling: Int,
            val totalEtterbetaling: Int,
            val totalFeilutbetaling: Int,
        )

        @Serializable
        data class SimuleringDetaljer(
            val gjelderId: String,
            @Serializable(with = LocalDateSerializer::class)
            val datoBeregnet: LocalDate,
            val totalBeløp: Int,
            val perioder: List<Periode>,
        )

        @Serializable
        data class Periode(
            @Serializable(with = LocalDateSerializer::class)
            val fom: LocalDate,
            @Serializable(with = LocalDateSerializer::class)
            val tom: LocalDate,
            val posteringer: List<Postering>,
        )

        @Serializable
        data class Postering(
            val fagområde: String,
            val sakId: String,
            @Serializable(with = LocalDateSerializer::class)
            val fom: LocalDate,
            @Serializable(with = LocalDateSerializer::class)
            val tom: LocalDate,
            val beløp: Int,
            val type: PosteringType,
            val klassekode: String,
        )

        @Serializable
        enum class PosteringType {
            YTELSE,
            FEILUTBETALING,
            FORSKUDSSKATT,
            JUSTERING,
            TREKK,
            MOTPOSTERING,
        }
    }

    @Serializable
    @SerialName("v2")
    data class V2(
        val perioder: List<Simuleringsperiode>,
    ) : HelVedSimuleringResponse {
        @Serializable
        data class Simuleringsperiode(
            @Serializable(with = LocalDateSerializer::class)
            val fom: LocalDate,
            @Serializable(with = LocalDateSerializer::class)
            val tom: LocalDate,
            val utbetalinger: List<SimulertUtbetaling>,
        )

        @Serializable
        data class SimulertUtbetaling(
            val fagsystem: Fagsystem,
            val sakId: String,
            val utbetalesTil: String,
            val stønadstype: String? = null,
            val tidligereUtbetalt: Int,
            val nyttBeløp: Int,
            val posteringer: List<Postering>,
        )

        @Serializable
        data class Postering(
            @Serializable(with = LocalDateSerializer::class)
            val fom: LocalDate,
            @Serializable(with = LocalDateSerializer::class)
            val tom: LocalDate,
            val beløp: Int,
            val type: Type,
            val klassekode: String,
        )

        @Serializable
        enum class Type {
            YTEL,
            FEIL,
            SKAT,
            JUST,
            TREK,
            MOTP,
        }
    }

    @Serializable
    @SerialName("info")
    data class Info(
        val status: Status,
        val fagsystem: Fagsystem,
        val message: String,
    ) : HelVedSimuleringResponse {
        @Serializable
        enum class Status {
            OK_UTEN_ENDRING,
            FEILET,
            UTILGJENGELIG,
            UGYLDIG_REQUEST,
        }
    }

    @Serializable
    enum class Fagsystem {
        VALP,
    }
}
