package no.nav.mulighetsrommet.api.gjennomforing.mapper

import no.nav.mulighetsrommet.api.amo.AmoKategoriseringRequest
import no.nav.mulighetsrommet.api.avtale.model.UtdanningslopDto
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingDetaljerRequest
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingRequest
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingVeilederinfoRequest
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtaleDetaljer
import no.nav.mulighetsrommet.api.navenhet.Kontorstruktur
import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.AmoKurstype
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo

object GjennomforingRequestMapper {
    fun fromGjennomforing(
        gjennomforing: GjennomforingAvtale,
        detaljer: GjennomforingAvtaleDetaljer,
    ): GjennomforingRequest = GjennomforingRequest(
        id = gjennomforing.id,
        tiltakstypeId = gjennomforing.tiltakstype.id,
        avtaleId = gjennomforing.avtaleId,
        detaljer = GjennomforingDetaljerRequest(
            navn = gjennomforing.navn,
            startDato = gjennomforing.startDato,
            sluttDato = gjennomforing.sluttDato,
            antallPlasser = gjennomforing.antallPlasser,
            arrangorId = gjennomforing.arrangor.id,
            arrangorKontaktpersoner = detaljer.arrangorKontaktpersoner.map { it.id }.toSet(),
            kontaktpersoner = detaljer.kontaktpersoner.map { it.toGjennomforingRequestKontaktperson() }.toSet(),
            administratorer = detaljer.administratorer.map { it.navIdent }.toSet(),
            oppstart = gjennomforing.oppstart,
            oppmoteSted = detaljer.oppmoteSted,
            deltidsprosent = gjennomforing.deltidsprosent,
            estimertVentetid = detaljer.estimertVentetid?.toEstimertVentetid(),
            tilgjengeligForArrangorDato = detaljer.tilgjengeligForArrangorDato,
            amoKategorisering = detaljer.amoKategorisering?.toAmoKategorieringRequest(),
            utdanningslop = detaljer.utdanningslop?.toUtdanningslopDbo(),
            prismodellId = gjennomforing.prismodell.id,
            pameldingType = gjennomforing.pameldingType,
        ),
        veilederinformasjon = detaljer.toGjennomforingVeilederinfoRequest(),
    )

    private fun GjennomforingAvtaleDetaljer.EstimertVentetid.toEstimertVentetid(): GjennomforingDetaljerRequest.EstimertVentetid {
        return GjennomforingDetaljerRequest.EstimertVentetid(verdi, enhet)
    }

    private fun GjennomforingAvtaleDetaljer.toGjennomforingVeilederinfoRequest(): GjennomforingVeilederinfoRequest {
        return GjennomforingVeilederinfoRequest(
            navRegioner = kontorstruktur.map { it.region.enhetsnummer }.toSet(),
            navKontorer = kontorstruktur.flatMap { region ->
                region.kontorer.filter { it.type == Kontorstruktur.Kontortype.LOKAL }.map { it.enhetsnummer }
            }.toSet(),
            navAndreEnheter = kontorstruktur.flatMap { region ->
                region.kontorer.filter { it.type == Kontorstruktur.Kontortype.SPESIALENHET }.map { it.enhetsnummer }
            }.toSet(),
            beskrivelse = beskrivelse,
            faneinnhold = faneinnhold,
        )
    }

    private fun GjennomforingAvtaleDetaljer.GjennomforingKontaktperson.toGjennomforingRequestKontaktperson(): GjennomforingDetaljerRequest.Kontaktperson {
        return GjennomforingDetaljerRequest.Kontaktperson(navIdent, beskrivelse)
    }

    private fun AmoKategorisering.toAmoKategorieringRequest(): AmoKategoriseringRequest? {
        return when (this) {
            is AmoKategorisering.BransjeOgYrkesrettet -> AmoKategoriseringRequest(
                kurstype = AmoKurstype.BRANSJE_OG_YRKESRETTET,
                bransje = bransje,
                sertifiseringer = sertifiseringer,
                forerkort = forerkort,
                innholdElementer = innholdElementer,
            )

            is AmoKategorisering.Norskopplaering -> AmoKategoriseringRequest(
                kurstype = AmoKurstype.NORSKOPPLAERING,
                norskprove = norskprove,
                innholdElementer = innholdElementer,
            )

            is AmoKategorisering.GrunnleggendeFerdigheter -> AmoKategoriseringRequest(
                kurstype = AmoKurstype.GRUNNLEGGENDE_FERDIGHETER,
                innholdElementer = innholdElementer,
            )

            is AmoKategorisering.ForberedendeOpplaeringForVoksne -> AmoKategoriseringRequest(
                kurstype = AmoKurstype.FORBEREDENDE_OPPLAERING_FOR_VOKSNE,
                innholdElementer = innholdElementer,
            )

            is AmoKategorisering.Studiespesialisering -> AmoKategoriseringRequest(
                kurstype = AmoKurstype.STUDIESPESIALISERING,
            )
        }
    }

    private fun UtdanningslopDto.toUtdanningslopDbo(): UtdanningslopDbo {
        return UtdanningslopDbo(utdanningsprogram.id, utdanninger.map { it.id }.toSet())
    }
}
