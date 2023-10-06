package no.nav.mulighetsrommet.api.avtaler

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.ValidationError
import no.nav.mulighetsrommet.domain.Tiltakskoder.isTiltakMedAvtalerFraMulighetsrommet
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.domain.dto.Avtalestatus
import no.nav.mulighetsrommet.domain.dto.TiltakstypeDto

class AvtaleValidator(
    private val tiltakstyper: TiltakstypeRepository,
    private val avtaler: AvtaleRepository,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
) {
    fun validate(dbo: AvtaleDbo): Either<List<ValidationError>, AvtaleDbo> = either {
        val tiltakstype = tiltakstyper.get(dbo.tiltakstypeId)
            ?: raise(ValidationError("tiltakstype", "Tiltakstypen finnes ikke").nel())

        val errors = buildList {
            if (!dbo.startDato.isBefore(dbo.sluttDato)) {
                add(ValidationError("startDato", "Startdato må være før sluttdato"))
            }

            if (dbo.navEnheter.isEmpty()) {
                add(ValidationError("navEnheter", "Minst ett NAV-kontor må være valgt"))
            }

            if (dbo.leverandorUnderenheter.isEmpty()) {
                add(ValidationError("leverandorUnderenheter", "Minst én underenhet til leverandøren må være valgt"))
            }

            avtaler.get(dbo.id)?.also { avtale ->
                ensure(avtale.avtalestatus in listOf(Avtalestatus.Planlagt, Avtalestatus.Aktiv)) {
                    plus(
                        ValidationError(
                            "navn",
                            "Kan bare gjøre endringer når avtalen har status Planlagt eller Aktiv",
                        ),
                    )
                }

                ensure(dbo.opphav == avtale.opphav) {
                    plus(ValidationError("opphav", "Avtalens opphav kan ikke endres"))
                }

                val (numGjennomforinger) = tiltaksgjennomforinger.getAll(avtaleId = dbo.id)
                if (numGjennomforinger > 0) {
                    if (dbo.tiltakstypeId != avtale.tiltakstype.id) {
                        add(
                            ValidationError(
                                "tiltakstypeId",
                                "Kan ikke endre tiltakstype fordi det finnes gjennomføringer for avtalen",
                            ),
                        )
                    }
                    // TODO validere at start- og slutt-dato passer med gjennomføringer... men ta høyde for dette ikke gjelder AFT/VTA?
                }

                if (avtaleIsLocked(avtale, tiltakstype)) {
                    if (dbo.tiltakstypeId != tiltakstype.id) {
                        add(ValidationError("tiltakstypeId", "Tiltakstype kan ikke endres når avtalen er aktiv"))
                    }

                    if (dbo.avtaletype != avtale.avtaletype) {
                        add(ValidationError("avtaletype", "Avtaletype kan ikke endres når avtalen er aktiv"))
                    }

                    if (dbo.startDato != avtale.startDato) {
                        add(ValidationError("startDato", "Startdato kan ikke endres når avtalen er aktiv"))
                    }

                    if (dbo.sluttDato != avtale.sluttDato) {
                        add(ValidationError("sluttDato", "Sluttdato kan ikke endres når avtalen er aktiv"))
                    }

                    if (avtale.navRegion != null && dbo.navRegion != avtale.navRegion?.enhetsnummer) {
                        add(ValidationError("navRegion", "NAV-region kan ikke endres når avtalen er aktiv"))
                    }

                    if (dbo.prisbetingelser != avtale.prisbetingelser) {
                        add(
                            ValidationError(
                                "prisbetingelser",
                                "Pris- og betalingsinformasjon kan ikke endres når avtalen er aktiv",
                            ),
                        )
                    }

                    if (dbo.leverandorOrganisasjonsnummer != avtale.leverandor.organisasjonsnummer) {
                        add(
                            ValidationError(
                                "leverandorOrganisasjonsnummer",
                                "Leverandøren kan ikke endres når avtalen er aktiv",
                            ),
                        )
                    }
                }
            } ?: run {
                if (!isTiltakMedAvtalerFraMulighetsrommet(tiltakstype.arenaKode)) {
                    add(
                        ValidationError(
                            name = "tiltakstype",
                            message = "Avtaler kan bare opprettes når de gjelder for tiltakstypene AFT eller VTA",
                        ),
                    )
                }

                if (dbo.opphav != ArenaMigrering.Opphav.MR_ADMIN_FLATE) {
                    add(ValidationError("opphav", "Opphav må være MR_ADMIN_FLATE"))
                }
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: dbo.right()
    }

    /**
     * Når avtalen har blitt godkjent så skal alle datafelter som påvirker økonomien være låst.
     *
     * Vi mangler fortsatt en del innsikt og løsning rundt tilsagn og refursjon (f.eks. når blir avtalen godkjent?),
     * så reglene for når en avtale er låst er foreløpig ganske naive...
     */
    private fun avtaleIsLocked(avtale: AvtaleAdminDto, tiltakstype: TiltakstypeDto): Boolean {
        val avtaleErAktiv = avtale.avtalestatus == Avtalestatus.Aktiv
        val avtaleErIkkeForhaandsgodkjent = !isTiltakMedAvtalerFraMulighetsrommet(tiltakstype.arenaKode)
        return avtaleErAktiv && avtaleErIkkeForhaandsgodkjent
    }
}
