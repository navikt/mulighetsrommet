package no.nav.mulighetsrommet.api.avtale

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.right
import no.nav.mulighetsrommet.api.Queries
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.domain.dto.Prismodell
import no.nav.mulighetsrommet.domain.dto.allowedAvtaletypes
import no.nav.mulighetsrommet.unleash.Toggle
import no.nav.mulighetsrommet.unleash.UnleashService

class AvtaleValidator(
    private val db: Database,
    private val tiltakstyper: TiltakstypeService,
    private val navEnheterService: NavEnhetService,
    private val unleash: UnleashService,
) {
    private val opsjonsmodellerUtenValidering =
        listOf(Opsjonsmodell.AVTALE_UTEN_OPSJONSMODELL, Opsjonsmodell.AVTALE_VALGFRI_SLUTTDATO)

    fun validate(avtale: AvtaleDbo, currentAvtale: AvtaleDto?): Either<List<ValidationError>, AvtaleDbo> = db.session {
        val tiltakstype = tiltakstyper.getById(avtale.tiltakstypeId)
            ?: return@session ValidationError.of(AvtaleDbo::tiltakstypeId, "Tiltakstypen finnes ikke").nel().left()

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
                    Avtaletype.Forhaandsgodkjent != avtale.avtaletype &&
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

            if (Avtaletype.Forhaandsgodkjent != avtale.avtaletype && !opsjonsmodellerUtenValidering.contains(avtale.opsjonsmodell)) {
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

            if (currentAvtale?.opsjonerRegistrert?.isNotEmpty() == true && avtale.avtaletype != currentAvtale.avtaletype) {
                add(
                    ValidationError.of(
                        AvtaleDbo::avtaletype,
                        "Du kan ikke endre avtaletype når opsjoner er registrert",
                    ),
                )
            }

            if (currentAvtale?.opsjonerRegistrert?.isNotEmpty() == true && avtale.opsjonsmodell != currentAvtale.opsjonsmodellData?.opsjonsmodell) {
                add(
                    ValidationError.of(
                        AvtaleDbo::opsjonsmodell,
                        "Du kan ikke endre opsjonsmodell når opsjoner er registrert",
                    ),
                )
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
                if (avtale.avtaletype != Avtaletype.Forhaandsgodkjent && avtale.opsjonsmodell != Opsjonsmodell.AVTALE_VALGFRI_SLUTTDATO && avtale.sluttDato == null) {
                    add(ValidationError.of(AvtaleDbo::sluttDato, "Du må legge inn sluttdato for avtalen"))
                }
            }

            if (unleash.isEnabledForTiltakstype(Toggle.MIGRERING_OKONOMI, tiltakstype.tiltakskode!!)) {
                if (avtale.prismodell == null) {
                    add(ValidationError.of(AvtaleDbo::prismodell, "Du må velge en prismodell"))
                } else if (avtale.avtaletype == Avtaletype.Forhaandsgodkjent && avtale.prismodell != Prismodell.FORHANDSGODKJENT) {
                    add(ValidationError.of(AvtaleDbo::prismodell, "Prismodellen må være forhåndsgodkjent"))
                } else if (avtale.avtaletype != Avtaletype.Forhaandsgodkjent && avtale.prismodell == Prismodell.FORHANDSGODKJENT) {
                    add(ValidationError.of(AvtaleDbo::prismodell, "Prismodellen kan ikke være forhåndsgodkjent"))
                }
            } else if (avtale.prismodell != null) {
                add(
                    ValidationError.of(
                        AvtaleDbo::prismodell,
                        "Prismodell kan foreløpig ikke velges for tiltakstypen ${tiltakstype.tiltakskode}",
                    ),
                )
            }

            if (tiltakstype.tiltakskode == Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING && avtale.amoKategorisering == null) {
                add(ValidationError.ofCustomLocation("amoKategorisering.kurstype", "Du må velge en kurstype"))
            }

            if (tiltakstype.tiltakskode == Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING) {
                val utdanninger = avtale.utdanningslop
                if (utdanninger == null) {
                    add(
                        ValidationError.of(
                            AvtaleDbo::utdanningslop,
                            "Du må velge et utdanningsprogram og minst ett lærefag",
                        ),
                    )
                } else if (utdanninger.utdanninger.isEmpty()) {
                    add(ValidationError.of(AvtaleDbo::utdanningslop, "Du må velge minst ett lærefag"))
                }
            }

            validateNavEnheter(avtale.navEnheter)
            validateAdministratorer(avtale)

            if (currentAvtale == null) {
                validateCreateAvtale(avtale)
            } else {
                validateUpdateAvtale(avtale, currentAvtale)
            }
        }

        errors.takeIf { it.isNotEmpty() }?.left() ?: avtale.right()
    }

    private fun MutableList<ValidationError>.validateCreateAvtale(
        avtale: AvtaleDbo,
    ) = db.session {
        val hovedenhet = Queries.arrangor.getById(avtale.arrangorId)

        if (hovedenhet.slettetDato != null) {
            add(
                ValidationError.of(
                    AvtaleDbo::arrangorId,
                    "Arrangøren ${hovedenhet.navn} er slettet i Brønnøysundregistrene. Avtaler kan ikke opprettes for slettede bedrifter.",
                ),
            )
        }

        avtale.arrangorUnderenheter.forEach { underenhetId ->
            val underenhet = Queries.arrangor.getById(underenhetId)

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
        currentAvtale: AvtaleDto,
    ) = db.session {
        val (numGjennomforinger, gjennomforinger) = Queries.gjennomforing.getAll(avtaleId = avtale.id)

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

            gjennomforinger.forEach { gjennomforing ->
                val arrangorId = gjennomforing.arrangor.id
                if (arrangorId !in avtale.arrangorUnderenheter) {
                    val arrangor = Queries.arrangor.getById(arrangorId)
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
                            "Startdato kan ikke være etter startdatoen til gjennomføringer koblet til avtalen. Minst en gjennomføring har startdato: $gjennomforingsStartDato",
                        ),
                    )
                }

                gjennomforing.utdanningslop?.also {
                    if (avtale.utdanningslop?.utdanningsprogram != it.utdanningsprogram.id) {
                        add(
                            ValidationError.of(
                                AvtaleDbo::utdanningslop,
                                "Utdanningsprogram kan ikke endres fordi en gjennomføring allerede er opprettet for utdanningsprogrammet ${it.utdanningsprogram.navn}",
                            ),
                        )
                    }

                    it.utdanninger.forEach { utdanning ->
                        val utdanninger = avtale.utdanningslop?.utdanninger ?: listOf()
                        if (!utdanninger.contains(utdanning.id)) {
                            add(
                                ValidationError.of(
                                    AvtaleDbo::utdanningslop,
                                    "Lærefaget ${utdanning.navn} mangler i avtalen, men er i bruk på en av avtalens gjennomføringer",
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun MutableList<ValidationError>.validateAdministratorer(
        next: AvtaleDbo,
    ) {
        val slettedeNavIdenter = db.session {
            next.administratorer.mapNotNull { ident ->
                Queries.ansatt.getByNavIdent(ident)?.takeIf { it.skalSlettesDato != null }?.navIdent?.value
            }
        }

        if (slettedeNavIdenter.isNotEmpty()) {
            add(
                ValidationError.of(
                    AvtaleDbo::administratorer,
                    "Administratorene med Nav ident " + slettedeNavIdenter.joinToString(", ") + " er slettet og må fjernes",
                ),
            )
        }
    }

    private fun MutableList<ValidationError>.validateNavEnheter(navEnheter: List<String>) {
        val actualNavEnheter = resolveNavEnheter(navEnheter)

        if (!actualNavEnheter.any { it.value.type == Norg2Type.FYLKE }) {
            add(ValidationError.of(AvtaleDbo::navEnheter, "Du må velge minst én Nav-region"))
        }

        navEnheter.forEach { enhet ->
            if (!actualNavEnheter.containsKey(enhet)) {
                add(
                    ValidationError.of(
                        AvtaleDbo::navEnheter,
                        "Nav-enheten $enhet passer ikke i avtalens kontorstruktur",
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
