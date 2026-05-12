package no.nav.mulighetsrommet.api.datavarehus.db

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.amo.AmoKategorisering
import no.nav.mulighetsrommet.api.amo.models.Bransje
import no.nav.mulighetsrommet.api.amo.models.ForerkortKlasse
import no.nav.mulighetsrommet.api.amo.models.Kurstype
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakV1
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakV1AmoDto
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakV1Dto
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakV1YrkesfagDto
import no.nav.mulighetsrommet.api.janzz.Sertifisering
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import org.intellij.lang.annotations.Language
import java.util.UUID

class DatavarehusTiltakQueries(private val session: Session) {
    fun getDatavarehusTiltak(id: UUID): DatavarehusTiltakV1 {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_datavarehus_tiltak
            where id = ?
        """.trimIndent()

        val dto = session.requireSingle(queryOf(query, id)) { it.toDatavarehusTiltakDto() }

        // TODO: inkluder utdanningsløp/amo-kategorisering når vi har dette for enkeltplasser
        return when (dto.tiltakskode) {
            Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING -> {
                val utdanningslop = getUtdanningslop(id)
                DatavarehusTiltakV1YrkesfagDto(
                    dto.tiltakskode,
                    dto.avtale,
                    dto.gjennomforing,
                    utdanningslop,
                )
            }

            Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING -> {
                val amoKategorisering = getAmoKategorisering(id)
                DatavarehusTiltakV1AmoDto(
                    dto.tiltakskode,
                    dto.avtale,
                    dto.gjennomforing,
                    amoKategorisering,
                )
            }

            else -> dto
        }
    }

    private fun getUtdanningslop(id: UUID): DatavarehusTiltakV1YrkesfagDto.Utdanningslop? {
        @Language("PostgreSQL")
        val query = """
            select
                program.id as utdanningsprogram_id,
                array_agg(utdanning.id) as utdanning_ids
            from gjennomforing_utdanningsprogram
                join utdanningsprogram program on gjennomforing_utdanningsprogram.utdanningsprogram_id = program.id
                join utdanning on gjennomforing_utdanningsprogram.utdanning_id = utdanning.id
            where gjennomforing_utdanningsprogram.gjennomforing_id = ?
            group by program.id
        """.trimIndent()

        return session.single(queryOf(query, id)) { row ->
            val utdanningsprogramId = row.uuid("utdanningsprogram_id")
            val utdanningIds = row.array<UUID>("utdanning_ids").toSet()
            DatavarehusTiltakV1YrkesfagDto.Utdanningslop(utdanningsprogramId, utdanningIds)
        }
    }

    private fun getAmoKategorisering(id: UUID): AmoKategorisering? {
        @Language("PostgreSQL")
        val sertifiseringQuery = """
            select s.label,
                   s.konsept_id
            from gjennomforing_amo_kategorisering_sertifisering k
                     join amo_sertifisering s on k.konsept_id = s.konsept_id
            where k.gjennomforing_id = ?
        """.trimIndent()

        val sertifiseringer = session.list(queryOf(sertifiseringQuery, id)) {
            Sertifisering(
                konseptId = it.long("konsept_id"),
                label = it.string("label"),
            )
        }.toSet()

        @Language("PostgreSQL")
        val amoKategoriseringQuery = """
            select kurstype,
                   bransje,
                   forerkort,
                   norskprove,
                   innhold_elementer
            from gjennomforing_amo_kategorisering
            where gjennomforing_id = ?
        """.trimIndent()

        return session.single(queryOf(amoKategoriseringQuery, id)) { it.toAmoKategorisering(sertifiseringer) }
    }
}

private fun Row.toAmoKategorisering(
    sertifiseringer: Set<Sertifisering>,
): AmoKategorisering = AmoKategorisering(
    kurstype = Json.decodeFromString<Kurstype>(string("kurstype")),
    bransje = Json.decodeFromString<Bransje>(string("bransje")),
    sertifiseringer = sertifiseringer,
    forerkort = array<String>("forerkort")
        .map { Json.decodeFromString<ForerkortKlasse>(it) }.toSet(),
    innholdElementer = array<String>("innhold_elementer")
        .map { AmoKategorisering.InnholdElement.valueOf(it) }.toSet(),
    norskprove = false,
)

private fun Row.toDatavarehusTiltakDto(): DatavarehusTiltakV1Dto {
    val tiltakskode = Tiltakskode.valueOf(string("tiltakstype_tiltakskode"))
    val oppstartstype = GjennomforingOppstartstype.valueOf(string("oppstart_type"))
    return DatavarehusTiltakV1Dto(
        tiltakskode = tiltakskode,
        avtale = uuidOrNull("avtale_id")?.let {
            DatavarehusTiltakV1.Avtale(
                id = it,
                navn = string("avtale_navn"),
                opprettetTidspunkt = localDateTime("avtale_opprettet_tidspunkt"),
                oppdatertTidspunkt = localDateTime("avtale_oppdatert_tidspunkt"),
            )
        },
        gjennomforing = DatavarehusTiltakV1.Gjennomforing(
            id = uuid("id"),
            opprettetTidspunkt = localDateTime("opprettet_tidspunkt"),
            oppdatertTidspunkt = localDateTime("oppdatert_tidspunkt"),
            arrangor = DatavarehusTiltakV1.Arrangor(
                organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
            ),
            arena = stringOrNull("arena_tiltaksnummer")?.let { Tiltaksnummer(it) }?.let {
                DatavarehusTiltakV1.ArenaData(aar = it.aar, lopenummer = it.lopenummer)
            },
            oppstartstype = oppstartstype,
            pameldingstype = GjennomforingPameldingType.valueOf(string("pamelding_type")),
            navn = string("navn").takeIfIsGruppetiltak(oppstartstype),
            startDato = localDateOrNull("start_dato")?.takeIfIsGruppetiltak(oppstartstype),
            sluttDato = localDateOrNull("slutt_dato")?.takeIfIsGruppetiltak(oppstartstype),
            status = GjennomforingStatusType.valueOf(string("status")).takeIfIsGruppetiltak(oppstartstype),
            deltidsprosent = double("deltidsprosent").takeIfIsGruppetiltak(oppstartstype),
        ),
    )
}

private fun <T> T.takeIfIsGruppetiltak(type: GjennomforingOppstartstype): T? = takeIf {
    when (type) {
        GjennomforingOppstartstype.FELLES, GjennomforingOppstartstype.LOPENDE -> true
        GjennomforingOppstartstype.ENKELTPLASS -> false
    }
}
