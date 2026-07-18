package no.nav.mulighetsrommet.validation

sealed interface BrregError {
    data class FjernetAvJuridiskeArsaker(val enhet: FjernetBrregEnhetDto) : BrregError
    data object BadRequest : BrregError
    data object NotFound : BrregError
    data object Error : BrregError
}
