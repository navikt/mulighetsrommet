package no.nav.mulighetsrommet.altinn.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.altinn.model.AltinnRessurs
import no.nav.mulighetsrommet.altinn.model.BedriftRettigheter
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures.underenhet1
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures.underenhet2
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import java.time.LocalDateTime

class AltinnRettigheterRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val norskIdent1 = NorskIdent("12345678901")
    val rettighet1 = PersonBedriftRettigheterDbo(
        norskIdent = norskIdent1,
        bedriftRettigheter = listOf(
            BedriftRettigheter(
                organisasjonsnummer = underenhet1.organisasjonsnummer,
                rettigheter = listOf(AltinnRessurs.TILTAK_ARRANGOR_REFUSJON),
            ),
        ),
        expiry = LocalDateTime.of(2024, 1, 1, 0, 0),
    )

    val norskIdent2 = NorskIdent("42345678903")
    val rettighet2 = PersonBedriftRettigheterDbo(
        norskIdent = norskIdent2,
        bedriftRettigheter = listOf(
            BedriftRettigheter(
                organisasjonsnummer = underenhet2.organisasjonsnummer,
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
                    it.rettigheter.map { ressurs ->
                        RettighetDbo(
                            rettighet = ressurs,
                            expiry = LocalDateTime.of(2024, 1, 1, 0, 0),
                        )
                    },
                )
            }
    }
})
