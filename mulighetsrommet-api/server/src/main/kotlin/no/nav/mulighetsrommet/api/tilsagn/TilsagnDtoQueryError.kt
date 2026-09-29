package no.nav.mulighetsrommet.api.tilsagn

sealed class TilsagnDtoQueryError {
    data class IkkeFunnet(val message: String) : TilsagnDtoQueryError()
    data class ManglerTilgang(val message: String) : TilsagnDtoQueryError()
}
