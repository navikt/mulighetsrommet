package no.nav.mulighetsrommet.api.enhetsregister

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.admin.enhetsregister.EnhetsregisterError
import no.nav.mulighetsrommet.admin.enhetsregister.EnhetsregisterGateway
import no.nav.mulighetsrommet.admin.enhetsregister.Virksomhet
import no.nav.mulighetsrommet.admin.enhetsregister.VirksomhetOppslag
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregError
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.validation.BrregHovedenhet
import no.nav.mulighetsrommet.validation.BrregHovedenhetDto
import no.nav.mulighetsrommet.validation.BrregUnderenhet
import no.nav.mulighetsrommet.validation.BrregUnderenhetDto
import no.nav.mulighetsrommet.validation.SlettetBrregHovedenhetDto
import no.nav.mulighetsrommet.validation.SlettetBrregUnderenhetDto

class BrregEnhetsregisterGateway(
    private val brregClient: BrregClient,
) : EnhetsregisterGateway {
    override suspend fun sokHovedenheter(sok: String): Either<EnhetsregisterError, List<Virksomhet.Hovedenhet>> {
        return brregClient.searchHovedenhet(sok)
            .map { hovedenheter -> hovedenheter.map { it.toHovedenhet() } }
            .mapLeft { it.toEnhetsregisterError() }
    }

    override suspend fun sokUnderenheter(sok: String): Either<EnhetsregisterError, List<Virksomhet.Underenhet>> {
        return brregClient.searchUnderenhet(sok)
            .map { underenheter -> underenheter.map { it.toUnderenhet() } }
            .mapLeft { it.toEnhetsregisterError() }
    }

    override suspend fun hentUnderenheterForHovedenhet(
        orgnr: Organisasjonsnummer,
    ): Either<EnhetsregisterError, List<Virksomhet.Underenhet>> {
        return brregClient.getUnderenheterForHovedenhet(orgnr)
            .map { underenheter -> underenheter.map { it.toUnderenhet(overordnetEnhet = orgnr) } }
            .mapLeft { it.toEnhetsregisterError() }
    }

    override suspend fun hentVirksomhet(orgnr: Organisasjonsnummer): Either<EnhetsregisterError, VirksomhetOppslag> {
        return brregClient.getBrregEnhet(orgnr).fold(
            { error ->
                when (error) {
                    is BrregError.FjernetAvJuridiskeArsaker -> VirksomhetOppslag.FjernetAvJuridiskeArsaker(
                        organisasjonsnummer = error.enhet.organisasjonsnummer,
                        slettetDato = error.enhet.slettetDato,
                    ).right()

                    else -> error.toEnhetsregisterError().left()
                }
            },
            { enhet ->
                val virksomhet = when (enhet) {
                    is BrregHovedenhet -> enhet.toHovedenhet()
                    is BrregUnderenhet -> enhet.toUnderenhet()
                }
                VirksomhetOppslag.Funnet(virksomhet).right()
            },
        )
    }
}

private fun BrregHovedenhet.toHovedenhet(): Virksomhet.Hovedenhet = when (this) {
    is BrregHovedenhetDto -> Virksomhet.Hovedenhet(
        organisasjonsnummer = organisasjonsnummer,
        navn = navn,
        organisasjonsform = organisasjonsform,
        overordnetEnhet = overordnetEnhet,
    )

    is SlettetBrregHovedenhetDto -> Virksomhet.Hovedenhet(
        organisasjonsnummer = organisasjonsnummer,
        navn = navn,
        organisasjonsform = organisasjonsform,
        slettetDato = slettetDato,
    )
}

private fun BrregUnderenhet.toUnderenhet(overordnetEnhet: Organisasjonsnummer? = null): Virksomhet.Underenhet = when (this) {
    is BrregUnderenhetDto -> Virksomhet.Underenhet(
        organisasjonsnummer = organisasjonsnummer,
        navn = navn,
        organisasjonsform = organisasjonsform,
        overordnetEnhet = overordnetEnhet ?: this.overordnetEnhet,
    )

    is SlettetBrregUnderenhetDto -> Virksomhet.Underenhet(
        organisasjonsnummer = organisasjonsnummer,
        navn = navn,
        organisasjonsform = organisasjonsform,
        overordnetEnhet = overordnetEnhet,
        slettetDato = slettetDato,
    )
}

private fun BrregError.toEnhetsregisterError(): EnhetsregisterError = when (this) {
    is BrregError.NotFound -> EnhetsregisterError.IkkeFunnet

    is BrregError.FjernetAvJuridiskeArsaker,
    is BrregError.BadRequest,
    is BrregError.Error,
    -> EnhetsregisterError.Feil
}
