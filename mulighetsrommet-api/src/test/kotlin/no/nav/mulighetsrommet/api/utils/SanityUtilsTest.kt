package no.nav.mulighetsrommet.api.utils

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.server.plugins.*
import no.nav.mulighetsrommet.api.clients.norg2.Norg2EnhetDto
import no.nav.mulighetsrommet.api.clients.norg2.Norg2EnhetStatus
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Response
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type

class SanityUtilsTest : FunSpec({
    context("SanityUtils") {
        test("isUnderliggendeEnhet skal returnere true hvis enhet er underenhet av fylke") {
            val fylkesenhet = Norg2EnhetDto(
                enhetId = 1,
                navn = "NAV Vestland",
                enhetNr = "1200",
                status = Norg2EnhetStatus.AKTIV,
                type = Norg2Type.FYLKE,
            )

            val enhet = Norg2Response(
                overordnetEnhet = "1200",
                enhet = Norg2EnhetDto(
                    enhetId = 2,
                    navn = "NAV Bergen Vest",
                    enhetNr = "1289",
                    status = Norg2EnhetStatus.AKTIV,
                    type = Norg2Type.LOKAL,
                ),
            )
            val result = SanityUtils.isUnderliggendeEnhet(fylkesenhet, enhet)
            result shouldBe true
        }

        test("isUnderliggendeEnhet skal returnere false hvis enhet ikke er underenhet av fylke") {
            val fylkesenhet = Norg2EnhetDto(
                enhetId = 1,
                navn = "NAV Vestland",
                enhetNr = "1200",
                status = Norg2EnhetStatus.AKTIV,
                type = Norg2Type.FYLKE,
            )

            val enhet = Norg2Response(
                overordnetEnhet = "1890",
                enhet = Norg2EnhetDto(
                    enhetId = 2,
                    navn = "NAV Arbeidslivsenter Vestland",
                    enhetNr = "1576",
                    status = Norg2EnhetStatus.AKTIV,
                    type = Norg2Type.ALS,
                ),
            )
            val result = SanityUtils.isUnderliggendeEnhet(fylkesenhet, enhet)
            result shouldBe false
        }

        test("Relevante statuser") {
            SanityUtils.relevanteStatuser(Norg2EnhetStatus.AKTIV) shouldBe true
            SanityUtils.relevanteStatuser(Norg2EnhetStatus.UNDER_AVVIKLING) shouldBe true
            SanityUtils.relevanteStatuser(Norg2EnhetStatus.UNDER_ETABLERING) shouldBe true
            SanityUtils.relevanteStatuser(Norg2EnhetStatus.NEDLAGT) shouldBe false
        }

        test("toType skal returnere typer med stor forbokstav") {
            SanityUtils.toType("FYLKE") shouldBe "Fylke"
            SanityUtils.toType("LOKAL") shouldBe "Lokal"
            SanityUtils.toType("TILTAK") shouldBe "Tiltak"
            SanityUtils.toType("ALS") shouldBe "Als"
            val exception = shouldThrow<BadRequestException> {
                SanityUtils.toType("Ukjent type")
            }
            exception.localizedMessage shouldBe "'Ukjent type' er ikke en gyldig type for enhet. Gyldige typer er 'FYLKE', 'LOKAL', 'ALS', 'TILTAK'."
        }

        test("toStatus skal returnere status med stor forbokstav") {
            SanityUtils.toStatus("AKTIV") shouldBe "Aktiv"
            SanityUtils.toStatus("NEDLAGT") shouldBe "Nedlagt"
            SanityUtils.toStatus("UNDER_ETABLERING") shouldBe "Under_etablering"
            SanityUtils.toStatus("UNDER_AVVIKLING") shouldBe "Under_avvikling"
            val exception = shouldThrow<BadRequestException> {
                SanityUtils.toStatus("Ukjent status")
            }
            exception.localizedMessage shouldBe "'Ukjent status' er ikke en gyldig status. Gyldige statuser er 'AKTIV', 'NEDLAGT', 'UNDER_ETABLERING', 'UNDER_AVVIKLING'"
        }
    }
})
