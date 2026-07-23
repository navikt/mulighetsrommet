package no.nav.mulighetsrommet.admin.tiltakdokument.service

import arrow.core.Either
import no.nav.mulighetsrommet.api.domain.tiltakdokument.TiltakDokument
import no.nav.mulighetsrommet.api.domain.tiltakdokument.TiltakDokument.Kontaktperson
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.validation.validation

object TiltakDokumentValidator {
    fun validate(request: TiltakDokumentRequest): Either<List<FieldError>, TiltakDokument> = validation {
        validate(request.navn.isNotBlank()) {
            FieldError.of("Navn er påkrevd", TiltakDokumentRequest::navn)
        }
        validate(request.navn.length <= 500) {
            FieldError.of("Navn kan ikke være lengre enn 500 tegn", TiltakDokumentRequest::navn)
        }
        validate(request.administratorer.isNotEmpty()) {
            FieldError.of("Du må velge minst én administrator", TiltakDokumentRequest::administratorer)
        }

        val vi = request.veilederinformasjon
        validateVeilederinfo(vi).bind()

        val navEnheter = (vi.navRegioner + vi.navKontorer + vi.navAndreEnheter)
        TiltakDokument(
            id = request.id,
            navn = request.navn,
            sanityId = null,
            tiltaksnummer = null,
            tiltakstypeId = request.tiltakstypeId,
            stedForGjennomforing = request.stedForGjennomforing,
            arrangorId = request.arrangorId,
            faneinnhold = vi.faneinnhold,
            beskrivelse = vi.beskrivelse,
            publisert = false,
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

    fun validateVeilederinfo(vi: TiltakDokumentRequest.VeilederinfoRequest): Either<List<FieldError>, Unit> = validation(TiltakDokumentRequest::veilederinformasjon) {
        // TODO: Valider slettet navAnsatt i kontaktperson listen når NavAnsattService er flyttet ut av
        // TODO: server modulen
        validate(vi.navRegioner.isNotEmpty()) {
            FieldError.of(
                "Du må velge minst én Nav-region fra avtalen",
                TiltakDokumentRequest.VeilederinfoRequest::navRegioner,
            )
        }

        validate((vi.navKontorer + vi.navAndreEnheter).isNotEmpty()) {
            FieldError.of(
                "Du må velge minst én Nav-enhet",
                TiltakDokumentRequest.VeilederinfoRequest::navKontorer,
            )
        }
    }
}
