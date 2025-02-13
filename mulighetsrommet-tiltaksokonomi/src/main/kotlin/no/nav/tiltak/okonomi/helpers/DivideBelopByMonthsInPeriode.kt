package no.nav.tiltak.okonomi.helpers

import no.nav.mulighetsrommet.model.Periode
import kotlin.math.floor

fun divideBelopByMonthsInPeriode(bestillingsperiode: Periode, belop: Int): List<Pair<Periode, Int>> {
    val monthlyPeriods = bestillingsperiode.splitByMonth()

    val belopPerDay = belop.toDouble() / bestillingsperiode.getDurationInDays()

    val belopByMonth = monthlyPeriods
        .associateWith { floor(belopPerDay * it.getDurationInDays()).toInt() }
        .toSortedMap()

    val remainder = belop - belopByMonth.values.sum()
    if (remainder > 0) {
        val firstPeriod = monthlyPeriods.first()
        belopByMonth[firstPeriod] = belopByMonth.getValue(firstPeriod) + remainder
    }

    return belopByMonth.toList()
}
