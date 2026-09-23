package no.nav.mulighetsrommet.admin

interface AdminDatabase {
    fun <T> session(block: QueryContext.() -> T): T
    fun <T> transaction(block: QueryContext.() -> T): T
}
