package no.nav.mulighetsrommet.api.application

interface ApiDatabase {
    fun <T> session(block: QueryContext.() -> T): T
    fun <T> transaction(block: QueryContext.() -> T): T
}
