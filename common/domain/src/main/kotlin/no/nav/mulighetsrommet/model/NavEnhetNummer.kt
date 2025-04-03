package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class NavEnhetNummer(val value: String) {
    @Override
    override fun toString(): String = value
}
