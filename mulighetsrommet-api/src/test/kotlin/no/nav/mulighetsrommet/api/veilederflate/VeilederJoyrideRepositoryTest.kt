package no.nav.mulighetsrommet.api.veilederflate

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.veilederflate.models.JoyrideType
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederJoyrideDto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.dto.NavIdent

class VeilederJoyrideRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    afterEach {
        database.db.truncateAll()
    }

    context("crud") {
        test("Lagre kjørt-status for Joyride fra veileder") {
            val veilederJoyrideRepository = VeilederJoyrideRepository(database.db)

            val joyrideKjortForOversikten = VeilederJoyrideDto(
                navIdent = NavIdent("S123456"),
                fullfort = true,
                type = JoyrideType.OVERSIKT,
            )

            val joyrideKjortForDetaljside = VeilederJoyrideDto(
                navIdent = NavIdent("S123456"),
                fullfort = true,
                type = JoyrideType.DETALJER,
            )
            veilederJoyrideRepository.upsert(joyrideKjortForOversikten).shouldBeRight()
            veilederJoyrideRepository.upsert(joyrideKjortForDetaljside).shouldBeRight()
        }
    }

    test("Returnerer true hvis veileder har kjørt en spesifkk joyride tidligere") {
        val veilederJoyrideRepository = VeilederJoyrideRepository(database.db)
        val navident = NavIdent("S123456")
        val joyrideKjortForOversikten = VeilederJoyrideDto(
            navIdent = navident,
            fullfort = true,
            type = JoyrideType.OVERSIKT,
        )

        veilederJoyrideRepository.upsert(joyrideKjortForOversikten).shouldBeRight()

        val result = veilederJoyrideRepository.harFullfortJoyride(
            navIdent = navident,
            type = JoyrideType.OVERSIKT,
        )
        result shouldBe true
    }

    test("Returnerer false hvis veileder ikke har kjørt en spesifkk joyride tidligere") {
        val veilederJoyrideRepository = VeilederJoyrideRepository(database.db)
        val navident = NavIdent("S123456")
        val joyrideKjortForOversikten = VeilederJoyrideDto(
            navIdent = navident,
            fullfort = false,
            type = JoyrideType.OVERSIKT,
        )

        veilederJoyrideRepository.upsert(joyrideKjortForOversikten).shouldBeRight()

        val result = veilederJoyrideRepository.harFullfortJoyride(
            navIdent = navident,
            type = JoyrideType.OVERSIKT,
        )
        result shouldBe false
    }
})
