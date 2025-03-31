package no.nav.mulighetsrommet.api.navansatt.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NavEnhetNummer

@Serializable
sealed class Rolle {
    abstract val rolle: NavAnsattRolle

    @Serializable
    sealed class Global : Rolle() {
        @Serializable
        @SerialName("TEAM_MULIGHETSROMMET")
        data object TeamMulighetsrommet : Global() {
            override val rolle = NavAnsattRolle.TEAM_MULIGHETSROMMET
        }

        @Serializable
        @SerialName("KONTAKTPERSON")
        data object Kontaktperson : Global() {
            override val rolle = NavAnsattRolle.KONTAKTPERSON
        }

        @Serializable
        @SerialName("TILTAKADMINISTRASJON_GENERELL")
        data object TiltakadministrasjonGenerell : Global() {
            override val rolle = NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL
        }

        @Serializable
        @SerialName("TILTAKADMINISTRASJON_ENDRINGSMELDING")
        data object TiltakadministrasjonEndringsmelding : Global() {
            override val rolle = NavAnsattRolle.TILTAKADMINISTRASJON_ENDRINGSMELDING
        }

        @Serializable
        @SerialName("TILTAKSGJENNOMFORINGER_SKRIV")
        data object TiltaksgjennomforingerSkriv : Global() {
            override val rolle = NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV
        }

        @Serializable
        @SerialName("AVTALER_SKRIV")
        data object AvtalerSkriv : Global() {
            override val rolle = NavAnsattRolle.AVTALER_SKRIV
        }

        @Serializable
        @SerialName("SAKSBEHANDLER_OKONOMI")
        data object SaksbehandlerOkonomi : Global() {
            override val rolle = NavAnsattRolle.SAKSBEHANDLER_OKONOMI
        }
    }

    @Serializable
    sealed class OfficeSpecific : Rolle() {
        abstract val enheter: Set<NavEnhetNummer>

        @Serializable
        @SerialName("BESLUTTER_TILSAGN")
        data class BeslutterTilsagn(
            override val enheter: Set<NavEnhetNummer>,
        ) : OfficeSpecific() {
            override val rolle = NavAnsattRolle.BESLUTTER_TILSAGN
        }

        @Serializable
        @SerialName("ATTESTANT_UTBETALING")
        data class AttestantUtbetaling(
            override val enheter: Set<NavEnhetNummer>,
        ) : OfficeSpecific() {
            override val rolle = NavAnsattRolle.ATTESTANT_UTBETALING
        }
    }

    companion object {
        fun fromRolleAndEnheter(rolle: NavAnsattRolle, enheter: Set<NavEnhetNummer>? = null): Rolle {
            return when (rolle) {
                NavAnsattRolle.TEAM_MULIGHETSROMMET -> Global.TeamMulighetsrommet
                NavAnsattRolle.KONTAKTPERSON -> Global.Kontaktperson
                NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL -> Global.TiltakadministrasjonGenerell
                NavAnsattRolle.TILTAKADMINISTRASJON_ENDRINGSMELDING -> Global.TiltakadministrasjonEndringsmelding
                NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV -> Global.TiltaksgjennomforingerSkriv
                NavAnsattRolle.AVTALER_SKRIV -> Global.AvtalerSkriv
                NavAnsattRolle.SAKSBEHANDLER_OKONOMI -> Global.SaksbehandlerOkonomi
                NavAnsattRolle.BESLUTTER_TILSAGN -> OfficeSpecific.BeslutterTilsagn(enheter ?: setOf())
                NavAnsattRolle.ATTESTANT_UTBETALING -> OfficeSpecific.AttestantUtbetaling(enheter ?: setOf())
            }
        }
    }
}
