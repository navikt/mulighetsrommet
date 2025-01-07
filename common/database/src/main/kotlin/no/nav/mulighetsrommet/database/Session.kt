package no.nav.mulighetsrommet.database

import kotliquery.Query
import kotliquery.Row
import kotliquery.Session
import java.sql.Array
import java.sql.SQLException
import java.util.*

fun Session.createTextArray(list: Collection<Any>): Array {
    return createArrayOf("text", list)
}

fun Session.createUuidArray(list: Collection<UUID>): Array {
    return createArrayOf("uuid", list)
}

fun <T : Enum<T>> Session.createEnumArray(name: String, list: Collection<T>): Array {
    return createArrayOf(name, list.map { it.name })
}

fun <A> Session.requireSingle(query: Query, extractor: (Row) -> A): A {
    val result = single(query, extractor)
    return result ?: throw SQLException("Expected 1 row but received null")
}
