package no.nav.mulighetsrommet.api.gjennomforing

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.right
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.avtale.model.Avtale
import no.nav.mulighetsrommet.api.avtale.model.AvtaleStatus
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.mapper.GjennomforingDboMapper
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.model.Tiltakskoder.isKursTiltak
import java.time.LocalDate
import java.util.*
import kotlin.reflect.KProperty1

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
    ): Either<List<FieldError>, GjennomforingDbo> = either {
        var next = request

        val errors = buildList {
            if (ctx.avtale.tiltakstype.id != next.tiltakstypeId) {
                add(
                    FieldError.of(
                        "Tiltakstypen må være den samme som for avtalen",
                        GjennomforingDbo::tiltakstypeId,
                    ),
                )
            }

            if (next.administratorer.isEmpty()) {
                add(
                    FieldError.of(
                        "Du må velge minst én administrator",
                        GjennomforingDbo::administratorer,
                    ),
                )
            }

            if (ctx.avtale.avtaletype != Avtaletype.FORHANDSGODKJENT && next.sluttDato == null) {
                add(
                    FieldError.of(
                        "Du må legge inn sluttdato for gjennomføringen",
                        GjennomforingDbo::sluttDato,
                    ),
                )
            }

            if (next.sluttDato != null && next.startDato.isAfter(next.sluttDato)) {
                add(FieldError.of("Startdato må være før sluttdato", GjennomforingDbo::startDato))
            }

            if (next.antallPlasser <= 0) {
                add(
                    FieldError.of(
                        "Du må legge inn antall plasser større enn 0",
                        GjennomforingDbo::antallPlasser,
                    ),
                )
            }

            if (isKursTiltak(ctx.avtale.tiltakstype.tiltakskode)) {
                validateKursTiltak(next)
            } else {
                if (next.oppstart == GjennomforingOppstartstype.FELLES) {
                    add(
                        FieldError.of(
                            "Tiltaket må ha løpende oppstartstype",
                            GjennomforingDbo::oppstart,
                        ),
                    )
                }
            }

            val avtaleHasArrangor = ctx.avtale.arrangor?.underenheter?.any {
                it.id == next.arrangorId
            } ?: false

            if (!avtaleHasArrangor) {
                add(FieldError.of("Du må velge en arrangør fra avtalen", GjennomforingDbo::arrangorId))
            }

            if (ctx.avtale.tiltakstype.tiltakskode == Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING) {
                if (ctx.avtale.amoKategorisering == null) {
                    add(
                        FieldError(
                            "/avtale.amoKategorisering",
                            "Du må velge en kurstype for avtalen",
                        ),
                    )
                }

                if (next.amoKategorisering == null) {
                    add(
                        FieldError.of(
                            "Du må velge et kurselement for gjennomføringen",
                            GjennomforingDbo::amoKategorisering,
                        ),
                    )
                }
            }

            if (ctx.avtale.tiltakstype.tiltakskode == Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING) {
                val utdanningslop = next.utdanningslop
                if (utdanningslop == null) {
                    add(
                        FieldError.of(
                            "Du må velge utdanningsprogram og lærefag på avtalen",
                            GjennomforingDbo::utdanningslop,
                        ),
                    )
                } else if (utdanningslop.utdanninger.isEmpty()) {
                    add(
                        FieldError.of(
                            "Du må velge minst ett lærefag",
                            GjennomforingDbo::utdanningslop,
                        ),
                    )
                } else if (utdanningslop.utdanningsprogram != ctx.avtale.utdanningslop?.utdanningsprogram?.id) {
                    add(
                        FieldError.of(
                            "Utdanningsprogrammet må være det samme som for avtalen: ${ctx.avtale.utdanningslop?.utdanningsprogram?.navn}",
                            GjennomforingDbo::utdanningslop,
                        ),
                    )
                } else {
                    val avtalensUtdanninger = ctx.avtale.utdanningslop.utdanninger.map { it.id }
                    if (!avtalensUtdanninger.containsAll(utdanningslop.utdanninger)) {
                        add(
                            FieldError.of(
                                "Lærefag må være valgt fra avtalens lærefag, minst ett av lærefagene mangler i avtalen.",
                                GjennomforingDbo::utdanningslop,
                            ),
                        )
                    }
                }
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
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: GjennomforingDboMapper.fromGjennomforingRequest(next, ctx.status).right()
    }

    private fun MutableList<FieldError>.validateNavEnheter(
        navEnheter: Set<NavEnhetNummer>,
        avtale: Avtale,
    ) {
        val avtaleRegioner = avtale.kontorstruktur.map { it.region.enhetsnummer }
        if (avtaleRegioner.intersect(navEnheter).isEmpty()) {
            add(
                FieldError.of(
                    "Du må velge minst én Nav-region fra avtalen",
                    GjennomforingDbo::navEnheter,
                ),
            )
        }

        val avtaleNavKontorer = avtale.kontorstruktur.flatMap { it.kontorer.map { kontor -> kontor.enhetsnummer } }
        if (avtaleNavKontorer.intersect(navEnheter).isEmpty()) {
            add(
                FieldError.of(
                    "Du må velge minst én Nav-enhet fra avtalen",
                    GjennomforingDbo::navEnheter,
                ),
            )
        }
        navEnheter.filterNot { it in avtaleRegioner || it in avtaleNavKontorer }.forEach { enhetsnummer ->
            add(FieldError.of("Nav-enhet $enhetsnummer mangler i avtalen", GjennomforingDbo::navEnheter))
        }
    }

    private fun MutableList<FieldError>.validateSlettetNavAnsatte(
        navAnsatte: List<NavAnsatt>,
        property: KProperty1<*, *>,
    ) {
        val slettedeNavIdenter = navAnsatte
            .filter { it.skalSlettesDato != null }

        if (slettedeNavIdenter.isNotEmpty()) {
            add(
                FieldError.of(
                    "Nav identer " + slettedeNavIdenter.joinToString(", ") { it.navIdent.value } + " er slettet og må fjernes",
                    property,
                ),
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

    private fun MutableList<FieldError>.validateCreateGjennomforing(
        arrangor: ArrangorDto,
        gjennomforing: GjennomforingRequest,
        status: GjennomforingStatusType,
        avtale: Avtale,
    ) {
        if (arrangor.slettetDato != null) {
            add(
                FieldError.of(
                    "Arrangøren ${arrangor.navn} er slettet i Brønnøysundregistrene. Gjennomføringer kan ikke opprettes for slettede bedrifter.",
                    GjennomforingDbo::arrangorId,
                ),
            )
        }

        if (gjennomforing.startDato.isBefore(avtale.startDato)) {
            add(
                FieldError.of(
                    "Du må legge inn en startdato som er etter avtalens startdato",
                    GjennomforingDbo::startDato,
                ),
            )
        }

        if (gjennomforing.stedForGjennomforing != null && gjennomforing.stedForGjennomforing.length > MAKS_ANTALL_TEGN_STED_FOR_GJENNOMFORING) {
            add(
                FieldError.of(
                    "Du kan bare skrive $MAKS_ANTALL_TEGN_STED_FOR_GJENNOMFORING tegn i \"Sted for gjennomføring\"",
                    GjennomforingDbo::stedForGjennomforing,
                ),
            )
        }

        if (avtale.status != AvtaleStatus.Aktiv) {
            add(
                FieldError.of(
                    "Avtalen må være aktiv for å kunne opprette tiltak",
                    GjennomforingDbo::avtaleId,
                ),
            )
        }

        if (status != GjennomforingStatusType.GJENNOMFORES) {
            add(
                FieldError.of(
                    "Du kan ikke opprette en gjennomføring som er ${status.name.lowercase()}",
                    GjennomforingDbo::navn,
                ),
            )
        }
    }

    private fun MutableList<FieldError>.validateUpdateGjennomforing(
        gjennomforing: GjennomforingRequest,
        previous: Ctx.Gjennomforing,
        avtale: Avtale,
        antallDeltakere: Int,
    ) {
        if (previous.status != GjennomforingStatusType.GJENNOMFORES) {
            add(
                FieldError.of(
                    "Du kan ikke gjøre endringer på en gjennomføring som er ${previous.status.name.lowercase()}",
                    GjennomforingDbo::navn,
                ),
            )
        }

        if (gjennomforing.arrangorId != previous.arrangorId) {
            add(
                FieldError.of(
                    "Du kan ikke endre arrangør når gjennomføringen er aktiv",
                    GjennomforingDbo::arrangorId,
                ),
            )
        }

        if (previous.status == GjennomforingStatusType.GJENNOMFORES) {
            if (gjennomforing.avtaleId != previous.avtaleId) {
                add(
                    FieldError.of(
                        "Du kan ikke endre avtalen når gjennomføringen er aktiv",
                        GjennomforingDbo::avtaleId,
                    ),
                )
            }

            if (gjennomforing.startDato.isBefore(avtale.startDato)) {
                add(
                    FieldError.of(
                        "Du må legge inn en startdato som er etter avtalens startdato",
                        GjennomforingDbo::startDato,
                    ),
                )
            }

            if (gjennomforing.sluttDato != null &&
                previous.sluttDato != null &&
                gjennomforing.sluttDato.isBefore(LocalDate.now())
            ) {
                add(
                    FieldError.of(
                        "Du kan ikke sette en sluttdato bakover i tid når gjennomføringen er aktiv",
                        GjennomforingDbo::sluttDato,
                    ),
                )
            }
        }

        if (antallDeltakere > 0 && gjennomforing.oppstart != previous.oppstart) {
            add(
                FieldError.of(
                    "Oppstartstype kan ikke endres fordi det er deltakere koblet til gjennomføringen",
                    GjennomforingDbo::oppstart,
                ),
            )
        }
    }

    private fun MutableList<FieldError>.validateKursTiltak(request: GjennomforingRequest) {
        if (request.deltidsprosent <= 0) {
            add(
                FieldError.of(
                    "Du må velge en deltidsprosent større enn 0",
                    GjennomforingDbo::deltidsprosent,
                ),
            )
        } else if (request.deltidsprosent > 100) {
            add(
                FieldError.of(
                    "Du må velge en deltidsprosent mindre enn 100",
                    GjennomforingDbo::deltidsprosent,
                ),
            )
        }
    }
}
