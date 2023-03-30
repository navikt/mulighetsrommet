package no.nav.mulighetsrommet.api.clients.norg2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus

@Serializable
data class Norg2EnhetDto(
    val enhetId: Int,
    val navn: String,
    val enhetNr: String,
    val status: Norg2EnhetStatus,
    val type: Norg2Type
) {
    fun toNavEnhetDbo(): NavEnhetDbo {
        return NavEnhetDbo(
            enhetId = enhetId,
            navn = navn,
            enhetNr = enhetNr,
            status = NavEnhetStatus.valueOf(status.name),
            type = Norg2Type.valueOf(type.name)
        )
    }
}

@Serializable
enum class Norg2EnhetStatus {
    @SerialName("Under etablering")
    UNDER_ETABLERING,

    @SerialName("Aktiv")
    AKTIV,

    @SerialName("Under avvikling")
    UNDER_AVVIKLING,

    @SerialName("Nedlagt")
    NEDLAGT
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
    YTA
}
