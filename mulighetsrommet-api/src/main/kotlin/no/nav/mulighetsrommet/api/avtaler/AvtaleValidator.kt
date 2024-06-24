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
import no.nav.mulighetsrommet.api.routes.v1.responses.ValidationError
import no.nav.mulighetsrommet.api.services.NavEnhetService
import no.nav.mulighetsrommet.api.services.TiltakstypeService
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.Tiltakskoder
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.AmoKategorisering
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.domain.dto.allowedAvtaletypes
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class AvtaleValidator(
    private val tiltakstyper: TiltakstypeService,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
    private val navEnheterService: NavEnhetService,
    private val arrangorer: ArrangorRepository,
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
                if (!listOf(
                        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
                        Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
                    ).contains(Tiltakskode.fromArenaKode(tiltakstype.arenaKode)) &&
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

            if (!allowedAvtaletypes(Tiltakskode.fromArenaKode(tiltakstype.arenaKode)).contains(avtale.avtaletype)) {
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
                Tiltakskode.fromArenaKode(tiltakstype.arenaKode) == Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING &&
                avtale.amoKategorisering?.kurstype != null &&
                avtale.amoKategorisering.kurstype !== AmoKategorisering.Kurstype.STUDIESPESIALISERING &&
                avtale.amoKategorisering.spesifisering == null
            ) {
                add(ValidationError.ofCustomLocation("amoKategorisering.spesifisering", "Du må velge en spesifisering"))
            }

            if (
                Tiltakskode.fromArenaKode(tiltakstype.arenaKode) == Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING &&
                avtale.amoKategorisering?.kurstype != null &&
                avtale.amoKategorisering.kurstype !== AmoKategorisering.Kurstype.STUDIESPESIALISERING &&
                avtale.amoKategorisering.innholdElementer.isNullOrEmpty()
            ) {
                add(ValidationError.ofCustomLocation("amoKategorisering.innholdElementer", "Du må velge minst étt element"))
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

                gjennomforing.navEnheter.forEach { enhet: NavEnhetDbo ->
                    val enhetsnummer = enhet.enhetsnummer
                    if (enhetsnummer !in avtale.navEnheter) {
                        add(
                            ValidationError.of(
                                AvtaleDbo::navEnheter,
                                "NAV-enheten $enhetsnummer er i bruk på en av avtalens gjennomføringer, men mangler blant avtalens NAV-enheter",
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
        tiltakstyper.isEnabled(Tiltakskode.fromArenaKode(arenakode)) ||
            Tiltakskoder.TiltakMedAvtalerFraMulighetsrommet.contains(arenakode)
}
