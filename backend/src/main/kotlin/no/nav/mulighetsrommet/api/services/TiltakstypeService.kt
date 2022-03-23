package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.database.DatabaseFactory
import no.nav.mulighetsrommet.api.domain.Tiltakskode
import no.nav.mulighetsrommet.api.domain.Tiltakstype
import no.nav.mulighetsrommet.api.domain.TiltakstypeTable
import org.jetbrains.exposed.sql.*

class TiltakstypeService(private val db: DatabaseFactory) {

    suspend fun getTiltakstyper(innsatsgruppe: List<Int>?, search: String?): List<Tiltakstype> {
        val rows = db.dbQuery {
            val query = TiltakstypeTable
                .selectAll()
                .orderBy(TiltakstypeTable.id to SortOrder.ASC)

            innsatsgruppe?.let { query.andWhere { TiltakstypeTable.innsatsgruppeId inList it } }
            search?.let { query.andWhere { TiltakstypeTable.navn like ("%$it%") } }

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
            }
        }
        return getTiltakstypeById(id.value)!!
    }

    suspend fun updateTiltakstype(tiltakstype: Tiltakstype): Tiltakstype {
        db.dbQuery {
            TiltakstypeTable.update({ TiltakstypeTable.tiltakskode eq tiltakstype.tiltakskode }) {
                it[navn] = tiltakstype.navn
                it[innsatsgruppeId] = tiltakstype.innsatsgruppe
                it[fraDato] = tiltakstype.fraDato
                it[tilDato] = tiltakstype.tilDato
                it[updatedBy] = tiltakstype.updatedBy
            }
        }
        return getTiltakstypeByTiltakskode(tiltakstype.tiltakskode)!!
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
            createdAt = row[TiltakstypeTable.createdAt],
            createdBy = row[TiltakstypeTable.createdBy],
            updatedAt = row[TiltakstypeTable.updatedAt],
            updatedBy = row[TiltakstypeTable.updatedBy]
        )
}
