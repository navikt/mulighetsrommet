package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.tiltak.okonomi.Tilskuddstype

@Serializable
enum class UtbetalingType {
    KORRIGERING,
    INVESTERING,
    ;

    companion object {
        fun fromUtbetaling(utbetaling: Utbetaling): UtbetalingType? = when {
            utbetaling.innsender is NavIdent && utbetaling.tilskuddstype == Tilskuddstype.TILTAK_DRIFTSTILSKUDD -> {
                KORRIGERING
            }

            utbetaling.innsender is Arrangor && utbetaling.tilskuddstype == Tilskuddstype.TILTAK_INVESTERINGER -> {
                INVESTERING
            }

            else -> {
                null
            }
        }
    }
}
