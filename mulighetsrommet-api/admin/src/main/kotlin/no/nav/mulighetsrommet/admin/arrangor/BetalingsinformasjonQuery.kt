package no.nav.mulighetsrommet.admin.arrangor

import no.nav.mulighetsrommet.admin.AdminDatabase
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
        val arrangor = db.session {
            requireNotNull(repository.arrangor.get(query.arrangorId)) {
                "Fant ikke arrangør med id ${query.arrangorId}"
            }
        }

        if (arrangor.erUtenlandsk) {
            val utenlandsk = db.session {
                requireNotNull(repository.arrangor.getUtenlandskArrangor(arrangor.id)) {
                    "Fant ikke betalingsinformasjon for utenlandsk bedrift: orgnr=${arrangor.organisasjonsnummer.value}, navn=${arrangor.navn}. Ta kontakt med team Valp for å legge inn."
                }
            }

            return Betalingsinformasjon.IBan(
                bic = utenlandsk.bic,
                iban = utenlandsk.iban,
                bankNavn = utenlandsk.bankNavn,
                bankLandKode = utenlandsk.landKode,
            )
        }

        return kontoregister.hentKontonummer(arrangor.organisasjonsnummer).fold(
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
