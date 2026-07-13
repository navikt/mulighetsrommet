package no.nav.mulighetsrommet.admin.arrangor

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import no.nav.mulighetsrommet.admin.testing.TestAdminDatabase
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.domain.arrangor.ArrangorKontaktperson
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.util.UUID

class ArrangorKontaktpersonServiceTest : FunSpec({
    val arrangorId = UUID.randomUUID()

    fun arrangor(kontaktpersoner: List<ArrangorKontaktperson> = emptyList()) = Arrangor.Norsk(
        id = arrangorId,
        overordnetEnhet = null,
        organisasjonsnummer = Organisasjonsnummer("123456789"),
        organisasjonsform = "AS",
        navn = "Fretex AS",
        kontaktpersoner = kontaktpersoner,
    )

    fun kontaktperson(id: UUID = UUID.randomUUID()) = ArrangorKontaktperson(
        id = id,
        arrangorId = arrangorId,
        navn = "Kari Nordmann",
        beskrivelse = null,
        telefon = null,
        epost = "kari@example.com",
        ansvarligFor = listOf(ArrangorKontaktperson.Ansvar.AVTALE),
    )

    context("upsert") {
        test("legger til ny kontaktperson på arrangør") {
            val db = TestAdminDatabase()
            val service = ArrangorKontaktpersonService(db)
            db.repository.arrangor.save(arrangor())

            val person = kontaktperson()
            service.upsert(person)

            db.repository.arrangor.get(arrangorId).kontaktpersoner shouldBe listOf(person)
        }

        test("erstatter eksisterende kontaktperson med samme id") {
            val db = TestAdminDatabase()
            val service = ArrangorKontaktpersonService(db)
            val person = kontaktperson()
            db.repository.arrangor.save(arrangor(listOf(person)))

            val oppdatert = person.copy(navn = "Ola Nordmann")
            service.upsert(oppdatert)

            db.repository.arrangor.get(arrangorId).kontaktpersoner shouldBe listOf(oppdatert)
        }

        test("kaster exception når arrangør ikke finnes") {
            val db = TestAdminDatabase()
            val service = ArrangorKontaktpersonService(db)

            shouldThrow<IllegalArgumentException> {
                service.upsert(kontaktperson())
            }
        }
    }

    context("delete") {
        test("sletter kontaktperson som ikke er i bruk") {
            val db = TestAdminDatabase()
            val service = ArrangorKontaktpersonService(db)
            val person = kontaktperson()
            db.repository.arrangor.save(arrangor(listOf(person)))
            every { db.queries.arrangor.koblingerTilKontaktperson(person.id) } returns (emptyList<DokumentKoblingForKontaktperson>() to emptyList())

            service.delete(arrangorId, person.id).shouldBeRight()

            db.repository.arrangor.get(arrangorId).kontaktpersoner shouldBe emptyList()
        }

        test("returnerer KontaktpersonErIBruk når kontaktperson har koblinger til gjennomføringer") {
            val db = TestAdminDatabase()
            val person = kontaktperson()
            db.repository.arrangor.save(arrangor(listOf(person)))
            val kobling = DokumentKoblingForKontaktperson(UUID.randomUUID(), "Gjennomføring 1")
            every { db.queries.arrangor.koblingerTilKontaktperson(person.id) } returns Pair(
                listOf(kobling),
                listOf(),
            )

            ArrangorKontaktpersonService(db).delete(arrangorId, person.id)
                .shouldBeLeft(ArrangorKontaktpersonError.KontaktpersonErIBruk)
        }

        test("returnerer KontaktpersonErIBruk når kontaktperson har koblinger til avtaler") {
            val db = TestAdminDatabase()
            val person = kontaktperson()
            db.repository.arrangor.save(arrangor(listOf(person)))
            val kobling = DokumentKoblingForKontaktperson(UUID.randomUUID(), "Avtale 1")
            every { db.queries.arrangor.koblingerTilKontaktperson(person.id) } returns Pair(
                listOf(),
                listOf(kobling),
            )

            ArrangorKontaktpersonService(db).delete(arrangorId, person.id)
                .shouldBeLeft(ArrangorKontaktpersonError.KontaktpersonErIBruk)
        }

        test("kaster exception når arrangør ikke finnes") {
            val db = TestAdminDatabase()

            shouldThrow<IllegalArgumentException> {
                ArrangorKontaktpersonService(db).delete(UUID.randomUUID(), UUID.randomUUID())
            }
        }
    }

    context("hentAlle") {
        test("henter alle kontaktpersoner for arrangør") {
            val db = TestAdminDatabase()
            val kontaktpersoner = listOf(kontaktperson())
            every { db.queries.arrangor.getKontaktpersoner(arrangorId) } returns kontaktpersoner

            ArrangorKontaktpersonService(db).hentAlle(arrangorId) shouldBe kontaktpersoner
        }
    }

    context("hentKoblinger") {
        test("henter koblinger for kontaktperson") {
            val db = TestAdminDatabase()
            val kontaktpersonId = UUID.randomUUID()
            val gjennomforing = DokumentKoblingForKontaktperson(UUID.randomUUID(), "Gjennomføring 1")
            val avtale = DokumentKoblingForKontaktperson(UUID.randomUUID(), "Avtale 1")
            every { db.queries.arrangor.koblingerTilKontaktperson(kontaktpersonId) } returns Pair(
                listOf(gjennomforing),
                listOf(avtale),
            )

            ArrangorKontaktpersonService(db).hentKoblinger(kontaktpersonId) shouldBe KoblingerForKontaktperson(
                gjennomforinger = listOf(gjennomforing),
                avtaler = listOf(avtale),
            )
        }
    }
})
