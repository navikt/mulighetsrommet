package no.nav.mulighetsrommet.api.gjennomforing

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.right
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.model.AvtaleStatus
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingStatus
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltakskoder.isKursTiltak
import java.time.LocalDate

class GjennomforingValidator(
    private val db: ApiDatabase,
) {
    private val maksAntallTegnStedForGjennomforing = 100

    fun validate(
        dbo: GjennomforingDbo,
        previous: GjennomforingDto?,
    ): Either<List<ValidationError>, GjennomforingDbo> = either {
        var next = dbo

        val avtale = db.session { queries.avtale.get(next.avtaleId) }
            ?: raise(ValidationError.of(GjennomforingDbo::avtaleId, "Avtalen finnes ikke").nel())

        val errors = buildList {
            if (avtale.tiltakstype.id != next.tiltakstypeId) {
                add(
                    ValidationError.of(
                        GjennomforingDbo::tiltakstypeId,
                        "Tiltakstypen må være den samme som for avtalen",
                    ),
                )
            }

            if (next.administratorer.isEmpty()) {
                add(
                    ValidationError.of(
                        GjennomforingDbo::administratorer,
                        "Du må velge minst én administrator",
                    ),
                )
            }

            if (avtale.avtaletype != Avtaletype.Forhaandsgodkjent && next.sluttDato == null) {
                add(
                    ValidationError.of(
                        GjennomforingDbo::sluttDato,
                        "Du må legge inn sluttdato for gjennomføringen",
                    ),
                )
            }

            if (next.sluttDato != null && next.startDato.isAfter(next.sluttDato)) {
                add(ValidationError.of(GjennomforingDbo::startDato, "Startdato må være før sluttdato"))
            }

            if (next.antallPlasser <= 0) {
                add(
                    ValidationError.of(
                        GjennomforingDbo::antallPlasser,
                        "Du må legge inn antall plasser større enn 0",
                    ),
                )
            }

            if (isKursTiltak(avtale.tiltakstype.tiltakskode)) {
                validateKursTiltak(next)
            } else {
                if (next.oppstart == GjennomforingOppstartstype.FELLES) {
                    add(
                        ValidationError.of(
                            GjennomforingDbo::oppstart,
                            "Tiltaket må ha løpende oppstartstype",
                        ),
                    )
                }
            }

            if (next.navEnheter.isEmpty()) {
                add(ValidationError.of(GjennomforingDbo::navEnheter, "Du må velge minst ett Nav-kontor"))
            }

            if (!avtale.kontorstruktur.any { it.region.enhetsnummer == next.navRegion }) {
                add(
                    ValidationError.of(
                        GjennomforingDbo::navEnheter,
                        "Nav-region ${next.navRegion} mangler i avtalen",
                    ),
                )
            }

            val avtaleNavEnheter = avtale.kontorstruktur.flatMap { it.kontorer }.associateBy { it.enhetsnummer }
            next.navEnheter.forEach { enhetsnummer ->
                if (!avtaleNavEnheter.containsKey(enhetsnummer)) {
                    add(
                        ValidationError.of(
                            GjennomforingDbo::navEnheter,
                            "Nav-enhet $enhetsnummer mangler i avtalen",
                        ),
                    )
                }
            }

            val avtaleHasArrangor = avtale.arrangor.underenheter.any {
                it.id == next.arrangorId
            }
            if (!avtaleHasArrangor) {
                add(ValidationError.of(GjennomforingDbo::arrangorId, "Du må velge en arrangør for avtalen"))
            }

            if (avtale.tiltakstype.tiltakskode == Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING) {
                if (avtale.amoKategorisering == null) {
                    add(
                        ValidationError(
                            "avtale.amoKategorisering",
                            "Du må velge en kurstype for avtalen",
                        ),
                    )
                }

                if (next.amoKategorisering == null) {
                    add(
                        ValidationError.of(
                            GjennomforingDbo::amoKategorisering,
                            "Du må velge et kurselement for gjennomføringen",
                        ),
                    )
                }
            }

            if (avtale.tiltakstype.tiltakskode == Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING) {
                val utdanningslop = next.utdanningslop
                if (utdanningslop == null) {
                    add(
                        ValidationError.of(
                            GjennomforingDbo::utdanningslop,
                            "Du må velge utdanningsprogram og lærefag på avtalen",
                        ),
                    )
                } else if (utdanningslop.utdanninger.isEmpty()) {
                    add(
                        ValidationError.of(
                            GjennomforingDbo::utdanningslop,
                            "Du må velge minst ett lærefag",
                        ),
                    )
                } else if (utdanningslop.utdanningsprogram != avtale.utdanningslop?.utdanningsprogram?.id) {
                    add(
                        ValidationError.of(
                            GjennomforingDbo::utdanningslop,
                            "Utdanningsprogrammet må være det samme som for avtalen: ${avtale.utdanningslop?.utdanningsprogram?.navn}",
                        ),
                    )
                } else {
                    val avtalensUtdanninger = avtale.utdanningslop.utdanninger.map { it.id }
                    if (!avtalensUtdanninger.containsAll(utdanningslop.utdanninger)) {
                        add(
                            ValidationError.of(
                                GjennomforingDbo::utdanningslop,
                                "Lærefag må være valgt fra avtalens lærefag, minst ett av lærefagene mangler i avtalen.",
                            ),
                        )
                    }
                }
            }
            validateKontaktpersoner(next)
            validateAdministratorer(next)

            next = validateOrResetTilgjengeligForArrangorDato(next)

            if (previous == null) {
                validateCreateGjennomforing(next, avtale)
            } else {
                validateUpdateGjennomforing(next, previous, avtale)
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: next.right()
    }

    private fun MutableList<ValidationError>.validateKontaktpersoner(
        next: GjennomforingDbo,
    ) {
        val slettedeNavIdenter = db.session {
            next.kontaktpersoner.mapNotNull { p ->
                queries.ansatt.getByNavIdent(p.navIdent)?.takeIf { it.skalSlettesDato != null }?.navIdent?.value
            }
        }

        if (slettedeNavIdenter.isNotEmpty()) {
            add(
                ValidationError.of(
                    GjennomforingDbo::kontaktpersoner,
                    "Kontaktpersonene med Nav ident " + slettedeNavIdenter.joinToString(", ") + " er slettet og må fjernes",
                ),
            )
        }
    }

    private fun MutableList<ValidationError>.validateAdministratorer(
        next: GjennomforingDbo,
    ) {
        val slettedeNavIdenter = db.session {
            next.administratorer.mapNotNull { ident ->
                queries.ansatt.getByNavIdent(ident)?.takeIf { it.skalSlettesDato != null }?.navIdent?.value
            }
        }

        if (slettedeNavIdenter.isNotEmpty()) {
            add(
                ValidationError.of(
                    GjennomforingDbo::administratorer,
                    "Administratorene med Nav ident " + slettedeNavIdenter.joinToString(", ") + " er slettet og må fjernes",
                ),
            )
        }
    }

    private fun validateOrResetTilgjengeligForArrangorDato(
        next: GjennomforingDbo,
    ): GjennomforingDbo {
        val nextTilgjengeligForArrangorDato = next.tilgjengeligForArrangorFraOgMedDato?.let { date ->
            validateTilgjengeligForArrangorDato(date, next.startDato).fold({ null }, { it })
        }
        return next.copy(tilgjengeligForArrangorFraOgMedDato = nextTilgjengeligForArrangorDato)
    }

    fun validateTilgjengeligForArrangorDato(
        tilgjengeligForArrangorDato: LocalDate,
        startDato: LocalDate,
    ): Either<List<ValidationError>, LocalDate> {
        val errors = buildList {
            if (tilgjengeligForArrangorDato < LocalDate.now()) {
                add(
                    ValidationError.of(
                        GjennomforingDbo::tilgjengeligForArrangorFraOgMedDato,
                        "Du må velge en dato som er etter dagens dato",
                    ),
                )
            } else if (tilgjengeligForArrangorDato < startDato.minusMonths(2)) {
                add(
                    ValidationError.of(
                        GjennomforingDbo::tilgjengeligForArrangorFraOgMedDato,
                        "Du må velge en dato som er tidligst to måneder før gjennomføringens oppstartsdato",
                    ),
                )
            }

            if (tilgjengeligForArrangorDato > startDato) {
                add(
                    ValidationError.of(
                        GjennomforingDbo::tilgjengeligForArrangorFraOgMedDato,
                        "Du må velge en dato som er før gjennomføringens oppstartsdato",
                    ),
                )
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: tilgjengeligForArrangorDato.right()
    }

    private fun MutableList<ValidationError>.validateCreateGjennomforing(
        gjennomforing: GjennomforingDbo,
        avtale: AvtaleDto,
    ) {
        val arrangor = db.session { queries.arrangor.getById(gjennomforing.arrangorId) }
        if (arrangor.slettetDato != null) {
            add(
                ValidationError.of(
                    GjennomforingDbo::arrangorId,
                    "Arrangøren ${arrangor.navn} er slettet i Brønnøysundregistrene. Gjennomføringer kan ikke opprettes for slettede bedrifter.",
                ),
            )
        }

        if (gjennomforing.startDato.isBefore(avtale.startDato)) {
            add(
                ValidationError.of(
                    GjennomforingDbo::startDato,
                    "Du må legge inn en startdato som er etter avtalens startdato",
                ),
            )
        }

        if (gjennomforing.stedForGjennomforing != null && gjennomforing.stedForGjennomforing.length > maksAntallTegnStedForGjennomforing) {
            add(
                ValidationError.of(
                    GjennomforingDbo::stedForGjennomforing,
                    "Du kan bare skrive $maksAntallTegnStedForGjennomforing tegn i \"Sted for gjennomføring\"",
                ),
            )
        }

        if (avtale.status != AvtaleStatus.AKTIV) {
            add(
                ValidationError.of(
                    GjennomforingDbo::avtaleId,
                    "Avtalen må være aktiv for å kunne opprette tiltak",
                ),
            )
        }
    }

    private fun MutableList<ValidationError>.validateUpdateGjennomforing(
        gjennomforing: GjennomforingDbo,
        previous: GjennomforingDto,
        avtale: AvtaleDto,
    ) {
        if (previous.status.status != GjennomforingStatus.GJENNOMFORES) {
            add(
                ValidationError.of(
                    GjennomforingDbo::navn,
                    "Du kan ikke gjøre endringer på en gjennomføring som er ${previous.status.status.name.lowercase()}",
                ),
            )
        }

        if (gjennomforing.arrangorId != previous.arrangor.id) {
            add(
                ValidationError.of(
                    GjennomforingDbo::arrangorId,
                    "Du kan ikke endre arrangør når gjennomføringen er aktiv",
                ),
            )
        }

        if (previous.status.status == GjennomforingStatus.GJENNOMFORES) {
            if (gjennomforing.avtaleId != previous.avtaleId) {
                add(
                    ValidationError.of(
                        GjennomforingDbo::avtaleId,
                        "Du kan ikke endre avtalen når gjennomføringen er aktiv",
                    ),
                )
            }

            if (gjennomforing.startDato.isBefore(avtale.startDato)) {
                add(
                    ValidationError.of(
                        GjennomforingDbo::startDato,
                        "Du må legge inn en startdato som er etter avtalens startdato",
                    ),
                )
            }

            if (gjennomforing.sluttDato != null &&
                previous.sluttDato != null &&
                gjennomforing.sluttDato.isBefore(LocalDate.now())
            ) {
                add(
                    ValidationError.of(
                        GjennomforingDbo::sluttDato,
                        "Du kan ikke sette en sluttdato bakover i tid når gjennomføringen er aktiv",
                    ),
                )
            }
        }

        val gjennomforingHarDeltakere = db.session {
            queries.deltaker.getAll(pagination = Pagination.of(1, 1), gjennomforingId = gjennomforing.id).isNotEmpty()
        }
        if (gjennomforingHarDeltakere) {
            if (gjennomforing.oppstart != previous.oppstart) {
                add(
                    ValidationError.of(
                        GjennomforingDbo::oppstart,
                        "Oppstartstype kan ikke endres fordi det er deltakere koblet til gjennomføringen",
                    ),
                )
            }
        }
    }

    private fun MutableList<ValidationError>.validateKursTiltak(dbo: GjennomforingDbo) {
        if (dbo.deltidsprosent <= 0) {
            add(
                ValidationError.of(
                    GjennomforingDbo::deltidsprosent,
                    "Du må velge en deltidsprosent større enn 0",
                ),
            )
        } else if (dbo.deltidsprosent > 100) {
            add(
                ValidationError.of(
                    GjennomforingDbo::deltidsprosent,
                    "Du må velge en deltidsprosent mindre enn 100",
                ),
            )
        }
    }
}
