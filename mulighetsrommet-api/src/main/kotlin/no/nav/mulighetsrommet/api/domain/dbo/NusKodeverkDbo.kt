package no.nav.mulighetsrommet.api.domain.dbo

data class NusKodeverkDbo(
    val id: String,
    val name: String,
    val parent: String,
    val level: String,
    val version: String,
    val selfLink: String,
)
