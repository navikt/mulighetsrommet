package no.nav.mulighetsrommet.api.navansatt.helper

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.model.NavEnhetNummer

class NavAnsattRolleHelperTest : FunSpec({
    context("hasRole") {
        test("false when no roles") {
            val roles = emptySet<NavAnsattRolle>()
            val requiredRole = NavAnsattRolle.generell(Rolle.TILTAKADMINISTRASJON_GENERELL)

            NavAnsattRolleHelper.hasRole(roles, requiredRole).shouldBeFalse()
        }

        test("false when no matching role") {
            val roles = setOf(
                NavAnsattRolle.generell(Rolle.AVTALER_SKRIV),
                NavAnsattRolle.generell(Rolle.TILTAKSGJENNOMFORINGER_SKRIV),
            )
            val requiredRole = NavAnsattRolle.generell(Rolle.TILTAKADMINISTRASJON_GENERELL)

            NavAnsattRolleHelper.hasRole(roles, requiredRole).shouldBeFalse()
        }

        test("true for a matching role") {
            val roles = setOf(
                NavAnsattRolle.generell(Rolle.TILTAKADMINISTRASJON_GENERELL),
                NavAnsattRolle.generell(Rolle.AVTALER_SKRIV),
            )
            val requiredRole = NavAnsattRolle.generell(Rolle.TILTAKADMINISTRASJON_GENERELL)

            NavAnsattRolleHelper.hasRole(roles, requiredRole).shouldBeTrue()
        }

        test("true when role is generell and required role is kontorspesfikk") {
            val roles = setOf(
                NavAnsattRolle.generell(Rolle.TILTAKADMINISTRASJON_GENERELL),
            )
            val requiredRole = NavAnsattRolle.kontorspesifikk(
                Rolle.TILTAKADMINISTRASJON_GENERELL,
                setOf(),
            )

            NavAnsattRolleHelper.hasRole(roles, requiredRole).shouldBeTrue()
        }

        test("false when role is kontorspesfikk and required role is generell") {
            val roles = setOf(
                NavAnsattRolle.kontorspesifikk(Rolle.TILTAKADMINISTRASJON_GENERELL, setOf(NavEnhetNummer("1000"))),
            )
            val requiredRole = NavAnsattRolle.generell(Rolle.TILTAKADMINISTRASJON_GENERELL)

            NavAnsattRolleHelper.hasRole(roles, requiredRole).shouldBeFalse()
        }

        test("false when kontorspesifikk role with empty enheter") {
            val roles = setOf(
                NavAnsattRolle.kontorspesifikk(Rolle.TILTAKADMINISTRASJON_GENERELL, setOf()),
            )
            val requiredRole = NavAnsattRolle.kontorspesifikk(
                Rolle.TILTAKADMINISTRASJON_GENERELL,
                setOf(),
            )

            NavAnsattRolleHelper.hasRole(roles, requiredRole).shouldBeFalse()
        }

        test("false when no matching kontorspesifikk role") {
            val roles = setOf(
                NavAnsattRolle.kontorspesifikk(Rolle.TILTAKADMINISTRASJON_GENERELL, setOf(NavEnhetNummer("1000"))),
            )
            val requiredRole = NavAnsattRolle.kontorspesifikk(
                Rolle.TILTAKADMINISTRASJON_GENERELL,
                setOf(NavEnhetNummer("2000")),
            )

            NavAnsattRolleHelper.hasRole(roles, requiredRole).shouldBeFalse()
        }

        test("false when partially matching required enheter") {
            val roles = setOf(
                NavAnsattRolle.kontorspesifikk(Rolle.TILTAKADMINISTRASJON_GENERELL, setOf(NavEnhetNummer("1000"))),
            )
            val requiredRole = NavAnsattRolle.kontorspesifikk(
                Rolle.TILTAKADMINISTRASJON_GENERELL,
                setOf(NavEnhetNummer("1000"), NavEnhetNummer("2000")),
            )

            NavAnsattRolleHelper.hasRole(roles, requiredRole).shouldBeFalse()
        }

        test("true when all matching all required enheter") {
            val roles = setOf(
                NavAnsattRolle.kontorspesifikk(
                    Rolle.TILTAKADMINISTRASJON_GENERELL,
                    setOf(NavEnhetNummer("1000"), NavEnhetNummer("2000"), NavEnhetNummer("3000")),
                ),
            )
            val requiredRole = NavAnsattRolle.kontorspesifikk(
                Rolle.TILTAKADMINISTRASJON_GENERELL,
                setOf(NavEnhetNummer("1000"), NavEnhetNummer("2000")),
            )

            NavAnsattRolleHelper.hasRole(roles, requiredRole).shouldBeTrue()
        }

        test("true when all matching required enheter is empty") {
            val roles = setOf(
                NavAnsattRolle.kontorspesifikk(
                    Rolle.TILTAKADMINISTRASJON_GENERELL,
                    setOf(NavEnhetNummer("1000")),
                ),
            )
            val requiredRole = NavAnsattRolle.kontorspesifikk(
                Rolle.TILTAKADMINISTRASJON_GENERELL,
                setOf(),
            )

            NavAnsattRolleHelper.hasRole(roles, requiredRole).shouldBeTrue()
        }
    }
})
