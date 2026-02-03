package no.nav.mulighetsrommet.api.gjennomforing.service

import arrow.core.Either
import arrow.core.right
import no.nav.mulighetsrommet.api.amo.AmoKategoriseringRequest
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.avtale.mapper.toDbo
import no.nav.mulighetsrommet.api.avtale.model.Avtale
import no.nav.mulighetsrommet.api.avtale.model.AvtaleStatus
import no.nav.mulighetsrommet.api.gjennomforing.api.EstimertVentetid
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingRequest
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingVeilederinfoRequest
import no.nav.mulighetsrommet.api.gjennomforing.api.SetTilgjengligForArrangorRequest
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingGruppetiltakDbo
import no.nav.mulighetsrommet.api.gjennomforing.mapper.GjennomforingDboMapper
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.validation.FieldValidator
import no.nav.mulighetsrommet.api.validation.validation
import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.AmoKurstype
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.TiltakstypeEgenskap
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import java.time.LocalDate
import java.util.UUID
import kotlin.contracts.ExperimentalContracts
import kotlin.reflect.KProperty1

@OptIn(ExperimentalContracts::class)
object GjennomforingValidator {
    private const val MAKS_ANTALL_TEGN_OPPMOTE_STED = 500

    data class Ctx(
        val previous: Gjennomforing?,
        val avtale: Avtale,
        val arrangor: ArrangorDto?,
        val administratorer: List<NavAnsatt>,
        val kontaktpersoner: List<NavAnsatt>,
        val antallDeltakere: Int,
        val status: GjennomforingStatusType,
    ) {
        data class Gjennomforing(
            val arrangorId: UUID,
            val avtaleId: UUID?,
            val status: GjennomforingStatusType,
            val sluttDato: LocalDate?,
            val oppstart: GjennomforingOppstartstype,
            val pameldingType: GjennomforingPameldingType,
        )

        fun harEgenskap(vararg egenskap: TiltakstypeEgenskap): Boolean {
            return avtale.tiltakstype.tiltakskode.harEgenskap(*egenskap)
        }
    }

    fun validate(
        request: GjennomforingRequest,
        ctx: Ctx,
    ): Either<List<FieldError>, GjennomforingGruppetiltakDbo> = validation {
        var next = request

        validate(ctx.avtale.tiltakstype.id == next.tiltakstypeId) {
            FieldError.of(
                "Tiltakstypen må være den samme som for avtalen",
                GjennomforingRequest::tiltakstypeId,
            )
        }
        validate(next.administratorer.isNotEmpty()) {
            FieldError.of(
                "Du må velge minst én administrator",
                GjennomforingRequest::administratorer,
            )
        }
        validate(ctx.avtale.avtaletype == Avtaletype.FORHANDSGODKJENT || next.sluttDato != null) {
            FieldError.of(
                "Du må legge inn sluttdato for gjennomføringen",
                GjennomforingRequest::sluttDato,
            )
        }
        validate(next.startDato != null) {
            FieldError.of("Du må sette en startdato", GjennomforingRequest::startDato)
        }
        validate(next.sluttDato == null || (next.startDato != null && !next.startDato.isAfter(next.sluttDato))) {
            FieldError.of("Startdato må være før sluttdato", GjennomforingRequest::startDato)
        }
        validate(next.antallPlasser != null && next.antallPlasser > 0) {
            FieldError.of(
                "Du må legge inn antall plasser større enn 0",
                GjennomforingRequest::antallPlasser,
            )
        }
        validate(next.estimertVentetid == null || next.estimertVentetid.enhet != null) {
            FieldError.of(
                "Du må velge en enhet",
                GjennomforingRequest::estimertVentetid,
                EstimertVentetid::enhet,
            )
        }
        validate(next.estimertVentetid == null || (next.estimertVentetid.verdi != null && next.estimertVentetid.verdi > 0)) {
            FieldError.of(
                "Du må velge en verdi større enn 0",
                GjennomforingRequest::estimertVentetid,
                EstimertVentetid::verdi,
            )
        }
        if (ctx.harEgenskap(TiltakstypeEgenskap.KREVER_DELTIDSPROSENT)) {
            validate(request.deltidsprosent > 0 && request.deltidsprosent <= 100) {
                FieldError.of(
                    "Du må velge en deltidsprosent mellom 0 og 100",
                    GjennomforingRequest::deltidsprosent,
                )
            }
        }
        validateNotNull(request.oppstart) {
            FieldError.of(
                "Oppstartstype må være satt",
                GjennomforingRequest::oppstart,
            )
        }
        validateNotNull(request.pameldingType) {
            FieldError.of(
                "Påmeldingstype må være satt",
                GjennomforingRequest::pameldingType,
            )
        }
        validateNotNull(request.prismodellId) {
            FieldError.of(
                "Du må velge en prismodell fra avtalen",
                GjennomforingRequest::prismodellId,
            )
        }

        if (ctx.harEgenskap(
                TiltakstypeEgenskap.STOTTER_FELLES_OPPSTART,
                TiltakstypeEgenskap.STOTTER_LOPENDE_OPPSTART,
            )
        ) {
            if (request.oppstart == GjennomforingOppstartstype.FELLES) {
                validate(request.pameldingType == GjennomforingPameldingType.TRENGER_GODKJENNING) {
                    FieldError.of(
                        "Påmeldingstype kan ikke være “direkte vedtak” hvis oppstartstype er felles",
                        GjennomforingRequest::pameldingType,
                    )
                }
            }
        } else if (ctx.harEgenskap(TiltakstypeEgenskap.STOTTER_FELLES_OPPSTART)) {
            validate(next.oppstart == GjennomforingOppstartstype.FELLES) {
                FieldError.of("Tiltaket må ha felles oppstart", GjennomforingRequest::oppstart)
            }
        } else if (ctx.harEgenskap(TiltakstypeEgenskap.STOTTER_LOPENDE_OPPSTART)) {
            validate(next.oppstart == GjennomforingOppstartstype.LOPENDE) {
                FieldError.of("Tiltaket må ha løpende oppstart", GjennomforingRequest::oppstart)
            }
        }

        validate(ctx.avtale.arrangor?.underenheter?.any { it.id == next.arrangorId } ?: false) {
            FieldError.of("Du må velge en arrangør fra avtalen", GjennomforingRequest::arrangorId)
        }

        validateUtdanningslop(ctx.avtale.tiltakstype.tiltakskode, next.utdanningslop, ctx.avtale)
        validateNavEnheter(next.veilederinformasjon, ctx.avtale)
        validateSlettetNavAnsatte(ctx.kontaktpersoner, GjennomforingRequest::kontaktpersoner)
        validateSlettetNavAnsatte(ctx.administratorer, GjennomforingRequest::administratorer)
        validateNotNull(ctx.arrangor) {
            FieldError.of(
                "Du må velge en arrangør",
                GjennomforingRequest::arrangorId,
            )
        }
        val amoKategorisering = validateAmoKategorisering(ctx.avtale, request.amoKategorisering).bind()
        requireValid(ctx.arrangor != null)

        next = validateOrResetTilgjengeligForArrangorDato(next)

        if (ctx.previous == null) {
            validateCreateGjennomforing(ctx.arrangor, next, ctx.status, ctx.avtale)
        } else {
            validateUpdateGjennomforing(next, ctx.previous, ctx.avtale, ctx.antallDeltakere)
        }

        requireValid(next.antallPlasser != null && next.startDato != null && request.oppstart != null && request.pameldingType != null && request.prismodellId != null)
        GjennomforingDboMapper.fromGjennomforingRequest(
            request = next,
            startDato = next.startDato,
            antallPlasser = next.antallPlasser,
            arrangorId = ctx.arrangor.id,
            status = ctx.status,
            oppstartstype = request.oppstart,
            pameldingType = request.pameldingType,
            amoKategorisering = amoKategorisering,
            prismodellId = request.prismodellId,
        )
    }

    fun validateTilgjengeligForArrangorDato(
        tilgjengeligForArrangorDato: LocalDate?,
        startDato: LocalDate,
    ): Either<List<FieldError>, LocalDate> = validation {
        requireValid(tilgjengeligForArrangorDato != null) {
            FieldError.of("Dato må være satt", SetTilgjengligForArrangorRequest::tilgjengeligForArrangorDato)
        }

        validate(tilgjengeligForArrangorDato >= LocalDate.now()) {
            FieldError.of(
                "Du må velge en dato som er etter dagens dato",
                GjennomforingRequest::tilgjengeligForArrangorDato,
            )
        }

        validate(tilgjengeligForArrangorDato >= startDato.minusMonths(2)) {
            FieldError.of(
                "Du må velge en dato som er tidligst to måneder før gjennomføringens oppstartsdato",
                GjennomforingRequest::tilgjengeligForArrangorDato,
            )
        }

        validate(tilgjengeligForArrangorDato <= startDato) {
            FieldError.of(
                "Du må velge en dato som er før gjennomføringens oppstartsdato",
                GjennomforingRequest::tilgjengeligForArrangorDato,
            )
        }

        tilgjengeligForArrangorDato
    }

    private fun FieldValidator.validateUtdanningslop(
        tiltakskode: Tiltakskode,
        utdanningslop: UtdanningslopDbo?,
        avtale: Avtale,
    ) {
        when (tiltakskode) {
            Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
            Tiltakskode.ARBEIDSRETTET_REHABILITERING,
            Tiltakskode.AVKLARING,
            Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
            Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
            Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING,
            Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
            Tiltakskode.HOYERE_UTDANNING,
            Tiltakskode.JOBBKLUBB,
            Tiltakskode.OPPFOLGING,
            Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
            Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
            Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
            Tiltakskode.STUDIESPESIALISERING,
            Tiltakskode.HOYERE_YRKESFAGLIG_UTDANNING,
            -> return

            Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
            Tiltakskode.FAG_OG_YRKESOPPLAERING,
            -> Unit
        }

        requireValid(utdanningslop != null) {
            FieldError.of(
                "Du må velge utdanningsprogram og lærefag på avtalen",
                GjennomforingRequest::utdanningslop,
            )
        }
        validate(utdanningslop.utdanninger.isNotEmpty()) {
            FieldError.of(
                "Du må velge minst ett lærefag",
                GjennomforingRequest::utdanningslop,
            )
        }
        validate(utdanningslop.utdanningsprogram == avtale.utdanningslop?.utdanningsprogram?.id) {
            FieldError.of(
                "Utdanningsprogrammet må være det samme som for avtalen: ${avtale.utdanningslop?.utdanningsprogram?.navn}",
                GjennomforingRequest::utdanningslop,
            )
        }
        val avtalensUtdanninger = avtale.utdanningslop?.utdanninger?.map { it.id } ?: emptyList()
        validate(avtalensUtdanninger.containsAll(utdanningslop.utdanninger)) {
            FieldError.of(
                "Lærefag må være valgt fra avtalens lærefag, minst ett av lærefagene mangler i avtalen.",
                GjennomforingRequest::utdanningslop,
            )
        }
    }

    private fun FieldValidator.validateNavEnheter(
        veilederinfoRequest: GjennomforingVeilederinfoRequest,
        avtale: Avtale,
    ) {
        val avtaleRegioner = avtale.kontorstruktur.map { it.region.enhetsnummer }
        validate(avtaleRegioner.intersect(veilederinfoRequest.navRegioner.toSet()).isNotEmpty()) {
            FieldError.of(
                "Du må velge minst én Nav-region fra avtalen",
                GjennomforingRequest::veilederinformasjon,
                GjennomforingVeilederinfoRequest::navRegioner,
            )
        }

        val avtaleNavKontorer = avtale.kontorstruktur.flatMap { it.kontorer.map { kontor -> kontor.enhetsnummer } }
        val navKontorer = veilederinfoRequest.navKontorer.toSet()
        val navAndreEnheter = veilederinfoRequest.navAndreEnheter.toSet()
        validate(avtaleNavKontorer.intersect(navKontorer + navAndreEnheter).isNotEmpty()) {
            FieldError.of(
                "Du må velge minst én Nav-enhet fra avtalen",
                GjennomforingRequest::veilederinformasjon,
                GjennomforingVeilederinfoRequest::navKontorer,
            )
        }
        navKontorer.filterNot { it in avtaleRegioner || it in avtaleNavKontorer }.forEach { enhetsnummer ->
            error {
                FieldError.of(
                    "Nav-enhet $enhetsnummer mangler i avtalen",
                    GjennomforingRequest::veilederinformasjon,
                    GjennomforingVeilederinfoRequest::navKontorer,
                )
            }
        }
        veilederinfoRequest.navAndreEnheter.filterNot { it in avtaleRegioner || it in avtaleNavKontorer }
            .forEach { enhetsnummer ->
                error {
                    FieldError.of(
                        "Nav-enhet $enhetsnummer mangler i avtalen",
                        GjennomforingRequest::veilederinformasjon,
                        GjennomforingVeilederinfoRequest::navAndreEnheter,
                    )
                }
            }
    }

    private fun FieldValidator.validateSlettetNavAnsatte(
        navAnsatte: List<NavAnsatt>,
        property: KProperty1<*, *>,
    ) {
        val slettedeNavIdenter = navAnsatte.filter { it.skalSlettesDato != null }
        validate(slettedeNavIdenter.isEmpty()) {
            FieldError.of(
                "Nav identer " + slettedeNavIdenter.joinToString(", ") { it.navIdent.value } + " er slettet og må fjernes",
                property,
            )
        }
    }

    private fun FieldValidator.validateOrResetTilgjengeligForArrangorDato(request: GjennomforingRequest): GjennomforingRequest {
        requireValid(request.startDato != null)

        val nextTilgjengeligForArrangorDato = request.tilgjengeligForArrangorDato?.let { date ->
            validateTilgjengeligForArrangorDato(date, request.startDato).fold({ null }, { it })
        }

        return request.copy(tilgjengeligForArrangorDato = nextTilgjengeligForArrangorDato)
    }

    private fun FieldValidator.validateCreateGjennomforing(
        arrangor: ArrangorDto,
        gjennomforing: GjennomforingRequest,
        status: GjennomforingStatusType,
        avtale: Avtale,
    ) {
        validate(arrangor.slettetDato == null) {
            FieldError.of(
                "Arrangøren ${arrangor.navn} er slettet i Brønnøysundregistrene. Gjennomføringer kan ikke opprettes for slettede bedrifter",
                GjennomforingRequest::arrangorId,
            )
        }
        validate(gjennomforing.startDato != null && !gjennomforing.startDato.isBefore(avtale.startDato)) {
            FieldError.of(
                "Du må legge inn en startdato som er etter avtalens startdato",
                GjennomforingRequest::startDato,
            )
        }
        validate(gjennomforing.oppmoteSted == null || gjennomforing.oppmoteSted.length <= MAKS_ANTALL_TEGN_OPPMOTE_STED) {
            FieldError.of(
                "Du kan bare skrive $MAKS_ANTALL_TEGN_OPPMOTE_STED tegn i \"Oppmøtested\"",
                GjennomforingRequest::oppmoteSted,
            )
        }
        validate(avtale.status == AvtaleStatus.Aktiv) {
            FieldError.of(
                "Avtalen må være aktiv for å kunne opprette tiltak",
                GjennomforingRequest::avtaleId,
            )
        }
        validate(status == GjennomforingStatusType.GJENNOMFORES) {
            FieldError.of(
                "Du kan ikke opprette en gjennomføring med status ${status.beskrivelse}",
                GjennomforingRequest::navn,
            )
        }
    }

    private fun FieldValidator.validateUpdateGjennomforing(
        gjennomforing: GjennomforingRequest,
        previous: Ctx.Gjennomforing,
        avtale: Avtale,
        antallDeltakere: Int,
    ) {
        validate(previous.status == GjennomforingStatusType.GJENNOMFORES) {
            FieldError.of(
                "Du kan ikke gjøre endringer på en gjennomføring med status ${previous.status.beskrivelse}",
                GjennomforingRequest::navn,
            )
        }
        validate(gjennomforing.arrangorId == previous.arrangorId) {
            FieldError.of(
                "Du kan ikke endre arrangør når gjennomføringen er aktiv",
                GjennomforingRequest::arrangorId,
            )
        }
        if (previous.status == GjennomforingStatusType.GJENNOMFORES) {
            validate(gjennomforing.avtaleId == previous.avtaleId) {
                FieldError.of(
                    "Du kan ikke endre avtalen når gjennomføringen er aktiv",
                    GjennomforingRequest::avtaleId,
                )
            }
            validate(gjennomforing.startDato != null && !gjennomforing.startDato.isBefore(avtale.startDato)) {
                FieldError.of(
                    "Du må legge inn en startdato som er etter avtalens startdato",
                    GjennomforingRequest::startDato,
                )
            }
            validate(
                gjennomforing.sluttDato == null ||
                    previous.sluttDato == null ||
                    !gjennomforing.sluttDato.isBefore(LocalDate.now()),
            ) {
                FieldError.of(
                    "Du kan ikke sette en sluttdato bakover i tid når gjennomføringen er aktiv",
                    GjennomforingRequest::sluttDato,
                )
            }
        }
        validate(antallDeltakere <= 0 || gjennomforing.oppstart == previous.oppstart) {
            FieldError.of(
                "Oppstartstype kan ikke endres fordi det er deltakere koblet til gjennomføringen",
                GjennomforingRequest::oppstart,
            )
        }
        validate(antallDeltakere <= 0 || gjennomforing.pameldingType == previous.pameldingType) {
            FieldError.of(
                "Påmeldingstype kan ikke endres fordi det er deltakere koblet til gjennomføringen",
                GjennomforingRequest::pameldingType,
            )
        }
    }

    private fun FieldValidator.validateAmoKategorisering(
        avtale: Avtale,
        amoKategorisering: AmoKategoriseringRequest?,
    ): Either<List<FieldError>, AmoKategorisering?> {
        when (avtale.tiltakstype.tiltakskode) {
            Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
            Tiltakskode.ARBEIDSRETTET_REHABILITERING,
            Tiltakskode.AVKLARING,
            Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
            Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
            Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING,
            Tiltakskode.HOYERE_UTDANNING,
            Tiltakskode.JOBBKLUBB,
            Tiltakskode.OPPFOLGING,
            Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
            Tiltakskode.HOYERE_YRKESFAGLIG_UTDANNING,
            Tiltakskode.FAG_OG_YRKESOPPLAERING,
            Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
            -> Unit

            Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
            Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
            Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
            Tiltakskode.STUDIESPESIALISERING,
            -> {
                validate(avtale.amoKategorisering != null) {
                    FieldError(
                        "/avtale.amoKategorisering",
                        "Du må velge en kurstype for avtalen",
                    )
                }
            }
        }

        return when (avtale.tiltakstype.tiltakskode) {
            Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
            Tiltakskode.ARBEIDSRETTET_REHABILITERING,
            Tiltakskode.AVKLARING,
            Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
            Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
            Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING,
            Tiltakskode.HOYERE_UTDANNING,
            Tiltakskode.JOBBKLUBB,
            Tiltakskode.OPPFOLGING,
            Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
            Tiltakskode.HOYERE_YRKESFAGLIG_UTDANNING,
            Tiltakskode.FAG_OG_YRKESOPPLAERING,
            Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
            -> null

            Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING -> {
                requireValid(amoKategorisering?.kurstype != null) {
                    FieldError.of(
                        "Du må velge en kurstype",
                        GjennomforingRequest::amoKategorisering,
                        AmoKategoriseringRequest::kurstype,
                    )
                }
                if (amoKategorisering.kurstype == AmoKurstype.BRANSJE_OG_YRKESRETTET) {
                    requireValid(amoKategorisering.bransje != null) {
                        FieldError.of(
                            "Du må velge en bransje",
                            GjennomforingRequest::amoKategorisering,
                            AmoKategoriseringRequest::bransje,
                        )
                    }
                }
                amoKategorisering
            }

            Tiltakskode.ARBEIDSMARKEDSOPPLAERING -> {
                requireValid(amoKategorisering?.bransje != null) {
                    FieldError.of(
                        "Du må velge en bransje",
                        GjennomforingRequest::amoKategorisering,
                        AmoKategoriseringRequest::bransje,
                    )
                }
                amoKategorisering.copy(kurstype = AmoKurstype.BRANSJE_OG_YRKESRETTET)
            }

            Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV -> {
                requireValid(amoKategorisering?.kurstype != null) {
                    FieldError.of(
                        "Du må velge en kurstype",
                        GjennomforingRequest::amoKategorisering,
                        AmoKategoriseringRequest::kurstype,
                    )
                }
                amoKategorisering
            }

            Tiltakskode.STUDIESPESIALISERING,
            -> AmoKategoriseringRequest(kurstype = AmoKurstype.STUDIESPESIALISERING)
        }?.toDbo().right()
    }
}
