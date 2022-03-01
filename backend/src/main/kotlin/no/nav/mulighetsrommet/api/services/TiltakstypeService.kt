package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.database.DatabaseFactory
import no.nav.mulighetsrommet.api.domain.Tiltakskode
import no.nav.mulighetsrommet.api.domain.Tiltakstype
import no.nav.mulighetsrommet.api.domain.TiltakstypeTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll

class TiltakstypeService(private val db: DatabaseFactory) {

    suspend fun getTiltakstyper(): List<Tiltakstype> {
        val rows = db.dbQuery {
            val query = TiltakstypeTable
                .selectAll()
                .orderBy(TiltakstypeTable.id to SortOrder.ASC)
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
                it[navn] = tiltakstype.navn
                it[sanityId] = tiltakstype.sanityId
                it[tiltakskode] = tiltakstype.tiltakskode
                it[fraDato] = tiltakstype.fraDato
                it[tilDato] = tiltakstype.tilDato
                it[createdBy] = tiltakstype.createdBy
                it[createdAt] = tiltakstype.createdAt
            }
        }
        return getTiltakstypeById(id.value)!!
    }

    suspend fun getTiltakstypeByTiltakskode(tiltakskode: Tiltakskode): Tiltakstype? {
        val row = db.dbQuery {
            TiltakstypeTable.select { TiltakstypeTable.tiltakskode eq tiltakskode }.firstOrNull()
        }
        return row?.let { toTiltakstype(it) }
    }

    private suspend fun getTiltakstypeById(id: Int): Tiltakstype? {
        val row = db.dbQuery {
            TiltakstypeTable
                .select { TiltakstypeTable.id eq id }
                .firstOrNull()
        }
        return row?.let { toTiltakstype(it) }
    }

    private fun toTiltakstype(row: ResultRow): Tiltakstype =
        Tiltakstype(
            id = row[TiltakstypeTable.id].value,
            navn = row[TiltakstypeTable.navn],
            innsatsgruppe = row[TiltakstypeTable.innsatsgruppeId],
            sanityId = row[TiltakstypeTable.sanityId],
            tiltakskode = row[TiltakstypeTable.tiltakskode],
            fraDato = row[TiltakstypeTable.fraDato],
            tilDato = row[TiltakstypeTable.tilDato],
            createdBy = row[TiltakstypeTable.createdBy],
            createdAt = row[TiltakstypeTable.createdAt]
        )
}
