package no.nav.mulighetsrommet.api.avtale.db

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.avtale.model.toDto
import org.intellij.lang.annotations.Language
import java.util.UUID

class PrismodellQueries(private val session: Session) {
    fun upsertPrismodell(dbo: PrismodellDbo) {
        @Language("PostgreSQL")
        val query = """
            insert into prismodell(id,
                                   prisbetingelser,
                                   prismodell_type,
                                   satser)
            values (:id::uuid,
                    :prisbetingelser,
                    :prismodell::prismodell_type,
                    :satser::jsonb)
            on conflict (id) do update set prisbetingelser = excluded.prisbetingelser,
                                           prismodell_type = excluded.prismodell_type,
                                           satser          = excluded.satser
        """.trimIndent()
        val params = mapOf(
            "id" to dbo.id,
            "prismodell" to dbo.type.name,
            "prisbetingelser" to dbo.prisbetingelser,
            "satser" to Json.encodeToString(dbo.satser),
        )
        session.execute(queryOf(query, params))
    }

    fun getPrismodell(id: UUID): Prismodell? {
        @Language("PostgreSQL")
        val query = """
            select id as prismodell_id,
                   prismodell_type,
                   prisbetingelser as prismodell_prisbetingelser,
                   satser as prismodell_satser
            from prismodell
            where id = ?::uuid
        """.trimIndent()
        return session.single(queryOf(query, id)) { it.toPrismodell() }
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

fun Row.toPrismodell(): Prismodell? {
    return uuidOrNull("prismodell_id")?.let { prismodellId ->
        val prismodellType = PrismodellType.valueOf(string("prismodell_type"))
        val prisbetingelser = stringOrNull("prismodell_prisbetingelser")
        val satser = stringOrNull("prismodell_satser")
            ?.let { Json.decodeFromString<List<AvtaltSats>>(it) }
            ?: emptyList()
        when (prismodellType) {
            PrismodellType.ANNEN_AVTALT_PRIS -> Prismodell.AnnenAvtaltPris(
                id = prismodellId,
                prisbetingelser = prisbetingelser,
            )

            PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK -> Prismodell.ForhandsgodkjentPrisPerManedsverk(
                id = prismodellId,
            )

            PrismodellType.AVTALT_PRIS_PER_MANEDSVERK -> Prismodell.AvtaltPrisPerManedsverk(
                id = prismodellId,
                prisbetingelser = prisbetingelser,
                satser = satser.toDto(),
            )

            PrismodellType.AVTALT_PRIS_PER_UKESVERK -> Prismodell.AvtaltPrisPerUkesverk(
                id = prismodellId,
                prisbetingelser = prisbetingelser,
                satser = satser.toDto(),
            )

            PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK -> Prismodell.AvtaltPrisPerHeleUkesverk(
                id = prismodellId,
                prisbetingelser = prisbetingelser,
                satser = satser.toDto(),
            )

            PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER -> Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker(
                id = prismodellId,
                prisbetingelser = prisbetingelser,
                satser = satser.toDto(),
            )
        }
    }
}
