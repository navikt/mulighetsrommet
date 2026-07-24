package no.nav.mulighetsrommet.api.avtale

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.admin.navenhet.toDto
import no.nav.mulighetsrommet.api.avtale.AvtaleValidator.Ctx
import no.nav.mulighetsrommet.api.avtale.api.AmoKategoriseringRequest
import no.nav.mulighetsrommet.api.avtale.api.DetaljerRequest
import no.nav.mulighetsrommet.api.avtale.api.VeilederinfoRequest
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSatsRequest
import no.nav.mulighetsrommet.api.avtale.model.PrismodellRequest
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.domain.opplaring.Bransje
import no.nav.mulighetsrommet.api.domain.opplaring.ForerkortKlasse
import no.nav.mulighetsrommet.api.domain.opplaring.InnholdElement
import no.nav.mulighetsrommet.api.domain.opplaring.Kurstype
import no.nav.mulighetsrommet.api.domain.opplaring.Sertifisering
import no.nav.mulighetsrommet.api.domain.opplaring.Utdanningslop
import no.nav.mulighetsrommet.api.domain.tiltak.Avtale
import no.nav.mulighetsrommet.api.domain.tiltak.AvtaltSats
import no.nav.mulighetsrommet.api.domain.tiltak.OpsjonLoggStatus
import no.nav.mulighetsrommet.api.domain.tiltak.Opsjonsmodell
import no.nav.mulighetsrommet.api.domain.tiltak.OpsjonsmodellType
import no.nav.mulighetsrommet.api.domain.tiltak.Prismodell
import no.nav.mulighetsrommet.api.domain.tiltak.PrismodellType
import no.nav.mulighetsrommet.api.domain.tiltak.Tiltakstype
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.BransjeFixtures
import no.nav.mulighetsrommet.api.fixtures.ForerkortFixtures
import no.nav.mulighetsrommet.api.fixtures.InnholdElementFixtures
import no.nav.mulighetsrommet.api.fixtures.KurstypeFixtures
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.fixtures.PrismodellFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class AvtaleValidatorTest : FunSpec({
    val avtaleRequest = AvtaleFixtures.createAvtaleRequest(
        Tiltakskode.OPPFOLGING,
        avtaletype = Avtaletype.RAMMEAVTALE,
        prismodell = listOf(PrismodellFixtures.AvtaltPrisPerTimeOppfolging),
    )
    val gruppeAmo = AvtaleFixtures.createAvtaleRequest(
        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG,
        amo = AmoKategoriseringRequest(kurstype = Kurstype.Kode.STUDIESPESIALISERING),
    )
    val forhaandsgodkjent = AvtaleFixtures.createAvtaleRequest(
        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        avtaletype = Avtaletype.FORHANDSGODKJENT,
        opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.VALGFRI_SLUTTDATO, null),
        prismodell = listOf(),
    )
    val avtaleTypeAvtale = AvtaleFixtures.createAvtaleRequest(
        Tiltakskode.OPPFOLGING,
        avtaletype = Avtaletype.AVTALE,
    )
    val oppfolgingMedRammeAvtale = AvtaleFixtures.createAvtaleRequest(
        Tiltakskode.OPPFOLGING,
        avtaletype = Avtaletype.RAMMEAVTALE,
    )
    val prismodell = Prismodell.AnnenAvtaltPris(
        id = UUID.randomUUID(),
        valuta = Valuta.NOK,
        prisbetingelser = "",
        tilsagnPerDeltaker = false,
        totalbelop = null,
    )
    val kategoriseringCtx = Ctx.Kategorisering(
        kurstyper = KurstypeFixtures.all(),
        bransjer = BransjeFixtures.all(),
        forerkort = ForerkortFixtures.all(),
        innholdElementer = InnholdElementFixtures.all(),
        utdanninger = emptyList(),
    )
    val ctx = Ctx(
        previous = null,
        arrangor = Ctx.AvtaleArrangor(ArrangorFixtures.hovedenhet, listOf(ArrangorFixtures.underenhet1)),
        kategorisering = kategoriseringCtx,
        administratorer = emptyList(),
        tiltakstype = Ctx.Tiltakstype(
            navn = TiltakstypeFixtures.Oppfolging.navn,
            tiltakskode = TiltakstypeFixtures.Oppfolging.tiltakskode,
        ),
        navEnheter = listOf(NavEnhetFixtures.Innlandet.toDto(), NavEnhetFixtures.Gjovik.toDto()),
        systembestemtPrismodell = null,
    )

    val createForhandsgodkjentAvtaleContext = Ctx(
        previous = null,
        arrangor = Ctx.AvtaleArrangor(ArrangorFixtures.hovedenhet, listOf(ArrangorFixtures.underenhet1)),
        kategorisering = kategoriseringCtx,
        administratorer = emptyList(),
        tiltakstype = Ctx.Tiltakstype(
            navn = TiltakstypeFixtures.AFT.navn,
            tiltakskode = TiltakstypeFixtures.AFT.tiltakskode,
        ),
        navEnheter = listOf(NavEnhetFixtures.Innlandet.toDto(), NavEnhetFixtures.Gjovik.toDto()),
        systembestemtPrismodell = UUID.randomUUID(),
    )

    test("skal akkumulere feil når forespørselen har flere problemer") {
        val request = avtaleRequest.copy(
            detaljer = avtaleRequest.detaljer.copy(
                startDato = LocalDate.of(2023, 1, 1),
                sluttDato = LocalDate.of(2020, 1, 1),
                arrangor = DetaljerRequest.Arrangor(
                    hovedenhet = ArrangorFixtures.hovedenhet.organisasjonsnummer,
                    underenheter = emptyList(),
                    kontaktpersoner = emptyList(),
                ),
            ),
            veilederinformasjon = VeilederinfoRequest(navEnheter = emptyList(), beskrivelse = null, faneinnhold = null),
        )

        AvtaleValidator.validateCreateAvtale(
            request,
            ctx.copy(
                navEnheter = emptyList(),
            ),
        ).shouldBeLeft().shouldContainAll(
            listOf(
                FieldError("/detaljer/startDato", "Startdato må være før sluttdato"),
                FieldError("/detaljer/arrangor/underenheter", "Du må velge minst én underenhet for tiltaksarrangør"),
            ),
        )
    }

    test("Avtalenavn må være minst 5 tegn når avtalen er opprettet i Admin-flate") {
        val request = avtaleRequest.copy(detaljer = avtaleRequest.detaljer.copy(navn = "Avt"))

        AvtaleValidator.validateCreateAvtale(request, ctx).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(
                FieldError("/detaljer/navn", "Avtalenavn må være minst 5 tegn langt"),
            ),
        )
    }

    test("Avtalens startdato må være før eller lik som sluttdato") {
        val dagensDato = LocalDate.now()
        val request =
            avtaleRequest.copy(detaljer = avtaleRequest.detaljer.copy(startDato = dagensDato, sluttDato = dagensDato))

        AvtaleValidator.validateCreateAvtale(request, ctx).shouldBeRight()

        val request2 = avtaleRequest.copy(
            detaljer = avtaleRequest.detaljer.copy(
                startDato = dagensDato.plusDays(5),
                sluttDato = dagensDato,
            ),
        )

        AvtaleValidator.validateCreateAvtale(request2, ctx).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(FieldError("/detaljer/startDato", "Startdato må være før sluttdato")),
        )
    }

    test("Avtalens sluttdato være lik eller etter startdato") {
        val dagensDato = LocalDate.now()
        val request = avtaleRequest.copy(
            detaljer = avtaleRequest.detaljer.copy(
                startDato = dagensDato,
                sluttDato = dagensDato.minusDays(5),
            ),
        )

        AvtaleValidator.validateCreateAvtale(request, ctx).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(FieldError("/detaljer/startDato", "Startdato må være før sluttdato")),
        )

        val request2 =
            avtaleRequest.copy(detaljer = avtaleRequest.detaljer.copy(startDato = dagensDato, sluttDato = dagensDato))
        AvtaleValidator.validateCreateAvtale(request2, ctx).shouldBeRight()
    }

    test("skal validere at Nav-fylke og Nav-enheter er påkrevd") {
        AvtaleValidator.validateCreateAvtale(avtaleRequest, ctx.copy(navEnheter = emptyList())).shouldBeLeft()
            .shouldContainExactlyInAnyOrder(
                listOf(
                    FieldError("/veilederinformasjon/navRegioner", "Du må velge minst én Nav-region"),
                    FieldError("/veilederinformasjon/navKontorer", "Du må velge minst én Nav-enhet"),
                ),
            )
    }

    test("sluttDato er påkrevd hvis ikke forhåndsgodkjent") {
        val forhaandsgodkjent1 = forhaandsgodkjent.copy(
            detaljer = forhaandsgodkjent.detaljer.copy(
                sluttDato = null,
                opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.VALGFRI_SLUTTDATO, null),
            ),
        )
        val offentligOffentlig = gruppeAmo.copy(
            detaljer = gruppeAmo.detaljer.copy(
                sluttDato = null,
            ),
        )

        AvtaleValidator.validateCreateAvtale(forhaandsgodkjent1, createForhandsgodkjentAvtaleContext).shouldBeRight()
        AvtaleValidator.validateCreateAvtale(
            oppfolgingMedRammeAvtale.copy(detaljer = avtaleTypeAvtale.detaljer.copy(sluttDato = null)),
            ctx,
        ).shouldBeLeft(
            listOf(FieldError("/detaljer/sluttDato", "Du må legge inn sluttdato for avtalen")),
        )
        AvtaleValidator.validateCreateAvtale(
            avtaleTypeAvtale.copy(detaljer = avtaleTypeAvtale.detaljer.copy(sluttDato = null)),
            ctx,
        ).shouldBeLeft(
            listOf(FieldError("/detaljer/sluttDato", "Du må legge inn sluttdato for avtalen")),
        )
        AvtaleValidator.validateCreateAvtale(
            offentligOffentlig.copy(detaljer = avtaleTypeAvtale.detaljer.copy(sluttDato = null)),
            ctx,
        ).shouldBeLeft(
            listOf(FieldError("/detaljer/sluttDato", "Du må legge inn sluttdato for avtalen")),
        )
    }

    test("Opsjonsmodell må være VALGFRI_SLUTTDATO når avtale er forhåndsgodkjent") {
        AvtaleValidator.validateCreateAvtale(
            forhaandsgodkjent.copy(
                detaljer = forhaandsgodkjent.detaljer.copy(
                    opsjonsmodell = Opsjonsmodell(
                        OpsjonsmodellType.VALGFRI_SLUTTDATO,
                        null,

                    ),
                ),
            ),
            createForhandsgodkjentAvtaleContext,
        ).shouldBeRight()
        AvtaleValidator.validateCreateAvtale(
            forhaandsgodkjent.copy(
                detaljer = forhaandsgodkjent.detaljer.copy(
                    opsjonsmodell = Opsjonsmodell(
                        OpsjonsmodellType.TO_PLUSS_EN,
                        null,
                    ),
                ),
            ),
            createForhandsgodkjentAvtaleContext,
        ).shouldBeLeft(
            listOf(
                FieldError(
                    "/detaljer/opsjonsmodell/type",
                    "Du må velge opsjonsmodell med valgfri sluttdato når avtalen er forhåndsgodkjent",
                ),
            ),
        )
    }
    test("Opsjonsmodell må Opsjonsdata må være satt når avtaletypen ikke er forhåndsgodkjent") {
        AvtaleValidator.validateCreateAvtale(
            avtaleTypeAvtale.copy(
                detaljer = avtaleTypeAvtale.detaljer.copy(
                    opsjonsmodell = Opsjonsmodell(
                        OpsjonsmodellType.TO_PLUSS_EN,
                        null,
                    ),
                ),
            ),
            ctx,
        ).shouldBeLeft(
            listOf(
                FieldError("/detaljer/opsjonsmodell/opsjonMaksVarighet", "Du må legge inn maks varighet for opsjonen"),
            ),
        )
        AvtaleValidator.validateCreateAvtale(
            gruppeAmo.copy(
                detaljer = gruppeAmo.detaljer.copy(
                    avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG,
                    opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.VALGFRI_SLUTTDATO, null),
                ),
            ),
            ctx,
        ).shouldBeRight()
    }

    test("Custom navn for opsjon må være satt hvis opsjonsmodell er ANNET") {
        AvtaleValidator.validateCreateAvtale(
            oppfolgingMedRammeAvtale.copy(
                detaljer = oppfolgingMedRammeAvtale.detaljer.copy(
                    opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.ANNET, LocalDate.now().plusYears(3)),
                ),
            ),
            ctx,
        ).shouldBeLeft(
            listOf(
                FieldError("/detaljer/opsjonsmodell/customOpsjonsmodellNavn", "Du må beskrive opsjonsmodellen"),
            ),
        )
    }

    test("avtaletype må stemme overens med tiltakstypen") {
        AvtaleValidator.validateCreateAvtale(
            forhaandsgodkjent.copy(detaljer = forhaandsgodkjent.detaljer.copy(avtaletype = Avtaletype.RAMMEAVTALE)),
            createForhandsgodkjentAvtaleContext,
        ).shouldBeLeft().shouldContain(
            FieldError(
                "/detaljer/avtaletype",
                "Rammeavtale er ikke tillatt for tiltakstype Arbeidsforberedende trening",
            ),
        )
        AvtaleValidator.validateCreateAvtale(
            forhaandsgodkjent.copy(
                detaljer = forhaandsgodkjent.detaljer.copy(
                    tiltakskode = Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
                    avtaletype = Avtaletype.AVTALE,
                ),
            ),
            ctx.copy(tiltakstype = ctx.tiltakstype.copy(navn = TiltakstypeFixtures.VTA.navn)),
        ).shouldBeLeft().shouldContain(
            FieldError(
                "/detaljer/avtaletype",
                "Avtale er ikke tillatt for tiltakstype Varig tilrettelagt arbeid i skjermet virksomhet",
            ),
        )
        AvtaleValidator.validateCreateAvtale(
            avtaleTypeAvtale.copy(detaljer = avtaleTypeAvtale.detaljer.copy(avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG)),
            ctx.copy(tiltakstype = ctx.tiltakstype.copy(navn = TiltakstypeFixtures.Oppfolging.navn)),
        ).shouldBeLeft().shouldContain(
            FieldError(
                "/detaljer/avtaletype",
                "Offentlig-offentlig samarbeid er ikke tillatt for tiltakstype Oppfølging",
            ),
        )
        AvtaleValidator.validateCreateAvtale(
            gruppeAmo.copy(detaljer = gruppeAmo.detaljer.copy(avtaletype = Avtaletype.FORHANDSGODKJENT)),
            ctx.copy(tiltakstype = ctx.tiltakstype.copy(navn = TiltakstypeFixtures.GruppeAmo.navn)),
        ).shouldBeLeft().shouldContain(
            FieldError(
                "/detaljer/avtaletype",
                "Forhåndsgodkjent er ikke tillatt for tiltakstype Arbeidsmarkedsopplæring (Gruppe)",
            ),
        )

        AvtaleValidator.validateCreateAvtale(forhaandsgodkjent, createForhandsgodkjentAvtaleContext).shouldBeRight()
        AvtaleValidator.validateCreateAvtale(oppfolgingMedRammeAvtale, ctx).shouldBeRight()
        AvtaleValidator.validateCreateAvtale(gruppeAmo, ctx).shouldBeRight()
    }

    test("SakarkivNummer må være med når avtalen er avtale eller rammeavtale") {
        AvtaleValidator.validateCreateAvtale(
            oppfolgingMedRammeAvtale.copy(
                detaljer = oppfolgingMedRammeAvtale.detaljer.copy(
                    sakarkivNummer = null,
                ),
            ),
            ctx,
        ).shouldBeLeft(
            listOf(FieldError("/detaljer/sakarkivNummer", "Du må skrive inn saksnummer til avtalesaken")),
        )

        AvtaleValidator.validateCreateAvtale(
            avtaleTypeAvtale.copy(detaljer = avtaleTypeAvtale.detaljer.copy(sakarkivNummer = null)),
            ctx,
        ).shouldBeLeft(
            listOf(FieldError("/detaljer/sakarkivNummer", "Du må skrive inn saksnummer til avtalesaken")),
        )

        AvtaleValidator.validateCreateAvtale(
            gruppeAmo.copy(
                detaljer = gruppeAmo.detaljer.copy(
                    avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG,
                    sakarkivNummer = null,
                ),
            ),
            ctx,
        ).shouldBeRight()
    }

    test("arrangørens underenheter må tilhøre hovedenhet i Brreg") {
        val fretexHovedenhet = Arrangor.Norsk.opprett(
            id = UUID.randomUUID(),
            organisasjonsnummer = Organisasjonsnummer("983982433"),
            organisasjonsform = "AS",
            navn = "FRETEX AS",
        )

        val fretexUnderenhet = Arrangor.Norsk.opprett(
            id = UUID.randomUUID(),
            organisasjonsnummer = Organisasjonsnummer("992943084"),
            organisasjonsform = "BEDR",
            overordnetEnhet = Organisasjonsnummer("983982433"),
            navn = "FRETEX AS AVD OSLO",
        )

        AvtaleValidator.validateCreateAvtale(
            avtaleRequest,
            ctx.copy(
                arrangor = Ctx.AvtaleArrangor(fretexHovedenhet, listOf(ArrangorFixtures.underenhet1)),
            ),
        ).shouldBeLeft().shouldContainExactlyInAnyOrder(
            FieldError(
                "/detaljer/arrangor/underenheter",
                "Arrangøren Underenhet 1 AS (976663934) er ikke en gyldig underenhet til hovedenheten FRETEX AS.",
            ),
        )

        AvtaleValidator.validateCreateAvtale(
            avtaleRequest,
            ctx.copy(
                arrangor = Ctx.AvtaleArrangor(fretexHovedenhet, listOf(fretexUnderenhet)),
            ),
        ).shouldBeRight()
    }

    test("arrangøren må være aktiv i Brreg") {
        AvtaleValidator.validateCreateAvtale(
            avtaleRequest,
            ctx.copy(
                arrangor = Ctx.AvtaleArrangor(
                    ArrangorFixtures.hovedenhet.registrerSlettet(LocalDate.now()),
                    listOf(ArrangorFixtures.underenhet1.registrerSlettet(LocalDate.now())),
                ),
            ),
        ).shouldBeLeft().shouldContainExactlyInAnyOrder(
            FieldError(
                "/detaljer/arrangor/hovedenhet",
                "Arrangøren Hovedenhet AS er slettet i Brønnøysundregistrene. Avtaler kan ikke opprettes for slettede bedrifter.",
            ),
            FieldError(
                "/detaljer/arrangor/underenheter",
                "Arrangøren Underenhet 1 AS er slettet i Brønnøysundregistrene. Avtaler kan ikke opprettes for slettede bedrifter.",
            ),
        )
    }

    test("utdanningsprogram er påkrevd når tiltakstypen er Gruppe Fag- og yrkesopplæring") {
        val avtaleMedEndringer = avtaleRequest.copy(
            detaljer = avtaleRequest.detaljer.copy(
                tiltakskode = TiltakstypeFixtures.GruppeFagOgYrkesopplaering.tiltakskode,
                utdanningslop = null,
            ),
        )

        AvtaleValidator.validateCreateAvtale(avtaleMedEndringer, ctx) shouldBeLeft listOf(
            FieldError("/detaljer/utdanningslop", "Du må velge et utdanningsprogram og minst ett lærefag"),
        )
    }

    test("minst én utdanning er påkrevd når tiltakstypen er Gruppe Fag- og yrkesopplæring") {
        val avtaleMedEndringer = avtaleRequest.copy(
            detaljer = avtaleRequest.detaljer.copy(
                tiltakskode = TiltakstypeFixtures.GruppeFagOgYrkesopplaering.tiltakskode,
                utdanningslop = Utdanningslop(
                    utdanningsprogram = UUID.randomUUID(),
                    utdanninger = setOf(),
                ),
            ),
        )

        AvtaleValidator.validateCreateAvtale(avtaleMedEndringer, ctx).shouldBeLeft(
            listOf(
                FieldError("/detaljer/utdanningslop", "Du må velge minst ett lærefag"),
            ),
        )
    }

    test("minst én prismodell er påkrevd") {
        AvtaleValidator.validateCreateAvtale(
            avtaleRequest.copy(prismodeller = listOf()),
            ctx,
        ).shouldBeLeft().shouldContainExactlyInAnyOrder(
            FieldError("/prismodeller", "Minst én prismodell er påkrevd"),
        )

        AvtaleValidator.validateCreateAvtale(
            forhaandsgodkjent,
            createForhandsgodkjentAvtaleContext.copy(
                systembestemtPrismodell = null,
            ),
        ).shouldBeLeft().shouldContainExactlyInAnyOrder(
            FieldError("/prismodeller", "Minst én prismodell er påkrevd"),
        )
    }

    context("prismodell") {
        fun getContext(
            tiltakstype: Tiltakstype = TiltakstypeFixtures.Oppfolging,
            avtaletype: Avtaletype = Avtaletype.AVTALE,
            gyldigTilsagnPeriode: Map<Tiltakskode, Periode> = mapOf(),
            avtaleStartDato: LocalDate = LocalDate.of(2025, 1, 1),
        ) = AvtaleValidator.ValidatePrismodellerContext(
            avtaletype = avtaletype,
            tiltakskode = tiltakstype.tiltakskode,
            tiltakstypeNavn = tiltakstype.navn,
            gyldigTilsagnPeriode = gyldigTilsagnPeriode,
            avtaleStartDato = avtaleStartDato,
            bruktePrismodeller = emptySet(),
        )

        test("må ha minst én prismodell") {
            AvtaleValidator.validatePrismodeller(
                emptyList(),
                getContext(),
            ).shouldBeLeft().shouldContain(
                FieldError(
                    "/prismodeller",
                    "Minst én prismodell er påkrevd",
                ),
            )
        }

        test("må stemme overens med tiltakstypen") {
            AvtaleValidator.validatePrismodeller(
                listOf(
                    PrismodellRequest(
                        id = UUID.randomUUID(),
                        type = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
                        valuta = Valuta.NOK,
                        prisbetingelser = null,
                        satser = emptyList(),
                        tilsagnPerDeltaker = false,
                    ),
                ),
                getContext(TiltakstypeFixtures.Oppfolging),
            ).shouldBeLeft().shouldContain(
                FieldError(
                    "/prismodeller/0/type",
                    "Fast sats per benyttet tiltaksplass per måned er ikke tillatt for tiltakstype Oppfølging",
                ),
            )
            AvtaleValidator.validatePrismodeller(
                listOf(
                    PrismodellRequest(
                        id = UUID.randomUUID(),
                        type = PrismodellType.ANNEN_AVTALT_PRIS,
                        valuta = Valuta.NOK,
                        prisbetingelser = null,
                        satser = emptyList(),
                        tilsagnPerDeltaker = false,
                    ),
                ),
                getContext(TiltakstypeFixtures.AFT, avtaletype = Avtaletype.FORHANDSGODKJENT),
            ).shouldBeLeft().shouldContain(
                FieldError("/prismodeller", "Prismodell kan ikke opprettes for forhåndsgodkjente avtaler"),
            )

            AvtaleValidator.validatePrismodeller(
                listOf(
                    PrismodellRequest(
                        id = UUID.randomUUID(),
                        type = PrismodellType.ANNEN_AVTALT_PRIS,
                        valuta = Valuta.NOK,
                        prisbetingelser = null,
                        satser = emptyList(),
                        tilsagnPerDeltaker = false,
                    ),
                ),
                getContext(TiltakstypeFixtures.Oppfolging),
            ).shouldBeRight()
        }

        test("validerer at satsene må dekke hele tilsagnsperioden som overlapper med avtalens datoer") {
            val request = listOf(
                PrismodellRequest(
                    id = UUID.randomUUID(),
                    type = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
                    valuta = Valuta.NOK,
                    prisbetingelser = null,
                    satser = listOf(
                        AvtaltSatsRequest(
                            gjelderFra = LocalDate.of(2025, 3, 1),
                            pris = 1,
                        ),
                    ),
                    tilsagnPerDeltaker = false,
                ),
            )

            AvtaleValidator.validatePrismodeller(
                request,
                getContext(
                    TiltakstypeFixtures.Oppfolging,
                    gyldigTilsagnPeriode = mapOf(Tiltakskode.OPPFOLGING to Periode.forYear(2025)),
                    avtaleStartDato = LocalDate.of(2025, 2, 1),
                ),
            ).shouldBeLeft().shouldContainExactlyInAnyOrder(
                FieldError("/prismodeller/0/satser/0/gjelderFra", "Første sats må gjelde fra 01.02.2025"),
            )

            AvtaleValidator.validatePrismodeller(
                request,
                getContext(
                    TiltakstypeFixtures.Oppfolging,
                    gyldigTilsagnPeriode = mapOf(Tiltakskode.OPPFOLGING to Periode.forYear(2025)),
                    avtaleStartDato = LocalDate.of(2024, 12, 1),
                ),
            ).shouldBeLeft().shouldContainExactlyInAnyOrder(
                FieldError("/prismodeller/0/satser/0/gjelderFra", "Første sats må gjelde fra 01.01.2025"),
            )

            AvtaleValidator.validatePrismodeller(
                listOf(
                    PrismodellRequest(
                        id = UUID.randomUUID(),
                        type = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
                        valuta = Valuta.NOK,
                        prisbetingelser = null,
                        satser = listOf(
                            AvtaltSatsRequest(
                                gjelderFra = LocalDate.of(2024, 12, 1),
                                pris = 1,
                            ),
                        ),
                        tilsagnPerDeltaker = false,
                    ),
                ),
                getContext(
                    TiltakstypeFixtures.Oppfolging,
                    gyldigTilsagnPeriode = mapOf(Tiltakskode.OPPFOLGING to Periode.forYear(2025)),
                    avtaleStartDato = LocalDate.of(2025, 1, 1),
                ),
            ).shouldBeRight()
        }

        test("validerer at satsene er gyldige") {
            val request = PrismodellRequest(
                id = UUID.randomUUID(),
                type = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
                valuta = Valuta.NOK,
                prisbetingelser = null,
                satser = listOf(),
                tilsagnPerDeltaker = false,
            )

            AvtaleValidator.validatePrismodeller(
                listOf(request),
                getContext(),
            ).shouldBeLeft().shouldContainExactlyInAnyOrder(
                FieldError("/prismodeller/0/type", "Minst én pris er påkrevd"),
            )

            AvtaleValidator.validatePrismodeller(
                listOf(
                    request.copy(
                        satser = listOf(
                            AvtaltSatsRequest(
                                gjelderFra = null,
                                pris = 1,
                            ),
                        ),
                    ),
                ),
                getContext(),
            ).shouldBeLeft().shouldContainExactlyInAnyOrder(
                FieldError("/prismodeller/0/satser/0/gjelderFra", "Gjelder fra må være satt"),
            )

            AvtaleValidator.validatePrismodeller(
                listOf(
                    request.copy(
                        satser = listOf(
                            AvtaltSatsRequest(
                                gjelderFra = LocalDate.of(2025, 1, 1),
                                pris = null,
                            ),
                        ),
                    ),
                ),
                getContext(),
            ).shouldBeLeft().shouldContainExactlyInAnyOrder(
                FieldError("/prismodeller/0/satser/0/pris", "Pris må være positiv"),
            )

            AvtaleValidator.validatePrismodeller(
                listOf(
                    request.copy(
                        satser = listOf(
                            AvtaltSatsRequest(
                                gjelderFra = LocalDate.of(2025, 1, 1),
                                pris = 0,
                            ),
                        ),
                    ),
                ),
                getContext(),
            ).shouldBeLeft().shouldContainExactlyInAnyOrder(
                FieldError("/prismodeller/0/satser/0/pris", "Pris må være positiv"),
            )

            AvtaleValidator.validatePrismodeller(
                listOf(
                    request.copy(
                        satser = listOf(
                            AvtaltSatsRequest(
                                gjelderFra = LocalDate.of(2025, 1, 1),
                                pris = 1,
                            ),
                        ),
                    ),
                ),
                getContext(),
            ).shouldBeRight()[0].satser() shouldBe listOf(
                AvtaltSats(LocalDate.of(2025, 1, 1), 1.NOK),
            )
        }

        test("tillater forskjellig valuta på forskjellige prismodeller") {
            AvtaleValidator.validatePrismodeller(
                listOf(
                    PrismodellRequest(
                        id = UUID.randomUUID(),
                        type = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
                        valuta = Valuta.NOK,
                        prisbetingelser = null,
                        satser = listOf(
                            AvtaltSatsRequest(gjelderFra = LocalDate.of(2025, 1, 1), pris = 1),
                            AvtaltSatsRequest(gjelderFra = LocalDate.of(2025, 2, 1), pris = 2),
                        ),
                        tilsagnPerDeltaker = false,
                    ),
                    PrismodellRequest(
                        id = UUID.randomUUID(),
                        type = PrismodellType.AVTALT_PRIS_PER_UKESVERK,
                        valuta = Valuta.SEK,
                        prisbetingelser = null,
                        satser = listOf(
                            AvtaltSatsRequest(gjelderFra = LocalDate.of(2025, 1, 1), pris = 1),
                            AvtaltSatsRequest(gjelderFra = LocalDate.of(2025, 2, 1), pris = 2),
                        ),
                        tilsagnPerDeltaker = false,
                    ),
                ),
                getContext(),
            ).shouldBeRight()
        }

        test("tillater ikke flere satser som starter på samme dato") {
            AvtaleValidator.validatePrismodeller(
                listOf(
                    PrismodellRequest(
                        id = UUID.randomUUID(),
                        type = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
                        valuta = Valuta.NOK,
                        prisbetingelser = null,
                        satser = listOf(
                            AvtaltSatsRequest(gjelderFra = LocalDate.of(2025, 1, 1), pris = 1),
                            AvtaltSatsRequest(gjelderFra = LocalDate.of(2025, 1, 1), pris = 1),
                            AvtaltSatsRequest(gjelderFra = LocalDate.of(2025, 2, 1), pris = 2),
                        ),
                        tilsagnPerDeltaker = false,
                    ),
                ),
                getContext(),
            ).shouldBeLeft().shouldContainExactlyInAnyOrder(
                FieldError("/prismodeller/0/satser/0/gjelderFra", "Gjelder fra må være unik per rad"),
                FieldError("/prismodeller/0/satser/1/gjelderFra", "Gjelder fra må være unik per rad"),
            )
        }

        test("sorterer satsene etter gjelderFra-dato") {
            AvtaleValidator.validatePrismodeller(
                listOf(
                    PrismodellRequest(
                        id = UUID.randomUUID(),
                        type = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
                        valuta = Valuta.NOK,
                        prisbetingelser = null,
                        satser = listOf(
                            AvtaltSatsRequest(gjelderFra = LocalDate.of(2025, 3, 1), pris = 3),
                            AvtaltSatsRequest(gjelderFra = LocalDate.of(2025, 1, 1), pris = 1),
                            AvtaltSatsRequest(gjelderFra = LocalDate.of(2025, 2, 1), pris = 2),
                        ),
                        tilsagnPerDeltaker = false,
                    ),
                ),
                getContext(),
            ).shouldBeRight()[0].satser() shouldBe listOf(
                AvtaltSats(LocalDate.of(2025, 1, 1), 1.NOK),
                AvtaltSats(LocalDate.of(2025, 2, 1), 2.NOK),
                AvtaltSats(LocalDate.of(2025, 3, 1), 3.NOK),
            )
        }
    }

    context("når avtalen allerede eksisterer") {
        val previous = Ctx.Avtale(
            status = AvtaleStatusType.AKTIV,
            opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.VALGFRI_SLUTTDATO, LocalDate.now().plusYears(4)),
            opsjonerRegistrert = emptyList(),
            avtaletype = Avtaletype.AVTALE,
            tiltakskode = Tiltakskode.OPPFOLGING,
            gjennomforinger = listOf(
                Ctx.Gjennomforing(
                    arrangor = Gjennomforing.ArrangorUnderenhet(
                        id = ArrangorFixtures.underenhet1.id,
                        organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                        navn = ArrangorFixtures.underenhet1.navn,
                        slettet = false,
                    ),
                    startDato = LocalDate.now(),
                    utdanningslop = null,
                    status = GjennomforingStatusType.GJENNOMFORES,
                    prismodellId = prismodell.id,
                ),
            ),
            prismodeller = listOf(prismodell),
        )

        test("Skal ikke kunne endre tiltakstype") {
            AvtaleValidator.validateUpdateDetaljer(
                gruppeAmo.detaljer,
                ctx.copy(previous = previous),
            ) shouldBeLeft listOf(
                FieldError("/detaljer/tiltakskode", "Tiltakstype kan ikke endres etter at avtalen er opprettet"),
            )
        }

        test("Skal ikke kunne endre opsjonsmodell eller avtaletype når opsjon er registrert") {
            val startDato = LocalDate.of(2024, 5, 7)
            val avtale = gruppeAmo.copy(
                detaljer = gruppeAmo.detaljer.copy(
                    avtaletype = Avtaletype.AVTALE,
                    opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN, startDato.plusYears(3)),
                ),
            )

            AvtaleValidator.validateUpdateDetaljer(
                avtale.detaljer,
                ctx.copy(
                    previous = previous.copy(
                        tiltakskode = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
                        avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG,
                        opsjonerRegistrert = listOf(
                            Avtale.OpsjonLogg(
                                id = UUID.randomUUID(),
                                createdAt = LocalDateTime.now(),
                                sluttDato = LocalDate.now(),
                                forrigeSluttDato = LocalDate.now(),
                                status = OpsjonLoggStatus.OPSJON_UTLOST,
                            ),
                        ),
                        gjennomforinger = emptyList(),
                    ),
                ),
            ) shouldBeLeft listOf(
                FieldError("/detaljer/avtaletype", "Du kan ikke endre avtaletype når opsjoner er registrert"),
                FieldError(
                    "/detaljer/opsjonsmodell/type",
                    "Du kan ikke endre opsjonsmodell når opsjoner er registrert",
                ),
            )
        }

        context("når avtalen har gjennomføringer") {
            test("skal validere at data samsvarer med avtalens gjennomføringer") {
                val startDatoForGjennomforing = LocalDate.now()

                val request = oppfolgingMedRammeAvtale.copy(
                    detaljer = oppfolgingMedRammeAvtale.detaljer.copy(
                        tiltakskode = Tiltakskode.ARBEIDSRETTET_REHABILITERING,
                        startDato = startDatoForGjennomforing.plusDays(1),
                        arrangor = DetaljerRequest.Arrangor(
                            hovedenhet = ArrangorFixtures.hovedenhet.organisasjonsnummer,
                            underenheter = listOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                            kontaktpersoner = emptyList(),
                        ),
                    ),
                )

                val formatertDato = startDatoForGjennomforing.formaterDatoTilEuropeiskDatoformat()

                AvtaleValidator.validateUpdateDetaljer(
                    request.detaljer,
                    ctx.copy(
                        previous = previous.copy(
                            gjennomforinger = listOf(
                                Ctx.Gjennomforing(
                                    arrangor = Gjennomforing.ArrangorUnderenhet(
                                        id = ArrangorFixtures.underenhet2.id,
                                        organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                                        navn = ArrangorFixtures.underenhet2.navn,
                                        slettet = false,
                                    ),
                                    startDato = LocalDate.now(),
                                    utdanningslop = null,
                                    status = GjennomforingStatusType.GJENNOMFORES,
                                    prismodellId = request.prismodeller.first().id,
                                ),
                            ),
                        ),
                    ),
                ).shouldBeLeft() shouldContainExactlyInAnyOrder listOf(
                    FieldError(
                        "/detaljer/tiltakskode",
                        "Tiltakstype kan ikke endres etter at avtalen er opprettet",
                    ),
                    FieldError(
                        "/detaljer/arrangor/underenheter",
                        "Arrangøren Underenhet 2 AS er i bruk på en av avtalens gjennomføringer, men mangler blant tiltaksarrangørens underenheter",
                    ),
                    FieldError(
                        "/detaljer/startDato",
                        "Startdato kan ikke være etter startdatoen til gjennomføringer koblet til avtalen. Minst en gjennomføring har startdato: $formatertDato",
                    ),
                )
            }

            test("skal godta at gjennomføring har andre Nav-enheter enn avtalen") {
                val request = avtaleRequest.copy(
                    veilederinformasjon = VeilederinfoRequest(
                        navEnheter = listOf(
                            NavEnhetFixtures.Oslo.enhetsnummer,
                            NavEnhetFixtures.Sagene.enhetsnummer,
                        ),
                        beskrivelse = null,
                        faneinnhold = null,
                    ),
                )

                AvtaleValidator.validateUpdateDetaljer(request.detaljer, ctx.copy(previous = previous)).shouldBeRight()
            }

            test("kan ikke fjerne alle prismodeller på avtalen") {
                AvtaleValidator.validatePrismodeller(
                    emptyList(),
                    AvtaleValidator.ValidatePrismodellerContext(
                        avtaletype = Avtaletype.AVTALE,
                        tiltakskode = previous.tiltakskode,
                        tiltakstypeNavn = ctx.tiltakstype.navn,
                        gyldigTilsagnPeriode = emptyMap(),
                        avtaleStartDato = LocalDate.now().minusDays(1),
                        bruktePrismodeller = previous.gjennomforinger.map { it.prismodellId }.toSet(),
                    ),
                ).shouldBeLeft() shouldContain
                    FieldError(
                        "/prismodeller",
                        "Minst én prismodell er påkrevd",
                    )
            }

            test("kan ikke fjerne prismodell som er i bruk av en gjennomføring") {
                val prismodellRequest = listOf(
                    PrismodellRequest(
                        id = UUID.randomUUID(),
                        type = PrismodellType.ANNEN_AVTALT_PRIS,
                        valuta = Valuta.NOK,
                        satser = emptyList(),
                        prisbetingelser = null,
                        tilsagnPerDeltaker = false,
                    ),
                )

                AvtaleValidator.validatePrismodeller(
                    prismodellRequest,
                    AvtaleValidator.ValidatePrismodellerContext(
                        avtaletype = Avtaletype.AVTALE,
                        tiltakskode = previous.tiltakskode,
                        tiltakstypeNavn = ctx.tiltakstype.navn,
                        gyldigTilsagnPeriode = emptyMap(),
                        avtaleStartDato = LocalDate.now().minusDays(1),
                        bruktePrismodeller = previous.gjennomforinger.map { it.prismodellId }
                            .toSet(),
                    ),
                ).shouldBeLeft() shouldContain
                    FieldError(
                        "/prismodeller",
                        "Prismodell kan ikke fjernes fordi en eller flere gjennomføringer er koblet til prismodellen",
                    )
            }
        }

        test("tillater ikke slettede administratorer") {
            AvtaleValidator.validateUpdateDetaljer(
                avtaleRequest.detaljer.copy(tiltakskode = previous.tiltakskode),
                ctx.copy(
                    previous = previous,
                    administratorer = listOf(
                        NavAnsattFixture.DonaldDuck.skalSlettes(LocalDate.now()),
                    ),
                ),
            ).shouldBeLeft().shouldContainExactlyInAnyOrder(
                FieldError("/detaljer/administratorer", "Nav identer DD1 er slettet og må fjernes"),
            )
        }
    }

    context("endring av status") {
        val previous = Ctx.Avtale(
            status = AvtaleStatusType.AKTIV,
            opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.VALGFRI_SLUTTDATO, LocalDate.now().plusYears(4)),
            opsjonerRegistrert = emptyList(),
            avtaletype = Avtaletype.AVTALE,
            tiltakskode = Tiltakskode.OPPFOLGING,
            gjennomforinger = listOf(),
            prismodeller = listOf(prismodell),
        )

        test("status blir UTKAST når avtalen lagres uten en arrangør") {
            AvtaleValidator.validateCreateAvtale(
                avtaleRequest.copy(detaljer = avtaleRequest.detaljer.copy(arrangor = null)),
                ctx,
            ).shouldBeRight().should {
                it.detaljerDbo.status shouldBe AvtaleStatusType.UTKAST
            }
        }

        test("status blir AKTIV når avtalen lagres med sluttdato i fremtiden") {
            AvtaleValidator.validateCreateAvtale(avtaleRequest, ctx).shouldBeRight().should {
                it.detaljerDbo.status shouldBe AvtaleStatusType.AKTIV
            }
        }

        test("status blir AVSLUTTET når avtalen lagres med en sluttdato som er passert") {
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)

            val request = avtaleRequest.copy(
                detaljer = avtaleRequest.detaljer.copy(
                    startDato = yesterday,
                    sluttDato = yesterday,
                ),
            )

            AvtaleValidator.validateUpdateDetaljer(request.detaljer, ctx.copy(previous = previous)).shouldBeRight()
                .should {
                    it.status shouldBe AvtaleStatusType.AVSLUTTET
                }
        }

        test("status forblir AVBRUTT på en avtale som allerede er AVBRUTT") {
            val today = LocalDate.now()

            val avtale = AvtaleFixtures.oppfolging

            val request = avtaleRequest.copy(
                id = avtale.id,
                detaljer = avtaleRequest.detaljer.copy(
                    startDato = today,
                    sluttDato = today,
                ),
            )

            AvtaleValidator.validateUpdateDetaljer(
                request.detaljer,
                ctx.copy(
                    previous = previous.copy(status = AvtaleStatusType.AVBRUTT),
                ),
            ).shouldBeRight().should {
                it.status shouldBe AvtaleStatusType.AVBRUTT
            }
        }
    }

    context("amo kategorisering") {
        test("amoKategorisering er påkrevd hvis gruppe amo") {
            AvtaleValidator.validateCreateAvtale(
                gruppeAmo.copy(detaljer = gruppeAmo.detaljer.copy(amoKategorisering = null)),
                ctx,
            ).shouldBeLeft(
                listOf(FieldError("/detaljer/amoKategorisering/kurstype", "Du må velge en kurstype")),
            )
        }

        test("bransje er påkrevd hvis bransje kurstype") {
            AvtaleValidator.validateCreateAvtale(
                gruppeAmo.copy(
                    detaljer = gruppeAmo.detaljer.copy(
                        tiltakskode = Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
                        amoKategorisering = AmoKategoriseringRequest(
                            kurstype = Kurstype.Kode.BRANSJE_OG_YRKESRETTET,
                        ),
                    ),
                ),
                ctx,
            ).shouldBeLeft(
                listOf(FieldError("/detaljer/amoKategorisering/bransje", "Du må velge en bransje")),
            )
        }

        test("amoKategorisering blir mappet til riktig type") {
            AvtaleValidator.validateCreateAvtale(
                gruppeAmo,
                ctx,
            ).shouldBeRight().detaljerDbo.opplaringKategorisering
                .shouldNotBeNull {
                    kurstype shouldBe KurstypeFixtures.studiespesialisering.id
                }

            AvtaleValidator.validateCreateAvtale(
                gruppeAmo.copy(
                    detaljer = gruppeAmo.detaljer.copy(
                        tiltakskode = Tiltakskode.STUDIESPESIALISERING,
                        amoKategorisering = null,
                    ),
                ),
                ctx,
            ).shouldBeRight().detaljerDbo.opplaringKategorisering
                .shouldNotBeNull {
                    kurstype shouldBe KurstypeFixtures.studiespesialisering.id
                }

            AvtaleValidator.validateCreateAvtale(
                gruppeAmo.copy(
                    detaljer = gruppeAmo.detaljer.copy(
                        tiltakskode = Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
                        amoKategorisering = AmoKategoriseringRequest(
                            kurstype = Kurstype.Kode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE,

                        ),
                    ),
                ),
                ctx,
            ).shouldBeRight().detaljerDbo.opplaringKategorisering.shouldNotBeNull {
                kurstype shouldBe KurstypeFixtures.fov.id
            }

            AvtaleValidator.validateCreateAvtale(
                gruppeAmo.copy(
                    detaljer = gruppeAmo.detaljer.copy(
                        tiltakskode = Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
                        amoKategorisering = AmoKategoriseringRequest(
                            kurstype = Kurstype.Kode.GRUNNLEGGENDE_FERDIGHETER,
                            innholdElementer = listOf(
                                InnholdElement.Kode.GRUNNLEGGENDE_FERDIGHETER,
                            ),
                        ),
                    ),
                ),
                ctx,
            ).shouldBeRight().detaljerDbo.opplaringKategorisering.shouldNotBeNull {
                this.kurstype shouldBe KurstypeFixtures.grunnleggendeFerdigheter.id
                this.innholdElementer shouldContainExactly setOf(
                    InnholdElementFixtures.grunnleggendeFerdigheter.id,
                )
            }

            AvtaleValidator.validateCreateAvtale(
                gruppeAmo.copy(
                    detaljer = gruppeAmo.detaljer.copy(
                        amoKategorisering = AmoKategoriseringRequest(
                            kurstype = Kurstype.Kode.BRANSJE_OG_YRKESRETTET,
                            bransje = Bransje.Kode.KONTORARBEID,
                            forerkort = listOf(
                                ForerkortKlasse.Kode.A,
                                ForerkortKlasse.Kode.B,
                            ),
                            sertifiseringer = listOf(
                                Sertifisering(
                                    konseptId = 123,
                                    label = "label",
                                ),
                            ),
                        ),
                    ),
                ),
                ctx,
            ).shouldBeRight().detaljerDbo.opplaringKategorisering.shouldNotBeNull {
                kurstype shouldBe KurstypeFixtures.bransjeOgYrkesrettet.id
                bransje shouldBe BransjeFixtures.kontorarbeid.id
                forerkort shouldContainExactlyInAnyOrder listOf(
                    ForerkortFixtures.A.id,
                    ForerkortFixtures.B.id,
                )
                sertifiseringer shouldContainExactly listOf(
                    Sertifisering(
                        konseptId = 123,
                        label = "label",
                    ),
                )
            }
        }
    }
    context("rammedetaljer") {
        test("totalramme må være positiv") {
            // TODO
            AvtaleValidator.validateCreateAvtale(
                gruppeAmo.copy(detaljer = gruppeAmo.detaljer.copy(amoKategorisering = null)),
                ctx,
            ).shouldBeLeft(
                listOf(FieldError("/detaljer/amoKategorisering/kurstype", "Du må velge en kurstype")),
            )
        }
    }
})
