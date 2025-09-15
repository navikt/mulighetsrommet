package no.nav.mulighetsrommet.api.avtale

import arrow.core.*
import arrow.core.raise.either
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.avtale.api.AvtaleRequest
import no.nav.mulighetsrommet.api.avtale.api.OpprettOpsjonLoggRequest
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.db.PrismodellDbo
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
        previous: Avtale?,
    ): Either<List<FieldError>, AvtaleDbo> = either {
        val tiltakstype = tiltakstyper.getByTiltakskode(request.tiltakskode)
        val tiltakskode = tiltakstype.tiltakskode
            ?: return FieldError.of("Tiltakstypen mangler tiltalkskode", AvtaleRequest::tiltakskode).nel().left()

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
            if (request.startDato == null) {
                add(FieldError.of("Du må legge inn startdato for avtalen", AvtaleRequest::navn))
            }

            if (request.navn.length < 5 && previous?.opphav != ArenaMigrering.Opphav.ARENA) {
                add(FieldError.of("Avtalenavn må være minst 5 tegn langt", AvtaleRequest::navn))
            }

            if (request.administratorer.isEmpty()) {
                add(FieldError.of("Du må velge minst én administrator", AvtaleRequest::administratorer))
            }

            if (request.sluttDato != null && request.sluttDato.isBefore(request.startDato)) {
                add(FieldError.of("Startdato må være før sluttdato", AvtaleRequest::startDato))
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
                add(FieldError.of("Du må skrive inn saksnummer til avtalesaken", AvtaleRequest::sakarkivNummer))
            }

            if (request.avtaletype !in Avtaletyper.getAvtaletyperForTiltak(tiltakskode)) {
                add(
                    FieldError.of(
                        "${request.avtaletype.beskrivelse} er ikke tillatt for tiltakstype ${tiltakstype.navn}",
                        AvtaleRequest::avtaletype,
                    ),
                )
            }

            if (request.avtaletype == Avtaletype.FORHANDSGODKJENT) {
                if (request.opsjonsmodell.type != OpsjonsmodellType.VALGFRI_SLUTTDATO) {
                    add(
                        FieldError.of(
                            "Du må velge opsjonsmodell med valgfri sluttdato når avtalen er forhåndsgodkjent",
                            AvtaleRequest::opsjonsmodell,
                        ),
                    )
                }
            } else {
                if (request.opsjonsmodell.type != OpsjonsmodellType.VALGFRI_SLUTTDATO && request.sluttDato == null) {
                    add(FieldError.of("Du må legge inn sluttdato for avtalen", AvtaleRequest::sluttDato))
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
                            "Du må velge et utdanningsprogram og minst ett lærefag",
                            AvtaleRequest::utdanningslop,
                        ),
                    )
                } else if (utdanninger.utdanninger.isEmpty()) {
                    add(FieldError.of("Du må velge minst ett lærefag", AvtaleRequest::utdanningslop))
                }
            }

            validateNavEnheter(request.navEnheter)
            validateAdministratorer(request)

            if (previous == null) {
                validateCreateAvtale(arrangor)
            } else {
                validateUpdateAvtale(request, arrangor, previous)
            }
        }
        if (errors.isNotEmpty()) {
            return errors.left()
        }

        return validatePrismodell(request.prismodell, tiltakskode, tiltakstype.navn)
            .map {
                AvtaleDboMapper
                    .fromAvtaleRequest(request, request.startDato!!, it, arrangor, resolveStatus(request, previous, LocalDate.now()), tiltakstype.id)
                    .copy(navEnheter = sanitizeNavEnheter(request.navEnheter))
            }
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
        previous: Avtale,
    ) = db.session {
        if (previous.opsjonerRegistrert.isNotEmpty()) {
            if (request.avtaletype != previous.avtaletype) {
                add(
                    FieldError.of(
                        "Du kan ikke endre avtaletype når opsjoner er registrert",
                        AvtaleRequest::avtaletype,
                    ),
                )
            }

            if (request.opsjonsmodell.type != previous.opsjonsmodell.type) {
                add(
                    FieldError.of(
                        "Du kan ikke endre opsjonsmodell når opsjoner er registrert",
                        AvtaleRequest::opsjonsmodell,
                    ),
                )
            }
        }
        if (request.prismodell.type !in Prismodeller.getPrismodellerForTiltak(request.tiltakskode)) {
            add(
                FieldError.of(
                    "Tiltakstype kan ikke endres ikke fordi prismodellen “${request.prismodell.type.beskrivelse}” er i bruk",
                    AvtaleRequest::tiltakskode,
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
                        "Startdato kan ikke være etter startdatoen til gjennomføringer koblet til avtalen. Minst en gjennomføring har startdato: ${earliestGjennomforingStartDato.formaterDatoTilEuropeiskDatoformat()}",
                        AvtaleRequest::startDato,
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
                                "Utdanningsprogram kan ikke endres fordi en gjennomføring allerede er opprettet for utdanningsprogrammet ${it.utdanningsprogram.navn}",
                                AvtaleRequest::utdanningslop,
                            ),
                        )
                    }

                    it.utdanninger.forEach { utdanning ->
                        val utdanninger = request.utdanningslop?.utdanninger ?: listOf()
                        if (!utdanninger.contains(utdanning.id)) {
                            add(
                                FieldError.of(
                                    "Lærefaget ${utdanning.navn} mangler i avtalen, men er i bruk på en av avtalens gjennomføringer",
                                    AvtaleRequest::utdanningslop,
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
    ): Either<NonEmptyList<FieldError>, PrismodellDbo> {
        val errors: List<FieldError> = buildList {
            if (request.type !in Prismodeller.getPrismodellerForTiltak(tiltakskode)) {
                add(
                    FieldError.of(
                        "${request.type.beskrivelse} er ikke tillatt for tiltakstype $tiltakstypeNavn",
                        AvtaleRequest::prismodell,
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

        return errors.toNonEmptyListOrNull()?.left() ?: PrismodellDbo(
            prismodell = request.type,
            prisbetingelser = request.prisbetingelser,
            satser = request.satser.map {
                AvtaltSats(gjelderFra = it.gjelderFra!!, sats = it.pris!!)
            },
        ).right()
    }

    fun validateOpprettOpsjonLoggRequest(
        request: OpprettOpsjonLoggRequest,
        avtale: Avtale,
        navIdent: NavIdent,
    ): Either<NonEmptyList<FieldError>, OpsjonLoggDbo> {
        requireNotNull(avtale.sluttDato) {
            "avtalen mangler sluttdato"
        }
        val nySluttDato = when (request.type) {
            OpprettOpsjonLoggRequest.Type.CUSTOM_LENGDE -> {
                request.nySluttDato
            }
            OpprettOpsjonLoggRequest.Type.ETT_AAR -> {
                avtale.sluttDato.plusYears(1)
            }
            OpprettOpsjonLoggRequest.Type.SKAL_IKKE_UTLOSE_OPSJON -> {
                null
            }
        }

        val errors: List<FieldError> = buildList {
            if (request.type == OpprettOpsjonLoggRequest.Type.CUSTOM_LENGDE && nySluttDato == null) {
                add(
                    FieldError.of(
                        "Ny sluttdato må være satt",
                        OpprettOpsjonLoggRequest::nySluttDato,
                    ),
                )
            }

            val maksVarighet = avtale.opsjonsmodell.opsjonMaksVarighet
            if (nySluttDato != null && maksVarighet != null && nySluttDato.isAfter(maksVarighet)) {
                add(
                    FieldError.of(
                        "Ny sluttdato er forbi maks varighet av avtalen",
                        OpprettOpsjonLoggRequest::nySluttDato,
                    ),
                )
            }
            val skalIkkeUtloseOpsjonerForAvtale = avtale.opsjonerRegistrert.any {
                it.status === OpsjonLoggStatus.SKAL_IKKE_UTLOSE_OPSJON
            }
            if (skalIkkeUtloseOpsjonerForAvtale) {
                add(FieldError.of("Kan ikke utløse flere opsjoner", OpprettOpsjonLoggRequest::type))
            }
        }

        return errors.toNonEmptyListOrNull()?.left() ?: OpsjonLoggDbo(
            avtaleId = avtale.id,
            sluttDato = nySluttDato,
            forrigeSluttDato = avtale.sluttDato,
            status = OpsjonLoggStatus.fromType(request.type),
            registrertAv = navIdent,
        ).right()
    }

    private fun MutableList<FieldError>.validateSatser(satser: List<AvtaltSatsRequest>) {
        if (satser.isEmpty()) {
            add(
                FieldError.of(
                    "Minst én pris er påkrevd",
                    AvtaleRequest::prismodell,
                ),
            )
        }
        satser.forEachIndexed { index, sats ->
            if (sats.pris == null || sats.pris <= 0) {
                add(FieldError.ofPointer("/satser/$index/pris", "Pris må være positiv"))
            }
        }
        for (i in satser.indices) {
            val a = satser[i]
            if (a.gjelderFra == null) {
                add(FieldError.ofPointer("/satser/$i/gjelderFra", "Gjelder fra må være satt"))
                continue
            }
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
                    "Administratorene med Nav ident " + slettedeNavIdenter.joinToString(", ") + " er slettet og må fjernes",
                    AvtaleRequest::administratorer,
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
