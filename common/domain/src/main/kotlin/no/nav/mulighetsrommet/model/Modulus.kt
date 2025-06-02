package no.nav.mulighetsrommet.model

object Modulus {
    enum class Algorithm {
        MOD10,
        MOD11,
    }

    private val mod11Weights = listOf(2, 3, 4, 5, 6, 7)
    private val mod10Weights = listOf(2, 1)

    fun hasValidControlDigit(number: String, modAlgorithm: Algorithm): Boolean {
        val numberWithoutControlDigit = number.slice(0..<number.length - 1)
        val controlDigit: Char = number.last()
        val calculatedControlDigit = when (modAlgorithm) {
            Algorithm.MOD10 -> mod10(numberWithoutControlDigit)
            Algorithm.MOD11 -> mod11(numberWithoutControlDigit)
        }
        return controlDigit == calculatedControlDigit
    }

    private fun mod10(number: String): Char {
        var sum = 0
        for (i in number.indices) {
            val digit = number[number.length - 1 - i]
            val weight = mod10Weights[i % mod10Weights.size]
            val weightedDigit = digit.digitToInt() * weight
            sum += if (weightedDigit >= 10) {
                1 + (weightedDigit % 10)
            } else {
                weightedDigit
            }
        }
        val control = 10 - (sum % 10)
        return if (control == 10) '0' else control.digitToChar()
    }

    private fun mod11(number: String): Char {
        var sum = 0
        for (i in number.indices) {
            val digit = number[number.length - 1 - i]
            val weight = mod11Weights[i % mod11Weights.size]
            sum += digit.digitToInt() * weight
        }
        return when (val control = 11 - (sum % 11)) {
            11 -> '0'
            10 -> '-'
            else -> control.digitToChar()
        }
    }
}
