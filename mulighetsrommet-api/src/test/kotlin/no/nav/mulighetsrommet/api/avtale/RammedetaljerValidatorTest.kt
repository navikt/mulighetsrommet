package no.nav.mulighetsrommet.api.avtale

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.avtale.db.PrismodellDbo
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.avtale.model.RammedetaljerRequest
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.PrismodellFixtures
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.model.Valuta

class RammedetaljerValidatorTest : FunSpec({
    fun PrismodellDbo.toPrismodell() = Prismodell.from(this.type, this.id, this.valuta, this.prisbetingelser, this.satser ?: emptyList())

    context("rammedetaljer") {
        test("må være anskaffet tiltak") {
            val ikkeAnskaffetCtx = RammeDetaljerValidator.Ctx(
                avtaleId = AvtaleFixtures.AFT.id,
                prismodeller = listOf(PrismodellFixtures.ForhandsgodkjentAft.toPrismodell()),
            )

            RammeDetaljerValidator.validateRammedetaljer(
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
            val ctx = RammeDetaljerValidator.Ctx(
                avtaleId = AvtaleFixtures.ARR.id,
                prismodeller = listOf(
                    PrismodellFixtures.AvtaltPrisPerManedsverk.copy(valuta = Valuta.NOK).toPrismodell(),
                    PrismodellFixtures.AnnenAvtaltPris.copy(valuta = Valuta.SEK).toPrismodell(),
                ),
            )

            RammeDetaljerValidator.validateRammedetaljer(
                ctx,
                RammedetaljerRequest(
                    totalRamme = 1000,
                    utbetaltArena = null,
                ),
            ).shouldBeLeft().shouldContainExactlyInAnyOrder(
                listOf(
                    FieldError("/totalRamme", "Rammedetaljer kan kun legges til avtaler med én type valuta på prismodellene"),
                ),
            )
        }

        test("total ramme må være positivt beløp") {
            val ikkeAnskaffetCtx = RammeDetaljerValidator.Ctx(
                avtaleId = AvtaleFixtures.ARR.id,
                prismodeller = listOf(
                    PrismodellFixtures.AvtaltPrisPerManedsverk.copy(valuta = Valuta.NOK).toPrismodell(),
                ),
            )

            RammeDetaljerValidator.validateRammedetaljer(
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
            val ctx = RammeDetaljerValidator.Ctx(
                avtaleId = AvtaleFixtures.ARR.id,
                prismodeller = listOf(
                    PrismodellFixtures.AvtaltPrisPerManedsverk.copy(valuta = Valuta.NOK).toPrismodell(),
                ),
            )

            RammeDetaljerValidator.validateRammedetaljer(
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
            val ctx = RammeDetaljerValidator.Ctx(
                avtaleId = AvtaleFixtures.ARR.id,
                prismodeller = listOf(
                    PrismodellFixtures.AvtaltPrisPerManedsverk.copy(valuta = Valuta.NOK).toPrismodell(),
                ),
            )

            RammeDetaljerValidator.validateRammedetaljer(
                ctx,
                RammedetaljerRequest(
                    totalRamme = 1000,
                    utbetaltArena = null,
                ),
            ).isRight() shouldBe true

            RammeDetaljerValidator.validateRammedetaljer(
                ctx,
                RammedetaljerRequest(
                    totalRamme = 1000,
                    utbetaltArena = 100,
                ),
            ).isRight() shouldBe true
        }
    }
})
