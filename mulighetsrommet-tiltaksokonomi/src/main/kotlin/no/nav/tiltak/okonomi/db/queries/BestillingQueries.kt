package no.nav.tiltak.okonomi.db.queries

import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.datatypes.periode
import no.nav.mulighetsrommet.database.datatypes.toDaterange
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.tiltak.okonomi.OkonomiPart
import no.nav.tiltak.okonomi.model.Bestilling
import no.nav.tiltak.okonomi.model.BestillingStatusType
import org.intellij.lang.annotations.Language

class BestillingQueries(private val session: Session) {

    fun insertBestilling(bestilling: Bestilling) = withTransaction(session) {
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
                status,
                opprettelse_behandlet_av,
                opprettelse_behandlet_tidspunkt,
                opprettelse_besluttet_av,
                opprettelse_besluttet_tidspunkt,
                annullering_behandlet_av,
                annullering_behandlet_tidspunkt,
                annullering_besluttet_av,
                annullering_besluttet_tidspunkt
            ) values (
                :bestillingsnummer,
                :avtalenummer,
                :tiltakskode,
                :arrangor_hovedenhet,
                :arrangor_underenhet,
                :kostnadssted,
                :belop,
                :periode::daterange,
                :status,
                :opprettelse_behandlet_av,
                :opprettelse_behandlet_tidspunkt,
                :opprettelse_besluttet_av,
                :opprettelse_besluttet_tidspunkt,
                :annullering_behandlet_av,
                :annullering_behandlet_tidspunkt,
                :annullering_besluttet_av,
                :annullering_besluttet_tidspunkt
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
            "periode" to bestilling.periode.toDaterange(),
            "status" to bestilling.status.name,
            "opprettelse_behandlet_av" to bestilling.opprettelse.behandletAv.part,
            "opprettelse_behandlet_tidspunkt" to bestilling.opprettelse.behandletTidspunkt,
            "opprettelse_besluttet_av" to bestilling.opprettelse.besluttetAv.part,
            "opprettelse_besluttet_tidspunkt" to bestilling.opprettelse.besluttetTidspunkt,
            "annullering_behandlet_av" to bestilling.annullering?.behandletAv?.part,
            "annullering_behandlet_tidspunkt" to bestilling.annullering?.behandletTidspunkt,
            "annullering_besluttet_av" to bestilling.annullering?.besluttetAv?.part,
            "annullering_besluttet_tidspunkt" to bestilling.annullering?.besluttetTidspunkt,
        )
        val bestillingId = single(queryOf(insertBestilling, params)) { it.int("id") }

        @Language("PostgreSQL")
        val insertLinje = """
            insert into bestilling_linje (bestilling_id, linjenummer, periode, belop)
            values (:bestilling_id, :linjenummer, :periode::daterange, :belop)
        """.trimIndent()
        val linjer = bestilling.linjer.map {
            mapOf(
                "bestilling_id" to bestillingId,
                "linjenummer" to it.linjenummer,
                "periode" to it.periode.toDaterange(),
                "belop" to it.belop,
            )
        }
        batchPreparedNamedStatement(insertLinje, linjer)
    }

    fun setAnnullering(bestillingsnummer: String, annullering: Bestilling.Totrinnskontroll) {
        @Language("PostgreSQL")
        val query = """
            update bestilling
            set annullering_behandlet_av = :behandlet_av,
                annullering_behandlet_tidspunkt = :behandlet_tidspunkt,
                annullering_besluttet_av = :besluttet_av,
                annullering_besluttet_tidspunkt = :besluttet_tidspunkt
            where bestillingsnummer = :bestillingsnummer
        """.trimIndent()
        val params = mapOf(
            "bestillingsnummer" to bestillingsnummer,
            "behandlet_av" to annullering.behandletAv.part,
            "behandlet_tidspunkt" to annullering.behandletTidspunkt,
            "besluttet_av" to annullering.besluttetAv.part,
            "besluttet_tidspunkt" to annullering.besluttetTidspunkt,
        )
        session.execute(queryOf(query, params))
    }

    fun setStatus(bestillingsnummer: String, status: BestillingStatusType) {
        @Language("PostgreSQL")
        val query = """
            update bestilling
            set status = ?
            where bestillingsnummer = ?
        """.trimIndent()
        session.execute(queryOf(query, status.name, bestillingsnummer))
    }

    fun getByBestillingsnummer(bestillingsnummer: String): Bestilling? {
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
                status,
                opprettelse_behandlet_av,
                opprettelse_behandlet_tidspunkt,
                opprettelse_besluttet_av,
                opprettelse_besluttet_tidspunkt,
                annullering_behandlet_av,
                annullering_behandlet_tidspunkt,
                annullering_besluttet_av,
                annullering_besluttet_tidspunkt
            from bestilling
            where bestillingsnummer = ?
        """.trimIndent()

        return session.single(queryOf(selectBestilling, bestillingsnummer)) { bestilling ->
            val linjer = session.list(queryOf(selectLinje, bestilling.int("id"))) { linje ->
                Bestilling.Linje(
                    linjenummer = linje.int("linjenummer"),
                    periode = linje.periode("periode"),
                    belop = linje.int("belop"),
                )
            }

            val status = BestillingStatusType.valueOf(bestilling.string("status"))
            Bestilling(
                tiltakskode = Tiltakskode.valueOf(bestilling.string("tiltakskode")),
                arrangorHovedenhet = Organisasjonsnummer(bestilling.string("arrangor_hovedenhet")),
                arrangorUnderenhet = Organisasjonsnummer(bestilling.string("arrangor_underenhet")),
                kostnadssted = NavEnhetNummer(bestilling.string("kostnadssted")),
                bestillingsnummer = bestilling.string("bestillingsnummer"),
                avtalenummer = bestilling.stringOrNull("avtalenummer"),
                belop = bestilling.int("belop"),
                periode = bestilling.periode("periode"),
                status = status,
                opprettelse = Bestilling.Totrinnskontroll(
                    behandletAv = OkonomiPart.fromString(bestilling.string("opprettelse_behandlet_av")),
                    behandletTidspunkt = bestilling.localDateTime("opprettelse_behandlet_tidspunkt"),
                    besluttetAv = OkonomiPart.fromString(bestilling.string("opprettelse_besluttet_av")),
                    besluttetTidspunkt = bestilling.localDateTime("opprettelse_besluttet_tidspunkt"),
                ),
                annullering = bestilling.stringOrNull("annullering_behandlet_av")?.let {
                    Bestilling.Totrinnskontroll(
                        behandletAv = OkonomiPart.fromString(it),
                        behandletTidspunkt = bestilling.localDateTime("annullering_behandlet_tidspunkt"),
                        besluttetAv = OkonomiPart.fromString(bestilling.string("annullering_besluttet_av")),
                        besluttetTidspunkt = bestilling.localDateTime("annullering_besluttet_tidspunkt"),
                    )
                },
                linjer = linjer,
            )
        }
    }
}
