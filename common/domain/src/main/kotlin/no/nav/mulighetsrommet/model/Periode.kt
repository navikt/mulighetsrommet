package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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

    companion object {
        /**
         * Oppretter en [Periode] hvis startdatoen er før sluttdatoen, ellers null.
         *
         * @param start Startdatoen for perioden.
         * @param slutt Sluttdatoen for perioden.
         */
        fun of(start: LocalDate, slutt: LocalDate): Periode? {
            return if (start < slutt) Periode(start, slutt) else null
        }

        /**
         * Oppretter en [Periode] for måneden til den gitte datoen.
         *
         * @param date Datoen som måneden skal baseres på.
         */
        fun forMonthOf(date: LocalDate): Periode {
            val periodeStart = date.with(TemporalAdjusters.firstDayOfMonth())
            return Periode(periodeStart, periodeStart.plusMonths(1))
        }

        /**
         * Oppretter en [Periode] fra og med den gitte startdatoen til og med den gitte sluttdatoen.
         *
         * @param inclusiveStart Startdatoen for perioden (inkludert).
         * @param inclusiveEnd Sluttdatoen for perioden (inkludert).
         */
        fun fromInclusiveDates(inclusiveStart: LocalDate, inclusiveEnd: LocalDate): Periode {
            return Periode(inclusiveStart, inclusiveEnd.plusDays(1))
        }

        /**
         * Lager en Periode som spenner fra tidligste start til seneste slutt i en liste av perioder.
         */
        fun fromRange(perioder: Collection<Periode>): Periode {
            require(perioder.isNotEmpty()) { "Kan ikke lage Periode fra tom liste" }
            val earliestStart = perioder.minOf { it.start }
            val latestEnd = perioder.maxOf { it.slutt }
            return Periode(earliestStart, latestEnd)
        }
    }

    override fun compareTo(other: Periode): Int {
        return compareValuesBy(this, other, Periode::start, Periode::slutt)
    }

    operator fun contains(date: LocalDate): Boolean {
        return date.isEqual(start) || date.isAfter(start) && date.isBefore(slutt)
    }

    operator fun contains(periode: Periode): Boolean {
        return periode.start in this && periode.getLastInclusiveDate() in this
    }

    fun getDurationInDays(): Long {
        return ChronoUnit.DAYS.between(start, slutt)
    }

    fun getLastInclusiveDate(): LocalDate {
        return slutt.minusDays(1)
    }

    fun intersects(periode: Periode): Boolean {
        val start = maxOf(start, periode.start)
        val slutt = minOf(slutt, periode.slutt)
        return start < slutt
    }

    fun intersect(periode: Periode): Periode? {
        val start = maxOf(start, periode.start)
        val slutt = minOf(slutt, periode.slutt)
        return if (start < slutt) {
            Periode(start, slutt)
        } else {
            null
        }
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

    fun subtractPeriods(exclusions: List<Periode>): List<Periode> {
        if (exclusions.isEmpty()) {
            return listOf(this)
        }

        val sortedExclusions = exclusions.sortedBy { it.start }

        val result = mutableListOf<Periode>()
        var currentStart = this.start
        val periodeEnd = this.slutt

        for (exclusion in sortedExclusions) {
            if (exclusion.slutt <= currentStart) {
                continue
            }
            if (exclusion.start >= periodeEnd) {
                break
            }
            if (exclusion.start > currentStart) {
                result.add(Periode(currentStart, exclusion.start))
            }

            currentStart = maxOf(currentStart, exclusion.slutt)
            if (currentStart >= periodeEnd) {
                break
            }
        }

        if (currentStart < periodeEnd) {
            result.add(Periode(currentStart, periodeEnd))
        }

        return result
    }

    fun formatPeriode(): String = "${formatDate(start)} - ${formatDate(getLastInclusiveDate())}"

    private fun formatDate(localDate: LocalDate): String = localDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
}
