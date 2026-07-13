package no.nav.mulighetsrommet.admin.arrangor

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.admin.testing.TestAdminDatabase
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.domain.arrangor.Betalingsinformasjon
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.util.UUID

class BetalingsinformasjonQueryTest : FunSpec({
    val arrangorId = UUID.randomUUID()
    val organisasjonsnummer = Organisasjonsnummer("123456789")

    fun norskArrangor() = Arrangor.Norsk(
        id = arrangorId,
        organisasjonsnummer = organisasjonsnummer,
        organisasjonsform = "AS",
        navn = "Fretex AS",
    )

    fun utenlandskArrangor(
        betalingsinformasjon: Betalingsinformasjon.IBan? = null,
    ) = Arrangor.Utenlandsk(
        id = arrangorId,
        organisasjonsnummer = organisasjonsnummer,
        organisasjonsform = "AS",
        navn = "Fretex AS",
        betalingsinformasjon = betalingsinformasjon,
    )

    test("henter IBan for utenlandsk arrangør") {
        val db = TestAdminDatabase()
        db.repository.arrangor.save(
            utenlandskArrangor(
                betalingsinformasjon = Betalingsinformasjon.IBan(
                    bic = "DABANOKKXXX",
                    iban = "NO9386011117947",
                    bankNavn = "Danske Bank",
                    bankLandKode = "NO",
                ),
            ),
        )

        val result = BetalingsinformasjonQuery(db, mockk()).execute(HentBetalingsinformasjon(arrangorId))

        result shouldBe Betalingsinformasjon.IBan(
            bic = "DABANOKKXXX",
            iban = "NO9386011117947",
            bankNavn = "Danske Bank",
            bankLandKode = "NO",
        )
    }

    test("kaster exception når utenlandsk arrangør mangler betalingsinformasjon") {
        val db = TestAdminDatabase()
        db.repository.arrangor.save(utenlandskArrangor())

        shouldThrow<IllegalArgumentException> {
            BetalingsinformasjonQuery(db, mockk()).execute(HentBetalingsinformasjon(arrangorId))
        }
    }

    test("kaster exception når arrangør ikke finnes") {
        val db = TestAdminDatabase()

        shouldThrow<IllegalArgumentException> {
            BetalingsinformasjonQuery(db, mockk()).execute(HentBetalingsinformasjon(arrangorId))
        }
    }

    test("henter BBan for norsk arrangør via kontoregister") {
        val db = TestAdminDatabase()
        db.repository.arrangor.save(norskArrangor())
        val kontoregister = mockk<KontoregisterGateway>()
        coEvery { kontoregister.hentKontonummer(organisasjonsnummer) } returns Kontonummer("12345678901").right()

        val result = BetalingsinformasjonQuery(db, kontoregister).execute(HentBetalingsinformasjon(arrangorId))

        result shouldBe Betalingsinformasjon.BBan(Kontonummer("12345678901"), null)
    }

    test("returnerer null når kontoregister ikke finner kontonummer") {
        val db = TestAdminDatabase()
        db.repository.arrangor.save(norskArrangor())
        val kontoregister = mockk<KontoregisterGateway>()
        coEvery { kontoregister.hentKontonummer(organisasjonsnummer) } returns KontoregisterError.IkkeFunnet.left()

        val result = BetalingsinformasjonQuery(db, kontoregister).execute(HentBetalingsinformasjon(arrangorId))

        result shouldBe null
    }

    test("kaster exception når kontoregister feiler") {
        val db = TestAdminDatabase()
        db.repository.arrangor.save(norskArrangor())
        val kontoregister = mockk<KontoregisterGateway>()
        coEvery { kontoregister.hentKontonummer(organisasjonsnummer) } returns KontoregisterError.Feil.left()

        shouldThrow<IllegalStateException> {
            BetalingsinformasjonQuery(db, kontoregister).execute(HentBetalingsinformasjon(arrangorId))
        }
    }
})
