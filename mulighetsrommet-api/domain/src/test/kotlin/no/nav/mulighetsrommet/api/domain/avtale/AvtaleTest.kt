package no.nav.mulighetsrommet.api.domain.avtale

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.domain.testing.fixture.AvtaleFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.PrismodellFixtures
import no.nav.mulighetsrommet.api.domain.tiltak.PrismodellType
import no.nav.mulighetsrommet.model.Avtaletype
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

    context("medPrismodeller") {
        test("kan ikke endres for forhåndsgodkjente avtaler") {
            val avtale = AvtaleFixtures.AFT

            avtale.medPrismodeller(listOf(PrismodellFixtures.AnnenAvtaltPris)).shouldBeLeft(
                listOf(FieldError.of("Prismodell kan ikke endres for forhåndsgodkjente avtaler")),
            )
        }

        test("krever minst én prismodell") {
            val avtale = AvtaleFixtures.oppfolging

            avtale.medPrismodeller(emptyList()).shouldBeLeft(
                listOf(FieldError("/prismodeller", "Minst én prismodell er påkrevd")),
            )
        }

        test("prismodelltype må være tillatt for avtalens tiltakskode") {
            val avtale = AvtaleFixtures.oppfolging

            avtale.medPrismodeller(
                listOf(PrismodellFixtures.createPrismodell(type = PrismodellType.FAST_SATS_PER_AVTALT_PLASS_PER_MANED)),
            ).shouldBeLeft(
                listOf(
                    FieldError(
                        "/prismodeller/0/type",
                        "Fast sats per avtalt tiltaksplass per måned er ikke tillatt for tiltakskode OPPFOLGING",
                    ),
                ),
            )
        }

        test("FAST_SATS_PER_BENYTTET_PLASS_PER_MANED er forbeholdt systembestemte prismodeller") {
            val avtale = AvtaleFixtures.AFT.copy(
                avtaletype = Avtaletype.RAMMEAVTALE,
                prisinfo = Avtale.Prisinfo.Egendefinert(listOf(PrismodellFixtures.ForhandsgodkjentAft)),
            )

            avtale.medPrismodeller(listOf(PrismodellFixtures.ForhandsgodkjentAft)).shouldBeLeft(
                listOf(
                    FieldError(
                        "/prismodeller",
                        "Prismodell kan ikke opprettes med typen Fast sats per benyttet tiltaksplass per måned",
                    ),
                ),
            )
        }

        test("oppdaterer prisinfo til egendefinerte prismodeller når validering går bra") {
            val avtale = AvtaleFixtures.oppfolging

            val oppdatert = avtale.medPrismodeller(listOf(PrismodellFixtures.AnnenAvtaltPris)).shouldBeRight()

            oppdatert.prisinfo shouldBe Avtale.Prisinfo.Egendefinert(listOf(PrismodellFixtures.AnnenAvtaltPris))
        }
    }
})
