package no.nav.mulighetsrommet.api.avtale

import arrow.core.*
import arrow.core.raise.either
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.avtale.api.AvtaleRequest
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.mapper.AvtaleDboMapper
import no.nav.mulighetsrommet.api.avtale.model.*
import no.nav.mulighetsrommet.api.navenhet.*
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.model.*
import java.time.LocalDate

class AvtaleValidator(
    private val db: ApiDatabase,
    private val tiltakstyper: TiltakstypeService,
    private val arrangorService: ArrangorService,
    private val navEnheterService: NavEnhetService,
) {
    private val opsjonsmodellerUtenValidering = listOf(
        OpsjonsmodellType.INGEN_OPSJONSMULIGHET,
        OpsjonsmodellType.VALGFRI_SLUTTDATO,
    )

    suspend fun validate(
        request: AvtaleRequest,
        previous: AvtaleDto?,
    ): Either<List<FieldError>, AvtaleDbo> = either {
        val tiltakstype = tiltakstyper.getByTiltakskode(request.tiltakskode)
        val tiltakskode = tiltakstype.tiltakskode
            ?: return FieldError.of(AvtaleRequest::tiltakskode, "Tiltakstypen mangler tiltalkskode").nel().left()

        val arrangor = request.arrangor?.let {
            val (arrangor, underenheter) = syncArrangorerFromBrreg(
                it.hovedenhet,
                it.underenheter,
            ).bind()
            AvtaleDbo.Arrangor(
                hovedenhet = arrangor.id,
                underenheter = underenheter.map { underenhet -> underenhet.id },
                kontaktpersoner = it.kontaktpersoner,
            )
        }

        val errors = buildList {
            if (request.navn.length < 5 && previous?.opphav != ArenaMigrering.Opphav.ARENA) {
                add(FieldError.of(AvtaleRequest::navn, "Avtalenavn må være minst 5 tegn langt"))
            }

            if (request.administratorer.isEmpty()) {
                add(FieldError.of(AvtaleRequest::administratorer, "Du må velge minst én administrator"))
            }

            if (request.sluttDato != null && request.sluttDato.isBefore(request.startDato)) {
                add(FieldError.of(AvtaleRequest::startDato, "Startdato må være før sluttdato"))
            }

            if (request.arrangor?.underenheter?.isEmpty() == true) {
                add(
                    FieldError.ofPointer(
                        "/arrangorUnderenheter",
                        "Du må velge minst én underenhet for tiltaksarrangør",
                    ),
                )
            }

            if (request.avtaletype.kreverSakarkivNummer() && request.sakarkivNummer == null) {
                add(FieldError.of(AvtaleRequest::sakarkivNummer, "Du må skrive inn saksnummer til avtalesaken"))
            }

            if (request.avtaletype !in Avtaletyper.getAvtaletyperForTiltak(tiltakskode)) {
                add(
                    FieldError.of(
                        AvtaleRequest::avtaletype,
                        "${request.avtaletype.beskrivelse} er ikke tillatt for tiltakstype ${tiltakstype.navn}",
                    ),
                )
            }

            if (request.avtaletype == Avtaletype.FORHANDSGODKJENT) {
                if (request.opsjonsmodell.type != OpsjonsmodellType.VALGFRI_SLUTTDATO) {
                    add(
                        FieldError.of(
                            AvtaleRequest::opsjonsmodell,
                            "Du må velge opsjonsmodell med valgfri sluttdato når avtalen er forhåndsgodkjent",
                        ),
                    )
                }
            } else {
                if (request.opsjonsmodell.type != OpsjonsmodellType.VALGFRI_SLUTTDATO && request.sluttDato == null) {
                    add(FieldError.of(AvtaleRequest::sluttDato, "Du må legge inn sluttdato for avtalen"))
                }

                if (request.opsjonsmodell.type !in opsjonsmodellerUtenValidering) {
                    if (request.opsjonsmodell.opsjonMaksVarighet == null) {
                        add(
                            FieldError.of(
                                "Du må legge inn maks varighet for opsjonen",
                                AvtaleRequest::opsjonsmodell,
                                Opsjonsmodell::opsjonMaksVarighet,
                            ),
                        )
                    }

                    if (request.opsjonsmodell.type == OpsjonsmodellType.ANNET) {
                        if (request.opsjonsmodell.customOpsjonsmodellNavn.isNullOrBlank()) {
                            add(
                                FieldError.of(
                                    "Du må beskrive opsjonsmodellen",
                                    AvtaleRequest::opsjonsmodell,
                                    Opsjonsmodell::customOpsjonsmodellNavn,
                                ),
                            )
                        }
                    }
                }
            }

            if (tiltakskode == Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING && request.amoKategorisering == null) {
                add(FieldError.ofPointer("/amoKategorisering.kurstype", "Du må velge en kurstype"))
            }

            if (tiltakskode == Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING) {
                val utdanninger = request.utdanningslop
                if (utdanninger == null) {
                    add(
                        FieldError.of(
                            AvtaleRequest::utdanningslop,
                            "Du må velge et utdanningsprogram og minst ett lærefag",
                        ),
                    )
                } else if (utdanninger.utdanninger.isEmpty()) {
                    add(FieldError.of(AvtaleRequest::utdanningslop, "Du må velge minst ett lærefag"))
                }
            }

            validateNavEnheter(request.navEnheter)
            validateAdministratorer(request)

            if (previous == null) {
                validateCreateAvtale(arrangor)
            } else {
                validateUpdateAvtale(request, arrangor, previous)
            }

            validatePrismodell(request.prismodell, tiltakskode, tiltakstype.navn)
                .onLeft { addAll(it) }
        }

        return errors.takeIf { it.isNotEmpty() }?.left()
            ?: AvtaleDboMapper
                .fromAvtaleRequest(request, arrangor, resolveStatus(request, previous, LocalDate.now()), tiltakstype.id)
                .copy(navEnheter = sanitizeNavEnheter(request.navEnheter)).right()
    }

    private fun MutableList<FieldError>.validateCreateAvtale(
        arrangor: AvtaleDbo.Arrangor?,
    ) = db.session {
        if (arrangor?.hovedenhet != null) {
            val hovedenhet = queries.arrangor.getById(arrangor.hovedenhet)

            if (hovedenhet.slettetDato != null) {
                add(
                    FieldError.ofPointer(
                        "/arrangorHovedenhet",
                        "Arrangøren ${hovedenhet.navn} er slettet i Brønnøysundregistrene. Avtaler kan ikke opprettes for slettede bedrifter.",
                    ),
                )
            }

            arrangor.underenheter.forEach { underenhetId ->
                val underenhet = queries.arrangor.getById(underenhetId)

                if (underenhet.slettetDato != null) {
                    add(
                        FieldError.ofPointer(
                            "/arrangorUnderenheter",
                            "Arrangøren ${underenhet.navn} er slettet i Brønnøysundregistrene. Avtaler kan ikke opprettes for slettede bedrifter.",
                        ),
                    )
                }

                if (underenhet.overordnetEnhet != hovedenhet.organisasjonsnummer) {
                    add(
                        FieldError.ofPointer(
                            "/arrangorUnderenheter",
                            "Arrangøren ${underenhet.navn} er ikke en gyldig underenhet til hovedenheten ${hovedenhet.navn}.",
                        ),
                    )
                }
            }
        }
    }

    private fun MutableList<FieldError>.validateUpdateAvtale(
        request: AvtaleRequest,
        arrangor: AvtaleDbo.Arrangor?,
        previous: AvtaleDto,
    ) = db.session {
        if (previous.opsjonerRegistrert.isNotEmpty()) {
            if (request.avtaletype != previous.avtaletype) {
                add(
                    FieldError.of(
                        AvtaleRequest::avtaletype,
                        "Du kan ikke endre avtaletype når opsjoner er registrert",
                    ),
                )
            }

            if (request.opsjonsmodell.type != previous.opsjonsmodell.type) {
                add(
                    FieldError.of(
                        AvtaleRequest::opsjonsmodell,
                        "Du kan ikke endre opsjonsmodell når opsjoner er registrert",
                    ),
                )
            }
        }
        if (request.prismodell.type !in Prismodeller.getPrismodellerForTiltak(request.tiltakskode)) {
            add(
                FieldError.of(
                    AvtaleRequest::tiltakskode,
                    "Tiltakstype kan ikke endres ikke fordi prismodellen “${request.prismodell.type.beskrivelse}” er i bruk",
                ),
            )
        }

        val (numGjennomforinger, gjennomforinger) = queries.gjennomforing.getAll(avtaleId = request.id)

        /**
         * Når avtalen har blitt godkjent så skal alle datafelter som påvirker økonomien, påmelding, osv. være låst.
         *
         * Vi mangler fortsatt en del innsikt og løsning rundt tilsagn og utbetaling (f.eks. når blir avtalen godkjent?),
         * så reglene for når en avtale er låst er foreløpig ganske naive og baserer seg kun på om det finnes
         * gjennomføringer på avtalen eller ikke...
         */
        if (numGjennomforinger > 0) {
            if (request.tiltakskode != previous.tiltakstype.tiltakskode) {
                add(
                    FieldError.of(
                        "Tiltakstype kan ikke endres fordi det finnes gjennomføringer for avtalen",
                        AvtaleRequest::tiltakskode,
                    ),
                )
            }

            val earliestGjennomforingStartDato = gjennomforinger.minBy { it.startDato }.startDato
            if (earliestGjennomforingStartDato.isBefore(request.startDato)) {
                add(
                    FieldError.of(
                        AvtaleRequest::startDato,
                        "Startdato kan ikke være etter startdatoen til gjennomføringer koblet til avtalen. Minst en gjennomføring har startdato: ${earliestGjennomforingStartDato.formaterDatoTilEuropeiskDatoformat()}",
                    ),
                )
            }

            gjennomforinger.forEach { gjennomforing ->
                val arrangorId = gjennomforing.arrangor.id

                if (request.arrangor == null) {
                    add(
                        FieldError.of(
                            "Arrangør kan ikke fjernes fordi en gjennomføring er koblet til avtalen",
                            AvtaleRequest::arrangor,
                            AvtaleRequest.Arrangor::hovedenhet,
                        ),
                    )
                } else if (arrangor?.underenheter?.contains(arrangorId) != true) {
                    add(
                        FieldError.ofPointer(
                            "/arrangorUnderenheter",
                            "Arrangøren ${gjennomforing.arrangor.navn} er i bruk på en av avtalens gjennomføringer, men mangler blant tiltaksarrangørens underenheter",
                        ),
                    )
                }

                gjennomforing.utdanningslop?.also {
                    if (request.utdanningslop?.utdanningsprogram != it.utdanningsprogram.id) {
                        add(
                            FieldError.of(
                                AvtaleRequest::utdanningslop,
                                "Utdanningsprogram kan ikke endres fordi en gjennomføring allerede er opprettet for utdanningsprogrammet ${it.utdanningsprogram.navn}",
                            ),
                        )
                    }

                    it.utdanninger.forEach { utdanning ->
                        val utdanninger = request.utdanningslop?.utdanninger ?: listOf()
                        if (!utdanninger.contains(utdanning.id)) {
                            add(
                                FieldError.of(
                                    AvtaleRequest::utdanningslop,
                                    "Lærefaget ${utdanning.navn} mangler i avtalen, men er i bruk på en av avtalens gjennomføringer",
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    fun validatePrismodell(
        request: PrismodellRequest,
        tiltakskode: Tiltakskode,
        tiltakstypeNavn: String,
    ): Either<NonEmptyList<FieldError>, PrismodellRequest> {
        val errors: List<FieldError> = buildList {
            if (request.type !in Prismodeller.getPrismodellerForTiltak(tiltakskode)) {
                add(
                    FieldError.of(
                        AvtaleRequest::prismodell,
                        "${request.type.beskrivelse} er ikke tillatt for tiltakstype $tiltakstypeNavn",
                    ),
                )
            }
            when (request.type) {
                Prismodell.ANNEN_AVTALT_PRIS,
                Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
                -> Unit

                Prismodell.AVTALT_PRIS_PER_MANEDSVERK,
                Prismodell.AVTALT_PRIS_PER_UKESVERK,
                Prismodell.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
                -> validateSatser(request.satser)
            }
        }

        return errors.toNonEmptyListOrNull()?.left() ?: request.right()
    }

    private fun MutableList<FieldError>.validateSatser(satser: List<AvtaltSatsDto>) {
        if (satser.isEmpty()) {
            add(
                FieldError.of(
                    AvtaleRequest::prismodell,
                    "Minst én pris er påkrevd",
                ),
            )
        }
        satser.forEachIndexed { index, sats ->
            if (sats.pris <= 0) {
                add(FieldError.ofPointer("/satser/$index/pris", "Pris må være positiv"))
            }
        }
        for (i in satser.indices) {
            val a = satser[i]
            for (j in i + 1 until satser.size) {
                val b = satser[j]
                if (!a.gjelderFra.isBefore(b.gjelderFra)) {
                    add(FieldError.ofPointer("/satser/$j/gjelderFra", "Ny pris må gjelde etter forrige pris"))
                    continue
                }
            }
        }
    }

    private fun MutableList<FieldError>.validateAdministratorer(
        request: AvtaleRequest,
    ) {
        val slettedeNavIdenter = db.session {
            request.administratorer.mapNotNull { ident ->
                queries.ansatt.getByNavIdent(ident)?.takeIf { it.skalSlettesDato != null }?.navIdent?.value
            }
        }

        if (slettedeNavIdenter.isNotEmpty()) {
            add(
                FieldError.of(
                    AvtaleRequest::administratorer,
                    "Administratorene med Nav ident " + slettedeNavIdenter.joinToString(", ") + " er slettet og må fjernes",
                ),
            )
        }
    }

    private fun MutableList<FieldError>.validateNavEnheter(navEnheter: List<NavEnhetNummer>) {
        val actualNavEnheter = resolveNavEnheter(navEnheter)

        if (!actualNavEnheter.any { it.value.type == NavEnhetType.FYLKE }) {
            add(FieldError.ofPointer("/navRegioner", "Du må velge minst én Nav-region"))
        }

        if (!actualNavEnheter.any { it.value.type != NavEnhetType.FYLKE }) {
            add(FieldError.ofPointer("/navKontorer", "Du må velge minst én Nav-enhet"))
        }
    }

    private fun resolveNavEnheter(enhetsnummer: List<NavEnhetNummer>): Map<NavEnhetNummer, NavEnhetDto> {
        val navEnheter = enhetsnummer.mapNotNull { navEnheterService.hentEnhet(it) }
        return navEnheter
            .filter { it.type == NavEnhetType.FYLKE }
            .flatMap { listOf(it) + navEnheter.filter { enhet -> enhet.overordnetEnhet == it.enhetsnummer } }
            .associateBy { it.enhetsnummer }
    }

    fun sanitizeNavEnheter(navEnheter: List<NavEnhetNummer>): Set<NavEnhetNummer> {
        // Filtrer vekk underenheter uten fylke
        return NavEnhetHelpers.buildNavRegioner(
            navEnheter.mapNotNull { navEnheterService.hentEnhet(it) },
        )
            .flatMap { it.enheter.map { it.enhetsnummer } + it.enhetsnummer }
            .toSet()
    }

    private suspend fun syncArrangorerFromBrreg(
        orgnr: Organisasjonsnummer,
        underenheterOrgnummere: List<Organisasjonsnummer>,
    ): Either<List<FieldError>, Pair<ArrangorDto, List<ArrangorDto>>> = either {
        val arrangor = syncArrangorFromBrreg(orgnr).bind()
        val underenheter = underenheterOrgnummere.mapOrAccumulate({ e1, e2 -> e1 + e2 }) {
            syncArrangorFromBrreg(it).bind()
        }.bind()
        Pair(arrangor, underenheter)
    }

    private suspend fun syncArrangorFromBrreg(
        orgnr: Organisasjonsnummer,
    ): Either<List<FieldError>, ArrangorDto> = arrangorService
        .getArrangorOrSyncFromBrreg(orgnr)
        .mapLeft {
            FieldError.ofPointer(
                "/arrangorHovedenhet",
                "Tiltaksarrangøren finnes ikke i Brønnøysundregistrene",
            ).nel()
        }
}
