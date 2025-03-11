package no.nav.mulighetsrommet.api.arrangorflate

import arrow.core.getOrElse
import arrow.core.toNonEmptySetOrNull
import io.ktor.http.*
import no.nav.amt.model.Melding
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.arrangorflate.model.*
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.tilsagn.model.ArrangorflateTilsagn
import no.nav.mulighetsrommet.api.utbetaling.HentAdressebeskyttetPersonBolkPdlQuery
import no.nav.mulighetsrommet.api.utbetaling.HentPersonBolkResponse
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerForslag
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakerDto
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningForhandsgodkjent
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingDto
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

class ArrangorFlateService(
    val pdl: HentAdressebeskyttetPersonBolkPdlQuery,
    val db: ApiDatabase,
) {
    fun getUtbetalinger(orgnr: Organisasjonsnummer): List<ArrFlateUtbetalingKompakt> = db.session {
        return queries.utbetaling.getByArrangorIds(orgnr).map { utbetaling ->
            val status = getArrFlateUtbetalingStatus(utbetaling)
            ArrFlateUtbetalingKompakt.fromUtbetalingDto(utbetaling, status)
        }
    }

    fun getUtbetaling(id: UUID): UtbetalingDto? = db.session {
        return queries.utbetaling.get(id)
    }

    fun getTilsagn(id: UUID): ArrangorflateTilsagn? = db.session {
        return queries.tilsagn.getArrangorflateTilsagn(id)
    }

    fun getTilsagnByOrgnr(orgnr: Organisasjonsnummer): List<ArrangorflateTilsagn> = db.session {
        return queries.tilsagn.getAllArrangorflateTilsagn(orgnr)
    }

    fun getRelevanteForslag(utbetaling: UtbetalingDto): List<RelevanteForslag> = db.session {
        return queries.deltakerForslag.getForslagByGjennomforing(utbetaling.gjennomforing.id)
            .map { (deltakerId, forslag) ->
                RelevanteForslag(
                    deltakerId = deltakerId,
                    antallRelevanteForslag = forslag.count { it.relevantForDeltakelse(utbetaling) },
                )
            }
    }

    suspend fun toArrFlateUtbetaling(utbetaling: UtbetalingDto): ArrFlateUtbetaling = db.session {
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
                        forstePeriodeStartDato = forstePeriode.start,
                        sistePeriodeSluttDato = sistePeriode.slutt.minusDays(1),
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
                    periodeStart = utbetaling.periode.start,
                    periodeSlutt = utbetaling.periode.getLastInclusiveDate(),
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
                periodeStart = utbetaling.periode.start,
                periodeSlutt = utbetaling.periode.getLastInclusiveDate(),
                beregning = Beregning.Fri(
                    belop = beregning.output.belop,
                    digest = beregning.getDigest(),
                ),
                betalingsinformasjon = utbetaling.betalingsinformasjon,
            )
        }
    }

    private fun QueryContext.getArrFlateUtbetalingStatus(utbetaling: UtbetalingDto): ArrFlateUtbetalingStatus {
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
}

fun DeltakerForslag.relevantForDeltakelse(
    utbetaling: UtbetalingDto,
): Boolean = when (utbetaling.beregning) {
    is UtbetalingBeregningForhandsgodkjent -> this.relevantForDeltakelse(utbetaling.beregning)
    is UtbetalingBeregningFri -> false
}

fun DeltakerForslag.relevantForDeltakelse(
    beregning: UtbetalingBeregningForhandsgodkjent,
): Boolean {
    val deltakelser = beregning.input.deltakelser
        .find { it.deltakelseId == this.deltakerId }
        ?: return false

    val periode = beregning.input.periode
    val sisteSluttDato = deltakelser.perioder.maxOf { it.slutt }
    val forsteStartDato = deltakelser.perioder.minOf { it.start }

    return when (this.endring) {
        is Melding.Forslag.Endring.AvsluttDeltakelse -> {
            val sluttDato = this.endring.sluttdato

            this.endring.harDeltatt == false || (sluttDato != null && sluttDato.isBefore(sisteSluttDato))
        }

        is Melding.Forslag.Endring.Deltakelsesmengde -> {
            this.endring.gyldigFra?.isBefore(sisteSluttDato) ?: true
        }

        is Melding.Forslag.Endring.ForlengDeltakelse -> {
            this.endring.sluttdato.isAfter(sisteSluttDato) && this.endring.sluttdato.isBefore(periode.slutt)
        }

        is Melding.Forslag.Endring.IkkeAktuell -> {
            true
        }

        is Melding.Forslag.Endring.Sluttarsak -> {
            false
        }

        is Melding.Forslag.Endring.Sluttdato -> {
            this.endring.sluttdato.isBefore(sisteSluttDato)
        }

        is Melding.Forslag.Endring.Startdato -> {
            this.endring.startdato.isAfter(forsteStartDato)
        }

        Melding.Forslag.Endring.FjernOppstartsdato -> true
    }
}
