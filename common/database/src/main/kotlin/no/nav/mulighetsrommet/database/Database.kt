package no.nav.mulighetsrommet.database

import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.action.*
import java.sql.Array
import java.util.*
import javax.sql.DataSource

interface Database {

    fun getDatasource(): DataSource

    fun isHealthy(): Boolean

    fun <T> useSession(operation: (Session) -> T): T

    fun createArrayOf(arrayType: String, list: Collection<Any>): Array {
        return useSession {
            it.createArrayOf(arrayType, list)
        }
    }

    fun createTextArray(list: Collection<String>): Array {
        return createArrayOf("text", list)
    }

    fun createUuidArray(list: Collection<UUID>): Array {
        return createArrayOf("uuid", list)
    }

    fun createIntArray(list: Collection<Int>): Array {
        return createArrayOf("integer", list)
    }

    fun <T> run(query: NullableResultQueryAction<T>): T? {
        return useSession {
            it.run(query)
        }
    }

    fun <T> run(query: ListResultQueryAction<T>): List<T> {
        return useSession {
            it.run(query)
        }
    }

    fun run(query: ExecuteQueryAction): Boolean {
        return useSession {
            it.run(query)
        }
    }

    fun run(query: UpdateQueryAction): Int {
        return useSession {
            it.run(query)
        }
    }

    fun run(query: UpdateAndReturnGeneratedKeyQueryAction): Long? {
        return useSession {
            it.run(query)
        }
    }

    fun <T> transaction(operation: (TransactionalSession) -> T): T {
        return useSession {
            it.transaction(operation)
        }
    }

    suspend fun <T> transactionSuspend(operation: suspend (TransactionalSession) -> T): T
}
