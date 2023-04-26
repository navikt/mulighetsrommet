package no.nav.mulighetsrommet.api.utils

import io.ktor.server.plugins.*
import no.nav.mulighetsrommet.api.clients.norg2.Norg2EnhetDto
import no.nav.mulighetsrommet.api.clients.norg2.Norg2EnhetStatus
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Response

object SanityUtils {
    fun isUnderliggendeEnhet(fylke: Norg2EnhetDto, enhet: Norg2Response): Boolean {
        return relevanteStatuser(enhet.enhet.status) && enhet.overordnetEnhet == fylke.enhetNr
    }

    fun relevanteStatuser(status: Norg2EnhetStatus): Boolean {
        val relevanteStatuser =
            listOf(
                Norg2EnhetStatus.UNDER_ETABLERING.name,
                Norg2EnhetStatus.UNDER_AVVIKLING.name,
                Norg2EnhetStatus.AKTIV.name,
            )
        return relevanteStatuser.contains(status.name)
    }

    fun toEnhetId(enhet: Norg2EnhetDto): String {
        return "enhet.${enhet.type.name.lowercase()}.${enhet.enhetNr}"
    }

    fun toType(type: String?): String {
        return when (type) {
            "FYLKE" -> type.capitalize()
            "LOKAL" -> type.capitalize()
            "ALS" -> type.capitalize()
            "TILTAK" -> type.capitalize()
            else -> throw BadRequestException("'$type' er ikke en gyldig type for enhet. Gyldige typer er 'FYLKE', 'LOKAL', 'ALS', 'TILTAK'.")
        }
    }

    fun toStatus(status: String?): String {
        return when (status) {
            "AKTIV" -> status.capitalize()
            "NEDLAGT" -> status.capitalize()
            "UNDER_ETABLERING" -> status.capitalize()
            "UNDER_AVVIKLING" -> status.capitalize()
            else -> throw BadRequestException("'$status' er ikke en gyldig status. Gyldige statuser er 'AKTIV', 'NEDLAGT', 'UNDER_ETABLERING', 'UNDER_AVVIKLING'")
        }
    }

    private fun String.capitalize(): String {
        return this.lowercase().replaceFirstChar { it.uppercase() }
    }
}
