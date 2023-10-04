package no.nav.mulighetsrommet.api.tiltaksgjennomforinger

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.right
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.routes.v1.TiltaksgjennomforingRequest
import no.nav.mulighetsrommet.api.routes.v1.responses.ValidationError
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus.APENT_FOR_INNSOK
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus.GJENNOMFORES

class TiltaksgjennomforingRequestValidator(
    private val avtaler: AvtaleRepository,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
) {
    fun validate(request: TiltaksgjennomforingRequest): Either<List<ValidationError>, TiltaksgjennomforingRequest> =
        either {
            val avtale = avtaler.get(request.avtaleId)
            ensureNotNull(avtale) {
                ValidationError("avtaleId", "Avtalen finnes ikke").nel()
            }

            ensure(avtale.tiltakstype.id == request.tiltakstypeId) {
                ValidationError("tiltakstypeId", "Tiltakstypen må være den samme som avtalen").nel()
            }

            val errors = buildList {
                if (request.startDato.isBefore(avtale.startDato)) {
                    add(ValidationError("startDato", "Startdato må være etter avtalens startdato"))
                }

                if ((request.stengtFra != null) != (request.stengtTil != null)) {
                    add(ValidationError("stengtFra", "Både stengt fra og til må være satt"))
                }

                if (request.stengtTil?.isBefore(request.stengtFra) == true) {
                    add(ValidationError("stengtFra", "Stengt fra må være før stengt til"))
                }

                if (request.antallPlasser <= 0) {
                    add(ValidationError("antallPlasser", "Antall plasser må være større enn 0"))
                }

                if (request.startDato.isAfter(avtale.sluttDato)) {
                    add(ValidationError("startDato", "Startdato må være før avtalens sluttdato"))
                }

                if (request.sluttDato != null && request.sluttDato.isAfter(avtale.sluttDato)) {
                    add(ValidationError("sluttDato", "Sluttdato må være før avtalens sluttdato"))
                }

                if (request.sluttDato != null && request.startDato.isAfter(request.sluttDato)) {
                    add(ValidationError("startDato", "Startdato må være før sluttdato"))
                }

                if (request.navEnheter.isEmpty()) {
                    add(ValidationError("navEnheter", "Minst ett NAV-kontor må være valgt"))
                }

                tiltaksgjennomforinger.get(request.id)?.also { gjennomforing ->
                    if (request.opphav != gjennomforing.opphav) {
                        add(ValidationError("opphav", "Avtalens opphav kan ikke endres"))
                    }

                    if (gjennomforing.status !in listOf(APENT_FOR_INNSOK, GJENNOMFORES)) {
                        add(ValidationError("navn", "Kan bare gjøre endringer når gjennomføringen er aktiv"))
                    }

                    if (gjennomforing.status == GJENNOMFORES) {
                        if (request.avtaleId != gjennomforing.avtaleId) {
                            add(ValidationError("avtaleId", "Avtalen kan ikke endres når gjennomføringen er aktiv"))
                        }

                        if (request.oppstart != gjennomforing.oppstart) {
                            add(
                                ValidationError(
                                    "oppstart",
                                    "Oppstartstype kan ikke endres når gjennomføringen er aktiv",
                                ),
                            )
                        }

                        if (request.startDato != gjennomforing.startDato) {
                            add(ValidationError("startDato", "Startdato kan ikke endres når gjennomføringen er aktiv"))
                        }

                        if (request.sluttDato != gjennomforing.sluttDato) {
                            add(ValidationError("sluttDato", "Sluttdato kan ikke endres når gjennomføringen er aktiv"))
                        }

                        if (request.antallPlasser != gjennomforing.antallPlasser) {
                            add(
                                ValidationError(
                                    "antallPlasser",
                                    "Antall plasser kan ikke endres når gjennomføringen er aktiv",
                                ),
                            )
                        }
                    }
                } ?: run {
                    if (request.opphav != ArenaMigrering.Opphav.MR_ADMIN_FLATE) {
                        add(ValidationError("opphav", "Opphav må være MR_ADMIN_FLATE"))
                    }
                }
            }

            return errors.takeIf { it.isNotEmpty() }?.left() ?: request.right()
        }
}
