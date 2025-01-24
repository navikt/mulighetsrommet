package no.nav.mulighetsrommet.api.refusjon.db

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.domain.dto.amt.Melding
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.intellij.lang.annotations.Language
import java.util.*

class DeltakerForslagQueries(private val session: Session) {

    fun upsert(forslag: DeltakerForslag) = with(session) {
        @Language("PostgreSQL")
        val query = """
            insert into deltaker_forslag (
                id,
                deltaker_id,
                endring,
                status
            ) values (
                :id::uuid,
                :deltaker_id::uuid,
                :endring::jsonb,
                :status::deltaker_forslag_status
            ) on conflict (id) do update set
                deltaker_id = excluded.deltaker_id,
                endring     = excluded.endring,
                status      = excluded.status
        """.trimIndent()

        val params = mapOf(
            "id" to forslag.id,
            "deltaker_id" to forslag.deltakerId,
            "endring" to Json.encodeToString(forslag.endring),
            "status" to forslag.status.name,
        )

        execute(queryOf(query, params))
    }

    fun delete(id: UUID) = with(session) {
        @Language("PostgreSQL")
        val query = """
            delete from deltaker_forslag
            where id = ?::uuid
        """.trimIndent()

        execute(queryOf(query, id))
    }

    fun getForslagByGjennomforing(gjennomforingId: UUID): Map<UUID, List<DeltakerForslag>> = with(session) {
        @Language("PostgreSQL")
        val query = """
        select
            deltaker.id as deltaker_id,
            deltaker_forslag.id,
            deltaker_forslag.endring,
            deltaker_forslag.status
        from deltaker
        inner join deltaker_forslag on deltaker.id = deltaker_forslag.deltaker_id
        where deltaker.gjennomforing_id = ?::uuid
        """.trimIndent()

        return list(queryOf(query, gjennomforingId)) { it.toForslagDbo() }
            .groupBy { it.deltakerId }
    }
}

private fun Row.toForslagDbo(): DeltakerForslag {
    val endring = string("endring")
        .let { Json.decodeFromString<Melding.Forslag.Endring>(it) }

    return DeltakerForslag(
        id = uuid("id"),
        deltakerId = uuid("deltaker_id"),
        endring = endring,
        status = DeltakerForslag.Status.valueOf(string("status")),
    )
}

@Serializable
data class DeltakerForslag(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val deltakerId: UUID,
    val endring: Melding.Forslag.Endring,
    val status: Status,
) {
    enum class Status {
        GODKJENT,
        AVVIST,
        TILBAKEKALT,
        ERSTATTET,
        VENTER_PA_SVAR,
    }
}
