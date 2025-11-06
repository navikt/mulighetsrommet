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
import no.nav.mulighetsrommet.api.validation.ValidationDsl
import no.nav.mulighetsrommet.api.validation.validation
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
    ): Either<List<FieldError>, AvtaleDbo> = validation {
        validateNotNull(request.startDato) {
            FieldError.of("Du må legge inn startdato for avtalen", AvtaleRequest::navn)
        }
        validate(request.navn.length >= 5 || ctx.previous?.opphav == ArenaMigrering.Opphav.ARENA) {
            FieldError.of("Avtalenavn må være minst 5 tegn langt", AvtaleRequest::navn)
        }
        validate(request.administratorer.isNotEmpty()) {
            FieldError.of("Du må velge minst én administrator", AvtaleRequest::administratorer)
        }
        validate(request.sluttDato == null || !request.sluttDato.isBefore(request.startDato)) {
            FieldError.of("Startdato må være før sluttdato", AvtaleRequest::startDato)
        }
        validate(request.arrangor == null || request.arrangor.underenheter.isNotEmpty()) {
            FieldError.ofPointer(
                "/arrangorUnderenheter",
                "Du må velge minst én underenhet for tiltaksarrangør",
            )
        }
        validate(!request.avtaletype.kreverSakarkivNummer() || request.sakarkivNummer != null) {
            FieldError.of("Du må skrive inn saksnummer til avtalesaken", AvtaleRequest::sakarkivNummer)
        }
        validate(request.avtaletype in Avtaletyper.getAvtaletyperForTiltak(request.tiltakskode)) {
            FieldError.of(
                "${request.avtaletype.beskrivelse} er ikke tillatt for tiltakstype ${ctx.tiltakstype.navn}",
                AvtaleRequest::avtaletype,
            )
        }
        if (request.avtaletype == Avtaletype.FORHANDSGODKJENT) {
            validate(request.opsjonsmodell.type == OpsjonsmodellType.VALGFRI_SLUTTDATO) {
                FieldError.of(
                    "Du må velge opsjonsmodell med valgfri sluttdato når avtalen er forhåndsgodkjent",
                    AvtaleRequest::opsjonsmodell,
                )
            }
        } else {
            validate(request.opsjonsmodell.type == OpsjonsmodellType.VALGFRI_SLUTTDATO || request.sluttDato != null) {
                FieldError.of("Du må legge inn sluttdato for avtalen", AvtaleRequest::sluttDato)
            }
            if (request.opsjonsmodell.type !in opsjonsmodellerUtenValidering) {
                validate(request.opsjonsmodell.opsjonMaksVarighet != null) {
                    FieldError.of(
                        "Du må legge inn maks varighet for opsjonen",
                        AvtaleRequest::opsjonsmodell,
                        Opsjonsmodell::opsjonMaksVarighet,
                    )
                }
                if (request.opsjonsmodell.type == OpsjonsmodellType.ANNET) {
                    validate(!request.opsjonsmodell.customOpsjonsmodellNavn.isNullOrBlank()) {
                        FieldError.of(
                            "Du må beskrive opsjonsmodellen",
                            AvtaleRequest::opsjonsmodell,
                            Opsjonsmodell::customOpsjonsmodellNavn,
                        )
                    }
                }
            }
        }
        validate(request.tiltakskode != Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING || request.amoKategorisering != null) {
            FieldError.ofPointer("/amoKategorisering.kurstype", "Du må velge en kurstype")
        }
        if (request.tiltakskode == Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING) {
            val utdanninger = request.utdanningslop
            validateNotNull(utdanninger) {
                FieldError.of(
                    "Du må velge et utdanningsprogram og minst ett lærefag",
                    AvtaleRequest::utdanningslop,
                )
            }
            validate(utdanninger == null || utdanninger.utdanninger.isNotEmpty()) {
                FieldError.of("Du må velge minst ett lærefag", AvtaleRequest::utdanningslop)
            }
        }

        validateNavEnheter(ctx.navEnheter)
        validateSlettetNavAnsatte(ctx.administratorer, AvtaleRequest::administratorer)

        if (ctx.previous == null) {
            validateCreateAvtale(ctx.arrangor)
        } else {
            validateUpdateAvtale(request, ctx.arrangor, ctx.previous)
        }

        validatePrismodell(request.prismodell, request.tiltakskode, ctx.tiltakstype.navn)
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
            .bind()
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

    private fun ValidationDsl.validateCreateAvtale(
        arrangor: ArrangorDto?,
    ) {
        if (arrangor == null) {
            return
        }
        validate(arrangor.slettetDato == null) {
            FieldError.ofPointer(
                "/arrangorHovedenhet",
                "Arrangøren ${arrangor.navn} er slettet i Brønnøysundregistrene. Avtaler kan ikke opprettes for slettede bedrifter.",
            )
        }

        arrangor.underenheter?.forEach { underenhet ->
            validate(underenhet.slettetDato == null) {
                FieldError.ofPointer(
                    "/arrangorUnderenheter",
                    "Arrangøren ${underenhet.navn} er slettet i Brønnøysundregistrene. Avtaler kan ikke opprettes for slettede bedrifter.",
                )
            }

            validate(underenhet.overordnetEnhet == arrangor.organisasjonsnummer) {
                FieldError.ofPointer(
                    "/arrangorUnderenheter",
                    "Arrangøren ${underenhet.navn} er ikke en gyldig underenhet til hovedenheten ${arrangor.navn}.",
                )
            }
        }
    }

    private fun ValidationDsl.validateUpdateAvtale(
        request: AvtaleRequest,
        arrangor: ArrangorDto?,
        previous: Ctx.Avtale,
    ) {
        if (previous.opsjonerRegistrert.isNotEmpty()) {
            validate(request.avtaletype == previous.avtaletype) {
                FieldError.of(
                    "Du kan ikke endre avtaletype når opsjoner er registrert",
                    AvtaleRequest::avtaletype,
                )
            }
            validate(request.opsjonsmodell.type == previous.opsjonsmodell.type) {
                FieldError.of(
                    "Du kan ikke endre opsjonsmodell når opsjoner er registrert",
                    AvtaleRequest::opsjonsmodell,
                )
            }
        }
        validate(request.prismodell.type in Prismodeller.getPrismodellerForTiltak(request.tiltakskode)) {
            FieldError.of(
                "Tiltakstype kan ikke endres ikke fordi prismodellen “${request.prismodell.type.navn}” er i bruk",
                AvtaleRequest::tiltakskode,
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
            validate(request.tiltakskode == previous.tiltakskode) {
                FieldError.of(
                    "Tiltakstype kan ikke endres fordi det finnes gjennomføringer for avtalen",
                    AvtaleRequest::tiltakskode,
                )
            }

            val earliestGjennomforingStartDato = previous.gjennomforinger.minBy { it.startDato }.startDato
            validate(!earliestGjennomforingStartDato.isBefore(request.startDato)) {
                FieldError.of(
                    "Startdato kan ikke være etter startdatoen til gjennomføringer koblet til avtalen. Minst en gjennomføring har startdato: ${earliestGjennomforingStartDato.formaterDatoTilEuropeiskDatoformat()}",
                    AvtaleRequest::startDato,
                )
            }

            previous.gjennomforinger.forEach { gjennomforing ->
                val arrangorId = gjennomforing.arrangor.id

                validateNotNull(request.arrangor) {
                    FieldError.of(
                        "Arrangør kan ikke fjernes fordi en gjennomføring er koblet til avtalen",
                        AvtaleRequest::arrangor,
                        AvtaleRequest.Arrangor::hovedenhet,
                    )
                }
                validate(arrangor?.underenheter?.map { it.id }?.contains(arrangorId) == true) {
                    FieldError.ofPointer(
                        "/arrangorUnderenheter",
                        "Arrangøren ${gjennomforing.arrangor.navn} er i bruk på en av avtalens gjennomføringer, men mangler blant tiltaksarrangørens underenheter",
                    )
                }

                gjennomforing.utdanningslop?.also {
                    validate(request.utdanningslop?.utdanningsprogram == it.utdanningsprogram.id) {
                        FieldError.of(
                            "Utdanningsprogram kan ikke endres fordi en gjennomføring allerede er opprettet for utdanningsprogrammet ${it.utdanningsprogram.navn}",
                            AvtaleRequest::utdanningslop,
                        )
                    }

                    it.utdanninger.forEach { utdanning ->
                        val utdanninger = request.utdanningslop?.utdanninger ?: listOf()
                        validate(utdanninger.contains(utdanning.id)) {
                            FieldError.of(
                                "Lærefaget ${utdanning.navn} mangler i avtalen, men er i bruk på en av avtalens gjennomføringer",
                                AvtaleRequest::utdanningslop,
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
                FieldError.ofPointer("/satser/$index/pris", "Pris må være positiv")
            }
        }
        for (i in satser.indices) {
            val a = satser[i]
            if (a.gjelderFra == null) {
                validate(false) {
                    FieldError.ofPointer("/satser/$i/gjelderFra", "Gjelder fra må være satt")
                }
                continue
            }
            for (j in i + 1 until satser.size) {
                val b = satser[j]
                if (!a.gjelderFra.isBefore(b.gjelderFra)) {
                    validate(false) {
                        FieldError.ofPointer("/satser/$j/gjelderFra", "Ny pris må gjelde etter forrige pris")
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
}
