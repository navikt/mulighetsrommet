package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.database.DatabaseFactory
import no.nav.mulighetsrommet.api.domain.Tiltaksgjennomforing
import no.nav.mulighetsrommet.api.domain.TiltaksgjennomforingTable
import no.nav.mulighetsrommet.api.domain.Tiltakskode
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll

class TiltaksgjennomforingService(private val db: DatabaseFactory) {

    suspend fun getTiltaksgjennomforinger(): List<Tiltaksgjennomforing> {
        val tiltaksgjennomforingRows = db.dbQuery {
            TiltaksgjennomforingTable.selectAll().toList()
        }
        return tiltaksgjennomforingRows.map { row ->
            toTiltaksgjennomforing(row)
        }
    }

    suspend fun getTiltaksgjennomforingById(id: Int): Tiltaksgjennomforing? {
        val tiltaksgjennomforingRow = db.dbQuery {
            TiltaksgjennomforingTable.select { TiltaksgjennomforingTable.id eq id }.firstOrNull()
        }
        if (tiltaksgjennomforingRow != null) {
            return toTiltaksgjennomforing(tiltaksgjennomforingRow)
        } else {
            return null
        }
    }

    suspend fun getTiltaksgjennomforingerByTiltakskode(tiltakskode: Tiltakskode): List<Tiltaksgjennomforing> {
        val tiltaksgjennomforingRows = db.dbQuery {
            TiltaksgjennomforingTable.select { TiltaksgjennomforingTable.tiltakskode eq tiltakskode }.toList()
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
            tiltakskode = row[TiltaksgjennomforingTable.tiltakskode],
            fraDato = row[TiltaksgjennomforingTable.fraDato],
            tilDato = row[TiltaksgjennomforingTable.tilDato]
        )
}
