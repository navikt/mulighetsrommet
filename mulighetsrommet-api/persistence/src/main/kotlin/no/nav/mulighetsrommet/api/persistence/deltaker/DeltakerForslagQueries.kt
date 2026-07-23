package no.nav.mulighetsrommet.api.persistence.deltaker

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.deltaker.DeltakerForslag
import no.nav.mulighetsrommet.api.domain.deltaker.DeltakerForslagRepository
import org.intellij.lang.annotations.Language
import java.util.UUID

class DeltakerForslagQueries(private val session: Session) : DeltakerForslagRepository {

    override fun save(forslag: DeltakerForslag): Unit = with(session) {
        @Language("PostgreSQL")
        val query = """
            insert into deltaker_forslag (
                id,
                deltaker_id,
                gjennomforing_id,
                endring,
                status
            ) values (
                :id::uuid,
                :deltaker_id::uuid,
                :gjennomforing_id::uuid,
                :endring::jsonb,
                :status::deltaker_forslag_status
            ) on conflict (id) do update set
                deltaker_id      = excluded.deltaker_id,
                gjennomforing_id = excluded.gjennomforing_id,
                endring          = excluded.endring,
                status           = excluded.status
        """.trimIndent()

        val params = mapOf(
            "id" to forslag.id,
            "deltaker_id" to forslag.deltakerId,
            "gjennomforing_id" to forslag.gjennomforingId,
            "endring" to Json.encodeToString(forslag.endring),
            "status" to forslag.status.name,
        )

        execute(queryOf(query, params))
    }

    override fun delete(id: UUID): Unit = with(session) {
        @Language("PostgreSQL")
        val query = """
            delete from deltaker_forslag
            where id = ?::uuid
        """.trimIndent()

        execute(queryOf(query, id))
    }

    override fun getByGjennomforing(gjennomforingId: UUID): Map<UUID, List<DeltakerForslag>> = with(session) {
        @Language("PostgreSQL")
        val query = """
        select
            id,
            deltaker_id,
            gjennomforing_id,
            endring,
            status
        from deltaker_forslag
        where gjennomforing_id = ?::uuid
        """.trimIndent()

        return list(queryOf(query, gjennomforingId)) { it.toForslag() }
            .groupBy { it.deltakerId }
    }

    override fun get(id: UUID): DeltakerForslag? = with(session) {
        @Language("PostgreSQL")
        val query = """
        select
            id,
            deltaker_id,
            gjennomforing_id,
            endring,
            status
        from deltaker_forslag
        where id = ?::uuid
        """.trimIndent()

        return single(queryOf(query, id)) { it.toForslag() }
    }
}

private fun Row.toForslag(): DeltakerForslag {
    val endring = string("endring")
        .let { Json.decodeFromString<DeltakerForslag.Endring>(it) }

    return DeltakerForslag.fraLagring(
        id = uuid("id"),
        deltakerId = uuid("deltaker_id"),
        gjennomforingId = uuid("gjennomforing_id"),
        endring = endring,
        status = DeltakerForslag.Status.valueOf(string("status")),
    )
}
