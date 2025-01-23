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

    override fun toString(): String {
        return "$javaClass: $error"
    }

    companion object {
        /**
         * Resolves a [DatabaseOperationError] instance from the provided [PSQLException].
         */
        fun fromPSQLException(error: PSQLException) = when {
            error.sqlState.startsWith("23") -> IntegrityConstraintViolation.fromPSQLException(error)

            else -> PostgresError(error)
        }
    }
}

class PostgresError(error: PSQLException) : DatabaseOperationError(error)

sealed class IntegrityConstraintViolation private constructor(error: PSQLException) : DatabaseOperationError(error) {
    class OtherViolation internal constructor(error: PSQLException) : IntegrityConstraintViolation(error)
    class RestrictViolation(error: PSQLException) : IntegrityConstraintViolation(error)
    class NotNullViolation(error: PSQLException) : IntegrityConstraintViolation(error)
    class ForeignKeyViolation(error: PSQLException) : IntegrityConstraintViolation(error)
    class UniqueViolation(error: PSQLException) : IntegrityConstraintViolation(error)
    class CheckViolation(error: PSQLException) : IntegrityConstraintViolation(error)
    class ExclusionViolation(error: PSQLException) : IntegrityConstraintViolation(error)

    companion object {
        fun fromPSQLException(error: PSQLException) = when (error.sqlState) {
            "23001" -> RestrictViolation(error)
            "23502" -> NotNullViolation(error)
            "23503" -> ForeignKeyViolation(error)
            "23505" -> UniqueViolation(error)
            "23514" -> CheckViolation(error)
            "23P01" -> ExclusionViolation(error)
            else -> {
                require(error.sqlState.startsWith("23")) {
                    "${error.sqlState} is not a known SQL Integrity Constraint Violation"
                }

                OtherViolation(error)
            }
        }
    }
}
