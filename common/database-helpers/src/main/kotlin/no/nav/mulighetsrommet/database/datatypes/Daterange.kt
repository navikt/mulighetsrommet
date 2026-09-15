package no.nav.mulighetsrommet.database.datatypes

import kotliquery.Row
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate

fun Row.periode(property: String): Periode {
    val periode = string(property)
    return periode.toPeriode()
}

fun String.toPeriode(): Periode {
    val (start, end) = this.removeSurrounding("[", ")").split(",")
    return Periode(LocalDate.parse(start.trim()), LocalDate.parse(end.trim()))
}

fun Periode.toDaterange(): String {
    return "[$start,$slutt)"
}
