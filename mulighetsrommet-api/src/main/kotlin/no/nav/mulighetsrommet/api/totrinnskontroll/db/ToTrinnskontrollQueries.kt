package no.nav.mulighetsrommet.api.totrinnskontroll.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.api.totrinnskontroll.model.ToTrinnskontroll
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.model.NavIdent
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.*

enum class ToTrinnskontrollType {
    OPPRETT,
    ANNULLER,
}

class ToTrinnskontrollQueries(private val session: Session) {
    fun opprett(
        entityId: UUID,
        opprettetAv: NavIdent,
        aarsaker: List<String>,
        forklaring: String?,
        type: ToTrinnskontrollType,
    ) {
        @Language("PostgreSQL")
        val query = """
            insert into to_trinnskontroll (
                entity_id,
                opprettet_av,
                opprettet_tidspunkt,
                aarsaker,
                forklaring,
                type
            ) values (
                :entity_id::uuid,
                :opprettet_av,
                :opprettet_tidspunkt,
                :aarsaker,
                :forklaring,
                :type::to_trinnskontroll_type
            ) on conflict (entity_id, type) do update set
                opprettet_av        = excluded.opprettet_av,
                opprettet_tidspunkt = excluded.opprettet_tidspunkt,
                aarsaker            = coalesce(excluded.aarsaker, to_trinnskontroll.aarsaker),
                forklaring          = coalesce(excluded.forklaring, to_trinnskontroll.forklaring)
        """.trimIndent()

        val params = mapOf(
            "entity_id" to entityId,
            "opprettet_av" to opprettetAv.value,
            "opprettet_tidspunkt" to LocalDateTime.now(),
            "aarsaker" to session.createTextArray(aarsaker),
            "forklaring" to forklaring,
            "type" to type.name,
        )

        session.execute(queryOf(query, params))
    }

    fun beslutt(
        entityId: UUID,
        besluttetAv: NavIdent,
        aarsaker: List<String>?,
        forklaring: String?,
        besluttelse: Besluttelse,
        type: ToTrinnskontrollType,
    ) {
        @Language("PostgreSQL")
        val query = """
        update to_trinnskontroll set
            besluttet_av = :besluttet_av,
            besluttelse = :besluttelse::besluttelse,
            aarsaker = coalesce(:aarsaker, aarsaker),
            forklaring = coalesce(:forklaring, forklaring)
        where
            entity_id = :entity_id::uuid
            and type = :type::to_trinnskontroll_type
        """.trimIndent()

        val params = mapOf(
            "entity_id" to entityId,
            "besluttet_av" to besluttetAv.value,
            "aarsaker" to aarsaker?.let { session.createTextArray(it) }, // Keeps it nullable
            "forklaring" to forklaring,
            "besluttelse" to besluttelse.name,
            "type" to type.name,
        )

        session.execute(queryOf(query, params))
    }

    fun get(entityId: UUID, type: ToTrinnskontrollType): ToTrinnskontroll? {
        @Language("PostgreSQL")
        val query = """
            select * from to_trinnskontroll
            where entity_id = :entity_id and type = :type::to_trinnskontroll_type
        """.trimIndent()

        val params = mapOf(
            "entity_id" to entityId,
            "type" to type.name,
        )

        return session.single(queryOf(query, params)) { it.toToTrinnskontroll() }
    }

    private fun Row.toToTrinnskontroll(): ToTrinnskontroll {
        return when (val besluttetAv = stringOrNull("besluttet_av")) {
            null -> ToTrinnskontroll.Ubesluttet(
                opprettetAv = NavIdent(string("opprettet_av")),
                opprettetTidspunkt = localDateTime("opprettet_tidspunkt"),
                aarsaker = array<String>("aarsaker").toList(),
                forklaring = stringOrNull("forklaring"),
            )
            else -> ToTrinnskontroll.Besluttet(
                opprettetAv = NavIdent(string("opprettet_av")),
                opprettetTidspunkt = localDateTime("opprettet_tidspunkt"),
                besluttetAv = NavIdent(besluttetAv),
                besluttetTidspunkt = localDateTime("opprettet_tidspunkt"),
                besluttelse = Besluttelse.valueOf(string("besluttelse")),
                aarsaker = array<String>("aarsaker").toList(),
                forklaring = stringOrNull("forklaring"),
            )
        }
    }
}
