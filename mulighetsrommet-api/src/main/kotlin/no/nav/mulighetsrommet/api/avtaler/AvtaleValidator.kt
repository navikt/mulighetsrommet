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
import no.nav.mulighetsrommet.api.domain.dto.TiltakstypeAdminDto
import no.nav.mulighetsrommet.api.repositories.ArrangorRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.routes.v1.Opsjonsmodell
import no.nav.mulighetsrommet.api.routes.v1.responses.ValidationError
import no.nav.mulighetsrommet.api.services.NavEnhetService
import no.nav.mulighetsrommet.api.services.TiltakstypeService
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.Tiltakskoder
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.AmoKategorisering
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.domain.dto.allowedAvtaletypes
import no.nav.mulighetsrommet.unleash.UnleashService

class AvtaleValidator(
    private val tiltakstyper: TiltakstypeService,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
    private val navEnheterService: NavEnhetService,
    private val arrangorer: ArrangorRepository,
    private val unleashService: UnleashService,
) {

    fun validate(avtale: AvtaleDbo, currentAvtale: AvtaleAdminDto?): Either<List<ValidationError>, AvtaleDbo> = either {
        val tiltakstype = tiltakstyper.getById(avtale.tiltakstypeId)
            ?: raise(ValidationError.of(AvtaleDbo::tiltakstypeId, "Tiltakstypen finnes ikke").nel())

        if (isTiltakstypeDisabled(currentAvtale, tiltakstype)) {
            return ValidationError
                .of(
                    AvtaleDbo::tiltakstypeId,
                    "Opprettelse av avtale for tiltakstype: '${tiltakstype.navn}' er ikke skrudd på enda.",
                )
                .nel()
                .left()
        }

        val errors = buildList {
            if (avtale.navn.length < 5 && currentAvtale?.opphav != ArenaMigrering.Opphav.ARENA) {
                add(ValidationError.of(AvtaleDbo::navn, "Avtalenavn må være minst 5 tegn langt"))
            }

            if (avtale.administratorer.isEmpty()) {
                add(ValidationError.of(AvtaleDbo::administratorer, "Du må velge minst én administrator"))
            }

            if (avtale.sluttDato != null) {
                if (avtale.sluttDato.isBefore(avtale.startDato)) {
                    add(ValidationError.of(AvtaleDbo::startDato, "Startdato må være før sluttdato"))
                }
                if (
                    // Unntak for de som ikke er tatt over fra arena siden man ikke får endre avtaletype på de
                    !listOf(Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING, Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING).contains(tiltakstype.tiltakskode) &&
                    !avtaleTypeErForhandsgodkjent(avtale.avtaletype) &&
                    avtale.startDato.plusYears(5).isBefore(avtale.sluttDato)
                ) {
                    add(
                        ValidationError.of(
                            AvtaleDbo::sluttDato,
                            "Avtaleperioden kan ikke vare lenger enn 5 år for anskaffede tiltak",
                        ),
                    )
                }
            }

            if (unleashService.isEnabled("mulighetsrommet.admin-flate.registrere-opsjonsmodell")) {
                if (!avtaleTypeErForhandsgodkjent(avtale.avtaletype)) {
                    if (avtale.opsjonMaksVarighet == null) {
                        add(
                            ValidationError.of(
                                AvtaleDbo::opsjonMaksVarighet,
                                "Du må legge inn maks varighet for opsjonen",
                            ),
                        )
                    }

                    if (avtale.opsjonsmodell == null) {
                        add(ValidationError.of(AvtaleDbo::opsjonsmodell, "Du må velge en opsjonsmodell"))
                    }

                    if (avtale.opsjonsmodell != null && avtale.opsjonsmodell == Opsjonsmodell.ANNET) {
                        if (avtale.customOpsjonsmodellNavn.isNullOrBlank()) {
                            add(
                                ValidationError.of(
                                    AvtaleDbo::customOpsjonsmodellNavn,
                                    "Du må beskrive opsjonsmodellen",
                                ),
                            )
                        }
                    }
                }

                if (currentAvtale?.opsjonerRegistrert?.isNotEmpty() == true && avtale.opsjonsmodell != currentAvtale.opsjonsmodellData?.opsjonsmodell) {
                    add(
                        ValidationError.of(
                            AvtaleDbo::opsjonsmodell,
                            "Du kan ikke endre opsjonsmodell når opsjoner er registrert",
                        ),
                    )
                }
            }

            if (avtale.avtaletype.kreverWebsaknummer() && avtale.websaknummer == null) {
                add(ValidationError.of(AvtaleDbo::websaknummer, "Du må skrive inn Websaknummer til avtalesaken"))
            }

            if (avtale.arrangorUnderenheter.isEmpty()) {
                add(
                    ValidationError.of(
                        AvtaleDbo::arrangorUnderenheter,
                        "Du må velge minst én underenhet for tiltaksarrangør",
                    ),
                )
            }

            if (!allowedAvtaletypes(tiltakstype.tiltakskode).contains(avtale.avtaletype)) {
                add(
                    ValidationError.of(
                        AvtaleDbo::avtaletype,
                        "${avtale.avtaletype} er ikke tillatt for tiltakstype ${tiltakstype.navn}",
                    ),
                )
            } else {
                if (avtale.avtaletype != Avtaletype.Forhaandsgodkjent && avtale.sluttDato == null) {
                    add(ValidationError.of(AvtaleDbo::sluttDato, "Du må legge inn sluttdato for avtalen"))
                }
            }

            if (
                tiltakstype.tiltakskode == Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING &&
                avtale.amoKategorisering?.kurstype != null &&
                avtale.amoKategorisering.kurstype !== AmoKategorisering.Kurstype.STUDIESPESIALISERING &&
                avtale.amoKategorisering.spesifisering == null
            ) {
                add(ValidationError.ofCustomLocation("amoKategorisering.spesifisering", "Du må velge en spesifisering"))
            }

            if (
                tiltakstype.tiltakskode == Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING &&
                avtale.amoKategorisering?.kurstype != null &&
                avtale.amoKategorisering.kurstype !== AmoKategorisering.Kurstype.STUDIESPESIALISERING &&
                avtale.amoKategorisering.innholdElementer.isNullOrEmpty()
            ) {
                add(
                    ValidationError.ofCustomLocation(
                        "amoKategorisering.innholdElementer",
                        "Du må velge minst ett element",
                    ),
                )
            }

            validateNavEnheter(avtale.navEnheter)

            if (currentAvtale == null) {
                validateCreateAvtale(avtale)
            } else {
                validateUpdateAvtale(avtale, currentAvtale, tiltakstype)
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: avtale.right()
    }

    private fun MutableList<ValidationError>.validateCreateAvtale(
        avtale: AvtaleDbo,
    ) {
        val hovedenhet = arrangorer.getById(avtale.arrangorId)

        if (hovedenhet.slettetDato != null) {
            add(
                ValidationError.of(
                    AvtaleDbo::arrangorId,
                    "Arrangøren ${hovedenhet.navn} er slettet i Brønnøysundregistrene. Avtaler kan ikke opprettes for slettede bedrifter.",
                ),
            )
        }

        avtale.arrangorUnderenheter.forEach { underenhetId ->
            val underenhet = arrangorer.getById(underenhetId)

            if (underenhet.slettetDato != null) {
                add(
                    ValidationError.of(
                        AvtaleDbo::arrangorUnderenheter,
                        "Arrangøren ${underenhet.navn} er slettet i Brønnøysundregistrene. Avtaler kan ikke opprettes for slettede bedrifter.",
                    ),
                )
            }

            if (underenhet.overordnetEnhet != hovedenhet.organisasjonsnummer) {
                add(
                    ValidationError.of(
                        AvtaleDbo::arrangorUnderenheter,
                        "Arrangøren ${underenhet.navn} er ikke en gyldig underenhet til hovedenheten ${hovedenhet.navn}.",
                    ),
                )
            }
        }
    }

    private fun MutableList<ValidationError>.validateUpdateAvtale(
        avtale: AvtaleDbo,
        currentAvtale: AvtaleAdminDto,
        tiltakstype: TiltakstypeAdminDto,
    ) {
        val (numGjennomforinger, gjennomforinger) = tiltaksgjennomforinger.getAll(avtaleId = avtale.id)

        /**
         * Når avtalen har blitt godkjent så skal alle datafelter som påvirker økonomien, påmelding, osv. være låst.
         *
         * Vi mangler fortsatt en del innsikt og løsning rundt tilsagn og refusjon (f.eks. når blir avtalen godkjent?),
         * så reglene for når en avtale er låst er foreløpig ganske naive og baserer seg kun på om det finnes
         * gjennomføringer på avtalen eller ikke...
         */
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
                val arrangorId = gjennomforing.arrangor.id
                if (arrangorId !in avtale.arrangorUnderenheter) {
                    val arrangor = arrangorer.getById(arrangorId)
                    add(
                        ValidationError.of(
                            AvtaleDbo::arrangorUnderenheter,
                            "Arrangøren ${arrangor.navn} er i bruk på en av avtalens gjennomføringer, men mangler blant tiltaksarrangørens underenheter",
                        ),
                    )
                }

                if (gjennomforing.startDato.isBefore(avtale.startDato)) {
                    val gjennomforingsStartDato = gjennomforing.startDato.formaterDatoTilEuropeiskDatoformat()
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

            if (avtale.arrangorId != currentAvtale.arrangor.id) {
                add(
                    ValidationError.of(
                        AvtaleDbo::arrangorId,
                        "Tiltaksarrangøren kan ikke endres utenfor Arena",
                    ),
                )
            }
        }
    }

    private fun MutableList<ValidationError>.validateNavEnheter(navEnheter: List<String>) {
        val actualNavEnheter = resolveNavEnheter(navEnheter)

        if (!actualNavEnheter.any { it.value.type == Norg2Type.FYLKE }) {
            add(ValidationError.of(AvtaleDbo::navEnheter, "Du må velge minst én NAV-region"))
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
        return avtale.opphav == ArenaMigrering.Opphav.ARENA && !isEnabled(tiltakstype.tiltakskode)
    }

    private fun isTiltakstypeDisabled(
        previous: AvtaleAdminDto?,
        tiltakstype: TiltakstypeAdminDto,
    ): Boolean {
        val kanIkkeOppretteAvtale = previous == null && !isEnabled(tiltakstype.tiltakskode)

        val kanIkkeRedigereTiltakstypeForAvtale = previous != null &&
            tiltakstype.tiltakskode != previous.tiltakstype.tiltakskode &&
            !isEnabled(tiltakstype.tiltakskode)

        return kanIkkeOppretteAvtale || kanIkkeRedigereTiltakstypeForAvtale
    }

    private fun isEnabled(tiltakskode: Tiltakskode?) =
        tiltakstyper.isEnabled(tiltakskode) ||
            Tiltakskoder.TiltakMedAvtalerFraMulighetsrommet.contains(tiltakskode)
}

private fun avtaleTypeErForhandsgodkjent(avtaletype: Avtaletype): Boolean {
    return listOf(Avtaletype.Forhaandsgodkjent).contains(avtaletype)
}
