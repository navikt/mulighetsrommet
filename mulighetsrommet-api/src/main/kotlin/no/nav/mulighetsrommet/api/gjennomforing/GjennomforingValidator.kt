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
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.model.Tiltakskoder.isKursTiltak
import java.time.LocalDate

class GjennomforingValidator(
    private val db: ApiDatabase,
) {
    private val maksAntallTegnStedForGjennomforing = 100

    fun validate(
        dbo: GjennomforingDbo,
        previous: GjennomforingDto?,
    ): Either<List<FieldError>, GjennomforingDbo> = either {
        var next = dbo

        val avtale = db.session { queries.avtale.get(next.avtaleId) }
            ?: raise(FieldError.of(GjennomforingDbo::avtaleId, "Avtalen finnes ikke").nel())

        val errors = buildList {
            if (avtale.tiltakstype.id != next.tiltakstypeId) {
                add(
                    FieldError.of(
                        GjennomforingDbo::tiltakstypeId,
                        "Tiltakstypen må være den samme som for avtalen",
                    ),
                )
            }

            if (next.administratorer.isEmpty()) {
                add(
                    FieldError.of(
                        GjennomforingDbo::administratorer,
                        "Du må velge minst én administrator",
                    ),
                )
            }

            if (avtale.avtaletype != Avtaletype.FORHANDSGODKJENT && next.sluttDato == null) {
                add(
                    FieldError.of(
                        GjennomforingDbo::sluttDato,
                        "Du må legge inn sluttdato for gjennomføringen",
                    ),
                )
            }

            if (next.sluttDato != null && next.startDato.isAfter(next.sluttDato)) {
                add(FieldError.of(GjennomforingDbo::startDato, "Startdato må være før sluttdato"))
            }

            if (next.antallPlasser <= 0) {
                add(
                    FieldError.of(
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
                        FieldError.of(
                            GjennomforingDbo::oppstart,
                            "Tiltaket må ha løpende oppstartstype",
                        ),
                    )
                }
            }

            if (next.navEnheter.isEmpty()) {
                add(FieldError.of(GjennomforingDbo::navEnheter, "Du må velge minst ett Nav-kontor"))
            }

            val avtaleNavEnheter = avtale.kontorstruktur.flatMap {
                it.kontorer.map { kontor -> kontor.enhetsnummer } + it.region.enhetsnummer
            }
            next.navEnheter.forEach { enhetsnummer ->
                if (!avtaleNavEnheter.contains(enhetsnummer)) {
                    add(
                        FieldError.of(
                            GjennomforingDbo::navEnheter,
                            "Nav-enhet $enhetsnummer mangler i avtalen",
                        ),
                    )
                }
            }

            val avtaleHasArrangor = avtale.arrangor?.underenheter?.any {
                it.id == next.arrangorId
            } ?: false

            if (!avtaleHasArrangor) {
                add(FieldError.of(GjennomforingDbo::arrangorId, "Du må velge en arrangør fra avtalen"))
            }

            if (avtale.tiltakstype.tiltakskode == Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING) {
                if (avtale.amoKategorisering == null) {
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
                        FieldError.of(
                            GjennomforingDbo::utdanningslop,
                            "Du må velge utdanningsprogram og lærefag på avtalen",
                        ),
                    )
                } else if (utdanningslop.utdanninger.isEmpty()) {
                    add(
                        FieldError.of(
                            GjennomforingDbo::utdanningslop,
                            "Du må velge minst ett lærefag",
                        ),
                    )
                } else if (utdanningslop.utdanningsprogram != avtale.utdanningslop?.utdanningsprogram?.id) {
                    add(
                        FieldError.of(
                            GjennomforingDbo::utdanningslop,
                            "Utdanningsprogrammet må være det samme som for avtalen: ${avtale.utdanningslop?.utdanningsprogram?.navn}",
                        ),
                    )
                } else {
                    val avtalensUtdanninger = avtale.utdanningslop.utdanninger.map { it.id }
                    if (!avtalensUtdanninger.containsAll(utdanningslop.utdanninger)) {
                        add(
                            FieldError.of(
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

    private fun MutableList<FieldError>.validateKontaktpersoner(
        next: GjennomforingDbo,
    ) {
        val slettedeNavIdenter = db.session {
            next.kontaktpersoner.mapNotNull { p ->
                queries.ansatt.getByNavIdent(p.navIdent)?.takeIf { it.skalSlettesDato != null }?.navIdent?.value
            }
        }

        if (slettedeNavIdenter.isNotEmpty()) {
            add(
                FieldError.of(
                    GjennomforingDbo::kontaktpersoner,
                    "Kontaktpersonene med Nav ident " + slettedeNavIdenter.joinToString(", ") + " er slettet og må fjernes",
                ),
            )
        }
    }

    private fun MutableList<FieldError>.validateAdministratorer(
        next: GjennomforingDbo,
    ) {
        val slettedeNavIdenter = db.session {
            next.administratorer.mapNotNull { ident ->
                queries.ansatt.getByNavIdent(ident)?.takeIf { it.skalSlettesDato != null }?.navIdent?.value
            }
        }

        if (slettedeNavIdenter.isNotEmpty()) {
            add(
                FieldError.of(
                    GjennomforingDbo::administratorer,
                    "Administratorene med Nav ident " + slettedeNavIdenter.joinToString(", ") + " er slettet og må fjernes",
                ),
            )
        }
    }

    private fun validateOrResetTilgjengeligForArrangorDato(
        next: GjennomforingDbo,
    ): GjennomforingDbo {
        val nextTilgjengeligForArrangorDato = next.tilgjengeligForArrangorDato?.let { date ->
            validateTilgjengeligForArrangorDato(date, next.startDato).fold({ null }, { it })
        }
        return next.copy(tilgjengeligForArrangorDato = nextTilgjengeligForArrangorDato)
    }

    fun validateTilgjengeligForArrangorDato(
        tilgjengeligForArrangorDato: LocalDate,
        startDato: LocalDate,
    ): Either<List<FieldError>, LocalDate> {
        val errors = buildList {
            if (tilgjengeligForArrangorDato < LocalDate.now()) {
                add(
                    FieldError.of(
                        GjennomforingDbo::tilgjengeligForArrangorDato,
                        "Du må velge en dato som er etter dagens dato",
                    ),
                )
            } else if (tilgjengeligForArrangorDato < startDato.minusMonths(2)) {
                add(
                    FieldError.of(
                        GjennomforingDbo::tilgjengeligForArrangorDato,
                        "Du må velge en dato som er tidligst to måneder før gjennomføringens oppstartsdato",
                    ),
                )
            }

            if (tilgjengeligForArrangorDato > startDato) {
                add(
                    FieldError.of(
                        GjennomforingDbo::tilgjengeligForArrangorDato,
                        "Du må velge en dato som er før gjennomføringens oppstartsdato",
                    ),
                )
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: tilgjengeligForArrangorDato.right()
    }

    private fun MutableList<FieldError>.validateCreateGjennomforing(
        gjennomforing: GjennomforingDbo,
        avtale: AvtaleDto,
    ) {
        val arrangor = db.session { queries.arrangor.getById(gjennomforing.arrangorId) }
        if (arrangor.slettetDato != null) {
            add(
                FieldError.of(
                    GjennomforingDbo::arrangorId,
                    "Arrangøren ${arrangor.navn} er slettet i Brønnøysundregistrene. Gjennomføringer kan ikke opprettes for slettede bedrifter.",
                ),
            )
        }

        if (gjennomforing.startDato.isBefore(avtale.startDato)) {
            add(
                FieldError.of(
                    GjennomforingDbo::startDato,
                    "Du må legge inn en startdato som er etter avtalens startdato",
                ),
            )
        }

        if (gjennomforing.stedForGjennomforing != null && gjennomforing.stedForGjennomforing.length > maksAntallTegnStedForGjennomforing) {
            add(
                FieldError.of(
                    GjennomforingDbo::stedForGjennomforing,
                    "Du kan bare skrive $maksAntallTegnStedForGjennomforing tegn i \"Sted for gjennomføring\"",
                ),
            )
        }

        if (avtale.status != AvtaleStatus.AKTIV) {
            add(
                FieldError.of(
                    GjennomforingDbo::avtaleId,
                    "Avtalen må være aktiv for å kunne opprette tiltak",
                ),
            )
        }

        if (gjennomforing.status != GjennomforingStatus.GJENNOMFORES) {
            add(
                FieldError.of(
                    GjennomforingDbo::navn,
                    "Du kan ikke opprette en gjennomføring som er ${gjennomforing.status.name.lowercase()}",
                ),
            )
        }
    }

    private fun MutableList<FieldError>.validateUpdateGjennomforing(
        gjennomforing: GjennomforingDbo,
        previous: GjennomforingDto,
        avtale: AvtaleDto,
    ) {
        if (previous.status.status != GjennomforingStatus.GJENNOMFORES) {
            add(
                FieldError.of(
                    GjennomforingDbo::navn,
                    "Du kan ikke gjøre endringer på en gjennomføring som er ${previous.status.status.name.lowercase()}",
                ),
            )
        }

        if (gjennomforing.arrangorId != previous.arrangor.id) {
            add(
                FieldError.of(
                    GjennomforingDbo::arrangorId,
                    "Du kan ikke endre arrangør når gjennomføringen er aktiv",
                ),
            )
        }

        if (previous.status.status == GjennomforingStatus.GJENNOMFORES) {
            if (gjennomforing.avtaleId != previous.avtaleId) {
                add(
                    FieldError.of(
                        GjennomforingDbo::avtaleId,
                        "Du kan ikke endre avtalen når gjennomføringen er aktiv",
                    ),
                )
            }

            if (gjennomforing.startDato.isBefore(avtale.startDato)) {
                add(
                    FieldError.of(
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
                    FieldError.of(
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
                    FieldError.of(
                        GjennomforingDbo::oppstart,
                        "Oppstartstype kan ikke endres fordi det er deltakere koblet til gjennomføringen",
                    ),
                )
            }
        }
    }

    private fun MutableList<FieldError>.validateKursTiltak(dbo: GjennomforingDbo) {
        if (dbo.deltidsprosent <= 0) {
            add(
                FieldError.of(
                    GjennomforingDbo::deltidsprosent,
                    "Du må velge en deltidsprosent større enn 0",
                ),
            )
        } else if (dbo.deltidsprosent > 100) {
            add(
                FieldError.of(
                    GjennomforingDbo::deltidsprosent,
                    "Du må velge en deltidsprosent mindre enn 100",
                ),
            )
        }
    }
}
