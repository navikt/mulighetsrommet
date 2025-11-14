package no.nav.mulighetsrommet.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class TimelineDto(
    @Serializable(with = LocalDateSerializer::class)
    val startDate: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val endDate: LocalDate,
    val rows: List<Row>,
) {
    @Serializable
    data class Row(
        val periods: List<Period>,
        val label: String,
    ) {
        @Serializable
        data class Period(
            val key: String,
            @Serializable(with = LocalDateSerializer::class)
            val start: LocalDate,
            @Serializable(with = LocalDateSerializer::class)
            val end: LocalDate,
            val status: Variant,
            val content: String,
            val hover: String,
        ) {
            enum class Variant {
                @SerialName("info")
                INFO,

                @SerialName("success")
                SUCCESS,

                @SerialName("warning")
                WARNING,

                @SerialName("danger")
                DANGER,

                @SerialName("neutral")
                NEUTRAL,
            }
        }
    }
}
