package no.nav.mulighetsrommet.api.arrangorflate

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.toNonEmptySetOrNull
import io.ktor.http.*
import no.nav.amt.model.Melding
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.arrangorflate.api.*
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontonummerRegisterOrganisasjonError
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatusAarsak
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.HentAdressebeskyttetPersonBolkPdlQuery
import no.nav.mulighetsrommet.api.utbetaling.HentPersonBolkResponse
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerForslag
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakerDto
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningForhandsgodkjent
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

private val TILSAGN_TYPE_RELEVANT_FOR_UTBETALING = listOf(
    TilsagnType.TILSAGN,
    TilsagnType.EKSTRATILSAGN,
)

private val TILSAGN_STATUS_RELEVANT_FOR_ARRANGOR = listOf(
    TilsagnStatus.GODKJENT,
    TilsagnStatus.TIL_ANNULLERING,
    TilsagnStatus.ANNULLERT,
)

class ArrangorFlateService(
    val pdl: HentAdressebeskyttetPersonBolkPdlQuery,
    val db: ApiDatabase,
    val kontoregisterOrganisasjonClient: KontoregisterOrganisasjonClient,
) {
    fun getUtbetalinger(orgnr: Organisasjonsnummer): List<ArrFlateUtbetalingKompaktDto> = db.session {
        return queries.utbetaling.getByArrangorIds(orgnr).map { utbetaling ->
            val status = getArrFlateUtbetalingStatus(utbetaling)
            ArrFlateUtbetalingKompaktDto.fromUtbetaling(utbetaling, status)
        }
    }

    fun getUtbetaling(id: UUID): Utbetaling? = db.session {
        return queries.utbetaling.get(id)
    }

    fun getTilsagn(id: UUID): ArrangorflateTilsagnDto? = db.session {
        queries.tilsagn.get(id)
            ?.takeIf { it.status in TILSAGN_STATUS_RELEVANT_FOR_ARRANGOR }
            ?.let { toArrangorflateTilsagn(it) }
    }

    fun getTilsagnByOrgnr(orgnr: Organisasjonsnummer): List<ArrangorflateTilsagnDto> = db.session {
        queries.tilsagn
            .getAll(
                arrangor = orgnr,
                statuser = TILSAGN_STATUS_RELEVANT_FOR_ARRANGOR,
            )
            .map { toArrangorflateTilsagn(it) }
    }

    fun getArrangorflateTilsagnTilUtbetaling(
        gjennomforingId: UUID,
        periode: Periode,
    ): List<ArrangorflateTilsagnDto> = db.session {
        queries.tilsagn
            .getAll(
                gjennomforingId = gjennomforingId,
                periodeIntersectsWith = periode,
                typer = TILSAGN_TYPE_RELEVANT_FOR_UTBETALING,
                statuser = TILSAGN_STATUS_RELEVANT_FOR_ARRANGOR,
            )
            .map { toArrangorflateTilsagn(it) }
    }

    fun getRelevanteForslag(utbetaling: Utbetaling): List<RelevanteForslag> = db.session {
        return queries.deltakerForslag.getForslagByGjennomforing(utbetaling.gjennomforing.id)
            .map { (deltakerId, forslag) ->
                RelevanteForslag(
                    deltakerId = deltakerId,
                    antallRelevanteForslag = forslag.count { it.relevantForDeltakelse(utbetaling) },
                )
            }
    }

    suspend fun toArrFlateUtbetaling(utbetaling: Utbetaling): ArrFlateUtbetaling = db.session {
        val status = getArrFlateUtbetalingStatus(utbetaling)
        return when (val beregning = utbetaling.beregning) {
            is UtbetalingBeregningForhandsgodkjent -> {
                val deltakere = queries.deltaker.getAll(gjennomforingId = utbetaling.gjennomforing.id)

                val deltakereById = deltakere.associateBy { it.id }
                val personerByNorskIdent: Map<NorskIdent, UtbetalingDeltakelse.Person> = getPersoner(deltakere)
                val perioderById = beregning.input.deltakelser.associateBy { it.deltakelseId }
                val manedsverkById = beregning.output.deltakelser.associateBy { it.deltakelseId }

                val deltakelser = perioderById.map { (id, deltakelse) ->
                    val deltaker = deltakereById.getValue(id)
                    val manedsverk = manedsverkById.getValue(id).manedsverk
                    val person = personerByNorskIdent[deltaker.norskIdent]

                    val forstePeriode = deltakelse.perioder.first()
                    val sistePeriode = deltakelse.perioder.last()

                    UtbetalingDeltakelse(
                        id = id,
                        startDato = deltaker.startDato,
                        sluttDato = deltaker.startDato,
                        forstePeriodeStartDato = forstePeriode.periode.start,
                        sistePeriodeSluttDato = sistePeriode.periode.getLastInclusiveDate(),
                        sistePeriodeDeltakelsesprosent = sistePeriode.deltakelsesprosent,
                        manedsverk = manedsverk,
                        person = person,
                        // TODO data om veileder hos arrangør
                        veileder = null,
                    )
                }

                val antallManedsverk = deltakelser
                    .map { BigDecimal(it.manedsverk) }
                    .sumOf { it }
                    .setScale(2, RoundingMode.HALF_UP)
                    .toDouble()

                ArrFlateUtbetaling(
                    id = utbetaling.id,
                    status = status,
                    fristForGodkjenning = utbetaling.fristForGodkjenning,
                    tiltakstype = utbetaling.tiltakstype,
                    gjennomforing = utbetaling.gjennomforing,
                    arrangor = utbetaling.arrangor,
                    periode = utbetaling.periode,
                    beregning = Beregning.Forhandsgodkjent(
                        antallManedsverk = antallManedsverk,
                        belop = beregning.output.belop,
                        digest = beregning.getDigest(),
                        deltakelser = deltakelser,
                    ),
                    betalingsinformasjon = utbetaling.betalingsinformasjon,
                )
            }

            is UtbetalingBeregningFri -> ArrFlateUtbetaling(
                id = utbetaling.id,
                status = status,
                fristForGodkjenning = utbetaling.fristForGodkjenning,
                tiltakstype = utbetaling.tiltakstype,
                gjennomforing = utbetaling.gjennomforing,
                arrangor = utbetaling.arrangor,
                periode = utbetaling.periode,
                beregning = Beregning.Fri(
                    belop = beregning.output.belop,
                    digest = beregning.getDigest(),
                ),
                betalingsinformasjon = utbetaling.betalingsinformasjon,
            )
        }
    }

    private fun QueryContext.getArrFlateUtbetalingStatus(utbetaling: Utbetaling): ArrFlateUtbetalingStatus {
        val delutbetalinger = queries.delutbetaling.getByUtbetalingId(utbetaling.id)
        val relevanteForslag = getRelevanteForslag(utbetaling)
        return ArrFlateUtbetalingStatus.fromUtbetaling(
            utbetaling,
            delutbetalinger,
            relevanteForslag,
        )
    }

    private suspend fun getPersoner(deltakere: List<DeltakerDto>): Map<NorskIdent, UtbetalingDeltakelse.Person> {
        val identer = deltakere
            .mapNotNull { deltaker -> deltaker.norskIdent?.value?.let { PdlIdent(it) } }
            .toNonEmptySetOrNull()
            ?: return mapOf()

        return pdl.hentPersonBolk(identer)
            .map {
                buildMap {
                    it.entries.forEach { (ident, person) ->
                        val utbetalingPerson = toUtbetalingPerson(person)
                        put(NorskIdent(ident.value), utbetalingPerson)
                    }
                }
            }
            .getOrElse {
                throw StatusException(
                    status = HttpStatusCode.InternalServerError,
                    detail = "Klarte ikke hente informasjon om deltakere i utbetalingen",
                )
            }
    }

    private fun toUtbetalingPerson(person: HentPersonBolkResponse.Person): UtbetalingDeltakelse.Person {
        val gradering = person.adressebeskyttelse.firstOrNull()?.gradering ?: PdlGradering.UGRADERT
        return when (gradering) {
            PdlGradering.UGRADERT -> {
                val navn = person.navn.first().let { navn ->
                    val fornavnOgMellomnavn = listOfNotNull(navn.fornavn, navn.mellomnavn).joinToString(" ")
                    listOf(navn.etternavn, fornavnOgMellomnavn).joinToString(", ")
                }
                val foedselsdato = person.foedselsdato.first()
                UtbetalingDeltakelse.Person(
                    navn = navn,
                    fodselsaar = foedselsdato.foedselsaar,
                    fodselsdato = foedselsdato.foedselsdato,
                )
            }

            else -> UtbetalingDeltakelse.Person(
                navn = "Adressebeskyttet",
                fodselsaar = null,
                fodselsdato = null,
            )
        }
    }

    suspend fun synkroniserKontonummer(utbetaling: Utbetaling): Either<KontonummerRegisterOrganisasjonError, String> {
        db.session {
            return kontoregisterOrganisasjonClient.getKontonummerForOrganisasjon(Organisasjonsnummer(utbetaling.arrangor.organisasjonsnummer.value))
                .map {
                    queries.utbetaling.setBetalingsinformasjon(
                        id = utbetaling.id,
                        kontonummer = Kontonummer(it.kontonr),
                        kid = utbetaling.betalingsinformasjon.kid,
                    )
                    it.kontonr
                }
        }
    }
}

fun DeltakerForslag.relevantForDeltakelse(
    utbetaling: Utbetaling,
): Boolean = when (utbetaling.beregning) {
    is UtbetalingBeregningForhandsgodkjent -> relevantForDeltakelse(utbetaling.beregning)
    is UtbetalingBeregningFri -> false
}

fun DeltakerForslag.relevantForDeltakelse(
    beregning: UtbetalingBeregningForhandsgodkjent,
): Boolean {
    val deltakelser = beregning.input.deltakelser
        .find { it.deltakelseId == this.deltakerId }
        ?: return false

    val periode = beregning.input.periode
    val sisteSluttDato = deltakelser.perioder.maxOf { it.periode.getLastInclusiveDate() }
    val forsteStartDato = deltakelser.perioder.minOf { it.periode.start }

    return when (this.endring) {
        is Melding.Forslag.Endring.AvsluttDeltakelse -> {
            val sluttDato = endring.sluttdato

            endring.harDeltatt == false || (sluttDato != null && sluttDato.isBefore(sisteSluttDato))
        }

        is Melding.Forslag.Endring.Deltakelsesmengde -> {
            endring.gyldigFra?.isBefore(sisteSluttDato) ?: true
        }

        is Melding.Forslag.Endring.ForlengDeltakelse -> {
            endring.sluttdato.isAfter(sisteSluttDato) && endring.sluttdato.isBefore(periode.slutt)
        }

        is Melding.Forslag.Endring.IkkeAktuell -> {
            true
        }

        is Melding.Forslag.Endring.Sluttarsak -> {
            false
        }

        is Melding.Forslag.Endring.Sluttdato -> {
            endring.sluttdato.isBefore(sisteSluttDato)
        }

        is Melding.Forslag.Endring.Startdato -> {
            endring.startdato.isAfter(forsteStartDato)
        }

        Melding.Forslag.Endring.FjernOppstartsdato -> true
    }
}

private fun QueryContext.toArrangorflateTilsagn(
    tilsagn: Tilsagn,
): ArrangorflateTilsagnDto {
    val annullering = queries.totrinnskontroll.get(tilsagn.id, Totrinnskontroll.Type.ANNULLER)
    return ArrangorflateTilsagnDto(
        id = tilsagn.id,
        gjennomforing = ArrangorflateTilsagnDto.Gjennomforing(
            navn = tilsagn.gjennomforing.navn,
        ),
        gjenstaendeBelop = tilsagn.belopGjenstaende,
        tiltakstype = ArrangorflateTilsagnDto.Tiltakstype(
            navn = tilsagn.tiltakstype.navn,
        ),
        type = tilsagn.type,
        periode = tilsagn.periode,
        beregning = tilsagn.beregning,
        arrangor = ArrangorflateTilsagnDto.Arrangor(
            id = tilsagn.arrangor.id,
            navn = tilsagn.arrangor.navn,
            organisasjonsnummer = tilsagn.arrangor.organisasjonsnummer,
        ),
        status = ArrangorflateTilsagnDto.StatusOgAarsaker(
            status = tilsagn.status,
            aarsaker = annullering?.aarsaker?.map { TilsagnStatusAarsak.valueOf(it) } ?: listOf(),
        ),
    )
}
