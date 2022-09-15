package no.nav.mulighetsrommet.secureLog

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SecureLog {
    companion object {
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
        fun getSecurelogger(): Logger {
            return secureLogger
        }
    }
}
