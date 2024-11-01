package no.nav.mulighetsrommet.api.navenhet

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.server.plugins.*
import no.nav.mulighetsrommet.api.clients.norg2.Norg2EnhetStatus

class NavEnhetUtilsTest : FunSpec({
    context("SanityUtils") {
        test("Relevante statuser") {
            NavEnhetUtils.isRelevantEnhetStatus(Norg2EnhetStatus.AKTIV) shouldBe true
            NavEnhetUtils.isRelevantEnhetStatus(Norg2EnhetStatus.UNDER_AVVIKLING) shouldBe true
            NavEnhetUtils.isRelevantEnhetStatus(Norg2EnhetStatus.UNDER_ETABLERING) shouldBe true
            NavEnhetUtils.isRelevantEnhetStatus(Norg2EnhetStatus.NEDLAGT) shouldBe false
        }

        test("toType skal returnere typer med stor forbokstav") {
            NavEnhetUtils.toType("FYLKE") shouldBe "Fylke"
            NavEnhetUtils.toType("LOKAL") shouldBe "Lokal"
            NavEnhetUtils.toType("TILTAK") shouldBe "Tiltak"
            NavEnhetUtils.toType("ALS") shouldBe "Als"
            val exception = shouldThrow<BadRequestException> {
                NavEnhetUtils.toType("Ukjent type")
            }
            exception.localizedMessage shouldBe "'Ukjent type' er ikke en gyldig type for enhet. Gyldige typer er 'FYLKE', 'LOKAL', 'ALS', 'TILTAK'."
        }

        test("toStatus skal returnere status med stor forbokstav") {
            NavEnhetUtils.toStatus("AKTIV") shouldBe "Aktiv"
            NavEnhetUtils.toStatus("NEDLAGT") shouldBe "Nedlagt"
            NavEnhetUtils.toStatus("UNDER_ETABLERING") shouldBe "Under_etablering"
            NavEnhetUtils.toStatus("UNDER_AVVIKLING") shouldBe "Under_avvikling"
            val exception = shouldThrow<BadRequestException> {
                NavEnhetUtils.toStatus("Ukjent status")
            }
            exception.localizedMessage shouldBe "'Ukjent status' er ikke en gyldig status. Gyldige statuser er 'AKTIV', 'NEDLAGT', 'UNDER_ETABLERING', 'UNDER_AVVIKLING'"
        }
    }
})
