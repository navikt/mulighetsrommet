package no.nav.mulighetsrommet.api.totrinnskontroll.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.model.textRepr
import no.nav.mulighetsrommet.model.toAgent
import org.intellij.lang.annotations.Language
import java.util.*

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
            "behandlet_av" to totrinnskontroll.behandletAv.textRepr(),
            "behandlet_tidspunkt" to totrinnskontroll.behandletTidspunkt,
            "besluttet_av" to totrinnskontroll.besluttetAv?.textRepr(),
            "besluttet_tidspunkt" to totrinnskontroll.besluttetTidspunkt,
            "besluttelse" to totrinnskontroll.besluttelse?.name,
            "aarsaker" to totrinnskontroll.aarsaker.let { session.createTextArray(it) },
            "forklaring" to totrinnskontroll.forklaring,
        )

        session.execute(queryOf(query, params))
    }

    fun getOrError(entityId: UUID, type: Totrinnskontroll.Type): Totrinnskontroll {
        return requireNotNull(get(entityId, type)) {
            "Totrinnskontroll mangler for type $type"
        }
    }

    fun get(entityId: UUID, type: Totrinnskontroll.Type): Totrinnskontroll? {
        @Language("PostgreSQL")
        val query = """
            select
                totrinnskontroll.*,
                nav_ansatt_behandlet.fornavn || ' ' || nav_ansatt_behandlet.etternavn AS behandlet_av_navn,
                nav_ansatt_besluttet.fornavn || ' ' || nav_ansatt_besluttet.etternavn AS besluttet_av_navn
            from totrinnskontroll
                left join nav_ansatt nav_ansatt_behandlet on behandlet_av = nav_ansatt_behandlet.nav_ident
                left join nav_ansatt nav_ansatt_besluttet on besluttet_av = nav_ansatt_besluttet.nav_ident
            where entity_id = :entity_id::uuid and type = :type::totrinnskontroll_type
            order by behandlet_tidspunkt desc
            limit 1
        """.trimIndent()

        val params = mapOf(
            "entity_id" to entityId,
            "type" to type.name,
        )

        return session.single(queryOf(query, params)) { it.toToTrinnskontroll() }
    }

    fun getAll(entityId: UUID): List<Totrinnskontroll> {
        @Language("PostgreSQL")
        val query = """
            select
                totrinnskontroll.*,
                nav_ansatt_behandlet.fornavn || ' ' || nav_ansatt_behandlet.etternavn AS behandlet_av_navn,
                nav_ansatt_besluttet.fornavn || ' ' || nav_ansatt_besluttet.etternavn AS besluttet_av_navn
            from totrinnskontroll
                left join nav_ansatt nav_ansatt_behandlet on behandlet_av = nav_ansatt_behandlet.nav_ident
                left join nav_ansatt nav_ansatt_besluttet on besluttet_av = nav_ansatt_besluttet.nav_ident
            where entity_id = :entity_id::uuid
            order by behandlet_tidspunkt desc
        """.trimIndent()

        val params = mapOf(
            "entity_id" to entityId,
        )

        return session.list(queryOf(query, params)) { it.toToTrinnskontroll() }
    }

    private fun Row.toToTrinnskontroll(): Totrinnskontroll {
        return Totrinnskontroll(
            id = uuid("id"),
            entityId = uuid("entity_id"),
            type = Totrinnskontroll.Type.valueOf(string("type")),
            behandletAv = string("behandlet_av").toAgent(),
            behandletTidspunkt = localDateTime("behandlet_tidspunkt"),
            aarsaker = array<String>("aarsaker").toList(),
            forklaring = stringOrNull("forklaring"),
            besluttetAv = stringOrNull("besluttet_av")?.toAgent(),
            besluttetTidspunkt = localDateTimeOrNull("besluttet_tidspunkt"),
            besluttelse = stringOrNull("besluttelse")?.let { Besluttelse.valueOf(it) },
            besluttetAvNavn = stringOrNull("besluttet_av_navn"),
            behandletAvNavn = stringOrNull("behandlet_av_navn"),
        )
    }
}
