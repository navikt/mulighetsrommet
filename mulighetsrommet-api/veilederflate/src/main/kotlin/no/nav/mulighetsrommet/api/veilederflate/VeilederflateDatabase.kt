package no.nav.mulighetsrommet.api.veilederflate

interface VeilederflateDatabase {
    fun <T> session(block: QueryContext.() -> T): T
    fun <T> transaction(block: QueryContext.() -> T): T
    suspend fun <T> suspendSession(block: suspend QueryContext.() -> T): T
    suspend fun <T> suspendTransaction(block: suspend QueryContext.() -> T): T
}
