package no.nav.mulighetsrommet.api.domain.utdanning

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.serialization.Serializable

/**
 * Et [Utdanningsprogram] (Vg1-nivå) eier sine [Utdanning] (Vg2/Vg3-nivå)
 */
@Serializable
data class Utdanningsprogram private constructor(
    val programomradekode: String,
    val navn: String,
    val type: UtdanningsprogramType?,
    val nusKoder: List<String>,
    val utdanninger: List<Utdanning>,
) {
    companion object {
        fun opprett(
            programomradekode: String,
            navn: String,
            type: UtdanningsprogramType?,
            utdanninger: List<Utdanning>,
        ): Either<UtdanningsprogramError, Utdanningsprogram> {
            val nusKoder = UtdanningsprogramNusKoder.forProgramomradekode(programomradekode)
                ?: return UtdanningsprogramError.UkjentProgramomrade(programomradekode).left()

            val utdanningUtenforProgrammet = utdanninger.filter { it.utdanningslop.firstOrNull() != programomradekode }
            if (utdanningUtenforProgrammet.isNotEmpty()) {
                return UtdanningsprogramError.UtdanningTilhorerAnnetProgram(
                    programomradekode = programomradekode,
                    utdanningIder = utdanningUtenforProgrammet.map { it.utdanningId },
                ).left()
            }

            return Utdanningsprogram(
                programomradekode = programomradekode,
                navn = navn,
                type = type,
                nusKoder = nusKoder,
                utdanninger = utdanninger,
            ).right()
        }

        fun fromStorage(
            programomradekode: String,
            navn: String,
            type: UtdanningsprogramType?,
            nusKoder: List<String>,
            utdanninger: List<Utdanning>,
        ) = Utdanningsprogram(
            programomradekode = programomradekode,
            navn = navn,
            type = type,
            nusKoder = nusKoder,
            utdanninger = utdanninger,
        )
    }
}

@Serializable
enum class UtdanningsprogramType {
    YRKESFAGLIG,
    STUDIEFORBEREDENDE,
}

sealed interface UtdanningsprogramError {
    data class UkjentProgramomrade(val programomradekode: String) : UtdanningsprogramError

    data class UtdanningTilhorerAnnetProgram(
        val programomradekode: String,
        val utdanningIder: List<String>,
    ) : UtdanningsprogramError
}
