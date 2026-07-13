package no.nav.mulighetsrommet.api.arrangor

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.admin.arrangor.KontoregisterError
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontonummerRegisterOrganisasjonError
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontonummerResponse
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer

class KontoregisterOrganisasjonGatewayTest : FunSpec({
    val orgnr = Organisasjonsnummer("123456789")

    test("mapper vellykket oppslag til kontonummer") {
        val client = mockk<KontoregisterOrganisasjonClient>()
        coEvery { client.getKontonummerForOrganisasjon(orgnr) } answers {
            KontonummerResponse(mottaker = "Fretex AS", kontonr = "12345678901").right()
        }

        val result = KontoregisterOrganisasjonGateway(client).hentKontonummer(orgnr)

        result.shouldBeRight(Kontonummer("12345678901"))
    }

    test("mapper FantIkkeKontonummer til IkkeFunnet") {
        val client = mockk<KontoregisterOrganisasjonClient>()
        coEvery { client.getKontonummerForOrganisasjon(orgnr) } answers {
            KontonummerRegisterOrganisasjonError.FantIkkeKontonummer.left()
        }

        val result = KontoregisterOrganisasjonGateway(client).hentKontonummer(orgnr)

        result.shouldBeLeft(KontoregisterError.IkkeFunnet)
    }

    test("mapper UgyldigInput til Feil") {
        val client = mockk<KontoregisterOrganisasjonClient>()
        coEvery { client.getKontonummerForOrganisasjon(orgnr) } answers {
            KontonummerRegisterOrganisasjonError.UgyldigInput.left()
        }

        val result = KontoregisterOrganisasjonGateway(client).hentKontonummer(orgnr)

        result.shouldBeLeft(KontoregisterError.Feil)
    }

    test("mapper Error til Feil") {
        val client = mockk<KontoregisterOrganisasjonClient>()
        coEvery { client.getKontonummerForOrganisasjon(orgnr) } answers {
            KontonummerRegisterOrganisasjonError.Error.left()
        }

        val result = KontoregisterOrganisasjonGateway(client).hentKontonummer(orgnr)

        result.shouldBeLeft(KontoregisterError.Feil)
    }
})
