package no.nav.mulighetsrommet.api.avtale

import arrow.core.*
import arrow.core.raise.either
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.avtale.api.AvtaleRequest
import no.nav.mulighetsrommet.api.avtale.api.OpprettOpsjonLoggRequest
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.db.PrismodellDbo
import no.nav.mulighetsrommet.api.avtale.mapper.AvtaleDboMapper
import no.nav.mulighetsrommet.api.avtale.model.*
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.navenhet.*
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.model.*
import java.time.LocalDate
import java.util.UUID
import kotlin.reflect.KProperty1

object AvtaleValidator {
    private val opsjonsmodellerUtenValidering = listOf(
        OpsjonsmodellType.INGEN_OPSJONSMULIGHET,
        OpsjonsmodellType.VALGFRI_SLUTTDATO,
    )

    data class Ctx(
        val previous: Avtale?,
        val arrangor: ArrangorDto?,
        val administratorer: List<NavAnsatt>,
        val tiltakstype: Tiltakstype,
        val navEnheter: List<NavEnhetDto>,
    ) {
        data class Avtale(
            val status: AvtaleStatusType,
            val opphav: ArenaMigrering.Opphav,
            val opsjonerRegistrert: List<no.nav.mulighetsrommet.api.avtale.model.Avtale.OpsjonLoggDto>,
            val opsjonsmodell: Opsjonsmodell,
            val avtaletype: Avtaletype,
            val tiltakskode: Tiltakskode,
            val gjennomforinger: List<Gjennomforing>,
        )
        data class Gjennomforing(
            val arrangor: no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing.ArrangorUnderenhet,
            val startDato: LocalDate,
            val utdanningslop: UtdanningslopDto?,
        )
        data class Tiltakstype(
            val navn: String,
            val id: UUID,
        )
    }

    fun validate(
        request: AvtaleRequest,
        ctx: Ctx,
    ): Either<List<FieldError>, AvtaleDbo> = either {
        val errors = buildList {
            if (request.startDato == null) {
                add(FieldError.of("Du må legge inn startdato for avtalen", AvtaleRequest::navn))
            }

            if (request.navn.length < 5 && ctx.previous?.opphav != ArenaMigrering.Opphav.ARENA) {
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

            if (request.avtaletype !in Avtaletyper.getAvtaletyperForTiltak(request.tiltakskode)) {
                add(
                    FieldError.of(
                        "${request.avtaletype.beskrivelse} er ikke tillatt for tiltakstype ${ctx.tiltakstype.navn}",
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

            if (request.tiltakskode == Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING && request.amoKategorisering == null) {
                add(FieldError.ofPointer("/amoKategorisering.kurstype", "Du må velge en kurstype"))
            }

            if (request.tiltakskode == Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING) {
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

            validateNavEnheter(ctx.navEnheter).onLeft { addAll(it.toList()) }
            validateSlettetNavAnsatte(ctx.administratorer, AvtaleRequest::administratorer)

            if (ctx.previous == null) {
                validateCreateAvtale(ctx.arrangor)
            } else {
                validateUpdateAvtale(request, ctx.arrangor, ctx.previous)
            }
        }

        if (errors.isNotEmpty()) {
            return errors.left()
        }

        return validatePrismodell(request.prismodell, request.tiltakskode, ctx.tiltakstype.navn)
            .map {
                AvtaleDboMapper
                    .fromAvtaleRequest(
                        request,
                        request.startDato!!,
                        it,
                        arrangor = ctx.arrangor?.let {
                            AvtaleDbo.Arrangor(
                                hovedenhet = it.id,
                                underenheter = it.underenheter?.map { it.id } ?: emptyList(),
                                kontaktpersoner = request.arrangor?.kontaktpersoner ?: emptyList(),
                            )
                        },
                        resolveStatus(request, ctx.previous, LocalDate.now()),
                        ctx.tiltakstype.id,
                    )
            }
    }

    fun resolveStatus(
        request: AvtaleRequest,
        previous: Ctx.Avtale?,
        today: LocalDate,
    ): AvtaleStatusType = if (request.arrangor == null) {
        AvtaleStatusType.UTKAST
    } else if (previous?.status == AvtaleStatusType.AVBRUTT) {
        previous.status
    } else if (request.sluttDato == null || !request.sluttDato.isBefore(today)) {
        AvtaleStatusType.AKTIV
    } else {
        AvtaleStatusType.AVSLUTTET
    }

    private fun MutableList<FieldError>.validateCreateAvtale(
        arrangor: ArrangorDto?,
    ) {
        if (arrangor == null) {
            return
        }
        if (arrangor.slettetDato != null) {
            add(
                FieldError.ofPointer(
                    "/arrangorHovedenhet",
                    "Arrangøren ${arrangor.navn} er slettet i Brønnøysundregistrene. Avtaler kan ikke opprettes for slettede bedrifter.",
                ),
            )
        }

        arrangor.underenheter?.forEach { underenhet ->
            if (underenhet.slettetDato != null) {
                add(
                    FieldError.ofPointer(
                        "/arrangorUnderenheter",
                        "Arrangøren ${underenhet.navn} er slettet i Brønnøysundregistrene. Avtaler kan ikke opprettes for slettede bedrifter.",
                    ),
                )
            }

            if (underenhet.overordnetEnhet != arrangor.organisasjonsnummer) {
                add(
                    FieldError.ofPointer(
                        "/arrangorUnderenheter",
                        "Arrangøren ${underenhet.navn} er ikke en gyldig underenhet til hovedenheten ${arrangor.navn}.",
                    ),
                )
            }
        }
    }

    private fun MutableList<FieldError>.validateUpdateAvtale(
        request: AvtaleRequest,
        arrangor: ArrangorDto?,
        previous: Ctx.Avtale,
    ) {
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
                    "Tiltakstype kan ikke endres ikke fordi prismodellen “${request.prismodell.type.navn}” er i bruk",
                    AvtaleRequest::tiltakskode,
                ),
            )
        }

        /**
         * Når avtalen har blitt godkjent så skal alle datafelter som påvirker økonomien, påmelding, osv. være låst.
         *
         * Vi mangler fortsatt en del innsikt og løsning rundt tilsagn og utbetaling (f.eks. når blir avtalen godkjent?),
         * så reglene for når en avtale er låst er foreløpig ganske naive og baserer seg kun på om det finnes
         * gjennomføringer på avtalen eller ikke...
         */
        if (previous.gjennomforinger.isNotEmpty()) {
            if (request.tiltakskode != previous.tiltakskode) {
                add(
                    FieldError.of(
                        "Tiltakstype kan ikke endres fordi det finnes gjennomføringer for avtalen",
                        AvtaleRequest::tiltakskode,
                    ),
                )
            }

            val earliestGjennomforingStartDato = previous.gjennomforinger.minBy { it.startDato }.startDato
            if (earliestGjennomforingStartDato.isBefore(request.startDato)) {
                add(
                    FieldError.of(
                        "Startdato kan ikke være etter startdatoen til gjennomføringer koblet til avtalen. Minst en gjennomføring har startdato: ${earliestGjennomforingStartDato.formaterDatoTilEuropeiskDatoformat()}",
                        AvtaleRequest::startDato,
                    ),
                )
            }

            previous.gjennomforinger.forEach { gjennomforing ->
                val arrangorId = gjennomforing.arrangor.id

                if (request.arrangor == null) {
                    add(
                        FieldError.of(
                            "Arrangør kan ikke fjernes fordi en gjennomføring er koblet til avtalen",
                            AvtaleRequest::arrangor,
                            AvtaleRequest.Arrangor::hovedenhet,
                        ),
                    )
                } else if (arrangor?.underenheter?.map { it.id }?.contains(arrangorId) != true) {
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
                        "${request.type.navn} er ikke tillatt for tiltakstype $tiltakstypeNavn",
                        AvtaleRequest::prismodell,
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

    private fun MutableList<FieldError>.validateSlettetNavAnsatte(
        navAnsatte: List<NavAnsatt>,
        property: KProperty1<*, *>,
    ) {
        val slettedeNavIdenter = navAnsatte
            .filter { it.skalSlettesDato != null }

        if (slettedeNavIdenter.isNotEmpty()) {
            add(
                FieldError.of(
                    "Nav identer " + slettedeNavIdenter.joinToString(", ") { it.navIdent.value } + " er slettet og må fjernes",
                    property,
                ),
            )
        }
    }

    fun validateNavEnheter(navEnheter: List<NavEnhetDto>): Either<Nel<FieldError>, Set<NavEnhetNummer>> {
        val errors = buildList {
            if (!navEnheter.any { it.type == NavEnhetType.FYLKE }) {
                add(FieldError.ofPointer("/navRegioner", "Du må velge minst én Nav-region"))
            }

            if (!navEnheter.any { it.type != NavEnhetType.FYLKE }) {
                add(FieldError.ofPointer("/navKontorer", "Du må velge minst én Nav-enhet"))
            }
        }
        return errors.toNonEmptyListOrNull()?.left() ?: navEnheter.map { it.enhetsnummer }.toSet().right()
    }
}
