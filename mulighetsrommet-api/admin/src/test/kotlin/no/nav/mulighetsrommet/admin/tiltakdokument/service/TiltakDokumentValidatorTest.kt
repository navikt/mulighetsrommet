package no.nav.mulighetsrommet.admin.tiltakdokument.service

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import java.util.UUID

class TiltakDokumentValidatorTest : FunSpec({
    val validVeilederinfo = TiltakDokumentRequest.VeilederinfoRequest(
        navRegioner = setOf(NavEnhetNummer("0300")),
        navKontorer = setOf(NavEnhetNummer("0301")),
    )
    val validRequest = TiltakDokumentRequest(
        id = UUID.randomUUID(),
        navn = "Testtiltak",
        tiltakstypeId = UUID.randomUUID(),
        administratorer = setOf(NavIdent("Z999999")),
        veilederinformasjon = validVeilederinfo,
    )

    context("validate") {
        test("returnerer Right for gyldig request") {
            TiltakDokumentValidator.validate(validRequest).shouldBeRight()
        }

        test("navn blank gir valideringsfeil") {
            val errors = TiltakDokumentValidator.validate(validRequest.copy(navn = "  "))
                .shouldBeLeft()

            errors shouldContain FieldError.of("Navn er påkrevd", TiltakDokumentRequest::navn)
        }

        test("navn tomt gir valideringsfeil") {
            val errors = TiltakDokumentValidator.validate(validRequest.copy(navn = ""))
                .shouldBeLeft()

            errors shouldContain FieldError.of("Navn er påkrevd", TiltakDokumentRequest::navn)
        }

        test("navn over 500 tegn gir valideringsfeil") {
            val errors = TiltakDokumentValidator.validate(validRequest.copy(navn = "a".repeat(501)))
                .shouldBeLeft()

            errors shouldContain FieldError.of("Navn kan ikke være lengre enn 500 tegn", TiltakDokumentRequest::navn)
        }

        test("navn på nøyaktig 500 tegn er gyldig") {
            TiltakDokumentValidator.validate(validRequest.copy(navn = "a".repeat(500))).shouldBeRight()
        }

        test("ingen administratorer gir valideringsfeil") {
            val errors = TiltakDokumentValidator.validate(validRequest.copy(administratorer = emptySet()))
                .shouldBeLeft()

            errors shouldContain FieldError.of("Du må velge minst én administrator", TiltakDokumentRequest::administratorer)
        }

        test("samler opp flere valideringsfeil") {
            val errors = TiltakDokumentValidator.validate(
                validRequest.copy(navn = "", administratorer = emptySet()),
            ).shouldBeLeft()

            errors shouldContainAll listOf(
                FieldError.of("Navn er påkrevd", TiltakDokumentRequest::navn),
                FieldError.of("Du må velge minst én administrator", TiltakDokumentRequest::administratorer),
            )
        }
    }

    context("validateVeilederinfo") {
        test("ingen navRegioner gir valideringsfeil") {
            val errors = TiltakDokumentValidator.validate(
                validRequest.copy(veilederinformasjon = validVeilederinfo.copy(navRegioner = emptySet())),
            ).shouldBeLeft()

            errors shouldContain FieldError.of(
                "Du må velge minst én Nav-region fra avtalen",
                TiltakDokumentRequest::veilederinformasjon,
                TiltakDokumentRequest.VeilederinfoRequest::navRegioner,
            )
        }

        test("ingen navKontorer og ingen navAndreEnheter gir valideringsfeil") {
            val errors = TiltakDokumentValidator.validate(
                validRequest.copy(
                    veilederinformasjon = validVeilederinfo.copy(navKontorer = emptySet(), navAndreEnheter = emptySet()),
                ),
            ).shouldBeLeft()

            errors shouldContain FieldError.of(
                "Du må velge minst én Nav-enhet",
                TiltakDokumentRequest::veilederinformasjon,
                TiltakDokumentRequest.VeilederinfoRequest::navKontorer,
            )
        }

        test("kun navAndreEnheter (uten navKontorer) er gyldig") {
            TiltakDokumentValidator.validate(
                validRequest.copy(
                    veilederinformasjon = validVeilederinfo.copy(
                        navKontorer = emptySet(),
                        navAndreEnheter = setOf(NavEnhetNummer("1234")),
                    ),
                ),
            ).shouldBeRight()
        }

        test("kun navKontorer (uten navAndreEnheter) er gyldig") {
            TiltakDokumentValidator.validate(
                validRequest.copy(
                    veilederinformasjon = validVeilederinfo.copy(
                        navKontorer = setOf(NavEnhetNummer("0301")),
                        navAndreEnheter = emptySet(),
                    ),
                ),
            ).shouldBeRight()
        }

        test("både navKontorer og navAndreEnheter er gyldig") {
            TiltakDokumentValidator.validate(
                validRequest.copy(
                    veilederinformasjon = validVeilederinfo.copy(
                        navKontorer = setOf(NavEnhetNummer("0301")),
                        navAndreEnheter = setOf(NavEnhetNummer("1234")),
                    ),
                ),
            ).shouldBeRight()
        }

        test("mangler både navRegioner og navEnheter gir to feil") {
            val errors = TiltakDokumentValidator.validate(
                validRequest.copy(
                    veilederinformasjon = validVeilederinfo.copy(
                        navRegioner = emptySet(),
                        navKontorer = emptySet(),
                        navAndreEnheter = emptySet(),
                    ),
                ),
            ).shouldBeLeft()

            errors shouldHaveSize 2
            errors shouldContainAll listOf(
                FieldError.of(
                    "Du må velge minst én Nav-region fra avtalen",
                    TiltakDokumentRequest::veilederinformasjon,
                    TiltakDokumentRequest.VeilederinfoRequest::navRegioner,
                ),
                FieldError.of(
                    "Du må velge minst én Nav-enhet",
                    TiltakDokumentRequest::veilederinformasjon,
                    TiltakDokumentRequest.VeilederinfoRequest::navKontorer,
                ),
            )
        }
    }
})
