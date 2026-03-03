package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable

private val JOURNALPOST_ID_REGEX = "^\\d+$".toRegex()

@Serializable
@JvmInline
value class JournalpostId(val value: String) {
    init {
        require(JOURNALPOST_ID_REGEX.matches(value)) {
            "Feil format på Journalpost-ID: $value"
        }
    }

    override fun toString(): String = value

    companion object {
        fun parse(value: String): JournalpostId? = value
            .takeIf { it.matches(JOURNALPOST_ID_REGEX) }
            ?.let { JournalpostId(it) }
    }
}
