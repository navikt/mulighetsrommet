package no.nav.mulighetsrommet.database

import kotliquery.Session
import java.sql.Array

fun Session.createTextArray(list: List<String>): Array {
    return createArrayOf("text", list)
}
