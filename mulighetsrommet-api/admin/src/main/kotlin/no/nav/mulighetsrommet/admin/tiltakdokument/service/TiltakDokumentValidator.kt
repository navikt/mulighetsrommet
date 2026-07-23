package no.nav.mulighetsrommet.admin.tiltakdokument.service

import arrow.core.Either
import no.nav.mulighetsrommet.api.domain.tiltakdokument.TiltakDokument
import no.nav.mulighetsrommet.api.domain.tiltakdokument.TiltakDokument.Kontaktperson
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.NavEnhetNummer
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

        validateVeilederinfo(
            navRegioner = request.navRegioner,
            navKontorer = request.navKontorer,
            navAndreEnheter = request.navAndreEnheter,
        ).bind()

        val navEnheter = (request.navRegioner + request.navKontorer + request.navAndreEnheter)
        TiltakDokument(
            id = request.id,
            navn = request.navn,
            sanityId = null,
            tiltaksnummer = null,
            tiltakstypeId = request.tiltakstypeId,
            stedForGjennomforing = request.stedForGjennomforing,
            arrangorId = request.arrangorId,
            faneinnhold = request.faneinnhold,
            beskrivelse = request.beskrivelse,
            publisert = false,
            administratorer = request.administratorer.toList(),
            navEnheter = navEnheter.toList(),
            kontaktpersoner = request.kontaktpersoner.map {
                Kontaktperson(
                    navIdent = it.navIdent,
                    beskrivelse = it.beskrivelse,
                )
            },
            arrangorKontaktpersoner = request.arrangorKontaktpersoner.toList(),
        )
    }

    fun validateVeilederinfo(
        navRegioner: Set<NavEnhetNummer>,
        navKontorer: Set<NavEnhetNummer>,
        navAndreEnheter: Set<NavEnhetNummer>,
    ): Either<List<FieldError>, Unit> = validation {
        // TODO: Valider slettet navAnsatt i kontaktperson listen når NavAnsattService er flyttet ut av
        // TODO: server modulen
        validate(navRegioner.isNotEmpty()) {
            FieldError.of(
                "Du må velge minst én Nav-region fra avtalen",
                TiltakDokumentRequest::navRegioner,
            )
        }

        validate((navKontorer + navAndreEnheter).isNotEmpty()) {
            FieldError.of(
                "Du må velge minst én Nav-enhet",
                TiltakDokumentRequest::navKontorer,
            )
        }
    }
}
