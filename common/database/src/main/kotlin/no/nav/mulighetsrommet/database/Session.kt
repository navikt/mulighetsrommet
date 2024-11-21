package no.nav.mulighetsrommet.database

import kotliquery.Session
import java.sql.Array
import java.util.*

fun Session.createTextArray(list: Collection<String>): Array {
    return createArrayOf("text", list)
}

fun Session.createUuidArray(list: Collection<UUID>): Array {
    return createArrayOf("uuid", list)
}

fun Session.createIntArray(list: Collection<Int>): Array {
    return createArrayOf("integer", list)
}
