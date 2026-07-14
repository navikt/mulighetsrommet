package no.nav.mulighetsrommet.admin.arrangor

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.admin.testing.TestAdminDatabase
import no.nav.mulighetsrommet.api.domain.arrangor.Betalingsinformasjon
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.model.Kontonummer

class BetalingsinformasjonQueryTest : FunSpec({
    val arrangor = ArrangorFixtures.hovedenhet
    val utenlandsk = ArrangorFixtures.Utenlandsk.hovedenhet

    test("henter IBan for utenlandsk arrangør") {
        val db = TestAdminDatabase()
        db.repository.arrangor.save(
            utenlandsk.copy(
                betalingsinformasjon = Betalingsinformasjon.IBan(
                    bic = "DABANOKKXXX",
                    iban = "NO9386011117947",
                    bankNavn = "Danske Bank",
                    bankLandKode = "NO",
                ),
            ),
        )

        val result = BetalingsinformasjonQuery(db, mockk()).execute(HentBetalingsinformasjon(utenlandsk.id))

        result shouldBe Betalingsinformasjon.IBan(
            bic = "DABANOKKXXX",
            iban = "NO9386011117947",
            bankNavn = "Danske Bank",
            bankLandKode = "NO",
        )
    }

    test("kaster exception når utenlandsk arrangør mangler betalingsinformasjon") {
        val db = TestAdminDatabase()
        db.repository.arrangor.save(utenlandsk.copy(betalingsinformasjon = null))

        shouldThrow<IllegalArgumentException> {
            BetalingsinformasjonQuery(db, mockk()).execute(HentBetalingsinformasjon(utenlandsk.id))
        }
    }

    test("kaster exception når arrangør ikke finnes") {
        val db = TestAdminDatabase()

        shouldThrow<IllegalArgumentException> {
            BetalingsinformasjonQuery(db, mockk()).execute(HentBetalingsinformasjon(arrangor.id))
        }
    }

    test("henter BBan for norsk arrangør via kontoregister") {
        val kontoregister = mockk<KontoregisterGateway> {
            coEvery { hentKontonummer(arrangor.organisasjonsnummer) } returns Kontonummer("12345678901").right()
        }

        val db = TestAdminDatabase()
        db.repository.arrangor.save(arrangor)

        val result = BetalingsinformasjonQuery(db, kontoregister).execute(HentBetalingsinformasjon(arrangor.id))

        result shouldBe Betalingsinformasjon.BBan(Kontonummer("12345678901"), null)
    }

    test("returnerer null når kontoregister ikke finner kontonummer") {
        val kontoregister = mockk<KontoregisterGateway> {
            coEvery { hentKontonummer(arrangor.organisasjonsnummer) } returns KontoregisterError.IkkeFunnet.left()
        }

        val db = TestAdminDatabase()
        db.repository.arrangor.save(arrangor)

        val result = BetalingsinformasjonQuery(db, kontoregister).execute(HentBetalingsinformasjon(arrangor.id))

        result shouldBe null
    }

    test("kaster exception når kontoregister feiler") {
        val kontoregister = mockk<KontoregisterGateway> {
            coEvery { hentKontonummer(arrangor.organisasjonsnummer) } returns KontoregisterError.Feil.left()
        }

        val db = TestAdminDatabase()
        db.repository.arrangor.save(arrangor)

        shouldThrow<IllegalStateException> {
            BetalingsinformasjonQuery(db, kontoregister).execute(HentBetalingsinformasjon(arrangor.id))
        }
    }
})
