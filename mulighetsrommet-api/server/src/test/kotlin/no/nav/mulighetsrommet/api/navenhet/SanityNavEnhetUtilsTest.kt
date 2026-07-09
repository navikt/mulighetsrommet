package no.nav.mulighetsrommet.api.navenhet

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.server.plugins.BadRequestException

class SanityNavEnhetUtilsTest : FunSpec({
    context("SanityUtils") {
        test("toType skal returnere typer med stor forbokstav") {
            SanityNavEnhetUtils.toType("FYLKE") shouldBe "Fylke"
            SanityNavEnhetUtils.toType("LOKAL") shouldBe "Lokal"
            SanityNavEnhetUtils.toType("TILTAK") shouldBe "Tiltak"
            SanityNavEnhetUtils.toType("ALS") shouldBe "Als"
            val exception = shouldThrow<BadRequestException> {
                SanityNavEnhetUtils.toType("Ukjent type")
            }
            exception.localizedMessage shouldBe "'Ukjent type' er ikke en gyldig type for enhet. Gyldige typer er 'FYLKE', 'LOKAL', 'ALS', 'TILTAK'."
        }

        test("toStatus skal returnere status med stor forbokstav") {
            SanityNavEnhetUtils.toStatus("AKTIV") shouldBe "Aktiv"
            SanityNavEnhetUtils.toStatus("NEDLAGT") shouldBe "Nedlagt"
            SanityNavEnhetUtils.toStatus("UNDER_ETABLERING") shouldBe "Under_etablering"
            SanityNavEnhetUtils.toStatus("UNDER_AVVIKLING") shouldBe "Under_avvikling"
            val exception = shouldThrow<BadRequestException> {
                SanityNavEnhetUtils.toStatus("Ukjent status")
            }
            exception.localizedMessage shouldBe "'Ukjent status' er ikke en gyldig status. Gyldige statuser er 'AKTIV', 'NEDLAGT', 'UNDER_ETABLERING', 'UNDER_AVVIKLING'"
        }
    }
})
