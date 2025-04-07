package no.nav.mulighetsrommet.api.navansatt.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NavEnhetNummer

@Serializable
sealed class Rolle {
    abstract val rolle: NavAnsattRolle

    @Serializable
    sealed class Generell : Rolle()

    @Serializable
    sealed class Kontorspesifikk : Rolle() {
        abstract val enheter: Set<NavEnhetNummer>
    }

    @Serializable
    @SerialName("TEAM_MULIGHETSROMMET")
    data object TeamMulighetsrommet : Generell() {
        override val rolle = NavAnsattRolle.TEAM_MULIGHETSROMMET
    }

    @Serializable
    @SerialName("KONTAKTPERSON")
    data object Kontaktperson : Generell() {
        override val rolle = NavAnsattRolle.KONTAKTPERSON
    }

    @Serializable
    @SerialName("TILTAKADMINISTRASJON_GENERELL")
    data object TiltakadministrasjonGenerell : Generell() {
        override val rolle = NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL
    }

    @Serializable
    @SerialName("TILTAKADMINISTRASJON_ENDRINGSMELDING")
    data object TiltakadministrasjonEndringsmelding : Generell() {
        override val rolle = NavAnsattRolle.TILTAKADMINISTRASJON_ENDRINGSMELDING
    }

    @Serializable
    @SerialName("TILTAKSGJENNOMFORINGER_SKRIV")
    data object TiltaksgjennomforingerSkriv : Generell() {
        override val rolle = NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV
    }

    @Serializable
    @SerialName("AVTALER_SKRIV")
    data object AvtalerSkriv : Generell() {
        override val rolle = NavAnsattRolle.AVTALER_SKRIV
    }

    @Serializable
    @SerialName("SAKSBEHANDLER_OKONOMI")
    data object SaksbehandlerOkonomi : Generell() {
        override val rolle = NavAnsattRolle.SAKSBEHANDLER_OKONOMI
    }

    @Serializable
    @SerialName("BESLUTTER_TILSAGN")
    data class BeslutterTilsagn(
        override val enheter: Set<NavEnhetNummer>,
    ) : Kontorspesifikk() {
        override val rolle = NavAnsattRolle.BESLUTTER_TILSAGN
    }

    @Serializable
    @SerialName("ATTESTANT_UTBETALING")
    data class AttestantUtbetaling(
        override val enheter: Set<NavEnhetNummer>,
    ) : Kontorspesifikk() {
        override val rolle = NavAnsattRolle.ATTESTANT_UTBETALING
    }

    companion object {
        // TODO: office specific roles
        fun fromRolleAndEnheter(rolle: NavAnsattRolle, enheter: Set<NavEnhetNummer>? = null): Rolle {
            return when (rolle) {
                NavAnsattRolle.TEAM_MULIGHETSROMMET -> TeamMulighetsrommet
                NavAnsattRolle.KONTAKTPERSON -> Kontaktperson
                NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL -> TiltakadministrasjonGenerell
                NavAnsattRolle.TILTAKADMINISTRASJON_ENDRINGSMELDING -> TiltakadministrasjonEndringsmelding
                NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV -> TiltaksgjennomforingerSkriv
                NavAnsattRolle.AVTALER_SKRIV -> AvtalerSkriv
                NavAnsattRolle.SAKSBEHANDLER_OKONOMI -> SaksbehandlerOkonomi
                NavAnsattRolle.BESLUTTER_TILSAGN -> BeslutterTilsagn(enheter ?: setOf())
                NavAnsattRolle.ATTESTANT_UTBETALING -> AttestantUtbetaling(enheter ?: setOf())
            }
        }
    }
}
