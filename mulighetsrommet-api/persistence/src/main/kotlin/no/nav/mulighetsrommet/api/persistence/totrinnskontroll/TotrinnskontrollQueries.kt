package no.nav.mulighetsrommet.api.persistence.totrinnskontroll

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.admin.totrinnskontroll.AgentDto
import no.nav.mulighetsrommet.admin.totrinnskontroll.TotrinnskontrollDto
import no.nav.mulighetsrommet.admin.totrinnskontroll.TotrinnskontrollQueryHandler
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.Totrinnskontroll
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollStatus
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.model.textRepr
import no.nav.mulighetsrommet.model.toAgent
import org.intellij.lang.annotations.Language
import java.util.UUID

class TotrinnskontrollQueries(val session: Session) : TotrinnskontrollQueryHandler {
    override fun upsert(totrinnskontroll: Totrinnskontroll) {
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
                status
            ) values (
                :id::uuid,
                :entity_id::uuid,
                :behandlet_av,
                :behandlet_tidspunkt,
                :aarsaker,
                :forklaring,
                :type,
                :besluttet_av,
                :besluttet_tidspunkt,
                :status
            ) on conflict (id) do update set
                behandlet_av = excluded.behandlet_av,
                behandlet_tidspunkt = excluded.behandlet_tidspunkt,
                aarsaker = excluded.aarsaker,
                forklaring = excluded.forklaring,
                type = excluded.type,
                besluttet_av = excluded.besluttet_av,
                besluttet_tidspunkt = excluded.besluttet_tidspunkt,
                status = excluded.status
        """.trimIndent()

        val params = mapOf(
            "id" to totrinnskontroll.id,
            "entity_id" to totrinnskontroll.entityId,
            "type" to totrinnskontroll.type.name,
            "status" to totrinnskontroll.status.name,
            "behandlet_av" to totrinnskontroll.behandletAv.textRepr(),
            "behandlet_tidspunkt" to totrinnskontroll.behandletTidspunkt,
            "besluttet_av" to totrinnskontroll.besluttetAv?.textRepr(),
            "besluttet_tidspunkt" to totrinnskontroll.besluttetTidspunkt,
            "aarsaker" to totrinnskontroll.aarsaker.let { session.createTextArray(it) },
            "forklaring" to totrinnskontroll.forklaring,
        )

        session.execute(queryOf(query, params))
    }

    override fun getOrError(entityId: UUID, type: TotrinnskontrollType): Totrinnskontroll {
        return requireNotNull(get(entityId, type)) {
            "Totrinnskontroll mangler for type $type"
        }
    }

    override fun get(entityId: UUID, type: TotrinnskontrollType): Totrinnskontroll? {
        @Language("PostgreSQL")
        val query = """
            select *
            from totrinnskontroll
            where entity_id = :entity_id::uuid and type = :type
            order by behandlet_tidspunkt desc
            limit 1
        """.trimIndent()

        val params = mapOf(
            "entity_id" to entityId,
            "type" to type.name,
        )

        return session.single(queryOf(query, params)) { it.toTotrinnskontroll() }
    }

    override fun getDtoOrError(entityId: UUID, type: TotrinnskontrollType): TotrinnskontrollDto {
        return requireNotNull(getDto(entityId, type)) {
            "Totrinnskontroll mangler for type $type"
        }
    }

    override fun getDto(entityId: UUID, type: TotrinnskontrollType): TotrinnskontrollDto? {
        @Language("PostgreSQL")
        val query = """
            select
                totrinnskontroll.*,
                nav_ansatt_behandlet.fornavn || ' ' || nav_ansatt_behandlet.etternavn AS behandlet_av_navn,
                nav_ansatt_besluttet.fornavn || ' ' || nav_ansatt_besluttet.etternavn AS besluttet_av_navn
            from totrinnskontroll
                left join nav_ansatt nav_ansatt_behandlet on behandlet_av = nav_ansatt_behandlet.nav_ident
                left join nav_ansatt nav_ansatt_besluttet on besluttet_av = nav_ansatt_besluttet.nav_ident
            where entity_id = :entity_id::uuid and type = :type
            order by behandlet_tidspunkt desc
            limit 1
        """.trimIndent()

        val params = mapOf(
            "entity_id" to entityId,
            "type" to type.name,
        )

        return session.single(queryOf(query, params)) { it.toDto() }
    }

    fun getAll(entityId: UUID): List<Totrinnskontroll> {
        @Language("PostgreSQL")
        val query = """
            select totrinnskontroll.*
            from totrinnskontroll
            where entity_id = :entity_id::uuid
            order by behandlet_tidspunkt desc
        """.trimIndent()

        val params = mapOf(
            "entity_id" to entityId,
        )

        return session.list(queryOf(query, params)) { it.toTotrinnskontroll() }
    }

    private fun Row.toTotrinnskontroll(): Totrinnskontroll {
        return Totrinnskontroll(
            id = uuid("id"),
            entityId = uuid("entity_id"),
            type = TotrinnskontrollType.valueOf(string("type")),
            behandletAv = string("behandlet_av").toAgent(),
            behandletTidspunkt = instant("behandlet_tidspunkt"),
            aarsaker = array<String>("aarsaker").toList(),
            forklaring = stringOrNull("forklaring"),
            besluttetAv = stringOrNull("besluttet_av")?.toAgent(),
            besluttetTidspunkt = instantOrNull("besluttet_tidspunkt"),
            status = string("status").let { TotrinnskontrollStatus.valueOf(it) },
        )
    }

    private fun Row.toDto(): TotrinnskontrollDto {
        val behandletAv = string("behandlet_av").toAgent()
        val behandletAvNavn = stringOrNull("behandlet_av_navn")
        val behandletTidspunkt = localDateTime("behandlet_tidspunkt")
        val aarsaker = array<String>("aarsaker").toList()
        val forklaring = stringOrNull("forklaring")
        val status = string("status").let { TotrinnskontrollStatus.valueOf(it) }

        return if (status == TotrinnskontrollStatus.TIL_BEHANDLING) {
            TotrinnskontrollDto.TilBeslutning(
                behandletAv = AgentDto.fromAgent(behandletAv, behandletAvNavn),
                behandletTidspunkt = behandletTidspunkt,
                aarsaker = aarsaker,
                forklaring = forklaring,
            )
        } else {
            val besluttetAv = string("besluttet_av").toAgent()
            val besluttetAvNavn = stringOrNull("besluttet_av_navn")
            TotrinnskontrollDto.Besluttet(
                behandletAv = AgentDto.fromAgent(behandletAv, behandletAvNavn),
                behandletTidspunkt = behandletTidspunkt,
                aarsaker = aarsaker,
                forklaring = forklaring,
                besluttetAv = AgentDto.fromAgent(besluttetAv, besluttetAvNavn),
                besluttetTidspunkt = localDateTime("besluttet_tidspunkt"),
                beslutning = when (status) {
                    TotrinnskontrollStatus.TIL_BEHANDLING -> error("Status TIL_BEHANDLING kan ikke mappes til TotrinnskontrollDto.Besluttet")
                    TotrinnskontrollStatus.SATT_PA_VENT -> TotrinnskontrollDto.Beslutning.SATT_PA_VENT
                    TotrinnskontrollStatus.GODKJENT -> TotrinnskontrollDto.Beslutning.GODKJENT
                    TotrinnskontrollStatus.RETURNERT -> TotrinnskontrollDto.Beslutning.RETURNERT
                },
            )
        }
    }
}
