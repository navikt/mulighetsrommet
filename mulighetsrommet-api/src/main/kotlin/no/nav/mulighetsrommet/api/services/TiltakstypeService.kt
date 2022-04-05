package no.nav.mulighetsrommet.api.services

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.database.Database
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.Tiltakstype

class TiltakstypeService(private val db: Database) {

//    suspend fun getTiltakstyper(innsatsgruppe: List<Int>?, search: String?): List<Tiltakstype> {
//        val rows = db.dbQuery {
//            val query = TiltakstypeTable
//                .selectAll()
//                .orderBy(TiltakstypeTable.id to SortOrder.ASC)
//
//            innsatsgruppe?.let { query.andWhere { TiltakstypeTable.innsatsgruppeId inList it } }
//            search?.let { query.andWhere { TiltakstypeTable.navn like ("%$it%") } }
//
//            query.toList()
//        }
//        return rows.map { row ->
//            toTiltakstype(row)
//        }
//    }
//
//    suspend fun createTiltakstype(tiltakstype: Tiltakstype): Tiltakstype {
//        val id = db.dbQuery {
//            TiltakstypeTable.insertAndGetId {
//                it[innsatsgruppeId] = tiltakstype.innsatsgruppe
//                it[navn] = tiltakstype.navn
//                it[sanityId] = tiltakstype.sanityId
//                it[tiltakskode] = tiltakstype.tiltakskode
//                it[fraDato] = tiltakstype.fraDato
//                it[tilDato] = tiltakstype.tilDato
//                it[createdBy] = tiltakstype.createdBy
//            }
//        }
//        return getTiltakstypeById(id.value)!!
//    }
//
//    suspend fun updateTiltakstype(tiltakstype: Tiltakstype): Tiltakstype {
//        db.dbQuery {
//            TiltakstypeTable.update({ TiltakstypeTable.tiltakskode eq tiltakstype.tiltakskode }) {
//                it[navn] = tiltakstype.navn
//                it[innsatsgruppeId] = tiltakstype.innsatsgruppe
//                it[fraDato] = tiltakstype.fraDato
//                it[tilDato] = tiltakstype.tilDato
//                it[updatedBy] = tiltakstype.updatedBy
//            }
//        }
//        return getTiltakstypeByTiltakskode(tiltakstype.tiltakskode)!!
//    }
//
//    suspend fun getTiltakstypeByTiltakskode(tiltakskode: Tiltakskode): Tiltakstype? {
//        val row = db.dbQuery {
//            TiltakstypeTable.select { TiltakstypeTable.tiltakskode eq tiltakskode }.firstOrNull()
//        }
//        return row?.let { toTiltakstype(it) }
//    }
//
//    private suspend fun getTiltakstypeById(id: Int): Tiltakstype? {
//        val row = db.dbQuery {
//            TiltakstypeTable
//                .select { TiltakstypeTable.id eq id }
//                .firstOrNull()
//        }
//        return row?.let { toTiltakstype(it) }
//    }

    fun getTiltakstyper(innsatsgruppe: Int?, searchQuery: String?): List<Tiltakstype> {
        val query = """
            select id, navn, innsatsgruppe_id, sanity_id, tiltakskode, dato_fra, dato_til from tiltakstype ${}
        """.trimIndent()
        val queryResult = queryOf(query, searchQuery).map { toTiltakstype(it) }.asList
        return db.session.run(queryResult)
    }

    

    private fun toTiltakstype(row: Row): Tiltakstype =
        Tiltakstype(
            id = row.int("id"),
            navn = row.string("navn"),
            innsatsgruppe = row.int("innsatsgruppe_id"),
            sanityId = row.intOrNull("sanity_id"),
            tiltakskode = Tiltakskode.valueOf(row.string("tiltakskode")),
            fraDato = row.localDateTime("dato_fra"),
            tilDato = row.localDateTime("dato_til"),
        )
}
