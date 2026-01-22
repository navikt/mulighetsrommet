package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable

@Serializable
data class ValutaBelop(
    val belop: Int,
    val valuta: Valuta,
)

operator fun ValutaBelop.plus(other: ValutaBelop): ValutaBelop {
    if (valuta != other.valuta) {
        throw IllegalArgumentException("Kan ikke addere beløp med ulik valuta: $valuta og ${other.valuta}")
    }
    return ValutaBelop(belop + other.belop, valuta)
}

operator fun ValutaBelop.minus(other: ValutaBelop): ValutaBelop {
    if (valuta != other.valuta) {
        throw IllegalArgumentException("Kan ikke substrahere beløp med ulik valuta: $valuta og ${other.valuta}")
    }
    return ValutaBelop(belop - other.belop, valuta)
}

operator fun ValutaBelop.compareTo(other: ValutaBelop): Int {
    if (valuta != other.valuta) {
        throw IllegalArgumentException("Kan ikke sammenligne beløp med ulik valuta: $valuta og ${other.valuta}")
    }
    return belop.compareTo(other.belop)
}

fun Int.withValuta(valuta: Valuta) = ValutaBelop(this, valuta)
