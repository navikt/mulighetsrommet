package no.nav.mulighetsrommet.api.datavarehus.db

import kotlinx.serialization.json.Json
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
    fun get(session: Session, id: UUID): DatavarehusTiltak {
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

        val dto = queryOf(query, id)
            .map { it.toDatavarehusTiltakDto() }
            .asSingle
            .runWithSession(session)
            .let { requireNotNull(it) { "Gjennomføring med id=$id finnes ikke" } }

        return when (dto.tiltakskode) {
            Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING -> {
                val utdanningslop = getUtdanningslop(session, id)
                DatavarehusTiltakYrkesfagDto(
                    dto.tiltakskode,
                    dto.avtale,
                    dto.gjennomforing,
                    dto.arrangor,
                    utdanningslop,
                )
            }

            Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING -> {
                val amoKategorisering = getAmoKategorisering(session, id)
                DatavarehusTiltakAmoDto(
                    dto.tiltakskode,
                    dto.avtale,
                    dto.gjennomforing,
                    dto.arrangor,
                    amoKategorisering,
                )
            }

            else -> dto
        }
    }

    private fun getUtdanningslop(session: Session, id: UUID): DatavarehusTiltakYrkesfagDto.Utdanningslop? {
        @Language("PostgreSQL")
        val utdanningsprogramQuery = """
            select program.id,
                   program.navn,
                   program.created_at as opprettet_tidspunkt,
                   program.updated_at as oppdatert_tidspunkt,
                   array_to_json(program.nus_koder) as nus_koder
            from tiltaksgjennomforing_utdanningsprogram
                    join utdanningsprogram program on utdanningsprogram_id = program.id
            where tiltaksgjennomforing_id = ?
            group by program.id
        """.trimIndent()

        val utdanningsprogram = queryOf(utdanningsprogramQuery, id)
            .map {
                DatavarehusTiltakYrkesfagDto.Utdanningslop.Utdanningsprogram(
                    id = it.uuid("id"),
                    navn = it.string("navn"),
                    opprettetTidspunkt = it.localDateTime("opprettet_tidspunkt"),
                    oppdatertTidspunkt = it.localDateTime("oppdatert_tidspunkt"),
                    nusKoder = Json.decodeFromString(it.string("nus_koder")),
                )
            }
            .asSingle
            .runWithSession(session)

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
                   jsonb_agg(utdanning_nus_kode.nus_kode) as nus_koder
            from tiltaksgjennomforing_utdanningsprogram
                    join utdanning on tiltaksgjennomforing_utdanningsprogram.utdanning_id = utdanning.id
                    left join utdanning_nus_kode on utdanning.utdanning_id = utdanning_nus_kode.utdanning_id
            where tiltaksgjennomforing_id = ?
            group by utdanning.id;
        """.trimIndent()

        val utdanninger = queryOf(utdanningerQuery, id)
            .map {
                DatavarehusTiltakYrkesfagDto.Utdanningslop.Utdanning(
                    id = it.uuid("id"),
                    navn = it.string("navn"),
                    sluttkompetanse = Utdanning.Sluttkompetanse.valueOf(it.string("sluttkompetanse")),
                    opprettetTidspunkt = it.localDateTime("opprettet_tidspunkt"),
                    oppdatertTidspunkt = it.localDateTime("oppdatert_tidspunkt"),
                    nusKoder = Json.decodeFromString(it.string("nus_koder")),
                )
            }
            .asList
            .runWithSession(session)
            .toSet()

        return DatavarehusTiltakYrkesfagDto.Utdanningslop(utdanningsprogram, utdanninger)
    }

    private fun getAmoKategorisering(session: Session, id: UUID): AmoKategorisering? {
        @Language("PostgreSQL")
        val sertifiseringQuery = """
            select s.label,
                   s.konsept_id
            from tiltaksgjennomforing_amo_kategorisering_sertifisering k
                     join amo_sertifisering s on k.konsept_id = s.konsept_id
            where k.tiltaksgjennomforing_id = ?
        """.trimIndent()

        val sertifiseringer = queryOf(sertifiseringQuery, id)
            .map {
                AmoKategorisering.BransjeOgYrkesrettet.Sertifisering(
                    konseptId = it.long("konsept_id"),
                    label = it.string("label"),
                )
            }
            .asList
            .runWithSession(session)

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

        return queryOf(amoKategoriseringQuery, id)
            .map { it.toAmoKategorisering(sertifiseringer) }
            .asSingle
            .runWithSession(session)
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
            arena = stringOrNull("tiltaksnummer")?.let {
                val tiltaksnummmer = Tiltaksnummer(it)
                DatavarehusTiltak.ArenaData(
                    aar = tiltaksnummmer.aar,
                    lopenummer = tiltaksnummmer.lopenummer,
                )
            },
        ),
        arrangor = DatavarehusTiltak.Arrangor(
            organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
        ),
    )
}
