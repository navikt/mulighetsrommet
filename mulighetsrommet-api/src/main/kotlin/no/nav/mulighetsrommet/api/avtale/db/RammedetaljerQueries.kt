package no.nav.mulighetsrommet.api.avtale.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.Valuta
import org.intellij.lang.annotations.Language
import java.util.UUID

class RammedetaljerQueries(private val session: Session) {

    fun upsert(dbo: RammedetaljerDbo) = withTransaction(session) {
        @Language("PostgreSQL")
        val query = """
            insert into avtale_rammedetaljer (
                avtale_id,
                valuta,
                total_ramme,
                utbetalt_arena
            ) values (
                :avtale_id::uuid,
                :valuta::currency,
                :total_ramme,
                :utbetalt_arena
            )
            on conflict (avtale_id) do update set
                valuta = excluded.valuta,
                total_ramme = excluded.total_ramme,
                utbetalt_arena = excluded.utbetalt_arena
        """.trimIndent()

        val params = mapOf(
            "avtale_id" to dbo.avtaleId,
            "valuta" to dbo.valuta.name,
            "total_ramme" to dbo.totalRamme,
            "utbetalt_arena" to dbo.utbetaltArena,
        )

        execute(queryOf(query, params))
    }

    fun get(avtaleId: UUID): RammedetaljerDbo? {
        @Language("PostgreSQL")
        val query = """
            select * from avtale_rammedetaljer
            where avtale_id = :avtale_id::uuid
        """.trimIndent()
        val params = mapOf(
            "avtale_id" to avtaleId,
        )

        return session.single(queryOf(query, params)) { it.toDbo() }
    }

    fun delete(avtaleId: UUID) = withTransaction(session) {
        @Language("PostgreSQL")
        val query = """
            delete from avtale_rammedetaljer
            where avtale_id = :avtale_id::uuid
        """.trimIndent()
        val params = mapOf(
            "avtale_id" to avtaleId,
        )
        execute(queryOf(query, params))
    }
}

fun Row.toDbo(): RammedetaljerDbo {
    return RammedetaljerDbo(
        avtaleId = uuid("avtale_id"),
        valuta = string("valuta").let { Valuta.valueOf(it) },
        totalRamme = long("total_ramme"),
        utbetaltArena = longOrNull("utbetalt_arena"),
    )
}
