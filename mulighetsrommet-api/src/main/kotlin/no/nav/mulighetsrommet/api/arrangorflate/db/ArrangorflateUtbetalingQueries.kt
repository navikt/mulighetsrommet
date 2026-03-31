package no.nav.mulighetsrommet.api.arrangorflate.db

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.arrangor.model.Betalingsinformasjon
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateFilterDirection
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateUtbetalingFilter
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregning
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
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.database.utils.mapPaginated
import no.nav.mulighetsrommet.model.JournalpostId
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

class ArrangorflateUtbetalingQueries(val session: Session) {
    fun getFiltered(
        arrangorer: Set<Organisasjonsnummer>,
        filter: ArrangorflateUtbetalingFilter = ArrangorflateUtbetalingFilter(),
    ): PaginatedResult<Utbetaling> {
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
            from view_utbetaling
            where (:sok::text is null
                or arrangor_navn ilike :sok
                or arrangor_organisasjonsnummer ilike :sok
                or tiltakstype_navn ilike :sok
                or belop_beregnet::text ilike :sok
                or gjennomforing_navn ilike :sok
                or gjennomforing_lopenummer ilike :sok
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
            "sok" to filter.sok?.let { "%$it%" },
            "orgnr_list" to session.createArrayOfValue(arrangorer) { it.value },
            "status_list" to session.createTextArray(filter.type.utbetalingStatuser()),
        )
        return queryOf(query, params + filter.pagination.parameters)
            .mapPaginated { it.toUtbetaling() }
            .runWithSession(session)
    }

    private fun Row.toUtbetaling(): Utbetaling {
        val id = uuid("id")
        val valuta = string("valuta").let { Valuta.valueOf(it) }
        val beregning = getBeregning(id, valuta, UtbetalingBeregningType.valueOf(string("beregning_type")))
        return Utbetaling(
            id = id,
            gjennomforing = Utbetaling.Gjennomforing(
                id = uuid("gjennomforing_id"),
                lopenummer = Tiltaksnummer(string("gjennomforing_lopenummer")),
                navn = string("gjennomforing_navn"),
                start = localDate("gjennomforing_start_dato"),
                slutt = localDateOrNull("gjennomforing_slutt_dato"),
            ),
            arrangor = Utbetaling.Arrangor(
                id = uuid("arrangor_id"),
                organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
                navn = string("arrangor_navn"),
                slettet = boolean("arrangor_slettet"),
            ),
            tiltakstype = Utbetaling.Tiltakstype(
                navn = string("tiltakstype_navn"),
                tiltakskode = Tiltakskode.valueOf(string("tiltakskode")),
            ),
            korreksjon = uuidOrNull("korreksjon_gjelder_utbetaling_id")?.let { gjelderUtbetalingId ->
                Utbetaling.Korreksjon(
                    gjelderUtbetalingId = gjelderUtbetalingId,
                    begrunnelse = string("korreksjon_begrunnelse"),
                )
            },
            innsending = localDateTimeOrNull("innsendt_av_arrangor_tidspunkt")?.let { tidspunkt ->
                Utbetaling.Innsending(tidspunkt)
            },
            status = UtbetalingStatusType.valueOf(string("status")),
            valuta = valuta,
            beregning = beregning,
            betalingsinformasjon = this.toBankKonto(),
            journalpostId = stringOrNull("journalpost_id")?.let { JournalpostId.parse(it) },
            periode = periode("periode"),
            createdAt = localDateTime("created_at"),
            updatedAt = localDateTime("updated_at"),
            kommentar = stringOrNull("kommentar"),
            begrunnelseMindreBetalt = stringOrNull("begrunnelse_mindre_betalt"),
            tilskuddstype = Tilskuddstype.valueOf(string("tilskuddstype")),
            utbetalesTidligstTidspunkt = instantOrNull("utbetales_tidligst_tidspunkt"),
            avbruttBegrunnelse = stringOrNull("avbrutt_begrunnelse"),
            avbruttTidspunkt = instantOrNull("avbrutt_tidspunkt"),
            blokkeringer = array<String>("blokkeringer").map { Utbetaling.Blokkering.valueOf(it) }.toSet(),
        )
    }

    private fun Row.toBankKonto(): Betalingsinformasjon? = when (val iban = stringOrNull("iban")) {
        null -> stringOrNull("kontonummer")?.let {
            Betalingsinformasjon.BBan(
                kontonummer = Kontonummer(it),
                kid = stringOrNull("kid")?.let { Kid.parseOrThrow(it) },
            )
        }

        else -> Betalingsinformasjon.IBan(
            iban = iban,
            bic = string("bic"),
            bankLandKode = string("bank_land_kode"),
            bankNavn = string("bank_navn"),
        )
    }

    private fun getBeregning(id: UUID, valuta: Valuta, beregning: UtbetalingBeregningType): UtbetalingBeregning {
        return when (beregning) {
            UtbetalingBeregningType.FRI -> getBeregningFri(id, valuta)

            UtbetalingBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED -> {
                getBeregningFastSatsPerTiltaksplassPerManed(id, valuta)
            }

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
}
