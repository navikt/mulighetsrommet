package no.nav.mulighetsrommet.api.repositories

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import java.time.LocalDateTime
import java.util.*

class UtkastRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    beforeEach {
        database.db.truncateAll()
        val navAnsatte = NavAnsattRepository(database.db)
        val enheter = NavEnhetRepository(database.db)
        enheter.upsert(
            NavEnhetDbo(
                navn = "IT-avdelingen",
                enhetsnummer = "2990",
                status = NavEnhetStatus.AKTIV,
                type = Norg2Type.LOKAL,
                overordnetEnhet = null,
            ),
        )
        navAnsatte.upsert(
            NavAnsattDbo(
                navIdent = "B123456",
                fornavn = "Bertil",
                etternavn = "Betabruker",
                hovedenhet = "2990",
                azureId = UUID.randomUUID(),
                mobilnummer = null,
                epost = "",
                roller = emptyList(),
                skalSlettesDato = null,
            ),
        )
    }

    context("CRUD for Utkast") {
        val utkastRepository = UtkastRepository(database.db)

        test("Upsert, Get og Delete") {
            val utkastId = UUID.randomUUID()
            val utkast = UtkastDbo(
                id = utkastId,
                opprettetAv = "B123456",
                utkastData = "{\"id\":\"123\",\"navn\":\"Min gjennomføring er kul\"}",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                type = Utkasttype.Tiltaksgjennomforing,
            )
            utkastRepository.upsert(utkast).shouldBeRight().should {
                it?.opprettetAv shouldBe "B123456"
                it?.utkastData shouldContain "Min gjennomføring er kul"
                it?.type shouldBe Utkasttype.Tiltaksgjennomforing
            }

            val redigertUtkast = utkast.copy(utkastData = "{\"id\":\"123\",\"navn\":\"Min gjennomføring er fet\"}")
            utkastRepository.upsert(redigertUtkast).shouldBeRight().should {
                it?.opprettetAv shouldBe "B123456"
                it?.utkastData shouldContain "Min gjennomføring er fet"
                it?.type shouldBe Utkasttype.Tiltaksgjennomforing
            }

            utkastRepository.get(utkastId).shouldBeRight().should {
                it?.id shouldBe utkastId
                it?.opprettetAv shouldBe "B123456"
                it?.utkastData shouldContain "Min gjennomføring er fet"
                it?.type shouldBe Utkasttype.Tiltaksgjennomforing
            }

            utkastRepository.delete(utkastId).shouldBeRight()

            utkastRepository.get(utkastId).shouldBeRight(null)
        }
    }
})
