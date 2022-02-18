package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.database.DatabaseFactory
import no.nav.mulighetsrommet.api.domain.Tiltakstype
import no.nav.mulighetsrommet.api.domain.TiltakstypeTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update

class TiltakstypeService(private val db: DatabaseFactory) {

    suspend fun getTiltakstyper(innsatsgruppe: List<Int>?, search: String?): List<Tiltakstype> {
        val rows = db.dbQuery {
            val query = TiltakstypeTable
                .select { TiltakstypeTable.archived eq false }
                .orderBy(TiltakstypeTable.id to SortOrder.ASC)

            innsatsgruppe?.let { query.andWhere { TiltakstypeTable.innsatsgruppeId inList it } }
            search?.let { query.andWhere { TiltakstypeTable.tittel like ("%$it%") } }

            query.toList()
        }
        return rows.map { row ->
            toTiltakstype(row)
        }
    }

    suspend fun createTiltakstype(tiltakstype: Tiltakstype): Tiltakstype {
        val id = db.dbQuery {
            TiltakstypeTable.insertAndGetId {
                it[innsatsgruppeId] = tiltakstype.innsatsgruppe
                it[tittel] = tiltakstype.tittel
                it[beskrivelse] = tiltakstype.beskrivelse
                it[ingress] = tiltakstype.ingress
                it[archived] = false
            }
        }
        return getTiltakstypeById(id.value)!!
    }

    suspend fun updateTiltakstype(id: Int, tiltakstype: Tiltakstype): Tiltakstype? {
        db.dbQuery {
            TiltakstypeTable.update({ TiltakstypeTable.id eq id and (TiltakstypeTable.archived eq false) }) {
                it[innsatsgruppeId] = tiltakstype.innsatsgruppe
                it[tittel] = tiltakstype.tittel
                it[beskrivelse] = tiltakstype.beskrivelse
                it[ingress] = tiltakstype.ingress
            }
        }
        return getTiltakstypeById(id)
    }

    suspend fun archivedTiltakstype(tiltakstype: Tiltakstype) = db.dbQuery {
        TiltakstypeTable.update({ TiltakstypeTable.id eq tiltakstype.id }) {
            it[archived] = true
        }
    }

    suspend fun getTiltakstypeById(id: Int): Tiltakstype? {
        val tiltakstypeRow = db.dbQuery {
            TiltakstypeTable
                .select { TiltakstypeTable.id eq id and (TiltakstypeTable.archived eq false) }
                .firstOrNull()
        }

        return tiltakstypeRow?.let { toTiltakstype(it) }
    }

    private fun toTiltakstype(row: ResultRow): Tiltakstype =
        Tiltakstype(
            id = row[TiltakstypeTable.id].value,
            tittel = row[TiltakstypeTable.tittel],
            beskrivelse = row[TiltakstypeTable.beskrivelse],
            ingress = row[TiltakstypeTable.ingress],
            innsatsgruppe = row[TiltakstypeTable.innsatsgruppeId]
        )
}
