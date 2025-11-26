package no.nav.mulighetsrommet.api.datavarehus.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakV1
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakV1AmoDto
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakV1Dto
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakV1YrkesfagDto
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.model.*
import org.intellij.lang.annotations.Language
import java.util.*

class DatavarehusTiltakQueries(private val session: Session) {
    fun getGruppetiltak(id: UUID): DatavarehusTiltakV1 {
        @Language("PostgreSQL")
        val query = """
            select gjennomforing.id,
                   gjennomforing.navn,
                   gjennomforing.start_dato,
                   gjennomforing.slutt_dato,
                   gjennomforing.status,
                   gjennomforing.deltidsprosent,
                   gjennomforing.arena_tiltaksnummer,
                   gjennomforing.created_at     as opprettet_tidspunkt,
                   gjennomforing.updated_at     as oppdatert_tidspunkt,
                   tiltakstype.tiltakskode      as tiltakstype_tiltakskode,
                   avtale.id                    as avtale_id,
                   avtale.navn                  as avtale_navn,
                   avtale.created_at            as avtale_opprettet_tidspunkt,
                   avtale.updated_at            as avtale_oppdatert_tidspunkt,
                   arrangor.organisasjonsnummer as arrangor_organisasjonsnummer
            from gjennomforing
                     join tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id
                     left join avtale on gjennomforing.avtale_id = avtale.id
                     join arrangor on gjennomforing.arrangor_id = arrangor.id
            where gjennomforing.id = ?
        """.trimIndent()

        val dto = session.requireSingle(queryOf(query, id)) { it.toDatavarehusTiltakDto() }

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

    fun getEnkeltplass(id: UUID): DatavarehusTiltakV1 {
        @Language("PostgreSQL")
        val query = """
            select enkeltplass.id,
                   enkeltplass.arena_tiltaksnummer,
                   enkeltplass.created_at     as opprettet_tidspunkt,
                   enkeltplass.updated_at     as oppdatert_tidspunkt,
                   tiltakstype.tiltakskode      as tiltakstype_tiltakskode,
                   arrangor.organisasjonsnummer as arrangor_organisasjonsnummer
            from enkeltplass
                     join tiltakstype on enkeltplass.tiltakstype_id = tiltakstype.id
                     join arrangor on enkeltplass.arrangor_id = arrangor.id
            where enkeltplass.id = ?
        """.trimIndent()

        // TODO: inkluder utdanningsløp/amo-kategorisering når vi har dette for enkeltplasser
        return session.requireSingle(queryOf(query, id)) { it.toDatavarehusEnkeltplassDto() }
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
            AmoKategorisering.BransjeOgYrkesrettet.Sertifisering(
                konseptId = it.long("konsept_id"),
                label = it.string("label"),
            )
        }

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
    sertifiseringer: List<AmoKategorisering.BransjeOgYrkesrettet.Sertifisering>,
): AmoKategorisering {
    val kurstype = AmoKurstype.valueOf(string("kurstype"))
    return when (kurstype) {
        AmoKurstype.BRANSJE_OG_YRKESRETTET -> AmoKategorisering.BransjeOgYrkesrettet(
            bransje = AmoKategorisering.BransjeOgYrkesrettet.Bransje.valueOf(string("bransje")),
            sertifiseringer = sertifiseringer,
            forerkort = array<String>("forerkort")
                .toList()
                .map { AmoKategorisering.BransjeOgYrkesrettet.ForerkortKlasse.valueOf(it) },
            innholdElementer = array<String>("innhold_elementer")
                .toList()
                .map { AmoKategorisering.InnholdElement.valueOf(it) },
        )

        AmoKurstype.NORSKOPPLAERING -> AmoKategorisering.Norskopplaering(
            norskprove = boolean("norskprove"),
            innholdElementer = array<String>("innhold_elementer")
                .toList()
                .map { AmoKategorisering.InnholdElement.valueOf(it) },
        )

        AmoKurstype.GRUNNLEGGENDE_FERDIGHETER -> AmoKategorisering.GrunnleggendeFerdigheter(
            innholdElementer = array<String>("innhold_elementer")
                .toList()
                .map { AmoKategorisering.InnholdElement.valueOf(it) },
        )

        AmoKurstype.FORBEREDENDE_OPPLAERING_FOR_VOKSNE -> AmoKategorisering.ForberedendeOpplaeringForVoksne

        AmoKurstype.STUDIESPESIALISERING -> AmoKategorisering.Studiespesialisering
    }
}

private fun Row.toDatavarehusTiltakDto() = DatavarehusTiltakV1Dto(
    tiltakskode = Tiltakskode.valueOf(string("tiltakstype_tiltakskode")),
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
        navn = string("navn"),
        startDato = localDate("start_dato"),
        sluttDato = localDateOrNull("slutt_dato"),
        status = GjennomforingStatusType.valueOf(string("status")),
        deltidsprosent = double("deltidsprosent"),
    ),
)

private fun Row.toDatavarehusEnkeltplassDto() = DatavarehusTiltakV1Dto(
    tiltakskode = Tiltakskode.valueOf(string("tiltakstype_tiltakskode")),
    avtale = null,
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
        navn = null,
        startDato = null,
        sluttDato = null,
        status = null,
        deltidsprosent = null,
    ),
)
