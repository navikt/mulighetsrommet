package no.nav.mulighetsrommet.api.repositories

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.*
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.utils.UtkastFilter
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import java.time.LocalDateTime
import java.util.*

class UtkastRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))
    val avtaleFixture = AvtaleFixtures(database)

    val domain = MulighetsrommetTestDomain()

    beforeContainer {
        avtaleFixture.runBeforeTests()
    }

    beforeEach {

        domain.initialize(database.db)

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
                roller = emptySet(),
                skalSlettesDato = null,
            ),
        )
        navAnsatte.upsert(
            NavAnsattDbo(
                navIdent = "P998877",
                fornavn = "Per",
                etternavn = "Pilotbruker",
                hovedenhet = "2990",
                azureId = UUID.randomUUID(),
                mobilnummer = null,
                epost = "",
                roller = emptySet(),
                skalSlettesDato = null,
            ),
        )
    }

    context("CRUD for Utkast") {
        val utkastRepository = UtkastRepository(database.db)
        val avtale = avtaleFixture.createAvtaleForTiltakstype()
        avtaleFixture.upsertAvtaler(listOf(avtale))

        val utkastId = UUID.randomUUID()
        val utkast = UtkastDbo(
            id = utkastId,
            opprettetAv = domain.ansatt1.navIdent,
            utkastData = Json.parseToJsonElement("{\"id\":\"123\",\"navn\":\"Min gjennomføring er kul\"}"),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            type = Utkasttype.Tiltaksgjennomforing,
            avtaleId = avtale.id,
        )

        test("Upsert, Get og Delete") {
            val utkastId = UUID.randomUUID()
            val utkast = UtkastDbo(
                id = utkastId,
                opprettetAv = domain.ansatt1.navIdent,
                utkastData = Json.parseToJsonElement("{\"id\":\"123\",\"navn\":\"Min gjennomføring er kul\"}"),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                type = Utkasttype.Tiltaksgjennomforing,
                avtaleId = avtale.id,
            )
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

        test("GetAll skal støtte filter for type og opprettetAv") {
            val avtale = avtaleFixture.createAvtaleForTiltakstype()
            avtaleFixture.upsertAvtaler(listOf(avtale))
            val utkast1 = UtkastDbo(
                id = UUID.randomUUID(),
                opprettetAv = domain.ansatt1.navIdent,
                utkastData = Json.parseToJsonElement("{\"id\":\"123\",\"navn\":\"Min gjennomføring er kul\"}"),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                type = Utkasttype.Tiltaksgjennomforing,
                avtaleId = avtale.id,
            )
            val utkast2 = UtkastDbo(
                id = UUID.randomUUID(),
                opprettetAv = domain.ansatt1.navIdent,
                utkastData = Json.parseToJsonElement("{\"id\":\"123\",\"navn\":\"Min gjennomføring er fet\"}"),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                type = Utkasttype.Tiltaksgjennomforing,
                avtaleId = avtale.id,
            )
            val utkast3 = UtkastDbo(
                id = UUID.randomUUID(),
                opprettetAv = domain.ansatt2.navIdent,
                utkastData = Json.parseToJsonElement("{\"id\":\"123\",\"navn\":\"Min avtale er fet\"}"),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                type = Utkasttype.Avtale,
                avtaleId = avtale.id,
            )
            val utkast4 = UtkastDbo(
                id = UUID.randomUUID(),
                opprettetAv = domain.ansatt2.navIdent,
                utkastData = Json.parseToJsonElement("{\"id\":\"123\",\"navn\":\"Min tiltaksgjennomføring er rar\"}"),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                type = Utkasttype.Tiltaksgjennomforing,
                avtaleId = avtale.id,
            )
            utkastRepository.upsert(utkast1).shouldBeRight()
            utkastRepository.upsert(utkast2).shouldBeRight()
            utkastRepository.upsert(utkast3).shouldBeRight()
            utkastRepository.upsert(utkast4).shouldBeRight()

            utkastRepository.getAll(
                filter = UtkastFilter(
                    type = Utkasttype.Avtale,
                    opprettetAv = null,
                    avtaleId = avtale.id,
                ),
            ).shouldBeRight()
                .should {
                    it.size shouldBe 1
                    it[0].opprettetAv shouldBe domain.ansatt2.navIdent
                }

            utkastRepository.getAll(
                filter = UtkastFilter(
                    type = Utkasttype.Tiltaksgjennomforing,
                    opprettetAv = domain.ansatt1.navIdent,
                    avtaleId = avtale.id,
                ),
            ).shouldBeRight()
                .should {
                    it.size shouldBe 2
                }

            utkastRepository.getAll(
                filter = UtkastFilter(
                    type = Utkasttype.Tiltaksgjennomforing,
                    opprettetAv = null,
                    avtaleId = avtale.id,
                ),
            ).shouldBeRight()
                .should {
                    it.size shouldBe 3
                }
        }
    }
})
