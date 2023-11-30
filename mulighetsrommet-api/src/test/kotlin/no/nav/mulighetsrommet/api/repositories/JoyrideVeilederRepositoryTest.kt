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

    beforeEach {
        database.db.truncateAll()
    }

    context("crud") {
        test("Lagre kjørt-status for Joyride fra veileder") {
            val veilederJoyrideRepository = VeilederJoyrideRepository(database.db)

            val joyrideKjortForOversikten = VeilederJoyrideDto(
                navident = "S123456",
                fullfort = true,
                type = JoyrideType.OVERSIKT,
            )

            val joyrideKjortForDetaljside = VeilederJoyrideDto(
                navident = "S123456",
                fullfort = true,
                type = JoyrideType.DETALJER,
            )
            veilederJoyrideRepository.save(joyrideKjortForOversikten).shouldBeRight()
            veilederJoyrideRepository.save(joyrideKjortForDetaljside).shouldBeRight()
        }

        test("Lagre kjørt-status for Joyride fra veileder krasjer hvis primærnøkkelen brytes") {
            val veilederJoyrideRepository = VeilederJoyrideRepository(database.db)

            val detaljer1 = VeilederJoyrideDto(
                navident = "S123456",
                fullfort = true,
                type = JoyrideType.DETALJER,
            )

            val detaljer2 = VeilederJoyrideDto(
                navident = "S123456",
                fullfort = true,
                type = JoyrideType.DETALJER,
            )
            veilederJoyrideRepository.save(detaljer1).shouldBeRight()
            veilederJoyrideRepository.save(detaljer2).shouldBeLeft()
        }
    }

    test("Returnerer true hvis veileder har kjørt en spesifkk joyride tidligere") {
        val veilederJoyrideRepository = VeilederJoyrideRepository(database.db)
        val navident = "S123456"
        val joyrideKjortForOversikten = VeilederJoyrideDto(
            navident = navident,
            fullfort = true,
            type = JoyrideType.OVERSIKT,
        )

        val joyrideKjortForDetaljside = VeilederJoyrideDto(
            navident = navident,
            fullfort = true,
            type = JoyrideType.DETALJER,
        )
        veilederJoyrideRepository.save(joyrideKjortForOversikten).shouldBeRight()
        veilederJoyrideRepository.save(joyrideKjortForDetaljside).shouldBeRight()

        val result = veilederJoyrideRepository.harFullfortJoyride(
            navident = navident,
            type = JoyrideType.OVERSIKT,
        )
        result shouldBe true
    }

    test("Returnerer false hvis veileder ikke har kjørt en spesifkk joyride tidligere") {
        val veilederJoyrideRepository = VeilederJoyrideRepository(database.db)
        val navident = "S123456"
        val joyrideKjortForOversikten = VeilederJoyrideDto(
            navident = navident,
            fullfort = false,
            type = JoyrideType.OVERSIKT,
        )

        val joyrideKjortForDetaljside = VeilederJoyrideDto(
            navident = navident,
            fullfort = true,
            type = JoyrideType.DETALJER,
        )
        veilederJoyrideRepository.save(joyrideKjortForOversikten).shouldBeRight()
        veilederJoyrideRepository.save(joyrideKjortForDetaljside).shouldBeRight()

        val result = veilederJoyrideRepository.harFullfortJoyride(
            navident = navident,
            type = JoyrideType.OVERSIKT,
        )
        result shouldBe false
    }
})
