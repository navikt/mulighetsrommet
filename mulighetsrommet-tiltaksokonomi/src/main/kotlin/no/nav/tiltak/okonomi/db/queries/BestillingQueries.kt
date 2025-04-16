package no.nav.tiltak.okonomi.db.queries

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.database.datatypes.periode
import no.nav.mulighetsrommet.database.datatypes.toDaterange
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.tiltak.okonomi.BestillingStatusType
import no.nav.tiltak.okonomi.OkonomiPart
import no.nav.tiltak.okonomi.model.Bestilling
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime

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

    fun setAvstemtTidspunkt(tidspunkt: LocalDateTime, bestillingsnummer: List<String>) {
        @Language("PostgreSQL")
        val query = """
            update bestilling
            set avstemt_tidspunkt = :tidspunkt
            where bestillingsnummer = any(:bestillingsnummer)
        """.trimIndent()

        session.execute(
            queryOf(
                query,
                mapOf(
                    "tidspunkt" to tidspunkt,
                    "bestillingsnummer" to session.createTextArray(bestillingsnummer),
                ),
            ),
        )
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

    fun setFeilmelding(
        bestillingsnummer: String,
        feilKode: String?,
        feilMelding: String?,
    ) {
        @Language("PostgreSQL")
        val query = """
            update bestilling set
                feil_kode = :feil_kode,
                feil_melding = :feil_melding
            where bestillingsnummer = :bestillingsnummer
        """.trimIndent()
        val params = mapOf(
            "bestillingsnummer" to bestillingsnummer,
            "feil_kode" to feilKode,
            "feil_melding" to feilMelding,
        )
        session.execute(queryOf(query, params))
    }

    fun getByBestillingsnummer(bestillingsnummer: String): Bestilling? {
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

        return session.single(queryOf(selectBestilling, bestillingsnummer)) { it.toBestilling() }
    }

    fun getNotAvstemt(): List<Bestilling> {
        @Language("PostgreSQL")
        val query = """
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
            where avstemt_tidspunkt is null
        """.trimIndent()

        return session.list(queryOf(query)) { it.toBestilling() }
    }

    private fun Row.toBestilling(): Bestilling {
        @Language("PostgreSQL")
        val selectLinje = """
            select linjenummer, periode, belop
            from bestilling_linje
            where bestilling_id = ?
            order by linjenummer
        """.trimIndent()

        val linjer = session.list(queryOf(selectLinje, int("id"))) { linje ->
            Bestilling.Linje(
                linjenummer = linje.int("linjenummer"),
                periode = linje.periode("periode"),
                belop = linje.int("belop"),
            )
        }

        val status = BestillingStatusType.valueOf(string("status"))
        return Bestilling(
            tiltakskode = Tiltakskode.valueOf(string("tiltakskode")),
            arrangorHovedenhet = Organisasjonsnummer(string("arrangor_hovedenhet")),
            arrangorUnderenhet = Organisasjonsnummer(string("arrangor_underenhet")),
            kostnadssted = NavEnhetNummer(string("kostnadssted")),
            bestillingsnummer = string("bestillingsnummer"),
            avtalenummer = stringOrNull("avtalenummer"),
            belop = int("belop"),
            periode = periode("periode"),
            status = status,
            opprettelse = Bestilling.Totrinnskontroll(
                behandletAv = OkonomiPart.fromString(this.string("opprettelse_behandlet_av")),
                behandletTidspunkt = localDateTime("opprettelse_behandlet_tidspunkt"),
                besluttetAv = OkonomiPart.fromString(this.string("opprettelse_besluttet_av")),
                besluttetTidspunkt = localDateTime("opprettelse_besluttet_tidspunkt"),
            ),
            annullering = this.stringOrNull("annullering_behandlet_av")?.let {
                Bestilling.Totrinnskontroll(
                    behandletAv = OkonomiPart.fromString(it),
                    behandletTidspunkt = localDateTime("annullering_behandlet_tidspunkt"),
                    besluttetAv = OkonomiPart.fromString(string("annullering_besluttet_av")),
                    besluttetTidspunkt = localDateTime("annullering_besluttet_tidspunkt"),
                )
            },
            linjer = linjer,
        )
    }
}
