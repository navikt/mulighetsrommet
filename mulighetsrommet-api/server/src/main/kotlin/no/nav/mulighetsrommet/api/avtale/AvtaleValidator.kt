package no.nav.mulighetsrommet.api.avtale

import arrow.core.Either
import arrow.core.right
import no.nav.mulighetsrommet.admin.navenhet.NavEnhetDto
import no.nav.mulighetsrommet.admin.opplaring.UtdanningslopDetaljer
import no.nav.mulighetsrommet.api.avtale.api.AmoKategoriseringRequest
import no.nav.mulighetsrommet.api.avtale.api.DetaljerRequest
import no.nav.mulighetsrommet.api.avtale.api.OpprettAvtaleRequest
import no.nav.mulighetsrommet.api.avtale.api.OpprettOpsjonLoggRequest
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSatsRequest
import no.nav.mulighetsrommet.api.avtale.model.OpsjonLoggDbo
import no.nav.mulighetsrommet.api.avtale.model.PrismodellRequest
import no.nav.mulighetsrommet.api.avtale.model.toOpsjonLoggStatus
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetType
import no.nav.mulighetsrommet.api.domain.opplaring.Bransje
import no.nav.mulighetsrommet.api.domain.opplaring.ForerkortKlasse
import no.nav.mulighetsrommet.api.domain.opplaring.InnholdElement
import no.nav.mulighetsrommet.api.domain.opplaring.Kurstype
import no.nav.mulighetsrommet.api.domain.opplaring.OpplaringKategorisering
import no.nav.mulighetsrommet.api.domain.opplaring.Utdanningslop
import no.nav.mulighetsrommet.api.domain.tiltak.Avtale
import no.nav.mulighetsrommet.api.domain.tiltak.AvtaltSats
import no.nav.mulighetsrommet.api.domain.tiltak.OpsjonLoggStatus
import no.nav.mulighetsrommet.api.domain.tiltak.Opsjonsmodell
import no.nav.mulighetsrommet.api.domain.tiltak.OpsjonsmodellType
import no.nav.mulighetsrommet.api.domain.tiltak.Prismodell
import no.nav.mulighetsrommet.api.domain.tiltak.PrismodellType
import no.nav.mulighetsrommet.api.domain.tiltak.Prismodeller
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing.ArrangorUnderenhet
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.Avtaletyper
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.SakarkivNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.validation.FieldValidator
import no.nav.mulighetsrommet.validation.validation
import java.time.LocalDate
import java.util.UUID
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
object AvtaleValidator {
    data class Ctx(
        val previous: Avtale?,
        val arrangor: AvtaleArrangor?,
        val administratorer: List<NavAnsatt>,
        val kategorisering: Kategorisering,
        val tiltakstype: Tiltakstype,
        val navEnheter: List<NavEnhetDto>,
    ) {
        data class Avtale(
            val status: AvtaleStatusType,
            val opsjoner: Avtale.Opsjoner,
            val avtaletype: Avtaletype,
            val tiltakskode: Tiltakskode,
            val gjennomforinger: List<Gjennomforing>,
        )

        data class AvtaleArrangor(
            val hovedenhet: Arrangor,
            val underenheter: List<Arrangor>,
        )

        data class Gjennomforing(
            val arrangor: ArrangorUnderenhet,
            val startDato: LocalDate,
            val utdanningslop: UtdanningslopDetaljer?,
            val status: GjennomforingStatusType,
            val prismodellId: UUID,
        )

        data class Kategorisering(
            val kurstyper: Set<Kurstype>,
            val bransjer: Set<Bransje>,
            val forerkort: Set<ForerkortKlasse>,
            val innholdElementer: Set<InnholdElement>,
            val utdanninger: List<UtdanningslopDetaljer>,
        )

        data class Tiltakstype(
            val navn: String,
            val tiltakskode: Tiltakskode,
        )
    }

    data class ValidatedDetaljer(
        val tiltakskode: Tiltakskode,
        val navn: String,
        val sakarkivNummer: SakarkivNummer?,
        val arrangor: Avtale.Arrangor?,
        val startDato: LocalDate,
        val sluttDato: LocalDate?,
        val status: AvtaleStatusType,
        val avtaletype: Avtaletype,
        val administratorer: List<NavIdent>,
        val opplaring: OpplaringKategorisering?,
        val opsjonsmodell: Opsjonsmodell,
    )

    fun validateCreateAvtale(
        request: OpprettAvtaleRequest,
        ctx: Ctx,
    ): Either<List<FieldError>, ValidatedDetaljer> = validation {
        val detaljer = path(OpprettAvtaleRequest::detaljer) {
            validateDetaljer(request.detaljer, ctx).bind()
        }

        validateNavEnheter(ctx.navEnheter).bind()
        detaljer
    }

    fun validateUpdateDetaljer(
        request: DetaljerRequest,
        ctx: Ctx,
    ): Either<List<FieldError>, ValidatedDetaljer> = validation(OpprettAvtaleRequest::detaljer) {
        val previous = requireNotNull(ctx.previous) { FieldError.of("Avtalen finnes ikke") }

        val detaljer = validateDetaljer(request, ctx).bind()

        validate(request.tiltakskode == previous.tiltakskode) {
            FieldError.of(
                "Tiltakstype kan ikke endres etter at avtalen er opprettet",
                DetaljerRequest::tiltakskode,
            )
        }

        if (previous.opsjoner.registreringer.isNotEmpty()) {
            validate(request.avtaletype == previous.avtaletype) {
                FieldError.of(
                    "Du kan ikke endre avtaletype når opsjoner er registrert",
                    DetaljerRequest::avtaletype,
                )
            }
            validate(request.opsjonsmodell.type == previous.opsjoner.modell.type) {
                FieldError.of(
                    "Du kan ikke endre opsjonsmodell når opsjoner er registrert",
                    DetaljerRequest::opsjonsmodell,
                    Opsjonsmodell::type,
                )
            }
        }

        if (previous.gjennomforinger.isNotEmpty()) {
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
                    FieldError.of(
                        "Arrangøren ${gjennomforing.arrangor.navn} er i bruk på en av avtalens gjennomføringer, men mangler blant tiltaksarrangørens underenheter",
                        DetaljerRequest::arrangor,
                        DetaljerRequest.Arrangor::underenheter,
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

        detaljer
    }

    data class ValidatePrismodellerContext(
        val avtaletype: Avtaletype,
        val tiltakskode: Tiltakskode,
        val tiltakstypeNavn: String,
        val avtaleStartDato: LocalDate,
        val gyldigTilsagnPeriode: Map<Tiltakskode, Periode>,
        val bruktePrismodeller: Set<UUID>,
        val systembestemtPrismodell: Prismodell?,
    )

    fun validatePrismodeller(
        request: List<PrismodellRequest>,
        context: ValidatePrismodellerContext,
    ): Either<List<FieldError>, Avtale.Prisinfo> = validation {
        if (context.avtaletype == Avtaletype.FORHANDSGODKJENT) {
            requireValid(request.isEmpty()) {
                FieldError.of(
                    "Prismodell kan ikke opprettes for forhåndsgodkjente avtaler",
                    OpprettAvtaleRequest::prismodeller,
                )
            }
            requireNotNull(context.systembestemtPrismodell) {
                FieldError.of(
                    "Systembestemt prismodell mangler for forhåndsgodkjent avtale",
                    OpprettAvtaleRequest::prismodeller,
                )
            }
            return@validation Avtale.Prisinfo.Systembestemt(context.systembestemtPrismodell)
        }

        requireValid(request.isNotEmpty()) {
            FieldError.of("Minst én prismodell er påkrevd", OpprettAvtaleRequest::prismodeller)
        }

        context.bruktePrismodeller.forEach { prismodellId ->
            validate(request.any { it.id == prismodellId }) {
                FieldError.of(
                    "Prismodell kan ikke fjernes fordi en eller flere gjennomføringer er koblet til prismodellen",
                    OpprettAvtaleRequest::prismodeller,
                )
            }
        }

        request.forEach { prismodell ->
            validate(prismodell.type != PrismodellType.FAST_SATS_PER_BENYTTET_PLASS_PER_MANED) {
                FieldError.of(
                    "Prismodell kan ikke opprettes med typen ${prismodell.type.navn}",
                    OpprettAvtaleRequest::prismodeller,
                )
            }
        }

        val prismodeller = request.mapIndexed { index, prismodell ->
            validate(prismodell.type in Prismodeller.getPrismodellerForTiltak(context.tiltakskode)) {
                FieldError(
                    "/prismodeller/$index/type",
                    "${prismodell.type.navn} er ikke tillatt for tiltakstype ${context.tiltakstypeNavn}",
                )
            }
            validate(prismodell.tilsagnPerDeltaker != true || prismodell.type == PrismodellType.ANNEN_AVTALT_PRIS) {
                FieldError(
                    "/prismodeller/$index/type",
                    "${prismodell.type.navn} kan ikke ha tilsagn per deltaker",
                )
            }

            val satser = when (prismodell.type) {
                PrismodellType.ANNEN_AVTALT_PRIS,
                PrismodellType.TILSKUDD_TIL_OPPLAERING,
                PrismodellType.INGEN_KOSTNADER,
                -> null

                PrismodellType.FAST_SATS_PER_BENYTTET_PLASS_PER_MANED,
                PrismodellType.FAST_SATS_PER_AVTALT_PLASS_PER_MANED,
                PrismodellType.AVTALT_PRIS_PER_BENYTTET_PLASS_PER_MANED,
                PrismodellType.AVTALT_PRIS_PER_BENYTTET_PLASS_PER_UKE,
                PrismodellType.AVTALT_PRIS_PER_BENYTTET_PLASS_PER_HELE_UKE,
                PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
                -> validateSatser(context, prismodell.valuta, index, prismodell.satser)
            }
            Prismodell.from(
                type = prismodell.type,
                id = prismodell.id,
                valuta = prismodell.valuta,
                prisbetingelser = prismodell.prisbetingelser,
                satser = satser,
                tilsagnPerDeltaker = prismodell.tilsagnPerDeltaker,
            )
        }

        Avtale.Prisinfo.Egendefinert(prismodeller)
    }

    data class ValidateOpprettOpsjonContext(
        val avtale: Avtale,
        val navIdent: NavIdent,
    )

    fun validateOpprettOpsjonLoggRequest(
        context: ValidateOpprettOpsjonContext,
        request: OpprettOpsjonLoggRequest,
    ): Either<List<FieldError>, OpsjonLoggDbo> = validation {
        val sluttDato = context.avtale.sluttDato
        requireValid(sluttDato != null) {
            FieldError.of("Opsjon kan ikke utløses fordi avtalen mangler sluttdato", OpprettOpsjonLoggRequest::type)
        }

        val nySluttDato = when (request.type) {
            OpprettOpsjonLoggRequest.Type.CUSTOM_LENGDE -> request.nySluttDato
            OpprettOpsjonLoggRequest.Type.ETT_AAR -> sluttDato.plusYears(1)
            OpprettOpsjonLoggRequest.Type.SKAL_IKKE_UTLOSE_OPSJON -> null
        }

        validate(request.type != OpprettOpsjonLoggRequest.Type.CUSTOM_LENGDE || nySluttDato != null) {
            FieldError.of("Ny sluttdato må være satt", OpprettOpsjonLoggRequest::nySluttDato)
        }

        val maksVarighet = context.avtale.opsjoner.modell.opsjonMaksVarighet
        validate(!(nySluttDato != null && maksVarighet != null && nySluttDato.isAfter(maksVarighet))) {
            FieldError.of("Ny sluttdato er forbi maks varighet av avtalen", OpprettOpsjonLoggRequest::nySluttDato)
        }
        val skalIkkeUtloseOpsjonerForAvtale = context.avtale.opsjoner.registreringer.any {
            it.status == OpsjonLoggStatus.SKAL_IKKE_UTLOSE_OPSJON
        }
        validate(!skalIkkeUtloseOpsjonerForAvtale) {
            FieldError.of("Kan ikke utløse flere opsjoner", OpprettOpsjonLoggRequest::type)
        }

        OpsjonLoggDbo(
            avtaleId = context.avtale.id,
            sluttDato = nySluttDato,
            forrigeSluttDato = sluttDato,
            status = request.type.toOpsjonLoggStatus(),
            registrertAv = context.navIdent,
        )
    }

    fun validateNavEnheter(navEnheter: List<NavEnhetDto>): Either<List<FieldError>, Set<NavEnhetNummer>> = validation {
        val regioner = navEnheter.filter { it.type == NavEnhetType.FYLKE }.map { it.enhetsnummer }.toSet()
        validate(regioner.isNotEmpty()) {
            FieldError("/veilederinformasjon/navRegioner", "Du må velge minst én Nav-region")
        }

        val kontorer = navEnheter.filter { it.overordnetEnhet in regioner }.map { it.enhetsnummer }.toSet()
        validate(kontorer.isNotEmpty()) {
            FieldError("/veilederinformasjon/navKontorer", "Du må velge minst én Nav-enhet")
        }

        regioner + kontorer
    }

    private fun FieldValidator.validateDetaljer(
        request: DetaljerRequest,
        ctx: Ctx,
    ): Either<List<FieldError>, ValidatedDetaljer> {
        validateNotNull(request.startDato) {
            FieldError.of("Du må legge inn startdato for avtalen", DetaljerRequest::navn)
        }
        validate(request.navn.length >= 5) {
            FieldError.of("Avtalenavn må være minst 5 tegn langt", DetaljerRequest::navn)
        }
        validate(request.administratorer.isNotEmpty()) {
            FieldError.of("Du må velge minst én administrator", DetaljerRequest::administratorer)
        }
        validate(request.sluttDato == null || !request.sluttDato.isBefore(request.startDato)) {
            FieldError.of("Startdato må være før sluttdato", DetaljerRequest::startDato)
        }
        validate(request.arrangor == null || request.arrangor.underenheter.isNotEmpty()) {
            FieldError.of(
                "Du må velge minst én underenhet for tiltaksarrangør",
                DetaljerRequest::arrangor,
                DetaljerRequest.Arrangor::underenheter,
            )
        }
        validate(!request.avtaletype.kreverSakarkivNummer() || request.sakarkivNummer != null) {
            FieldError.of("Du må skrive inn saksnummer til avtalesaken", DetaljerRequest::sakarkivNummer)
        }
        validate(request.avtaletype in Avtaletyper.getAvtaletyperForTiltak(request.tiltakskode)) {
            FieldError.of(
                "${request.avtaletype.tittel} er ikke tillatt for tiltakstype ${ctx.tiltakstype.navn}",
                DetaljerRequest::avtaletype,
            )
        }
        if (request.avtaletype == Avtaletype.FORHANDSGODKJENT) {
            validate(request.opsjonsmodell.type == OpsjonsmodellType.VALGFRI_SLUTTDATO) {
                FieldError.of(
                    "Du må velge opsjonsmodell med valgfri sluttdato når avtalen er forhåndsgodkjent",
                    DetaljerRequest::opsjonsmodell,
                    Opsjonsmodell::type,
                )
            }
        } else {
            validate(request.opsjonsmodell.type == OpsjonsmodellType.VALGFRI_SLUTTDATO || request.sluttDato != null) {
                FieldError.of("Du må legge inn sluttdato for avtalen", DetaljerRequest::sluttDato)
            }

            when (request.opsjonsmodell.type) {
                OpsjonsmodellType.INGEN_OPSJONSMULIGHET, OpsjonsmodellType.VALGFRI_SLUTTDATO -> Unit

                OpsjonsmodellType.TO_PLUSS_EN,
                OpsjonsmodellType.TO_PLUSS_EN_PLUSS_EN,
                OpsjonsmodellType.TO_PLUSS_EN_PLUSS_EN_PLUSS_EN,
                OpsjonsmodellType.ANNET,
                -> {
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
        }

        validateSlettetNavAnsatte(ctx.administratorer)

        val opplaring = context(ctx.kategorisering) {
            validateOpplaringKategorisering(
                request.tiltakskode,
                request.amoKategorisering,
                request.utdanningslop,
            ).bind()
        }

        val arrangor = ctx.arrangor?.let {
            validateArrangor(it, request.arrangor?.kontaktpersoner).bind()
        }

        val status = resolveStatus(request, ctx.previous, LocalDate.now())

        return request.toValidatedDetaljer(arrangor, status, opplaring).right()
    }

    private fun resolveStatus(
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

    private fun validateArrangor(
        arrangor: Ctx.AvtaleArrangor,
        kontaktpersoner: List<UUID>?,
    ): Either<List<FieldError>, Avtale.Arrangor> = validation(DetaljerRequest::arrangor) {
        val avtaleArrangor = Avtale.Arrangor(
            hovedenhet = arrangor.hovedenhet.id,
            underenheter = arrangor.underenheter.map { it.id },
            kontaktpersoner = kontaktpersoner ?: emptyList(),
        )

        if (arrangor.hovedenhet is Arrangor.Utenlandsk) {
            return@validation avtaleArrangor
        }

        validate(arrangor.hovedenhet.slettetDato == null) {
            FieldError.of(
                "Arrangøren ${arrangor.hovedenhet.navn} er slettet i Brønnøysundregistrene. Avtaler kan ikke opprettes for slettede bedrifter.",
                DetaljerRequest.Arrangor::hovedenhet,
            )
        }

        arrangor.underenheter.forEach { underenhet ->
            validate(underenhet.slettetDato == null) {
                FieldError.of(
                    "Arrangøren ${underenhet.navn} er slettet i Brønnøysundregistrene. Avtaler kan ikke opprettes for slettede bedrifter.",
                    DetaljerRequest.Arrangor::underenheter,
                )
            }

            validate(underenhet is Arrangor.Norsk && underenhet.overordnetEnhet == arrangor.hovedenhet.organisasjonsnummer) {
                FieldError.of(
                    "Arrangøren ${underenhet.navn} (${underenhet.organisasjonsnummer}) er ikke en gyldig underenhet til hovedenheten ${arrangor.hovedenhet.navn}.",
                    DetaljerRequest.Arrangor::underenheter,
                )
            }
        }

        avtaleArrangor
    }

    private fun FieldValidator.validateSatser(
        context: ValidatePrismodellerContext,
        prismodellValuta: Valuta,
        prismodellIndex: Int,
        satserRequest: List<AvtaltSatsRequest>,
    ): List<AvtaltSats> {
        requireValid(satserRequest.isNotEmpty()) {
            FieldError("/prismodeller/$prismodellIndex/type", "Minst én pris er påkrevd")
        }

        val satser = satserRequest.mapIndexed { index, request ->
            requireValid(request.pris != null && request.pris > 0) {
                FieldError("/prismodeller/$prismodellIndex/satser/$index/pris", "Pris må være positiv")
            }
            requireValid(request.gjelderFra != null) {
                FieldError(
                    "/prismodeller/$prismodellIndex/satser/$index/gjelderFra",
                    "Gjelder fra må være satt",
                )
            }
            AvtaltSats(request.gjelderFra, sats = ValutaBelop(request.pris, prismodellValuta))
        }

        val duplicateDates = satser.map { it.gjelderFra }.groupBy { it }.filter { it.value.size > 1 }.keys
        satser.forEachIndexed { index, (gjelderFra) ->
            validate(gjelderFra !in duplicateDates) {
                FieldError(
                    "/prismodeller/$prismodellIndex/satser/$index/gjelderFra",
                    "Gjelder fra må være unik per rad",
                )
            }
        }

        return satser.sortedBy { it.gjelderFra }.also { satser ->
            val minSatsDato = satser.first().gjelderFra
            val requiredMinSatsDato = maxOf(
                context.avtaleStartDato,
                context.gyldigTilsagnPeriode[context.tiltakskode]?.start ?: context.avtaleStartDato,
            )
            validate(minSatsDato <= requiredMinSatsDato) {
                FieldError(
                    "/prismodeller/$prismodellIndex/satser/0/gjelderFra",
                    "Første sats må gjelde fra ${requiredMinSatsDato.formaterDatoTilEuropeiskDatoformat()}",
                )
            }
        }
    }

    private fun FieldValidator.validateSlettetNavAnsatte(navAnsatte: List<NavAnsatt>) {
        val slettedeNavIdenter = navAnsatte.filter { it.skalSlettesDato != null }
        validate(!slettedeNavIdenter.isNotEmpty()) {
            FieldError.of(
                "Nav identer " + slettedeNavIdenter.joinToString(", ") { it.navIdent.value } + " er slettet og må fjernes",
                DetaljerRequest::administratorer,
            )
        }
    }

    context(ctx: Ctx.Kategorisering)
    private fun FieldValidator.validateOpplaringKategorisering(
        tiltakskode: Tiltakskode,
        amoKategorisering: AmoKategoriseringRequest?,
        utdanningslop: Utdanningslop?,
    ): Either<List<FieldError>, OpplaringKategorisering?> = when (tiltakskode) {
        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING -> {
            requireValid(amoKategorisering?.kurstype != null) {
                FieldError.of(
                    "Du må velge en kurstype",
                    DetaljerRequest::amoKategorisering,
                    AmoKategoriseringRequest::kurstype,
                )
            }
            if (amoKategorisering.kurstype == Kurstype.Kode.BRANSJE_OG_YRKESRETTET) {
                requireValid(amoKategorisering.bransje != null) {
                    FieldError.of(
                        "Du må velge en bransje",
                        DetaljerRequest::amoKategorisering,
                        AmoKategoriseringRequest::bransje,
                    )
                }
            }
            amoKategorisering.toOpplaringKategorisering()
        }

        Tiltakskode.ARBEIDSMARKEDSOPPLAERING -> {
            requireValid(amoKategorisering?.bransje != null) {
                FieldError.of(
                    "Du må velge en bransje",
                    DetaljerRequest::amoKategorisering,
                    AmoKategoriseringRequest::bransje,
                )
            }
            amoKategorisering.copy(kurstype = Kurstype.Kode.BRANSJE_OG_YRKESRETTET).toOpplaringKategorisering()
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
                    Kurstype.Kode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE,
                    Kurstype.Kode.NORSKOPPLAERING,
                    Kurstype.Kode.GRUNNLEGGENDE_FERDIGHETER,
                ),
            ) {
                FieldError.of(
                    "Ugyldig kurstype",
                    DetaljerRequest::amoKategorisering,
                    AmoKategoriseringRequest::kurstype,
                )
            }
            amoKategorisering.toOpplaringKategorisering()
        }

        Tiltakskode.STUDIESPESIALISERING,
        -> AmoKategoriseringRequest(kurstype = Kurstype.Kode.STUDIESPESIALISERING).toOpplaringKategorisering()

        Tiltakskode.FAG_OG_YRKESOPPLAERING,
        Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
        ->
            validateUtdanningslop(utdanningslop).bind()

        else -> null
    }.right()

    context(ctx: Ctx.Kategorisering)
    private fun AmoKategoriseringRequest.toOpplaringKategorisering(): OpplaringKategorisering {
        return OpplaringKategorisering(
            kurstype = ctx.kurstyper.find { it.kode == kurstype }?.id,
            bransje = ctx.bransjer.find { it.kode == bransje }?.id,
            forerkort = ctx.forerkort.mapNotNull {
                if (forerkort?.contains(it.kode) == true) {
                    it.id
                } else {
                    null
                }
            }.toSet(),
            innholdElementer = ctx.innholdElementer.mapNotNull {
                if (innholdElementer?.contains(it.kode) == true) {
                    it.id
                } else {
                    null
                }
            }.toSet(),
            norskprove = norskprove,
            sertifiseringer = sertifiseringer?.toSet() ?: emptySet(),
            utdanningslop = null, // TODO: Håndteres seperat per nå, så lenge AmoKategoriseringRequest består
        )
    }

    private fun FieldValidator.validateUtdanningslop(
        utdanningslop: Utdanningslop?,
    ): Either<List<FieldError>, OpplaringKategorisering?> {
        validateNotNull(utdanningslop) {
            FieldError.of(
                "Du må velge et utdanningsprogram og minst ett lærefag",
                DetaljerRequest::utdanningslop,
            )
        }
        validate(utdanningslop == null || utdanningslop.utdanninger.isNotEmpty()) {
            FieldError.of("Du må velge minst ett lærefag", DetaljerRequest::utdanningslop)
        }

        return OpplaringKategorisering(utdanningslop = utdanningslop).right()
    }
}

private fun DetaljerRequest.toValidatedDetaljer(
    arrangor: Avtale.Arrangor?,
    status: AvtaleStatusType,
    opplaring: OpplaringKategorisering?,
): AvtaleValidator.ValidatedDetaljer = AvtaleValidator.ValidatedDetaljer(
    tiltakskode = tiltakskode,
    navn = navn,
    sakarkivNummer = sakarkivNummer,
    arrangor = arrangor,
    startDato = startDato,
    sluttDato = sluttDato,
    status = status,
    avtaletype = avtaletype,
    administratorer = administratorer,
    opplaring = opplaring,
    opsjonsmodell = opsjonsmodell,
)
