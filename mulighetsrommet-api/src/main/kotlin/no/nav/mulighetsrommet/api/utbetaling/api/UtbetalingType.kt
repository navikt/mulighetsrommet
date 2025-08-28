package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.tiltak.okonomi.Tilskuddstype

enum class UtbetalingType(val displayName: String, val displayNameLong: String?, val tagName: String?) {
    KORRIGERING("Korrigering", null, "KOR"),
    INVESTERING("Investering", "Utbetaling for investering", "INV"),
    INNSENDING("Innsending", null, null),
    ;

    companion object {
        fun from(utbetaling: Utbetaling): UtbetalingType = when {
            utbetaling.innsender is NavIdent && utbetaling.tilskuddstype == Tilskuddstype.TILTAK_DRIFTSTILSKUDD -> {
                KORRIGERING
            }

            utbetaling.innsender is Arrangor && utbetaling.tilskuddstype == Tilskuddstype.TILTAK_INVESTERINGER -> {
                INVESTERING
            }

            else -> {
                INNSENDING
            }
        }
    }
}

@Serializable
data class UtbetalingTypeDto(val displayName: String, val displayNameLong: String?, val tagName: String?)

fun UtbetalingType.toDto(): UtbetalingTypeDto {
    return UtbetalingTypeDto(this.displayName, this.displayNameLong, this.tagName)
}
