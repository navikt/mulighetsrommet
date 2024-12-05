package no.nav.mulighetsrommet.tasks

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.Task
import java.io.PrintWriter
import java.sql.Connection
import javax.sql.DataSource

// A client that can be used in a transaction by preventing the connection from being closed.
// The user is responsible for managing the connection lifecycle and closing it after use.
// This approach is necessary because the `create` call and other operations on the SchedulerClient
// closes the connection internally.
fun <T> transactionalSchedulerClient(task: Task<T>, connection: Connection): SchedulerClient {
    return SchedulerClient.Builder
        .create(UncloseableDataSource(connection), task)
        .serializer(DbSchedulerKotlinSerializer())
        .build()
}

class UncloseableDataSource(private val connection: Connection) : DataSource {
    override fun getConnection(): Connection {
        return object : Connection by connection {
            override fun close() {
                // Do nothing
            }
        }
    }

    override fun getConnection(username: String?, password: String?): Connection {
        throw UnsupportedOperationException("Connection with username and password is not supported.")
    }

    override fun getLogWriter(): PrintWriter? {
        return null // Optional: Implement if needed
    }

    override fun setLogWriter(out: PrintWriter?) {
        throw UnsupportedOperationException("LogWriter is not supported.")
    }

    override fun getLoginTimeout(): Int {
        return 0 // Optional: Implement if needed
    }

    override fun setLoginTimeout(seconds: Int) {
        throw UnsupportedOperationException("LoginTimeout is not supported.")
    }

    override fun getParentLogger(): java.util.logging.Logger {
        throw UnsupportedOperationException("ParentLogger is not supported.")
    }

    override fun <T : Any?> unwrap(iface: Class<T>?): T {
        throw UnsupportedOperationException("Unsupported unwrap operation.")
    }

    override fun isWrapperFor(iface: Class<*>?): Boolean {
        return false
    }
}
