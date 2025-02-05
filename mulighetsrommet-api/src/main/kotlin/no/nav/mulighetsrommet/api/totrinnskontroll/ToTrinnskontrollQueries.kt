package no.nav.mulighetsrommet.api.totrinnskontroll

import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.model.NavIdent
import org.intellij.lang.annotations.Language
import java.util.*

enum class ToTrinnskontrollHandling {
    FORESLA_OPPRETT,
    FORESLA_ANNULLER,
    GODKJENN,
    AVVIS,
}

class ToTrinnskontrollQueries(private val session: Session) {
    fun foreslaOpprett(
        entityId: UUID,
        opprettetAv: NavIdent,
        aarsaker: List<String>,
        forklaring: String?,
    ) {
        return insert(entityId, opprettetAv, ToTrinnskontrollHandling.FORESLA_OPPRETT, aarsaker, forklaring)
    }

    fun foreslaAnnuller(
        entityId: UUID,
        opprettetAv: NavIdent,
        aarsaker: List<String>,
        forklaring: String?,
    ) {
        return insert(entityId, opprettetAv, ToTrinnskontrollHandling.FORESLA_ANNULLER, aarsaker, forklaring)
    }

    fun godkjenn(
        entityId: UUID,
        opprettetAv: NavIdent,
        aarsaker: List<String>,
        forklaring: String?,
    ) {
        return insert(entityId, opprettetAv, ToTrinnskontrollHandling.GODKJENN, aarsaker, forklaring)
    }

    fun avvis(
        entityId: UUID,
        opprettetAv: NavIdent,
        aarsaker: List<String>,
        forklaring: String?,
    ) {
        return insert(entityId, opprettetAv, ToTrinnskontrollHandling.AVVIS, aarsaker, forklaring)
    }

    fun sistHandling(entityId: UUID): ToTrinnskontrollHandling? {
        @Language("PostgreSQL")
        val query = """
            select * from to_trinnskontroll_handling_log
            where entity_id = :entity_id::uuid
            order by id desc
            limit 1
        """.trimIndent()

        return session.single(queryOf(query, mapOf("entity_id" to entityId))) {
            ToTrinnskontrollHandling.valueOf(it.string("handling"))
        }
    }

    private fun insert(
        entityId: UUID,
        opprettetAv: NavIdent,
        handling: ToTrinnskontrollHandling,
        aarsaker: List<String>,
        forklaring: String?,
    ) {
        when (sistHandling(entityId)) {
            ToTrinnskontrollHandling.FORESLA_OPPRETT, ToTrinnskontrollHandling.FORESLA_ANNULLER ->
                require(handling in listOf(ToTrinnskontrollHandling.GODKJENN, ToTrinnskontrollHandling.AVVIS)) {
                    "Kan ikke foreslå endring før forrige er godkjent eller avvist"
                }
            ToTrinnskontrollHandling.GODKJENN, ToTrinnskontrollHandling.AVVIS ->
                require(handling == ToTrinnskontrollHandling.FORESLA_ANNULLER) {
                    "Allerede godkjent eller avvist"
                }
            null ->
                require(handling == ToTrinnskontrollHandling.FORESLA_OPPRETT) {
                    "Kan kun foreslå opprettelse først"
                }
        }

        @Language("PostgreSQL")
        val query = """
            insert into to_trinnskontroll_handling_log (
                entity_id,
                handling,
                opprettet_av,
                aarsaker,
                forklaring
            ) values (
                :entity_id::uuid,
                :handling::to_trinnskontroll_handling,
                :opprettet_av,
                :aarsaker,
                :forklaring
            )
        """.trimIndent()

        val params = mapOf(
            "entity_id" to entityId,
            "opprettet_av" to opprettetAv.value,
            "handling" to handling.name,
            "aarsaker" to session.createTextArray(aarsaker),
            "forklaring" to forklaring,
        )

        session.execute(queryOf(query, params))
    }
}
