package no.nav.mulighetsrommet.api.datavarehus.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.amo.AmoKategorisering
import no.nav.mulighetsrommet.api.amo.OpplaringKategorisering
import no.nav.mulighetsrommet.api.amo.db.OpplaringKategoriseringQueries
import no.nav.mulighetsrommet.api.amo.models.Kurstype
import no.nav.mulighetsrommet.api.avtale.model.UtdanningslopDto
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakV1
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakV1AmoDto
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakV1Dto
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakV1YrkesfagDto
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
            Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
            -> {
                val kategorisering = context(this.session) {
                    OpplaringKategoriseringQueries.get(
                        id,
                    )
                }
                DatavarehusTiltakV1YrkesfagDto(
                    dto.tiltakskode,
                    dto.avtale,
                    dto.gjennomforing,
                    kategorisering?.utdanningslop?.toDatavarehusUtdanningslop(),
                )
            }

            Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
            -> {
                val kategorisering = context(this.session) {
                    OpplaringKategoriseringQueries.get(
                        id,
                    )
                }
                DatavarehusTiltakV1AmoDto(
                    dto.tiltakskode,
                    dto.avtale,
                    dto.gjennomforing,
                    kategorisering?.toAmoKategorisering(id),
                )
            }

            else -> dto
        }
    }
}

private fun UtdanningslopDto.toDatavarehusUtdanningslop() = DatavarehusTiltakV1YrkesfagDto.Utdanningslop(utdanningsprogram.id, utdanninger.map { it.id }.toSet())

private fun OpplaringKategorisering.toAmoKategorisering(id: UUID): AmoKategorisering {
    val mappedInnholdsElementer = innholdElementer.map { AmoKategorisering.InnholdElement.valueOf(it.kode.name) }
    requireNotNull(kurstype) {
        "Kurstype kan ikke være null for amo kategorisering gjennomforing_id=$id"
    }
    return when (kurstype.kode) {
        Kurstype.Kode.BRANSJE_OG_YRKESRETTET -> {
            requireNotNull(bransje) {
                "Bransje kan ikke være null for kurstype BRANSJE_OG_YRKESRETTET gjennomforing_id=$id"
            }
            AmoKategorisering.BransjeOgYrkesrettet(
                bransje = bransje.let { AmoKategorisering.BransjeOgYrkesrettet.Bransje.valueOf(it.kode.toString()) },
                sertifiseringer = sertifiseringer.toList(),
                forerkort = forerkort
                    .map { AmoKategorisering.BransjeOgYrkesrettet.ForerkortKlasse.valueOf(it.kode.toString()) },
                innholdElementer = mappedInnholdsElementer,
            )
        }

        Kurstype.Kode.NORSKOPPLAERING -> AmoKategorisering.Norskopplaering(
            norskprove = norskprove ?: false,
            innholdElementer = mappedInnholdsElementer,
        )

        Kurstype.Kode.GRUNNLEGGENDE_FERDIGHETER -> AmoKategorisering.GrunnleggendeFerdigheter(
            innholdElementer = mappedInnholdsElementer,
        )

        Kurstype.Kode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE -> AmoKategorisering.ForberedendeOpplaeringForVoksne(
            innholdElementer = mappedInnholdsElementer,
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
