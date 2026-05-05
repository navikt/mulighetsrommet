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
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDetaljerDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingKontaktpersonDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingType
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

    data class Context(
        val today: LocalDate,
        val avtale: Avtale,
        val arrangor: ArrangorDto?,
        val previous: Gjennomforing? = null,
    ) {
        data class Gjennomforing(
            val arrangorId: UUID,
            val status: GjennomforingStatusType,
            val oppstart: GjennomforingOppstartstype,
            val pameldingType: GjennomforingPameldingType,
            val antallDeltakere: Int,
        )

        fun harEgenskap(vararg egenskap: TiltakstypeEgenskap): Boolean {
            return avtale.tiltakstype.tiltakskode.harEgenskap(*egenskap)
        }
    }

    data class CreateGjennomforingResult(
        val gjennomforing: GjennomforingDbo,
        val detaljer: DetaljerResult,
    )

    fun validateCreateGjennomforing(
        ctx: Context,
        id: UUID,
        request: GjennomforingDetaljerRequest,
    ): Validated<CreateGjennomforingResult> = validation {
        validate(ctx.arrangor != null && ctx.arrangor.slettetDato == null) {
            FieldError.of(
                "Arrangøren ${ctx.arrangor?.navn} er slettet i Brønnøysundregistrene. Gjennomføringer kan ikke opprettes for slettede bedrifter",
                GjennomforingDetaljerRequest::arrangorId,
            )
        }
        validate(request.startDato != null && request.startDato >= ctx.avtale.startDato) {
            FieldError.of(
                "Du må legge inn en startdato som er etter avtalens startdato",
                GjennomforingDetaljerRequest::startDato,
            )
        }
        validate(ctx.avtale.status == AvtaleStatus.Aktiv) {
            FieldError.of(
                "Avtalen må være aktiv for å kunne opprette tiltak",
                GjennomforingRequest::avtaleId,
            )
        }

        val result = validateDetaljer(ctx, id, request).bind()
        validate(result.detaljer.status == GjennomforingStatusType.GJENNOMFORES) {
            FieldError.of(
                "Du kan ikke opprette en gjennomføring med status ${result.detaljer.status.beskrivelse}",
                GjennomforingDetaljerRequest::navn,
            )
        }

        CreateGjennomforingResult(
            gjennomforing = GjennomforingDbo(
                id = id,
                type = GjennomforingType.AVTALE,
                tiltakstypeId = ctx.avtale.tiltakstype.id,
                avtaleId = ctx.avtale.id,
                arrangorId = result.detaljer.arrangorId,
                navn = result.detaljer.navn,
                startDato = result.detaljer.startDato,
                sluttDato = result.detaljer.sluttDato,
                status = result.detaljer.status,
                deltidsprosent = result.detaljer.deltidsprosent,
                antallPlasser = result.detaljer.antallPlasser,
                prismodellId = result.detaljer.prismodellId,
                oppstart = result.detaljer.oppstart,
                pameldingType = result.detaljer.pameldingType,
                ansvarligEnhet = null,
                arenaTiltaksnummer = null,
                arenaAnsvarligEnhet = null,
            ),
            detaljer = result,
        )
    }

    data class DetaljerResult(
        val detaljer: GjennomforingDetaljerDbo,
        val administratorer: Set<NavIdent>,
        val arrangorKontaktpersoner: Set<UUID>,
        val utdanningslop: UtdanningslopDbo?,
        val amoKategorisering: AmoKategorisering?,
    )

    fun validateUpdateDetaljer(
        ctx: Context,
        id: UUID,
        request: GjennomforingDetaljerRequest,
    ): Validated<DetaljerResult> = validation {
        requireNotNull(ctx.previous) {
            FieldError.of("Gjennomføring mangler i valideringskontekst")
        }

        validate(ctx.previous.status == GjennomforingStatusType.GJENNOMFORES) {
            FieldError.of(
                "Du kan ikke gjøre endringer på en gjennomføring med status ${ctx.previous.status.beskrivelse}",
                GjennomforingDetaljerRequest::navn,
            )
        }
        validate(request.arrangorId == ctx.previous.arrangorId) {
            FieldError.of(
                "Du kan ikke endre arrangør når gjennomføringen er aktiv",
                GjennomforingDetaljerRequest::arrangorId,
            )
        }
        validate(request.startDato != null && !request.startDato.isBefore(ctx.avtale.startDato)) {
            FieldError.of(
                "Du må legge inn en startdato som er etter avtalens startdato",
                GjennomforingDetaljerRequest::startDato,
            )
        }
        validate(request.sluttDato == null || request.sluttDato >= ctx.today) {
            FieldError.of(
                "Du kan ikke sette en sluttdato bakover i tid når gjennomføringen er aktiv",
                GjennomforingDetaljerRequest::sluttDato,
            )
        }
        validate(ctx.previous.antallDeltakere <= 0 || request.oppstart == ctx.previous.oppstart) {
            FieldError.of(
                "Oppstartstype kan ikke endres fordi det er deltakere koblet til gjennomføringen",
                GjennomforingDetaljerRequest::oppstart,
            )
        }
        validate(ctx.previous.antallDeltakere <= 0 || request.pameldingType == ctx.previous.pameldingType) {
            FieldError.of(
                "Påmeldingstype kan ikke endres fordi det er deltakere koblet til gjennomføringen",
                GjennomforingDetaljerRequest::pameldingType,
            )
        }

        validateDetaljer(ctx, id, request).bind()
    }

    private fun validateDetaljer(
        ctx: Context,
        id: UUID,
        request: GjennomforingDetaljerRequest,
    ): Validated<DetaljerResult> = validation {
        var detaljer = request

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
        validate(detaljer.sluttDato == null || (detaljer.startDato != null && detaljer.startDato <= detaljer.sluttDato)) {
            FieldError.of("Startdato må være før sluttdato", GjennomforingDetaljerRequest::startDato)
        }
        validate(detaljer.antallPlasser != null && detaljer.antallPlasser > 0) {
            FieldError.of(
                "Du må legge inn antall plasser større enn 0",
                GjennomforingDetaljerRequest::antallPlasser,
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
        validate(detaljer.oppmoteSted == null || detaljer.oppmoteSted.length <= MAKS_ANTALL_TEGN_OPPMOTE_STED) {
            FieldError.of(
                "Du kan bare skrive $MAKS_ANTALL_TEGN_OPPMOTE_STED tegn i \"Oppmøtested\"",
                GjennomforingDetaljerRequest::oppmoteSted,
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

        detaljer = validateOrResetTilgjengeligForArrangorDato(detaljer)

        val utdanningslop = validateUtdanningslop(ctx.avtale, detaljer.utdanningslop).bind()
        val amoKategorisering = validateAmoKategorisering(ctx.avtale, detaljer.amoKategorisering).bind()

        DetaljerResult(
            detaljer = GjennomforingDetaljerDbo(
                id = id,
                arrangorId = requireNotNull(ctx.arrangor?.id) {
                    FieldError.of("Du må velge en arrangør", GjennomforingDetaljerRequest::arrangorId)
                },
                oppstart = requireNotNull(detaljer.oppstart),
                pameldingType = requireNotNull(detaljer.pameldingType),
                navn = detaljer.navn,
                startDato = detaljer.startDato,
                sluttDato = detaljer.sluttDato,
                status = resolveStatus(ctx, detaljer),
                deltidsprosent = detaljer.deltidsprosent,
                antallPlasser = requireNotNull(detaljer.antallPlasser),
                prismodellId = detaljer.prismodellId,
                oppmoteSted = detaljer.oppmoteSted?.ifBlank { null },
                tilgjengeligForArrangorDato = detaljer.tilgjengeligForArrangorDato,
            ),
            administratorer = detaljer.administratorer,
            arrangorKontaktpersoner = detaljer.arrangorKontaktpersoner,
            utdanningslop = utdanningslop,
            amoKategorisering = amoKategorisering,
        )
    }

    data class VeilederinfoResult(
        val navEnheter: Set<NavEnhetNummer>,
        val kontaktpersoner: Set<GjennomforingKontaktpersonDbo>,
    )

    fun validateVeilederinfo(
        request: GjennomforingVeilederinfoRequest,
        avtale: Avtale,
        kontaktpersoner: List<NavAnsatt>,
    ): Validated<VeilederinfoResult> = validation(GjennomforingRequest::veilederinformasjon) {
        validateSlettetNavAnsatte(kontaktpersoner, GjennomforingVeilederinfoRequest::kontaktpersoner)

        val avtaleRegioner = avtale.kontorstruktur.map { it.region.enhetsnummer }
        val navRegioner = avtaleRegioner.intersect(request.navRegioner.toSet())
        validate(navRegioner.isNotEmpty()) {
            FieldError.of(
                "Du må velge minst én Nav-region fra avtalen",
                GjennomforingVeilederinfoRequest::navRegioner,
            )
        }

        val avtaleNavKontorer = avtale.kontorstruktur.flatMap { it.kontorer.map { kontor -> kontor.enhetsnummer } }
        val navKontorer = avtaleNavKontorer.intersect(request.navKontorer.toSet())
        val navAndreEnheter = avtaleNavKontorer.intersect(request.navAndreEnheter.toSet())
        validate((navKontorer + navAndreEnheter).isNotEmpty()) {
            FieldError.of(
                "Du må velge minst én Nav-enhet fra avtalen",
                GjennomforingVeilederinfoRequest::navKontorer,
            )
        }

        VeilederinfoResult(
            navEnheter = navRegioner + navKontorer + navAndreEnheter,
            kontaktpersoner = request.kontaktpersoner
                .map { GjennomforingKontaktpersonDbo(it.navIdent, it.beskrivelse) }
                .toSet(),
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

    private fun resolveStatus(
        ctx: Context,
        request: GjennomforingDetaljerRequest,
    ): GjennomforingStatusType {
        return when (ctx.previous?.status) {
            GjennomforingStatusType.AVLYST, GjennomforingStatusType.AVBRUTT -> ctx.previous.status

            else -> if (request.sluttDato == null || !request.sluttDato.isBefore(ctx.today)) {
                GjennomforingStatusType.GJENNOMFORES
            } else {
                GjennomforingStatusType.AVSLUTTET
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

    private fun FieldValidator.validateOrResetTilgjengeligForArrangorDato(detaljer: GjennomforingDetaljerRequest): GjennomforingDetaljerRequest {
        requireValid(detaljer.startDato != null)

        val nextTilgjengeligForArrangorDato = detaljer.tilgjengeligForArrangorDato?.let { date ->
            validateTilgjengeligForArrangorDato(date, detaljer.startDato).fold({ null }, { it })
        }

        return detaljer.copy(tilgjengeligForArrangorDato = nextTilgjengeligForArrangorDato)
    }
}
