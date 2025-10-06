package no.nav.mulighetsrommet.api.gjennomforing.service

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.right
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.avtale.model.Avtale
import no.nav.mulighetsrommet.api.avtale.model.AvtaleStatus
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingRequest
import no.nav.mulighetsrommet.api.gjennomforing.api.SetTilgjengligForArrangorRequest
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.mapper.GjennomforingDboMapper
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.validation.ValidationDsl
import no.nav.mulighetsrommet.api.validation.validation
import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltakskoder
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import java.time.LocalDate
import java.util.UUID
import kotlin.contracts.ExperimentalContracts
import kotlin.reflect.KProperty1

@OptIn(ExperimentalContracts::class)
object GjennomforingValidator {
    private const val MAKS_ANTALL_TEGN_STED_FOR_GJENNOMFORING = 100

    data class Ctx(
        val previous: Gjennomforing?,
        val avtale: Avtale,
        val arrangor: ArrangorDto,
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
        )
    }

    fun validate(
        request: GjennomforingRequest,
        ctx: Ctx,
    ): Either<List<FieldError>, GjennomforingDbo> = validation {
        var next = request

        validate(ctx.avtale.tiltakstype.id == next.tiltakstypeId) {
            FieldError.of(
                "Tiltakstypen må være den samme som for avtalen",
                GjennomforingDbo::tiltakstypeId,
            )
        }
        validate(next.administratorer.isNotEmpty()) {
            FieldError.of(
                "Du må velge minst én administrator",
                GjennomforingDbo::administratorer,
            )
        }
        validate(ctx.avtale.avtaletype == Avtaletype.FORHANDSGODKJENT || next.sluttDato != null) {
            FieldError.of(
                "Du må legge inn sluttdato for gjennomføringen",
                GjennomforingDbo::sluttDato,
            )
        }
        validate(next.sluttDato == null || !next.startDato.isAfter(next.sluttDato)) {
            FieldError.of("Startdato må være før sluttdato", GjennomforingDbo::startDato)
        }
        validate(next.antallPlasser > 0) {
            FieldError.of(
                "Du må legge inn antall plasser større enn 0",
                GjennomforingDbo::antallPlasser,
            )
        }
        if (Tiltakskoder.isKursTiltak(ctx.avtale.tiltakstype.tiltakskode)) {
            validateKursTiltak(next)
        } else {
            validate(next.oppstart != GjennomforingOppstartstype.FELLES) {
                FieldError.of(
                    "Tiltaket må ha løpende oppstartstype",
                    GjennomforingDbo::oppstart,
                )
            }
        }

        validate(ctx.avtale.arrangor?.underenheter?.any { it.id == next.arrangorId } ?: false) {
            FieldError.of("Du må velge en arrangør fra avtalen", GjennomforingDbo::arrangorId)
        }

        if (ctx.avtale.tiltakstype.tiltakskode == Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING) {
            validateGruppeAMO(next.amoKategorisering, ctx.avtale)
        }
        if (ctx.avtale.tiltakstype.tiltakskode == Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING) {
            validateGruppeFagOgYrke(next.utdanningslop, ctx.avtale)
        }
        validateNavEnheter(next.navEnheter, ctx.avtale)
        validateSlettetNavAnsatte(ctx.kontaktpersoner, GjennomforingDbo::kontaktpersoner)
        validateSlettetNavAnsatte(ctx.administratorer, GjennomforingDbo::administratorer)

        next = validateOrResetTilgjengeligForArrangorDato(next)

        if (ctx.previous == null) {
            validateCreateGjennomforing(ctx.arrangor, next, ctx.status, ctx.avtale)
        } else {
            validateUpdateGjennomforing(next, ctx.previous, ctx.avtale, ctx.antallDeltakere)
        }

        GjennomforingDboMapper.fromGjennomforingRequest(next, ctx.status)
    }

    private fun ValidationDsl.validateGruppeAMO(
        amoKategorisering: AmoKategorisering?,
        avtale: Avtale,
    ) {
        validate(avtale.amoKategorisering != null) {
            FieldError(
                "/avtale.amoKategorisering",
                "Du må velge en kurstype for avtalen",
            )
        }
        validate(amoKategorisering != null) {
            FieldError.of(
                "Du må velge et kurselement for gjennomføringen",
                GjennomforingDbo::amoKategorisering,
            )
        }
    }

    private fun ValidationDsl.validateGruppeFagOgYrke(
        utdanningslop: UtdanningslopDbo?,
        avtale: Avtale,
    ) {
        requireValid(utdanningslop != null) {
            FieldError.of(
                "Du må velge utdanningsprogram og lærefag på avtalen",
                GjennomforingDbo::utdanningslop,
            )
        }
        validate(utdanningslop.utdanninger.isNotEmpty()) {
            FieldError.of(
                "Du må velge minst ett lærefag",
                GjennomforingDbo::utdanningslop,
            )
        }
        validate(utdanningslop.utdanningsprogram == avtale.utdanningslop?.utdanningsprogram?.id) {
            FieldError.of(
                "Utdanningsprogrammet må være det samme som for avtalen: ${avtale.utdanningslop?.utdanningsprogram?.navn}",
                GjennomforingDbo::utdanningslop,
            )
        }
        val avtalensUtdanninger = avtale.utdanningslop?.utdanninger?.map { it.id } ?: emptyList()
        validate(avtalensUtdanninger.containsAll(utdanningslop.utdanninger)) {
            FieldError.of(
                "Lærefag må være valgt fra avtalens lærefag, minst ett av lærefagene mangler i avtalen.",
                GjennomforingDbo::utdanningslop,
            )
        }
    }

    private fun ValidationDsl.validateNavEnheter(
        navEnheter: Set<NavEnhetNummer>,
        avtale: Avtale,
    ) {
        val avtaleRegioner = avtale.kontorstruktur.map { it.region.enhetsnummer }
        validate(avtaleRegioner.intersect(navEnheter).isNotEmpty()) {
            FieldError.of(
                "Du må velge minst én Nav-region fra avtalen",
                GjennomforingDbo::navEnheter,
            )
        }

        val avtaleNavKontorer = avtale.kontorstruktur.flatMap { it.kontorer.map { kontor -> kontor.enhetsnummer } }
        validate(avtaleNavKontorer.intersect(navEnheter).isNotEmpty()) {
            FieldError.of(
                "Du må velge minst én Nav-enhet fra avtalen",
                GjennomforingDbo::navEnheter,
            )
        }
        navEnheter.filterNot { it in avtaleRegioner || it in avtaleNavKontorer }.forEach { enhetsnummer ->
            validate(false) {
                FieldError.of("Nav-enhet $enhetsnummer mangler i avtalen", GjennomforingDbo::navEnheter)
            }
        }
    }

    private fun ValidationDsl.validateSlettetNavAnsatte(
        navAnsatte: List<NavAnsatt>,
        property: KProperty1<*, *>,
    ) {
        val slettedeNavIdenter = navAnsatte
            .filter { it.skalSlettesDato != null }

        validate(slettedeNavIdenter.isEmpty()) {
            FieldError.of(
                "Nav identer " + slettedeNavIdenter.joinToString(", ") { it.navIdent.value } + " er slettet og må fjernes",
                property,
            )
        }
    }

    private fun validateOrResetTilgjengeligForArrangorDato(
        next: GjennomforingRequest,
    ): GjennomforingRequest {
        val nextTilgjengeligForArrangorDato = next.tilgjengeligForArrangorDato?.let { date ->
            validateTilgjengeligForArrangorDato(date, next.startDato).fold({ null }, { it })
        }
        return next.copy(tilgjengeligForArrangorDato = nextTilgjengeligForArrangorDato)
    }

    fun validateTilgjengeligForArrangorDato(
        tilgjengeligForArrangorDato: LocalDate?,
        startDato: LocalDate,
    ): Either<List<FieldError>, LocalDate> {
        if (tilgjengeligForArrangorDato == null) {
            return FieldError.of("Dato må være satt", SetTilgjengligForArrangorRequest::tilgjengeligForArrangorDato)
                .nel().left()
        }

        val errors = buildList {
            if (tilgjengeligForArrangorDato < LocalDate.now()) {
                add(
                    FieldError.of(
                        "Du må velge en dato som er etter dagens dato",
                        GjennomforingDbo::tilgjengeligForArrangorDato,
                    ),
                )
            } else if (tilgjengeligForArrangorDato < startDato.minusMonths(2)) {
                add(
                    FieldError.of(
                        "Du må velge en dato som er tidligst to måneder før gjennomføringens oppstartsdato",
                        GjennomforingDbo::tilgjengeligForArrangorDato,
                    ),
                )
            }

            if (tilgjengeligForArrangorDato > startDato) {
                add(
                    FieldError.of(
                        "Du må velge en dato som er før gjennomføringens oppstartsdato",
                        GjennomforingDbo::tilgjengeligForArrangorDato,
                    ),
                )
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: tilgjengeligForArrangorDato.right()
    }

    private fun ValidationDsl.validateCreateGjennomforing(
        arrangor: ArrangorDto,
        gjennomforing: GjennomforingRequest,
        status: GjennomforingStatusType,
        avtale: Avtale,
    ) {
        validate(arrangor.slettetDato == null) {
            FieldError.of(
                "Arrangøren ${arrangor.navn} er slettet i Brønnøysundregistrene. Gjennomføringer kan ikke opprettes for slettede bedrifter.",
                GjennomforingDbo::arrangorId,
            )
        }
        validate(!gjennomforing.startDato.isBefore(avtale.startDato)) {
            FieldError.of(
                "Du må legge inn en startdato som er etter avtalens startdato",
                GjennomforingDbo::startDato,
            )
        }
        validate(gjennomforing.stedForGjennomforing == null || gjennomforing.stedForGjennomforing.length <= MAKS_ANTALL_TEGN_STED_FOR_GJENNOMFORING) {
            FieldError.of(
                "Du kan bare skrive $MAKS_ANTALL_TEGN_STED_FOR_GJENNOMFORING tegn i \"Sted for gjennomføring\"",
                GjennomforingDbo::stedForGjennomforing,
            )
        }
        validate(avtale.status == AvtaleStatus.Aktiv) {
            FieldError.of(
                "Avtalen må være aktiv for å kunne opprette tiltak",
                GjennomforingDbo::avtaleId,
            )
        }
        validate(status == GjennomforingStatusType.GJENNOMFORES) {
            FieldError.of(
                "Du kan ikke opprette en gjennomføring som er ${status.name.lowercase()}",
                GjennomforingDbo::navn,
            )
        }
    }

    private fun ValidationDsl.validateUpdateGjennomforing(
        gjennomforing: GjennomforingRequest,
        previous: Ctx.Gjennomforing,
        avtale: Avtale,
        antallDeltakere: Int,
    ) {
        validate(previous.status == GjennomforingStatusType.GJENNOMFORES) {
            FieldError.of(
                "Du kan ikke gjøre endringer på en gjennomføring som er ${previous.status.name.lowercase()}",
                GjennomforingDbo::navn,
            )
        }
        validate(gjennomforing.arrangorId == previous.arrangorId) {
            FieldError.of(
                "Du kan ikke endre arrangør når gjennomføringen er aktiv",
                GjennomforingDbo::arrangorId,
            )
        }
        if (previous.status == GjennomforingStatusType.GJENNOMFORES) {
            validate(gjennomforing.avtaleId == previous.avtaleId) {
                FieldError.of(
                    "Du kan ikke endre avtalen når gjennomføringen er aktiv",
                    GjennomforingDbo::avtaleId,
                )
            }
            validate(!gjennomforing.startDato.isBefore(avtale.startDato)) {
                FieldError.of(
                    "Du må legge inn en startdato som er etter avtalens startdato",
                    GjennomforingDbo::startDato,
                )
            }
            validate(
                gjennomforing.sluttDato == null ||
                    previous.sluttDato == null ||
                    !gjennomforing.sluttDato.isBefore(LocalDate.now()),
            ) {
                FieldError.of(
                    "Du kan ikke sette en sluttdato bakover i tid når gjennomføringen er aktiv",
                    GjennomforingDbo::sluttDato,
                )
            }
        }
        validate(antallDeltakere <= 0 || gjennomforing.oppstart == previous.oppstart) {
            FieldError.of(
                "Oppstartstype kan ikke endres fordi det er deltakere koblet til gjennomføringen",
                GjennomforingDbo::oppstart,
            )
        }
    }

    private fun ValidationDsl.validateKursTiltak(request: GjennomforingRequest) {
        validate(request.deltidsprosent > 0 && request.deltidsprosent <= 100) {
            FieldError.of(
                "Du må velge en deltidsprosent mellom 0 og 100",
                GjennomforingDbo::deltidsprosent,
            )
        }
    }
}
