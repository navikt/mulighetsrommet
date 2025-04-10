package no.nav.mulighetsrommet.api.navansatt.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NavEnhetNummer

@Serializable
sealed class NavAnsattRolle {
    abstract val rolle: Rolle

    @Serializable
    sealed class Generell : NavAnsattRolle()

    @Serializable
    sealed class Kontorspesifikk : NavAnsattRolle() {
        abstract val enheter: Set<NavEnhetNummer>
    }

    @Serializable
    @SerialName("TEAM_MULIGHETSROMMET")
    data object TeamMulighetsrommet : Generell() {
        override val rolle = Rolle.TEAM_MULIGHETSROMMET
    }

    @Serializable
    @SerialName("KONTAKTPERSON")
    data object Kontaktperson : Generell() {
        override val rolle = Rolle.KONTAKTPERSON
    }

    @Serializable
    @SerialName("TILTAKADMINISTRASJON_GENERELL")
    data object TiltakadministrasjonGenerell : Generell() {
        override val rolle = Rolle.TILTAKADMINISTRASJON_GENERELL
    }

    @Serializable
    @SerialName("TILTAKADMINISTRASJON_ENDRINGSMELDING")
    data object TiltakadministrasjonEndringsmelding : Generell() {
        override val rolle = Rolle.TILTAKADMINISTRASJON_ENDRINGSMELDING
    }

    @Serializable
    @SerialName("TILTAKSGJENNOMFORINGER_SKRIV")
    data object TiltaksgjennomforingerSkriv : Generell() {
        override val rolle = Rolle.TILTAKSGJENNOMFORINGER_SKRIV
    }

    @Serializable
    @SerialName("AVTALER_SKRIV")
    data object AvtalerSkriv : Generell() {
        override val rolle = Rolle.AVTALER_SKRIV
    }

    @Serializable
    @SerialName("SAKSBEHANDLER_OKONOMI")
    data object SaksbehandlerOkonomi : Generell() {
        override val rolle = Rolle.SAKSBEHANDLER_OKONOMI
    }

    @Serializable
    @SerialName("BESLUTTER_TILSAGN")
    data class BeslutterTilsagn(
        override val enheter: Set<NavEnhetNummer>,
    ) : Kontorspesifikk() {
        override val rolle = Rolle.BESLUTTER_TILSAGN
    }

    @Serializable
    @SerialName("ATTESTANT_UTBETALING")
    data class AttestantUtbetaling(
        override val enheter: Set<NavEnhetNummer>,
    ) : Kontorspesifikk() {
        override val rolle = Rolle.ATTESTANT_UTBETALING
    }

    companion object {
        // TODO: vurdere om denne skal splittes en metode per type rolle
        fun fromRolleAndEnheter(rolle: Rolle, enheter: Set<NavEnhetNummer>? = null): NavAnsattRolle {
            return when (rolle) {
                Rolle.TEAM_MULIGHETSROMMET -> TeamMulighetsrommet
                Rolle.KONTAKTPERSON -> Kontaktperson
                Rolle.TILTAKADMINISTRASJON_GENERELL -> TiltakadministrasjonGenerell
                Rolle.TILTAKADMINISTRASJON_ENDRINGSMELDING -> TiltakadministrasjonEndringsmelding
                Rolle.TILTAKSGJENNOMFORINGER_SKRIV -> TiltaksgjennomforingerSkriv
                Rolle.AVTALER_SKRIV -> AvtalerSkriv
                Rolle.SAKSBEHANDLER_OKONOMI -> SaksbehandlerOkonomi
                Rolle.BESLUTTER_TILSAGN -> BeslutterTilsagn(enheter ?: setOf())
                Rolle.ATTESTANT_UTBETALING -> AttestantUtbetaling(enheter ?: setOf())
            }
        }
    }
}
