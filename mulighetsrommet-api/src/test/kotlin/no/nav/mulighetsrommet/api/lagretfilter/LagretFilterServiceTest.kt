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
            val filter1 = LagretFilter(
                id = UUID.randomUUID(),
                navn = "Avtalefilter for Benny",
                type = LagretFilterType.AVTALE,
                filter = Json.parseToJsonElement("""{"filter": "filter1"}"""),
                sortOrder = 0,
            )
            val filter2 = LagretFilter(
                id = UUID.randomUUID(),
                navn = "Gjennomføringsfilter for Johnny",
                type = LagretFilterType.GJENNOMFORING,
                filter = Json.parseToJsonElement("""{"filter": "filter1"}"""),
                sortOrder = 0,
            )

            lagretFilterService.upsertFilter(brukerId = bruker1Id, filter1).shouldBeRight()
            lagretFilterService.upsertFilter(brukerId = bruker2Id, filter2).shouldBeRight()

            val lagretFilterForBenny = lagretFilterService.getLagredeFiltereForBruker(
                brukerId = bruker1Id,
                LagretFilterType.AVTALE,
            )

            lagretFilterForBenny.shouldHaveSize(1).first() shouldBe filter1

            lagretFilterService.deleteFilter(brukerId = bruker1Id, filter1.id).shouldBeRight()

            lagretFilterService.getLagredeFiltereForBruker(
                bruker1Id,
                LagretFilterType.AVTALE,
            ).shouldBeEmpty()
        }

        test("Skal bare godta ett default filter per filter-type") {
            val filter1 = LagretFilter(
                id = UUID.randomUUID(),
                navn = "Filter 1",
                type = LagretFilterType.AVTALE,
                filter = Json.parseToJsonElement("""{"filter": 1}"""),
                isDefault = true,
                sortOrder = 0,
            )
            val filter2 = LagretFilter(
                id = UUID.randomUUID(),
                navn = "Filter 2",
                type = LagretFilterType.AVTALE,
                filter = Json.parseToJsonElement("""{"filter": 2}"""),
                isDefault = false,
                sortOrder = 1,
            )

            lagretFilterService.upsertFilter(brukerId = bruker1Id, filter1).shouldBeRight()
            lagretFilterService.upsertFilter(brukerId = bruker1Id, filter2).shouldBeRight()

            lagretFilterService.getLagredeFiltereForBruker(
                brukerId = bruker1Id,
                LagretFilterType.AVTALE,
            ) shouldBe listOf(filter1, filter2)

            val newDefaultFilter = filter2.copy(isDefault = true)
            lagretFilterService.upsertFilter(brukerId = bruker1Id, newDefaultFilter).shouldBeRight()

            lagretFilterService.getLagredeFiltereForBruker(
                brukerId = bruker1Id,
                LagretFilterType.AVTALE,
            ) shouldBe listOf(
                filter1.copy(isDefault = false),
                newDefaultFilter,
            )

            lagretFilterService.deleteFilter(brukerId = bruker1Id, filter1.id).shouldBeRight()
            lagretFilterService.deleteFilter(brukerId = bruker1Id, filter2.id).shouldBeRight()
        }

        test("Skal godta default filter per filter-type") {
            val filter1 = LagretFilter(
                id = UUID.randomUUID(),
                navn = "Default avtale",
                type = LagretFilterType.AVTALE,
                filter = Json.parseToJsonElement("""{"filter": 1}"""),
                isDefault = true,
                sortOrder = 0,
            )
            val filter2 = LagretFilter(
                id = UUID.randomUUID(),
                navn = "Default gjennomføring",
                type = LagretFilterType.GJENNOMFORING,
                filter = Json.parseToJsonElement("""{"filter": 2}"""),
                isDefault = true,
                sortOrder = 1,
            )

            lagretFilterService.upsertFilter(brukerId = bruker1Id, filter1).shouldBeRight()
            lagretFilterService.upsertFilter(brukerId = bruker1Id, filter2).shouldBeRight()

            lagretFilterService.getLagredeFiltereForBruker(
                brukerId = bruker1Id,
                LagretFilterType.AVTALE,
            ) shouldBe listOf(filter1)

            lagretFilterService.getLagredeFiltereForBruker(
                brukerId = bruker1Id,
                LagretFilterType.GJENNOMFORING,
            ) shouldBe listOf(filter2)

            lagretFilterService.deleteFilter(brukerId = bruker1Id, filter1.id).shouldBeRight()
            lagretFilterService.deleteFilter(brukerId = bruker1Id, filter2.id).shouldBeRight()
        }

        test("Skal ikke ha tilgang til andre brukere sine filtre") {
            val filter1 = LagretFilter(
                id = UUID.randomUUID(),
                navn = "Avtalefilter for Benny",
                type = LagretFilterType.AVTALE,
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

            lagretFilterService.deleteFilter(brukerId = bruker1Id, filter1.id).shouldBeRight()
        }
    }
})
