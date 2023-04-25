package no.nav.mulighetsrommet.api.clients.msgraph

data class AnsattDataDTO(
    val hovedenhetKode: String,
    val hovedenhetNavn: String,
    val fornavn: String,
    val etternavn: String,
    val navident: String,
)
