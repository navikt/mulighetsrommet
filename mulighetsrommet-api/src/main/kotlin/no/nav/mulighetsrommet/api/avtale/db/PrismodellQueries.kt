package no.nav.mulighetsrommet.api.avtale.db

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.model.Valuta
import org.intellij.lang.annotations.Language
import java.util.UUID

class PrismodellQueries(private val session: Session) {
    fun upsert(dbo: PrismodellDbo) {
        @Language("PostgreSQL")
        val query = """
            insert into prismodell(id,
                                   system_id,
                                   prisbetingelser,
                                   prismodell_type,
                                   satser,
                                   valuta,
                                   tilsagn_per_deltaker)
            values (:id::uuid,
                    :system_id,
                    :prisbetingelser,
                    :prismodell::prismodell_type,
                    :satser::jsonb,
                    :valuta::currency,
                    :tilsagn_per_deltaker)
            on conflict (id) do update set system_id            = excluded.system_id,
                                           prisbetingelser      = excluded.prisbetingelser,
                                           prismodell_type      = excluded.prismodell_type,
                                           satser               = excluded.satser,
                                           valuta               = excluded.valuta,
                                           tilsagn_per_deltaker = excluded.tilsagn_per_deltaker
        """.trimIndent()
        val params = mapOf(
            "id" to dbo.id,
            "system_id" to dbo.systemId,
            "prismodell" to dbo.type.name,
            "prisbetingelser" to dbo.prisbetingelser,
            "satser" to Json.encodeToString(dbo.satser),
            "valuta" to dbo.valuta.name,
            "tilsagn_per_deltaker" to dbo.tilsagnPerDeltaker,
        )
        session.execute(queryOf(query, params))
    }

    fun getById(id: UUID): Prismodell? {
        @Language("PostgreSQL")
        val query = """
            select id as prismodell_id,
                   valuta,
                   prismodell_type,
                   prisbetingelser as prismodell_prisbetingelser,
                   satser as prismodell_satser,
                   tilsagn_per_deltaker as prismodell_tilsagn_per_deltaker
            from prismodell
            where id = ?::uuid
        """.trimIndent()
        return session.single(queryOf(query, id)) { it.toPrismodell() }
    }

    fun getBySystemId(systemId: String): Prismodell? {
        @Language("PostgreSQL")
        val query = """
            select id as prismodell_id,
                   valuta as prismodell_valuta,
                   prismodell_type,
                   prisbetingelser as prismodell_prisbetingelser,
                   satser as prismodell_satser,
                   tilsagn_per_deltaker as prismodell_tilsagn_per_deltaker
            from prismodell
            where system_id = ?
        """.trimIndent()
        return session.single(queryOf(query, systemId)) { it.toPrismodell() }
    }

    fun deletePrismodell(id: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete from prismodell
            where id = ?::uuid
        """.trimIndent()
        session.execute(queryOf(query, id))
    }
}

fun Row.toPrismodell(): Prismodell {
    val id = uuid("prismodell_id")
    val type = PrismodellType.valueOf(string("prismodell_type"))
    val valuta = Valuta.valueOf(string("prismodell_valuta"))
    val prisbetingelser = stringOrNull("prismodell_prisbetingelser")
    val satser = stringOrNull("prismodell_satser")?.let { Json.decodeFromString<List<AvtaltSats>?>(it) }
    val tilsagnPerDeltaker = boolean("prismodell_tilsagn_per_deltaker")
    return Prismodell.from(type, id, valuta, prisbetingelser, satser, tilsagnPerDeltaker)
}
