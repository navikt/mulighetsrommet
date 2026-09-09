package no.nav.mulighetsrommet.api.domain.avtale

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.domain.testing.fixture.AvtaleFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.PrismodellFixtures
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.Valuta

class AvtaleTest : FunSpec({
    context("medRammedetaljer") {
        test("kan ikke legges til for systembestemte (forhåndsgodkjente) avtaler") {
            val avtale = AvtaleFixtures.AFT

            avtale.medRammedetaljer(totalRamme = 1000, utbetaltArena = null).shouldBeLeft(
                listOf(
                    FieldError(
                        "/totalRamme",
                        "Rammedetaljer kan kun legges til anskaffet avtaler",
                    ),
                ),
            )
        }

        test("krever at alle prismodeller på avtalen har samme valuta") {
            val avtale = AvtaleFixtures.ARR.copy(
                prisinfo = Avtale.Prisinfo.Egendefinert(
                    listOf(
                        PrismodellFixtures.AvtaltPrisPerManedsverk.copy(valuta = Valuta.NOK),
                        PrismodellFixtures.AnnenAvtaltPris.copy(valuta = Valuta.SEK),
                    ),
                ),
            )

            avtale.medRammedetaljer(totalRamme = 1000, utbetaltArena = null).shouldBeLeft(
                listOf(
                    FieldError(
                        "/totalRamme",
                        "Rammedetaljer kan kun legges til avtaler med én type valuta på prismodellene",
                    ),
                ),
            )
        }

        test("total ramme må være et positivt beløp") {
            val avtale = AvtaleFixtures.ARR.copy(
                prisinfo = Avtale.Prisinfo.Egendefinert(
                    listOf(PrismodellFixtures.AvtaltPrisPerManedsverk.copy(valuta = Valuta.NOK)),
                ),
            )

            avtale.medRammedetaljer(totalRamme = -1, utbetaltArena = null).shouldBeLeft(
                listOf(
                    FieldError("/totalRamme", "Total ramme må være et positivt beløp"),
                ),
            )
        }

        test("utbetalt beløp fra Arena må være et positivt beløp") {
            val avtale = AvtaleFixtures.ARR.copy(
                prisinfo = Avtale.Prisinfo.Egendefinert(
                    listOf(PrismodellFixtures.AvtaltPrisPerManedsverk.copy(valuta = Valuta.NOK)),
                ),
            )

            avtale.medRammedetaljer(totalRamme = 1000, utbetaltArena = -1).shouldBeLeft(
                listOf(
                    FieldError("/utbetaltArena", "Utbetalt beløp fra Arena må være et positivt beløp"),
                ),
            )
        }

        test("samler opp alle feil samtidig") {
            val avtale = AvtaleFixtures.AFT

            avtale.medRammedetaljer(totalRamme = -1, utbetaltArena = -1).shouldBeLeft().shouldContainExactlyInAnyOrder(
                listOf(
                    FieldError("/totalRamme", "Rammedetaljer kan kun legges til anskaffet avtaler"),
                    FieldError("/totalRamme", "Total ramme må være et positivt beløp"),
                    FieldError("/utbetaltArena", "Utbetalt beløp fra Arena må være et positivt beløp"),
                ),
            )
        }

        test("legger til rammedetaljer med valuta hentet fra prismodellene når validering går bra") {
            val avtale = AvtaleFixtures.ARR.copy(
                prisinfo = Avtale.Prisinfo.Egendefinert(
                    listOf(PrismodellFixtures.AvtaltPrisPerManedsverk.copy(valuta = Valuta.NOK)),
                ),
            )

            val oppdatert = avtale.medRammedetaljer(totalRamme = 1000, utbetaltArena = null).shouldBeRight()

            oppdatert.rammedetaljer shouldBe Avtale.Rammedetaljer(
                totalRamme = 1000,
                utbetaltArena = null,
                valuta = Valuta.NOK,
            )
        }

        test("tillater at både totalRamme og utbetaltArena er null") {
            val avtale = AvtaleFixtures.ARR.copy(
                prisinfo = Avtale.Prisinfo.Egendefinert(
                    listOf(PrismodellFixtures.AvtaltPrisPerManedsverk.copy(valuta = Valuta.NOK)),
                ),
            )

            val oppdatert = avtale.medRammedetaljer(totalRamme = null, utbetaltArena = null).shouldBeRight()

            oppdatert.rammedetaljer shouldBe Avtale.Rammedetaljer(
                totalRamme = null,
                utbetaltArena = null,
                valuta = Valuta.NOK,
            )
        }
    }

    context("slettRammedetaljer") {
        test("fjerner eksisterende rammedetaljer") {
            val avtale = AvtaleFixtures.ARR.copy(
                prisinfo = Avtale.Prisinfo.Egendefinert(
                    listOf(PrismodellFixtures.AvtaltPrisPerManedsverk.copy(valuta = Valuta.NOK)),
                ),
                rammedetaljer = Avtale.Rammedetaljer(totalRamme = 1000, utbetaltArena = 100, valuta = Valuta.NOK),
            )

            val oppdatert = avtale.slettRammedetaljer()

            oppdatert.rammedetaljer.shouldBeNull()
        }
    }
})
