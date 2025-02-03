package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Representerer en periode inklusiv [start] og eksklusiv [slutt].
 */
@Serializable
data class Periode(
    @Serializable(with = LocalDateSerializer::class)
    val start: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val slutt: LocalDate,
) : Comparable<Periode> {
    init {
        require(start < slutt) {
            "start ($start) må være mindre enn slutt ($slutt)"
        }
    }

    override fun compareTo(other: Periode): Int {
        return compareValuesBy(this, other, Periode::start, Periode::slutt)
    }

    companion object {
        fun forMonthOf(date: LocalDate): Periode {
            val periodeStart = date.with(TemporalAdjusters.firstDayOfMonth())
            return Periode(periodeStart, periodeStart.plusMonths(1))
        }

        fun fromInclusiveDates(inclusiveStart: LocalDate, inclusiveEnd: LocalDate): Periode {
            return Periode(inclusiveStart, inclusiveEnd.plusDays(1))
        }
    }

    fun getDurationInDays(): Long {
        return ChronoUnit.DAYS.between(start, slutt)
    }

    fun getLastDate(): LocalDate {
        return slutt.minusDays(1)
    }

    operator fun contains(date: LocalDate): Boolean {
        return date == start || date.isAfter(start) && date.isBefore(slutt)
    }

    fun splitByMonth(): List<Periode> {
        val perioder = mutableListOf<Periode>()
        var currentDate = start

        while (currentDate < slutt) {
            val endOfMonth = currentDate.with(TemporalAdjusters.lastDayOfMonth()).plusDays(1)
            val monthEnd = minOf(endOfMonth, slutt)
            perioder.add(Periode(currentDate, monthEnd))
            currentDate = monthEnd
        }

        return perioder
    }
}
