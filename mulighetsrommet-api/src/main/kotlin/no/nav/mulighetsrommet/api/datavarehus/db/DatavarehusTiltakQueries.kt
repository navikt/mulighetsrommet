package no.nav.mulighetsrommet.api.datavarehus.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltak
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakAmoDto
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakDto
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakYrkesfagDto
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dto.*
import no.nav.mulighetsrommet.utdanning.model.Utdanning
import org.intellij.lang.annotations.Language
import java.util.*

object DatavarehusTiltakQueries {

    context(Session)
    fun getTiltak(id: UUID): DatavarehusTiltak {
        @Language("PostgreSQL")
        val query = """
            select gjennomforing.id,
                   gjennomforing.navn,
                   gjennomforing.start_dato,
                   gjennomforing.slutt_dato,
                   gjennomforing.tiltaksnummer,
                   gjennomforing.created_at     as opprettet_tidspunkt,
                   gjennomforing.updated_at     as oppdatert_tidspunkt,
                   tiltaksgjennomforing_status(
                           gjennomforing.start_dato,
                           gjennomforing.slutt_dato,
                           gjennomforing.avsluttet_tidspunkt
                   )                            as status,
                   tiltakstype.tiltakskode      as tiltakstype_tiltakskode,
                   avtale.id                    as avtale_id,
                   avtale.navn                  as avtale_navn,
                   avtale.created_at            as avtale_opprettet_tidspunkt,
                   avtale.updated_at            as avtale_oppdatert_tidspunkt,
                   arrangor.organisasjonsnummer as arrangor_organisasjonsnummer
            from tiltaksgjennomforing gjennomforing
                     join tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id
                     left join avtale on gjennomforing.avtale_id = avtale.id
                     join arrangor on gjennomforing.arrangor_id = arrangor.id
            where gjennomforing.id = ?
        """.trimIndent()

        val dto = single(queryOf(query, id)) { it.toDatavarehusTiltakDto() }
            .let { requireNotNull(it) { "Gjennomføring med id=$id finnes ikke" } }

        return when (dto.tiltakskode) {
            Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING -> {
                val utdanningslop = getUtdanningslop(id)
                DatavarehusTiltakYrkesfagDto(
                    dto.tiltakskode,
                    dto.avtale,
                    dto.gjennomforing,
                    utdanningslop,
                )
            }

            Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING -> {
                val amoKategorisering = getAmoKategorisering(id)
                DatavarehusTiltakAmoDto(
                    dto.tiltakskode,
                    dto.avtale,
                    dto.gjennomforing,
                    amoKategorisering,
                )
            }

            else -> dto
        }
    }

    context(Session)
    private fun getUtdanningslop(id: UUID): DatavarehusTiltakYrkesfagDto.Utdanningslop? {
        @Language("PostgreSQL")
        val utdanningsprogramQuery = """
            select program.id,
                   program.navn,
                   program.created_at as opprettet_tidspunkt,
                   program.updated_at as oppdatert_tidspunkt,
                   program.nus_koder
            from tiltaksgjennomforing_utdanningsprogram
                    join utdanningsprogram program on utdanningsprogram_id = program.id
            where tiltaksgjennomforing_id = ?
            group by program.id
        """.trimIndent()

        val utdanningsprogram = single(queryOf(utdanningsprogramQuery, id)) {
            DatavarehusTiltakYrkesfagDto.Utdanningslop.Utdanningsprogram(
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
            from tiltaksgjennomforing_utdanningsprogram
                    join utdanning on tiltaksgjennomforing_utdanningsprogram.utdanning_id = utdanning.id
            where tiltaksgjennomforing_id = ?
            group by utdanning.id;
        """.trimIndent()

        val utdanninger = list(queryOf(utdanningerQuery, id)) {
            DatavarehusTiltakYrkesfagDto.Utdanningslop.Utdanning(
                id = it.uuid("id"),
                navn = it.string("navn"),
                sluttkompetanse = Utdanning.Sluttkompetanse.valueOf(it.string("sluttkompetanse")),
                opprettetTidspunkt = it.localDateTime("opprettet_tidspunkt"),
                oppdatertTidspunkt = it.localDateTime("oppdatert_tidspunkt"),
                nusKoder = it.array<String>("nus_koder").toList(),
            )
        }

        return DatavarehusTiltakYrkesfagDto.Utdanningslop(utdanningsprogram, utdanninger.toSet())
    }

    context(Session)
    private fun getAmoKategorisering(id: UUID): AmoKategorisering? {
        @Language("PostgreSQL")
        val sertifiseringQuery = """
            select s.label,
                   s.konsept_id
            from tiltaksgjennomforing_amo_kategorisering_sertifisering k
                     join amo_sertifisering s on k.konsept_id = s.konsept_id
            where k.tiltaksgjennomforing_id = ?
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
            from tiltaksgjennomforing_amo_kategorisering
            where tiltaksgjennomforing_id = ?
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

private fun Row.toDatavarehusTiltakDto() = DatavarehusTiltakDto(
    tiltakskode = Tiltakskode.valueOf(string("tiltakstype_tiltakskode")),
    avtale = uuidOrNull("avtale_id")?.let {
        DatavarehusTiltak.Avtale(
            id = it,
            navn = string("avtale_navn"),
            opprettetTidspunkt = localDateTime("avtale_opprettet_tidspunkt"),
            oppdatertTidspunkt = localDateTime("avtale_oppdatert_tidspunkt"),
        )
    },
    gjennomforing = DatavarehusTiltak.Gjennomforing(
        id = uuid("id"),
        navn = string("navn"),
        startDato = localDate("start_dato"),
        sluttDato = localDateOrNull("slutt_dato"),
        opprettetTidspunkt = localDateTime("opprettet_tidspunkt"),
        oppdatertTidspunkt = localDateTime("oppdatert_tidspunkt"),
        status = TiltaksgjennomforingStatus.valueOf(string("status")),
        arrangor = DatavarehusTiltak.Arrangor(
            organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
        ),
        arena = stringOrNull("tiltaksnummer")?.let {
            val tiltaksnummmer = Tiltaksnummer(it)
            DatavarehusTiltak.ArenaData(
                aar = tiltaksnummmer.aar,
                lopenummer = tiltaksnummmer.lopenummer,
            )
        },
    ),
)
