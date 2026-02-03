package no.nav.mulighetsrommet.api.avtale

import arrow.core.Either
import arrow.core.right
import no.nav.mulighetsrommet.api.amo.AmoKategoriseringRequest
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.avtale.api.DetaljerRequest
import no.nav.mulighetsrommet.api.avtale.api.OpprettAvtaleRequest
import no.nav.mulighetsrommet.api.avtale.api.OpprettOpsjonLoggRequest
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.db.DetaljerDbo
import no.nav.mulighetsrommet.api.avtale.db.PrismodellDbo
import no.nav.mulighetsrommet.api.avtale.db.RammedetaljerDbo
import no.nav.mulighetsrommet.api.avtale.db.RedaksjoneltInnholdDbo
import no.nav.mulighetsrommet.api.avtale.db.VeilederinformasjonDbo
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
import no.nav.mulighetsrommet.api.avtale.model.RammedetaljerRequest
import no.nav.mulighetsrommet.api.avtale.model.UtdanningslopDto
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.navenhet.NavEnhetDto
import no.nav.mulighetsrommet.api.navenhet.NavEnhetType
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.api.validation.FieldValidator
import no.nav.mulighetsrommet.api.validation.validation
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.AmoKurstype
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.Avtaletyper
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import java.time.LocalDate
import java.util.UUID
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
object AvtaleValidator {
    data class Ctx(
        val previous: Avtale?,
        val arrangor: ArrangorDto?,
        val administratorer: List<NavAnsatt>,
        val tiltakstype: Tiltakstype,
        val navEnheter: List<NavEnhetDto>,
        val systembestemtPrismodell: UUID?,
    ) {
        data class Avtale(
            val status: AvtaleStatusType,
            val opphav: ArenaMigrering.Opphav,
            val opsjonerRegistrert: List<no.nav.mulighetsrommet.api.avtale.model.Avtale.OpsjonLoggDto>,
            val opsjonsmodell: Opsjonsmodell,
            val avtaletype: Avtaletype,
            val tiltakskode: Tiltakskode,
            val gjennomforinger: List<Gjennomforing>,
            val prismodeller: List<Prismodell>,
        )

        data class Gjennomforing(
            val arrangor: no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing.ArrangorUnderenhet,
            val startDato: LocalDate,
            val utdanningslop: UtdanningslopDto?,
            val status: GjennomforingStatusType,
            val prismodellId: UUID,
        )

        data class Tiltakstype(
            val navn: String,
            val id: UUID,
        )
    }

    fun validateCreateAvtale(
        request: OpprettAvtaleRequest,
        ctx: Ctx,
    ): Either<List<FieldError>, AvtaleDbo> = validation {
        val amoKategorisering = path(OpprettAvtaleRequest::detaljer) {
            validateDetaljer(request.detaljer, ctx).bind()
        }

        validateNavEnheter(ctx.navEnheter).bind()
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

        val prismodeller = ctx.systembestemtPrismodell?.let { listOf(it) } ?: request.prismodeller.map { it.id }
        validate(prismodeller.isNotEmpty()) {
            FieldError.of("Minst én prismodell er påkrevd", OpprettAvtaleRequest::prismodeller)
        }

        val personvernDbo = request.personvern.toDbo()

        val navEnheter = validateNavEnheter(ctx.navEnheter).bind()
        val veilederinformasjonDbo = VeilederinformasjonDbo(
            redaksjoneltInnhold = RedaksjoneltInnholdDbo(
                beskrivelse = request.veilederinformasjon.beskrivelse,
                faneinnhold = request.veilederinformasjon.faneinnhold,
            ),
            navEnheter = navEnheter,
        )

        fromValidatedAvtaleRequest(request.id, detaljerDbo, prismodeller, personvernDbo, veilederinformasjonDbo)
    }

    fun validateUpdateDetaljer(
        request: DetaljerRequest,
        ctx: Ctx,
    ): Either<List<FieldError>, DetaljerDbo> = validation(OpprettAvtaleRequest::detaljer) {
        val amoKategorisering = validateDetaljer(request, ctx).bind()

        val previous = requireNotNull(ctx.previous) { "Avtalen finnes ikke" }

        validate(request.tiltakskode == previous.tiltakskode) {
            FieldError.of(
                "Tiltakstype kan ikke endres etter at avtalen er opprettet",
                DetaljerRequest::tiltakskode,
            )
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

        request.toDbo(
            ctx.tiltakstype.id,
            ctx.arrangor?.toDbo(request.arrangor?.kontaktpersoner),
            resolveStatus(request, previous, LocalDate.now()),
            amoKategorisering = amoKategorisering,
        )
    }

    data class ValidatePrismodellerContext(
        val avtaletype: Avtaletype,
        val tiltakskode: Tiltakskode,
        val tiltakstypeNavn: String,
        val avtaleStartDato: LocalDate,
        val gyldigTilsagnPeriode: Map<Tiltakskode, Periode>,
        val bruktePrismodeller: Set<UUID>,
    )

    fun validatePrismodeller(
        request: List<PrismodellRequest>,
        context: ValidatePrismodellerContext,
    ): Either<List<FieldError>, List<PrismodellDbo>> = validation {
        if (context.avtaletype == Avtaletype.FORHANDSGODKJENT) {
            requireValid(request.isEmpty()) {
                FieldError.of(
                    "Prismodell kan ikke opprettes for forhåndsgodkjente avtaler",
                    OpprettAvtaleRequest::prismodeller,
                )
            }
            return@validation listOf()
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
            validate(prismodell.type != PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK) {
                FieldError.of(
                    "Prismodell kan ikke opprettes med typen ${prismodell.type.navn}",
                    OpprettAvtaleRequest::prismodeller,
                )
            }
        }

        request.mapIndexed { index, prismodell ->
            validate(prismodell.type in Prismodeller.getPrismodellerForTiltak(context.tiltakskode)) {
                FieldError(
                    "/prismodeller/$index/type",
                    "${prismodell.type.navn} er ikke tillatt for tiltakstype ${context.tiltakstypeNavn}",
                )
            }

            val satser = when (prismodell.type) {
                PrismodellType.ANNEN_AVTALT_PRIS,
                -> null

                PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
                PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
                PrismodellType.AVTALT_PRIS_PER_UKESVERK,
                PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK,
                PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
                -> validateSatser(context, prismodell.valuta, index, prismodell.satser)
            }
            PrismodellDbo(
                id = prismodell.id,
                systemId = null,
                type = prismodell.type,
                prisbetingelser = prismodell.prisbetingelser,
                satser = satser,
                valuta = prismodell.valuta,
            )
        }
    }

    fun validateRammedetaljer(
        avtale: Avtale,
        request: RammedetaljerRequest,
    ): Either<List<FieldError>, RammedetaljerDbo> = validation {
        validate(avtale.prismodeller.all { kanHaRammedetaljer(it.type) }) {
            FieldError.of(
                "Rammedetaljer kan kun legges til anskaffet avtaler",
                RammedetaljerRequest::totalRamme,
            )
        }
        validate(avtale.prismodeller.distinctBy { it.valuta }.count() == 1) {
            FieldError.of(
                "Rammedetaljer kan kun legges til avtaler med én type valuta på prismodellene",
                RammedetaljerRequest::totalRamme,
            )
        }
        validate(request.totalRamme > 0) {
            FieldError.of(
                "Total ramme må være et positivt beløp",
            )
        }
        request.utbetaltArena?.let { utbetaltArena ->
            validate(utbetaltArena >= 0) {
                FieldError.of(
                    "Utbetalt beløp fra Arena må være et positivt beløp",
                    RammedetaljerRequest::utbetaltArena,
                )
            }
        }

        RammedetaljerDbo(
            avtaleId = avtale.id,
            valuta = avtale.prismodeller.first().valuta,
            totalRamme = request.totalRamme,
            utbetaltArena = request.utbetaltArena,
        )
    }

    private fun kanHaRammedetaljer(prismodellType: PrismodellType) = when (prismodellType) {
        PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK -> false

        PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
        PrismodellType.AVTALT_PRIS_PER_UKESVERK,
        PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK,
        PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
        PrismodellType.ANNEN_AVTALT_PRIS,
        -> true
    }

    data class ValidateOpprettOpsjonContext(
        val avtale: Avtale,
        val navIdent: NavIdent,
    )

    fun validateOpprettOpsjonLoggRequest(
        context: ValidateOpprettOpsjonContext,
        request: OpprettOpsjonLoggRequest,
    ): Either<List<FieldError>, OpsjonLoggDbo> = validation {
        requireValid(context.avtale.sluttDato != null) {
            FieldError.of("Opsjon kan ikke utløses fordi avtalen mangler sluttdato", OpprettOpsjonLoggRequest::type)
        }

        val nySluttDato = when (request.type) {
            OpprettOpsjonLoggRequest.Type.CUSTOM_LENGDE -> request.nySluttDato
            OpprettOpsjonLoggRequest.Type.ETT_AAR -> context.avtale.sluttDato.plusYears(1)
            OpprettOpsjonLoggRequest.Type.SKAL_IKKE_UTLOSE_OPSJON -> null
        }

        validate(request.type != OpprettOpsjonLoggRequest.Type.CUSTOM_LENGDE || nySluttDato != null) {
            FieldError.of("Ny sluttdato må være satt", OpprettOpsjonLoggRequest::nySluttDato)
        }

        val maksVarighet = context.avtale.opsjonsmodell.opsjonMaksVarighet
        validate(!(nySluttDato != null && maksVarighet != null && nySluttDato.isAfter(maksVarighet))) {
            FieldError.of("Ny sluttdato er forbi maks varighet av avtalen", OpprettOpsjonLoggRequest::nySluttDato)
        }
        val skalIkkeUtloseOpsjonerForAvtale = context.avtale.opsjonerRegistrert.any {
            it.status === OpsjonLoggStatus.SKAL_IKKE_UTLOSE_OPSJON
        }
        validate(!skalIkkeUtloseOpsjonerForAvtale) {
            FieldError.of("Kan ikke utløse flere opsjoner", OpprettOpsjonLoggRequest::type)
        }

        OpsjonLoggDbo(
            avtaleId = context.avtale.id,
            sluttDato = nySluttDato,
            forrigeSluttDato = context.avtale.sluttDato,
            status = OpsjonLoggStatus.fromType(request.type),
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
        val amoKategorisering = validateAmoKategorisering(request.tiltakskode, request.amoKategorisering)
        validateUtdanningslop(request.tiltakskode, request.utdanningslop)

        validateSlettetNavAnsatte(ctx.administratorer)
        ctx.arrangor?.let { validateArrangor(it).bind() }
        return amoKategorisering
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

    private fun validateArrangor(arrangor: ArrangorDto) = validation(DetaljerRequest::arrangor) {
        if (arrangor.erUtenlandsk) {
            return@validation
        }

        validate(arrangor.slettetDato == null) {
            FieldError.of(
                "Arrangøren ${arrangor.navn} er slettet i Brønnøysundregistrene. Avtaler kan ikke opprettes for slettede bedrifter.",
                DetaljerRequest.Arrangor::hovedenhet,
            )
        }

        arrangor.underenheter?.forEach { underenhet ->
            validate(underenhet.slettetDato == null) {
                FieldError.of(
                    "Arrangøren ${underenhet.navn} er slettet i Brønnøysundregistrene. Avtaler kan ikke opprettes for slettede bedrifter.",
                    DetaljerRequest.Arrangor::underenheter,
                )
            }

            validate(underenhet.overordnetEnhet == arrangor.organisasjonsnummer) {
                FieldError.of(
                    "Arrangøren ${underenhet.navn} - ${underenhet.organisasjonsnummer.value} er ikke en gyldig underenhet til hovedenheten ${arrangor.navn}.",
                    DetaljerRequest.Arrangor::underenheter,
                )
            }
        }
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

    private fun FieldValidator.validateAmoKategorisering(
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

    private fun FieldValidator.validateUtdanningslop(
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
