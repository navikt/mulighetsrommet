package no.nav.mulighetsrommet.api.repositories

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.UtkastDbo
import no.nav.mulighetsrommet.api.domain.dbo.Utkasttype
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import java.time.LocalDateTime
import java.util.*

class UtkastRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val domain = MulighetsrommetTestDomain()

    beforeEach {
        database.db.truncateAll()
        domain.initialize(database.db)
    }

    context("CRUD for Utkast") {
        val utkastRepository = UtkastRepository(database.db)

        val utkastId = UUID.randomUUID()
        val utkast = UtkastDbo(
            id = utkastId,
            opprettetAv = domain.ansatt1.navIdent,
            utkastData = Json.parseToJsonElement("{\"id\":\"123\",\"navn\":\"Min gjennomføring er kul\"}"),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            type = Utkasttype.Tiltaksgjennomforing,
        )

        test("Upsert, Get og Delete") {
            utkastRepository.upsert(utkast).shouldBeRight().should {
                it?.opprettetAv shouldBe domain.ansatt1.navIdent
                Json.encodeToString(it?.utkastData) shouldContain "Min gjennomføring er kul"
                it?.type shouldBe Utkasttype.Tiltaksgjennomforing
            }

            val redigertUtkast = utkast.copy(
                utkastData = Json.parseToJsonElement("{\"id\":\"123\",\"navn\":\"Min gjennomføring er fet\"}"),
            )
            utkastRepository.upsert(redigertUtkast).shouldBeRight().should {
                it?.opprettetAv shouldBe domain.ansatt1.navIdent
                Json.encodeToString(it?.utkastData) shouldContain "Min gjennomføring er fet"
                it?.type shouldBe Utkasttype.Tiltaksgjennomforing
            }

            utkastRepository.get(utkastId).shouldBeRight().should {
                it?.id shouldBe utkastId
                it?.opprettetAv shouldBe domain.ansatt1.navIdent
                Json.encodeToString(it?.utkastData) shouldContain "Min gjennomføring er fet"
                it?.type shouldBe Utkasttype.Tiltaksgjennomforing
            }

            utkastRepository.delete(utkastId).shouldBeRight()

            utkastRepository.get(utkastId).shouldBeRight(null)
        }

        test("Utkast til ansatt blir slettet når den ansatte blir slettet") {
            val ansatte = NavAnsattRepository(database.db)

            utkastRepository.upsert(utkast).shouldBeRight()
            ansatte.deleteByAzureId(domain.ansatt1.azureId).shouldBeRight()

            utkastRepository.get(utkastId).shouldBeRight(null)
        }
    }
})
