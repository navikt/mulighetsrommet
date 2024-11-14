package no.nav.mulighetsrommet.api.services

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import java.time.LocalDateTime

class LagretFilterServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

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
                sistBrukt = null,
            )
            val filter2 = UpsertFilterEntry(
                brukerId = "J987654",
                navn = "Gjennomføringsfilter for Johnny",
                type = UpsertFilterEntry.FilterDokumentType.Tiltaksgjennomføring,
                filter = Json.parseToJsonElement("""{"filter": "filter1"}"""),
                sortOrder = 0,
                sistBrukt = null,
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

        test("Skal sette sist brukt tidspunkt for filter") {
            val filter = UpsertFilterEntry(
                brukerId = "B123456",
                navn = "Avtalefilter for Benny",
                type = UpsertFilterEntry.FilterDokumentType.Avtale,
                filter = Json.parseToJsonElement("""{"filter": "filter1"}"""),
                sortOrder = 0,
                sistBrukt = null,
            )

            lagretFilterService.upsertFilter(filter).shouldBeRight()
            val lagretFilter = lagretFilterService.getLagredeFiltereForBruker("B123456", UpsertFilterEntry.FilterDokumentType.Avtale).shouldBeRight()
            lagretFilter.size shouldBe 1
            lagretFilter[0].navn shouldBe "Avtalefilter for Benny"
            lagretFilter[0].sistBrukt shouldBe null

            val sistBruktTidspunkt = LocalDateTime.of(2024, 10, 1, 12, 0)
            lagretFilterService.updateSistBruktTimestamp(lagretFilter[0].id, sistBruktTidspunkt)

            val updatedFilter = lagretFilterService.getLagredeFiltereForBruker("B123456", UpsertFilterEntry.FilterDokumentType.Avtale).shouldBeRight()
            updatedFilter.size shouldBe 1
            updatedFilter[0].navn shouldBe "Avtalefilter for Benny"
            updatedFilter[0].sistBrukt shouldBe sistBruktTidspunkt
        }

        test("Skal fjerne sist brukt tidspunkt for bruker") {
            val sistBruktTidspunkt = LocalDateTime.of(2024, 10, 1, 12, 0)
            val filter1 = UpsertFilterEntry(
                brukerId = "B123456",
                navn = "Avtalefilter for Benny",
                type = UpsertFilterEntry.FilterDokumentType.Avtale,
                filter = Json.parseToJsonElement("""{"filter": "filter1"}"""),
                sortOrder = 0,
                sistBrukt = sistBruktTidspunkt,
            )

            val filter2 = UpsertFilterEntry(
                brukerId = "B123456",
                navn = "Gjennomføringsfilter for Benny",
                type = UpsertFilterEntry.FilterDokumentType.Tiltaksgjennomføring,
                filter = Json.parseToJsonElement("""{"filter": "filter2"}"""),
                sortOrder = 0,
                sistBrukt = sistBruktTidspunkt,
            )

            lagretFilterService.upsertFilter(filter1).shouldBeRight()
            lagretFilterService.upsertFilter(filter2).shouldBeRight()
            lagretFilterService.clearSistBruktTimestampForBruker("B123456", UpsertFilterEntry.FilterDokumentType.Avtale)
            val avtaleFilterForBenny = lagretFilterService.getLagredeFiltereForBruker("B123456", UpsertFilterEntry.FilterDokumentType.Avtale).shouldBeRight()
            avtaleFilterForBenny.size shouldBe 1
            avtaleFilterForBenny[0].navn shouldBe "Avtalefilter for Benny"
            avtaleFilterForBenny[0].sistBrukt shouldBe null

            val gjennomforingFilterForBenny = lagretFilterService.getLagredeFiltereForBruker("B123456", UpsertFilterEntry.FilterDokumentType.Tiltaksgjennomføring).shouldBeRight()
            gjennomforingFilterForBenny.size shouldBe 1
            gjennomforingFilterForBenny[0].navn shouldBe "Gjennomføringsfilter for Benny"
            gjennomforingFilterForBenny[0].sistBrukt shouldBe sistBruktTidspunkt
        }
    }
})
