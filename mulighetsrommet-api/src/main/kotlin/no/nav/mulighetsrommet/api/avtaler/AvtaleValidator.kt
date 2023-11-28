package no.nav.mulighetsrommet.api.avtaler

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.right
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.ValidationError
import no.nav.mulighetsrommet.api.services.NavEnhetService
import no.nav.mulighetsrommet.domain.Tiltakskoder.isTiltakMedAvtalerFraMulighetsrommet
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.env.NaisEnv

class AvtaleValidator(
    private val tiltakstyper: TiltakstypeRepository,
    private val avtaler: AvtaleRepository,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
    private val navEnheterService: NavEnhetService,
) {
    fun validate(dbo: AvtaleDbo): Either<List<ValidationError>, AvtaleDbo> = either {
        val tiltakstype = tiltakstyper.get(dbo.tiltakstypeId)
            ?: raise(ValidationError.of(AvtaleDbo::tiltakstypeId, "Tiltakstypen finnes ikke").nel())

        val errors = buildList {
            if (dbo.administratorer.isEmpty()) {
                add(ValidationError.of(AvtaleDbo::administratorer, "Minst én administrator må være valgt"))
            }

            if (!dbo.startDato.isBefore(dbo.sluttDato)) {
                add(ValidationError.of(AvtaleDbo::startDato, "Startdato må være før sluttdato"))
            }

            addAll(validateNavEnheter(dbo.navEnheter))

            if (dbo.leverandorUnderenheter.isEmpty()) {
                add(
                    ValidationError.of(
                        AvtaleDbo::leverandorUnderenheter,
                        "Minst én underenhet til leverandøren må være valgt",
                    ),
                )
            }

            avtaler.get(dbo.id)?.also { avtale ->
                if (dbo.opphav != avtale.opphav) {
                    add(ValidationError.of(AvtaleDbo::opphav, "Avtalens opphav kan ikke endres"))
                }

                /**
                 * Når avtalen har blitt godkjent så skal alle datafelter som påvirker økonomien, påmelding, osv. være låst.
                 *
                 * Vi mangler fortsatt en del innsikt og løsning rundt tilsagn og refusjon (f.eks. når blir avtalen godkjent?),
                 * så reglene for når en avtale er låst er foreløpig ganske naive og baserer seg kun på om det finnes
                 * gjennomføringer på avtalen eller ikke...
                 */
                    val (numGjennomforinger, gjennomforinger) = tiltaksgjennomforinger.getAll(avtaleId = dbo.id)
                if (numGjennomforinger > 0) {
                    if (dbo.tiltakstypeId != avtale.tiltakstype.id) {
                        add(
                            ValidationError.of(
                                AvtaleDbo::tiltakstypeId,
                                "Tiltakstype kan ikke endres fordi det finnes gjennomføringer for avtalen",
                            ),
                        )
                    }

                    if (dbo.avtaletype != avtale.avtaletype) {
                        add(
                            ValidationError.of(
                                AvtaleDbo::avtaletype,
                                "Avtaletype kan ikke endres fordi det finnes gjennomføringer for avtalen",
                            ),
                        )
                    }

                    gjennomforinger.forEach { gjennomforing ->
                        val arrangor = gjennomforing.arrangor.organisasjonsnummer
                        if (arrangor !in dbo.leverandorUnderenheter) {
                            add(
                                ValidationError.of(
                                    AvtaleDbo::leverandorUnderenheter,
                                    "Arrangøren $arrangor er i bruk på en av avtalens gjennomføringer, men mangler blandt leverandørens underenheter",
                                ),
                            )
                        }

                        gjennomforing.navEnheter.forEach { enhet ->
                            val enhetsnummer = enhet.enhetsnummer
                            if (enhetsnummer !in dbo.navEnheter) {
                                add(
                                    ValidationError.of(
                                        AvtaleDbo::navEnheter,
                                        "NAV-enheten $enhetsnummer er i bruk på en av avtalens gjennomføringer, men mangler blandt avtalens NAV-enheter",
                                    ),
                                )
                            }
                        }
                    }
                }

                if (avtale.opphav == ArenaMigrering.Opphav.ARENA) {
                    if (dbo.navn != avtale.navn) {
                        add(ValidationError.of(AvtaleDbo::navn, "Navn kan ikke endres utenfor Arena"))
                    }

                    if (dbo.tiltakstypeId != avtale.tiltakstype.id) {
                        add(
                            ValidationError.of(
                                AvtaleDbo::tiltakstypeId,
                                "Tiltakstype kan ikke endres utenfor Arena",
                            ),
                        )
                    }

                    if (dbo.avtalenummer != avtale.avtalenummer) {
                        add(
                            ValidationError.of(
                                AvtaleDbo::avtalenummer,
                                "Avtalenummer kan ikke endres utenfor Arena",
                            ),
                        )
                    }

                    if (dbo.startDato != avtale.startDato) {
                        add(ValidationError.of(AvtaleDbo::startDato, "Startdato kan ikke endres utenfor Arena"))
                    }

                    if (dbo.sluttDato != avtale.sluttDato) {
                        add(ValidationError.of(AvtaleDbo::sluttDato, "Sluttdato kan ikke endres utenfor Arena"))
                    }

                    if (dbo.avtaletype != avtale.avtaletype) {
                        add(ValidationError.of(AvtaleDbo::avtaletype, "Avtaletype kan ikke endres utenfor Arena"))
                    }

                    if (dbo.prisbetingelser != avtale.prisbetingelser) {
                        add(
                            ValidationError.of(
                                AvtaleDbo::prisbetingelser,
                                "Pris- og betalingsinformasjon kan ikke endres utenfor Arena",
                            ),
                        )
                    }

                    if (dbo.leverandorOrganisasjonsnummer != avtale.leverandor.organisasjonsnummer) {
                        add(
                            ValidationError.of(
                                AvtaleDbo::leverandorOrganisasjonsnummer,
                                "Leverandøren kan ikke endres utenfor Arena",
                            ),
                        )
                    }
                }
            } ?: run {
                if (!isTiltakMedAvtalerFraMulighetsrommet(tiltakstype.arenaKode) && NaisEnv.current().isProdGCP()) {
                    add(
                        ValidationError.of(
                            AvtaleDbo::tiltakstypeId,
                            message = "Avtaler kan bare opprettes når de gjelder for tiltakstypene AFT eller VTA",
                        ),
                    )
                }

                if (dbo.opphav != ArenaMigrering.Opphav.MR_ADMIN_FLATE) {
                    add(ValidationError.of(AvtaleDbo::opphav, "Opphav må være MR_ADMIN_FLATE"))
                }
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: dbo.right()
    }

    private fun validateNavEnheter(navEnheter: List<String>) = buildList {
        val actualNavEnheter = resolveNavEnheter(navEnheter)

        if (!actualNavEnheter.any { it.value.type == Norg2Type.FYLKE }) {
            add(ValidationError.of(AvtaleDbo::navEnheter, "Minst én NAV-region må være valgt"))
        }

        navEnheter.forEach { enhet ->
            if (!actualNavEnheter.containsKey(enhet)) {
                add(
                    ValidationError.of(
                        AvtaleDbo::navEnheter,
                        "NAV-enheten $enhet passer ikke i avtalens kontorstruktur",
                    ),
                )
            }
        }
    }

    private fun resolveNavEnheter(enhetsnummer: List<String>): Map<String, NavEnhetDbo> {
        val navEnheter = enhetsnummer.mapNotNull { navEnheterService.hentEnhet(it) }
        return navEnheter
            .filter { it.type == Norg2Type.FYLKE }
            .flatMap { listOf(it) + navEnheter.filter { enhet -> enhet.overordnetEnhet == it.enhetsnummer } }
            .associateBy { it.enhetsnummer }
    }
}
