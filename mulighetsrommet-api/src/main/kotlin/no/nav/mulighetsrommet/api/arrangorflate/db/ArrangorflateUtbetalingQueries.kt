package no.nav.mulighetsrommet.api.arrangorflate.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.arrangor.model.Betalingsinformasjon
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateArrangorDto
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateFilterDirection
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateGjennomforingDto
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateTiltakstypeDto
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateUtbetalingFilter
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateUtbetaling
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateUtbetalingKompakt
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateUtbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingType
import no.nav.mulighetsrommet.api.utbetaling.api.toDto
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingQueries
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningType
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.database.datatypes.periode
import no.nav.mulighetsrommet.database.utils.DatabaseUtils.toFTSPrefixQuery
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.database.utils.mapPaginated
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.withValuta
import no.nav.tiltak.okonomi.Tilskuddstype
import org.intellij.lang.annotations.Language
import java.util.UUID
import kotlin.IllegalStateException

class ArrangorflateUtbetalingQueries(private val session: Session) {
    fun getFilteredKompakt(
        filter: ArrangorflateUtbetalingFilter,
    ): PaginatedResult<ArrangorflateUtbetalingKompakt> {
        val direction = when (filter.direction) {
            ArrangorflateFilterDirection.ASC -> "asc"
            ArrangorflateFilterDirection.DESC -> "desc"
        }

        val order = when (filter.orderBy) {
            ArrangorflateUtbetalingFilter.OrderBy.TILTAK -> "tiltakstype_navn $direction, gjennomforing_navn $direction"
            ArrangorflateUtbetalingFilter.OrderBy.ARRANGOR -> "arrangor_navn $direction, arrangor_organisasjonsnummer $direction"
            ArrangorflateUtbetalingFilter.OrderBy.PERIODE -> "periode $direction"
            ArrangorflateUtbetalingFilter.OrderBy.BELOP -> "belop_beregnet $direction"
            ArrangorflateUtbetalingFilter.OrderBy.STATUS -> "status $direction"
        }

        @Language("PostgreSQL")
        val query = """
            select *, count(*) over() as total_count
            from view_arrangorflate_utbetaling_kompakt
            where (:sok::text is null
                or gjennomforing_fts @@ to_tsquery('norwegian', :fts)
                or arrangor_organisasjonsnummer ilike :sok
                or belop_beregnet::text ilike :sok
                or to_char(lower(periode), 'DD.MM.YYYY') ilike :sok
                or to_char((upper(periode) - interval '1 day')::date, 'DD.MM.YYYY') ilike :sok
            )
            and arrangor_organisasjonsnummer = any (:orgnr_list::text[])
            and status = any (:status_list::text[])
            order by $order
            limit :limit
            offset :offset
        """.trimIndent()
        val params = mapOf(
            "fts" to filter.sok?.toFTSPrefixQuery(),
            "sok" to filter.sok?.let { "%$it%" },
            "orgnr_list" to session.createArrayOfValue(filter.arrangorer) { it.value },
            "status_list" to session.createTextArray(filter.type.utbetalingStatuser()),
        )
        return queryOf(query, params + filter.pagination.parameters)
            .mapPaginated { it.toArrangorflateUtbetalingKompakt() }
            .runWithSession(session)
    }

    fun getOrError(id: UUID): ArrangorflateUtbetaling {
        return checkNotNull(get(id)) { "Utbetaling med id $id finnes ikke" }
    }

    fun get(utbetalingId: UUID): ArrangorflateUtbetaling? {
        @Language("PostgreSQL")
        val utbetalingQuery = """
            select *
            from view_arrangorflate_utbetaling
            where id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(utbetalingQuery, utbetalingId)) { it.toArrangorflateUtbetaling() }
    }

    private fun Row.toArrangorflateUtbetaling(): ArrangorflateUtbetaling {
        val id = uuid("id")
        val valuta = string("valuta").let { Valuta.valueOf(it) }
        val beregning = context(session) {
            UtbetalingQueries.getBeregning(
                id,
                valuta,
                UtbetalingBeregningType.valueOf(string("beregning_type")),
            )
        }

        val arrangorId = uuid("arrangor_id")
        return ArrangorflateUtbetaling(
            id = id,
            gjennomforing = ArrangorflateUtbetaling.Gjennomforing(
                id = uuid("gjennomforing_id"),
                lopenummer = Tiltaksnummer(string("gjennomforing_lopenummer")),
                navn = string("gjennomforing_navn"),
                startDato = localDate("gjennomforing_start_dato"),
                sluttDato = localDateOrNull("gjennomforing_slutt_dato"),
            ),
            arrangor = ArrangorflateUtbetaling.Arrangor(
                id = arrangorId,
                organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
                navn = string("arrangor_navn"),
            ),
            tiltakstype = ArrangorflateUtbetaling.Tiltakstype(
                navn = string("tiltakstype_navn"),
                tiltakskode = Tiltakskode.valueOf(string("tiltakskode")),
            ),
            korreksjon = uuidOrNull("korreksjon_gjelder_utbetaling_id")?.let { gjelderUtbetalingId ->
                ArrangorflateUtbetaling.Korreksjon(
                    gjelderUtbetalingId = gjelderUtbetalingId,
                    begrunnelse = string("korreksjon_begrunnelse"),
                )
            },
            innsending = localDateTimeOrNull("innsendt_av_arrangor_tidspunkt")?.let { tidspunkt ->
                ArrangorflateUtbetaling.Innsending(tidspunkt)
            },
            status = UtbetalingStatusType.valueOf(string("status")),
            valuta = valuta,
            beregning = beregning,
            betalingsinformasjon = toBankKonto(arrangorId),
            periode = periode("periode"),
            createdAt = localDateTime("created_at"),
            updatedAt = localDateTime("updated_at"),
            tilskuddstype = Tilskuddstype.valueOf(string("tilskuddstype")),
            utbetalesTidligstTidspunkt = instantOrNull("utbetales_tidligst_tidspunkt"),
            avbruttTidspunkt = instantOrNull("avbrutt_tidspunkt"),
            blokkeringer = array<String>("blokkeringer").map { Utbetaling.Blokkering.valueOf(it) }.toSet(),
        )
    }

    private fun Row.toBankKonto(arrangorId: UUID): Betalingsinformasjon.BBan? = when (val iban = stringOrNull("iban")) {
        null -> stringOrNull("kontonummer")?.let {
            Betalingsinformasjon.BBan(
                kontonummer = Kontonummer(it),
                kid = stringOrNull("kid")?.let { Kid.parseOrThrow(it) },
            )
        }

        else -> throw IllegalStateException("IBan funnet for norsk arrangor med id: $arrangorId")
    }

    private fun Row.toArrangorflateUtbetalingKompakt(): ArrangorflateUtbetalingKompakt {
        val valuta = string("valuta").let { Valuta.valueOf(it) }
        val tilskuddstype = Tilskuddstype.valueOf(string("tilskuddstype"))
        val blokkeringer = array<String>("blokkeringer").map { Utbetaling.Blokkering.valueOf(it) }.toSet()
        val status = UtbetalingStatusType.valueOf(string("status"))
            .let { ArrangorflateUtbetalingStatus.fromUtbetaling(it, blokkeringer) }
        val godkjentBelop = when (status) {
            ArrangorflateUtbetalingStatus.OVERFORT_TIL_UTBETALING,
            ArrangorflateUtbetalingStatus.DELVIS_UTBETALT,
            ArrangorflateUtbetalingStatus.UTBETALT,
            -> intOrNull("sum_utbetaling_linje")?.withValuta(valuta)

            ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
            ArrangorflateUtbetalingStatus.UBEHANDLET_FORSLAG,
            ArrangorflateUtbetalingStatus.BEHANDLES_AV_NAV,
            ArrangorflateUtbetalingStatus.AVBRUTT,
            -> null
        }

        val type = UtbetalingType.from(uuidOrNull("korreksjon_gjelder_utbetaling_id"), tilskuddstype).toDto()
        return ArrangorflateUtbetalingKompakt(
            id = uuid("id"),
            status = status,
            type = type,
            pris = int("belop_beregnet").withValuta(valuta),
            godkjentBelop = godkjentBelop,
            periode = periode("periode"),
            gjennomforing = ArrangorflateGjennomforingDto(
                id = uuid("gjennomforing_id"),
                navn = string("gjennomforing_navn"),
                lopenummer = Tiltaksnummer(string("gjennomforing_lopenummer")),
            ),
            arrangor = ArrangorflateArrangorDto(
                id = uuid("arrangor_id"),
                organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
                navn = string("arrangor_navn"),
            ),
            tiltakstype = ArrangorflateTiltakstypeDto(
                navn = string("tiltakstype_navn"),
                tiltakskode = Tiltakskode.valueOf(string("tiltakskode")),
            ),
        )
    }
}
