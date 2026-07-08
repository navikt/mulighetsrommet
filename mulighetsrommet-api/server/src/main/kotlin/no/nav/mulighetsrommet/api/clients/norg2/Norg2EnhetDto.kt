package no.nav.mulighetsrommet.api.clients.norg2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetType
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

/**
 * Eksplisitt mapping av NavEnhetType for å ha kontroll oversetting fra Norg2 til domain.
 *
 * TODO: Vurdere å redusere antall enheter/typer vi replikerer fra Norg2?
 *   Det er mange typer som egentlig ikke er av relevans for våre applikasjoner, men av
 *   historiske (praktiske?) årsaker så har vi replikert alle enheter og heller filtrert på
 *   vei ut heller inn.
 */
fun Norg2Type.toNavEnhetType(): NavEnhetType = when (this) {
    Norg2Type.KO -> NavEnhetType.KO
    Norg2Type.FYLKE -> NavEnhetType.FYLKE
    Norg2Type.TILTAK -> NavEnhetType.TILTAK
    Norg2Type.AAREG -> NavEnhetType.AAREG
    Norg2Type.ALS -> NavEnhetType.ALS
    Norg2Type.ARK -> NavEnhetType.ARK
    Norg2Type.DIR -> NavEnhetType.DIR
    Norg2Type.DOKSENTER -> NavEnhetType.DOKSENTER
    Norg2Type.EKSTERN -> NavEnhetType.EKSTERN
    Norg2Type.FORVALTNING -> NavEnhetType.FORVALTNING
    Norg2Type.FPY -> NavEnhetType.FPY
    Norg2Type.HELFO -> NavEnhetType.HELFO
    Norg2Type.HMS -> NavEnhetType.HMS
    Norg2Type.INNKREV -> NavEnhetType.INNKREV
    Norg2Type.INTRO -> NavEnhetType.INTRO
    Norg2Type.IT -> NavEnhetType.IT
    Norg2Type.KLAGE -> NavEnhetType.KLAGE
    Norg2Type.KONTAKT -> NavEnhetType.KONTAKT
    Norg2Type.KONTROLL -> NavEnhetType.KONTROLL
    Norg2Type.LOKAL -> NavEnhetType.LOKAL
    Norg2Type.OKONOMI -> NavEnhetType.OKONOMI
    Norg2Type.OTENESTE -> NavEnhetType.OTENESTE
    Norg2Type.OPPFUTLAND -> NavEnhetType.OPPFUTLAND
    Norg2Type.OTENESE -> NavEnhetType.OTENESE
    Norg2Type.RIKSREV -> NavEnhetType.RIKSREV
    Norg2Type.ROBOT -> NavEnhetType.ROBOT
    Norg2Type.ROL -> NavEnhetType.ROL
    Norg2Type.TILLIT -> NavEnhetType.TILLIT
    Norg2Type.UTLAND -> NavEnhetType.UTLAND
    Norg2Type.YTA -> NavEnhetType.YTA
}

/**
 * Eksplisitt mapping av NavEnhetStatus for å ha kontroll oversetting fra Norg2 til domain.
 */
fun Norg2EnhetStatus.toNavEnhetStatus(): NavEnhetStatus = when (this) {
    Norg2EnhetStatus.UNDER_ETABLERING -> NavEnhetStatus.UNDER_ETABLERING
    Norg2EnhetStatus.AKTIV -> NavEnhetStatus.AKTIV
    Norg2EnhetStatus.UNDER_AVVIKLING -> NavEnhetStatus.UNDER_AVVIKLING
    Norg2EnhetStatus.NEDLAGT -> NavEnhetStatus.NEDLAGT
}
