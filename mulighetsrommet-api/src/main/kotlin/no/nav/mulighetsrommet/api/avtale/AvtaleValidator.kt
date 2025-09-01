package no.nav.mulighetsrommet.api.avtale

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.right
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.model.*
import no.nav.mulighetsrommet.api.navenhet.*
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.Avtaletyper
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Tiltakskode

class AvtaleValidator(
    private val db: ApiDatabase,
    private val tiltakstyper: TiltakstypeService,
    private val navEnheterService: NavEnhetService,
) {
    private val opsjonsmodellerUtenValidering = listOf(
        OpsjonsmodellType.INGEN_OPSJONSMULIGHET,
        OpsjonsmodellType.VALGFRI_SLUTTDATO,
    )

    fun validate(avtale: AvtaleDbo, currentAvtale: AvtaleDto?): Either<List<FieldError>, AvtaleDbo> {
        val tiltakstype = tiltakstyper.getById(avtale.tiltakstypeId)
            ?: return FieldError.of(AvtaleDbo::tiltakstypeId, "Tiltakstypen finnes ikke").nel().left()

        val tiltakskode = tiltakstype.tiltakskode
            ?: return FieldError.of(AvtaleDbo::tiltakstypeId, "Tiltakstypen mangler tiltalkskode").nel().left()

        val errors = buildList {
            if (avtale.navn.length < 5 && currentAvtale?.opphav != ArenaMigrering.Opphav.ARENA) {
                add(FieldError.of(AvtaleDbo::navn, "Avtalenavn må være minst 5 tegn langt"))
            }

            if (avtale.administratorer.isEmpty()) {
                add(FieldError.of(AvtaleDbo::administratorer, "Du må velge minst én administrator"))
            }

            if (avtale.sluttDato != null && avtale.sluttDato.isBefore(avtale.startDato)) {
                add(FieldError.of(AvtaleDbo::startDato, "Startdato må være før sluttdato"))
            }

            if (avtale.arrangor?.underenheter?.isEmpty() == true) {
                add(
                    FieldError.ofPointer(
                        "/arrangorUnderenheter",
                        "Du må velge minst én underenhet for tiltaksarrangør",
                    ),
                )
            }

            if (avtale.avtaletype.kreverSakarkivNummer() && avtale.sakarkivNummer == null) {
                add(FieldError.of(AvtaleDbo::sakarkivNummer, "Du må skrive inn saksnummer til avtalesaken"))
            }

            if (avtale.avtaletype !in Avtaletyper.getAvtaletyperForTiltak(tiltakskode)) {
                add(
                    FieldError.of(
                        AvtaleDbo::avtaletype,
                        "${avtale.avtaletype.beskrivelse} er ikke tillatt for tiltakstype ${tiltakstype.navn}",
                    ),
                )
            }

            if (avtale.avtaletype == Avtaletype.FORHANDSGODKJENT) {
                if (avtale.opsjonsmodell.type != OpsjonsmodellType.VALGFRI_SLUTTDATO) {
                    add(
                        FieldError.of(
                            AvtaleDbo::opsjonsmodell,
                            "Du må velge opsjonsmodell med valgfri sluttdato når avtalen er forhåndsgodkjent",
                        ),
                    )
                }
            } else {
                if (avtale.opsjonsmodell.type != OpsjonsmodellType.VALGFRI_SLUTTDATO && avtale.sluttDato == null) {
                    add(FieldError.of(AvtaleDbo::sluttDato, "Du må legge inn sluttdato for avtalen"))
                }

                if (avtale.opsjonsmodell.type !in opsjonsmodellerUtenValidering) {
                    if (avtale.opsjonsmodell.opsjonMaksVarighet == null) {
                        add(
                            FieldError.of(
                                "Du må legge inn maks varighet for opsjonen",
                                AvtaleDbo::opsjonsmodell,
                                Opsjonsmodell::opsjonMaksVarighet,
                            ),
                        )
                    }

                    if (avtale.opsjonsmodell.type == OpsjonsmodellType.ANNET) {
                        if (avtale.opsjonsmodell.customOpsjonsmodellNavn.isNullOrBlank()) {
                            add(
                                FieldError.of(
                                    "Du må beskrive opsjonsmodellen",
                                    AvtaleDbo::opsjonsmodell,
                                    Opsjonsmodell::customOpsjonsmodellNavn,
                                ),
                            )
                        }
                    }
                }
            }

            if (tiltakskode == Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING && avtale.amoKategorisering == null) {
                add(FieldError.ofPointer("/amoKategorisering.kurstype", "Du må velge en kurstype"))
            }

            if (tiltakskode == Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING) {
                val utdanninger = avtale.utdanningslop
                if (utdanninger == null) {
                    add(
                        FieldError.of(
                            AvtaleDbo::utdanningslop,
                            "Du må velge et utdanningsprogram og minst ett lærefag",
                        ),
                    )
                } else if (utdanninger.utdanninger.isEmpty()) {
                    add(FieldError.of(AvtaleDbo::utdanningslop, "Du må velge minst ett lærefag"))
                }
            }

            validatePrismodell(avtale, tiltakskode, tiltakstype.navn)
            validateNavEnheter(avtale.navEnheter)
            validateAdministratorer(avtale)

            if (currentAvtale == null) {
                validateCreateAvtale(avtale)
            } else {
                validateUpdateAvtale(avtale, currentAvtale)
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left()
            ?: avtale.copy(navEnheter = sanitizeNavEnheter(avtale.navEnheter)).right()
    }

    private fun MutableList<FieldError>.validateCreateAvtale(
        avtale: AvtaleDbo,
    ) = db.session {
        if (avtale.arrangor?.hovedenhet !== null) {
            val hovedenhet = queries.arrangor.getById(avtale.arrangor.hovedenhet)

            if (hovedenhet.slettetDato != null) {
                add(
                    FieldError.of(
                        "Arrangøren ${hovedenhet.navn} er slettet i Brønnøysundregistrene. Avtaler kan ikke opprettes for slettede bedrifter.",
                        AvtaleDbo::arrangor,
                        AvtaleDbo.Arrangor::hovedenhet,
                    ),
                )
            }

            avtale.arrangor.underenheter.forEach { underenhetId ->
                val underenhet = queries.arrangor.getById(underenhetId)

                if (underenhet.slettetDato != null) {
                    add(
                        FieldError.of(
                            "Arrangøren ${underenhet.navn} er slettet i Brønnøysundregistrene. Avtaler kan ikke opprettes for slettede bedrifter.",
                            AvtaleDbo::arrangor,
                            AvtaleDbo.Arrangor::underenheter,
                        ),
                    )
                }

                if (underenhet.overordnetEnhet != hovedenhet.organisasjonsnummer) {
                    add(
                        FieldError.of(
                            "Arrangøren ${underenhet.navn} er ikke en gyldig underenhet til hovedenheten ${hovedenhet.navn}.",
                            AvtaleDbo::arrangor,
                            AvtaleDbo.Arrangor::underenheter,
                        ),
                    )
                }
            }
        }
    }

    private fun MutableList<FieldError>.validateUpdateAvtale(
        avtale: AvtaleDbo,
        currentAvtale: AvtaleDto,
    ) = db.session {
        if (currentAvtale.opsjonerRegistrert?.isNotEmpty() == true) {
            if (avtale.avtaletype != currentAvtale.avtaletype) {
                add(
                    FieldError.of(
                        AvtaleDbo::avtaletype,
                        "Du kan ikke endre avtaletype når opsjoner er registrert",
                    ),
                )
            }

            if (avtale.opsjonsmodell.type != currentAvtale.opsjonsmodell.type) {
                add(
                    FieldError.of(
                        AvtaleDbo::opsjonsmodell,
                        "Du kan ikke endre opsjonsmodell når opsjoner er registrert",
                    ),
                )
            }
        }

        val (numGjennomforinger, gjennomforinger) = queries.gjennomforing.getAll(avtaleId = avtale.id)

        /**
         * Når avtalen har blitt godkjent så skal alle datafelter som påvirker økonomien, påmelding, osv. være låst.
         *
         * Vi mangler fortsatt en del innsikt og løsning rundt tilsagn og utbetaling (f.eks. når blir avtalen godkjent?),
         * så reglene for når en avtale er låst er foreløpig ganske naive og baserer seg kun på om det finnes
         * gjennomføringer på avtalen eller ikke...
         */
        if (numGjennomforinger > 0) {
            if (avtale.tiltakstypeId != currentAvtale.tiltakstype.id) {
                add(
                    FieldError.ofPointer(
                        "/tiltakskode",
                        "Tiltakstype kan ikke endres fordi det finnes gjennomføringer for avtalen",
                    ),
                )
            }

            val earliestGjennomforingStartDato = gjennomforinger.minBy { it.startDato }.startDato
            if (earliestGjennomforingStartDato.isBefore(avtale.startDato)) {
                add(
                    FieldError.of(
                        AvtaleDbo::startDato,
                        "Startdato kan ikke være etter startdatoen til gjennomføringer koblet til avtalen. Minst en gjennomføring har startdato: ${earliestGjennomforingStartDato.formaterDatoTilEuropeiskDatoformat()}",
                    ),
                )
            }

            gjennomforinger.forEach { gjennomforing ->
                val arrangorId = gjennomforing.arrangor.id

                if (avtale.arrangor == null) {
                    add(
                        FieldError.of(
                            "Arrangør kan ikke fjernes fordi en gjennomføring er koblet til avtalen",
                            AvtaleDbo::arrangor,
                            AvtaleDbo.Arrangor::hovedenhet,
                        ),
                    )
                } else if (arrangorId !in avtale.arrangor.underenheter) {
                    val arrangor = queries.arrangor.getById(arrangorId)
                    add(
                        FieldError.of(
                            "Arrangøren ${arrangor.navn} er i bruk på en av avtalens gjennomføringer, men mangler blant tiltaksarrangørens underenheter",
                            AvtaleDbo::arrangor,
                            AvtaleDbo.Arrangor::underenheter,
                        ),
                    )
                }

                gjennomforing.utdanningslop?.also {
                    if (avtale.utdanningslop?.utdanningsprogram != it.utdanningsprogram.id) {
                        add(
                            FieldError.of(
                                AvtaleDbo::utdanningslop,
                                "Utdanningsprogram kan ikke endres fordi en gjennomføring allerede er opprettet for utdanningsprogrammet ${it.utdanningsprogram.navn}",
                            ),
                        )
                    }

                    it.utdanninger.forEach { utdanning ->
                        val utdanninger = avtale.utdanningslop?.utdanninger ?: listOf()
                        if (!utdanninger.contains(utdanning.id)) {
                            add(
                                FieldError.of(
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

    private fun MutableList<FieldError>.validatePrismodell(
        next: AvtaleDbo,
        tiltakskode: Tiltakskode,
        tiltakstypeNavn: String,
    ) {
        if (next.prismodell !in Prismodeller.getPrismodellerForTiltak(tiltakskode)) {
            add(
                FieldError.of(
                    AvtaleDbo::prismodell,
                    "${next.prismodell.beskrivelse} er ikke tillatt for tiltakstype $tiltakstypeNavn",
                ),
            )
        }
        if (next.prismodell in listOf(
                Prismodell.AVTALT_PRIS_PER_MANEDSVERK,
                Prismodell.AVTALT_PRIS_PER_UKESVERK,
            ) &&
            next.satser.isEmpty()
        ) {
            add(
                FieldError.of(
                    AvtaleDbo::prismodell,
                    "Minst én periode er påkrevd",
                ),
            )
        }
        next.satser.forEachIndexed { index, sats ->
            if (sats.sats <= 0) {
                add(FieldError.ofPointer("/satser/$index/pris", "Pris må være positiv"))
            }
        }
        for (i in next.satser.indices) {
            val a = next.satser[i]
            for (j in i + 1 until next.satser.size) {
                val b = next.satser[j]

                if (a.periode.intersects(b.periode)) {
                    add(FieldError.ofPointer("/satser/$i/periodeStart", "Overlappende perioder"))
                    add(FieldError.ofPointer("/satser/$j/periodeStart", "Overlappende perioder"))
                }
            }
        }
    }

    private fun MutableList<FieldError>.validateAdministratorer(
        next: AvtaleDbo,
    ) {
        val slettedeNavIdenter = db.session {
            next.administratorer.mapNotNull { ident ->
                queries.ansatt.getByNavIdent(ident)?.takeIf { it.skalSlettesDato != null }?.navIdent?.value
            }
        }

        if (slettedeNavIdenter.isNotEmpty()) {
            add(
                FieldError.of(
                    AvtaleDbo::administratorer,
                    "Administratorene med Nav ident " + slettedeNavIdenter.joinToString(", ") + " er slettet og må fjernes",
                ),
            )
        }
    }

    private fun MutableList<FieldError>.validateNavEnheter(navEnheter: Set<NavEnhetNummer>) {
        val actualNavEnheter = resolveNavEnheter(navEnheter)

        if (!actualNavEnheter.any { it.value.type == NavEnhetType.FYLKE }) {
            add(FieldError.ofPointer("/navRegioner", "Du må velge minst én Nav-region"))
        }

        if (!actualNavEnheter.any { it.value.type != NavEnhetType.FYLKE }) {
            add(FieldError.ofPointer("/navKontorer", "Du må velge minst én Nav-enhet"))
        }
    }

    private fun resolveNavEnheter(enhetsnummer: Set<NavEnhetNummer>): Map<NavEnhetNummer, NavEnhetDto> {
        val navEnheter = enhetsnummer.mapNotNull { navEnheterService.hentEnhet(it) }
        return navEnheter
            .filter { it.type == NavEnhetType.FYLKE }
            .flatMap { listOf(it) + navEnheter.filter { enhet -> enhet.overordnetEnhet == it.enhetsnummer } }
            .associateBy { it.enhetsnummer }
    }

    fun sanitizeNavEnheter(navEnheter: Set<NavEnhetNummer>): Set<NavEnhetNummer> {
        // Filtrer vekk underenheter uten fylke
        return NavEnhetHelpers.buildNavRegioner(
            navEnheter.mapNotNull { navEnheterService.hentEnhet(it) },
        )
            .flatMap { it.enheter.map { it.enhetsnummer } + it.enhetsnummer }
            .toSet()
    }
}
