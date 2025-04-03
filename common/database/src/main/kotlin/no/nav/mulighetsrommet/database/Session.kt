package no.nav.mulighetsrommet.database

import kotliquery.Query
import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import java.sql.Array
import java.sql.SQLException
import java.util.*

/**
 * Kjører [block] i kontekst av en [TransactionalSession], utledet fra [session] (som allerede kan være en [Session]
 * eller en [TransactionalSession]).
 */
inline fun <R> withTransaction(session: Session, block: TransactionalSession.() -> R): R {
    return if (session is TransactionalSession) {
        session.block()
    } else {
        session.transaction { it.block() }
    }
}

fun Session.createTextArray(list: Collection<Any>): Array {
    return createArrayOf("text", list)
}

inline fun <reified T> Session.createArrayFromSelector(
    list: Collection<T>,
    type: String = "text",
    valueSelector: (T) -> Any,
): Array {
    return createArrayOf(type, list.map(valueSelector))
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
