package no.nav.mulighetsrommet.api.avtale

import arrow.core.Either
import no.nav.mulighetsrommet.api.avtale.db.RammedetaljerDbo
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.avtale.model.RammedetaljerRequest
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.validation.validation
import java.util.UUID
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
object RammedetaljerValidator {
    data class Ctx(
        val avtaleId: UUID,
        val prismodeller: List<Prismodell>,
    )

    fun validateRammedetaljer(
        context: Ctx,
        request: RammedetaljerRequest,
    ): Either<List<FieldError>, RammedetaljerDbo> = validation {
        validate(context.prismodeller.all { kanHaRammedetaljer(it.type) }) {
            FieldError.of(
                "Rammedetaljer kan kun legges til anskaffet avtaler",
                RammedetaljerRequest::totalRamme,
            )
        }
        validate(context.prismodeller.distinctBy { it.valuta }.count() == 1) {
            FieldError.of(
                "Rammedetaljer kan kun legges til avtaler med én type valuta på prismodellene",
                RammedetaljerRequest::totalRamme,
            )
        }
        validate(request.totalRamme > 0) {
            FieldError.of(
                "Total ramme må være et positivt beløp",
                RammedetaljerRequest::totalRamme,
            )
        }
        request.utbetaltArena?.let { utbetaltArena ->
            validate(utbetaltArena >= 0) {
                FieldError.of(
                    "Utbetalt beløp fra Arena må være et positivt beløp",
                    RammedetaljerRequest::utbetaltArena,
                )
            }
        }

        RammedetaljerDbo(
            avtaleId = context.avtaleId,
            valuta = context.prismodeller.first().valuta,
            totalRamme = request.totalRamme,
            utbetaltArena = request.utbetaltArena,
        )
    }

    private fun kanHaRammedetaljer(prismodellType: PrismodellType) = when (prismodellType) {
        PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK -> false

        PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
        PrismodellType.AVTALT_PRIS_PER_UKESVERK,
        PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK,
        PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
        PrismodellType.ANNEN_AVTALT_PRIS,
        -> true
    }
}
