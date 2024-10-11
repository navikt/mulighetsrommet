package no.nav.mulighetsrommet.utdanning.db

import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.utdanning.model.Programomrade
import no.nav.mulighetsrommet.utdanning.model.ProgramomradeMedUtdanninger
import no.nav.mulighetsrommet.utdanning.model.Utdanning
import org.intellij.lang.annotations.Language

class UtdanningRepository(private val db: Database) {

    fun getUtdanningerMedProgramomrader(): List<ProgramomradeMedUtdanninger> {
        @Language("PostgreSQL")
        val programomraderQuery = """
            select *
            from utdanning_programomrade
            where utdanningsprogram = 'YRKESFAGLIG' and array_length(nus_koder, 1) > 0
            order by navn
        """.trimIndent()

        @Language("PostgreSQL")
        val utdanningerQuery = """
            select
                u.id,
                u.navn,
                u.programlop_start,
                coalesce(array_agg(nki.nus_kode) filter (where nki.nus_kode is not null), '{}') as nuskoder
            from
                utdanning u
                    left join
                utdanning_nus_kode unk on u.utdanning_id = unk.utdanning_id
                    left join
                utdanning_nus_kode_innhold nki on unk.nus_kode = nki.nus_kode
            group by
                u.id, u.navn
            having
                coalesce(array_agg(nki.nus_kode) filter (where nki.nus_kode is not null), '{}') <> '{}'
            order by u.navn;
        """.trimIndent()

        val programomrader = queryOf(programomraderQuery).map { row ->
            ProgramomradeMedUtdanninger.Programomrade(
                id = row.uuid("id"),
                navn = row.string("navn"),
                nusKoder = row.array<String>("nus_koder").toList(),
            )
        }.asList.let { db.run(it) }

        val utdanninger = queryOf(utdanningerQuery).map { row ->
            ProgramomradeMedUtdanninger.Utdanning(
                id = row.uuid("id"),
                navn = row.string("navn"),
                programlopStart = row.uuid("programlop_start"),
                nusKoder = row.array<String>("nusKoder").toList(),
            )
        }.asList.let { db.run(it) }

        val utdanningerMedProgramomrade = programomrader.map { programomrade ->
            ProgramomradeMedUtdanninger(
                programomrade = programomrade,
                utdanninger = utdanninger.filter { it.programlopStart == programomrade.id },
            )
        }

        return utdanningerMedProgramomrade
    }

    fun upsertPrograomomrade(session: TransactionalSession, programomrade: Programomrade) {
        @Language("PostgreSQL")
        val query = """
            insert into utdanning_programomrade (navn, programomradekode, utdanningsprogram)
            values (:navn, :programomradekode, :utdanningsprogram::utdanning_program)
            on conflict (programomradekode) do update set
                navn = excluded.navn,
                utdanningsprogram = excluded.utdanningsprogram
        """.trimIndent()

        val params = mapOf(
            "navn" to programomrade.navn,
            "programomradekode" to programomrade.programomradekode,
            "utdanningsprogram" to programomrade.utdanningsprogram?.name,
        )

        queryOf(query, params).asExecute.runWithSession(session)
    }

    fun upsertUtdanning(session: TransactionalSession, utdanning: Utdanning) {
        @Language("PostgreSQL")
        val getIdForProgramomradeQuery = """
            select id from utdanning_programomrade where programomradekode = :programomradekode
        """.trimIndent()

        val programomradeId =
            queryOf(getIdForProgramomradeQuery, mapOf("programomradekode" to utdanning.utdanningslop.first()))
                .map { it.uuid("id") }
                .asSingle
                .runWithSession(session)

        @Language("PostgreSQL")
        val upsertUtdanning = """
            insert into utdanning (utdanning_id, programomradekode, navn, utdanningsprogram, sluttkompetanse, aktiv, utdanningstatus, utdanningslop, programlop_start)
            values (:utdanning_id, :programomradekode, :navn, :utdanningsprogram::utdanning_program, :sluttkompetanse::utdanning_sluttkompetanse, :aktiv, :utdanningstatus::utdanning_status, :utdanningslop, :programlop_start::uuid)
            on conflict (utdanning_id) do update set
                programomradekode = excluded.programomradekode,
                navn = excluded.navn,
                utdanningsprogram = excluded.utdanningsprogram,
                sluttkompetanse = excluded.sluttkompetanse,
                aktiv = excluded.aktiv,
                utdanningstatus = excluded.utdanningstatus,
                utdanningslop = excluded.utdanningslop,
                programlop_start = excluded.programlop_start
        """.trimIndent()

        @Language("PostgreSQL")
        val nuskodeInnholdInsertQuery = """
            insert into utdanning_nus_kode_innhold(title, nus_kode)
            values(:title, :nus_kode)
            on conflict (nus_kode) do update set
                title = excluded.title
        """.trimIndent()

        @Language("PostgreSQL")
        val nusKodeKoblingforUtdanningQuery = """
            insert into utdanning_nus_kode(utdanning_id, nus_kode)
            values (:utdanning_id, :nus_kode_id)
        """.trimIndent()

        queryOf(
            upsertUtdanning,
            mapOf(
                "utdanning_id" to utdanning.utdanningId,
                "programomradekode" to utdanning.programomradekode,
                "navn" to utdanning.navn,
                "utdanningsprogram" to utdanning.utdanningsprogram.name,
                "sluttkompetanse" to utdanning.sluttkompetanse?.name,
                "aktiv" to utdanning.aktiv,
                "utdanningstatus" to utdanning.utdanningstatus.name,
                "utdanningslop" to db.createTextArray(utdanning.utdanningslop),
                "programlop_start" to programomradeId,
            ),
        ).asExecute.runWithSession(session)

        utdanning.nusKodeverk.forEach { nus ->
            queryOf(
                nuskodeInnholdInsertQuery,
                mapOf("title" to nus.navn, "nus_kode" to nus.kode),
            ).asExecute.runWithSession(session)

            queryOf(
                nusKodeKoblingforUtdanningQuery,
                mapOf("utdanning_id" to utdanning.utdanningId, "nus_kode_id" to nus.kode),
            ).asExecute.runWithSession(session)
        }
    }
}
