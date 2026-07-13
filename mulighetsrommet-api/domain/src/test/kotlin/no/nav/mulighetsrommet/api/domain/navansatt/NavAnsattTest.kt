package no.nav.mulighetsrommet.api.domain.navansatt

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.model.NavEnhetNummer
import java.time.LocalDate

class NavAnsattTest : FunSpec({
    val roller = setOf(NavAnsattRolle.generell(Rolle.TILTAKADMINISTRASJON_GENERELL))

    context("medRoller") {
        test("setter roller") {
            val ansatt = NavAnsattFixture.DonaldDuck.medRoller(roller)

            ansatt.roller shouldBe roller
        }

        test("nullstiller skalSlettesDato, siden en ansatt med aktive roller ikke skal være markert for sletting") {
            val slettet = NavAnsattFixture.DonaldDuck.skalSlettes(LocalDate.now())

            val ansatt = slettet.medRoller(roller)

            ansatt.roller shouldBe roller
            ansatt.skalSlettesDato.shouldBeNull()
        }
    }

    context("skalSlettes") {
        test("setter skalSlettesDato") {
            val dato = LocalDate.now().plusDays(1)

            val ansatt = NavAnsattFixture.DonaldDuck.medRoller(roller).skalSlettes(dato)

            ansatt.skalSlettesDato shouldBe dato
        }

        test("fratar alle roller, siden en ansatt markert for sletting ikke skal ha aktive roller") {
            val ansatt = NavAnsattFixture.DonaldDuck.medRoller(roller).skalSlettes(LocalDate.now())

            ansatt.roller.shouldBeEmpty()
        }
    }

    context("hasGenerellRolle") {
        test("true når ansatt har den generelle rollen") {
            val ansatt = NavAnsattFixture.DonaldDuck.medRoller(
                setOf(NavAnsattRolle.generell(Rolle.TILTAKADMINISTRASJON_GENERELL)),
            )

            ansatt.hasGenerellRolle(Rolle.TILTAKADMINISTRASJON_GENERELL).shouldBeTrue()
        }

        test("false når ansatt ikke har rollen") {
            val ansatt = NavAnsattFixture.DonaldDuck.medRoller(
                setOf(NavAnsattRolle.generell(Rolle.AVTALER_SKRIV)),
            )

            ansatt.hasGenerellRolle(Rolle.TILTAKADMINISTRASJON_GENERELL).shouldBeFalse()
        }

        test("false når ansatt kun har rollen kontorspesifikt") {
            val ansatt = NavAnsattFixture.DonaldDuck.medRoller(
                setOf(NavAnsattRolle.kontorspesifikk(Rolle.TILTAKADMINISTRASJON_GENERELL, setOf(NavEnhetNummer("1000")))),
            )

            ansatt.hasGenerellRolle(Rolle.TILTAKADMINISTRASJON_GENERELL).shouldBeFalse()
        }
    }

    context("hasKontorspesifikkRolle") {
        test("true når ansatt har rollen generelt") {
            val ansatt = NavAnsattFixture.DonaldDuck.medRoller(
                setOf(NavAnsattRolle.generell(Rolle.TILTAKADMINISTRASJON_GENERELL)),
            )

            ansatt.hasKontorspesifikkRolle(Rolle.TILTAKADMINISTRASJON_GENERELL, setOf(NavEnhetNummer("1000"))).shouldBeTrue()
        }

        test("true når ansatt har rollen for alle etterspurte enheter") {
            val ansatt = NavAnsattFixture.DonaldDuck.medRoller(
                setOf(
                    NavAnsattRolle.kontorspesifikk(
                        Rolle.TILTAKADMINISTRASJON_GENERELL,
                        setOf(NavEnhetNummer("1000"), NavEnhetNummer("2000")),
                    ),
                ),
            )

            ansatt.hasKontorspesifikkRolle(Rolle.TILTAKADMINISTRASJON_GENERELL, setOf(NavEnhetNummer("1000"))).shouldBeTrue()
        }

        test("false når ansatt mangler rollen for en av de etterspurte enhetene") {
            val ansatt = NavAnsattFixture.DonaldDuck.medRoller(
                setOf(NavAnsattRolle.kontorspesifikk(Rolle.TILTAKADMINISTRASJON_GENERELL, setOf(NavEnhetNummer("1000")))),
            )

            ansatt.hasKontorspesifikkRolle(
                Rolle.TILTAKADMINISTRASJON_GENERELL,
                setOf(NavEnhetNummer("1000"), NavEnhetNummer("2000")),
            ).shouldBeFalse()
        }
    }

    context("hasAnyGenerellRolle") {
        test("true når ansatt har minst en av de etterspurte rollene") {
            val ansatt = NavAnsattFixture.DonaldDuck.medRoller(
                setOf(NavAnsattRolle.generell(Rolle.AVTALER_SKRIV)),
            )

            ansatt.hasAnyGenerellRolle(Rolle.TILTAKADMINISTRASJON_GENERELL, Rolle.AVTALER_SKRIV).shouldBeTrue()
        }

        test("false når ansatt ikke har noen av de etterspurte rollene") {
            val ansatt = NavAnsattFixture.DonaldDuck.medRoller(
                setOf(NavAnsattRolle.generell(Rolle.KONTAKTPERSON)),
            )

            ansatt.hasAnyGenerellRolle(Rolle.TILTAKADMINISTRASJON_GENERELL, Rolle.AVTALER_SKRIV).shouldBeFalse()
        }
    }
})
