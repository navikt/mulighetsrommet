package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.database.DatabaseFactory.dbQuery
import no.nav.mulighetsrommet.api.domain.Tiltaksvariant
import no.nav.mulighetsrommet.api.domain.TiltaksvariantTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update

class TiltaksvariantService {

    suspend fun getTiltaksvarianter(innsatsgruppe: Int?): List<Tiltaksvariant> {
        val rows = dbQuery {
            val query = TiltaksvariantTable
                .select { TiltaksvariantTable.archived eq false }
                .orderBy(TiltaksvariantTable.id to SortOrder.ASC)

            innsatsgruppe?.let { query.andWhere { TiltaksvariantTable.innsatsgruppeId eq it } }

            query.toList()
        }
        return rows.map { row ->
            toTiltaksvariant(row)
        }
    }

    suspend fun createTiltaksvariant(tiltaksvariant: Tiltaksvariant): Tiltaksvariant {
        val id = dbQuery {
            TiltaksvariantTable.insertAndGetId {
                it[innsatsgruppeId] = tiltaksvariant.innsatsgruppe
                it[tittel] = tiltaksvariant.tittel
                it[beskrivelse] = tiltaksvariant.beskrivelse
                it[ingress] = tiltaksvariant.ingress
                it[archived] = false
            }
        }
        return getTiltaksvariantById(id.value)!!
    }

    suspend fun updateTiltaksvariant(id: Int, tiltaksvariant: Tiltaksvariant): Tiltaksvariant? {
        dbQuery {
            TiltaksvariantTable.update({ TiltaksvariantTable.id eq id and (TiltaksvariantTable.archived eq false) }) {
                it[innsatsgruppeId] = tiltaksvariant.innsatsgruppe
                it[tittel] = tiltaksvariant.tittel
                it[beskrivelse] = tiltaksvariant.beskrivelse
                it[ingress] = tiltaksvariant.ingress
            }
        }
        return getTiltaksvariantById(id)
    }

    suspend fun archivedTiltaksvariant(tiltaksvariant: Tiltaksvariant) = dbQuery {
        TiltaksvariantTable.update({ TiltaksvariantTable.id eq tiltaksvariant.id }) {
            it[archived] = true
        }
    }

    suspend fun getTiltaksvariantById(id: Int): Tiltaksvariant? {
        val tiltaksvariantRow = dbQuery {
            TiltaksvariantTable
                .select { TiltaksvariantTable.id eq id and (TiltaksvariantTable.archived eq false) }
                .firstOrNull()
        }

        return tiltaksvariantRow?.let { toTiltaksvariant(it) }
    }

    private fun toTiltaksvariant(row: ResultRow): Tiltaksvariant =
        Tiltaksvariant(
            id = row[TiltaksvariantTable.id].value,
            tittel = row[TiltaksvariantTable.tittel],
            beskrivelse = row[TiltaksvariantTable.beskrivelse],
            ingress = row[TiltaksvariantTable.ingress],
            innsatsgruppe = row[TiltaksvariantTable.innsatsgruppeId]
        )
}
