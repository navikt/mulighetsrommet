package no.nav.mulighetsrommet.api.totrinnskontroll.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.model.NavIdent
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.*

enum class TotrinnskontrollType {
    OPPRETT,
    ANNULLER,
}

class TotrinnskontrollQueries(private val session: Session) {
    fun upsert(totrinnskontroll: Totrinnskontroll) {
        @Language("PostgreSQL")
        val query = """
            insert into totrinnskontroll (
                id,
                entity_id,
                behandlet_av,
                behandlet_tidspunkt,
                aarsaker,
                forklaring,
                type,
                besluttet_av,
                besluttet_tidspunkt,
                besluttelse
            ) values (
                :id::uuid,
                :entity_id::uuid,
                :behandlet_av,
                :behandlet_tidspunkt,
                :aarsaker,
                :forklaring,
                :type::totrinnskontroll_type,
                :besluttet_av,
                :besluttet_tidspunkt,
                :besluttelse::besluttelse
            ) on conflict (id) do update set
                behandlet_av = excluded.behandlet_av,
                behandlet_tidspunkt = excluded.behandlet_tidspunkt,
                aarsaker = excluded.aarsaker,
                forklaring = excluded.forklaring,
                type = excluded.type,
                besluttet_av = excluded.besluttet_av,
                besluttet_tidspunkt = excluded.besluttet_tidspunkt,
                besluttelse = excluded.besluttelse
        """.trimIndent()

        val params = mapOf(
            "id" to totrinnskontroll.id,
            "entity_id" to totrinnskontroll.entityId,
            "type" to totrinnskontroll.type.name,
            "behandlet_av" to totrinnskontroll.behandletAv.value,
            "behandlet_tidspunkt" to totrinnskontroll.behandletTidspunkt,
            "besluttet_av" to totrinnskontroll.besluttetAv?.value,
            "besluttet_tidspunkt" to totrinnskontroll.besluttetTidspunkt,
            "besluttelse" to totrinnskontroll.besluttelse?.name,
            "aarsaker" to totrinnskontroll.aarsaker.let { session.createTextArray(it) },
            "forklaring" to totrinnskontroll.forklaring,
        )

        session.execute(queryOf(query, params))
    }

    fun beslutter(
        id: Int,
        navIdent: NavIdent,
        aarsaker: List<String>?,
        forklaring: String?,
        besluttelse: Besluttelse,
        tidspunkt: LocalDateTime,
    ) {
        @Language("PostgreSQL")
        val query = """
        update totrinnskontroll set
            besluttet_av = :besluttet_av,
            besluttelse = :besluttelse::besluttelse,
            besluttet_tidspunkt = :tidspunkt,
            aarsaker = coalesce(:aarsaker, aarsaker),
            forklaring = coalesce(:forklaring, forklaring)
        where id = :id
        """.trimIndent()

        val params = mapOf(
            "id" to id,
            "besluttet_av" to navIdent.value,
            "aarsaker" to aarsaker?.let { session.createTextArray(it) }, // Keeps it nullable
            "forklaring" to forklaring,
            "besluttelse" to besluttelse.name,
            "tidspunkt" to tidspunkt,
        )

        session.execute(queryOf(query, params))
    }

    fun get(entityId: UUID, type: TotrinnskontrollType): Totrinnskontroll? {
        @Language("PostgreSQL")
        val query = """
            select * from totrinnskontroll
            where entity_id = :entity_id::uuid and type = :type::totrinnskontroll_type
            order by behandlet_tidspunkt desc limit 1
        """.trimIndent()

        val params = mapOf(
            "entity_id" to entityId,
            "type" to type.name,
        )

        return session.single(queryOf(query, params)) { it.toToTrinnskontroll() }
    }

    private fun Row.toToTrinnskontroll(): Totrinnskontroll {
        return Totrinnskontroll(
            id = uuid("id"),
            entityId = uuid("entity_id"),
            type = TotrinnskontrollType.valueOf(string("type")),
            behandletAv = NavIdent(string("behandlet_av")),
            behandletTidspunkt = localDateTime("behandlet_tidspunkt"),
            aarsaker = array<String>("aarsaker").toList(),
            forklaring = stringOrNull("forklaring"),
            besluttetAv = stringOrNull("besluttet_av")?.let { NavIdent(it) },
            besluttetTidspunkt = localDateTimeOrNull("besluttet_tidspunkt"),
            besluttelse = stringOrNull("besluttelse")?.let { Besluttelse.valueOf(it) },
        )
    }
}
