package no.nav.mulighetsrommet.api.arrangor

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.admin.arrangor.ArrangorDto
import no.nav.mulighetsrommet.admin.arrangor.DokumentKoblingForKontaktperson
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontonummerRegisterOrganisasjonError
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.domain.arrangor.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.domain.arrangor.Betalingsinformasjon
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.responses.StatusResponse
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregEnhet
import no.nav.mulighetsrommet.brreg.BrregHovedenhet
import no.nav.mulighetsrommet.brreg.BrregHovedenhetDto
import no.nav.mulighetsrommet.brreg.BrregUnderenhet
import no.nav.mulighetsrommet.brreg.BrregUnderenhetDto
import no.nav.mulighetsrommet.brreg.FjernetBrregEnhetDto
import no.nav.mulighetsrommet.brreg.SlettetBrregHovedenhetDto
import no.nav.mulighetsrommet.brreg.SlettetBrregUnderenhetDto
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import org.slf4j.LoggerFactory
import java.util.UUID
import no.nav.mulighetsrommet.brreg.BrregError as BrregClientError

interface ArrangorError {
    data class BrregError(val error: BrregClientError) : ArrangorError
    data class TomtSok(val message: String = "'sok' kan ikke være en tom streng") : ArrangorError
}

class ArrangorService(
    private val db: ApiDatabase,
    private val brregClient: BrregClient,
    private val kontoregisterOrganisasjonClient: KontoregisterOrganisasjonClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun brregSok(sok: String): Either<ArrangorError, List<BrregHovedenhet>> {
        if (sok.isBlank()) {
            return ArrangorError.TomtSok().left()
        }

        return brregClient.searchHovedenhet(sok)
            .map { hovedenheter ->
                val utenlandskeVirksomheter = db.session {
                    queries.arrangor.getAll(sok = sok, utenlandsk = true).items.map {
                        toBrregHovedenhet(it)
                    }
                }
                // Kombinerer resultat med utenlandske virksomheter siden de ikke finnes i brreg
                hovedenheter + utenlandskeVirksomheter
            }
            .mapLeft { ArrangorError.BrregError(it) }
    }

    suspend fun brregSokUnderenheter(sok: String): Either<ArrangorError, List<BrregUnderenhet>> {
        if (sok.isBlank()) {
            return ArrangorError.TomtSok().left()
        }

        return brregClient.searchUnderenhet(sok).mapLeft { ArrangorError.BrregError(it) }
    }

    suspend fun brregUnderenheter(orgnr: Organisasjonsnummer): Either<ArrangorError, List<BrregUnderenhet>> {
        val arrangor = db.session { queries.arrangor.get(orgnr) }
        if (arrangor != null && arrangor.erUtenlandsk) {
            return listOf(
                BrregUnderenhetDto(
                    organisasjonsnummer = arrangor.organisasjonsnummer,
                    organisasjonsform = "IKS", // Interkommunalt selskap (X i Arena)
                    navn = arrangor.navn,
                    overordnetEnhet = arrangor.organisasjonsnummer,
                ),
            ).right()
        }

        return brregClient.getUnderenheterForHovedenhet(orgnr)
            .map { underenheter ->
                val slettedeVirksomheter = db.session {
                    queries.arrangor.getAll(overordnetEnhetOrgnr = orgnr, slettet = true).items.map {
                        toBrregUnderenhet(it)
                    }
                }
                // Kombinerer resultat med virksomheter som er slettet fra brreg for å støtte avtaler/gjennomføringer som henger etter
                underenheter + slettedeVirksomheter
            }
            .mapLeft { ArrangorError.BrregError(it) }
    }

    fun getArrangor(orgnr: Organisasjonsnummer): ArrangorDto? = db.session {
        queries.arrangor.get(orgnr)
    }

    suspend fun getArrangorOrSyncFromBrreg(orgnr: Organisasjonsnummer): Either<ArrangorError, ArrangorDto> {
        return getArrangor(orgnr)?.right() ?: syncArrangorFromBrreg(orgnr)
    }

    suspend fun syncArrangorFromBrreg(orgnr: Organisasjonsnummer): Either<ArrangorError, ArrangorDto> {
        log.info("Synkroniserer enhet fra brreg orgnr=$orgnr")
        return brregClient.getBrregEnhet(orgnr)
            .mapLeft { ArrangorError.BrregError(it) }
            .flatMap { virksomhet ->
                when (virksomhet) {
                    is BrregUnderenhetDto -> getArrangorOrSyncFromBrreg(virksomhet.overordnetEnhet).map { virksomhet }
                    else -> virksomhet.right()
                }
            }
            .map { virksomhet ->
                syncToDatbase(virksomhet)
            }
            .onLeft { err ->
                if (err is ArrangorError.BrregError && err.error is BrregClientError.FjernetAvJuridiskeArsaker) {
                    syncToDatabase(err.error.enhet)
                }
            }
    }

    suspend fun getBetalingsinformasjon(id: UUID): Betalingsinformasjon? {
        val arrangor = db.session { queries.arrangor.getById(id) }

        if (arrangor.erUtenlandsk) {
            val arrangorUtenlandsk =
                requireNotNull(db.session { queries.arrangor.getUtenlandskArrangor(arrangor.id) }) {
                    "Fant ikke betalingsinformasjon for utenlandsk bedrift: " +
                        "orgnr=${arrangor.organisasjonsnummer.value}, navn=${arrangor.navn}. Ta kontakt " +
                        "med team Valp for å legge inn."
                }

            return Betalingsinformasjon.IBan(
                bic = arrangorUtenlandsk.bic,
                iban = arrangorUtenlandsk.iban,
                bankNavn = arrangorUtenlandsk.bankNavn,
                bankLandKode = arrangorUtenlandsk.landKode,
            )
        } else {
            return kontoregisterOrganisasjonClient.getKontonummerForOrganisasjon(arrangor.organisasjonsnummer)
                .fold(
                    {
                        when (it) {
                            KontonummerRegisterOrganisasjonError.FantIkkeKontonummer -> null

                            KontonummerRegisterOrganisasjonError.UgyldigInput,
                            KontonummerRegisterOrganisasjonError.Error,
                            -> throw IllegalStateException("Klarte ikke hente kontonummer for arrangør")
                        }
                    },
                    {
                        Betalingsinformasjon.BBan(Kontonummer(it.kontonr), null)
                    },
                )
        }
    }

    fun deleteArrangor(orgnr: Organisasjonsnummer): Unit = db.session {
        repository.arrangor.delete(orgnr)
    }

    fun upsertKontaktperson(kontaktperson: ArrangorKontaktperson): Unit = db.transaction {
        val arrangor = requireNotNull(queries.arrangor.get(kontaktperson.arrangorId)) {
            "Fant ikke arrangør med id=${kontaktperson.arrangorId}"
        }

        val kontaktpersoner = arrangor.kontaktpersoner
            .filterNot { it.id == kontaktperson.id } + kontaktperson

        repository.arrangor.save(arrangor.copy(kontaktpersoner = kontaktpersoner))
    }

    fun deleteKontaktperson(arrangorId: UUID, kontaktpersonId: UUID): StatusResponse<Unit> = db.transaction {
        val (gjennomforinger, avtaler) = queries.arrangor.koblingerTilKontaktperson(kontaktpersonId)

        if (gjennomforinger.isNotEmpty() || avtaler.isNotEmpty()) {
            return@transaction ValidationError(
                errors = listOf(
                    FieldError.of("Kontaktpersonen er i bruk og kan derfor ikke slettes"),
                ),
            ).left()
        }

        val arrangor = requireNotNull(repository.arrangor.get(arrangorId)) {
            "Fant ikke arrangør med id=$arrangorId"
        }

        val kontaktpersoner = arrangor.kontaktpersoner.filterNot { it.id == kontaktpersonId }

        repository.arrangor.save(arrangor.copy(kontaktpersoner = kontaktpersoner))

        Unit.right()
    }

    fun hentKontaktpersoner(arrangorId: UUID): List<ArrangorKontaktperson> = db.session {
        queries.arrangor.getKontaktpersoner(arrangorId)
    }

    fun hentKoblingerForKontaktperson(kontaktpersonId: UUID): KoblingerForKontaktperson = db.session {
        val (gjennomforinger, avtaler) = queries.arrangor.koblingerTilKontaktperson(kontaktpersonId)
        KoblingerForKontaktperson(
            gjennomforinger = gjennomforinger,
            avtaler = avtaler,
        )
    }

    private fun syncToDatbase(virksomhet: BrregEnhet): ArrangorDto = db.transaction {
        val eksisterende = queries.arrangor.getByOrganisasjonsnummer(virksomhet.organisasjonsnummer)
        val id = eksisterende?.id ?: UUID.randomUUID()
        val arrangor = virksomhet.toArrangor(id).copy(
            kontaktpersoner = eksisterende?.kontaktpersoner ?: emptyList(),
        )
        repository.arrangor.save(arrangor)
        queries.arrangor.getById(id)
    }

    private fun syncToDatabase(enhet: FjernetBrregEnhetDto): Unit = db.transaction {
        repository.arrangor.getByOrganisasjonsnummer(enhet.organisasjonsnummer)
            ?.copy(slettetDato = enhet.slettetDato)
            ?.also { arrangor ->
                log.info("Markerer arrangør som slettet: $arrangor")
                repository.arrangor.save(arrangor)
            }
    }
}

@Serializable
data class KoblingerForKontaktperson(
    val gjennomforinger: List<DokumentKoblingForKontaktperson>,
    val avtaler: List<DokumentKoblingForKontaktperson>,
)

private fun BrregEnhet.toArrangor(id: UUID): Arrangor {
    return when (this) {
        is BrregHovedenhetDto -> Arrangor(
            id = id,
            organisasjonsnummer = organisasjonsnummer,
            organisasjonsform = organisasjonsform,
            navn = navn,
            overordnetEnhet = null,
            slettetDato = null,
            erUtenlandsk = false,
        )

        is SlettetBrregHovedenhetDto -> Arrangor(
            id = id,
            organisasjonsnummer = organisasjonsnummer,
            organisasjonsform = organisasjonsform,
            navn = navn,
            slettetDato = slettetDato,
            overordnetEnhet = null,
            erUtenlandsk = false,
        )

        is BrregUnderenhetDto -> Arrangor(
            id = id,
            organisasjonsnummer = organisasjonsnummer,
            organisasjonsform = organisasjonsform,
            navn = navn,
            overordnetEnhet = overordnetEnhet,
            slettetDato = null,
            erUtenlandsk = false,
        )

        is SlettetBrregUnderenhetDto -> Arrangor(
            id = id,
            organisasjonsnummer = organisasjonsnummer,
            organisasjonsform = organisasjonsform,
            navn = navn,
            slettetDato = slettetDato,
            overordnetEnhet = null,
            erUtenlandsk = false,
        )
    }
}

private fun toBrregHovedenhet(arrangor: ArrangorDto): BrregHovedenhet {
    val slettetDato = arrangor.slettetDato
    return when {
        slettetDato != null -> SlettetBrregHovedenhetDto(
            organisasjonsnummer = arrangor.organisasjonsnummer,
            organisasjonsform = "IKS", // Interkommunalt selskap (X i Arena)
            navn = arrangor.navn,
            slettetDato = slettetDato,
        )

        else -> BrregHovedenhetDto(
            organisasjonsnummer = arrangor.organisasjonsnummer,
            organisasjonsform = "IKS", // Interkommunalt selskap (X i Arena)
            navn = arrangor.navn,
            overordnetEnhet = null,
            postadresse = null,
            forretningsadresse = null,
        )
    }
}

private fun toBrregUnderenhet(arrangor: ArrangorDto): BrregUnderenhet {
    val overordnetEnhet = requireNotNull(arrangor.overordnetEnhet)
    val slettetDato = arrangor.slettetDato
    return when {
        slettetDato != null -> SlettetBrregUnderenhetDto(
            organisasjonsnummer = arrangor.organisasjonsnummer,
            organisasjonsform = "IKS", // Interkommunalt selskap (X i Arena)
            navn = arrangor.navn,
            slettetDato = slettetDato,
        )

        else -> BrregUnderenhetDto(
            organisasjonsnummer = arrangor.organisasjonsnummer,
            organisasjonsform = "IKS", // Interkommunalt selskap (X i Arena)
            navn = arrangor.navn,
            overordnetEnhet = overordnetEnhet,
        )
    }
}
