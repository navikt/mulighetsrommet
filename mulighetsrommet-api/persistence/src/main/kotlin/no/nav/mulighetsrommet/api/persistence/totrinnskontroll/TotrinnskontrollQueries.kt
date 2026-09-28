package no.nav.mulighetsrommet.api.persistence.totrinnskontroll

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.admin.totrinnskontroll.AgentDto
import no.nav.mulighetsrommet.admin.totrinnskontroll.BehandlingDto
import no.nav.mulighetsrommet.admin.totrinnskontroll.BeslutningDto
import no.nav.mulighetsrommet.admin.totrinnskontroll.TotrinnskontrollDto
import no.nav.mulighetsrommet.admin.totrinnskontroll.TotrinnskontrollQueryHandler
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.Totrinnskontroll
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollStatus
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.database.requireSingle
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
                behandlet_begrunnelse,
                behandlet_aarsaker,
                type,
                besluttet_av,
                besluttet_tidspunkt,
                besluttet_begrunnelse,
                besluttet_aarsaker,
                status
            ) values (
                :id::uuid,
                :entity_id::uuid,
                :behandlet_av,
                :behandlet_tidspunkt,
                :behandlet_begrunnelse,
                :behandlet_aarsaker,
                :type,
                :besluttet_av,
                :besluttet_tidspunkt,
                :besluttet_begrunnelse,
                :besluttet_aarsaker,
                :status
            ) on conflict (id) do update set
                behandlet_av = excluded.behandlet_av,
                behandlet_tidspunkt = excluded.behandlet_tidspunkt,
                behandlet_begrunnelse = excluded.behandlet_begrunnelse,
                behandlet_aarsaker = excluded.behandlet_aarsaker,
                type = excluded.type,
                besluttet_av = excluded.besluttet_av,
                besluttet_tidspunkt = excluded.besluttet_tidspunkt,
                besluttet_begrunnelse = excluded.besluttet_begrunnelse,
                besluttet_aarsaker = excluded.besluttet_aarsaker,
                status = excluded.status
        """.trimIndent()

        val params = mapOf(
            "id" to totrinnskontroll.id,
            "entity_id" to totrinnskontroll.entityId,
            "type" to totrinnskontroll.type.name,
            "status" to totrinnskontroll.status.name,
            "behandlet_av" to totrinnskontroll.behandling.utfortAv.textRepr(),
            "behandlet_tidspunkt" to totrinnskontroll.behandling.tidspunkt,
            "behandlet_begrunnelse" to totrinnskontroll.behandling.begrunnelse,
            "behandlet_aarsaker" to totrinnskontroll.behandling.aarsaker.let { session.createTextArray(it) },
            "besluttet_av" to totrinnskontroll.beslutning?.utfortAv?.textRepr(),
            "besluttet_tidspunkt" to totrinnskontroll.beslutning?.tidspunkt,
            "besluttet_begrunnelse" to totrinnskontroll.beslutning?.begrunnelse,
            "besluttet_aarsaker" to totrinnskontroll.beslutning?.aarsaker.orEmpty().let { session.createTextArray(it) },
        )

        session.execute(queryOf(query, params))
    }

    fun getById(id: UUID): Totrinnskontroll {
        @Language("PostgreSQL")
        val query = """
            select *
            from totrinnskontroll
            where id = ?::uuid
        """.trimIndent()

        return session.requireSingle(queryOf(query, id)) { it.toTotrinnskontroll() }
    }

    fun findById(id: UUID): Totrinnskontroll? {
        @Language("PostgreSQL")
        val query = """
            select *
            from totrinnskontroll
            where id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, id)) { it.toTotrinnskontroll() }
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

    override fun getDtoByIdOrError(id: UUID): TotrinnskontrollDto {
        return requireNotNull(getDtoById(id)) {
            "Totrinnskontroll mangler for $id"
        }
    }

    override fun getDtoById(id: UUID): TotrinnskontrollDto? {
        @Language("PostgreSQL")
        val query = """
            select
                totrinnskontroll.*,
                nav_ansatt_behandlet.fornavn || ' ' || nav_ansatt_behandlet.etternavn AS behandlet_av_navn,
                nav_ansatt_besluttet.fornavn || ' ' || nav_ansatt_besluttet.etternavn AS besluttet_av_navn
            from totrinnskontroll
                left join nav_ansatt nav_ansatt_behandlet on behandlet_av = nav_ansatt_behandlet.nav_ident
                left join nav_ansatt nav_ansatt_besluttet on besluttet_av = nav_ansatt_besluttet.nav_ident
            where id = :id::uuid
            order by behandlet_tidspunkt desc
            limit 1
        """.trimIndent()

        val params = mapOf(
            "id" to id,
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

    fun Row.toTotrinnskontroll(): Totrinnskontroll {
        return Totrinnskontroll(
            id = uuid("id"),
            entityId = uuid("entity_id"),
            type = TotrinnskontrollType.valueOf(string("type")),
            behandling = Totrinnskontroll.Behandling(
                utfortAv = string("behandlet_av").toAgent(),
                tidspunkt = instant("behandlet_tidspunkt"),
                begrunnelse = stringOrNull("behandlet_begrunnelse"),
                aarsaker = array<String>("behandlet_aarsaker").toList(),
            ),
            beslutning = stringOrNull("besluttet_av")?.let {
                Totrinnskontroll.Beslutning(
                    utfortAv = it.toAgent(),
                    tidspunkt = instant("besluttet_tidspunkt"),
                    begrunnelse = stringOrNull("besluttet_begrunnelse"),
                    aarsaker = array<String>("besluttet_aarsaker").toList(),
                )
            },
            status = string("status").let { TotrinnskontrollStatus.valueOf(it) },
        )
    }

    private fun Row.toDto(): TotrinnskontrollDto {
        val id = uuid("id")
        val behandletAv = string("behandlet_av").toAgent()
        val behandletAvNavn = stringOrNull("behandlet_av_navn")
        val behandletTidspunkt = localDateTime("behandlet_tidspunkt")
        val behandletBegrunnelse = stringOrNull("behandlet_begrunnelse")
        val behandletAarsaker = array<String>("behandlet_aarsaker").toList()
        val besluttetBegrunnelse = stringOrNull("besluttet_begrunnelse")
        val besluttetAarsaker = array<String>("besluttet_aarsaker").toList()
        val status = string("status").let { TotrinnskontrollStatus.valueOf(it) }

        return if (status == TotrinnskontrollStatus.TIL_BEHANDLING) {
            TotrinnskontrollDto.TilBeslutning(
                id = id,
                behandling = BehandlingDto(
                    utfortAv = AgentDto.fromAgent(behandletAv, behandletAvNavn),
                    tidspunkt = behandletTidspunkt,
                    begrunnelse = behandletBegrunnelse,
                    aarsaker = behandletAarsaker,
                ),
            )
        } else {
            val besluttetAv = string("besluttet_av").toAgent()
            val besluttetAvNavn = stringOrNull("besluttet_av_navn")
            TotrinnskontrollDto.Besluttet(
                id = id,
                behandling = BehandlingDto(
                    utfortAv = AgentDto.fromAgent(behandletAv, behandletAvNavn),
                    tidspunkt = behandletTidspunkt,
                    begrunnelse = behandletBegrunnelse,
                    aarsaker = behandletAarsaker,
                ),
                beslutning = BeslutningDto(
                    utfortAv = AgentDto.fromAgent(besluttetAv, besluttetAvNavn),
                    tidspunkt = localDateTime("besluttet_tidspunkt"),
                    begrunnelse = besluttetBegrunnelse,
                    aarsaker = besluttetAarsaker,
                    utfall = when (status) {
                        TotrinnskontrollStatus.TIL_BEHANDLING -> error("Status TIL_BEHANDLING kan ikke mappes til TotrinnskontrollDto.Besluttet")
                        TotrinnskontrollStatus.SATT_PA_VENT -> TotrinnskontrollDto.Utfall.SATT_PA_VENT
                        TotrinnskontrollStatus.GODKJENT -> TotrinnskontrollDto.Utfall.GODKJENT
                        TotrinnskontrollStatus.RETURNERT -> TotrinnskontrollDto.Utfall.RETURNERT
                    },
                ),
            )
        }
    }
}
