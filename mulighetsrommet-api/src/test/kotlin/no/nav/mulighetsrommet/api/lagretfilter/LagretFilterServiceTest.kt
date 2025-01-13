package no.nav.mulighetsrommet.api.lagretfilter

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener

class LagretFilterServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    context("CRUD LagreFilterService") {
        val lagretFilterService = LagretFilterService(database.db)

        test("Skal kunne lagre og hente ut lagrede filter for bruker") {
            val filter1 = LagretFilterUpsert(
                brukerId = "B123456",
                navn = "Avtalefilter for Benny",
                type = FilterDokumentType.Avtale,
                filter = Json.parseToJsonElement("""{"filter": "filter1"}"""),
                sortOrder = 0,
            )
            val filter2 = LagretFilterUpsert(
                brukerId = "J987654",
                navn = "Gjennomføringsfilter for Johnny",
                type = FilterDokumentType.Tiltaksgjennomføring,
                filter = Json.parseToJsonElement("""{"filter": "filter1"}"""),
                sortOrder = 0,
            )

            lagretFilterService.upsertFilter(filter1)
            lagretFilterService.upsertFilter(filter2)

            val lagretFilterForBenny = lagretFilterService.getLagredeFiltereForBruker(
                brukerId = "B123456",
                FilterDokumentType.Avtale,
            )
            lagretFilterForBenny.shouldHaveSize(1).first().navn shouldBe "Avtalefilter for Benny"

            lagretFilterService.deleteFilter(lagretFilterForBenny[0].id)

            lagretFilterService.getLagredeFiltereForBruker(
                "B123456",
                FilterDokumentType.Avtale,
            ).shouldBeEmpty()
        }
    }
})
