package no.nav.mulighetsrommet.database

import kotliquery.TransactionalSession
import kotliquery.action.*
import java.sql.Array
import javax.sql.DataSource

interface Database {
    fun getDatasource(): DataSource

    fun isHealthy(): Boolean

    fun createArrayOf(arrayType: String, list: Collection<Any>): Array

    fun createTextArray(list: Collection<String>): Array

    fun <T> run(query: NullableResultQueryAction<T>): T?

    fun <T> run(query: ListResultQueryAction<T>): List<T>

    fun run(query: ExecuteQueryAction): Boolean

    fun run(query: UpdateQueryAction): Int

    fun run(query: UpdateAndReturnGeneratedKeyQueryAction): Long?

    fun <T> transaction(operation: (TransactionalSession) -> T): T
}
