package no.nav.mulighetsrommet.api.gjennomforing

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.right
import no.nav.mulighetsrommet.api.arrangor.db.ArrangorRepository
import no.nav.mulighetsrommet.api.avtale.db.AvtaleRepository
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.model.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.Tiltakskoder.isKursTiltak
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.AvtaleStatus
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus
import no.nav.mulighetsrommet.unleash.UnleashService
import java.time.LocalDate

class TiltaksgjennomforingValidator(
    private val tiltakstyper: TiltakstypeService,
    private val avtaler: AvtaleRepository,
    private val arrangorer: ArrangorRepository,
    private val unleashService: UnleashService,
) {
    private val maksAntallTegnStedForGjennomforing = 100

    fun validate(
        dbo: TiltaksgjennomforingDbo,
        previous: TiltaksgjennomforingDto?,
    ): Either<List<ValidationError>, TiltaksgjennomforingDbo> = either {
        var next = dbo

        val tiltakstype = tiltakstyper.getById(next.tiltakstypeId)
            ?: raise(ValidationError.of(TiltaksgjennomforingDbo::tiltakstypeId, "Tiltakstypen finnes ikke").nel())

        val avtale = avtaler.get(next.avtaleId)
            ?: raise(ValidationError.of(TiltaksgjennomforingDbo::avtaleId, "Avtalen finnes ikke").nel())

        val errors = buildList {
            if (avtale.tiltakstype.id != next.tiltakstypeId) {
                add(
                    ValidationError.of(
                        TiltaksgjennomforingDbo::tiltakstypeId,
                        "Tiltakstypen må være den samme som for avtalen",
                    ),
                )
            }

            if (next.administratorer.isEmpty()) {
                add(
                    ValidationError.of(
                        TiltaksgjennomforingDbo::administratorer,
                        "Du må velge minst én administrator",
                    ),
                )
            }

            if (avtale.avtaletype != Avtaletype.Forhaandsgodkjent && next.sluttDato == null) {
                add(
                    ValidationError.of(
                        TiltaksgjennomforingDbo::sluttDato,
                        "Du må legge inn sluttdato for gjennomføringen",
                    ),
                )
            }

            if (next.sluttDato != null && next.startDato.isAfter(next.sluttDato)) {
                add(ValidationError.of(TiltaksgjennomforingDbo::startDato, "Startdato må være før sluttdato"))
            }

            if (next.antallPlasser <= 0) {
                add(
                    ValidationError.of(
                        TiltaksgjennomforingDbo::antallPlasser,
                        "Du må legge inn antall plasser større enn 0",
                    ),
                )
            }

            if (isKursTiltak(avtale.tiltakstype.tiltakskode)) {
                validateKursTiltak(next)
            } else {
                if (next.oppstart == TiltaksgjennomforingOppstartstype.FELLES) {
                    add(
                        ValidationError.of(
                            TiltaksgjennomforingDbo::oppstart,
                            "Tiltaket må ha løpende oppstartstype",
                        ),
                    )
                }
            }

            if (next.navEnheter.isEmpty()) {
                add(ValidationError.of(TiltaksgjennomforingDbo::navEnheter, "Du må velge minst ett Nav-kontor"))
            }

            if (!avtale.kontorstruktur.any { it.region.enhetsnummer == next.navRegion }) {
                add(
                    ValidationError.of(
                        TiltaksgjennomforingDbo::navEnheter,
                        "Nav-region ${next.navRegion} mangler i avtalen",
                    ),
                )
            }

            val avtaleNavEnheter = avtale.kontorstruktur.flatMap { it.kontorer }.associateBy { it.enhetsnummer }
            next.navEnheter.forEach { enhetsnummer ->
                if (!avtaleNavEnheter.containsKey(enhetsnummer)) {
                    add(
                        ValidationError.of(
                            TiltaksgjennomforingDbo::navEnheter,
                            "Nav-enhet $enhetsnummer mangler i avtalen",
                        ),
                    )
                }
            }

            val avtaleHasArrangor = avtale.arrangor.underenheter.any {
                it.id == next.arrangorId
            }
            if (!avtaleHasArrangor) {
                add(ValidationError.of(TiltaksgjennomforingDbo::arrangorId, "Du må velge en arrangør for avtalen"))
            }
            if (isKursTiltak(tiltakstype.tiltakskode) && next.faneinnhold?.kurstittel.isNullOrBlank()) {
                add(ValidationError.ofCustomLocation("faneinnhold.kurstittel", "Du må skrive en kurstittel"))
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
                            TiltaksgjennomforingDbo::amoKategorisering,
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
                            TiltaksgjennomforingDbo::utdanningslop,
                            "Du må velge utdanningsprogram og lærefag på avtalen",
                        ),
                    )
                } else if (utdanningslop.utdanninger.isEmpty()) {
                    add(
                        ValidationError.of(
                            TiltaksgjennomforingDbo::utdanningslop,
                            "Du må velge minst ett lærefag",
                        ),
                    )
                } else if (utdanningslop.utdanningsprogram != avtale.utdanningslop?.utdanningsprogram?.id) {
                    add(
                        ValidationError.of(
                            TiltaksgjennomforingDbo::utdanningslop,
                            "Utdanningsprogrammet må være det samme som for avtalen: ${avtale.utdanningslop?.utdanningsprogram?.navn}",
                        ),
                    )
                } else {
                    val avtalensUtdanninger = avtale.utdanningslop.utdanninger.map { it.id }
                    if (!avtalensUtdanninger.containsAll(utdanningslop.utdanninger)) {
                        add(
                            ValidationError.of(
                                TiltaksgjennomforingDbo::utdanningslop,
                                "Lærefag må være valgt fra avtalens lærefag, minst ett av lærefagene mangler i avtalen.",
                            ),
                        )
                    }
                }
            }
            next = validateOrResetTilgjengeligForArrangorDato(next)

            if (previous == null) {
                validateCreateGjennomforing(next, avtale)
            } else {
                validateUpdateGjennomforing(next, previous, avtale)
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: next.right()
    }

    private fun validateOrResetTilgjengeligForArrangorDato(
        next: TiltaksgjennomforingDbo,
    ): TiltaksgjennomforingDbo {
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
                        TiltaksgjennomforingDbo::tilgjengeligForArrangorFraOgMedDato,
                        "Du må velge en dato som er etter dagens dato",
                    ),
                )
            } else if (tilgjengeligForArrangorDato < startDato.minusMonths(2)) {
                add(
                    ValidationError.of(
                        TiltaksgjennomforingDbo::tilgjengeligForArrangorFraOgMedDato,
                        "Du må velge en dato som er tidligst to måneder før gjennomføringens oppstartsdato",
                    ),
                )
            }

            if (tilgjengeligForArrangorDato > startDato) {
                add(
                    ValidationError.of(
                        TiltaksgjennomforingDbo::tilgjengeligForArrangorFraOgMedDato,
                        "Du må velge en dato som er før gjennomføringens oppstartsdato",
                    ),
                )
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: tilgjengeligForArrangorDato.right()
    }

    private fun MutableList<ValidationError>.validateCreateGjennomforing(
        gjennomforing: TiltaksgjennomforingDbo,
        avtale: AvtaleDto,
    ) {
        val arrangor = arrangorer.getById(gjennomforing.arrangorId)
        if (arrangor.slettetDato != null) {
            add(
                ValidationError.of(
                    TiltaksgjennomforingDbo::arrangorId,
                    "Arrangøren ${arrangor.navn} er slettet i Brønnøysundregistrene. Gjennomføringer kan ikke opprettes for slettede bedrifter.",
                ),
            )
        }

        if (gjennomforing.startDato.isBefore(avtale.startDato)) {
            add(
                ValidationError.of(
                    TiltaksgjennomforingDbo::startDato,
                    "Du må legge inn en startdato som er etter avtalens startdato",
                ),
            )
        }

        if (gjennomforing.stedForGjennomforing != null && gjennomforing.stedForGjennomforing.length > maksAntallTegnStedForGjennomforing) {
            add(
                ValidationError.of(
                    TiltaksgjennomforingDbo::stedForGjennomforing,
                    "Du kan bare skrive $maksAntallTegnStedForGjennomforing tegn i \"Sted for gjennomføring\"",
                ),
            )
        }

        if (avtale.status != AvtaleStatus.AKTIV) {
            add(
                ValidationError.of(
                    TiltaksgjennomforingDbo::avtaleId,
                    "Avtalen må være aktiv for å kunne opprette tiltak",
                ),
            )
        }
    }

    private fun MutableList<ValidationError>.validateUpdateGjennomforing(
        gjennomforing: TiltaksgjennomforingDbo,
        previous: TiltaksgjennomforingDto,
        avtale: AvtaleDto,
    ) {
        if (!previous.isAktiv()) {
            add(
                ValidationError.of(
                    TiltaksgjennomforingDbo::navn,
                    "Du kan ikke gjøre endringer på en gjennomføring som ikke er aktiv",
                ),
            )
        }

        if (gjennomforing.arrangorId != previous.arrangor.id) {
            add(
                ValidationError.of(
                    TiltaksgjennomforingDbo::arrangorId,
                    "Du kan ikke endre arrangør når gjennomføringen er aktiv",
                ),
            )
        }

        if (previous.status.status == TiltaksgjennomforingStatus.GJENNOMFORES) {
            if (gjennomforing.avtaleId != previous.avtaleId) {
                add(
                    ValidationError.of(
                        TiltaksgjennomforingDbo::avtaleId,
                        "Du kan ikke endre avtalen når gjennomføringen er aktiv",
                    ),
                )
            }

            if (gjennomforing.startDato.isBefore(avtale.startDato)) {
                add(
                    ValidationError.of(
                        TiltaksgjennomforingDbo::startDato,
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
                        TiltaksgjennomforingDbo::sluttDato,
                        "Du kan ikke sette en sluttdato bakover i tid når gjennomføringen er aktiv",
                    ),
                )
            }
        }
    }

    private fun MutableList<ValidationError>.validateKursTiltak(dbo: TiltaksgjennomforingDbo) {
        if (dbo.deltidsprosent <= 0) {
            add(
                ValidationError.of(
                    TiltaksgjennomforingDbo::deltidsprosent,
                    "Du må velge en deltidsprosent større enn 0",
                ),
            )
        } else if (dbo.deltidsprosent > 100) {
            add(
                ValidationError.of(
                    TiltaksgjennomforingDbo::deltidsprosent,
                    "Du må velge en deltidsprosent mindre enn 100",
                ),
            )
        }
    }
}
