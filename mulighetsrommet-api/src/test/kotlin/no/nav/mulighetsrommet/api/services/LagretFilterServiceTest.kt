package no.nav.mulighetsrommet.api.services

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll

class LagretFilterServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    afterEach {
        database.db.truncateAll()
    }

    context("CRUD LagreFilterService") {
        val lagretFilterService = LagretFilterService(database.db)
        test("Skal kunne lagre og hente ut lagrede filter for bruker") {
            val filter1 = UpsertFilterEntry(
                brukerId = "B123456",
                navn = "Avtalefilter for Benny",
                type = UpsertFilterEntry.FilterDokumentType.Avtale,
                filter = Json.parseToJsonElement("""{"filter": "filter1"}"""),
                sortOrder = 0,
            )
            val filter2 = UpsertFilterEntry(
                brukerId = "J987654",
                navn = "Gjennomføringsfilter for Johnny",
                type = UpsertFilterEntry.FilterDokumentType.Tiltaksgjennomføring,
                filter = Json.parseToJsonElement("""{"filter": "filter1"}"""),
                sortOrder = 0,
            )

            lagretFilterService.upsertFilter(filter1).shouldBeRight()
            lagretFilterService.upsertFilter(filter2).shouldBeRight()

            val lagretFilterForBenny =
                lagretFilterService.getLagredeFiltereForBruker("B123456", UpsertFilterEntry.FilterDokumentType.Avtale).shouldBeRight()
            lagretFilterForBenny.size shouldBe 1
            lagretFilterForBenny[0].navn shouldBe "Avtalefilter for Benny"

            lagretFilterService.deleteFilter(lagretFilterForBenny[0].id).shouldBeRight()

            val bennysFiltere =
                lagretFilterService.getLagredeFiltereForBruker("B123456", UpsertFilterEntry.FilterDokumentType.Avtale).shouldBeRight()
            bennysFiltere.size shouldBe 0
        }
    }
})
