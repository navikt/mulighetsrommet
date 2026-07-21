package no.nav.mulighetsrommet.admin.utdanning

import no.nav.mulighetsrommet.admin.AdminDatabase
import no.nav.mulighetsrommet.api.domain.utdanning.Utdanning
import no.nav.mulighetsrommet.api.domain.utdanning.Utdanningsprogram
import no.nav.mulighetsrommet.api.domain.utdanning.UtdanningsprogramError
import no.nav.mulighetsrommet.api.domain.utdanning.UtdanningsprogramType

data class SynkroniserUtdanningerCommand(
    val programomrader: List<Programomrade>,
    val utdanninger: List<Utdanning>,
) {
    data class Programomrade(
        val programomradekode: String,
        val navn: String,
        val type: UtdanningsprogramType?,
    )
}

class SynkroniserUtdanningerUseCase(
    private val db: AdminDatabase,
) {
    fun execute(command: SynkroniserUtdanningerCommand): List<UtdanningsprogramError> = db.transaction {
        val relevanteUtdanninger = command.utdanninger
            .filter { it.nusKoder.isNotEmpty() }
            .map { it.copy(navn = sanitizeNavn(it.navn)) }
            .groupBy { it.utdanningslop.first() }

        command.programomrader
            .filter { it.type == UtdanningsprogramType.YRKESFAGLIG }
            .mapNotNull { programomrade ->
                val utdanningerForProgram = relevanteUtdanninger[programomrade.programomradekode].orEmpty()

                val navn = sanitizeNavn(programomrade.navn)

                val utdanningsprogram = repository.utdanning.findByProgramomradekode(programomrade.programomradekode)
                    ?.oppdater(navn = navn, type = programomrade.type, utdanninger = utdanningerForProgram)
                    ?: Utdanningsprogram.opprett(
                        programomradekode = programomrade.programomradekode,
                        navn = navn,
                        type = programomrade.type,
                        utdanninger = utdanningerForProgram,
                    )

                utdanningsprogram.fold(
                    { error -> error },
                    { utdanningsprogram ->
                        repository.utdanning.save(utdanningsprogram)
                        null
                    },
                )
            }
    }
}

private fun sanitizeNavn(navn: String) = navn
    .replace("^Vg\\d ".toRegex(), "")
    .replace(" \\(opplæring i bedrift\\)$".toRegex(), "")
