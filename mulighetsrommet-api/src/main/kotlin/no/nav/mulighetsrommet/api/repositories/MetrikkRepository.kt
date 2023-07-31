package no.nav.mulighetsrommet.api.repositories

import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.Deltakeropphav
import org.intellij.lang.annotations.Language

class MetrikkRepository(private val db: Database) {
    fun hentAntallUlesteNotifikasjoner(): Int {
        @Language("PostgreSQL")
        val query = """
            select count(*) as antallUlesteNotifikasjoner from user_notification where done_at is null
        """.trimIndent()

        return queryOf(query).map { it.int("antallUlesteNotifikasjoner") }.asSingle.let { db.run(it) } ?: 0
    }

    fun hentAntallLesteNotifikasjoner(): Int {
        @Language("PostgreSQL")
        val query = """
            select count(*) as antallLesteNotifikasjoner from user_notification where done_at is not null
        """.trimIndent()

        return queryOf(query).map { it.int("antallLesteNotifikasjoner") }.asSingle.let { db.run(it) } ?: 0
    }

    fun hentAntallAvtalerMedAnsvarlig(): Int {
        @Language("PostgreSQL")
        val query = """
            select count(*) as antallAvtalerMedAnsvarlig from avtale_ansvarlig
        """.trimIndent()
        return queryOf(query).map { it.int("antallAvtalerMedAnsvarlig") }.asSingle.let { db.run(it) } ?: 0
    }

    fun hentAntallLeverandorer(): Int {
        @Language("PostgreSQL")
        val query = """
            select distinct count(organisasjonsnummer) as antallLeverandorer from virksomhet where overordnet_enhet is null
        """.trimIndent()

        return queryOf(query).map { it.int("antallLeverandorer") }.asSingle.let { db.run(it) } ?: 0
    }

    fun hentAntallArrangorer(): Int {
        @Language("PostgreSQL")
        val query = """
            select distinct count(organisasjonsnummer) as antallArrangorer from virksomhet where overordnet_enhet is not null
        """.trimIndent()

        return queryOf(query).map { it.int("antallArrangorer") }.asSingle.let { db.run(it) } ?: 0
    }

    fun hentAntallAnsvarligForTiltaksgjennomforing(): Int {
        @Language("PostgreSQL")
        val query = """
            select count(*) as antallAnsvarligForGjennomforing from tiltaksgjennomforing_ansvarlig
        """.trimIndent()

        return queryOf(query).map { it.int("antallAnsvarligForGjennomforing") }.asSingle.let { db.run(it) } ?: 0
    }

    fun hentAntallTiltaksgjennomforingerMedOpphav(opphav: ArenaMigrering.Opphav): Int {
        val params = mapOf(
            "opphav" to opphav.name,
        )

        @Language("PostgreSQL")
        val query = """
            select count(*) as antallGjennomforinger from tiltaksgjennomforing
            where opphav = :opphav::opphav
        """.trimIndent()

        return queryOf(query, params).map { it.int("antallGjennomforinger") }.asSingle.let { db.run(it) } ?: 0
    }

    fun hentAntallDeltakerMedOpphav(opphav: Deltakeropphav): Int {
        val params = mapOf(
            "opphav" to opphav.name,
        )

        @Language("PostgreSQL")
        val query = """
            select count(*) as antallDeltakere from deltaker
            where opphav = :opphav::deltakeropphav
        """.trimIndent()

        return queryOf(query, params).map { it.int("antallDeltakere") }.asSingle.let { db.run(it) } ?: 0
    }

    fun hentAntallAvtalerMedOpphav(opphav: ArenaMigrering.Opphav): Int {
        val params = mapOf(
            "opphav" to opphav.name,
        )

        @Language("PostgreSQL")
        val query = """
            select count(*) as antallAvtaler from avtale
            where opphav = :opphav::opphav
        """.trimIndent()

        return queryOf(query, params).map { it.int("antallAvtaler") }.asSingle.let { db.run(it) } ?: 0
    }

    fun hentAntallAvtaleNotater(): Int {
        @Language("PostgreSQL")
        val query = """
            select count(id) as antallAvtaleNotater from avtale_notat
        """.trimIndent()

        return queryOf(query).map { it.int("antallAvtaleNotater") }.asSingle.let { db.run(it) } ?: 0
    }

    fun hentAntallTiltaksgjennomforingNotater(): Int {
        @Language("PostgreSQL")
        val query = """
            select count(id) as antallTiltaksgjennomforingNotater from "mulighetsrommet-api-db".public.tiltaksgjennomforing_notat
        """.trimIndent()

        return queryOf(query).map { it.int("antallTiltaksgjennomforingNotater") }.asSingle.let { db.run(it) } ?: 0
    }
}
