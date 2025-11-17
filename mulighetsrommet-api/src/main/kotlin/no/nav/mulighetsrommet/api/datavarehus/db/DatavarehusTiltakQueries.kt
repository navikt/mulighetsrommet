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
import no.nav.mulighetsrommet.utdanning.model.Utdanning
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
                   gjennomforing.tiltaksnummer,
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

    private fun getUtdanningslop(id: UUID): DatavarehusTiltakV1YrkesfagDto.Utdanningslop? = with(session) {
        @Language("PostgreSQL")
        val utdanningsprogramQuery = """
            select program.id,
                   program.navn,
                   program.created_at as opprettet_tidspunkt,
                   program.updated_at as oppdatert_tidspunkt,
                   program.nus_koder
            from gjennomforing_utdanningsprogram
                    join utdanningsprogram program on utdanningsprogram_id = program.id
            where gjennomforing_id = ?
            group by program.id
        """.trimIndent()

        val utdanningsprogram = single(queryOf(utdanningsprogramQuery, id)) {
            DatavarehusTiltakV1YrkesfagDto.Utdanningslop.Utdanningsprogram(
                id = it.uuid("id"),
                navn = it.string("navn"),
                opprettetTidspunkt = it.localDateTime("opprettet_tidspunkt"),
                oppdatertTidspunkt = it.localDateTime("oppdatert_tidspunkt"),
                nusKoder = it.array<String>("nus_koder").toList(),
            )
        }

        if (utdanningsprogram == null) {
            return null
        }

        @Language("PostgreSQL")
        val utdanningerQuery = """
            select utdanning.id,
                   utdanning.navn,
                   utdanning.sluttkompetanse,
                   utdanning.created_at as opprettet_tidspunkt,
                   utdanning.updated_at as oppdatert_tidspunkt,
                   utdanning.nus_koder
            from gjennomforing_utdanningsprogram
                    join utdanning on gjennomforing_utdanningsprogram.utdanning_id = utdanning.id
            where gjennomforing_id = ?
            group by utdanning.id;
        """.trimIndent()

        val utdanninger = list(queryOf(utdanningerQuery, id)) { row ->
            DatavarehusTiltakV1YrkesfagDto.Utdanningslop.Utdanning(
                id = row.uuid("id"),
                navn = row.string("navn"),
                sluttkompetanse = row.stringOrNull("sluttkompetanse")?.let { Utdanning.Sluttkompetanse.valueOf(it) },
                opprettetTidspunkt = row.localDateTime("opprettet_tidspunkt"),
                oppdatertTidspunkt = row.localDateTime("oppdatert_tidspunkt"),
                nusKoder = row.array<String>("nus_koder").toList(),
            )
        }

        return DatavarehusTiltakV1YrkesfagDto.Utdanningslop(utdanningsprogram, utdanninger.toSet())
    }

    private fun getAmoKategorisering(id: UUID): AmoKategorisering? = with(session) {
        @Language("PostgreSQL")
        val sertifiseringQuery = """
            select s.label,
                   s.konsept_id
            from gjennomforing_amo_kategorisering_sertifisering k
                     join amo_sertifisering s on k.konsept_id = s.konsept_id
            where k.gjennomforing_id = ?
        """.trimIndent()

        val sertifiseringer = list(queryOf(sertifiseringQuery, id)) {
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

        return single(queryOf(amoKategoriseringQuery, id)) { it.toAmoKategorisering(sertifiseringer) }
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
        arena = stringOrNull("tiltaksnummer")?.let {
            val tiltaksnummmer = Tiltaksnummer(it)
            DatavarehusTiltakV1.ArenaData(
                aar = tiltaksnummmer.aar,
                lopenummer = tiltaksnummmer.lopenummer,
            )
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
        arena = stringOrNull("arena_tiltaksnummer")?.let {
            val tiltaksnummmer = Tiltaksnummer(it)
            DatavarehusTiltakV1.ArenaData(
                aar = tiltaksnummmer.aar,
                lopenummer = tiltaksnummmer.lopenummer,
            )
        },
        navn = null,
        startDato = null,
        sluttDato = null,
        status = null,
        deltidsprosent = null,
    ),
)
