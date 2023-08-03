package no.nav.mulighetsrommet.notifications

class SindreTesterCodeql {

    fun vulnerableCode() {
        val a = listOf(1, 2, 3)
        var sum = 0
        for (i in 0..a.size) { // BAD
            sum += a[i]
        }
    }
}
