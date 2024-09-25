package no.nav.mulighetsrommet.api.repositories

import kotlinx.serialization.Serializable
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.intellij.lang.annotations.Language
import java.util.*

class UtdanningRepository(private val db: Database) {

    fun getUtdanningerMedProgramomrader(): List<UtdanningerMedProgramomrade> {
        @Language("PostgreSQL")
        val programomraderQuery = """
            SELECT * FROM utdanning_programomrade where utdanningsprogram = 'YRKESFAGLIG' and array_length(nus_koder, 1) > 0
            order by navn
        """.trimIndent()

        @Language("PostgreSQL")
        val utdanningerQuery = """
            SELECT
                u.id,
                u.navn,
                u.programlop_start,
                COALESCE(ARRAY_AGG(nki.nus_kode) FILTER (WHERE nki.nus_kode IS NOT NULL), '{}') AS nusKoder
            FROM
                utdanning u
                    LEFT JOIN
                utdanning_nus_kode unk ON u.utdanning_id = unk.utdanning_id
                    LEFT JOIN
                utdanning_nus_kode_innhold nki ON unk.nus_kode = nki.nus_kode
            GROUP BY
                u.id, u.navn
            HAVING
                COALESCE(ARRAY_AGG(nki.nus_kode) FILTER (WHERE nki.nus_kode IS NOT NULL), '{}') <> '{}'
            order by u.navn;
        """.trimIndent()

        val programomrader = queryOf(programomraderQuery).map { row ->
            Programomrade(
                id = row.uuid("id"),
                navn = row.string("navn"),
                nusKoder = row.array<String>("nus_koder").toList(),
            )
        }.asList.let { db.run(it) }

        val utdanninger = queryOf(utdanningerQuery).map { row ->
            UtdanningDbo(
                id = row.uuid("id"),
                navn = row.string("navn"),
                programlopStart = row.uuid("programlop_start"),
                nusKoder = row.array<String>("nusKoder").toList(),
            )
        }.asList.let { db.run(it) }

        val utdanningerMedProgramomrade = programomrader.map { programomrade ->
            UtdanningerMedProgramomrade(
                programomrade = programomrade,
                utdanninger = utdanninger.filter { it.programlopStart == programomrade.id },
            )
        }

        return utdanningerMedProgramomrade
    }
}

@Serializable
data class UtdanningerMedProgramomrade(
    val programomrade: Programomrade,
    val utdanninger: List<UtdanningDbo>,
)

@Serializable
data class Programomrade(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val nusKoder: List<String>,
)

@Serializable
data class UtdanningDbo(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    @Serializable(with = UUIDSerializer::class)
    val programlopStart: UUID,
    val nusKoder: List<String>,
)
