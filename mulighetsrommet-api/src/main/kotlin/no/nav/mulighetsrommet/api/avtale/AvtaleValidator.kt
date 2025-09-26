package no.nav.mulighetsrommet.api.avtale

import arrow.core.*
import arrow.core.raise.either
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.avtale.api.AvtaleArrangor
import no.nav.mulighetsrommet.api.avtale.api.AvtaleDetaljerRequest
import no.nav.mulighetsrommet.api.avtale.api.AvtaleRequest
import no.nav.mulighetsrommet.api.avtale.api.OpprettOpsjonLoggRequest
import no.nav.mulighetsrommet.api.avtale.db.ArrangorDbo
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.db.DetaljerDbo
import no.nav.mulighetsrommet.api.avtale.db.PrismodellDbo
import no.nav.mulighetsrommet.api.avtale.mapper.AvtaleDboMapper.fromAvtaleRequest
import no.nav.mulighetsrommet.api.avtale.mapper.AvtaleDboMapper.toDbo
import no.nav.mulighetsrommet.api.avtale.mapper.prismodell
import no.nav.mulighetsrommet.api.avtale.model.*
import no.nav.mulighetsrommet.api.navenhet.*
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeDto
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.Avtaletyper
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID
import kotlin.collections.flatMap

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

    suspend fun validateCreateAvtale(request: AvtaleRequest): Either<Nel<FieldError>, AvtaleDbo> = either {
        val tiltakstype = tiltakstyper.getByTiltakskode(request.detaljer.tiltakskode)
        val tiltakskode = tiltakstype.tiltakskode
            ?: return FieldError.of("Tiltakstypen mangler tiltakskode", AvtaleDetaljerRequest::tiltakskode).nel()
                .left()

        val detaljerDbo = validateDetaljer(
            request.detaljer,
            tiltakskode,
            tiltakstype,
            ArenaMigrering.Opphav.TILTAKSADMINISTRASJON,
        ).bind()
        val personvernDbo = request.personvern.toDbo()
        val prismodellDbo = validatePrismodell(request.prismodell, tiltakskode, tiltakstype.navn).bind()
        val navEnheter = validateNavEnheter(request.veilederinformasjon.navEnheter).bind()
        val veilederinfoDbo = request.veilederinformasjon.toDbo(navEnheter)

        fromAvtaleRequest(
            request.id,
            AvtaleStatusType.AKTIV,
            detaljerDbo,
            prismodellDbo,
            personvernDbo,
            veilederinfoDbo,
        )
    }

    suspend fun validateUpdateDetaljer(
        avtaleId: UUID,
        detaljer: AvtaleDetaljerRequest,
        currentAvtale: Avtale,
    ): Either<List<FieldError>, DetaljerDbo> = either {
        db.session {
            val tiltakstype = tiltakstyper.getByTiltakskode(detaljer.tiltakskode)
            val tiltakskode = tiltakstype.tiltakskode
                ?: return FieldError.of("Tiltakstypen mangler tiltakskode", AvtaleDetaljerRequest::tiltakskode).nel()
                    .left()
            val detaljerDbo = validateDetaljer(detaljer, tiltakskode, tiltakstype, currentAvtale.opphav).bind()

            val errors = buildList {
                if (currentAvtale.opsjonerRegistrert.isNotEmpty()) {
                    if (detaljer.avtaletype != currentAvtale.avtaletype) {
                        add(
                            FieldError.of(
                                "Du kan ikke endre avtaletype når opsjoner er registrert",
                                AvtaleDetaljerRequest::avtaletype,
                            ),
                        )
                    }

                    if (detaljer.opsjonsmodell.type != currentAvtale.opsjonsmodell.type) {
                        add(
                            FieldError.of(
                                "Du kan ikke endre opsjonsmodell når opsjoner er registrert",
                                AvtaleDetaljerRequest::opsjonsmodell,
                            ),
                        )
                    }
                }
                if (currentAvtale.prismodell.prismodell() !in Prismodeller.getPrismodellerForTiltak(detaljer.tiltakskode)) {
                    add(
                        FieldError.of(
                            "Tiltakstype kan ikke endres fordi prismodellen “${currentAvtale.prismodell.prismodell().beskrivelse}” er i bruk",
                            AvtaleDetaljerRequest::tiltakskode,
                        ),
                    )
                }

                val (numGjennomforinger, gjennomforinger) = queries.gjennomforing.getAll(avtaleId = avtaleId)

                /**
                 * Når avtalen har blitt godkjent så skal alle datafelter som påvirker økonomien, påmelding, osv. være låst.
                 *
                 * Vi mangler fortsatt en del innsikt og løsning rundt tilsagn og utbetaling (f.eks. når blir avtalen godkjent?),
                 * så reglene for når en avtale er låst er foreløpig ganske naive og baserer seg kun på om det finnes
                 * gjennomføringer på avtalen eller ikke...
                 */
                if (numGjennomforinger > 0) {
                    if (detaljer.tiltakskode != currentAvtale.tiltakstype.tiltakskode) {
                        add(
                            FieldError.of(
                                "Tiltakstype kan ikke endres fordi det finnes gjennomføringer for avtalen",
                                AvtaleDetaljerRequest::tiltakskode,
                            ),
                        )
                    }

                    val earliestGjennomforingStartDato = gjennomforinger.minBy { it.startDato }.startDato
                    if (earliestGjennomforingStartDato.isBefore(detaljer.startDato)) {
                        add(
                            FieldError.of(
                                "Startdato kan ikke være etter startdatoen til gjennomføringer koblet til avtalen. Minst en gjennomføring har startdato: ${earliestGjennomforingStartDato.formaterDatoTilEuropeiskDatoformat()}",
                                AvtaleDetaljerRequest::tiltakskode,
                            ),
                        )
                    }

                    gjennomforinger.forEach { gjennomforing ->
                        val arrangorId = gjennomforing.arrangor.id

                        if (detaljerDbo.arrangor == null) {
                            add(
                                FieldError.of(
                                    "Arrangør kan ikke fjernes fordi en gjennomføring er koblet til avtalen",
                                    DetaljerDbo::arrangor,
                                    ArrangorDbo::hovedenhet,
                                ),
                            )
                        } else if (detaljerDbo.arrangor?.underenheter?.contains(arrangorId) != true) {
                            add(
                                FieldError.ofPointer(
                                    "/arrangorUnderenheter",
                                    "Arrangøren ${gjennomforing.arrangor.navn} er i bruk på en av avtalens gjennomføringer, men mangler blant tiltaksarrangørens underenheter",
                                ),
                            )
                        }

                        gjennomforing.utdanningslop?.also {
                            if (detaljer.utdanningslop?.utdanningsprogram != it.utdanningsprogram.id) {
                                add(
                                    FieldError.of(
                                        "Utdanningsprogram kan ikke endres fordi en gjennomføring allerede er opprettet for utdanningsprogrammet ${it.utdanningsprogram.navn}",
                                        AvtaleDetaljerRequest::utdanningslop,
                                    ),
                                )
                            }

                            it.utdanninger.forEach { utdanning ->
                                val utdanninger = detaljer.utdanningslop?.utdanninger ?: listOf()
                                if (!utdanninger.contains(utdanning.id)) {
                                    add(
                                        FieldError.of(
                                            "Lærefaget ${utdanning.navn} mangler i avtalen, men er i bruk på en av avtalens gjennomføringer",
                                            AvtaleDetaljerRequest::utdanningslop,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            return errors.toNonEmptyListOrNull()?.left() ?: detaljerDbo.right()
        }
    }

    private suspend fun validateDetaljer(
        detaljer: AvtaleDetaljerRequest,
        tiltakskode: Tiltakskode,
        tiltakstype: TiltakstypeDto,
        opphav: ArenaMigrering.Opphav?,
    ): Either<Nel<FieldError>, DetaljerDbo> = either {
        db.session {
            val arrangor = detaljer.arrangor?.let { validateArrangor(it).bind() }

            val errors = buildList {
                if (detaljer.navn.length < 5 && opphav != ArenaMigrering.Opphav.ARENA) {
                    add(FieldError.of("Avtalenavn må være minst 5 tegn langt", DetaljerDbo::navn))
                }
                if (detaljer.sluttDato != null && detaljer.sluttDato.isBefore(detaljer.startDato)) {
                    add(FieldError.of("Startdato må være før sluttdato", DetaljerDbo::startDato))
                }
                if (detaljer.avtaletype.kreverSakarkivNummer() && detaljer.sakarkivNummer == null) {
                    add(FieldError.of("Du må skrive inn saksnummer til avtalesaken", DetaljerDbo::sakarkivnummer))
                }
                if (detaljer.avtaletype !in Avtaletyper.getAvtaletyperForTiltak(tiltakskode)) {
                    add(
                        FieldError.of(
                            "${detaljer.avtaletype.beskrivelse} er ikke tillatt for tiltakstype ${tiltakstype.navn}",
                            DetaljerDbo::avtaletype,

                        ),
                    )
                }
                if (detaljer.avtaletype == Avtaletype.FORHANDSGODKJENT) {
                    if (detaljer.opsjonsmodell.type != OpsjonsmodellType.VALGFRI_SLUTTDATO) {
                        add(
                            FieldError.of(
                                "Du må velge opsjonsmodell med valgfri sluttdato når avtalen er forhåndsgodkjent",
                                DetaljerDbo::opsjonsmodell,
                            ),
                        )
                    }
                } else {
                    if (detaljer.opsjonsmodell.type != OpsjonsmodellType.VALGFRI_SLUTTDATO && detaljer.sluttDato == null) {
                        add(FieldError.of("Du må legge inn sluttdato for avtalen", DetaljerDbo::sluttDato))
                    }

                    if (detaljer.opsjonsmodell.type !in opsjonsmodellerUtenValidering) {
                        if (detaljer.opsjonsmodell.maksVarighet == null) {
                            add(
                                FieldError.of(
                                    "Du må legge inn maks varighet for opsjonen",
                                    DetaljerDbo::opsjonsmodell,
                                    Opsjonsmodell::maksVarighet,
                                ),
                            )
                        }

                        if (detaljer.opsjonsmodell.type == OpsjonsmodellType.ANNET) {
                            if (detaljer.opsjonsmodell.customNavn.isNullOrBlank()) {
                                add(
                                    FieldError.of(
                                        "Du må beskrive opsjonsmodellen",
                                        DetaljerDbo::opsjonsmodell,
                                        Opsjonsmodell::customNavn,
                                    ),
                                )
                            }
                        }
                    }
                }

                if (tiltakskode == Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING && detaljer.amoKategorisering == null) {
                    add(FieldError.ofPointer("/amoKategorisering.kurstype", "Du må velge en kurstype"))
                }

                if (tiltakskode == Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING) {
                    val utdanninger = detaljer.utdanningslop
                    if (utdanninger == null) {
                        add(
                            FieldError.of(
                                "Du må velge et utdanningsprogram og minst ett lærefag",
                                DetaljerDbo::utdanningslop,

                            ),
                        )
                    } else if (utdanninger.utdanninger.isEmpty()) {
                        add(FieldError.of("Du må velge minst ett lærefag", DetaljerDbo::utdanningslop))
                    }
                }
            }
            validateAdministratorer(detaljer.administratorer)

            return errors.toNonEmptyListOrNull()?.left() ?: detaljer.toDbo(arrangor, tiltakstype.id).right()
        }
    }

    suspend fun validateArrangor(arrangor: AvtaleArrangor): Either<Nel<FieldError>, ArrangorDbo> = either {
        db.session {
            val arrangor = arrangor.let {
                val (arrangor, underenheter) = syncArrangorerFromBrreg(
                    it.hovedenhet,
                    it.underenheter,
                ).bind()
                ArrangorDbo(
                    hovedenhet = arrangor.id,
                    underenheter = underenheter.map { underenhet -> underenhet.id },
                    kontaktpersoner = it.kontaktpersoner,
                )
            }
            val errors = buildList {
                if (arrangor.underenheter.isEmpty()) {
                    add(
                        FieldError.ofPointer(
                            "/arrangorUnderenheter",
                            "Du må velge minst én underenhet for tiltaksarrangør",
                        ),
                    )
                }

                val hovedenhet = queries.arrangor.getById(arrangor.hovedenhet)

                if (hovedenhet.slettetDato != null) {
                    add(
                        FieldError.of(
                            "Arrangøren ${hovedenhet.navn} er slettet i Brønnøysundregistrene. Avtaler kan ikke opprettes for slettede bedrifter.",
                            DetaljerDbo::arrangor,
                            ArrangorDbo::hovedenhet,
                        ),
                    )
                }

                arrangor.underenheter.forEach { underenhet ->
                    val underenhet = queries.arrangor.getById(underenhet)

                    if (underenhet.slettetDato != null) {
                        add(
                            FieldError.of(
                                "Arrangøren ${underenhet.navn} er slettet i Brønnøysundregistrene. Avtaler kan ikke opprettes for slettede bedrifter.",
                                DetaljerDbo::arrangor,
                                ArrangorDbo::underenheter,
                            ),
                        )
                    }

                    if (underenhet.overordnetEnhet != hovedenhet.organisasjonsnummer) {
                        add(
                            FieldError.of(
                                "Arrangøren ${underenhet.navn} er ikke en gyldig underenhet til hovedenheten ${hovedenhet.navn}.",
                                DetaljerDbo::arrangor,
                                ArrangorDbo::underenheter,
                            ),
                        )
                    }
                }
            }
            return errors.toNonEmptyListOrNull()?.left() ?: arrangor.right()
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
                        PrismodellRequest::type,
                    ),
                )
            }
            when (request.type) {
                PrismodellType.ANNEN_AVTALT_PRIS,
                PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
                -> Unit

                PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
                PrismodellType.AVTALT_PRIS_PER_UKESVERK,
                PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK,
                PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
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

            val maksVarighet = avtale.opsjonsmodell.maksVarighet
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

    private fun validateSatser(satser: List<AvtaltSatsRequest>): Nel<FieldError>? {
        val errors = buildList {
            if (satser.isEmpty()) {
                add(
                    FieldError.of(
                        "Minst én pris er påkrevd",
                        PrismodellRequest::type,
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
        return errors.toNonEmptyListOrNull()
    }

    private fun validateAdministratorer(
        administratorer: List<NavIdent>,
    ): Nel<FieldError>? {
        val errors = buildList {
            if (administratorer.isEmpty()) {
                add(FieldError.of("Du må velge minst én administrator", DetaljerDbo::administratorer))
            }
            val slettedeNavIdenter = db.session {
                administratorer.mapNotNull { ident ->
                    queries.ansatt.getByNavIdent(ident)?.takeIf { it.skalSlettesDato != null }?.navIdent?.value
                }
            }

            if (slettedeNavIdenter.isNotEmpty()) {
                add(
                    FieldError.of(
                        "Administratorene med Nav ident " + slettedeNavIdenter.joinToString(", ") + " er slettet og må fjernes",
                        DetaljerDbo::administratorer,
                        AvtaleDetaljerRequest::administratorer,
                    ),
                )
            }
        }
        return errors.toNonEmptyListOrNull()
    }

    private fun validateNavEnheter(navEnheter: List<NavEnhetNummer>): Either<Nel<FieldError>, Set<NavEnhetNummer>> {
        val actualNavEnheter = resolveNavEnheter(navEnheter)

        val errors = buildList {
            if (!actualNavEnheter.any { it.value.type == NavEnhetType.FYLKE }) {
                add(FieldError.ofPointer("/navRegioner", "Du må velge minst én Nav-region"))
            }

            if (!actualNavEnheter.any { it.value.type != NavEnhetType.FYLKE }) {
                add(FieldError.ofPointer("/navKontorer", "Du må velge minst én Nav-enhet"))
            }
        }

        return errors.toNonEmptyListOrNull()?.left() ?: sanitizeNavEnheter(navEnheter).right()
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
    ): Either<Nel<FieldError>, Pair<ArrangorDto, List<ArrangorDto>>> = either {
        val arrangor = syncArrangorFromBrreg(orgnr).bind()
        val underenheter = underenheterOrgnummere.mapOrAccumulate({ e1, e2 -> e1 + e2 }) {
            syncArrangorFromBrreg(it).bind()
        }.bind()
        Pair(arrangor, underenheter)
    }

    private suspend fun syncArrangorFromBrreg(
        orgnr: Organisasjonsnummer,
    ): Either<Nel<FieldError>, ArrangorDto> = arrangorService
        .getArrangorOrSyncFromBrreg(orgnr)
        .mapLeft {
            FieldError.ofPointer(
                "/arrangorHovedenhet",
                "Tiltaksarrangøren finnes ikke i Brønnøysundregistrene",
            ).nel()
        }
}
