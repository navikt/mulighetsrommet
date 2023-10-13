package no.nav.mulighetsrommet.api.tiltaksgjennomforinger

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.right
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.ValidationError
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.Avtalestatus
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus.APENT_FOR_INNSOK
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus.GJENNOMFORES

class TiltaksgjennomforingValidator(
    private val avtaler: AvtaleRepository,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
) {
    fun validate(dbo: TiltaksgjennomforingDbo): Either<List<ValidationError>, TiltaksgjennomforingDbo> = either {
        val avtale = avtaler.get(dbo.avtaleId)
        ensureNotNull(avtale) {
            ValidationError("avtaleId", "Avtalen finnes ikke").nel()
        }

        ensure(avtale.tiltakstype.id == dbo.tiltakstypeId) {
            ValidationError("tiltakstypeId", "Tiltakstypen må være den samme som for avtalen").nel()
        }

        ensure(avtale.avtalestatus in listOf(Avtalestatus.Planlagt, Avtalestatus.Aktiv)) {
            ValidationError(
                "avtaleId",
                "Kan ikke endre gjennomføring fordi avtalen har status ${avtale.avtalestatus}",
            ).nel()
        }

        val errors = buildList {
            if (dbo.startDato.isBefore(avtale.startDato)) {
                add(ValidationError("startDato", "Startdato må være etter avtalens startdato"))
            }

            if ((dbo.stengtFra != null) != (dbo.stengtTil != null)) {
                add(ValidationError("stengtFra", "Både stengt fra og til må være satt"))
            }

            if (dbo.stengtTil?.isBefore(dbo.stengtFra) == true) {
                add(ValidationError("stengtFra", "Stengt fra må være før stengt til"))
            }

            if (dbo.antallPlasser <= 0) {
                add(ValidationError("antallPlasser", "Antall plasser må være større enn 0"))
            }

            if (dbo.startDato.isAfter(avtale.sluttDato)) {
                add(ValidationError("startDato", "Startdato må være før avtalens sluttdato"))
            }

            if (dbo.sluttDato != null && dbo.sluttDato.isAfter(avtale.sluttDato)) {
                add(ValidationError("sluttDato", "Sluttdato må være før avtalens sluttdato"))
            }

            if (dbo.sluttDato != null && dbo.startDato.isAfter(dbo.sluttDato)) {
                add(ValidationError("startDato", "Startdato må være før sluttdato"))
            }

            if (dbo.navEnheter.isEmpty()) {
                add(ValidationError("navEnheter", "Minst ett NAV-kontor må være valgt"))
            }

            val avtaleNavEnheter = avtale.navEnheter.associateBy { it.enhetsnummer }
            dbo.navEnheter.forEach { enhetsnummer ->
                if (!avtaleNavEnheter.containsKey(enhetsnummer)) {
                    add(ValidationError("navEnheter", "NAV-enhet $enhetsnummer mangler i avtalen"))
                }
            }

            val avtaleHasArrangor = avtale.leverandorUnderenheter.any {
                it.organisasjonsnummer == dbo.arrangorOrganisasjonsnummer
            }
            if (!avtaleHasArrangor) {
                add(ValidationError("arrangorOrganisasjonsnummer", "Arrangøren mangler i avtalen"))
            }

            tiltaksgjennomforinger.get(dbo.id)?.also { gjennomforing ->
                ensure(gjennomforing.status in listOf(APENT_FOR_INNSOK, GJENNOMFORES)) {
                    plus(ValidationError("navn", "Kan bare gjøre endringer når gjennomføringen er aktiv"))
                }

                ensure(dbo.opphav == gjennomforing.opphav) {
                    plus(ValidationError("opphav", "Avtalens opphav kan ikke endres"))
                }

                if (gjennomforing.status == GJENNOMFORES) {
                    if (dbo.avtaleId != gjennomforing.avtaleId) {
                        add(ValidationError("avtaleId", "Avtalen kan ikke endres når gjennomføringen er aktiv"))
                    }

                    if (dbo.oppstart != gjennomforing.oppstart) {
                        add(
                            ValidationError(
                                "oppstart",
                                "Oppstartstype kan ikke endres når gjennomføringen er aktiv",
                            ),
                        )
                    }

                    if (dbo.startDato != gjennomforing.startDato) {
                        add(ValidationError("startDato", "Startdato kan ikke endres når gjennomføringen er aktiv"))
                    }

                    if (dbo.sluttDato != gjennomforing.sluttDato) {
                        add(ValidationError("sluttDato", "Sluttdato kan ikke endres når gjennomføringen er aktiv"))
                    }

                    if (dbo.antallPlasser != gjennomforing.antallPlasser) {
                        add(
                            ValidationError(
                                "antallPlasser",
                                "Antall plasser kan ikke endres når gjennomføringen er aktiv",
                            ),
                        )
                    }
                }
            } ?: run {
                if (dbo.opphav != ArenaMigrering.Opphav.MR_ADMIN_FLATE) {
                    add(ValidationError("opphav", "Opphav må være MR_ADMIN_FLATE"))
                }
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: dbo.right()
    }
}
