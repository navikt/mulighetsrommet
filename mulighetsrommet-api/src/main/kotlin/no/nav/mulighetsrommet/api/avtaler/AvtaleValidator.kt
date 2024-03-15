package no.nav.mulighetsrommet.api.avtaler

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.right
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.VirksomhetRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.ValidationError
import no.nav.mulighetsrommet.api.services.NavEnhetService
import no.nav.mulighetsrommet.api.services.TiltakstypeService
import no.nav.mulighetsrommet.domain.Tiltakskoder
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.domain.dto.TiltakstypeAdminDto
import no.nav.mulighetsrommet.domain.dto.allowedAvtaletypes
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class AvtaleValidator(
    private val tiltakstyper: TiltakstypeService,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
    private val navEnheterService: NavEnhetService,
    private val virksomheter: VirksomhetRepository,
) {
    fun validate(avtale: AvtaleDbo, previous: AvtaleAdminDto?): Either<List<ValidationError>, AvtaleDbo> = either {
        val tiltakstype = tiltakstyper.getById(avtale.tiltakstypeId)
            ?: raise(ValidationError.of(AvtaleDbo::tiltakstypeId, "Tiltakstypen finnes ikke").nel())

        if (isTiltakstypeDisabled(previous, tiltakstype)) {
            return ValidationError
                .of(
                    AvtaleDbo::tiltakstypeId,
                    "Opprettelse av avtale for tiltakstype: '${tiltakstype.navn}' er ikke skrudd på enda.",
                )
                .nel()
                .left()
        }

        val errors = buildList {
            if (avtale.administratorer.isEmpty()) {
                add(ValidationError.of(AvtaleDbo::administratorer, "Minst én administrator må være valgt"))
            }

            if (avtale.sluttDato != null && avtale.sluttDato.isBefore(avtale.startDato)) {
                add(ValidationError.of(AvtaleDbo::startDato, "Startdato må være før sluttdato"))
            }

            addAll(validateNavEnheter(avtale.navEnheter))

            if (avtale.leverandorUnderenheter.isEmpty()) {
                add(
                    ValidationError.of(
                        AvtaleDbo::leverandorUnderenheter,
                        "Minst én underenhet til leverandøren må være valgt",
                    ),
                )
            }

            if (!allowedAvtaletypes(tiltakstype.arenaKode).contains(avtale.avtaletype)) {
                add(
                    ValidationError.of(
                        AvtaleDbo::avtaletype,
                        "${avtale.avtaletype} er ikke tillat for tiltakstype ${tiltakstype.navn}",
                    ),
                )
            } else {
                if (avtale.avtaletype != Avtaletype.Forhaandsgodkjent && avtale.sluttDato == null) {
                    add(ValidationError.of(AvtaleDbo::sluttDato, "Sluttdato må være satt"))
                }
            }

            previous?.also { currentAvtale ->
                if (avtale.navn.length < 5 && currentAvtale.opphav != ArenaMigrering.Opphav.ARENA) {
                    add(ValidationError.of(AvtaleDbo::navn, "Avtalenavn må være minst 5 tegn langt"))
                }

                /**
                 * Når avtalen har blitt godkjent så skal alle datafelter som påvirker økonomien, påmelding, osv. være låst.
                 *
                 * Vi mangler fortsatt en del innsikt og løsning rundt tilsagn og refusjon (f.eks. når blir avtalen godkjent?),
                 * så reglene for når en avtale er låst er foreløpig ganske naive og baserer seg kun på om det finnes
                 * gjennomføringer på avtalen eller ikke...
                 */
                    val (numGjennomforinger, gjennomforinger) = tiltaksgjennomforinger.getAll(avtaleId = avtale.id)
                if (numGjennomforinger > 0) {
                    if (avtale.tiltakstypeId != currentAvtale.tiltakstype.id) {
                        add(
                            ValidationError.of(
                                AvtaleDbo::tiltakstypeId,
                                "Tiltakstype kan ikke endres fordi det finnes gjennomføringer for avtalen",
                            ),
                        )
                    }

                    if (avtale.avtaletype != currentAvtale.avtaletype) {
                        add(
                            ValidationError.of(
                                AvtaleDbo::avtaletype,
                                "Avtaletype kan ikke endres fordi det finnes gjennomføringer for avtalen",
                            ),
                        )
                    }

                    gjennomforinger.forEach { gjennomforing ->
                        val arrangor = gjennomforing.arrangor.id
                        if (arrangor !in avtale.leverandorUnderenheter) {
                            val virksomhet: VirksomhetDto = virksomheter.getById(arrangor)
                            add(
                                ValidationError.of(
                                    AvtaleDbo::leverandorUnderenheter,
                                    "Arrangøren ${virksomhet.navn} er i bruk på en av avtalens gjennomføringer, men mangler blandt leverandørens underenheter",
                                ),
                            )
                        }

                        gjennomforing.navEnheter.forEach { enhet: NavEnhetDbo ->
                            val enhetsnummer = enhet.enhetsnummer
                            if (enhetsnummer !in avtale.navEnheter) {
                                add(
                                    ValidationError.of(
                                        AvtaleDbo::navEnheter,
                                        "NAV-enheten $enhetsnummer er i bruk på en av avtalens gjennomføringer, men mangler blandt avtalens NAV-enheter",
                                    ),
                                )
                            }
                        }

                        if (gjennomforing.startDato.isBefore(avtale.startDato)) {
                            val gjennomforingsStartDato = gjennomforing.startDato.format(
                                DateTimeFormatter.ofLocalizedDate(
                                    FormatStyle.SHORT,
                                ),
                            )
                            add(
                                ValidationError.of(
                                    AvtaleDbo::startDato,
                                    "Startdato kan ikke være før startdatoen til tiltaksgjennomføringer koblet til avtalen. Minst en gjennomføring har startdato: $gjennomforingsStartDato",
                                ),
                            )
                        }
                    }
                }

                if (skalValidereArenafelter(currentAvtale, tiltakstype)) {
                    if (avtale.navn != currentAvtale.navn) {
                        add(ValidationError.of(AvtaleDbo::navn, "Navn kan ikke endres utenfor Arena"))
                    }

                    if (avtale.avtalenummer != currentAvtale.avtalenummer) {
                        add(
                            ValidationError.of(
                                AvtaleDbo::avtalenummer,
                                "Avtalenummer kan ikke endres utenfor Arena",
                            ),
                        )
                    }

                    if (avtale.startDato != currentAvtale.startDato) {
                        add(ValidationError.of(AvtaleDbo::startDato, "Startdato kan ikke endres utenfor Arena"))
                    }

                    if (avtale.sluttDato != currentAvtale.sluttDato) {
                        add(ValidationError.of(AvtaleDbo::sluttDato, "Sluttdato kan ikke endres utenfor Arena"))
                    }

                    if (avtale.avtaletype != currentAvtale.avtaletype) {
                        add(ValidationError.of(AvtaleDbo::avtaletype, "Avtaletype kan ikke endres utenfor Arena"))
                    }

                    if (avtale.prisbetingelser != currentAvtale.prisbetingelser) {
                        add(
                            ValidationError.of(
                                AvtaleDbo::prisbetingelser,
                                "Pris- og betalingsinformasjon kan ikke endres utenfor Arena",
                            ),
                        )
                    }

                    if (avtale.leverandorVirksomhetId != currentAvtale.leverandor.id) {
                        add(
                            ValidationError.of(
                                AvtaleDbo::leverandorVirksomhetId,
                                "Leverandøren kan ikke endres utenfor Arena",
                            ),
                        )
                    }
                }
            } ?: run {
                if (avtale.navn.length < 5) {
                    add(ValidationError.of(AvtaleDbo::navn, "Avtalenavn må være minst 5 tegn langt"))
                }
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: avtale.right()
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

    private fun skalValidereArenafelter(
        avtale: AvtaleAdminDto,
        tiltakstype: TiltakstypeAdminDto,
    ): Boolean {
        return avtale.opphav == ArenaMigrering.Opphav.ARENA && !isEnabled(tiltakstype.arenaKode)
    }

    private fun isTiltakstypeDisabled(
        previous: AvtaleAdminDto?,
        tiltakstype: TiltakstypeAdminDto,
    ): Boolean {
        val kanIkkeOppretteAvtale = previous == null && !isEnabled(tiltakstype.arenaKode)

        val kanIkkeRedigereTiltakstypeForAvtale = previous != null &&
            tiltakstype.arenaKode != previous.tiltakstype.arenaKode &&
            !isEnabled(tiltakstype.arenaKode)

        return kanIkkeOppretteAvtale || kanIkkeRedigereTiltakstypeForAvtale
    }

    private fun isEnabled(arenakode: String) =
        tiltakstyper.isEnabled(arenakode) || Tiltakskoder.TiltakMedAvtalerFraMulighetsrommet.contains(arenakode)
}
