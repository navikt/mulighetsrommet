package no.nav.mulighetsrommet.admin.arrangor

import no.nav.mulighetsrommet.admin.AdminDatabase
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.domain.arrangor.Betalingsinformasjon
import java.util.UUID

data class HentBetalingsinformasjon(
    val arrangorId: UUID,
)

class BetalingsinformasjonQuery(
    private val db: AdminDatabase,
    private val kontoregister: KontoregisterGateway,
) {
    suspend fun execute(query: HentBetalingsinformasjon): Betalingsinformasjon? {
        val arrangor = db.session { repository.arrangor.get(query.arrangorId) }

        return when (arrangor) {
            is Arrangor.Utenlandsk -> requireNotNull(arrangor.betalingsinformasjon) {
                "Fant ikke betalingsinformasjon for utenlandsk bedrift: orgnr=${arrangor.organisasjonsnummer.value}, navn=${arrangor.navn}. Ta kontakt med team Valp for å legge inn."
            }

            is Arrangor.Norsk -> kontoregister.hentKontonummer(arrangor.organisasjonsnummer).fold(
                {
                    when (it) {
                        KontoregisterError.IkkeFunnet -> null
                        KontoregisterError.Feil -> throw IllegalStateException("Klarte ikke hente kontonummer for arrangør")
                    }
                },
                { kontonummer -> Betalingsinformasjon.BBan(kontonummer, null) },
            )
        }
    }
}
