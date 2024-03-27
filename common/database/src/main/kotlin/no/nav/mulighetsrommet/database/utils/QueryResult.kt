package no.nav.mulighetsrommet.database.utils

import arrow.core.Either
import arrow.core.getOrElse
import org.postgresql.util.PSQLException

typealias QueryResult<T> = Either<DatabaseOperationError, T>

/**
 * Utility function to unwrap the value from the [QueryResult] when it's [Either.Right], or throwing the underlying
 * [PSQLException] when it's [Either.Left].
 *
 * It's best to avoid using this method, but it can be convenient during e.g. testing.
 */
fun <T> QueryResult<T>.getOrThrow(): T {
    return getOrElse { throw it.error }
}

/**
 * Runs the provided [queryRunner] and returns a [QueryResult] with the value [T] when it runs successfully,
 * or a [DatabaseOperationError] resolved from the underlying [PSQLException].
 */
fun <T> query(queryRunner: () -> T): QueryResult<T> = try {
    Either.Right(queryRunner.invoke())
} catch (e: PSQLException) {
    Either.Left(DatabaseOperationError.fromPSQLException(e))
}

/**
 * The [DatabaseOperationError] can be used for more readable and explicit error handling of database operations
 * that result in a [PSQLException].
 *
 * See the postgres documentation [0] for description of all error codes if more error variants are needed.
 *
 * [0]: https://www.postgresql.org/docs/current/errcodes-appendix.html
 */
sealed class DatabaseOperationError(val error: PSQLException) {
    class ForeignKeyViolation(error: PSQLException) : DatabaseOperationError(error)
    class DatabaseError(error: PSQLException) : DatabaseOperationError(error)

    override fun toString(): String {
        return "$javaClass: $error"
    }

    companion object {
        /**
         * Resolves a [DatabaseOperationError] instance from the provided [PSQLException].
         */
        fun fromPSQLException(error: PSQLException) = when (error.sqlState) {
            "23503" -> ForeignKeyViolation(error)
            else -> DatabaseError(error)
        }
    }
}
