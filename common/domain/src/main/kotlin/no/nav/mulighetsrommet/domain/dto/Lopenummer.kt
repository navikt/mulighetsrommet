package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable

private val LOPENUMMER_REGEX = "^(\\d{4})/(\\d+)$".toRegex()

@Serializable
@JvmInline
value class Lopenummer(val value: String) {
    init {
        require(LOPENUMMER_REGEX.matches(value)) {
            "The format of 'Lopenummer' is invalid. Expected '{year}/{id}'."
        }
    }

    fun getParts(): Pair<Int, Int> {
        val groupValues = LOPENUMMER_REGEX.matchEntire(value)?.groupValues ?: emptyList()
        val first = requireNotNull(groupValues.getOrNull(1)?.toInt()) {
            "'year' is missing from Lopenummer."
        }
        val second = requireNotNull(groupValues.getOrNull(2)?.toInt()) {
            "'id' is missing from Lopenummer."
        }
        return Pair(first, second)
    }
}
