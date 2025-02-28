package no.nav.tiltak.okonomi.db.queries

import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.tiltak.okonomi.Bestillingstype
import no.nav.tiltak.okonomi.db.periode
import no.nav.tiltak.okonomi.db.toDaterange
import no.nav.tiltak.okonomi.model.OebsKontering
import org.intellij.lang.annotations.Language

class TiltakKonteringQueries(private val session: Session) {

    fun insertKontering(kontering: OebsKontering) {
        @Language("PostgreSQL")
        val query = """
            insert into tiltak_kontering_oebs (bestillingstype, tiltakskode, periode, statlig_regnskapskonto, statlig_artskonto)
            values (:bestillingstype::bestillingstype, :tiltakskode, :periode::daterange, :statlig_regnskapskonto, :statlig_artskonto)
        """.trimIndent()

        val params = mapOf(
            "bestillingstype" to kontering.bestillingstype.name,
            "tiltakskode" to kontering.tiltakskode.name,
            "periode" to kontering.periode.toDaterange(),
            "statlig_regnskapskonto" to kontering.statligRegnskapskonto,
            "statlig_artskonto" to kontering.statligArtskonto,
        )

        session.execute(queryOf(query, params))
    }

    fun getOebsKontering(
        tiltakskode: Tiltakskode,
        bestillingstype: Bestillingstype,
        periode: Periode,
    ): OebsKontering? {
        @Language("PostgreSQL")
        val query = """
            select bestillingstype, tiltakskode, periode, statlig_regnskapskonto, statlig_artskonto
            from tiltak_kontering_oebs
            where bestillingstype = :bestillingstype
              and tiltakskode = :tiltakskode
              and periode @> :periode::daterange
        """.trimIndent()

        val params = mapOf(
            "bestillingstype" to bestillingstype.name,
            "tiltakskode" to tiltakskode.name,
            "periode" to periode.toDaterange(),
        )

        return session.single(queryOf(query, params)) {
            OebsKontering(
                bestillingstype = Bestillingstype.valueOf(it.string("bestillingstype")),
                tiltakskode = Tiltakskode.valueOf(it.string("tiltakskode")),
                periode = it.periode("periode"),
                statligRegnskapskonto = it.string("statlig_regnskapskonto"),
                statligArtskonto = it.string("statlig_artskonto"),
            )
        }
    }
}
