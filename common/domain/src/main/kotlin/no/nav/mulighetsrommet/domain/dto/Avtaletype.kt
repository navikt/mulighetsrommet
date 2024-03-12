package no.nav.mulighetsrommet.domain.dto

enum class Avtaletype {
    Avtale,
    Rammeavtale,
    Forhaandsgodkjent,
    OffentligOffentlig,
}

fun allowedAvtaletypes(arenaKode: String): List<Avtaletype> {
    // TODO: Bruk den nye tiltakskoden når den er merget
    return when (arenaKode) {
        "ARBFORB", "VASV" ->
            listOf(Avtaletype.Forhaandsgodkjent)
        "AVKLARAG", "INDOPPFAG", "ARBRRHDAG", "DIGIOPPARB", "JOBBK" ->
            listOf(Avtaletype.Rammeavtale, Avtaletype.Avtale)
        "GRUFAGYRKE", "GRUPPEAMO" ->
            listOf(Avtaletype.Rammeavtale, Avtaletype.Avtale, Avtaletype.OffentligOffentlig)
        // Hvis ikke gruppetiltak
        else ->
            listOf(Avtaletype.Rammeavtale, Avtaletype.Avtale, Avtaletype.Forhaandsgodkjent, Avtaletype.OffentligOffentlig)
    }
}
