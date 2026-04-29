package no.nav.mulighetsrommet.api.tiltakstype.service

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tiltakstype.model.RedaksjoneltInnholdLenke
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import java.util.UUID

class RegelverklenkeServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    beforeEach {
        database.truncateAll()
    }

    fun createService() = RedaksjoneltInnholdLenkeService(db = database.db)

    fun create(url: String, navn: String? = null, beskrivelse: String? = null) = RedaksjoneltInnholdLenke(id = UUID.randomUUID(), url = url, navn = navn, beskrivelse = beskrivelse)

    context("upsert") {
        test("oppretter ny og oppdaterer eksisterende lenke når ID allerede finnes") {
            val service = createService()
            val id = UUID.randomUUID()

            service.upsert(
                RedaksjoneltInnholdLenke(
                    id = id,
                    url = "https://gammel.example.com",
                    navn = "Gammel lenke",
                    beskrivelse = null,
                ),
            ).should { created ->
                created.id shouldBe id
                created.url shouldBe "https://gammel.example.com"
                created.navn shouldBe "Gammel lenke"
                created.beskrivelse.shouldBeNull()
            }

            service.upsert(
                RedaksjoneltInnholdLenke(
                    id = id,
                    url = "https://ny.example.com",
                    navn = "Ny lenke",
                    beskrivelse = "Beskrivelse",
                ),
            ).should { updated ->
                updated.id shouldBe id
                updated.url shouldBe "https://ny.example.com"
                updated.navn shouldBe "Ny lenke"
                updated.beskrivelse shouldBe "Beskrivelse"
            }

            service.getAll() shouldHaveSize 1
        }
    }

    context("getById") {
        test("returnerer null når lenken ikke finnes") {
            val service = createService()

            service.getById(UUID.randomUUID()).shouldBeNull()
        }

        test("returnerer lenken med korrekte felter") {
            val service = createService()

            val saved =
                service.upsert(create("https://nav.no/regelverk", "Regelverk", "Beskrivelse av regelverket"))

            val result = service.getById(saved.id).shouldNotBeNull()

            result.id shouldBe saved.id
            result.url shouldBe "https://nav.no/regelverk"
            result.navn shouldBe "Regelverk"
            result.beskrivelse shouldBe "Beskrivelse av regelverket"
        }
    }

    context("getAll") {
        test("returnerer tom liste når det ikke finnes regelverkslenker") {
            val service = createService()

            service.getAll() shouldBe emptyList()
        }

        test("returnerer alle lenker sortert på URL") {
            val service = createService()

            service.upsert(create("https://z.example.com", "Z-lenke"))
            service.upsert(create("https://a.example.com", "A-lenke"))

            val result = service.getAll()

            result shouldHaveSize 2
            result[0].url shouldBe "https://a.example.com"
            result[1].url shouldBe "https://z.example.com"
        }
    }

    context("delete") {
        test("returnerer ok selv når lenken ikke finnes") {
            val service = createService()

            service.delete(UUID.randomUUID()).shouldBeRight()
        }

        test("sletter lenken når den ikke er i bruk") {
            val service = createService()

            val link = service.upsert(create("https://slett.example.com"))

            service.delete(link.id).shouldBeRight()

            service.getById(link.id).shouldBeNull()

            service.getAll().shouldBeEmpty()
        }

        test("returnerer valideringsfeil når lenken er i bruk") {
            val service = createService()

            val link = service.upsert(create("https://bruk.example.com"))

            database.db.transaction {
                queries.tiltakstype.upsert(TiltakstypeFixtures.AFT)
                queries.tiltakstype.upsert(TiltakstypeFixtures.VTA)
                queries.tiltakstype.setFaglenker(TiltakstypeFixtures.AFT.id, listOf(link.id))
                queries.tiltakstype.setFaglenker(TiltakstypeFixtures.VTA.id, listOf(link.id))
            }

            service.delete(link.id) shouldBeLeft listOf(
                FieldError.of("Lenken er i bruk av tiltakstypen «Arbeidsforberedende trening»"),
                FieldError.of("Lenken er i bruk av tiltakstypen «Varig tilrettelagt arbeid i skjermet virksomhet»"),
            )
        }
    }
})
