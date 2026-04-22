package no.nav.mulighetsrommet.api.gjennomforing.service

import arrow.core.Either
import no.nav.mulighetsrommet.api.amo.AmoKategoriseringRequest
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.avtale.mapper.toDbo
import no.nav.mulighetsrommet.api.avtale.model.Avtale
import no.nav.mulighetsrommet.api.avtale.model.AvtaleStatus
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingDetaljerRequest
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingRequest
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingVeilederinfoRequest
import no.nav.mulighetsrommet.api.gjennomforing.api.SetTilgjengligForArrangorRequest
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingKontaktpersonDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingType
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing.ArenaData
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.validation.FieldValidator
import no.nav.mulighetsrommet.api.validation.Validated
import no.nav.mulighetsrommet.api.validation.validation
import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.AmoKurstype
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
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
            val avtaleId: UUID,
            val status: GjennomforingStatusType,
            val sluttDato: LocalDate?,
            val oppstart: GjennomforingOppstartstype,
            val pameldingType: GjennomforingPameldingType,
            val arena: ArenaData?,
        )

        fun harEgenskap(vararg egenskap: TiltakstypeEgenskap): Boolean {
            return avtale.tiltakstype.tiltakskode.harEgenskap(*egenskap)
        }
    }

    data class GjennomforingAvtaleResult(
        val gjennomforing: GjennomforingDbo,
        val administratorer: Set<NavIdent>,
        val kontaktpersoner: Set<GjennomforingKontaktpersonDbo>,
        val arrangorKontaktpersoner: Set<UUID>,
    )

    fun validate(
        request: GjennomforingRequest,
        ctx: Ctx,
    ): Validated<GjennomforingAvtaleResult> = validation {
        var detaljer = request.detaljer

        validate(ctx.avtale.tiltakstype.id == request.tiltakstypeId) {
            FieldError.of(
                "Tiltakstypen må være den samme som for avtalen",
                GjennomforingRequest::tiltakstypeId,
            )
        }
        validate(detaljer.administratorer.isNotEmpty()) {
            FieldError.of(
                "Du må velge minst én administrator",
                GjennomforingDetaljerRequest::administratorer,
            )
        }
        validate(ctx.avtale.avtaletype == Avtaletype.FORHANDSGODKJENT || detaljer.sluttDato != null) {
            FieldError.of(
                "Du må legge inn sluttdato for gjennomføringen",
                GjennomforingDetaljerRequest::sluttDato,
            )
        }
        validate(detaljer.startDato != null) {
            FieldError.of("Du må sette en startdato", GjennomforingDetaljerRequest::startDato)
        }
        validate(detaljer.sluttDato == null || (detaljer.startDato != null && !detaljer.startDato.isAfter(detaljer.sluttDato))) {
            FieldError.of("Startdato må være før sluttdato", GjennomforingDetaljerRequest::startDato)
        }
        validate(detaljer.antallPlasser != null && detaljer.antallPlasser > 0) {
            FieldError.of(
                "Du må legge inn antall plasser større enn 0",
                GjennomforingDetaljerRequest::antallPlasser,
            )
        }
        validate(detaljer.estimertVentetid == null || detaljer.estimertVentetid.enhet != null) {
            FieldError.of(
                "Du må velge en enhet",
                GjennomforingDetaljerRequest::estimertVentetid,
                GjennomforingDetaljerRequest.EstimertVentetid::enhet,
            )
        }
        validate(detaljer.estimertVentetid == null || (detaljer.estimertVentetid.verdi != null && detaljer.estimertVentetid.verdi > 0)) {
            FieldError.of(
                "Du må velge en verdi større enn 0",
                GjennomforingDetaljerRequest::estimertVentetid,
                GjennomforingDetaljerRequest.EstimertVentetid::verdi,
            )
        }
        if (ctx.harEgenskap(TiltakstypeEgenskap.KREVER_DELTIDSPROSENT)) {
            validate(detaljer.deltidsprosent > 0 && detaljer.deltidsprosent <= 100) {
                FieldError.of(
                    "Du må velge en deltidsprosent mellom 0 og 100",
                    GjennomforingDetaljerRequest::deltidsprosent,
                )
            }
        }
        validateNotNull(detaljer.oppstart) {
            FieldError.of(
                "Oppstartstype må være satt",
                GjennomforingDetaljerRequest::oppstart,
            )
        }
        validateNotNull(detaljer.pameldingType) {
            FieldError.of(
                "Påmeldingstype må være satt",
                GjennomforingDetaljerRequest::pameldingType,
            )
        }
        validateNotNull(detaljer.prismodellId) {
            FieldError.of(
                "Du må velge en prismodell fra avtalen",
                GjennomforingDetaljerRequest::prismodellId,
            )
        }

        when (detaljer.oppstart) {
            GjennomforingOppstartstype.LOPENDE -> if (ctx.harEgenskap(TiltakstypeEgenskap.KREVER_DIREKTE_VEDTAK_FOR_LOPENDE_OPPSTART)) {
                validate(detaljer.pameldingType == GjennomforingPameldingType.DIREKTE_VEDTAK) {
                    FieldError.of(
                        "Påmeldingstype må være “direkte vedtak” når tiltaket har løpende oppstart (gjelder ${ctx.avtale.tiltakstype.navn})",
                        GjennomforingDetaljerRequest::pameldingType,
                    )
                }
            }

            GjennomforingOppstartstype.FELLES -> validate(detaljer.pameldingType == GjennomforingPameldingType.TRENGER_GODKJENNING) {
                FieldError.of(
                    "Påmeldingstype må være “trenger godkjenning” når tiltaket har felles oppstart",
                    GjennomforingDetaljerRequest::pameldingType,
                )
            }

            GjennomforingOppstartstype.ENKELTPLASS -> error {
                FieldError.of("Tiltaket støtter ikke enkeltplasser", GjennomforingDetaljerRequest::oppstart)
            }

            null -> error {
                FieldError.of("Oppstartstype er påkrevd", GjennomforingDetaljerRequest::oppstart)
            }
        }

        if (ctx.harEgenskap(TiltakstypeEgenskap.KREVER_DIREKTE_VEDTAK)) {
            validate(detaljer.oppstart == GjennomforingOppstartstype.LOPENDE) {
                FieldError.of("Tiltaket må ha løpende oppstart", GjennomforingDetaljerRequest::oppstart)
            }
            validate(detaljer.pameldingType == GjennomforingPameldingType.DIREKTE_VEDTAK) {
                FieldError.of("Påmeldingstype må være “direkte vedtak”", GjennomforingDetaljerRequest::pameldingType)
            }
        }

        validate(ctx.avtale.arrangor?.underenheter?.any { it.id == detaljer.arrangorId } ?: false) {
            FieldError.of("Du må velge en arrangør fra avtalen", GjennomforingDetaljerRequest::arrangorId)
        }

        validateSlettetNavAnsatte(ctx.kontaktpersoner, GjennomforingDetaljerRequest::kontaktpersoner)
        validateSlettetNavAnsatte(ctx.administratorer, GjennomforingDetaljerRequest::administratorer)
        requireValid(ctx.arrangor != null) {
            FieldError.of(
                "Du må velge en arrangør",
                GjennomforingDetaljerRequest::arrangorId,
            )
        }

        detaljer = validateOrResetTilgjengeligForArrangorDato(detaljer)

        if (ctx.previous == null) {
            validateCreateGjennomforing(ctx.arrangor, detaljer, ctx.status, ctx.avtale)
        } else {
            validateUpdateGjennomforing(detaljer, ctx.previous, ctx.avtale, ctx.antallDeltakere)
        }

        requireValid(detaljer.antallPlasser != null && detaljer.startDato != null && detaljer.oppstart != null && detaljer.pameldingType != null && detaljer.prismodellId != null)
        GjennomforingAvtaleResult(
            GjennomforingDbo(
                id = request.id,
                type = GjennomforingType.AVTALE,
                tiltakstypeId = request.tiltakstypeId,
                arrangorId = ctx.arrangor.id,
                navn = detaljer.navn,
                startDato = detaljer.startDato,
                sluttDato = detaljer.sluttDato,
                status = ctx.status,
                deltidsprosent = detaljer.deltidsprosent,
                antallPlasser = detaljer.antallPlasser,
                avtaleId = request.avtaleId,
                prismodellId = detaljer.prismodellId,
                oppstart = detaljer.oppstart,
                pameldingType = detaljer.pameldingType,
                oppmoteSted = detaljer.oppmoteSted?.ifBlank { null },
                faneinnhold = request.veilederinformasjon.faneinnhold,
                beskrivelse = request.veilederinformasjon.beskrivelse,
                estimertVentetidVerdi = detaljer.estimertVentetid?.verdi,
                estimertVentetidEnhet = detaljer.estimertVentetid?.enhet,
                tilgjengeligForArrangorDato = detaljer.tilgjengeligForArrangorDato,
                ansvarligEnhet = null,
                arenaTiltaksnummer = ctx.previous?.arena?.tiltaksnummer,
                arenaAnsvarligEnhet = ctx.previous?.arena?.ansvarligNavEnhet,
            ),
            detaljer.administratorer,
            detaljer.kontaktpersoner.map { GjennomforingKontaktpersonDbo(it.navIdent, it.beskrivelse) }.toSet(),
            detaljer.arrangorKontaktpersoner,
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
                GjennomforingDetaljerRequest::tilgjengeligForArrangorDato,
            )
        }

        validate(tilgjengeligForArrangorDato <= startDato) {
            FieldError.of(
                "Du må velge en dato som er før gjennomføringens oppstartsdato",
                GjennomforingDetaljerRequest::tilgjengeligForArrangorDato,
            )
        }

        tilgjengeligForArrangorDato
    }

    fun validateUtdanningslop(
        avtale: Avtale,
        utdanningslop: UtdanningslopDbo?,
    ): Validated<UtdanningslopDbo?> = validation {
        when (avtale.tiltakstype.tiltakskode) {
            Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
            Tiltakskode.FAG_OG_YRKESOPPLAERING,
            -> Unit

            else -> return@validation null
        }

        requireValid(utdanningslop != null) {
            FieldError.of(
                "Du må velge utdanningsprogram og lærefag på avtalen",
                GjennomforingDetaljerRequest::utdanningslop,
            )
        }
        validate(utdanningslop.utdanninger.isNotEmpty()) {
            FieldError.of(
                "Du må velge minst ett lærefag",
                GjennomforingDetaljerRequest::utdanningslop,
            )
        }
        validate(utdanningslop.utdanningsprogram == avtale.utdanningslop?.utdanningsprogram?.id) {
            FieldError.of(
                "Utdanningsprogrammet må være det samme som for avtalen: ${avtale.utdanningslop?.utdanningsprogram?.navn}",
                GjennomforingDetaljerRequest::utdanningslop,
            )
        }
        val avtalensUtdanninger = avtale.utdanningslop?.utdanninger?.map { it.id } ?: emptyList()
        validate(avtalensUtdanninger.containsAll(utdanningslop.utdanninger)) {
            FieldError.of(
                "Lærefag må være valgt fra avtalens lærefag, minst ett av lærefagene mangler i avtalen.",
                GjennomforingDetaljerRequest::utdanningslop,
            )
        }

        utdanningslop
    }

    fun validateNavEnheter(
        avtale: Avtale,
        veilederinfoRequest: GjennomforingVeilederinfoRequest,
    ): Validated<Set<NavEnhetNummer>> = validation(GjennomforingRequest::veilederinformasjon) {
        val avtaleRegioner = avtale.kontorstruktur.map { it.region.enhetsnummer }
        val navRegioner = avtaleRegioner.intersect(veilederinfoRequest.navRegioner.toSet())
        validate(navRegioner.isNotEmpty()) {
            FieldError.of(
                "Du må velge minst én Nav-region fra avtalen",
                GjennomforingVeilederinfoRequest::navRegioner,
            )
        }

        val avtaleNavKontorer = avtale.kontorstruktur.flatMap { it.kontorer.map { kontor -> kontor.enhetsnummer } }
        val navKontorer = avtaleNavKontorer.intersect(veilederinfoRequest.navKontorer.toSet())
        val navAndreEnheter = avtaleNavKontorer.intersect(veilederinfoRequest.navAndreEnheter.toSet())
        validate((navKontorer + navAndreEnheter).isNotEmpty()) {
            FieldError.of(
                "Du må velge minst én Nav-enhet fra avtalen",
                GjennomforingVeilederinfoRequest::navKontorer,
            )
        }

        navRegioner + navKontorer + navAndreEnheter
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

    private fun FieldValidator.validateOrResetTilgjengeligForArrangorDato(detaljer: GjennomforingDetaljerRequest): GjennomforingDetaljerRequest {
        requireValid(detaljer.startDato != null)

        val nextTilgjengeligForArrangorDato = detaljer.tilgjengeligForArrangorDato?.let { date ->
            validateTilgjengeligForArrangorDato(date, detaljer.startDato).fold({ null }, { it })
        }

        return detaljer.copy(tilgjengeligForArrangorDato = nextTilgjengeligForArrangorDato)
    }

    private fun FieldValidator.validateCreateGjennomforing(
        arrangor: ArrangorDto,
        gjennomforing: GjennomforingDetaljerRequest,
        status: GjennomforingStatusType,
        avtale: Avtale,
    ) {
        validate(arrangor.slettetDato == null) {
            FieldError.of(
                "Arrangøren ${arrangor.navn} er slettet i Brønnøysundregistrene. Gjennomføringer kan ikke opprettes for slettede bedrifter",
                GjennomforingDetaljerRequest::arrangorId,
            )
        }
        validate(gjennomforing.startDato != null && !gjennomforing.startDato.isBefore(avtale.startDato)) {
            FieldError.of(
                "Du må legge inn en startdato som er etter avtalens startdato",
                GjennomforingDetaljerRequest::startDato,
            )
        }
        validate(gjennomforing.oppmoteSted == null || gjennomforing.oppmoteSted.length <= MAKS_ANTALL_TEGN_OPPMOTE_STED) {
            FieldError.of(
                "Du kan bare skrive $MAKS_ANTALL_TEGN_OPPMOTE_STED tegn i \"Oppmøtested\"",
                GjennomforingDetaljerRequest::oppmoteSted,
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
                GjennomforingDetaljerRequest::navn,
            )
        }
    }

    private fun FieldValidator.validateUpdateGjennomforing(
        gjennomforing: GjennomforingDetaljerRequest,
        previous: Ctx.Gjennomforing,
        avtale: Avtale,
        antallDeltakere: Int,
    ) {
        validate(previous.status == GjennomforingStatusType.GJENNOMFORES) {
            FieldError.of(
                "Du kan ikke gjøre endringer på en gjennomføring med status ${previous.status.beskrivelse}",
                GjennomforingDetaljerRequest::navn,
            )
        }
        validate(gjennomforing.arrangorId == previous.arrangorId) {
            FieldError.of(
                "Du kan ikke endre arrangør når gjennomføringen er aktiv",
                GjennomforingDetaljerRequest::arrangorId,
            )
        }
        if (previous.status == GjennomforingStatusType.GJENNOMFORES) {
            validate(gjennomforing.startDato != null && !gjennomforing.startDato.isBefore(avtale.startDato)) {
                FieldError.of(
                    "Du må legge inn en startdato som er etter avtalens startdato",
                    GjennomforingDetaljerRequest::startDato,
                )
            }
            validate(gjennomforing.sluttDato == null || !gjennomforing.sluttDato.isBefore(LocalDate.now())) {
                FieldError.of(
                    "Du kan ikke sette en sluttdato bakover i tid når gjennomføringen er aktiv",
                    GjennomforingDetaljerRequest::sluttDato,
                )
            }
        }
        validate(antallDeltakere <= 0 || gjennomforing.oppstart == previous.oppstart) {
            FieldError.of(
                "Oppstartstype kan ikke endres fordi det er deltakere koblet til gjennomføringen",
                GjennomforingDetaljerRequest::oppstart,
            )
        }
        validate(antallDeltakere <= 0 || gjennomforing.pameldingType == previous.pameldingType) {
            FieldError.of(
                "Påmeldingstype kan ikke endres fordi det er deltakere koblet til gjennomføringen",
                GjennomforingDetaljerRequest::pameldingType,
            )
        }
    }

    fun validateAmoKategorisering(
        avtale: Avtale,
        amoKategorisering: AmoKategoriseringRequest?,
    ): Either<List<FieldError>, AmoKategorisering?> = validation {
        when (avtale.tiltakstype.tiltakskode) {
            Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
            Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
            Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
            Tiltakskode.STUDIESPESIALISERING,
            -> validate(avtale.amoKategorisering != null) {
                FieldError.of("Du må velge en kurstype for avtalen", GjennomforingRequest::avtaleId)
            }

            else -> Unit
        }

        when (avtale.tiltakstype.tiltakskode) {
            Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING -> {
                requireValid(amoKategorisering?.kurstype != null) {
                    FieldError.of(
                        "Du må velge en kurstype",
                        GjennomforingDetaljerRequest::amoKategorisering,
                        AmoKategoriseringRequest::kurstype,
                    )
                }
                if (amoKategorisering.kurstype == AmoKurstype.BRANSJE_OG_YRKESRETTET) {
                    requireValid(amoKategorisering.bransje != null) {
                        FieldError.of(
                            "Du må velge en bransje",
                            GjennomforingDetaljerRequest::amoKategorisering,
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
                        GjennomforingDetaljerRequest::amoKategorisering,
                        AmoKategoriseringRequest::bransje,
                    )
                }
                amoKategorisering.copy(kurstype = AmoKurstype.BRANSJE_OG_YRKESRETTET)
            }

            Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV -> {
                requireValid(amoKategorisering?.kurstype != null) {
                    FieldError.of(
                        "Du må velge en kurstype",
                        GjennomforingDetaljerRequest::amoKategorisering,
                        AmoKategoriseringRequest::kurstype,
                    )
                }
                amoKategorisering
            }

            Tiltakskode.STUDIESPESIALISERING,
            -> AmoKategoriseringRequest(kurstype = AmoKurstype.STUDIESPESIALISERING)

            else -> null
        }?.toDbo()
    }
}
