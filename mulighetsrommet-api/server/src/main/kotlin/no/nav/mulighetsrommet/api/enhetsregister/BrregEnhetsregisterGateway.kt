package no.nav.mulighetsrommet.api.enhetsregister

import arrow.core.Either
import no.nav.mulighetsrommet.admin.enhetsregister.EnhetsregisterError
import no.nav.mulighetsrommet.admin.enhetsregister.EnhetsregisterGateway
import no.nav.mulighetsrommet.admin.enhetsregister.Hovedenhet
import no.nav.mulighetsrommet.admin.enhetsregister.Underenhet
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregError
import no.nav.mulighetsrommet.brreg.BrregHovedenhet
import no.nav.mulighetsrommet.brreg.BrregHovedenhetDto
import no.nav.mulighetsrommet.brreg.BrregUnderenhet
import no.nav.mulighetsrommet.brreg.BrregUnderenhetDto
import no.nav.mulighetsrommet.brreg.SlettetBrregHovedenhetDto
import no.nav.mulighetsrommet.brreg.SlettetBrregUnderenhetDto
import no.nav.mulighetsrommet.model.Organisasjonsnummer

class BrregEnhetsregisterGateway(
    private val brregClient: BrregClient,
) : EnhetsregisterGateway {
    override suspend fun sokHovedenheter(sok: String): Either<EnhetsregisterError, List<Hovedenhet>> {
        return brregClient.searchHovedenhet(sok)
            .map { hovedenheter -> hovedenheter.map { it.toHovedenhet() } }
            .mapLeft { it.toEnhetsregisterError() }
    }

    override suspend fun sokUnderenheter(sok: String): Either<EnhetsregisterError, List<Underenhet>> {
        return brregClient.searchUnderenhet(sok)
            .map { underenheter -> underenheter.map { it.toUnderenhet() } }
            .mapLeft { it.toEnhetsregisterError() }
    }

    override suspend fun hentUnderenheterForHovedenhet(
        orgnr: Organisasjonsnummer,
    ): Either<EnhetsregisterError, List<Underenhet>> {
        return brregClient.getUnderenheterForHovedenhet(orgnr)
            .map { underenheter -> underenheter.map { it.toUnderenhet(overordnetEnhet = orgnr) } }
            .mapLeft { it.toEnhetsregisterError() }
    }
}

private fun BrregHovedenhet.toHovedenhet(): Hovedenhet = when (this) {
    is BrregHovedenhetDto -> Hovedenhet(
        organisasjonsnummer = organisasjonsnummer,
        navn = navn,
        organisasjonsform = organisasjonsform,
        overordnetEnhet = overordnetEnhet,
    )

    is SlettetBrregHovedenhetDto -> Hovedenhet(
        organisasjonsnummer = organisasjonsnummer,
        navn = navn,
        organisasjonsform = organisasjonsform,
        slettetDato = slettetDato,
    )
}

private fun BrregUnderenhet.toUnderenhet(overordnetEnhet: Organisasjonsnummer? = null): Underenhet = when (this) {
    is BrregUnderenhetDto -> Underenhet(
        organisasjonsnummer = organisasjonsnummer,
        navn = navn,
        organisasjonsform = organisasjonsform,
        overordnetEnhet = overordnetEnhet ?: this.overordnetEnhet,
    )

    is SlettetBrregUnderenhetDto -> Underenhet(
        organisasjonsnummer = organisasjonsnummer,
        navn = navn,
        organisasjonsform = organisasjonsform,
        overordnetEnhet = overordnetEnhet,
        slettetDato = slettetDato,
    )
}

private fun BrregError.toEnhetsregisterError(): EnhetsregisterError = when (this) {
    is BrregError.NotFound,
    is BrregError.FjernetAvJuridiskeArsaker,
    is BrregError.BadRequest,
    is BrregError.Error,
    -> EnhetsregisterError.Feil
}
