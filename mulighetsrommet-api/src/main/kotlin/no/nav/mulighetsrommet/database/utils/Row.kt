package no.nav.mulighetsrommet.database.utils

import kotliquery.Row
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate

fun Row.periode(property: String): Periode {
    val periode = string(property)
    val (start, end) = periode.removeSurrounding("[", ")").split(",")
    return Periode(LocalDate.parse(start.trim()), LocalDate.parse(end.trim()))
}
