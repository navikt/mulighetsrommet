package no.nav.mulighetsrommet.api.domain.dto

data class NavAnsattDto(
    val navident: String,
    val fornavn: String,
    val etternavn: String,
    val hovedenhetKode: String,
    val hovedenhetNavn: String,
)
