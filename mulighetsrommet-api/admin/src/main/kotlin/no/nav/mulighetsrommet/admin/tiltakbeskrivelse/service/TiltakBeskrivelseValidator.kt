package no.nav.mulighetsrommet.admin.tiltakbeskrivelse.service

import arrow.core.Either
import no.nav.mulighetsrommet.admin.tiltakbeskrivelse.TiltakBeskrivelseDto
import no.nav.mulighetsrommet.api.domain.tiltak.Tiltakstype
import no.nav.mulighetsrommet.api.domain.tiltakbeskrivelse.TiltakBeskrivelse
import no.nav.mulighetsrommet.api.domain.tiltakbeskrivelse.TiltakBeskrivelse.Kontaktperson
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.validation.validation

object TiltakBeskrivelseValidator {
    fun validate(
        request: TiltakBeskrivelseRequest,
        tiltakstype: Tiltakstype,
        previous: TiltakBeskrivelseDto?,
    ): Either<List<FieldError>, TiltakBeskrivelse> = validation {
        validate(
            previous != null || tiltakstype.tiltakskode !in listOf(
                Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
                Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING,
            ),
        ) {
            FieldError.of("Tiltakstypen ${tiltakstype.navn} er utgått", TiltakBeskrivelseRequest::navn)
        }
        validate(request.navn.isNotBlank()) {
            FieldError.of("Navn er påkrevd", TiltakBeskrivelseRequest::navn)
        }
        validate(request.navn.length <= 500) {
            FieldError.of("Navn kan ikke være lengre enn 500 tegn", TiltakBeskrivelseRequest::navn)
        }
        validate(request.administratorer.isNotEmpty()) {
            FieldError.of("Du må velge minst én administrator", TiltakBeskrivelseRequest::administratorer)
        }

        val vi = request.veilederinformasjon
        validateVeilederinfo(vi).bind()

        val navEnheter = (vi.navRegioner + vi.navKontorer + vi.navAndreEnheter)
        TiltakBeskrivelse(
            id = request.id,
            navn = request.navn,
            sanityId = null,
            tiltaksnummer = request.tiltaksnummer,
            tiltakstypeId = request.tiltakstypeId,
            stedForGjennomforing = request.stedForGjennomforing,
            arrangorId = request.arrangorId,
            faneinnhold = vi.faneinnhold,
            beskrivelse = vi.beskrivelse,
            publisert = previous?.publisert ?: false,
            administratorer = request.administratorer.toList(),
            navEnheter = navEnheter.toList(),
            kontaktpersoner = vi.kontaktpersoner.map {
                Kontaktperson(
                    navIdent = it.navIdent,
                    beskrivelse = it.beskrivelse,
                )
            },
            arrangorKontaktpersoner = request.arrangorKontaktpersoner.toList(),
        )
    }

    fun validateVeilederinfo(vi: TiltakBeskrivelseRequest.VeilederinfoRequest): Either<List<FieldError>, Unit> = validation(TiltakBeskrivelseRequest::veilederinformasjon) {
        // TODO: Valider slettet navAnsatt i kontaktperson listen når NavAnsattService er flyttet ut av
        // TODO: server modulen
        validate(vi.navRegioner.isNotEmpty()) {
            FieldError.of(
                "Du må velge minst én Nav-region fra avtalen",
                TiltakBeskrivelseRequest.VeilederinfoRequest::navRegioner,
            )
        }

        validate((vi.navKontorer + vi.navAndreEnheter).isNotEmpty()) {
            FieldError.of(
                "Du må velge minst én Nav-enhet",
                TiltakBeskrivelseRequest.VeilederinfoRequest::navKontorer,
            )
        }
    }
}
