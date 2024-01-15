package no.nav.mulighetsrommet.api.tiltaksgjennomforinger

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.right
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.ValidationError
import no.nav.mulighetsrommet.domain.Tiltakskoder
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus.GJENNOMFORES

class TiltaksgjennomforingValidator(
    private val avtaler: AvtaleRepository,
    private val deltakere: DeltakerRepository,
) {
    fun validate(
        dbo: TiltaksgjennomforingDbo,
        previousDto: TiltaksgjennomforingAdminDto?,
    ): Either<List<ValidationError>, TiltaksgjennomforingDbo> = either {
        val avtale = avtaler.get(dbo.avtaleId)
            ?: raise(ValidationError.of(TiltaksgjennomforingDbo::avtaleId, "Avtalen finnes ikke").nel())

        val errors = buildList {
            if (avtale.tiltakstype.id != dbo.tiltakstypeId) {
                add(
                    ValidationError.of(
                        TiltaksgjennomforingDbo::tiltakstypeId,
                        "Tiltakstypen må være den samme som for avtalen",
                    ),
                )
            }

            if (dbo.administratorer.isEmpty()) {
                add(
                    ValidationError.of(
                        TiltaksgjennomforingDbo::administratorer,
                        "Minst én administrator må være valgt",
                    ),
                )
            }

            if (dbo.sluttDato != null && dbo.startDato.isAfter(dbo.sluttDato)) {
                add(ValidationError.of(TiltaksgjennomforingDbo::startDato, "Startdato må være før sluttdato"))
            }

            if ((dbo.stengtFra != null) != (dbo.stengtTil != null)) {
                add(ValidationError.of(TiltaksgjennomforingDbo::stengtFra, "Både stengt fra og til må være satt"))
            }

            if (dbo.stengtTil?.isBefore(dbo.stengtFra) == true) {
                add(ValidationError.of(TiltaksgjennomforingDbo::stengtFra, "Stengt fra må være før stengt til"))
            }

            if (dbo.antallPlasser <= 0) {
                add(ValidationError.of(TiltaksgjennomforingDbo::antallPlasser, "Antall plasser må være større enn 0"))
            }

            if (Tiltakskoder.isKursTiltak(avtale.tiltakstype.arenaKode)) {
                validateKursTiltak(dbo)
            }

            if (!Tiltakskoder.isTiltakMedAvtalerFraMulighetsrommet(avtale.tiltakstype.arenaKode)) {
                if (dbo.startDato.isBefore(avtale.startDato)) {
                    add(
                        ValidationError.of(
                            TiltaksgjennomforingDbo::startDato,
                            "Startdato må være etter avtalens startdato",
                        ),
                    )
                }

                if (dbo.startDato.isAfter(avtale.sluttDato)) {
                    add(
                        ValidationError.of(
                            TiltaksgjennomforingDbo::startDato,
                            "Startdato må være før avtalens sluttdato",
                        ),
                    )
                }
            }

            if (dbo.navEnheter.isEmpty()) {
                add(ValidationError.of(TiltaksgjennomforingDbo::navEnheter, "Minst ett NAV-kontor må være valgt"))
            }

            if (!avtale.kontorstruktur.any { it.region.enhetsnummer == dbo.navRegion }) {
                add(
                    ValidationError.of(
                        TiltaksgjennomforingDbo::navEnheter,
                        "NAV-region ${dbo.navRegion} mangler i avtalen",
                    ),
                )
            }

            val avtaleNavEnheter = avtale.kontorstruktur.flatMap { it.kontorer }.associateBy { it.enhetsnummer }
            dbo.navEnheter.forEach { enhetsnummer ->
                if (!avtaleNavEnheter.containsKey(enhetsnummer)) {
                    add(
                        ValidationError.of(
                            TiltaksgjennomforingDbo::navEnheter,
                            "NAV-enhet $enhetsnummer mangler i avtalen",
                        ),
                    )
                }
            }

            val avtaleHasArrangor = avtale.leverandorUnderenheter.any {
                it.organisasjonsnummer == dbo.arrangorOrganisasjonsnummer
            }
            if (!avtaleHasArrangor) {
                add(
                    ValidationError.of(
                        TiltaksgjennomforingDbo::arrangorOrganisasjonsnummer,
                        "Arrangøren mangler i avtalen",
                    ),
                )
            }

            previousDto?.also { gjennomforing ->
                if (!gjennomforing.isAktiv()) {
                    add(
                        ValidationError.of(
                            TiltaksgjennomforingDbo::navn,
                            "Kan bare gjøre endringer når gjennomføringen er aktiv",
                        ),
                    )
                }

                if (dbo.opphav != gjennomforing.opphav) {
                    add(ValidationError.of(TiltaksgjennomforingDbo::opphav, "Opphav kan ikke endres"))
                }

                val antallDeltagere = deltakere.getAll(gjennomforing.id).size
                if (antallDeltagere > 0 && dbo.oppstart != gjennomforing.oppstart) {
                    add(
                        ValidationError.of(
                            TiltaksgjennomforingDbo::oppstart,
                            "Oppstartstype kan ikke endres når det finnes påmeldte deltakere",
                        ),
                    )
                }

                if (gjennomforing.status == GJENNOMFORES) {
                    if (dbo.avtaleId != gjennomforing.avtaleId) {
                        add(
                            ValidationError.of(
                                TiltaksgjennomforingDbo::avtaleId,
                                "Avtalen kan ikke endres når gjennomføringen er aktiv",
                            ),
                        )
                    }

                    if (dbo.startDato != gjennomforing.startDato) {
                        add(
                            ValidationError.of(
                                TiltaksgjennomforingDbo::startDato,
                                "Startdato kan ikke endres når gjennomføringen er aktiv",
                            ),
                        )
                    }

                    if (dbo.sluttDato != gjennomforing.sluttDato) {
                        add(
                            ValidationError.of(
                                TiltaksgjennomforingDbo::sluttDato,
                                "Sluttdato kan ikke endres når gjennomføringen er aktiv",
                            ),
                        )
                    }

                    if (dbo.antallPlasser != gjennomforing.antallPlasser) {
                        add(
                            ValidationError.of(
                                TiltaksgjennomforingDbo::antallPlasser,
                                "Antall plasser kan ikke endres når gjennomføringen er aktiv",
                            ),
                        )
                    }

                    if (dbo.arrangorOrganisasjonsnummer != gjennomforing.arrangor.organisasjonsnummer) {
                        add(
                            ValidationError.of(
                                TiltaksgjennomforingDbo::arrangorOrganisasjonsnummer,
                                "Arrangøren kan ikke endres når gjennomføringen er aktiv",
                            ),
                        )
                    }
                }
            } ?: run {
                if (dbo.opphav != ArenaMigrering.Opphav.MR_ADMIN_FLATE) {
                    add(ValidationError.of(TiltaksgjennomforingDbo::opphav, "Opphav må være MR_ADMIN_FLATE"))
                }
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: dbo.right()
    }

    private fun MutableList<ValidationError>.validateKursTiltak(dbo: TiltaksgjennomforingDbo) {
        if (dbo.deltidsprosent <= 0) {
            add(
                ValidationError.of(
                    TiltaksgjennomforingDbo::deltidsprosent,
                    "Deltidsprosent må være større enn 0",
                ),
            )
        } else if (dbo.deltidsprosent > 100) {
            add(
                ValidationError.of(
                    TiltaksgjennomforingDbo::deltidsprosent,
                    "Deltidsprosent kan ikke være større enn 100",
                ),
            )
        }
    }
}
