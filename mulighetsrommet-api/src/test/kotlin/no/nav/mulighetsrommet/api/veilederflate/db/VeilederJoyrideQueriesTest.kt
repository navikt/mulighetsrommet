package no.nav.mulighetsrommet.api.veilederflate.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.veilederflate.models.JoyrideType
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederJoyrideDto
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.NavIdent

class VeilederJoyrideQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val navIdent = NavIdent("S123456")

    test("Lagre kjørt-status for Joyride fra veileder") {
        database.runAndRollback {
            val joyrideKjortForOversikten = VeilederJoyrideDto(
                navIdent = navIdent,
                fullfort = true,
                type = JoyrideType.OVERSIKT,
            )
            queries.veilederJoyride.upsert(joyrideKjortForOversikten)

            val joyrideKjortForDetaljside = VeilederJoyrideDto(
                navIdent = navIdent,
                fullfort = false,
                type = JoyrideType.DETALJER,
            )
            queries.veilederJoyride.upsert(joyrideKjortForDetaljside)

            queries.veilederJoyride.harFullfortJoyride(navIdent, JoyrideType.OVERSIKT) shouldBe true
            queries.veilederJoyride.harFullfortJoyride(navIdent, JoyrideType.DETALJER) shouldBe false
        }
    }

    test("Returnerer false hvis veileder ikke har kjørt en spesifkk joyride tidligere") {
        database.runAndRollback {
            val result = queries.veilederJoyride.harFullfortJoyride(
                navIdent = navIdent,
                type = JoyrideType.OVERSIKT,
            )

            result shouldBe false
        }
    }
})
