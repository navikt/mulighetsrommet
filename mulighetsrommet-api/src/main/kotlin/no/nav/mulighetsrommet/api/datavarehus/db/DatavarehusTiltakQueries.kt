package no.nav.mulighetsrommet.api.datavarehus.db

import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.amo.AmoKategorisering
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
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
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
        val amoKategoriseringQuery = """
            select
              coalesce(gak.norskprove, false) as norskprove,
              coalesce(gak.innhold_elementer, '{}') as innhold_elementer,
              'kurstype', (select jsonb_strip_nulls(
                            jsonb_build_object(
                               'id', okk.id,
                               'navn', okk.navn,
                               'kode', okk.kode,
                               'aktiv', okk.aktiv
                            )
                        )
                        from opplaring_kategorisering_kurstype okk
                        where okk.id = gak.kurstype_id),
               'bransje', (select jsonb_strip_nulls(
                                      jsonb_build_object(
                                              'id', okb.id,
                                              'navn', okb.navn,
                                              'kode', okb.kode
                                      )
                              )
                       from opplaring_kategorisering_bransje okb
                       where okb.id = gak.bransje_id),
               'forerkort', coalesce(
                   (select jsonb_strip_nulls(
                                   jsonb_agg(
                                           jsonb_build_object(
                                                   'id', okf.id,
                                                   'navn', okf.navn,
                                                   'kode', okf.kode
                                           )
                                   )
                           )
                    from opplaring_kategorisering_forerkort okf
                             join gjennomforing_amo_kategorisering_forerkort gokf on gokf.forerkort_id = okf.id
                    where gokf.gjennomforing_id = gak.gjennomforing_id),
                   '[]'::jsonb),
                'sertifiseringer',
                       coalesce((select jsonb_strip_nulls(
                                                jsonb_agg(
                                                        jsonb_build_object(
                                                                'label', s.label,
                                                                'konseptId', s.konsept_id
                                                        )
                                                ))
                                 from amo_sertifisering s
                                          join gjennomforing_amo_kategorisering_sertifisering gaks
                                               on gaks.konsept_id = s.konsept_id
                                 where gaks.gjennomforing_id = gak.gjennomforing_id),
                                '[]'::jsonb)
            from gjennomforing_amo_kategorisering gak
            where gak.gjennomforing_id = ?;
        """.trimIndent()

        return session.single(queryOf(amoKategoriseringQuery, id)) { it.toAmoKategorisering() }
    }
}

private fun Row.toAmoKategorisering(): AmoKategorisering {
    val kurstype = string("kurstype").let { JsonIgnoreUnknownKeys.decodeFromString<Kurstype>(it) }
    return when (kurstype.kode) {
        Kurstype.Kode.BRANSJE_OG_YRKESRETTET -> AmoKategorisering.BransjeOgYrkesrettet(
            bransje = AmoKategorisering.BransjeOgYrkesrettet.Bransje.valueOf(kurstype.kode.toString()),
            sertifiseringer = array<Sertifisering>("sertifisering").toList(),
            forerkort = array<ForerkortKlasse>("forerkort")
                .toList()
                .map { AmoKategorisering.BransjeOgYrkesrettet.ForerkortKlasse.valueOf(it.kode.toString()) },
            innholdElementer = array<String>("innhold_elementer")
                .toList()
                .map { AmoKategorisering.InnholdElement.valueOf(it) },
        )

        Kurstype.Kode.NORSKOPPLAERING -> AmoKategorisering.Norskopplaering(
            norskprove = stringOrNull("norskprove").toBoolean(),
            innholdElementer = array<String>("innhold_elementer")
                .toList()
                .map { AmoKategorisering.InnholdElement.valueOf(it) },
        )

        Kurstype.Kode.GRUNNLEGGENDE_FERDIGHETER -> AmoKategorisering.GrunnleggendeFerdigheter(
            innholdElementer = array<String>("innhold_elementer")
                .toList()
                .map { AmoKategorisering.InnholdElement.valueOf(it) },
        )

        Kurstype.Kode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE -> AmoKategorisering.ForberedendeOpplaeringForVoksne(
            innholdElementer = array<String>("innhold_elementer")
                .toList()
                .map { AmoKategorisering.InnholdElement.valueOf(it) },
        )

        Kurstype.Kode.STUDIESPESIALISERING -> AmoKategorisering.Studiespesialisering
    }
}

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
