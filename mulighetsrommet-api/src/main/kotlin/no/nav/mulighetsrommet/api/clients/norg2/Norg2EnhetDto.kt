package no.nav.mulighetsrommet.api.clients.norg2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NavEnhetNummer

@Serializable
data class Norg2Response(
    val enhet: Norg2EnhetDto,
    val overordnetEnhet: NavEnhetNummer?,
)

@Serializable
data class Norg2EnhetDto(
    val enhetId: Int,
    val navn: String,
    val enhetNr: NavEnhetNummer,
    val status: Norg2EnhetStatus,
    val type: Norg2Type,
)

@Serializable
enum class Norg2EnhetStatus {
    @SerialName("Under etablering")
    UNDER_ETABLERING,

    @SerialName("Aktiv")
    AKTIV,

    @SerialName("Under avvikling")
    UNDER_AVVIKLING,

    @SerialName("Nedlagt")
    NEDLAGT,
}

@Serializable
enum class Norg2Type {
    KO,
    FYLKE,
    TILTAK,
    AAREG,
    ALS,
    ARK,
    DIR,
    DOKSENTER,
    EKSTERN,
    FORVALTNING,
    FPY,
    HELFO,
    HMS,
    INNKREV,
    INTRO,
    IT,
    KLAGE,
    KONTAKT,
    KONTROLL,
    LOKAL,
    OKONOMI,
    OTENESTE,
    OPPFUTLAND,
    OTENESE,
    RIKSREV,
    ROBOT,
    ROL,
    TILLIT,
    UTLAND,
    YTA,
}
