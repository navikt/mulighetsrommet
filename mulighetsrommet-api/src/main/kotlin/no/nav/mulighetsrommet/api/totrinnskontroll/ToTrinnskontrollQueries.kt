package no.nav.mulighetsrommet.api.totrinnskontroll

import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.model.NavIdent
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.*

enum class ToTrinnskontrollType {
    OPPRETT_TILSAGN,
    ANNULLER_TILSAGN,
    OPPRETT_UTBETALING,
}

class ToTrinnskontrollQueries(private val session: Session) {
    fun upsert(
        entityId: UUID,
        opprettetAv: NavIdent,
        type: ToTrinnskontrollType,
        tidspunkt: LocalDateTime,
        aarsaker: List<String>,
        forklaring: String?,
    ) {
        @Language("PostgreSQL")
        val query = """
            insert into to_trinnskontroll (
                entity_id,
                type,
                opprettet_av,
                opprettet_tidspunkt,
                aarsaker,
                forklaring
            ) values (
                :entity_id::uuid,
                :type::to_trinnskontroll_type,
                :opprettet_av,
                :opprettet_tidspunkt,
                :aarsaker,
                :forklaring
            )
        """.trimIndent()

        val params = mapOf(
            "entity_id" to entityId,
            "opprettet_av" to opprettetAv.value,
            "opprettet_tidspunkt" to tidspunkt,
            "type" to type.name,
            "aarsaker" to session.createTextArray(aarsaker),
            "forklaring" to forklaring,
        )

        session.execute(queryOf(query, params))
    }

    fun godkjenn(
        entityId: UUID,
        type: ToTrinnskontrollType,
        besluttetAv: NavIdent,
        tidspunkt: LocalDateTime,
    ) {
        return beslutt(
            entityId = entityId,
            type = type,
            besluttetAv = besluttetAv,
            tidspunkt = tidspunkt,
            besluttelse = Besluttelse.GODKJENT,
            aarsaker = null,
            forklaring = null,
        )
    }

    fun avvis(
        entityId: UUID,
        type: ToTrinnskontrollType,
        besluttetAv: NavIdent,
        tidspunkt: LocalDateTime,
        aarsaker: List<String>?,
        forklaring: String?,
    ) {
        return beslutt(
            entityId = entityId,
            type = type,
            besluttetAv = besluttetAv,
            tidspunkt = tidspunkt,
            besluttelse = Besluttelse.AVVIST,
            aarsaker = aarsaker,
            forklaring = forklaring,
        )
    }

    private fun beslutt(
        entityId: UUID,
        type: ToTrinnskontrollType,
        besluttetAv: NavIdent,
        tidspunkt: LocalDateTime,
        besluttelse: Besluttelse,
        aarsaker: List<String>?,
        forklaring: String?,
    ) {
        @Language("PostgreSQL")
        val query = """
            update to_trinnskontroll set
                besluttet_av = :nav_ident,
                besluttet_tidspunkt = :tidspunkt,
                besluttelse = :besluttelse::besluttelse,
                aarsaker = coalesce(:aarsaker, aarsaker),
                forklaring = coalesce(:forklaring, forklaring)
            where
                entity_id = :entity_id::uuid and
                type = :type::to_trinnskontroll_type
        """.trimIndent()

        val params = mapOf(
            "entity_id" to entityId,
            "type" to type.name,
            "nav_ident" to besluttetAv.value,
            "tidspunkt" to tidspunkt,
            "besluttelse" to besluttelse.name,
            "aarsaker" to (aarsaker?.let { session.createTextArray(it) }),
            "forklaring" to forklaring,
        )

        session.execute(queryOf(query, params))
    }
}
