package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class NavIdent(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "'NavIdent' should not be empty"
        }
    }
}
