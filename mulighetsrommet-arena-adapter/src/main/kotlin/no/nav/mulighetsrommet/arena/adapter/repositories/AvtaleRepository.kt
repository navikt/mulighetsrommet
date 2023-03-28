package no.nav.mulighetsrommet.arena.adapter.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.arena.adapter.models.db.Avtale
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import org.intellij.lang.annotations.Language
import java.util.*

class AvtaleRepository(private val db: Database) {
    fun upsert(avtale: Avtale) = query {
        @Language("PostgreSQL")
        val query = """
            insert into avtale(id, avtale_id, aar, lopenr, tiltakskode, leverandor_id, navn, fra_dato, til_dato, ansvarlig_enhet, rammeavtale, status, prisbetingelser)
            values (:id::uuid, :avtale_id, :aar, :lopenr, :tiltakskode, :leverandor_id, :navn, :fra_dato, :til_dato, :ansvarlig_enhet, :rammeavtale, :status, :prisbetingelser)
            on conflict (id)
                do update set avtale_id       = excluded.avtale_id,
                              aar             = excluded.aar,
                              lopenr          = excluded.lopenr,
                              tiltakskode     = excluded.tiltakskode,
                              leverandor_id   = excluded.leverandor_id,
                              navn            = excluded.navn,
                              fra_dato        = excluded.fra_dato,
                              til_dato        = excluded.til_dato,
                              ansvarlig_enhet = excluded.ansvarlig_enhet,
                              rammeavtale     = excluded.rammeavtale,
                              status          = excluded.status,
                              prisbetingelser = excluded.prisbetingelser
            returning *
        """.trimIndent()

        queryOf(query, avtale.toSqlParameters())
            .map { it.toAvtale() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun get(id: UUID): Avtale? {
        @Language("PostgreSQL")
        val query = """
            select id, avtale_id, aar, lopenr, tiltakskode, leverandor_id, navn, fra_dato, til_dato, ansvarlig_enhet, rammeavtale, status, prisbetingelser
            from avtale
            where id = ?::uuid
        """.trimIndent()

        return queryOf(query, id)
            .map { it.toAvtale() }
            .asSingle
            .let { db.run(it) }
    }

    fun delete(id: UUID): QueryResult<Unit> = query {
        @Language("PostgreSQL")
        val query = """
            delete from avtale
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id)
            .asExecute
            .let { db.run(it) }
    }

    private fun Avtale.toSqlParameters() = mapOf(
        "id" to id,
        "avtale_id" to avtaleId,
        "aar" to aar,
        "lopenr" to lopenr,
        "tiltakskode" to tiltakskode,
        "leverandor_id" to leverandorId,
        "navn" to navn,
        "fra_dato" to fraDato,
        "til_dato" to tilDato,
        "ansvarlig_enhet" to ansvarligEnhet,
        "rammeavtale" to rammeavtale,
        "status" to status.name,
        "prisbetingelser" to prisbetingelser,
    )

    private fun Row.toAvtale() = Avtale(
        id = uuid("id"),
        avtaleId = int("avtale_id"),
        aar = int("aar"),
        lopenr = int("lopenr"),
        tiltakskode = string("tiltakskode"),
        leverandorId = int("leverandor_id"),
        navn = string("navn"),
        fraDato = localDateTime("fra_dato"),
        tilDato = localDateTime("til_dato"),
        ansvarligEnhet = string("ansvarlig_enhet"),
        rammeavtale = boolean("rammeavtale"),
        status = Avtale.Status.valueOf(string("status")),
        prisbetingelser = stringOrNull("prisbetingelser"),
    )
}
