package no.nav.mulighetsrommet.api.avtale

import arrow.core.Either
import no.nav.mulighetsrommet.api.avtale.db.RammedetaljerDbo
import no.nav.mulighetsrommet.api.avtale.model.RammedetaljerRequest
import no.nav.mulighetsrommet.api.domain.tiltak.Avtale
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.validation.validation
import java.util.UUID
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
object RammedetaljerValidator {
    data class Ctx(
        val avtaleId: UUID,
        val prisinfo: Avtale.Prisinfo,
    )

    fun validateRammedetaljer(
        context: Ctx,
        request: RammedetaljerRequest,
    ): Either<List<FieldError>, RammedetaljerDbo> = validation {
        validate(context.prisinfo !is Avtale.Prisinfo.Systembestemt) {
            FieldError.of(
                "Rammedetaljer kan kun legges til anskaffet avtaler",
                RammedetaljerRequest::totalRamme,
            )
        }

        val prismodeller = context.prisinfo.toList()
        validate(prismodeller.distinctBy { it.valuta }.count() == 1) {
            FieldError.of(
                "Rammedetaljer kan kun legges til avtaler med én type valuta på prismodellene",
                RammedetaljerRequest::totalRamme,
            )
        }
        request.totalRamme?.let { totalRamme ->
            validate(totalRamme > 0) {
                FieldError.of(
                    "Total ramme må være et positivt beløp",
                    RammedetaljerRequest::totalRamme,
                )
            }
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
            valuta = prismodeller.first().valuta,
            totalRamme = request.totalRamme,
            utbetaltArena = request.utbetaltArena,
        )
    }
}
