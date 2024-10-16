package no.nav.mulighetsrommet.utdanning.model

import kotlinx.serialization.Serializable

@Serializable
data class Utdanningsprogram(
    val navn: String,
    val nusKoder: List<String>,
    val programomradekode: String,
    val type: UtdanningsprogramType?,
)

@Serializable
data class Utdanning(
    val programomradekode: String,
    val utdanningId: String,
    val navn: String,
    val sluttkompetanse: Sluttkompetanse?,
    val aktiv: Boolean,
    val utdanningstatus: Status,
    val utdanningslop: List<String>,
    val nusKodeverk: List<NusKodeverk>,
) {
    @Serializable
    enum class Sluttkompetanse {
        FAGBREV,
        SVENNEBREV,
        STUDIEKOMPETANSE,
        YRKESKOMPETANSE,
    }

    @Serializable
    enum class Status {
        GYLDIG,
        KOMMENDE,
        UTGAAENDE,
        UTGAATT,
    }
}

@Serializable
enum class UtdanningsprogramType {
    YRKESFAGLIG,
    STUDIEFORBEREDENDE,
}

@Serializable
data class NusKodeverk(
    val navn: String,
    val kode: String,
)
