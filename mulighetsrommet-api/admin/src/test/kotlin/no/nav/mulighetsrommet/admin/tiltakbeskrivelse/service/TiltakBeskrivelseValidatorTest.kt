package no.nav.mulighetsrommet.admin.tiltakbeskrivelse.service

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import no.nav.mulighetsrommet.admin.tiltakbeskrivelse.TiltakBeskrivelseDto
import no.nav.mulighetsrommet.api.domain.testing.fixture.TiltakstypeFixtures
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID

class TiltakBeskrivelseValidatorTest : FunSpec({
    val validVeilederinfo = TiltakBeskrivelseRequest.VeilederinfoRequest(
        navRegioner = setOf(NavEnhetNummer("0300")),
        navKontorer = setOf(NavEnhetNummer("0301")),
    )
    val validTiltakstype = TiltakstypeFixtures.Amo
    val validRequest = TiltakBeskrivelseRequest(
        id = UUID.randomUUID(),
        navn = "Testtiltak",
        tiltakstypeId = UUID.randomUUID(),
        administratorer = setOf(NavIdent("Z999999")),
        veilederinformasjon = validVeilederinfo,
    )

    context("validate") {
        test("returnerer Right for gyldig request") {
            TiltakBeskrivelseValidator.validate(validRequest, validTiltakstype, previous = null).shouldBeRight()
        }

        test("navn blank gir valideringsfeil") {
            val errors = TiltakBeskrivelseValidator.validate(validRequest.copy(navn = "  "), validTiltakstype, previous = null)
                .shouldBeLeft()

            errors shouldContain FieldError.of("Navn er påkrevd", TiltakBeskrivelseRequest::navn)
        }

        test("navn tomt gir valideringsfeil") {
            val errors = TiltakBeskrivelseValidator.validate(validRequest.copy(navn = ""), validTiltakstype, previous = null)
                .shouldBeLeft()

            errors shouldContain FieldError.of("Navn er påkrevd", TiltakBeskrivelseRequest::navn)
        }

        test("navn over 500 tegn gir valideringsfeil") {
            val errors = TiltakBeskrivelseValidator.validate(validRequest.copy(navn = "a".repeat(501)), validTiltakstype, previous = null)
                .shouldBeLeft()

            errors shouldContain FieldError.of("Navn kan ikke være lengre enn 500 tegn", TiltakBeskrivelseRequest::navn)
        }

        test("navn på nøyaktig 500 tegn er gyldig") {
            TiltakBeskrivelseValidator.validate(validRequest.copy(navn = "a".repeat(500)), validTiltakstype, previous = null).shouldBeRight()
        }

        test("ingen administratorer gir valideringsfeil") {
            val errors = TiltakBeskrivelseValidator.validate(validRequest.copy(administratorer = emptySet()), validTiltakstype, previous = null)
                .shouldBeLeft()

            errors shouldContain FieldError.of("Du må velge minst én administrator", TiltakBeskrivelseRequest::administratorer)
        }

        test("samler opp flere valideringsfeil") {
            val errors = TiltakBeskrivelseValidator.validate(
                validRequest.copy(navn = "", administratorer = emptySet()),
                validTiltakstype,
                previous = null,
            ).shouldBeLeft()

            errors shouldContainAll listOf(
                FieldError.of("Navn er påkrevd", TiltakBeskrivelseRequest::navn),
                FieldError.of("Du må velge minst én administrator", TiltakBeskrivelseRequest::administratorer),
            )
        }
    }

    context("validateVeilederinfo") {
        test("ingen navRegioner gir valideringsfeil") {
            val errors = TiltakBeskrivelseValidator.validate(
                validRequest.copy(veilederinformasjon = validVeilederinfo.copy(navRegioner = emptySet())),
                validTiltakstype,
                previous = null,
            ).shouldBeLeft()

            errors shouldContain FieldError.of(
                "Du må velge minst én Nav-region fra avtalen",
                TiltakBeskrivelseRequest::veilederinformasjon,
                TiltakBeskrivelseRequest.VeilederinfoRequest::navRegioner,
            )
        }

        test("ingen navKontorer og ingen navAndreEnheter gir valideringsfeil") {
            val errors = TiltakBeskrivelseValidator.validate(
                validRequest.copy(
                    veilederinformasjon = validVeilederinfo.copy(navKontorer = emptySet(), navAndreEnheter = emptySet()),
                ),
                validTiltakstype,
                previous = null,
            ).shouldBeLeft()

            errors shouldContain FieldError.of(
                "Du må velge minst én Nav-enhet",
                TiltakBeskrivelseRequest::veilederinformasjon,
                TiltakBeskrivelseRequest.VeilederinfoRequest::navKontorer,
            )
        }

        test("enkel amo og enkel fag yrke kan ikke opprettes") {
            TiltakBeskrivelseValidator.validate(validRequest, TiltakstypeFixtures.EnkelAmo, previous = null).shouldBeLeft()
            TiltakBeskrivelseValidator.validate(validRequest, TiltakstypeFixtures.EnkelFagOgYrke, previous = null).shouldBeLeft()
        }

        test("enkel amo og enkel fag yrke kan redigeres når dokumentet finnes fra før") {
            val previous = tiltakBeskrivelseDto(validRequest.id, Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING)
            TiltakBeskrivelseValidator.validate(validRequest, TiltakstypeFixtures.EnkelAmo, previous).shouldBeRight()

            val previousFagYrke = tiltakBeskrivelseDto(validRequest.id, Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING)
            TiltakBeskrivelseValidator.validate(validRequest, TiltakstypeFixtures.EnkelFagOgYrke, previousFagYrke).shouldBeRight()
        }

        test("kun navAndreEnheter (uten navKontorer) er gyldig") {
            TiltakBeskrivelseValidator.validate(
                validRequest.copy(
                    veilederinformasjon = validVeilederinfo.copy(
                        navKontorer = emptySet(),
                        navAndreEnheter = setOf(NavEnhetNummer("1234")),
                    ),
                ),
                validTiltakstype,
                previous = null,
            ).shouldBeRight()
        }

        test("kun navKontorer (uten navAndreEnheter) er gyldig") {
            TiltakBeskrivelseValidator.validate(
                validRequest.copy(
                    veilederinformasjon = validVeilederinfo.copy(
                        navKontorer = setOf(NavEnhetNummer("0301")),
                        navAndreEnheter = emptySet(),
                    ),
                ),
                validTiltakstype,
                previous = null,
            ).shouldBeRight()
        }

        test("både navKontorer og navAndreEnheter er gyldig") {
            TiltakBeskrivelseValidator.validate(
                validRequest.copy(
                    veilederinformasjon = validVeilederinfo.copy(
                        navKontorer = setOf(NavEnhetNummer("0301")),
                        navAndreEnheter = setOf(NavEnhetNummer("1234")),
                    ),
                ),
                validTiltakstype,
                previous = null,
            ).shouldBeRight()
        }

        test("mangler både navRegioner og navEnheter gir to feil") {
            val errors = TiltakBeskrivelseValidator.validate(
                validRequest.copy(
                    veilederinformasjon = validVeilederinfo.copy(
                        navRegioner = emptySet(),
                        navKontorer = emptySet(),
                        navAndreEnheter = emptySet(),
                    ),
                ),
                validTiltakstype,
                previous = null,
            ).shouldBeLeft()

            errors shouldHaveSize 2
            errors shouldContainAll listOf(
                FieldError.of(
                    "Du må velge minst én Nav-region fra avtalen",
                    TiltakBeskrivelseRequest::veilederinformasjon,
                    TiltakBeskrivelseRequest.VeilederinfoRequest::navRegioner,
                ),
                FieldError.of(
                    "Du må velge minst én Nav-enhet",
                    TiltakBeskrivelseRequest::veilederinformasjon,
                    TiltakBeskrivelseRequest.VeilederinfoRequest::navKontorer,
                ),
            )
        }
    }
})

private fun tiltakBeskrivelseDto(id: UUID, tiltakskode: Tiltakskode) = TiltakBeskrivelseDto(
    id = id,
    navn = "Testtiltak",
    sanityId = null,
    tiltaksnummer = null,
    tiltakstype = TiltakBeskrivelseDto.Tiltakstype(
        id = UUID.randomUUID(),
        navn = "Enkeltplass",
        tiltakskode = tiltakskode,
    ),
    stedForGjennomforing = null,
    arrangor = null,
    administratorer = emptyList(),
    arrangorKontaktpersoner = emptyList(),
    veilederinfo = TiltakBeskrivelseDto.Veilederinfo(
        publisert = false,
        beskrivelse = null,
        faneinnhold = null,
        kontorstruktur = emptyList(),
        kontaktpersoner = emptyList(),
    ),
    publisert = false,
)
