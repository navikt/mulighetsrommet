package no.nav.mulighetsrommet.admin.enhetsregister

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.admin.AdminDatabase
import no.nav.mulighetsrommet.admin.arrangor.ArrangorDto
import no.nav.mulighetsrommet.admin.arrangor.ArrangorType
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.model.Organisasjonsnummer

/**
 * Enhetsregisteret er vår generaliserte modell av virksomheter, uavhengig av om de er hentet fra
 * Brreg (via [EnhetsregisterGateway]) eller er utenlandske/slettede/manuelt registrerte
 * virksomheter som i dag er lagret direkte i `arrangor`-tabellen.
 * Denne klassen eier logikken for å kombinere de to kildene.
 */
class EnhetsregisterQuery(
    private val gateway: EnhetsregisterGateway,
    private val db: AdminDatabase,
) {
    /**
     * Henter hovedenheter fra enhetsregisteret basert på [sok], som kan være formatert som enten
     * selskapsnavne eller organisasjonsnummer.
     *
     * Inkluderer også treff på arrangører fra eget register, så fremt disse ikke allerede blir
     * returnert fra Enhetsregisteret.
     */
    suspend fun sokHovedenheter(sok: String): Either<EnhetsregisterError, List<Virksomhet.Hovedenhet>> {
        if (sok.isBlank()) {
            return EnhetsregisterError.UgyldigSok().left()
        }

        return gateway.sokHovedenheter(sok).map { hovedenheter ->
            val hovedenheterFraEgetRegister = db.session {
                queries.arrangor.getAll(
                    sok = sok,
                    typer = setOf(ArrangorType.NORSK_HOVEDENHET, ArrangorType.UTENLANDSK),
                    slettet = false,
                )
            }

            val orgnrEnhetsregisteret = hovedenheter.map { it.organisasjonsnummer }.toSet()
            hovedenheter + hovedenheterFraEgetRegister.items
                .filter { it.organisasjonsnummer !in orgnrEnhetsregisteret }
                .map { it.toHovedenhet() }
        }
    }

    /**
     * Henter hovedenheter fra enhetsregisteret basert på [sok], som kan være formatert som enten
     * selskapsnavne eller organisasjonsnummer.
     *
     * Inkluderer også treff på arrangører fra eget register, så fremt disse ikke allerede blir
     * returnert fra Enhetsregisteret.
     */
    suspend fun sokUnderenheter(sok: String): Either<EnhetsregisterError, List<Virksomhet.Underenhet>> {
        if (sok.isBlank()) {
            return EnhetsregisterError.UgyldigSok().left()
        }

        return gateway.sokUnderenheter(sok).map { underenheter ->
            val underenheterFraEgetRegister = db.session {
                queries.arrangor.getAll(
                    sok = sok,
                    typer = setOf(ArrangorType.NORSK_UNDERENHET, ArrangorType.UTENLANDSK),
                    slettet = false,
                )
            }

            val orgnrEnhetsregisteret = underenheter.map { it.organisasjonsnummer }.toSet()
            underenheter + underenheterFraEgetRegister.items
                .filter { it.organisasjonsnummer !in orgnrEnhetsregisteret }
                .map { it.toUnderenhet() }
        }
    }

    /**
     * Henter alle underenheter for gitt [orgnr], inkludert underenheter fra eget register.
     *
     * Om [orgnr] er en utenlandsk virksomhet blir denne returnert direkte for å støtte opprettelse
     * av avtaler/gjennomføringer.
     *
     * Slettede (altså enheter slettet fra enhetsregisteret) blir også inkludert slik at disse kan
     * vises i eksisterende avtaler/gjennomføringer (selv om det ikke er lov å opprette nye
     * avtaler/gjennomføringer knyttet til disse arrangørene).
     */
    suspend fun hentUnderenheterForHovedenhet(orgnr: Organisasjonsnummer): Either<EnhetsregisterError, List<Virksomhet.Underenhet>> {
        val arrangor = db.session { repository.arrangor.getByOrganisasjonsnummer(orgnr) }
        if (arrangor != null && arrangor is Arrangor.Utenlandsk) {
            // Utenlandske virksomheter har ingen underenheter i brreg - de representerer seg selv
            return listOf(
                Virksomhet.Underenhet(
                    organisasjonsnummer = arrangor.organisasjonsnummer,
                    navn = arrangor.navn,
                    overordnetEnhet = null,
                    organisasjonsform = null,
                ),
            ).right()
        }

        return gateway.hentUnderenheterForHovedenhet(orgnr).map { underenheter ->
            val underenheterFraEgetRegister = db.session {
                queries.arrangor.getAll(overordnetEnhetOrgnr = orgnr)
            }

            val orgnrEnhetsregisteret = underenheter.map { it.organisasjonsnummer }.toSet()
            underenheter + underenheterFraEgetRegister.items
                .filter { it.organisasjonsnummer !in orgnrEnhetsregisteret }
                .map { it.toUnderenhet() }
        }
    }
}

private fun ArrangorDto.toHovedenhet(): Virksomhet.Hovedenhet = Virksomhet.Hovedenhet(
    organisasjonsnummer = organisasjonsnummer,
    navn = navn,
    organisasjonsform = null,
    overordnetEnhet = null,
    slettetDato = slettetDato,
)

private fun ArrangorDto.toUnderenhet(): Virksomhet.Underenhet = Virksomhet.Underenhet(
    organisasjonsnummer = organisasjonsnummer,
    navn = navn,
    overordnetEnhet = null,
    organisasjonsform = null,
    slettetDato = slettetDato,
)
