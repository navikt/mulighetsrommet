package no.nav.mulighetsrommet.api.clients.pdl

fun List<PdlNavn>.tilNavn(): String? {
    return this.firstOrNull()?.let { navn ->
        val fornavnOgMellomnavn = listOfNotNull(navn.fornavn, navn.mellomnavn).joinToString(" ")
        listOf(navn.etternavn, fornavnOgMellomnavn).joinToString(", ")
    }
}

fun List<Adressebeskyttelse>.tilGradering(): PdlGradering {
    return this.firstOrNull()?.gradering ?: PdlGradering.UGRADERT
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
