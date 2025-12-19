package no.nav.mulighetsrommet.api.avtale

import arrow.core.Either
import arrow.core.right
import no.nav.mulighetsrommet.api.amo.AmoKategoriseringRequest
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.avtale.api.AvtaleRequest
import no.nav.mulighetsrommet.api.avtale.api.DetaljerRequest
import no.nav.mulighetsrommet.api.avtale.api.OpprettOpsjonLoggRequest
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.db.DetaljerDbo
import no.nav.mulighetsrommet.api.avtale.db.PrismodellDbo
import no.nav.mulighetsrommet.api.avtale.mapper.AvtaleDboMapper.fromValidatedAvtaleRequest
import no.nav.mulighetsrommet.api.avtale.mapper.toDbo
import no.nav.mulighetsrommet.api.avtale.model.Avtale
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSatsRequest
import no.nav.mulighetsrommet.api.avtale.model.OpsjonLoggDbo
import no.nav.mulighetsrommet.api.avtale.model.OpsjonLoggStatus
import no.nav.mulighetsrommet.api.avtale.model.Opsjonsmodell
import no.nav.mulighetsrommet.api.avtale.model.OpsjonsmodellType
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.avtale.model.PrismodellRequest
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.avtale.model.Prismodeller
import no.nav.mulighetsrommet.api.avtale.model.UtdanningslopDto
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.navenhet.NavEnhetDto
import no.nav.mulighetsrommet.api.navenhet.NavEnhetType
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.api.validation.ValidationDsl
import no.nav.mulighetsrommet.api.validation.validation
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.AmoKurstype
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.Avtaletyper
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import java.time.LocalDate
import java.util.UUID
import kotlin.contracts.ExperimentalContracts
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
            val prismodell: Prismodell,
        )

        data class Gjennomforing(
            val arrangor: no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing.ArrangorUnderenhet,
            val startDato: LocalDate,
            val utdanningslop: UtdanningslopDto?,
            val status: GjennomforingStatusType,
        )

        data class Tiltakstype(
            val navn: String,
            val id: UUID,
        )
    }

    fun validateCreateAvtale(
        request: AvtaleRequest,
        ctx: Ctx,
    ): Either<List<FieldError>, AvtaleDbo> = validation {
        validateNavEnheter(ctx.navEnheter)
        val amoKategorisering = validateDetaljer(request.detaljer, ctx).bind()
        val prismodellDbo = validatePrismodell(
            request.prismodell,
            tiltakskode = request.detaljer.tiltakskode,
            tiltakstypeNavn = ctx.tiltakstype.navn,
        ).bind()
        val detaljerDbo = request.detaljer.toDbo(
            ctx.tiltakstype.id,
            ctx.arrangor?.toDbo(request.detaljer.arrangor?.kontaktpersoner),
            resolveStatus(
                request.detaljer,
                ctx.previous,
                LocalDate.now(),
            ),
            amoKategorisering,
        )
        val personvernDbo = request.personvern.toDbo()
        val veilederinformasjonDbo = request.veilederinformasjon.toDbo()
        fromValidatedAvtaleRequest(request.id, detaljerDbo, prismodellDbo, personvernDbo, veilederinformasjonDbo)
    }

    /**
     * Når avtalen har blitt godkjent så skal alle datafelter som påvirker økonomien, påmelding, osv. være låst.
     *
     * Vi mangler fortsatt en del innsikt og løsning rundt tilsagn og utbetaling (f.eks. når blir avtalen godkjent?),
     * så reglene for når en avtale er låst er foreløpig ganske naive og baserer seg kun på om det finnes
     * gjennomføringer på avtalen eller ikke...
     */
    fun validateUpdateDetaljer(
        request: DetaljerRequest,
        ctx: Ctx,
    ): Either<List<FieldError>, DetaljerDbo> = validation {
        val previous = ctx.previous
        requireNotNull(previous) {
            "Avtalen finnes ikke"
        }

        if (previous.opsjonerRegistrert.isNotEmpty()) {
            validate(request.avtaletype == previous.avtaletype) {
                FieldError.of(
                    "Du kan ikke endre avtaletype når opsjoner er registrert",
                    DetaljerRequest::avtaletype,
                )
            }
            validate(request.opsjonsmodell.type == previous.opsjonsmodell.type) {
                FieldError.of(
                    "Du kan ikke endre opsjonsmodell når opsjoner er registrert",
                    DetaljerRequest::opsjonsmodell,
                )
            }
        }
        validate(previous.prismodell.type in Prismodeller.getPrismodellerForTiltak(request.tiltakskode)) {
            FieldError.of(
                "Tiltakstype kan ikke endres fordi prismodellen “${previous.prismodell.type.navn}” er i bruk",
                DetaljerRequest::tiltakskode,
            )
        }

        if (previous.gjennomforinger.isNotEmpty()) {
            validate(request.tiltakskode == previous.tiltakskode) {
                FieldError.of(
                    "Tiltakstype kan ikke endres fordi det finnes gjennomføringer for avtalen",
                    DetaljerRequest::tiltakskode,
                )
            }

            val earliestGjennomforingStartDato = previous.gjennomforinger.minBy { it.startDato }.startDato
            validate(!earliestGjennomforingStartDato.isBefore(request.startDato)) {
                FieldError.of(
                    "Startdato kan ikke være etter startdatoen til gjennomføringer koblet til avtalen. Minst en gjennomføring har startdato: ${earliestGjennomforingStartDato.formaterDatoTilEuropeiskDatoformat()}",
                    DetaljerRequest::startDato,
                )
            }

            previous.gjennomforinger.forEach { gjennomforing ->
                val arrangorId = gjennomforing.arrangor.id

                validateNotNull(request.arrangor) {
                    FieldError.of(
                        "Arrangør kan ikke fjernes fordi en gjennomføring er koblet til avtalen",
                        DetaljerRequest::arrangor,
                        DetaljerRequest.Arrangor::hovedenhet,
                    )
                }
                validate(
                    gjennomforing.status != GjennomforingStatusType.GJENNOMFORES || ctx.arrangor?.underenheter?.map { it.id }
                        ?.contains(arrangorId) == true,
                ) {
                    FieldError.ofPointer(
                        "/arrangorUnderenheter",
                        "Arrangøren ${gjennomforing.arrangor.navn} er i bruk på en av avtalens gjennomføringer, men mangler blant tiltaksarrangørens underenheter",
                    )
                }

                gjennomforing.utdanningslop?.also {
                    validate(request.utdanningslop?.utdanningsprogram == it.utdanningsprogram.id) {
                        FieldError.of(
                            "Utdanningsprogram kan ikke endres fordi en gjennomføring allerede er opprettet for utdanningsprogrammet ${it.utdanningsprogram.navn}",
                            DetaljerRequest::utdanningslop,
                        )
                    }

                    it.utdanninger.forEach { utdanning ->
                        val utdanninger = request.utdanningslop?.utdanninger ?: listOf()
                        validate(utdanninger.contains(utdanning.id)) {
                            FieldError.of(
                                "Lærefaget ${utdanning.navn} mangler i avtalen, men er i bruk på en av avtalens gjennomføringer",
                                DetaljerRequest::utdanningslop,
                            )
                        }
                    }
                }
            }
        }
        val amoKategorisering = validateDetaljer(request, ctx).bind()

        request.toDbo(
            ctx.tiltakstype.id,
            ctx.arrangor?.toDbo(request.arrangor?.kontaktpersoner),
            resolveStatus(request, previous, LocalDate.now()),
            amoKategorisering = amoKategorisering,
        )
    }

    fun ValidationDsl.validateDetaljer(
        request: DetaljerRequest,
        ctx: Ctx,
    ): Either<List<FieldError>, AmoKategorisering?> {
        validateNotNull(request.startDato) {
            FieldError.of("Du må legge inn startdato for avtalen", DetaljerRequest::navn)
        }
        validate(request.navn.length >= 5 || ctx.previous?.opphav == ArenaMigrering.Opphav.ARENA) {
            FieldError.of("Avtalenavn må være minst 5 tegn langt", DetaljerRequest::navn)
        }
        validate(request.administratorer.isNotEmpty()) {
            FieldError.of("Du må velge minst én administrator", DetaljerRequest::administratorer)
        }
        validate(request.sluttDato == null || !request.sluttDato.isBefore(request.startDato)) {
            FieldError.of("Startdato må være før sluttdato", DetaljerRequest::startDato)
        }
        validate(request.arrangor == null || request.arrangor.underenheter.isNotEmpty()) {
            FieldError.ofPointer(
                "/arrangorUnderenheter",
                "Du må velge minst én underenhet for tiltaksarrangør",
            )
        }
        validate(!request.avtaletype.kreverSakarkivNummer() || request.sakarkivNummer != null) {
            FieldError.of("Du må skrive inn saksnummer til avtalesaken", DetaljerRequest::sakarkivNummer)
        }
        validate(request.avtaletype in Avtaletyper.getAvtaletyperForTiltak(request.tiltakskode)) {
            FieldError.of(
                "${request.avtaletype.beskrivelse} er ikke tillatt for tiltakstype ${ctx.tiltakstype.navn}",
                DetaljerRequest::avtaletype,
            )
        }
        if (request.avtaletype == Avtaletype.FORHANDSGODKJENT) {
            validate(request.opsjonsmodell.type == OpsjonsmodellType.VALGFRI_SLUTTDATO) {
                FieldError.of(
                    "Du må velge opsjonsmodell med valgfri sluttdato når avtalen er forhåndsgodkjent",
                    DetaljerRequest::opsjonsmodell,
                )
            }
        } else {
            validate(request.opsjonsmodell.type == OpsjonsmodellType.VALGFRI_SLUTTDATO || request.sluttDato != null) {
                FieldError.of("Du må legge inn sluttdato for avtalen", DetaljerRequest::sluttDato)
            }
            if (request.opsjonsmodell.type !in opsjonsmodellerUtenValidering) {
                validate(request.opsjonsmodell.opsjonMaksVarighet != null) {
                    FieldError.of(
                        "Du må legge inn maks varighet for opsjonen",
                        DetaljerRequest::opsjonsmodell,
                        Opsjonsmodell::opsjonMaksVarighet,
                    )
                }
                if (request.opsjonsmodell.type == OpsjonsmodellType.ANNET) {
                    validate(!request.opsjonsmodell.customOpsjonsmodellNavn.isNullOrBlank()) {
                        FieldError.of(
                            "Du må beskrive opsjonsmodellen",
                            DetaljerRequest::opsjonsmodell,
                            Opsjonsmodell::customOpsjonsmodellNavn,
                        )
                    }
                }
            }
        }
        val amoKategorisering = validateAmoKategorisering(request.tiltakskode, request.amoKategorisering)
        validateUtdanningslop(request.tiltakskode, request.utdanningslop)

        validateSlettetNavAnsatte(ctx.administratorer, DetaljerRequest::administratorer)
        ctx.arrangor?.let { validateArrangor(it) }
        return amoKategorisering
    }

    fun resolveStatus(
        request: DetaljerRequest,
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

    private fun ValidationDsl.validateArrangor(
        arrangor: ArrangorDto,
    ) {
        validate(arrangor.slettetDato == null) {
            FieldError.ofPointer(
                "/arrangor/hovedenhet",
                "Arrangøren ${arrangor.navn} er slettet i Brønnøysundregistrene. Avtaler kan ikke opprettes for slettede bedrifter.",
            )
        }

        arrangor.underenheter?.forEach { underenhet ->
            validate(underenhet.slettetDato == null) {
                FieldError.ofPointer(
                    "/arrangor/underenheter",
                    "Arrangøren ${underenhet.navn} er slettet i Brønnøysundregistrene. Avtaler kan ikke opprettes for slettede bedrifter.",
                )
            }

            validate(underenhet.overordnetEnhet == arrangor.organisasjonsnummer) {
                FieldError.ofPointer(
                    "/arrangor/underenheter",
                    "Arrangøren ${underenhet.navn} - ${underenhet.organisasjonsnummer.value} er ikke en gyldig underenhet til hovedenheten ${arrangor.navn}.",
                )
            }
        }
    }

    fun validatePrismodell(
        request: PrismodellRequest,
        tiltakskode: Tiltakskode,
        tiltakstypeNavn: String,
    ): Either<List<FieldError>, PrismodellDbo> = validation {
        validatePrismodell(request, tiltakskode, tiltakstypeNavn).bind()
    }

    private fun ValidationDsl.validatePrismodell(
        request: PrismodellRequest,
        tiltakskode: Tiltakskode,
        tiltakstypeNavn: String,
    ): Either<List<FieldError>, PrismodellDbo> {
        validate(request.type in Prismodeller.getPrismodellerForTiltak(tiltakskode)) {
            FieldError.of(
                "${request.type.navn} er ikke tillatt for tiltakstype $tiltakstypeNavn",
                AvtaleRequest::prismodell,
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

        return PrismodellDbo(
            id = request.id,
            type = request.type,
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
    ): Either<List<FieldError>, OpsjonLoggDbo> = validation {
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

        validate(request.type != OpprettOpsjonLoggRequest.Type.CUSTOM_LENGDE || nySluttDato != null) {
            FieldError.of(
                "Ny sluttdato må være satt",
                OpprettOpsjonLoggRequest::nySluttDato,
            )
        }

        val maksVarighet = avtale.opsjonsmodell.opsjonMaksVarighet
        validate(!(nySluttDato != null && maksVarighet != null && nySluttDato.isAfter(maksVarighet))) {
            FieldError.of(
                "Ny sluttdato er forbi maks varighet av avtalen",
                OpprettOpsjonLoggRequest::nySluttDato,
            )
        }
        val skalIkkeUtloseOpsjonerForAvtale = avtale.opsjonerRegistrert.any {
            it.status === OpsjonLoggStatus.SKAL_IKKE_UTLOSE_OPSJON
        }
        validate(!skalIkkeUtloseOpsjonerForAvtale) {
            FieldError.of("Kan ikke utløse flere opsjoner", OpprettOpsjonLoggRequest::type)
        }

        OpsjonLoggDbo(
            avtaleId = avtale.id,
            sluttDato = nySluttDato,
            forrigeSluttDato = avtale.sluttDato,
            status = OpsjonLoggStatus.fromType(request.type),
            registrertAv = navIdent,
        )
    }

    private fun ValidationDsl.validateSatser(satser: List<AvtaltSatsRequest>) {
        validate(satser.isNotEmpty()) {
            FieldError.of(
                "Minst én pris er påkrevd",
                AvtaleRequest::prismodell,
            )
        }
        satser.forEachIndexed { index, sats ->
            validate(sats.pris != null && sats.pris > 0) {
                FieldError.ofPointer("prismodell/satser/$index/pris", "Pris må være positiv")
            }
        }
        for (i in satser.indices) {
            val a = satser[i]
            if (a.gjelderFra == null) {
                validate(false) {
                    FieldError.ofPointer("prismodell/satser/$i/gjelderFra", "Gjelder fra må være satt")
                }
                continue
            }
            for (j in i + 1 until satser.size) {
                val b = satser[j]
                if (!a.gjelderFra.isBefore(b.gjelderFra)) {
                    validate(false) {
                        FieldError.ofPointer("prismodell/satser/$j/gjelderFra", "Ny pris må gjelde etter forrige pris")
                    }
                    continue
                }
            }
        }
    }

    private fun ValidationDsl.validateSlettetNavAnsatte(
        navAnsatte: List<NavAnsatt>,
        property: KProperty1<*, *>,
    ) {
        val slettedeNavIdenter = navAnsatte
            .filter { it.skalSlettesDato != null }

        validate(!slettedeNavIdenter.isNotEmpty()) {
            FieldError.of(
                "Nav identer " + slettedeNavIdenter.joinToString(", ") { it.navIdent.value } + " er slettet og må fjernes",
                property,
            )
        }
    }

    fun validateNavEnheter(navEnheter: List<NavEnhetDto>): Either<List<FieldError>, Unit> = validation {
        validateNavEnheter(navEnheter)
    }

    private fun ValidationDsl.validateNavEnheter(navEnheter: List<NavEnhetDto>) {
        validate(navEnheter.any { it.type == NavEnhetType.FYLKE }) {
            FieldError.ofPointer("/navRegioner", "Du må velge minst én Nav-region")
        }
        validate(navEnheter.any { it.type != NavEnhetType.FYLKE }) {
            FieldError.ofPointer("/navKontorer", "Du må velge minst én Nav-enhet")
        }
    }

    @OptIn(ExperimentalContracts::class)
    private fun ValidationDsl.validateAmoKategorisering(
        tiltakskode: Tiltakskode,
        amoKategorisering: AmoKategoriseringRequest?,
    ): Either<List<FieldError>, AmoKategorisering?> = when (tiltakskode) {
        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        Tiltakskode.ARBEIDSRETTET_REHABILITERING,
        Tiltakskode.AVKLARING,
        Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
        Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING,
        Tiltakskode.HOYERE_UTDANNING,
        Tiltakskode.JOBBKLUBB,
        Tiltakskode.OPPFOLGING,
        Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
        Tiltakskode.HOYERE_YRKESFAGLIG_UTDANNING,
        Tiltakskode.FAG_OG_YRKESOPPLAERING,
        Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
        ->
            null

        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING -> {
            requireValid(amoKategorisering?.kurstype != null) {
                FieldError.of(
                    "Du må velge en kurstype",
                    DetaljerRequest::amoKategorisering,
                    AmoKategoriseringRequest::kurstype,
                )
            }
            if (amoKategorisering.kurstype == AmoKurstype.BRANSJE_OG_YRKESRETTET) {
                requireValid(amoKategorisering.bransje != null) {
                    FieldError.of(
                        "Du må velge en bransje",
                        DetaljerRequest::amoKategorisering,
                        AmoKategoriseringRequest::bransje,
                    )
                }
            }
            amoKategorisering
        }

        Tiltakskode.ARBEIDSMARKEDSOPPLAERING -> {
            requireValid(amoKategorisering?.bransje != null) {
                FieldError.of(
                    "Du må velge en bransje",
                    DetaljerRequest::amoKategorisering,
                    AmoKategoriseringRequest::bransje,
                )
            }
            amoKategorisering.copy(kurstype = AmoKurstype.BRANSJE_OG_YRKESRETTET)
        }

        Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV -> {
            requireValid(amoKategorisering?.kurstype != null) {
                FieldError.of(
                    "Du må velge en kurstype",
                    DetaljerRequest::amoKategorisering,
                    AmoKategoriseringRequest::kurstype,
                )
            }
            validate(
                amoKategorisering.kurstype in listOf(
                    AmoKurstype.FORBEREDENDE_OPPLAERING_FOR_VOKSNE,
                    AmoKurstype.NORSKOPPLAERING,
                    AmoKurstype.GRUNNLEGGENDE_FERDIGHETER,
                ),
            ) {
                FieldError.of(
                    "Ugyldig kurstype",
                    DetaljerRequest::amoKategorisering,
                    AmoKategoriseringRequest::kurstype,
                )
            }
            amoKategorisering
        }

        Tiltakskode.STUDIESPESIALISERING,
        -> AmoKategoriseringRequest(kurstype = AmoKurstype.STUDIESPESIALISERING)
    }?.toDbo().right()

    private fun ValidationDsl.validateUtdanningslop(
        tiltakskode: Tiltakskode,
        utdanningslop: UtdanningslopDbo?,
    ): Either<List<FieldError>, Unit> = when (tiltakskode) {
        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        Tiltakskode.ARBEIDSRETTET_REHABILITERING,
        Tiltakskode.AVKLARING,
        Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
        Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING,
        Tiltakskode.HOYERE_UTDANNING,
        Tiltakskode.JOBBKLUBB,
        Tiltakskode.OPPFOLGING,
        Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
        Tiltakskode.HOYERE_YRKESFAGLIG_UTDANNING,
        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
        Tiltakskode.STUDIESPESIALISERING,
        -> Unit

        Tiltakskode.FAG_OG_YRKESOPPLAERING,
        Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
        -> {
            validateNotNull(utdanningslop) {
                FieldError.of(
                    "Du må velge et utdanningsprogram og minst ett lærefag",
                    DetaljerRequest::utdanningslop,
                )
            }
            validate(utdanningslop == null || utdanningslop.utdanninger.isNotEmpty()) {
                FieldError.of("Du må velge minst ett lærefag", DetaljerRequest::utdanningslop)
            }
        }
    }.right()
}
