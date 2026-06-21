package no.nav.mulighetsrommet.api.persistence.redaksjoneltinnhold

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.domain.redaksjoneltinnhold.RedaksjoneltInnholdLenke
import no.nav.mulighetsrommet.api.persistence.SqlApiDatabaseTestListener
import java.util.UUID

class RedaksjoneltInnholdLenkeQueriesTest : FunSpec({
    val database = extension(SqlApiDatabaseTestListener())

    beforeEach {
        database.truncateAll()
    }

    fun lenke(url: String, navn: String? = null, beskrivelse: String? = null) = RedaksjoneltInnholdLenke(
        id = UUID.randomUUID(),
        url = url,
        navn = navn,
        beskrivelse = beskrivelse,
    )

    context("upsert") {
        test("oppretter ny lenke") {
            val lenke = lenke("https://nav.no", "Nav")

            database.run { repository.redaksjoneltInnholdLenke.upsert(lenke) } shouldBe lenke
        }

        test("oppdaterer eksisterende lenke ved konflikt på id") {
            val lenke = lenke("https://nav.no", "Gammel")
            database.run { repository.redaksjoneltInnholdLenke.upsert(lenke) }

            val updated = lenke.copy(url = "https://nytt.nav.no", navn = "Ny")
            database.run { repository.redaksjoneltInnholdLenke.upsert(updated) } shouldBe updated

            database.run { repository.redaksjoneltInnholdLenke.getAll() } shouldHaveSize 1
        }
    }

    context("get") {
        test("returnerer null når lenken ikke finnes") {
            database.run { repository.redaksjoneltInnholdLenke.get(UUID.randomUUID()) }.shouldBeNull()
        }

        test("returnerer lenken med alle felter") {
            val lenke = lenke("https://nav.no/regelverk", "Regelverk", "Beskrivelse")
            database.run { repository.redaksjoneltInnholdLenke.upsert(lenke) }

            database.run { repository.redaksjoneltInnholdLenke.get(lenke.id) }.shouldNotBeNull() shouldBe lenke
        }
    }

    context("getAll") {
        test("returnerer tom liste") {
            database.run { repository.redaksjoneltInnholdLenke.getAll() }.shouldBeEmpty()
        }

        test("returnerer lenker sortert på url") {
            database.run {
                repository.redaksjoneltInnholdLenke.upsert(lenke("https://z.example.com"))
                repository.redaksjoneltInnholdLenke.upsert(lenke("https://a.example.com"))
            }

            database.run { repository.redaksjoneltInnholdLenke.getAll() }.let { result ->
                result shouldHaveSize 2
                result[0].url shouldBe "https://a.example.com"
                result[1].url shouldBe "https://z.example.com"
            }
        }
    }

    context("delete") {
        test("sletter lenken og returnerer 1") {
            val lenke = lenke("https://slett.example.com")
            database.run { repository.redaksjoneltInnholdLenke.upsert(lenke) }

            database.run { repository.redaksjoneltInnholdLenke.delete(lenke.id) } shouldBe 1
            database.run { repository.redaksjoneltInnholdLenke.get(lenke.id) }.shouldBeNull()
        }

        test("returnerer 0 når lenken ikke finnes") {
            database.run { repository.redaksjoneltInnholdLenke.delete(UUID.randomUUID()) } shouldBe 0
        }
    }
})
