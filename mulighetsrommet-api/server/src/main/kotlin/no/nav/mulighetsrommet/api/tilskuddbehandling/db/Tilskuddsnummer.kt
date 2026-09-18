package no.nav.mulighetsrommet.api.tilskuddbehandling.db

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Tiltaksnummer

@Serializable
@JvmInline
value class Tilskuddsnummer(val value: String) {
    init {
        try {
            val (tiltaksnummer, lopenummer) = value.split("-")
            Tiltaksnummer(tiltaksnummer)
            val _ = lopenummer.toInt()
        } catch (e: Exception) {
            throw IllegalArgumentException("The format of 'Tilskuddsnummer' is invalid. Expected '{tiltaksnummer}-{lopenummer}'", e)
        }
    }

    val tiltaksnummer: Tiltaksnummer
        get() = value.split("-").first().let { Tiltaksnummer(it) }

    val lopenummer: Int
        get() = value.split("-").last().toInt()

    override fun toString(): String = value

    fun toFreeTextSearch(): List<String> = listOfNotNull(
        value,
        tiltaksnummer.toFreeTextSearch().toString(),
        lopenummer.toString(),
    )
}
