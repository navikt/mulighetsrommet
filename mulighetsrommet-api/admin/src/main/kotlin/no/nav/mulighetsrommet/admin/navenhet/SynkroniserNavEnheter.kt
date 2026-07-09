package no.nav.mulighetsrommet.admin.navenhet

import no.nav.mulighetsrommet.admin.AdminDatabase
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhet
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetHelpers

data class SynkroniserNavEnheterCommand(
    val enheter: List<NavEnhet>,
)

class SynkroniserNavEnheterUseCase(
    private val db: AdminDatabase,
) {
    fun execute(command: SynkroniserNavEnheterCommand): List<NavEnhet> = db.transaction {
        command.enheter.map { enhet ->
            val enhetMedOverordnetEnhet = patchOverordnetEnhet(enhet)
            repository.navEnhet.save(enhetMedOverordnetEnhet)
            enhetMedOverordnetEnhet
        }
    }

    private fun patchOverordnetEnhet(enhet: NavEnhet): NavEnhet {
        val overordnetEnhet = enhet.overordnetEnhet
            ?: NavEnhetHelpers.finnOverordnetFylkeForSpesialenhet(enhet.enhetsnummer)
        return enhet.copy(overordnetEnhet = overordnetEnhet)
    }
}
