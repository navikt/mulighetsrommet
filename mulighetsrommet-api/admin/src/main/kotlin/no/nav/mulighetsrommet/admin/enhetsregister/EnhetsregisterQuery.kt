package no.nav.mulighetsrommet.admin.enhetsregister

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.admin.AdminDatabase
import no.nav.mulighetsrommet.admin.arrangor.ArrangorDto
import no.nav.mulighetsrommet.model.Organisasjonsnummer

/**
 * Enhetsregisteret er vår generaliserte modell av virksomheter, uavhengig av om de er hentet fra
 * Brreg (via [EnhetsregisterGateway]) eller er utenlandske/slettede virksomheter som i dag er
 * lagret direkte i `arrangor`-tabellen (siden Brreg som et 3.-parts register ikke har data om
 * utenlandske virksomheter). Denne klassen eier logikken for å kombinere de to kildene.
 */
class EnhetsregisterQuery(
    private val gateway: EnhetsregisterGateway,
    private val db: AdminDatabase,
) {
    suspend fun sokHovedenheter(sok: String): Either<EnhetsregisterError, List<Hovedenhet>> {
        if (sok.isBlank()) {
            return EnhetsregisterError.UgyldigSok().left()
        }

        return gateway.sokHovedenheter(sok).map { hovedenheter ->
            val utenlandske = db.session {
                queries.arrangor.getAll(sok = sok, utenlandsk = true).items.map { it.toHovedenhet() }
            }
            hovedenheter + utenlandske
        }
    }

    suspend fun sokUnderenheter(sok: String): Either<EnhetsregisterError, List<Underenhet>> {
        if (sok.isBlank()) {
            return EnhetsregisterError.UgyldigSok().left()
        }

        return gateway.sokUnderenheter(sok)
    }

    suspend fun hentUnderenheterForHovedenhet(orgnr: Organisasjonsnummer): Either<EnhetsregisterError, List<Underenhet>> {
        val arrangor = db.session { repository.arrangor.getByOrganisasjonsnummer(orgnr) }
        if (arrangor != null && arrangor.erUtenlandsk) {
            // Utenlandske virksomheter har ingen underenheter i brreg - de representerer seg selv
            return listOf(
                Underenhet(
                    organisasjonsnummer = arrangor.organisasjonsnummer,
                    navn = arrangor.navn,
                    overordnetEnhet = arrangor.organisasjonsnummer,
                ),
            ).right()
        }

        return gateway.hentUnderenheterForHovedenhet(orgnr).map { underenheter ->
            val slettede = db.session {
                queries.arrangor.getAll(overordnetEnhetOrgnr = orgnr, slettet = true).items.map {
                    it.toUnderenhet()
                }
            }
            // Kombinerer resultat med virksomheter som er slettet fra brreg for å støtte
            // avtaler/gjennomføringer som henger etter
            underenheter + slettede
        }
    }
}

private fun ArrangorDto.toHovedenhet(): Hovedenhet = Hovedenhet(
    organisasjonsnummer = organisasjonsnummer,
    navn = navn,
    organisasjonsform = organisasjonsform,
    overordnetEnhet = overordnetEnhet,
    slettetDato = slettetDato,
)

private fun ArrangorDto.toUnderenhet(): Underenhet = Underenhet(
    organisasjonsnummer = organisasjonsnummer,
    navn = navn,
    overordnetEnhet = overordnetEnhet,
    slettetDato = slettetDato,
)
