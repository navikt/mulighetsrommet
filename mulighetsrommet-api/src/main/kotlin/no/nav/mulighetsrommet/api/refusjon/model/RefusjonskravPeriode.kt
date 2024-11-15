package no.nav.mulighetsrommet.api.refusjon.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Representerer en periode inklusiv [start] og eksklusiv [slutt].
 */
@Serializable
data class RefusjonskravPeriode(
    @Serializable(with = LocalDateSerializer::class)
    val start: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val slutt: LocalDate,
) {
    init {
        require(start < slutt) {
            "start ($start) må være mindre enn slutt ($slutt)"
        }
    }

    companion object {
        fun fromDayInMonth(dayInMonth: LocalDate): RefusjonskravPeriode {
            val periodeStart = dayInMonth.with(TemporalAdjusters.firstDayOfMonth())
            return RefusjonskravPeriode(periodeStart, periodeStart.plusMonths(1))
        }
    }

    fun getDurationInDays(): Long {
        return ChronoUnit.DAYS.between(start, slutt)
    }

    fun getLastDate(): LocalDate {
        return slutt.plusDays(1)
    }
}
