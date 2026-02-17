package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable
import java.security.MessageDigest

private val NORSK_IDENT_REGEX = "^\\d{11}$".toRegex()

@Serializable
@JvmInline
value class NorskIdent(val value: String) {
    init {
        require(isValid(value)) {
            "'NorskIdent' må være på formatet '$NORSK_IDENT_REGEX'"
        }
    }

    companion object {
        fun isValid(value: String): Boolean = NORSK_IDENT_REGEX.matches(value)

        fun parse(value: String): NorskIdent? = value.trim().takeIf(::isValid)?.let(::NorskIdent)
    }

    override fun toString() = "***********"
}

object NorskIdentHasher {
    fun hashIfNorskIdent(value: String): String {
        return NorskIdent.parse(value)?.let(::hash) ?: value
    }

    fun hash(ident: NorskIdent): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(ident.value.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
