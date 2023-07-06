package no.nav.mulighetsrommet.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period

object Tiltakshistorikk {
    val TiltakshistorikkTimePeriod = Period.ofYears(5)

    fun isRelevantTiltakshistorikk(date: LocalDateTime, today: LocalDate = LocalDate.now()): Boolean {
        val minmumTiltakshistorikkDate = today.minus(TiltakshistorikkTimePeriod)
        return !minmumTiltakshistorikkDate.isAfter(date.toLocalDate())
    }
}
