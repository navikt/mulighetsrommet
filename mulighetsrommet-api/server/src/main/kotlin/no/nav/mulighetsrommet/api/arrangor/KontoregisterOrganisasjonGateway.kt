package no.nav.mulighetsrommet.api.arrangor

import arrow.core.Either
import no.nav.mulighetsrommet.admin.arrangor.KontoregisterError
import no.nav.mulighetsrommet.admin.arrangor.KontoregisterGateway
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontonummerRegisterOrganisasjonError
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer

class KontoregisterOrganisasjonGateway(
    private val client: KontoregisterOrganisasjonClient,
) : KontoregisterGateway {
    override suspend fun hentKontonummer(orgnr: Organisasjonsnummer): Either<KontoregisterError, Kontonummer> {
        return client.getKontonummerForOrganisasjon(orgnr)
            .map { Kontonummer(it.kontonr) }
            .mapLeft {
                when (it) {
                    KontonummerRegisterOrganisasjonError.FantIkkeKontonummer -> KontoregisterError.IkkeFunnet

                    KontonummerRegisterOrganisasjonError.UgyldigInput,
                    KontonummerRegisterOrganisasjonError.Error,
                    -> KontoregisterError.Feil
                }
            }
    }
}
