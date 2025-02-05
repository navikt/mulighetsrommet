package no.nav.mulighetsrommet.tiltak.okonomi.db.queries

import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.tiltak.okonomi.OkonomiPart
import no.nav.mulighetsrommet.tiltak.okonomi.db.BestillingDbo
import no.nav.mulighetsrommet.tiltak.okonomi.db.LinjeDbo
import no.nav.mulighetsrommet.tiltak.okonomi.db.periode
import org.intellij.lang.annotations.Language

class BestillingQueries(private val session: Session) {

    fun createBestilling(bestilling: BestillingDbo) = withTransaction(session) {
        @Language("PostgreSQL")
        val insertBestilling = """
            insert into bestilling (
                bestillingsnummer,
                avtalenummer,
                tiltakskode,
                arrangor_hovedenhet,
                arrangor_underenhet,
                kostnadssted,
                belop,
                periode,
                annullert,
                opprettet_av,
                opprettet_tidspunkt,
                besluttet_av,
                besluttet_tidspunkt
            ) values (
                :bestillingsnummer,
                :avtalenummer,
                :tiltakskode,
                :arrangor_hovedenhet,
                :arrangor_underenhet,
                :kostnadssted,
                :belop,
                daterange(:periode_start, :periode_slutt),
                :annullert,
                :opprettet_av,
                :opprettet_tidspunkt,
                :besluttet_av,
                :besluttet_tidspunkt
            )
            returning id
        """
        val params = mapOf(
            "bestillingsnummer" to bestilling.bestillingsnummer,
            "avtalenummer" to bestilling.avtalenummer,
            "tiltakskode" to bestilling.tiltakskode.name,
            "arrangor_hovedenhet" to bestilling.arrangorHovedenhet.value,
            "arrangor_underenhet" to bestilling.arrangorUnderenhet.value,
            "kostnadssted" to bestilling.kostnadssted.value,
            "belop" to bestilling.belop,
            "periode_start" to bestilling.periode.start,
            "periode_slutt" to bestilling.periode.slutt,
            "opprettet_av" to bestilling.opprettetAv.part,
            "opprettet_tidspunkt" to bestilling.opprettetTidspunkt,
            "besluttet_av" to bestilling.besluttetAv.part,
            "besluttet_tidspunkt" to bestilling.besluttetTidspunkt,
            "annullert" to bestilling.annullert,
        )
        val bestillingId = single(queryOf(insertBestilling, params)) { it.int("id") }

        @Language("PostgreSQL")
        val insertLinje = """
            insert into bestilling_linje (bestilling_id, linjenummer, periode, belop)
            values (:bestilling_id, :linjenummer, daterange(:periode_start, :periode_slutt), :belop)
        """.trimIndent()
        val linjer = bestilling.linjer.map {
            mapOf(
                "bestilling_id" to bestillingId,
                "linjenummer" to it.linjenummer,
                "periode_start" to it.periode.start,
                "periode_slutt" to it.periode.slutt,
                "belop" to it.belop,
            )
        }
        batchPreparedNamedStatement(insertLinje, linjer)
    }

    fun getBestilling(bestillingsnummer: String): BestillingDbo? {
        @Language("PostgreSQL")
        val selectLinje = """
            select linjenummer, periode, belop
            from bestilling_linje
            where bestilling_id = ?
            order by linjenummer
        """.trimIndent()

        @Language("PostgreSQL")
        val selectBestilling = """
            select
                id,
                bestillingsnummer,
                avtalenummer,
                tiltakskode,
                arrangor_hovedenhet,
                arrangor_underenhet,
                kostnadssted,
                belop,
                periode,
                opprettet_av,
                opprettet_tidspunkt,
                besluttet_av,
                besluttet_tidspunkt,
                annullert
            from bestilling
            where bestillingsnummer = ?
        """.trimIndent()
        return session.single(queryOf(selectBestilling, bestillingsnummer)) { bestilling ->
            val linjer = session.list(queryOf(selectLinje, bestilling.int("id"))) { linje ->
                LinjeDbo(
                    linjenummer = linje.int("linjenummer"),
                    periode = linje.periode("periode"),
                    belop = linje.int("belop"),
                )
            }

            BestillingDbo(
                tiltakskode = Tiltakskode.valueOf(bestilling.string("tiltakskode")),
                arrangorHovedenhet = Organisasjonsnummer(bestilling.string("arrangor_hovedenhet")),
                arrangorUnderenhet = Organisasjonsnummer(bestilling.string("arrangor_underenhet")),
                kostnadssted = NavEnhetNummer(bestilling.string("kostnadssted")),
                bestillingsnummer = bestilling.string("bestillingsnummer"),
                avtalenummer = bestilling.stringOrNull("avtalenummer"),
                belop = bestilling.int("belop"),
                periode = bestilling.periode("periode"),
                opprettetAv = OkonomiPart.fromString(bestilling.string("opprettet_av")),
                opprettetTidspunkt = bestilling.localDateTime("opprettet_tidspunkt"),
                besluttetAv = OkonomiPart.fromString(bestilling.string("besluttet_av")),
                besluttetTidspunkt = bestilling.localDateTime("besluttet_tidspunkt"),
                annullert = bestilling.boolean("annullert"),
                linjer = linjer,
            )
        }
    }

    fun setAnnullert(bestillingsnummer: String, annullert: Boolean) {
        @Language("PostgreSQL")
        val query = """
            update bestilling
            set annullert = :annullert
            where bestillingsnummer = :bestillingsnummer
        """.trimIndent()
        val params = mapOf(
            "bestillingsnummer" to bestillingsnummer,
            "annullert" to annullert,
        )
        session.execute(queryOf(query, params))
    }
}
