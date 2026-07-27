package no.nav.mulighetsrommet.api.domain.arrangor

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.time.LocalDate
import java.util.UUID

class ArrangorTest : FunSpec({
    context("Norsk") {
        test("setter defaultverdier for kontaktpersoner, slettetDato og overordnetEnhet") {
            val arrangor = Arrangor.Norsk.opprett(
                id = UUID.randomUUID(),
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                organisasjonsform = "AS",
                navn = "Testbedrift AS",
            )

            arrangor.kontaktpersoner.shouldBeEmpty()
            arrangor.slettetDato.shouldBeNull()
            arrangor.overordnetEnhet.shouldBeNull()
        }

        test("setter kontaktpersoner som tilhører arrangøren") {
            val arrangor = ArrangorFixtures.hovedenhet
            val kontaktperson1 = ArrangorFixtures.kontaktperson(
                arrangorId = arrangor.id,
                navn = "Kari Nordmann",
            )
            val kontaktperson2 = ArrangorFixtures.kontaktperson(arrangorId = arrangor.id, navn = "Ola Nordmann")

            val oppdatert = arrangor.registrerKontaktpersoner(listOf(kontaktperson1, kontaktperson2))

            oppdatert.kontaktpersoner shouldContainExactly listOf(kontaktperson1, kontaktperson2)
        }

        test("feiler dersom minst én kontaktperson tilhører en annen arrangør") {
            val arrangor = ArrangorFixtures.hovedenhet
            val kontaktpersonForAnnenArrangor =
                ArrangorFixtures.kontaktperson(id = UUID.randomUUID(), arrangorId = UUID.randomUUID())

            shouldThrow<IllegalArgumentException> {
                arrangor.registrerKontaktpersoner(listOf(kontaktpersonForAnnenArrangor))
            }.message shouldBe "Kontaktpersonene må tilhøre denne arrangøren"
        }

        test("setter slettetDato") {
            val arrangor = ArrangorFixtures.hovedenhet
            val dato = LocalDate.of(2025, 1, 1)

            val oppdatert = arrangor.registrerSlettet(dato)

            oppdatert.slettetDato shouldBe dato
        }

        test("oppdaterer navn og organisasjonsform, men beholder andre felter") {
            val arrangor = ArrangorFixtures.hovedenhet

            val oppdatert = arrangor.registrerVirksomhet(navn = "Nytt navn AS", organisasjonsform = "ENK")

            oppdatert.navn shouldBe "Nytt navn AS"
            oppdatert.organisasjonsform shouldBe "ENK"
            oppdatert.id shouldBe arrangor.id
            oppdatert.organisasjonsnummer shouldBe arrangor.organisasjonsnummer
        }
    }

    context("Utenlandsk") {
        test("setter defaultverdier for kontaktpersoner, betalingsinformasjon og adresse") {
            val arrangor = Arrangor.Utenlandsk.opprett(
                id = UUID.randomUUID(),
                organisasjonsnummer = Organisasjonsnummer("100000009"),
                navn = "Utenlandsk Tiger",
            )

            arrangor.kontaktpersoner.shouldBeEmpty()
            arrangor.betalingsinformasjon.shouldBeNull()
            arrangor.adresse.shouldBeNull()
            arrangor.slettetDato.shouldBeNull()
        }

        test("setter kontaktpersoner som tilhører arrangøren") {
            val arrangor = ArrangorFixtures.Utenlandsk.hovedenhet
            val kontaktperson = ArrangorFixtures.kontaktperson(
                arrangorId = arrangor.id,
                navn = "John Doe",
            )

            val oppdatert = arrangor.registrerKontaktpersoner(listOf(kontaktperson))

            oppdatert.kontaktpersoner shouldContainExactly listOf(kontaktperson)
        }

        test("feiler dersom en kontaktperson tilhører en annen arrangør") {
            val arrangor = ArrangorFixtures.Utenlandsk.hovedenhet
            val kontaktpersonForAnnenArrangor = ArrangorFixtures.kontaktperson(
                arrangorId = UUID.randomUUID(),
                navn = "John Doe",
            )

            shouldThrow<IllegalArgumentException> {
                arrangor.registrerKontaktpersoner(listOf(kontaktpersonForAnnenArrangor))
            }.message shouldBe "Kontaktpersonene må tilhøre denne arrangøren"
        }

        test("setter betalingsinformasjon og adresse sammen") {
            val arrangor = ArrangorFixtures.Utenlandsk.hovedenhet
            val betalingsinformasjon = Betalingsinformasjon.IBan(
                bic = "DEUTDEFF",
                iban = "DE89370400440532013000",
                bankNavn = "Deutsche Bank",
                bankLandKode = "DE",
            )
            val adresse = Arrangor.Utenlandsk.Adresse(
                gateNavn = "Hovedgata 1",
                by = "Berlin",
                postNummer = "10115",
                landKode = "DE",
            )

            val oppdatert = arrangor.registrerBetalingsinformasjon(betalingsinformasjon, adresse)

            oppdatert.betalingsinformasjon shouldBe betalingsinformasjon
            oppdatert.adresse shouldBe adresse
        }

        test("setter slettetDato") {
            val arrangor = ArrangorFixtures.Utenlandsk.hovedenhet
            val dato = LocalDate.of(2025, 1, 1)

            val oppdatert = arrangor.registrerSlettet(dato)

            oppdatert.slettetDato shouldBe dato
        }
    }
})
