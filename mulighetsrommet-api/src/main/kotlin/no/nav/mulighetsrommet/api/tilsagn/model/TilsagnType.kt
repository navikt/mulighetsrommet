package no.nav.mulighetsrommet.api.tilsagn.model

import no.nav.tiltak.okonomi.Tilskuddstype

enum class TilsagnType {
    TILSAGN,
    EKSTRATILSAGN,
    INVESTERING,
    ;

    companion object {
        fun fromTilskuddstype(tilskuddstype: Tilskuddstype): List<TilsagnType> = when (tilskuddstype) {
            Tilskuddstype.TILTAK_DRIFTSTILSKUDD -> listOf(EKSTRATILSAGN, TILSAGN)
            Tilskuddstype.TILTAK_INVESTERINGER -> listOf(INVESTERING)
        }
    }
}
