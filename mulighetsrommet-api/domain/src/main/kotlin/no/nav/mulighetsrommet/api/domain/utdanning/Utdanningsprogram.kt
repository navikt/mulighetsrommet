package no.nav.mulighetsrommet.api.domain.utdanning

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

/**
 * Et [Utdanningsprogram] (Vg1-nivå) eier sine [Utdanning] (Vg2/Vg3-nivå)
 */
@Serializable
data class Utdanningsprogram private constructor(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val programomradekode: String,
    val navn: String,
    val type: UtdanningsprogramType?,
    val nusKoder: List<String>,
    val utdanninger: List<Utdanning>,
) {
    fun oppdater(
        navn: String,
        type: UtdanningsprogramType?,
        utdanninger: List<Utdanning>,
    ): Either<UtdanningsprogramError, Utdanningsprogram> = valider(programomradekode, utdanninger).map { nusKoder ->
        val eksisterendeIdForUtdanningId = this.utdanninger.associate { it.utdanningId to it.id }
        val gjenbrukteUtdanninger = utdanninger.map { utdanning ->
            eksisterendeIdForUtdanningId[utdanning.utdanningId]?.let { utdanning.copy(id = it) } ?: utdanning
        }
        copy(navn = navn, type = type, nusKoder = nusKoder, utdanninger = gjenbrukteUtdanninger)
    }

    companion object {
        fun opprett(
            programomradekode: String,
            navn: String,
            type: UtdanningsprogramType?,
            utdanninger: List<Utdanning>,
        ): Either<UtdanningsprogramError, Utdanningsprogram> = valider(programomradekode, utdanninger).map { nusKoder ->
            Utdanningsprogram(
                id = UUID.randomUUID(),
                programomradekode = programomradekode,
                navn = navn,
                type = type,
                nusKoder = nusKoder,
                utdanninger = utdanninger,
            )
        }

        fun fromStorage(
            id: UUID,
            programomradekode: String,
            navn: String,
            type: UtdanningsprogramType?,
            nusKoder: List<String>,
            utdanninger: List<Utdanning>,
        ) = Utdanningsprogram(
            id = id,
            programomradekode = programomradekode,
            navn = navn,
            type = type,
            nusKoder = nusKoder,
            utdanninger = utdanninger,
        )

        private fun valider(
            programomradekode: String,
            utdanninger: List<Utdanning>,
        ): Either<UtdanningsprogramError, List<String>> {
            val nusKoder = UtdanningsprogramNusKoder.forProgramomradekode(programomradekode)
                ?: return UtdanningsprogramError.UkjentProgramomrade(programomradekode).left()

            val utdanningUtenforProgrammet = utdanninger.filter { it.utdanningslop.firstOrNull() != programomradekode }
            if (utdanningUtenforProgrammet.isNotEmpty()) {
                return UtdanningsprogramError.UtdanningTilhorerAnnetProgram(
                    programomradekode = programomradekode,
                    utdanningIder = utdanningUtenforProgrammet.map { it.utdanningId },
                ).left()
            }

            return nusKoder.right()
        }
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
