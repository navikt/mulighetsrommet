package no.nav.mulighetsrommet.api.avtale

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.avtale.model.RammedetaljerRequest
import no.nav.mulighetsrommet.api.domain.tiltak.Avtale
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.PrismodellFixtures
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.Valuta

class RammedetaljerValidatorTest : FunSpec({
    context("rammedetaljer") {
        test("må være anskaffet tiltak") {
            val ikkeAnskaffetCtx = RammedetaljerValidator.Ctx(
                avtaleId = AvtaleFixtures.AFT.id,
                prisinfo = AvtaleFixtures.AFT.prisinfo,
            )

            RammedetaljerValidator.validateRammedetaljer(
                ikkeAnskaffetCtx,
                RammedetaljerRequest(
                    totalRamme = 1000,
                    utbetaltArena = null,
                ),
            ).shouldBeLeft().shouldContainExactlyInAnyOrder(
                listOf(
                    FieldError("/totalRamme", "Rammedetaljer kan kun legges til anskaffet avtaler"),
                ),
            )
        }

        test("må være lik valuta på alle prismodeller") {
            val ctx = RammedetaljerValidator.Ctx(
                avtaleId = AvtaleFixtures.ARR.id,
                prisinfo = Avtale.Prisinfo.Egendefinert(
                    listOf(
                        PrismodellFixtures.AvtaltPrisPerManedsverk.copy(valuta = Valuta.NOK),
                        PrismodellFixtures.AnnenAvtaltPris.copy(valuta = Valuta.SEK),
                    ),
                ),
            )

            RammedetaljerValidator.validateRammedetaljer(
                ctx,
                RammedetaljerRequest(
                    totalRamme = 1000,
                    utbetaltArena = null,
                ),
            ).shouldBeLeft().shouldContainExactlyInAnyOrder(
                listOf(
                    FieldError(
                        "/totalRamme",
                        "Rammedetaljer kan kun legges til avtaler med én type valuta på prismodellene",
                    ),
                ),
            )
        }

        test("total ramme må være positivt beløp") {
            val ikkeAnskaffetCtx = RammedetaljerValidator.Ctx(
                avtaleId = AvtaleFixtures.ARR.id,
                prisinfo = Avtale.Prisinfo.Egendefinert(
                    listOf(PrismodellFixtures.AvtaltPrisPerManedsverk.copy(valuta = Valuta.NOK)),
                ),
            )

            RammedetaljerValidator.validateRammedetaljer(
                ikkeAnskaffetCtx,
                RammedetaljerRequest(
                    totalRamme = -1,
                    utbetaltArena = null,
                ),
            ).shouldBeLeft().shouldContainExactlyInAnyOrder(
                listOf(
                    FieldError("/totalRamme", "Total ramme må være et positivt beløp"),
                ),
            )
        }

        test("utetalt fra Arena må være positivt beløp") {
            val ctx = RammedetaljerValidator.Ctx(
                avtaleId = AvtaleFixtures.ARR.id,
                prisinfo = Avtale.Prisinfo.Egendefinert(
                    listOf(PrismodellFixtures.AvtaltPrisPerManedsverk.copy(valuta = Valuta.NOK)),
                ),
            )

            RammedetaljerValidator.validateRammedetaljer(
                ctx,
                RammedetaljerRequest(
                    totalRamme = 1000,
                    utbetaltArena = -1,
                ),
            ).shouldBeLeft().shouldContainExactlyInAnyOrder(
                listOf(
                    FieldError("/utbetaltArena", "Utbetalt beløp fra Arena må være et positivt beløp"),
                ),
            )
        }

        test("Skal kunne validere korrekt") {
            val ctx = RammedetaljerValidator.Ctx(
                avtaleId = AvtaleFixtures.ARR.id,
                prisinfo = Avtale.Prisinfo.Egendefinert(
                    listOf(PrismodellFixtures.AvtaltPrisPerManedsverk.copy(valuta = Valuta.NOK)),
                ),
            )

            RammedetaljerValidator.validateRammedetaljer(
                ctx,
                RammedetaljerRequest(
                    totalRamme = 1000,
                    utbetaltArena = null,
                ),
            ).isRight() shouldBe true

            RammedetaljerValidator.validateRammedetaljer(
                ctx,
                RammedetaljerRequest(
                    totalRamme = 1000,
                    utbetaltArena = 100,
                ),
            ).isRight() shouldBe true
        }
    }
})
