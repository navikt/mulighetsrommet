package no.nav.mulighetsrommet.brreg

sealed interface BrregError {
    data class FjernetAvJuridiskeArsaker(val enhet: FjernetBrregEnhetDto) : BrregError
    object BadRequest : BrregError
    object NotFound : BrregError
    object Error : BrregError
}
