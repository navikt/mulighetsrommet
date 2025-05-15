package no.nav.mulighetsrommet.api.clients.pdl

fun tilPersonNavn(pdlNavn: List<PdlNavn>): String {
    return pdlNavn.first().let { navn ->
        val fornavnOgMellomnavn = listOfNotNull(navn.fornavn, navn.mellomnavn).joinToString(" ")
        listOf(navn.etternavn, fornavnOgMellomnavn).joinToString(", ")
    }
}

fun toGeografiskTilknytning(pdlGeografiskTilknytning: PdlGeografiskTilknytning?): GeografiskTilknytning {
    return when (pdlGeografiskTilknytning?.gtType) {
        TypeGeografiskTilknytning.BYDEL -> {
            GeografiskTilknytning.GtBydel(requireNotNull(pdlGeografiskTilknytning.gtBydel))
        }

        TypeGeografiskTilknytning.KOMMUNE -> {
            GeografiskTilknytning.GtKommune(requireNotNull(pdlGeografiskTilknytning.gtKommune))
        }

        TypeGeografiskTilknytning.UTLAND -> {
            GeografiskTilknytning.GtUtland(pdlGeografiskTilknytning.gtLand)
        }

        TypeGeografiskTilknytning.UDEFINERT, null -> {
            GeografiskTilknytning.GtUdefinert
        }
    }
}
