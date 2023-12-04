package no.nav.mulighetsrommet.api.repositories

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dto.JoyrideType
import no.nav.mulighetsrommet.api.domain.dto.VeilederJoyrideDto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll

class JoyrideVeilederRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    afterEach {
        database.db.truncateAll()
    }

    context("crud") {
        test("Lagre kjørt-status for Joyride fra veileder") {
            val veilederJoyrideRepository = VeilederJoyrideRepository(database.db)

            val joyrideKjortForOversikten = VeilederJoyrideDto(
                navIdent = "S123456",
                fullfort = true,
                type = JoyrideType.OVERSIKT,
            )

            val joyrideKjortForDetaljside = VeilederJoyrideDto(
                navIdent = "S123456",
                fullfort = true,
                type = JoyrideType.DETALJER,
            )
            veilederJoyrideRepository.upsert(joyrideKjortForOversikten).shouldBeRight()
            veilederJoyrideRepository.upsert(joyrideKjortForDetaljside).shouldBeRight()
        }

        test("Lagre kjørt-status for Joyride fra veileder krasjer hvis primærnøkkelen brytes") {
            val veilederJoyrideRepository = VeilederJoyrideRepository(database.db)

            val detaljer1 = VeilederJoyrideDto(
                navIdent = "S123456",
                fullfort = true,
                type = JoyrideType.DETALJER,
            )

            val detaljer2 = VeilederJoyrideDto(
                navIdent = "S123456",
                fullfort = true,
                type = JoyrideType.DETALJER,
            )
            veilederJoyrideRepository.upsert(detaljer1).shouldBeRight()
            veilederJoyrideRepository.upsert(detaljer2).shouldBeLeft()
        }
    }

    test("Returnerer true hvis veileder har kjørt en spesifkk joyride tidligere") {
        val veilederJoyrideRepository = VeilederJoyrideRepository(database.db)
        val navident = "S123456"
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
        val navident = "S123456"
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
