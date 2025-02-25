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
    fun behandler(
        entityId: UUID,
        navIdent: NavIdent,
        aarsaker: List<String>?,
        forklaring: String?,
        type: TotrinnskontrollType,
        tidspunkt: LocalDateTime,
    ) {
        @Language("PostgreSQL")
        val query = """
            insert into totrinnskontroll (
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
                :entity_id::uuid,
                :behandlet_av,
                :behandlet_tidspunkt,
                coalesce(:aarsaker, array[]::text[]),
                :forklaring,
                :type::totrinnskontroll_type,
                null,
                null,
                null
            ) on conflict (entity_id, type) do update set
                behandlet_av        = excluded.behandlet_av,
                behandlet_tidspunkt = excluded.behandlet_tidspunkt,
                aarsaker            = coalesce(excluded.aarsaker, totrinnskontroll.aarsaker, array[]::text[]),
                forklaring          = coalesce(excluded.forklaring, totrinnskontroll.forklaring),
                besluttet_av        = null,
                besluttet_tidspunkt = null,
                besluttelse         = null
        """.trimIndent()

        val params = mapOf(
            "entity_id" to entityId,
            "behandlet_av" to navIdent.value,
            "behandlet_tidspunkt" to tidspunkt,
            "aarsaker" to aarsaker?.let { session.createTextArray(it) },
            "forklaring" to forklaring,
            "type" to type.name,
        )

        session.execute(queryOf(query, params))
    }

    fun beslutter(
        entityId: UUID,
        navIdent: NavIdent,
        aarsaker: List<String>?,
        forklaring: String?,
        besluttelse: Besluttelse,
        type: TotrinnskontrollType,
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
        where
            entity_id = :entity_id::uuid
            and type = :type::totrinnskontroll_type
        """.trimIndent()

        val params = mapOf(
            "entity_id" to entityId,
            "besluttet_av" to navIdent.value,
            "aarsaker" to aarsaker?.let { session.createTextArray(it) }, // Keeps it nullable
            "forklaring" to forklaring,
            "besluttelse" to besluttelse.name,
            "type" to type.name,
            "tidspunkt" to tidspunkt,
        )

        session.execute(queryOf(query, params))
    }

    fun get(entityId: UUID, type: TotrinnskontrollType): Totrinnskontroll? {
        @Language("PostgreSQL")
        val query = """
            select * from totrinnskontroll
            where entity_id = :entity_id::uuid and type = :type::totrinnskontroll_type
        """.trimIndent()

        val params = mapOf(
            "entity_id" to entityId,
            "type" to type.name,
        )

        return session.single(queryOf(query, params)) { it.toToTrinnskontroll() }
    }

    private fun Row.toToTrinnskontroll(): Totrinnskontroll {
        return when (val besluttetAv = stringOrNull("besluttet_av")) {
            null -> Totrinnskontroll.Ubesluttet(
                behandletAv = NavIdent(string("behandlet_av")),
                behandletTidspunkt = localDateTime("behandlet_tidspunkt"),
                aarsaker = array<String>("aarsaker").toList(),
                forklaring = stringOrNull("forklaring"),
            )
            else -> Totrinnskontroll.Besluttet(
                behandletAv = NavIdent(string("behandlet_av")),
                behandletTidspunkt = localDateTime("behandlet_tidspunkt"),
                besluttetAv = NavIdent(besluttetAv),
                besluttetTidspunkt = localDateTime("behandlet_tidspunkt"),
                besluttelse = Besluttelse.valueOf(string("besluttelse")),
                aarsaker = array<String>("aarsaker").toList(),
                forklaring = stringOrNull("forklaring"),
            )
        }
    }
}
