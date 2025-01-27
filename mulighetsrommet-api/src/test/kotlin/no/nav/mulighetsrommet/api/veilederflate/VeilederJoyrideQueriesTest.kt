package no.nav.mulighetsrommet.api.veilederflate

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.veilederflate.models.JoyrideType
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederJoyrideDto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.NavIdent

class VeilederJoyrideQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val navIdent = NavIdent("S123456")

    test("Lagre kjørt-status for Joyride fra veileder") {
        database.runAndRollback {
            val queries = VeilederJoyrideQueries(it)

            val joyrideKjortForOversikten = VeilederJoyrideDto(
                navIdent = navIdent,
                fullfort = true,
                type = JoyrideType.OVERSIKT,
            )
            queries.upsert(joyrideKjortForOversikten)

            val joyrideKjortForDetaljside = VeilederJoyrideDto(
                navIdent = navIdent,
                fullfort = false,
                type = JoyrideType.DETALJER,
            )
            queries.upsert(joyrideKjortForDetaljside)

            queries.harFullfortJoyride(navIdent, JoyrideType.OVERSIKT) shouldBe true
            queries.harFullfortJoyride(navIdent, JoyrideType.DETALJER) shouldBe false
        }
    }

    test("Returnerer false hvis veileder ikke har kjørt en spesifkk joyride tidligere") {
        database.runAndRollback {
            val queries = VeilederJoyrideQueries(it)

            val result = queries.harFullfortJoyride(
                navIdent = navIdent,
                type = JoyrideType.OVERSIKT,
            )

            result shouldBe false
        }
    }
})
