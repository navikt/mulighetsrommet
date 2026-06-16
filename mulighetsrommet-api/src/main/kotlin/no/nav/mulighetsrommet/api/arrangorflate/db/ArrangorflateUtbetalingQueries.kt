package no.nav.mulighetsrommet.api.arrangorflate.db

import kotlinx.serialization.json.Json
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
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregning
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerHeleUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerTimeOppfolging
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningType
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.database.datatypes.periode
import no.nav.mulighetsrommet.database.requireSingle
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
        val beregning = getBeregning(id, valuta, UtbetalingBeregningType.valueOf(string("beregning_type")))

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

    private fun getBeregning(id: UUID, valuta: Valuta, beregning: UtbetalingBeregningType): UtbetalingBeregning {
        return when (beregning) {
            UtbetalingBeregningType.FRI -> getBeregningFri(id, valuta)

            UtbetalingBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED -> {
                getBeregningFastSatsPerTiltaksplassPerManed(id, valuta)
            }

            UtbetalingBeregningType.FAST_SATS_PER_AVTALT_TILTAKSPLASS_PER_MANED -> getBeregningPrisFraTilsagn(
                id,
                valuta,
            )

            UtbetalingBeregningType.PRIS_PER_MANEDSVERK -> getBeregningPrisPerManedsverk(id, valuta)

            UtbetalingBeregningType.PRIS_PER_UKESVERK -> getBeregningPrisPerUkesverk(id, valuta)

            UtbetalingBeregningType.PRIS_PER_HELE_UKESVERK -> getBeregningPrisPerHeleUkesverk(id, valuta)

            UtbetalingBeregningType.PRIS_PER_TIME_OPPFOLGING -> getBeregningPrisPerTimeOppfolging(id, valuta)
        }
    }

    private fun getBeregningFri(id: UUID, valuta: Valuta): UtbetalingBeregningFri {
        @Language("PostgreSQL")
        val query = """
            select belop_beregnet
            from utbetaling
            where id = ?::uuid
        """.trimIndent()

        return session.requireSingle(queryOf(query, id)) { row ->
            UtbetalingBeregningFri(
                input = UtbetalingBeregningFri.Input(
                    pris = row.int("belop_beregnet").withValuta(valuta),
                ),
                output = UtbetalingBeregningFri.Output(
                    pris = row.int("belop_beregnet").withValuta(valuta),
                ),
            )
        }
    }

    private fun getBeregningFastSatsPerTiltaksplassPerManed(id: UUID, valuta: Valuta): UtbetalingBeregning {
        @Language("PostgreSQL")
        val query = """
            select utbetaling.belop_beregnet,
                   satser.perioder_json as sats_perioder_json,
                   coalesce(stengt.perioder_json, '[]'::jsonb) as stengt_perioder_json,
                   coalesce(deltakelser_input.perioder_json, '[]'::jsonb) as deltakelser_input_json,
                   coalesce(deltakelser_output.perioder_json, '[]'::jsonb) as deltakelser_output_json
            from utbetaling
                     join view_utbetaling_input_satser_json satser on utbetaling.id = satser.utbetaling_id
                     left join view_utbetaling_input_stengt_json stengt on utbetaling.id = stengt.utbetaling_id
                     left join view_utbetaling_input_deltakelsesprosent_perioder_json deltakelser_input on utbetaling.id = deltakelser_input.utbetaling_id
                     left join view_utbetaling_output_deltakelse_perioder_json deltakelser_output on utbetaling.id = deltakelser_output.utbetaling_id
            where id = ?::uuid
        """.trimIndent()

        return session.requireSingle(queryOf(query, id)) { row ->
            UtbetalingBeregningFastSatsPerTiltaksplassPerManed(
                input = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(
                    satser = Json.decodeFromString(row.string("sats_perioder_json")),
                    stengt = Json.decodeFromString(row.string("stengt_perioder_json")),
                    deltakelser = Json.decodeFromString(row.string("deltakelser_input_json")),
                ),
                output = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                    deltakelser = Json.decodeFromString(row.string("deltakelser_output_json")),
                    pris = row.int("belop_beregnet").withValuta(valuta),
                ),
            )
        }
    }

    private fun getBeregningPrisFraTilsagn(
        id: UUID,
        valuta: Valuta,
    ): UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed {
        @Language("PostgreSQL")
        val belopQuery = """
            select belop_beregnet
            from utbetaling
            where id = ?::uuid
        """.trimIndent()

        val belopBeregnet = session.requireSingle(queryOf(belopQuery, id)) { row ->
            row.int("belop_beregnet")
        }

        @Language("PostgreSQL")
        val bidragQuery = """
            select tilsagn_id,
                   tilsagn_periode,
                   tilsagn_belop,
                   tilsagn_gjenstaende_belop,
                   bidrag_periode,
                   bidrag_belop
            from utbetaling_tilsagn_bidrag
            where utbetaling_id = ?::uuid
        """.trimIndent()

        data class TilsagnRow(
            val input: UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed.TilsagnInput,
            val bidrag: UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed.TilsagnBidrag,
        )

        val rows = session.list(queryOf(bidragQuery, id)) { row ->
            TilsagnRow(
                input = UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed.TilsagnInput(
                    tilsagnId = row.uuid("tilsagn_id"),
                    periode = row.periode("tilsagn_periode"),
                    beregnetBelop = row.int("tilsagn_belop").withValuta(valuta),
                    gjenstaendeBelop = row.int("tilsagn_gjenstaende_belop").withValuta(valuta),
                ),
                bidrag = UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed.TilsagnBidrag(
                    tilsagnId = row.uuid("tilsagn_id"),
                    periode = row.periode("bidrag_periode"),
                    bidrag = row.int("bidrag_belop").withValuta(valuta),
                ),
            )
        }

        return UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed(
            input = UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed.Input(
                tilsagn = rows.map { it.input },
            ),
            output = UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed.Output(
                pris = belopBeregnet.withValuta(valuta),
                tilsagnBidrag = rows.map { it.bidrag },
            ),
        )
    }

    private fun getBeregningPrisPerManedsverk(id: UUID, valuta: Valuta): UtbetalingBeregning {
        return getBeregningDeltakelsesfaktor(id) { row ->
            UtbetalingBeregningPrisPerManedsverk(
                input = UtbetalingBeregningPrisPerManedsverk.Input(
                    satser = Json.decodeFromString(row.string("sats_perioder_json")),
                    stengt = Json.decodeFromString(row.string("stengt_perioder_json")),
                    deltakelser = Json.decodeFromString(row.string("deltakelser_input_json")),
                ),
                output = UtbetalingBeregningPrisPerManedsverk.Output(
                    deltakelser = Json.decodeFromString(row.string("deltakelser_output_json")),
                    pris = row.int("belop_beregnet").withValuta(valuta),
                ),
            )
        }
    }

    private fun getBeregningPrisPerUkesverk(id: UUID, valuta: Valuta): UtbetalingBeregningPrisPerUkesverk {
        return getBeregningDeltakelsesfaktor(id) { row ->
            UtbetalingBeregningPrisPerUkesverk(
                input = UtbetalingBeregningPrisPerUkesverk.Input(
                    satser = Json.decodeFromString(row.string("sats_perioder_json")),
                    stengt = Json.decodeFromString(row.string("stengt_perioder_json")),
                    deltakelser = Json.decodeFromString(row.string("deltakelser_input_json")),
                ),
                output = UtbetalingBeregningPrisPerUkesverk.Output(
                    deltakelser = Json.decodeFromString(row.string("deltakelser_output_json")),
                    pris = row.int("belop_beregnet").withValuta(valuta),
                ),
            )
        }
    }

    private fun getBeregningPrisPerHeleUkesverk(id: UUID, valuta: Valuta): UtbetalingBeregningPrisPerHeleUkesverk {
        return getBeregningDeltakelsesfaktor(id) { row ->
            UtbetalingBeregningPrisPerHeleUkesverk(
                input = UtbetalingBeregningPrisPerHeleUkesverk.Input(
                    satser = Json.decodeFromString(row.string("sats_perioder_json")),
                    stengt = Json.decodeFromString(row.string("stengt_perioder_json")),
                    deltakelser = Json.decodeFromString(row.string("deltakelser_input_json")),
                ),
                output = UtbetalingBeregningPrisPerHeleUkesverk.Output(
                    deltakelser = Json.decodeFromString(row.string("deltakelser_output_json")),
                    pris = row.int("belop_beregnet").withValuta(valuta),
                ),
            )
        }
    }

    private inline fun <T> getBeregningDeltakelsesfaktor(
        id: UUID,
        crossinline mapper: (Row) -> T,
    ): T {
        @Language("PostgreSQL")
        val query = """
            select utbetaling.belop_beregnet,
                   satser.perioder_json as sats_perioder_json,
                   coalesce(stengt.perioder_json, '[]'::jsonb) as stengt_perioder_json,
                   coalesce(deltakelser_input.perioder_json, '[]'::jsonb) as deltakelser_input_json,
                   coalesce(deltakelser_output.perioder_json, '[]'::jsonb) as deltakelser_output_json
            from utbetaling
                     join view_utbetaling_input_satser_json satser on utbetaling.id = satser.utbetaling_id
                     left join view_utbetaling_input_stengt_json stengt on utbetaling.id = stengt.utbetaling_id
                     left join view_utbetaling_input_deltakelse_perioder_json deltakelser_input on utbetaling.id = deltakelser_input.utbetaling_id
                     left join view_utbetaling_output_deltakelse_perioder_json deltakelser_output on utbetaling.id = deltakelser_output.utbetaling_id
            where id = ?::uuid
        """.trimIndent()
        return session.requireSingle(queryOf(query, id)) { row -> mapper.invoke(row) }
    }

    private fun getBeregningPrisPerTimeOppfolging(id: UUID, valuta: Valuta): UtbetalingBeregningPrisPerTimeOppfolging {
        @Language("PostgreSQL")
        val query = """
            select utbetaling.belop_beregnet,
                   satser.perioder_json as sats_perioder_json,
                   coalesce(stengt.perioder_json, '[]'::jsonb) as stengt_perioder_json,
                   coalesce(deltakelser_input.perioder_json, '[]'::jsonb) as deltakelser_input_json
            from utbetaling
                     join view_utbetaling_input_satser_json satser on utbetaling.id = satser.utbetaling_id
                     left join view_utbetaling_input_stengt_json stengt on utbetaling.id = stengt.utbetaling_id
                     left join view_utbetaling_input_deltakelse_perioder_json deltakelser_input on utbetaling.id = deltakelser_input.utbetaling_id
            where id = ?::uuid
        """.trimIndent()
        return session.requireSingle(queryOf(query, id)) { row ->
            UtbetalingBeregningPrisPerTimeOppfolging(
                input = UtbetalingBeregningPrisPerTimeOppfolging.Input(
                    satser = Json.decodeFromString(row.string("sats_perioder_json")),
                    stengt = Json.decodeFromString(row.string("stengt_perioder_json")),
                    deltakelser = Json.decodeFromString(row.string("deltakelser_input_json")),
                    pris = row.int("belop_beregnet").withValuta(valuta),
                ),
                output = UtbetalingBeregningPrisPerTimeOppfolging.Output(
                    pris = row.int("belop_beregnet").withValuta(valuta),
                ),
            )
        }
    }

    private fun Row.toArrangorflateUtbetalingKompakt(): ArrangorflateUtbetalingKompakt {
        val valuta = string("valuta").let { Valuta.valueOf(it) }
        val tilskuddstype = Tilskuddstype.valueOf(string("tilskuddstype"))
        val blokkeringer = array<String>("blokkeringer").map { Utbetaling.Blokkering.valueOf(it) }.toSet()
        val status = UtbetalingStatusType.valueOf(string("status")).let { ArrangorflateUtbetalingStatus.fromUtbetaling(it, blokkeringer) }
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
