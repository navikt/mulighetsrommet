package no.nav.amt_informasjon_api.services

import no.nav.amt_informasjon_api.database.DatabaseFactory.dbQuery
import no.nav.amt_informasjon_api.domain.Tiltaksgjennomforing
import no.nav.amt_informasjon_api.domain.TiltaksgjennomforingTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll

class TiltaksgjennomforingService {

    suspend fun getTiltaksgjennomforinger(): List<Tiltaksgjennomforing> {
        val tiltaksgjennomforingRows = dbQuery {
            TiltaksgjennomforingTable.selectAll().toList()
        }
        return tiltaksgjennomforingRows.map { row ->
            toTiltaksgjennomforing(row)
        }
    }

    suspend fun getTiltaksgjennomforingById(id: Int): Tiltaksgjennomforing? {
        val tiltaksgjennomforingRow = dbQuery {
            TiltaksgjennomforingTable.select { TiltaksgjennomforingTable.id eq id }.firstOrNull()
        }
        if (tiltaksgjennomforingRow != null) {
            return toTiltaksgjennomforing(tiltaksgjennomforingRow)
        } else {
            return null
        }
    }

    suspend fun getTiltaksgjennomforingerByTiltaksvariantId(id: Int): List<Tiltaksgjennomforing> {
        val tiltaksgjennomforingRows = dbQuery {
            TiltaksgjennomforingTable.select { TiltaksgjennomforingTable.tiltaksvariantId eq id }.toList()
        }
        return tiltaksgjennomforingRows.map { row ->
            toTiltaksgjennomforing(row)
        }
    }

    private fun toTiltaksgjennomforing(row: ResultRow): Tiltaksgjennomforing =
        Tiltaksgjennomforing(
            id = row[TiltaksgjennomforingTable.id].value,
            tittel = row[TiltaksgjennomforingTable.tittel],
            beskrivelse = row[TiltaksgjennomforingTable.beskrivelse],
            tiltaksnummer = row[TiltaksgjennomforingTable.tiltaksnummer],
            tiltaksvariantId = row[TiltaksgjennomforingTable.tiltaksvariantId].value,
            fraDato = row[TiltaksgjennomforingTable.fraDato],
            tilDato = row[TiltaksgjennomforingTable.tilDato]
        )
}
