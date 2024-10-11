package no.nav.mulighetsrommet.altinn

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.altinn.models.AltinnRessurs
import no.nav.mulighetsrommet.altinn.models.BedriftRettigheter
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures.underenhet1
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures.underenhet2
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import java.time.LocalDateTime
import java.util.*

class AltinnRettigheterRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val norskIdent1 = NorskIdent("12345678901")
    val rettighet1 = PersonBedriftRettigheter(
        norskIdent = norskIdent1,
        bedriftRettigheter = listOf(
            BedriftRettigheter(
                organisasjonsnummer = Organisasjonsnummer(underenhet1.organisasjonsnummer),
                rettigheter = listOf(AltinnRessurs.TILTAK_ARRANGOR_REFUSJON),
            ),
        ),
        expiry = LocalDateTime.of(2024, 1, 1, 0, 0),
    )

    val norskIdent2 = NorskIdent("42345678903")
    val rettighet2 = PersonBedriftRettigheter(
        norskIdent = norskIdent2,
        bedriftRettigheter = listOf(
            BedriftRettigheter(
                organisasjonsnummer = Organisasjonsnummer(underenhet2.organisasjonsnummer),
                rettigheter = listOf(AltinnRessurs.TILTAK_ARRANGOR_REFUSJON),
            ),
        ),
        expiry = LocalDateTime.of(2024, 1, 1, 0, 0),
    )

    test("CRUD") {
        val altinnRettigheterRepository = AltinnRettigheterRepository(database.db)
        altinnRettigheterRepository.upsertRettighet(rettighet1)
        altinnRettigheterRepository.upsertRettighet(rettighet2)

        altinnRettigheterRepository.getRettigheter(norskIdent1) shouldBe rettighet1
            .bedriftRettigheter
            .map {
                BedriftRettigheterDbo(
                    it.organisasjonsnummer,
                    it.rettigheter.map {
                        RettighetDbo(
                            rettighet = it,
                            expiry = LocalDateTime.of(2024, 1, 1, 0, 0),
                        )
                    },
                )
            }
    }
})
