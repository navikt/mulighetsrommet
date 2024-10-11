package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.services.ArrangorRolle
import no.nav.mulighetsrommet.api.services.ArrangorRolleType
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import org.intellij.lang.annotations.Language
import java.util.*

class ArrangorAnsattRepository(private val db: Database) {
    fun upsertRoller(ansattId: UUID, roller: List<ArrangorRolle>) =
        db.transaction { tx -> upsertRoller(ansattId, roller, tx) }

    fun upsertRoller(ansattId: UUID, roller: List<ArrangorRolle>, tx: Session) {
        @Language("PostgreSQL")
        val upsertRolle = """
             insert into arrangor_ansatt_rolle (
                arrangor_ansatt_id,
                arrangor_id,
                rolle,
                expiry
             ) values (
                :arrangor_ansatt_id::uuid,
                :arrangor_id::uuid,
                :rolle::arrangor_rolle,
                :expiry
             ) on conflict (arrangor_ansatt_id, arrangor_id) do update set
                rolle = excluded.rolle,
                expiry = excluded.expiry
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteRoller = """
             delete from arrangor_ansatt_rolle
             where arrangor_ansatt_id = ?::uuid and not (arrangor_id = any (?))
        """.trimIndent()
        roller.forEach {
            tx.run(queryOf(upsertRolle, it.toSqlParameters(ansattId)).asExecute)
        }

        tx.run(queryOf(deleteRoller, ansattId, db.createUuidArray(roller.map { it.arrangorId })).asExecute)
    }

    fun upsertAnsatt(ansatt: ArrangorAnsatt) =
        db.transaction { tx -> upsertAnsatt(ansatt, tx) }

    fun upsertAnsatt(ansatt: ArrangorAnsatt, tx: Session) {
        @Language("PostgreSQL")
        val query = """
            insert into arrangor_ansatt (
                id,
                norsk_ident
            ) values (
                :id::uuid,
                :norsk_ident
            ) on conflict (id) do update set
                norsk_ident       = excluded.norsk_ident
            returning *
        """.trimIndent()

        tx.run(
            queryOf(query, ansatt.toSqlParameters()).asExecute,
        )
    }

    fun getAnsatt(norskIdent: NorskIdent, tx: Session): ArrangorAnsatt? {
        @Language("PostgreSQL")
        val query = """
            select
                id,
                norsk_ident
            from arrangor_ansatt
            where norsk_ident = ?
        """.trimIndent()

        return queryOf(query, norskIdent.value)
            .map {
                ArrangorAnsatt(
                    id = it.uuid("id"),
                    norskIdent = NorskIdent(it.string("norsk_ident")),
                )
            }
            .asSingle
            .let { tx.run(it) }
    }

    fun getAnsatte(): List<ArrangorAnsatt> =
        db.transaction { tx -> getAnsatte(tx) }

    fun getAnsatte(tx: Session): List<ArrangorAnsatt> {
        @Language("PostgreSQL")
        val query = """
            select
                id,
                norsk_ident
            from arrangor_ansatt
        """.trimIndent()

        return queryOf(query)
            .map {
                ArrangorAnsatt(
                    id = it.uuid("id"),
                    norskIdent = NorskIdent(it.string("norsk_ident")),
                )
            }
            .asList
            .let { tx.run(it) }
    }

    fun getRoller(norskIdent: NorskIdent): List<ArrangorRolle> {
        @Language("PostgreSQL")
        val query = """
            select
                rolle,
                expiry,
                arrangor_id
            from arrangor_ansatt_rolle
                inner join arrangor_ansatt on arrangor_ansatt.id = arrangor_ansatt_rolle.arrangor_ansatt_id
            where arrangor_ansatt.norsk_ident = ?
        """.trimIndent()

        return queryOf(query, norskIdent.value)
            .map { it.toRolle() }
            .asList
            .let { db.run(it) }
    }

    private fun ArrangorRolle.toSqlParameters(ansattId: UUID) = mapOf(
        "arrangor_ansatt_id" to ansattId,
        "arrangor_id" to arrangorId,
        "rolle" to rolle.name,
        "expiry" to expiry,
    )

    private fun ArrangorAnsatt.toSqlParameters() = mapOf(
        "id" to id,
        "norsk_ident" to norskIdent.value,
    )

    fun Row.toRolle(): ArrangorRolle {
        return ArrangorRolle(
            arrangorId = uuid("arrangor_id"),
            rolle = ArrangorRolleType.valueOf(string("rolle")),
            expiry = localDateTime("expiry"),
        )
    }
}

data class ArrangorAnsatt(
    val id: UUID,
    val norskIdent: NorskIdent,
)
