package no.nav.mulighetsrommet.api.persistence.tiltak

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.domain.tiltak.AvtaltSats
import no.nav.mulighetsrommet.api.domain.tiltak.Prismodell
import no.nav.mulighetsrommet.api.domain.tiltak.PrismodellType
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.model.Valuta
import org.intellij.lang.annotations.Language
import java.util.UUID

class PrismodellQueries(private val session: Session) {

    fun upsert(prismodell: Prismodell) {
        @Language("PostgreSQL")
        val query = """
            insert into prismodell(id,
                                   system_id,
                                   prisbetingelser,
                                   prismodell_type,
                                   satser,
                                   valuta,
                                   tilsagn_per_deltaker,
                                   totalbelop,
                                   tilskudd,
                                   aarsak)
            values (:id::uuid,
                    :system_id,
                    :prisbetingelser,
                    :prismodell,
                    :satser::jsonb,
                    :valuta::currency,
                    :tilsagn_per_deltaker,
                    :totalbelop,
                    :tilskudd::jsonb,
                    :aarsak)
            on conflict (id) do update set system_id            = excluded.system_id,
                                           prisbetingelser      = excluded.prisbetingelser,
                                           prismodell_type      = excluded.prismodell_type,
                                           satser               = excluded.satser,
                                           valuta               = excluded.valuta,
                                           tilsagn_per_deltaker = excluded.tilsagn_per_deltaker,
                                           totalbelop           = excluded.totalbelop,
                                           tilskudd             = excluded.tilskudd,
                                           aarsak               = excluded.aarsak
        """.trimIndent()

        val params = mapOf(
            "id" to prismodell.id,
            "system_id" to when (prismodell) {
                is Prismodell.FastSatsPerBenyttetPlassPerManed -> prismodell.systemId
                is Prismodell.FastSatsPerAvtaltPlassPerManed -> prismodell.systemId
                else -> null
            },
            "prismodell" to prismodell.type.name,
            "prisbetingelser" to prismodell.prisbetingelser(),
            "satser" to Json.encodeToString(prismodell.satser()),
            "valuta" to prismodell.valuta.name,
            "tilsagn_per_deltaker" to (prismodell as? Prismodell.AnnenAvtaltPris)?.tilsagnPerDeltaker,
            "totalbelop" to (prismodell as? Prismodell.AnnenAvtaltPris)?.totalbelop,
            "tilskudd" to (prismodell as? Prismodell.TilskuddTilOpplaering)?.tilskudd?.let { Json.encodeToString(it) },
            "aarsak" to (prismodell as? Prismodell.IngenKostnader)?.aarsak?.name,
        )
        session.execute(queryOf(query, params))
    }

    fun getOrError(id: UUID): Prismodell {
        @Language("PostgreSQL")
        val query = """
            select id as prismodell_id,
                   valuta as prismodell_valuta,
                   prismodell_type,
                   prisbetingelser as prismodell_prisbetingelser,
                   satser as prismodell_satser,
                   tilsagn_per_deltaker as prismodell_tilsagn_per_deltaker,
                   totalbelop as prismodell_totalbelop,
                   tilskudd as prismodell_tilskudd,
                   aarsak as prismodell_aarsak
            from prismodell
            where id = ?::uuid
        """.trimIndent()
        return session.requireSingle(queryOf(query, id)) { it.toPrismodell() }
    }

    fun getBySystemId(systemId: String): Prismodell? {
        @Language("PostgreSQL")
        val query = """
            select id as prismodell_id,
                   valuta as prismodell_valuta,
                   prismodell_type,
                   prisbetingelser as prismodell_prisbetingelser,
                   satser as prismodell_satser,
                   tilsagn_per_deltaker as prismodell_tilsagn_per_deltaker,
                   totalbelop as prismodell_totalbelop,
                   tilskudd as prismodell_tilskudd,
                   aarsak as prismodell_aarsak
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
    val tilsagnPerDeltaker = anyOrNull("prismodell_tilsagn_per_deltaker") as Boolean?
    val totalbelop = intOrNull("prismodell_totalbelop")
    val tilskudd = stringOrNull("prismodell_tilskudd")?.let {
        Json.decodeFromString<Map<Opplaeringtilskudd.Kode, Int>>(it)
    }
    val aarsak = stringOrNull("prismodell_aarsak")
    return Prismodell.from(id, type, valuta, prisbetingelser, satser, tilsagnPerDeltaker, totalbelop, tilskudd, aarsak)
}
