package no.nav.mulighetsrommet.api.utbetaling.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utbetaling.model.Delutbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
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

class DelutbetalingQueries(private val session: Session) {
    fun upsert(delutbetaling: DelutbetalingDbo) {
        @Language("PostgreSQL")
        val query = """
            insert into delutbetaling (
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
                :status::delutbetaling_status,
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
            "id" to delutbetaling.id,
            "tilsagn_id" to delutbetaling.tilsagnId,
            "utbetaling_id" to delutbetaling.utbetalingId,
            "status" to delutbetaling.status.name,
            "faktura_status_endret_tidspunkt" to delutbetaling.fakturaStatusEndretTidspunkt,
            "belop" to delutbetaling.pris.belop,
            "valuta" to delutbetaling.pris.valuta.name,
            "gjor_opp_tilsagn" to delutbetaling.gjorOppTilsagn,
            "periode" to delutbetaling.periode.toDaterange(),
            "lopenummer" to delutbetaling.lopenummer,
            "fakturanummer" to delutbetaling.fakturanummer,
            "faktura_status" to delutbetaling.fakturaStatus?.name,
            "datastream_periode_start" to delutbetaling.periode.start,
            "datastream_periode_slutt" to delutbetaling.periode.getLastInclusiveDate(),
        )

        session.execute(queryOf(query, params))
    }

    fun getNextLopenummerByTilsagn(tilsagnId: UUID): Int {
        @Language("PostgreSQL")
        val query = """
            select coalesce(max(lopenummer), 0) + 1 as lopenummer
            from delutbetaling
            where tilsagn_id = ?
        """.trimIndent()

        return session.requireSingle(queryOf(query, tilsagnId)) { it.int("lopenummer") }
    }

    fun setStatus(id: UUID, status: DelutbetalingStatus) {
        @Language("PostgreSQL")
        val query = """
            update delutbetaling
            set status = :status::delutbetaling_status
            where id = :id::uuid
        """.trimIndent()

        val params = mapOf(
            "id" to id,
            "status" to status.name,
        )

        session.execute(queryOf(query, params))
    }

    fun setStatus(fakturanummer: String, status: DelutbetalingStatus) {
        @Language("PostgreSQL")
        val query = """
            update delutbetaling
            set status = :status::delutbetaling_status
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
            update delutbetaling
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
            update delutbetaling
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

    fun getByUtbetalingId(id: UUID): List<Delutbetaling> {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_utbetaling_linje
            where utbetaling_id = ?
        """.trimIndent()

        return session.list(queryOf(query, id)) { it.toDelutbetaling() }
    }

    fun getOrError(id: UUID): Delutbetaling {
        return checkNotNull(get(id)) { "Delutbetaling med id $id finnes ikke" }
    }

    fun get(id: UUID): Delutbetaling? {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_utbetaling_linje
            where id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, id)) { it.toDelutbetaling() }
    }

    fun getByAvtale(avtaleId: UUID, statuser: Set<DelutbetalingStatus> = emptySet()): List<Delutbetaling> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_utbetaling_linje du
              join utbetaling u on du.utbetaling_id = u.id
              join gjennomforing g on u.gjennomforing_id = g.id
            where g.avtale_id = :avtale_id::uuid
              and (:statuser::delutbetaling_status[] is null or du.status = any(:statuser::delutbetaling_status[]))
        """.trimIndent()
        val params = mapOf(
            "avtale_id" to avtaleId,
            "statuser" to statuser.ifEmpty { null }?.let { createArrayOfValue(it) { it.name } },
        )

        return list(queryOf(query, params)) { it.toDelutbetaling() }
    }

    fun getOrError(fakturanummer: String): Delutbetaling {
        return checkNotNull(get(fakturanummer)) { "Delutbetaling med fakturanummer $fakturanummer finnes ikke" }
    }

    fun get(fakturanummer: String): Delutbetaling? {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_utbetaling_linje
            where fakturanummer = ?
        """.trimIndent()

        return session.single(queryOf(query, fakturanummer)) { it.toDelutbetaling() }
    }

    fun delete(id: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete from delutbetaling where id = ?::uuid
        """.trimIndent()

        session.execute(queryOf(query, id))
    }
}

private fun Row.toDelutbetaling(): Delutbetaling {
    val valuta = string("valuta").let { Valuta.valueOf(it) }
    val faktura = stringOrNull("faktura_status")?.let { status ->
        Delutbetaling.Faktura(
            fakturanummer = string("fakturanummer"),
            utbetalesTidligstTidspunkt = instantOrNull("utbetales_tidligst_tidspunkt"),
            sendtTidspunkt = localDateTime("faktura_sendt_tidspunkt"),
            statusEndretTidspunkt = localDateTime("faktura_status_endret_tidspunkt"),
            status = FakturaStatusType.valueOf(status),
        )
    } ?: Delutbetaling.Faktura(
        fakturanummer = string("fakturanummer"),
        utbetalesTidligstTidspunkt = instantOrNull("utbetales_tidligst_tidspunkt"),
        sendtTidspunkt = localDateTimeOrNull("faktura_sendt_tidspunkt"),
        statusEndretTidspunkt = null,
        status = null,
    )
    return Delutbetaling(
        id = uuid("id"),
        tilsagnId = uuid("tilsagn_id"),
        utbetalingId = uuid("utbetaling_id"),
        pris = int("belop").withValuta(valuta),
        gjorOppTilsagn = boolean("gjor_opp_tilsagn"),
        periode = periode("periode"),
        lopenummer = int("lopenummer"),
        status = DelutbetalingStatus.valueOf(string("status")),
        faktura = faktura,
    )
}
