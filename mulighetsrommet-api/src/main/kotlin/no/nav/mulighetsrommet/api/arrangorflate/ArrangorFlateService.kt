package no.nav.mulighetsrommet.api.arrangorflate

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.toNonEmptySetOrNull
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.model.Melding
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.arrangorflate.api.*
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontonummerRegisterOrganisasjonError
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.clients.pdl.tilPersonNavn
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnBeregningDto
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnDto
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatusAarsak
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.api.ArrangorUtbetalingLinje
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerForslag
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.api.utbetaling.pdl.HentAdressebeskyttetPersonBolkPdlQuery
import no.nav.mulighetsrommet.api.utbetaling.pdl.HentPersonBolkResponse
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

private val TILSAGN_TYPE_RELEVANT_FOR_UTBETALING = listOf(
    TilsagnType.TILSAGN,
    TilsagnType.EKSTRATILSAGN,
)

private val TILSAGN_STATUS_RELEVANT_FOR_ARRANGOR = listOf(
    TilsagnStatus.GODKJENT,
    TilsagnStatus.TIL_ANNULLERING,
    TilsagnStatus.ANNULLERT,
    TilsagnStatus.OPPGJORT,
    TilsagnStatus.TIL_OPPGJOR,
)

class ArrangorFlateService(
    val db: ApiDatabase,
    val pdl: HentAdressebeskyttetPersonBolkPdlQuery,
    val kontoregisterOrganisasjonClient: KontoregisterOrganisasjonClient,
) {
    fun getUtbetalinger(orgnr: Organisasjonsnummer): List<ArrFlateUtbetalingKompaktDto> = db.session {
        return queries.utbetaling.getByArrangorIds(orgnr).map { utbetaling ->
            val status = getArrFlateUtbetalingStatus(utbetaling)
            val godkjentBelop =
                if (status in listOf(
                        ArrFlateUtbetalingStatus.OVERFORT_TIL_UTBETALING,
                        ArrFlateUtbetalingStatus.UTBETALT,
                    )
                ) {
                    getGodkjentBelopForUtbetaling(utbetaling.id)
                } else {
                    null
                }
            ArrFlateUtbetalingKompaktDto.fromUtbetaling(utbetaling, status, godkjentBelop)
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
                statuser = listOf(TilsagnStatus.GODKJENT),
            )
            .map { toArrangorflateTilsagn(it) }
    }

    fun getRelevanteForslag(utbetaling: Utbetaling): List<RelevanteForslag> = db.session {
        return queries.deltakerForslag.getForslagByGjennomforing(utbetaling.gjennomforing.id)
            .map { (deltakerId, forslag) ->
                RelevanteForslag(
                    deltakerId = deltakerId,
                    antallRelevanteForslag = forslag.count { isForslagRelevantForUtbetaling(it, utbetaling) },
                )
            }
    }

    private fun getGodkjentBelopForUtbetaling(utbetalingId: UUID): Int = db.session {
        return queries.delutbetaling.getByUtbetalingId(utbetalingId).sumOf { it.belop }
    }

    suspend fun toArrFlateUtbetaling(utbetaling: Utbetaling): ArrFlateUtbetaling = db.session {
        val status = getArrFlateUtbetalingStatus(utbetaling)
        val erTolvUkerEtterInnsending = utbetaling.godkjentAvArrangorTidspunkt?.let { it.plusWeeks(12) <= LocalDateTime.now() } ?: false
        val deltakere = when (utbetaling.beregning) {
            is UtbetalingBeregningFri -> emptyList()

            is UtbetalingBeregningPrisPerManedsverkMedDeltakelsesmengder,
            is UtbetalingBeregningPrisPerManedsverk,
            is UtbetalingBeregningPrisPerUkesverk,
            -> {
                if (erTolvUkerEtterInnsending) {
                    emptyList()
                } else {
                    queries.deltaker.getAll(gjennomforingId = utbetaling.gjennomforing.id)
                }
            }
        }
        val personerByNorskIdent = if (deltakere.isNotEmpty()) getPersoner(deltakere) else emptyMap()

        val linjer = queries.delutbetaling.getByUtbetalingId(utbetaling.id).map { delutbetaling ->
            val tilsagn = checkNotNull(queries.tilsagn.get(delutbetaling.tilsagnId)).let {
                TilsagnDto.fromTilsagn(it)
            }

            ArrangorUtbetalingLinje(
                id = delutbetaling.id,
                belop = delutbetaling.belop,
                status = delutbetaling.status,
                statusSistOppdatert = delutbetaling.fakturaStatusSistOppdatert,
                tilsagn = ArrangorUtbetalingLinje.Tilsagn(
                    id = tilsagn.id,
                    bestillingsnummer = tilsagn.bestillingsnummer,
                ),
            )
        }

        return mapUtbetalingToArrFlateUtbetaling(
            utbetaling = utbetaling,
            status = status,
            deltakere = deltakere,
            personerByNorskIdent = personerByNorskIdent,
            linjer = linjer,
            erTolvUkerEtterInnsending = erTolvUkerEtterInnsending,
        )
    }

    private fun QueryContext.getArrFlateUtbetalingStatus(utbetaling: Utbetaling): ArrFlateUtbetalingStatus {
        val delutbetalinger = queries.delutbetaling.getByUtbetalingId(utbetaling.id)
        val relevanteForslag = getRelevanteForslag(utbetaling)
        return ArrFlateUtbetalingStatus.fromUtbetaling(
            utbetaling.status,
            delutbetalinger,
            relevanteForslag,
        )
    }

    private suspend fun getPersoner(deltakere: List<Deltaker>): Map<NorskIdent, UtbetalingDeltakelsePerson> {
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

    private fun toUtbetalingPerson(person: HentPersonBolkResponse.Person): UtbetalingDeltakelsePerson {
        val gradering = person.adressebeskyttelse.firstOrNull()?.gradering ?: PdlGradering.UGRADERT
        return when (gradering) {
            PdlGradering.UGRADERT -> {
                val navn = if (person.navn.isNotEmpty()) tilPersonNavn(person.navn) else "Ukjent"
                val foedselsdato = if (person.foedselsdato.isNotEmpty()) person.foedselsdato.first() else null
                UtbetalingDeltakelsePerson(
                    navn = navn,
                    fodselsaar = foedselsdato?.foedselsaar,
                    fodselsdato = foedselsdato?.foedselsdato,
                )
            }

            else -> UtbetalingDeltakelsePerson(
                navn = "Adressebeskyttet",
                fodselsaar = null,
                fodselsdato = null,
            )
        }
    }

    fun getGjennomforinger(orgnr: Organisasjonsnummer): List<ArrangorflateGjennomforing> = db.session {
        queries.gjennomforing
            .getAll(
                arrangorOrgnr = listOf(orgnr),
            )
            .items.map {
                ArrangorflateGjennomforing(
                    id = it.id,
                    navn = it.navn,
                    startDato = it.startDato,
                    sluttDato = it.sluttDato,
                )
            }
    }

    fun getGjennomforingerByPrismodeller(orgnr: Organisasjonsnummer, prismodeller: List<Prismodell>): List<ArrangorflateGjennomforing> = db.session {
        val parameters = mapOf(
            "arrangor_orgnr" to orgnr.value,
            "prismodeller" to session.createArrayOfValue(prismodeller) { it.name },
        )

        @Language("PostgreSQL")
        val query = """
            select g.id, g.navn, g.start_dato, g.slutt_dato
            from gjennomforing g
                     join arrangor arr on arr.id = g.arrangor_id
                     inner join avtale a on g.avtale_id = a.id
            where a.prismodell = ANY (:prismodeller::prismodell[])
              and arr.organisasjonsnummer = :arrangor_orgnr
              order by g.slutt_dato desc, g.navn
        """.trimIndent()

        val result = session.list(queryOf(query, parameters)) {
            it.toArrangorflateGjennomforing()
        }
        return result
    }

    suspend fun getKontonummer(orgnr: Organisasjonsnummer): Either<KontonummerRegisterOrganisasjonError, String> {
        return kontoregisterOrganisasjonClient
            .getKontonummerForOrganisasjon(orgnr)
            .map { it.kontonr }
    }

    suspend fun synkroniserKontonummer(utbetaling: Utbetaling): Either<KontonummerRegisterOrganisasjonError, String> = db.session {
        getKontonummer(utbetaling.arrangor.organisasjonsnummer).onRight {
            queries.utbetaling.setKontonummer(
                id = utbetaling.id,
                kontonummer = Kontonummer(it),
            )
        }
    }
}

fun isForslagRelevantForUtbetaling(
    forslag: DeltakerForslag,
    utbetaling: Utbetaling,
): Boolean {
    val periode = when (utbetaling.beregning) {
        is UtbetalingBeregningFri -> return false

        is UtbetalingBeregningPrisPerManedsverkMedDeltakelsesmengder -> {
            val deltaker = utbetaling.beregning.input.deltakelser
                .find { it.deltakelseId == forslag.deltakerId }
                ?: return false
            Periode.fromRange(deltaker.perioder.map { it.periode })
        }

        is UtbetalingBeregningPrisPerManedsverk -> {
            utbetaling.beregning.input.deltakelser
                .find { it.deltakelseId == forslag.deltakerId }
                ?.periode
                ?: return false
        }

        is UtbetalingBeregningPrisPerUkesverk -> {
            utbetaling.beregning.input.deltakelser
                .find { it.deltakelseId == forslag.deltakerId }
                ?.periode
                ?: return false
        }
    }
    return isForslagRelevantForPeriode(forslag, utbetaling.periode, periode)
}

fun isForslagRelevantForPeriode(
    forslag: DeltakerForslag,
    utbetalingPeriode: Periode,
    deltakelsePeriode: Periode,
): Boolean {
    val deltakerPeriodeSluttDato = deltakelsePeriode.getLastInclusiveDate()

    return when (forslag.endring) {
        is Melding.Forslag.Endring.AvsluttDeltakelse -> {
            val sluttDato = forslag.endring.sluttdato

            forslag.endring.harDeltatt == false || (sluttDato != null && sluttDato.isBefore(deltakerPeriodeSluttDato))
        }

        is Melding.Forslag.Endring.Deltakelsesmengde -> {
            forslag.endring.gyldigFra?.isBefore(deltakerPeriodeSluttDato) ?: true
        }

        is Melding.Forslag.Endring.ForlengDeltakelse -> {
            forslag.endring.sluttdato.isAfter(deltakerPeriodeSluttDato) &&
                forslag.endring.sluttdato.isBefore(utbetalingPeriode.slutt)
        }

        is Melding.Forslag.Endring.IkkeAktuell -> {
            true
        }

        is Melding.Forslag.Endring.Sluttarsak -> {
            false
        }

        is Melding.Forslag.Endring.Sluttdato -> {
            forslag.endring.sluttdato.isBefore(deltakerPeriodeSluttDato)
        }

        is Melding.Forslag.Endring.Startdato -> {
            forslag.endring.startdato.isAfter(deltakelsePeriode.start)
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
            id = tilsagn.gjennomforing.id,
            navn = tilsagn.gjennomforing.navn,
        ),
        bruktBelop = tilsagn.belopBrukt,
        gjenstaendeBelop = tilsagn.gjenstaendeBelop(),
        tiltakstype = ArrangorflateTilsagnDto.Tiltakstype(
            navn = tilsagn.tiltakstype.navn,
        ),
        type = tilsagn.type,
        periode = tilsagn.periode,
        beregning = TilsagnBeregningDto.from(tilsagn.beregning),
        arrangor = ArrangorflateTilsagnDto.Arrangor(
            id = tilsagn.arrangor.id,
            navn = tilsagn.arrangor.navn,
            organisasjonsnummer = tilsagn.arrangor.organisasjonsnummer,
        ),
        status = ArrangorflateTilsagnDto.StatusOgAarsaker(
            status = tilsagn.status,
            aarsaker = annullering?.aarsaker?.map { TilsagnStatusAarsak.valueOf(it) } ?: listOf(),
        ),
        bestillingsnummer = tilsagn.bestilling.bestillingsnummer,
    )
}

@Serializable
data class ArrangorflateGjennomforing(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
)

fun Row.toArrangorflateGjennomforing(): ArrangorflateGjennomforing = ArrangorflateGjennomforing(
    id = uuid("id"),
    navn = string("navn"),
    startDato = localDate("start_dato"),
    sluttDato = localDateOrNull("slutt_dato"),
)
