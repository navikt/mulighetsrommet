package no.nav.mulighetsrommet.api.utbetaling.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinje
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeStatus
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.database.datatypes.periode
import no.nav.mulighetsrommet.database.datatypes.toDaterange
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.withValuta
import no.nav.tiltak.okonomi.FakturaStatusType
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.UUID

class UtbetalingLinjeQueries(private val session: Session) {
    fun upsert(linje: UtbetalingLinjeDbo) {
        @Language("PostgreSQL")
        val query = """
            insert into utbetaling_linje (
                id,
                tilsagn_id,
                utbetaling_id,
                status,
                belop,
                valuta,
                gjor_opp_tilsagn,
                periode,
                lopenummer,
                fakturanummer,
                faktura_status,
                faktura_status_endret_tidspunkt,
                datastream_periode_start,
                datastream_periode_slutt
            ) values (
                :id::uuid,
                :tilsagn_id::uuid,
                :utbetaling_id::uuid,
                :status,
                :belop,
                :valuta::currency,
                :gjor_opp_tilsagn::boolean,
                :periode::daterange,
                :lopenummer,
                :fakturanummer,
                :faktura_status,
                :faktura_status_endret_tidspunkt::timestamp,
                :datastream_periode_start::date,
                :datastream_periode_slutt::date
            ) on conflict (id) do update set
                status                          = excluded.status,
                belop                           = excluded.belop,
                valuta                          = excluded.valuta,
                gjor_opp_tilsagn                = excluded.gjor_opp_tilsagn,
                periode                         = excluded.periode,
                lopenummer                      = excluded.lopenummer,
                fakturanummer                   = excluded.fakturanummer,
                faktura_status                  = excluded.faktura_status,
                faktura_status_endret_tidspunkt = excluded.faktura_status_endret_tidspunkt,
                datastream_periode_start        = excluded.datastream_periode_start,
                datastream_periode_slutt        = excluded.datastream_periode_slutt
        """.trimIndent()

        val params = mapOf(
            "id" to linje.id,
            "tilsagn_id" to linje.tilsagnId,
            "utbetaling_id" to linje.utbetalingId,
            "status" to linje.status.name,
            "faktura_status_endret_tidspunkt" to linje.fakturaStatusEndretTidspunkt,
            "belop" to linje.pris.belop,
            "valuta" to linje.pris.valuta.name,
            "gjor_opp_tilsagn" to linje.gjorOppTilsagn,
            "periode" to linje.periode.toDaterange(),
            "lopenummer" to linje.lopenummer,
            "fakturanummer" to linje.fakturanummer,
            "faktura_status" to linje.fakturaStatus?.name,
            "datastream_periode_start" to linje.periode.start,
            "datastream_periode_slutt" to linje.periode.getLastInclusiveDate(),
        )

        session.execute(queryOf(query, params))
    }

    fun getNextLopenummerByTilsagn(tilsagnId: UUID): Int {
        @Language("PostgreSQL")
        val query = """
            select coalesce(max(lopenummer), 0) + 1 as lopenummer
            from utbetaling_linje
            where tilsagn_id = ?
        """.trimIndent()

        return session.requireSingle(queryOf(query, tilsagnId)) { it.int("lopenummer") }
    }

    fun setStatus(id: UUID, status: UtbetalingLinjeStatus) {
        @Language("PostgreSQL")
        val query = """
            update utbetaling_linje
            set status = :status
            where id = :id::uuid
        """.trimIndent()

        val params = mapOf(
            "id" to id,
            "status" to status.name,
        )

        session.execute(queryOf(query, params))
    }

    fun setStatus(fakturanummer: String, status: UtbetalingLinjeStatus) {
        @Language("PostgreSQL")
        val query = """
            update utbetaling_linje
            set status = :status
            where fakturanummer = :fakturanummer
        """.trimIndent()

        val params = mapOf(
            "fakturanummer" to fakturanummer,
            "status" to status.name,
        )

        session.execute(queryOf(query, params))
    }

    fun setFakturaSendtTidspunk(id: UUID, tidspunkt: LocalDateTime) {
        @Language("PostgreSQL")
        val query = """
            update utbetaling_linje
            set faktura_sendt_tidspunkt = :tidspunkt
            where id = :id::uuid
        """.trimIndent()

        val params = mapOf(
            "id" to id,
            "tidspunkt" to tidspunkt,
        )

        session.execute(queryOf(query, params))
    }

    fun setFakturaStatus(
        fakturanummer: String,
        status: FakturaStatusType,
        fakturaStatusEndretTidspunkt: LocalDateTime,
    ) {
        @Language("PostgreSQL")
        val query = """
            update utbetaling_linje
            set faktura_status = :faktura_status,
                faktura_status_endret_tidspunkt = :faktura_status_endret_tidspunkt
            where fakturanummer = :fakturanummer
        """.trimIndent()

        val params = mapOf(
            "fakturanummer" to fakturanummer,
            "faktura_status" to status.name,
            "faktura_status_endret_tidspunkt" to fakturaStatusEndretTidspunkt,
        )

        session.execute(queryOf(query, params))
    }

    fun getByUtbetalingId(id: UUID): List<UtbetalingLinje> {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_utbetaling_linje
            where utbetaling_id = ?
        """.trimIndent()

        return session.list(queryOf(query, id)) { it.toUtbetalingLinje() }
    }

    fun getOrError(id: UUID): UtbetalingLinje {
        return checkNotNull(get(id)) { "UtbetalingLinje med id $id finnes ikke" }
    }

    fun get(id: UUID): UtbetalingLinje? {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_utbetaling_linje
            where id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, id)) { it.toUtbetalingLinje() }
    }

    fun getByAvtale(avtaleId: UUID, statuser: Set<UtbetalingLinjeStatus> = emptySet()): List<UtbetalingLinje> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_utbetaling_linje du
              join utbetaling u on du.utbetaling_id = u.id
              join gjennomforing g on u.gjennomforing_id = g.id
            where g.avtale_id = :avtale_id::uuid
              and (:statuser::text[] is null or du.status = any(:statuser))
        """.trimIndent()
        val params = mapOf(
            "avtale_id" to avtaleId,
            "statuser" to statuser.ifEmpty { null }?.let { createArrayOfValue(it) { it.name } },
        )

        return list(queryOf(query, params)) { it.toUtbetalingLinje() }
    }

    fun getOrError(fakturanummer: String): UtbetalingLinje {
        return checkNotNull(get(fakturanummer)) { "UtbetalingLinje med fakturanummer $fakturanummer finnes ikke" }
    }

    fun get(fakturanummer: String): UtbetalingLinje? {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_utbetaling_linje
            where fakturanummer = ?
        """.trimIndent()

        return session.single(queryOf(query, fakturanummer)) { it.toUtbetalingLinje() }
    }

    fun delete(id: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete from utbetaling_linje where id = ?::uuid
        """.trimIndent()

        session.execute(queryOf(query, id))
    }
}

private fun Row.toUtbetalingLinje(): UtbetalingLinje {
    val valuta = string("valuta").let { Valuta.valueOf(it) }
    val faktura = stringOrNull("faktura_status")?.let { status ->
        UtbetalingLinje.Faktura(
            fakturanummer = string("fakturanummer"),
            utbetalesTidligstTidspunkt = instantOrNull("utbetales_tidligst_tidspunkt"),
            sendtTidspunkt = localDateTime("faktura_sendt_tidspunkt"),
            statusEndretTidspunkt = localDateTime("faktura_status_endret_tidspunkt"),
            status = FakturaStatusType.valueOf(status),
        )
    } ?: UtbetalingLinje.Faktura(
        fakturanummer = string("fakturanummer"),
        utbetalesTidligstTidspunkt = instantOrNull("utbetales_tidligst_tidspunkt"),
        sendtTidspunkt = localDateTimeOrNull("faktura_sendt_tidspunkt"),
        statusEndretTidspunkt = null,
        status = null,
    )
    return UtbetalingLinje(
        id = uuid("id"),
        tilsagnId = uuid("tilsagn_id"),
        utbetalingId = uuid("utbetaling_id"),
        pris = int("belop").withValuta(valuta),
        gjorOppTilsagn = boolean("gjor_opp_tilsagn"),
        periode = periode("periode"),
        lopenummer = int("lopenummer"),
        status = UtbetalingLinjeStatus.valueOf(string("status")),
        faktura = faktura,
    )
}
