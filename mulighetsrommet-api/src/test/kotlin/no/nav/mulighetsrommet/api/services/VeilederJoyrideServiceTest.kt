package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dto.JoyrideType
import no.nav.mulighetsrommet.api.domain.dto.VeilederJoyrideDto
import no.nav.mulighetsrommet.api.repositories.VeilederJoyrideRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll

class VeilederJoyrideServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    beforeEach {
        database.db.truncateAll()
    }

    context("Lagre at veileder har kjørt Joyride") {
        val veilederJoyrideRepository = VeilederJoyrideRepository(database.db)
        val veilederJoyrideService = VeilederJoyrideService(
            veilederJoyrideRepository,
        )

        test("Skal kunne lagre at veileder har kjørt Joyride") {
            val request = VeilederJoyrideDto(navident = "S123456", fullfort = true, type = JoyrideType.OVERSIKT)

            veilederJoyrideService.save(request)
        }
    }
})
