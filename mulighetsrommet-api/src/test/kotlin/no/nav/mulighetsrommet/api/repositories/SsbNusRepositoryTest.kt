package no.nav.mulighetsrommet.api.repositories

import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.api.clients.ssb.ClassificationItem
import no.nav.mulighetsrommet.api.clients.ssb.SsbNusData
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener

class SsbNusRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    test("CRUD") {
        val ssbNusRepository = SsbNusRepository(database.db)
        val ssbData = SsbNusData(
            classificationItems = listOf(
                ClassificationItem("1", "1", "1", "Høyere utdanning"),
                ClassificationItem("2", "2", "2", "Barnehage"),
                ClassificationItem("3", "3", "3", "Grunnskole"),
            ),
            validFrom = "20240425",
        )

        ssbNusRepository.upsert(ssbData, "1")
    }
})
