package no.nav.mulighetsrommet.api.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures.underenhet1
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures.underenhet2
import no.nav.mulighetsrommet.api.services.ArrangorRolle
import no.nav.mulighetsrommet.api.services.ArrangorRolleType
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import java.time.LocalDateTime
import java.util.*

class ArrangorAnsattRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val ansatt1 = ArrangorAnsatt(
        id = UUID.randomUUID(),
        norskIdent = NorskIdent("12345678901"),
    )
    val ansatt2 = ArrangorAnsatt(
        id = UUID.randomUUID(),
        norskIdent = NorskIdent("22345010203"),
    )
    val rolle1 = ArrangorRolle(
        arrangorId = underenhet1.id,
        rolle = ArrangorRolleType.TILTAK_ARRANGOR_REFUSJON,
        expiry = LocalDateTime.of(2024, 1, 1, 0, 0),
    )
    val rolle2 = ArrangorRolle(
        arrangorId = underenhet2.id,
        rolle = ArrangorRolleType.TILTAK_ARRANGOR_REFUSJON,
        expiry = LocalDateTime.of(2024, 1, 1, 0, 0),
    )

    test("CRUD") {
        val arrangorRepository = ArrangorRepository(database.db)
        val ansatteRepository = ArrangorAnsattRepository(database.db)
        arrangorRepository.upsert(underenhet1)
        arrangorRepository.upsert(underenhet2)

        ansatteRepository.upsertAnsatt(ansatt1)
        ansatteRepository.upsertAnsatt(ansatt2)

        ansatteRepository.upsertRoller(ansatt1.id, listOf(rolle1))
        ansatteRepository.upsertRoller(ansatt2.id, listOf(rolle2))

        ansatteRepository.getRoller(ansatt1.norskIdent) shouldBe listOf(rolle1)
        ansatteRepository.getRoller(ansatt2.norskIdent) shouldBe listOf(rolle2)
        ansatteRepository.getAnsatte().map { it.id } shouldContainExactlyInAnyOrder listOf(ansatt1.id, ansatt2.id)
    }
})
