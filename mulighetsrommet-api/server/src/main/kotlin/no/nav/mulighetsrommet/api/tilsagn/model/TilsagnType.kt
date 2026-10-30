package no.nav.mulighetsrommet.api.tilsagn.model

import no.nav.tiltak.okonomi.Tilskuddstype

enum class TilsagnType {
    TILSAGN,
    EKSTRATILSAGN,
    INVESTERING,
    TILSKUDD_TIL_OPPLAERING,
    ;

    fun displayName(): String = when (this) {
        TILSAGN -> "Tilsagn"
        EKSTRATILSAGN -> "Ekstratilsagn"
        INVESTERING -> "Tilsagn for investering"
        TILSKUDD_TIL_OPPLAERING -> "Tilskudd til opplæring"
    }

    companion object {
        fun fromTilskuddstype(tilskuddstype: Tilskuddstype): List<TilsagnType> = when (tilskuddstype) {
            Tilskuddstype.TILTAK_DRIFTSTILSKUDD -> listOf(EKSTRATILSAGN, TILSAGN)
            Tilskuddstype.TILTAK_INVESTERINGER -> listOf(INVESTERING)
            Tilskuddstype.TILTAK_OPPLAERING_TILSKUDD -> listOf(TILSKUDD_TIL_OPPLAERING)
        }
    }
}
