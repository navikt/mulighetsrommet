package no.nav.mulighetsrommet.api.lagretfilter

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import java.util.*

class LagretFilterServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val bruker1Id = "B123456"
    val bruker2Id = "J987654"

    context("LagretFilterService") {
        val lagretFilterService = LagretFilterService(database.db)

        test("Skal kunne lagre og hente ut lagrede filter for bruker") {
            val filter1 = LagretFilterRequest(
                id = UUID.randomUUID(),
                navn = "Avtalefilter for Benny",
                type = FilterDokumentType.AVTALE,
                filter = Json.parseToJsonElement("""{"filter": "filter1"}"""),
                sortOrder = 0,
            )
            val filter2 = LagretFilterRequest(
                id = UUID.randomUUID(),
                navn = "Gjennomføringsfilter for Johnny",
                type = FilterDokumentType.GJENNOMFORING,
                filter = Json.parseToJsonElement("""{"filter": "filter1"}"""),
                sortOrder = 0,
            )

            lagretFilterService.upsertFilter(brukerId = bruker1Id, filter1).shouldBeRight()
            lagretFilterService.upsertFilter(brukerId = bruker2Id, filter2).shouldBeRight()

            val lagretFilterForBenny = lagretFilterService.getLagredeFiltereForBruker(
                brukerId = bruker1Id,
                FilterDokumentType.AVTALE,
            )

            lagretFilterForBenny.shouldHaveSize(1).first().navn shouldBe "Avtalefilter for Benny"

            lagretFilterService.deleteFilter(brukerId = bruker1Id, filter1.id).shouldBeRight()

            lagretFilterService.getLagredeFiltereForBruker(
                bruker1Id,
                FilterDokumentType.AVTALE,
            ).shouldBeEmpty()
        }

        test("Skal ikke ha tilgang til andre brukere sine filtre") {
            val filter1 = LagretFilterRequest(
                id = UUID.randomUUID(),
                navn = "Avtalefilter for Benny",
                type = FilterDokumentType.AVTALE,
                filter = Json.parseToJsonElement("""{"filter": "filter1"}"""),
                sortOrder = 0,
            )

            lagretFilterService.upsertFilter(brukerId = bruker1Id, filter1).shouldBeRight()

            lagretFilterService.upsertFilter(brukerId = bruker2Id, filter1)
                .shouldBeLeft()
                .shouldBeTypeOf<LagretFilterError.Forbidden>()

            lagretFilterService.deleteFilter(brukerId = bruker2Id, filter1.id)
                .shouldBeLeft()
                .shouldBeTypeOf<LagretFilterError.Forbidden>()
        }
    }
})
