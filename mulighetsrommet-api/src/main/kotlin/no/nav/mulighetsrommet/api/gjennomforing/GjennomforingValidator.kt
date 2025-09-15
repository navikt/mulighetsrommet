package no.nav.mulighetsrommet.api.gjennomforing

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.right
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.avtale.model.Avtale
import no.nav.mulighetsrommet.api.avtale.model.AvtaleStatusDto
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
import no.nav.mulighetsrommet.api.navenhet.NavEnhetHelpers
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.model.Tiltakskoder.isKursTiltak
import java.time.LocalDate

class GjennomforingValidator(
    private val db: ApiDatabase,
    private val navEnhetService: NavEnhetService,
) {
    private val maksAntallTegnStedForGjennomforing = 100

    fun validate(
        dbo: GjennomforingDbo,
        previous: GjennomforingDto?,
    ): Either<List<FieldError>, GjennomforingDbo> = either {
        var next = dbo

        val avtale = db.session { queries.avtale.get(next.avtaleId) }
            ?: raise(FieldError.of("Avtalen finnes ikke", GjennomforingDbo::avtaleId).nel())

        val navEnheter = sanitizeNavEnheter(next.navEnheter)
        val errors = buildList {
            if (avtale.tiltakstype.id != next.tiltakstypeId) {
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

            if (avtale.avtaletype != Avtaletype.FORHANDSGODKJENT && next.sluttDato == null) {
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

            if (isKursTiltak(avtale.tiltakstype.tiltakskode)) {
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

            val avtaleHasArrangor = avtale.arrangor?.underenheter?.any {
                it.id == next.arrangorId
            } ?: false

            if (!avtaleHasArrangor) {
                add(FieldError.of("Du må velge en arrangør fra avtalen", GjennomforingDbo::arrangorId))
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
                            "Du må velge et kurselement for gjennomføringen",
                            GjennomforingDbo::amoKategorisering,
                        ),
                    )
                }
            }

            if (avtale.tiltakstype.tiltakskode == Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING) {
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
                } else if (utdanningslop.utdanningsprogram != avtale.utdanningslop?.utdanningsprogram?.id) {
                    add(
                        FieldError.of(
                            "Utdanningsprogrammet må være det samme som for avtalen: ${avtale.utdanningslop?.utdanningsprogram?.navn}",
                            GjennomforingDbo::utdanningslop,
                        ),
                    )
                } else {
                    val avtalensUtdanninger = avtale.utdanningslop.utdanninger.map { it.id }
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
            validateNavEnheter(navEnheter, avtale)
            validateKontaktpersoner(next)
            validateAdministratorer(next)

            next = validateOrResetTilgjengeligForArrangorDato(next)

            if (previous == null) {
                validateCreateGjennomforing(next, avtale)
            } else {
                validateUpdateGjennomforing(next, previous, avtale)
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left()
            ?: next.copy(navEnheter = navEnheter).right()
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
                    "Kontaktpersonene med Nav ident " + slettedeNavIdenter.joinToString(", ") + " er slettet og må fjernes",
                    GjennomforingDbo::kontaktpersoner,
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
                    "Administratorene med Nav ident " + slettedeNavIdenter.joinToString(", ") + " er slettet og må fjernes",
                    GjennomforingDbo::administratorer,
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
        gjennomforing: GjennomforingDbo,
        avtale: Avtale,
    ) {
        val arrangor = db.session { queries.arrangor.getById(gjennomforing.arrangorId) }
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

        if (gjennomforing.stedForGjennomforing != null && gjennomforing.stedForGjennomforing.length > maksAntallTegnStedForGjennomforing) {
            add(
                FieldError.of(
                    "Du kan bare skrive $maksAntallTegnStedForGjennomforing tegn i \"Sted for gjennomføring\"",
                    GjennomforingDbo::stedForGjennomforing,
                ),
            )
        }

        if (avtale.status != AvtaleStatusDto.Aktiv) {
            add(
                FieldError.of(
                    "Avtalen må være aktiv for å kunne opprette tiltak",
                    GjennomforingDbo::avtaleId,
                ),
            )
        }

        if (gjennomforing.status != GjennomforingStatus.GJENNOMFORES) {
            add(
                FieldError.of(
                    "Du kan ikke opprette en gjennomføring som er ${gjennomforing.status.name.lowercase()}",
                    GjennomforingDbo::navn,
                ),
            )
        }
    }

    private fun MutableList<FieldError>.validateUpdateGjennomforing(
        gjennomforing: GjennomforingDbo,
        previous: GjennomforingDto,
        avtale: Avtale,
    ) {
        if (previous.status.type != GjennomforingStatus.GJENNOMFORES) {
            add(
                FieldError.of(
                    "Du kan ikke gjøre endringer på en gjennomføring som er ${previous.status.type.name.lowercase()}",
                    GjennomforingDbo::navn,
                ),
            )
        }

        if (gjennomforing.arrangorId != previous.arrangor.id) {
            add(
                FieldError.of(
                    "Du kan ikke endre arrangør når gjennomføringen er aktiv",
                    GjennomforingDbo::arrangorId,
                ),
            )
        }

        if (previous.status.type == GjennomforingStatus.GJENNOMFORES) {
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

        val gjennomforingHarDeltakere = db.session {
            queries.deltaker.getAll(pagination = Pagination.of(1, 1), gjennomforingId = gjennomforing.id).isNotEmpty()
        }
        if (gjennomforingHarDeltakere) {
            if (gjennomforing.oppstart != previous.oppstart) {
                add(
                    FieldError.of(
                        "Oppstartstype kan ikke endres fordi det er deltakere koblet til gjennomføringen",
                        GjennomforingDbo::oppstart,
                    ),
                )
            }
        }
    }

    private fun MutableList<FieldError>.validateKursTiltak(dbo: GjennomforingDbo) {
        if (dbo.deltidsprosent <= 0) {
            add(
                FieldError.of(
                    "Du må velge en deltidsprosent større enn 0",
                    GjennomforingDbo::deltidsprosent,
                ),
            )
        } else if (dbo.deltidsprosent > 100) {
            add(
                FieldError.of(
                    "Du må velge en deltidsprosent mindre enn 100",
                    GjennomforingDbo::deltidsprosent,
                ),
            )
        }
    }

    fun sanitizeNavEnheter(navEnheter: Set<NavEnhetNummer>): Set<NavEnhetNummer> {
        // Filtrer vekk underenheter uten fylke
        return NavEnhetHelpers.buildNavRegioner(
            navEnheter.mapNotNull { navEnhetService.hentEnhet(it) },
        )
            .flatMap { it.enheter.map { it.enhetsnummer } + it.enhetsnummer }
            .toSet()
    }
}
