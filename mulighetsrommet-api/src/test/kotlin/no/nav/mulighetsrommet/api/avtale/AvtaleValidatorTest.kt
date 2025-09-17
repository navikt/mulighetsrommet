package no.nav.mulighetsrommet.api.avtale

import arrow.core.left
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.avtale.api.AvtaleRequest
import no.nav.mulighetsrommet.api.avtale.mapper.AvtaleDboMapper
import no.nav.mulighetsrommet.api.avtale.model.*
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregError
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import java.time.LocalDate
import java.util.*

class AvtaleValidatorTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        navEnheter = listOf(
            NavEnhetFixtures.Oslo,
            NavEnhetFixtures.Sagene,
            NavEnhetFixtures.Innlandet,
            NavEnhetFixtures.Gjovik,
        ),
        ansatte = listOf(NavAnsattFixture.DonaldDuck),
        arrangorer = listOf(
            ArrangorFixtures.hovedenhet,
            ArrangorFixtures.underenhet1,
            ArrangorFixtures.underenhet2,
            ArrangorFixtures.Fretex.hovedenhet,
            ArrangorFixtures.Fretex.underenhet1,
        ),
        avtaler = listOf(),
    )

    val avtaleRequest = AvtaleRequest(
        id = UUID.randomUUID(),
        navn = "Avtale",
        tiltakskode = TiltakstypeFixtures.Oppfolging.tiltakskode!!,
        arrangor = AvtaleRequest.Arrangor(
            hovedenhet = ArrangorFixtures.hovedenhet.organisasjonsnummer,
            underenheter = listOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
            kontaktpersoner = emptyList(),
        ),
        avtalenummer = "123456",
        sakarkivNummer = SakarkivNummer("24/1234"),
        startDato = LocalDate.now().minusDays(1),
        sluttDato = LocalDate.now().plusMonths(1),
        administratorer = listOf(NavAnsattFixture.DonaldDuck.navIdent),
        avtaletype = Avtaletype.RAMMEAVTALE,
        navEnheter = listOf(NavEnhetNummer("0400"), NavEnhetNummer("0502")),
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = emptyList(),
        personvernBekreftet = false,
        amoKategorisering = null,
        opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN, LocalDate.now().plusYears(3)),
        utdanningslop = null,
        prismodell = PrismodellRequest(
            type = PrismodellType.ANNEN_AVTALT_PRIS,
            prisbetingelser = null,
            satser = listOf(),
        ),
    )
    val gruppeAmo = AvtaleDboMapper.toAvtaleRequest(
        AvtaleFixtures.gruppeAmo,
        null,
        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
    )
    val forhaandsgodkjent = AvtaleDboMapper.toAvtaleRequest(
        AvtaleFixtures.AFT,
        null,
        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
    )
    val avtaleTypeAvtale = AvtaleDboMapper.toAvtaleRequest(
        AvtaleFixtures.oppfolgingMedAvtale,
        null,
        Tiltakskode.OPPFOLGING,
    )
    val oppfolgingMedRammeAvtale = AvtaleDboMapper.toAvtaleRequest(
        AvtaleFixtures.oppfolging,
        null,
        Tiltakskode.OPPFOLGING,
    )

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    fun createValidator(
        arrangorService: ArrangorService = ArrangorService(database.db, brregClient = mockk(relaxed = true)),
    ) = AvtaleValidator(
        db = database.db,
        tiltakstyper = TiltakstypeService(database.db),
        arrangorService = arrangorService,
        navEnheterService = NavEnhetService(database.db),
    )

    test("should accumulate errors when request has multiple issues") {
        val validator = createValidator()

        val request = avtaleRequest.copy(
            startDato = LocalDate.of(2023, 1, 1),
            sluttDato = LocalDate.of(2020, 1, 1),
            navEnheter = emptyList(),
            arrangor = AvtaleRequest.Arrangor(
                hovedenhet = ArrangorFixtures.hovedenhet.organisasjonsnummer,
                underenheter = emptyList(),
                kontaktpersoner = emptyList(),
            ),
        )

        validator.validate(request, null).shouldBeLeft().shouldContainAll(
            listOf(
                FieldError("/startDato", "Startdato må være før sluttdato"),
                FieldError("/navRegioner", "Du må velge minst én Nav-region"),
                FieldError("/arrangorUnderenheter", "Du må velge minst én underenhet for tiltaksarrangør"),
            ),
        )
    }

    test("Avtalenavn må være minst 5 tegn når avtalen er opprettet i Admin-flate") {
        val validator = createValidator()

        val request = avtaleRequest.copy(navn = "Avt")

        validator.validate(request, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(
                FieldError("/navn", "Avtalenavn må være minst 5 tegn langt"),
            ),
        )
    }

    test("Avtalens startdato må være før eller lik som sluttdato") {
        val validator = createValidator()

        val dagensDato = LocalDate.now()
        val request = avtaleRequest.copy(startDato = dagensDato, sluttDato = dagensDato)

        validator.validate(request, null).shouldBeRight()

        val request2 = avtaleRequest.copy(startDato = dagensDato.plusDays(5), sluttDato = dagensDato)

        validator.validate(request2, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(FieldError("/startDato", "Startdato må være før sluttdato")),
        )
    }

    test("Avtalens sluttdato være lik eller etter startdato") {
        val validator = createValidator()

        val dagensDato = LocalDate.now()
        val request = avtaleRequest.copy(startDato = dagensDato, sluttDato = dagensDato.minusDays(5))

        validator.validate(request, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(FieldError("/startDato", "Startdato må være før sluttdato")),
        )

        val request2 = avtaleRequest.copy(startDato = dagensDato, sluttDato = dagensDato)
        validator.validate(request2, null).shouldBeRight()
    }

    test("skal validere at Nav-fylke og Nav-enheter er påkrevd") {
        val validator = createValidator()

        val request = avtaleRequest.copy(
            navEnheter = listOf(),
        )

        validator.validate(request, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(
                FieldError("/navRegioner", "Du må velge minst én Nav-region"),
                FieldError("/navKontorer", "Du må velge minst én Nav-enhet"),
            ),
        )
    }

    test("sluttDato er påkrevd hvis ikke forhåndsgodkjent") {
        val forhaandsgodkjent1 = forhaandsgodkjent.copy(
            sluttDato = null,
            opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.VALGFRI_SLUTTDATO, null),
        )
        val offentligOffentlig = gruppeAmo.copy(
            sluttDato = null,
            amoKategorisering = AmoKategorisering.Studiespesialisering,
        )

        val validator = createValidator()

        validator.validate(forhaandsgodkjent1, null).shouldBeRight()
        validator.validate(oppfolgingMedRammeAvtale.copy(sluttDato = null), null).shouldBeLeft(
            listOf(FieldError("/sluttDato", "Du må legge inn sluttdato for avtalen")),
        )
        validator.validate(avtaleTypeAvtale.copy(sluttDato = null), null).shouldBeLeft(
            listOf(FieldError("/sluttDato", "Du må legge inn sluttdato for avtalen")),
        )
        validator.validate(offentligOffentlig.copy(sluttDato = null), null).shouldBeLeft(
            listOf(FieldError("/sluttDato", "Du må legge inn sluttdato for avtalen")),
        )
    }

    test("amoKategorisering er påkrevd hvis gruppe amo") {
        val validator = createValidator()
        validator.validate(
            gruppeAmo.copy(amoKategorisering = null),
            null,
        ).shouldBeLeft(
            listOf(FieldError("/amoKategorisering.kurstype", "Du må velge en kurstype")),
        )
    }

    test("Opsjonsmodell må være VALGFRI_SLUTTDATO når avtale er forhåndsgodkjent") {
        val validator = createValidator()

        validator.validate(
            forhaandsgodkjent.copy(opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.VALGFRI_SLUTTDATO, null)),
            null,
        ).shouldBeRight()
        validator.validate(
            forhaandsgodkjent.copy(opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN, null)),
            null,
        ).shouldBeLeft(
            listOf(
                FieldError(
                    "/opsjonsmodell",
                    "Du må velge opsjonsmodell med valgfri sluttdato når avtalen er forhåndsgodkjent",
                ),
            ),
        )
    }
    test("Opsjonsmodell må Opsjonsdata må være satt når avtaletypen ikke er forhåndsgodkjent") {
        val validator = createValidator()

        validator.validate(
            avtaleTypeAvtale.copy(opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN, null)),
            null,
        ).shouldBeLeft(
            listOf(
                FieldError("/opsjonsmodell/opsjonMaksVarighet", "Du må legge inn maks varighet for opsjonen"),
            ),
        )
        validator.validate(
            gruppeAmo.copy(
                avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG,
                opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.VALGFRI_SLUTTDATO, null),
            ),
            null,
        ).shouldBeRight()
    }

    test("Custom navn for opsjon må være satt hvis opsjonsmodell er ANNET") {
        val validator = createValidator()

        validator.validate(
            oppfolgingMedRammeAvtale.copy(
                opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.ANNET, LocalDate.now().plusYears(3)),
            ),
            null,
        ).shouldBeLeft(
            listOf(
                FieldError("/opsjonsmodell/customOpsjonsmodellNavn", "Du må beskrive opsjonsmodellen"),
            ),
        )
    }

    test("avtaletype må stemme overens med tiltakstypen") {
        val validator = createValidator()

        forAll(
            row(
                forhaandsgodkjent.copy(avtaletype = Avtaletype.RAMMEAVTALE),
                FieldError(
                    "/avtaletype",
                    "Rammeavtale er ikke tillatt for tiltakstype Arbeidsforberedende trening",
                ),
            ),
            row(
                forhaandsgodkjent.copy(
                    tiltakskode = Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
                    avtaletype = Avtaletype.AVTALE,
                ),
                FieldError(
                    "/avtaletype",
                    "Avtale er ikke tillatt for tiltakstype Varig tilrettelagt arbeid i skjermet virksomhet",
                ),
            ),
            row(
                avtaleTypeAvtale.copy(avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG),
                FieldError("/avtaletype", "Offentlig-offentlig samarbeid er ikke tillatt for tiltakstype Oppfølging"),
            ),
            row(
                gruppeAmo.copy(avtaletype = Avtaletype.FORHANDSGODKJENT),
                FieldError(
                    "/avtaletype",
                    "Forhåndsgodkjent er ikke tillatt for tiltakstype Arbeidsmarkedsopplæring (Gruppe)",
                ),
            ),
        ) { avtale, expectedError ->
            validator.validate(avtale, null).shouldBeLeft().shouldContain(expectedError)
        }

        forAll(
            row(forhaandsgodkjent),
            row(oppfolgingMedRammeAvtale),
            row(gruppeAmo),
        ) { avtale ->
            validator.validate(avtale, null).shouldBeRight()
        }
    }

    test("SakarkivNummer må være med når avtalen er avtale eller rammeavtale") {
        val validator = createValidator()

        validator.validate(oppfolgingMedRammeAvtale.copy(sakarkivNummer = null), null).shouldBeLeft(
            listOf(FieldError("/sakarkivNummer", "Du må skrive inn saksnummer til avtalesaken")),
        )

        validator.validate(avtaleTypeAvtale.copy(sakarkivNummer = null), null).shouldBeLeft(
            listOf(FieldError("/sakarkivNummer", "Du må skrive inn saksnummer til avtalesaken")),
        )

        validator.validate(
            gruppeAmo.copy(
                avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG,
                sakarkivNummer = null,
                amoKategorisering = AmoKategorisering.Studiespesialisering,
            ),
            null,
        ).shouldBeRight()
    }

    test("arrangørens underenheter må tilhøre hovedenhet i Brreg") {
        val validator = createValidator()

        val avtale1 = avtaleTypeAvtale.copy(
            arrangor = AvtaleRequest.Arrangor(
                hovedenhet = ArrangorFixtures.Fretex.hovedenhet.organisasjonsnummer,
                underenheter = listOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                kontaktpersoner = emptyList(),
            ),
        )

        validator.validate(avtale1, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            FieldError(
                "/arrangorUnderenheter",
                "Arrangøren Underenhet 1 AS er ikke en gyldig underenhet til hovedenheten FRETEX AS.",
            ),
        )

        val avtale2 = avtale1.copy(
            arrangor = AvtaleRequest.Arrangor(
                hovedenhet = ArrangorFixtures.Fretex.hovedenhet.organisasjonsnummer,
                underenheter = listOf(ArrangorFixtures.Fretex.underenhet1.organisasjonsnummer),
                kontaktpersoner = emptyList(),
            ),
        )
        validator.validate(avtale2, null).shouldBeRight()
    }

    test("arrangøren må være aktiv i Brreg") {
        database.run {
            queries.arrangor.upsert(ArrangorFixtures.Fretex.hovedenhet.copy(slettetDato = LocalDate.of(2024, 1, 1)))
            queries.arrangor.upsert(ArrangorFixtures.Fretex.underenhet1.copy(slettetDato = LocalDate.of(2024, 1, 1)))
        }

        val avtale1 = avtaleTypeAvtale.copy(
            arrangor = AvtaleRequest.Arrangor(
                hovedenhet = ArrangorFixtures.Fretex.hovedenhet.organisasjonsnummer,
                underenheter = listOf(ArrangorFixtures.Fretex.underenhet1.organisasjonsnummer),
                kontaktpersoner = emptyList(),
            ),
        )

        createValidator().validate(avtale1, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            FieldError(
                "/arrangorHovedenhet",
                "Arrangøren FRETEX AS er slettet i Brønnøysundregistrene. Avtaler kan ikke opprettes for slettede bedrifter.",
            ),
            FieldError(
                "/arrangorUnderenheter",
                "Arrangøren FRETEX AS AVD OSLO er slettet i Brønnøysundregistrene. Avtaler kan ikke opprettes for slettede bedrifter.",
            ),
        )
    }

    test("utdanningsprogram er påkrevd når tiltakstypen er Gruppe Fag- og yrkesopplæring") {
        val avtaleMedEndringer = avtaleRequest.copy(
            tiltakskode = TiltakstypeFixtures.GruppeFagOgYrkesopplaering.tiltakskode!!,
            utdanningslop = null,
        )

        val validator = createValidator()

        validator.validate(avtaleMedEndringer, null) shouldBeLeft listOf(
            FieldError("/utdanningslop", "Du må velge et utdanningsprogram og minst ett lærefag"),
        )
    }

    test("minst én utdanning er påkrevd når tiltakstypen er Gruppe Fag- og yrkesopplæring") {
        val avtaleMedEndringer = avtaleRequest.copy(
            tiltakskode = TiltakstypeFixtures.GruppeFagOgYrkesopplaering.tiltakskode!!,
            utdanningslop = UtdanningslopDbo(
                utdanningsprogram = UUID.randomUUID(),
                utdanninger = emptyList(),
            ),
        )

        val validator = createValidator()

        validator.validate(avtaleMedEndringer, null).shouldBeLeft(
            listOf(
                FieldError("/utdanningslop", "Du må velge minst ett lærefag"),
            ),
        )
    }

    context("prismodell") {
        test("prismodell må stemme overens med tiltakstypen") {
            val validator = createValidator()

            forAll(
                row(
                    avtaleRequest.copy(
                        tiltakskode = Tiltakskode.OPPFOLGING,
                        prismodell = PrismodellRequest(
                            type = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
                            prisbetingelser = null,
                            satser = emptyList(),
                        ),
                    ),
                    FieldError(
                        "/prismodell",
                        "Fast sats per tiltaksplass per måned er ikke tillatt for tiltakstype Oppfølging",
                    ),
                ),
                row(
                    forhaandsgodkjent.copy(
                        prismodell = PrismodellRequest(
                            type = PrismodellType.ANNEN_AVTALT_PRIS,
                            prisbetingelser = null,
                            satser = emptyList(),
                        ),
                    ),
                    FieldError(
                        "/prismodell",
                        "Annen avtalt pris er ikke tillatt for tiltakstype Arbeidsforberedende trening",
                    ),
                ),
                row(
                    gruppeAmo.copy(
                        prismodell = PrismodellRequest(
                            type = PrismodellType.AVTALT_PRIS_PER_UKESVERK,
                            prisbetingelser = null,
                            satser = emptyList(),
                        ),
                    ),
                    FieldError(
                        "/prismodell",
                        "Avtalt ukespris per tiltaksplass er ikke tillatt for tiltakstype Arbeidsmarkedsopplæring (Gruppe)",
                    ),
                ),
            ) { avtale, expectedError ->
                validator.validate(avtale, null).shouldBeLeft().shouldContain(expectedError)
            }

            val fri = avtaleRequest.copy(
                prismodell = PrismodellRequest(
                    type = PrismodellType.ANNEN_AVTALT_PRIS,
                    prisbetingelser = null,
                    satser = emptyList(),
                ),
            )
            validator.validate(fri, null).shouldBeRight()
        }
    }

    context("når avtalen allerede eksisterer") {
        test("Skal ikke kunne endre opsjonsmodell eller avtaletype når opsjon er registrert") {
            val startDato = LocalDate.of(2024, 5, 7)
            database.run {
                queries.avtale.upsert(
                    AvtaleFixtures.gruppeAmo.copy(
                        startDato = startDato,
                        sluttDato = startDato.plusYears(1),
                        opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN_PLUSS_EN, startDato.plusYears(4)),
                    ),
                )
                queries.opsjoner.insert(
                    OpsjonLoggDbo(
                        avtaleId = AvtaleFixtures.gruppeAmo.id,
                        sluttDato = avtaleRequest.sluttDato?.plusYears(1),
                        forrigeSluttDato = avtaleRequest.sluttDato!!,
                        status = OpsjonLoggStatus.OPSJON_UTLOST,
                        registrertAv = NavIdent("M123456"),
                    ),
                )
            }

            val previous = database.run { queries.avtale.get(AvtaleFixtures.gruppeAmo.id) }
            val avtale = gruppeAmo.copy(
                avtaletype = Avtaletype.AVTALE,
                opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN, startDato.plusYears(3)),
            )

            createValidator().validate(avtale, previous) shouldBeLeft listOf(
                FieldError("/avtaletype", "Du kan ikke endre avtaletype når opsjoner er registrert"),
                FieldError("/opsjonsmodell", "Du kan ikke endre opsjonsmodell når opsjoner er registrert"),
            )
        }

        context("når avtalen har gjennomføringer") {
            val startDatoForGjennomforing = LocalDate.now()

            val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(
                administratorer = emptyList(),
                startDato = startDatoForGjennomforing,
            )

            test("skal validere at data samsvarer med avtalens gjennomføringer") {
                MulighetsrommetTestDomain(
                    avtaler = listOf(AvtaleFixtures.oppfolging),
                    gjennomforinger = listOf(gjennomforing.copy(arrangorId = ArrangorFixtures.underenhet2.id)),
                ).initialize(database.db)

                val request = oppfolgingMedRammeAvtale.copy(
                    tiltakskode = Tiltakskode.ARBEIDSRETTET_REHABILITERING,
                    startDato = startDatoForGjennomforing.plusDays(1),
                    arrangor = AvtaleRequest.Arrangor(
                        hovedenhet = ArrangorFixtures.hovedenhet.organisasjonsnummer,
                        underenheter = listOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                        kontaktpersoner = emptyList(),
                    ),
                )

                val previous = database.run { queries.avtale.get(oppfolgingMedRammeAvtale.id) }
                val formatertDato = startDatoForGjennomforing.formaterDatoTilEuropeiskDatoformat()

                createValidator().validate(request, previous).shouldBeLeft() shouldContainExactlyInAnyOrder listOf(
                    FieldError(
                        "/tiltakskode",
                        "Tiltakstype kan ikke endres fordi det finnes gjennomføringer for avtalen",
                    ),
                    FieldError(
                        "/arrangorUnderenheter",
                        "Arrangøren Underenhet 2 AS er i bruk på en av avtalens gjennomføringer, men mangler blant tiltaksarrangørens underenheter",
                    ),
                    FieldError(
                        "/startDato",
                        "Startdato kan ikke være etter startdatoen til gjennomføringer koblet til avtalen. Minst en gjennomføring har startdato: $formatertDato",
                    ),
                )
            }

            test("skal godta at gjennomføring har andre Nav-enheter enn avtalen") {
                MulighetsrommetTestDomain(
                    avtaler = listOf(AvtaleFixtures.oppfolging),
                    gjennomforinger = listOf(
                        gjennomforing.copy(
                            navEnheter = setOf(
                                NavEnhetFixtures.Innlandet.enhetsnummer,
                                NavEnhetFixtures.Gjovik.enhetsnummer,
                            ),
                        ),
                    ),
                ).initialize(database.db)

                val request = avtaleRequest.copy(
                    navEnheter = listOf(NavEnhetFixtures.Oslo.enhetsnummer, NavEnhetFixtures.Sagene.enhetsnummer),
                )

                val previous = database.run { queries.avtale.get(oppfolgingMedRammeAvtale.id) }

                createValidator().validate(request, previous).shouldBeRight()
            }

            test("kan ikke endre tiltakstype hvis prismodell er inkompatibel") {
                MulighetsrommetTestDomain(
                    avtaler = listOf(AvtaleFixtures.oppfolging),
                ).initialize(database.db)

                val request = avtaleRequest.copy(
                    avtaletype = Avtaletype.FORHANDSGODKJENT,
                    opsjonsmodell = Opsjonsmodell(
                        type = OpsjonsmodellType.VALGFRI_SLUTTDATO,
                        opsjonMaksVarighet = null,
                    ),
                    tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
                )

                val previous = database.run { queries.avtale.get(oppfolgingMedRammeAvtale.id) }

                createValidator().validate(request, previous).shouldBeLeft() shouldContain
                    FieldError(
                        "/tiltakskode",
                        "Tiltakstype kan ikke endres ikke fordi prismodellen “Annen avtalt pris” er i bruk",
                    )
            }
        }
    }

    test("Slettede administratorer valideres") {
        MulighetsrommetTestDomain(
            ansatte = listOf(NavAnsattFixture.DonaldDuck.copy(skalSlettesDato = LocalDate.now())),
            avtaler = listOf(AvtaleFixtures.AFT),
        ).initialize(database.db)

        val request = avtaleRequest.copy(
            administratorer = listOf(NavIdent("DD1")),
        )

        createValidator().validate(request, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            FieldError("/administratorer", "Administratorene med Nav ident DD1 er slettet og må fjernes"),
        )
    }

    test("får ikke opprette avtale dersom virksomhet ikke finnes i Brreg") {
        val brregClient = mockk<BrregClient>()
        coEvery { brregClient.getBrregEnhet(Organisasjonsnummer("223442332")) } returns BrregError.NotFound.left()

        val validator = createValidator(
            arrangorService = ArrangorService(database.db, brregClient),
        )

        validator.validate(
            avtaleRequest.copy(
                arrangor = AvtaleRequest.Arrangor(
                    hovedenhet = Organisasjonsnummer("223442332"),
                    underenheter = listOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                    kontaktpersoner = emptyList(),
                ),
            ),
            null,
        ).shouldBeLeft(
            listOf(
                FieldError(
                    "/arrangorHovedenhet",
                    "Tiltaksarrangøren finnes ikke i Brønnøysundregistrene",
                ),
            ),
        )
    }

    context("status endringer") {
        test("status blir UTKAST når avtalen lagres uten en arrangør") {
            createValidator().validate(avtaleRequest.copy(arrangor = null), null).shouldBeRight().should {
                it.status shouldBe AvtaleStatusType.UTKAST
            }
        }

        test("status blir AKTIV når avtalen lagres med sluttdato i fremtiden") {
            createValidator().validate(avtaleRequest, null).shouldBeRight().should {
                it.status shouldBe AvtaleStatusType.AKTIV
            }
        }

        test("status blir AVSLUTTET når avtalen lagres med en sluttdato som er passert") {
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)

            val request = avtaleRequest.copy(
                startDato = yesterday,
                sluttDato = yesterday,
            )

            createValidator().validate(request, null).shouldBeRight().should {
                it.status shouldBe AvtaleStatusType.AVSLUTTET
            }
        }

        test("status forblir AVBRUTT på en avtale som allerede er AVBRUTT") {
            val today = LocalDate.now()

            val avtale = AvtaleFixtures.oppfolging

            MulighetsrommetTestDomain(
                avtaler = listOf(avtale),
            ) {
                queries.avtale.setStatus(
                    avtale.id,
                    AvtaleStatusType.AVBRUTT,
                    tidspunkt = today.atStartOfDay(),
                    aarsaker = listOf(AvbrytAvtaleAarsak.BUDSJETT_HENSYN),
                    forklaring = null,
                )
            }.initialize(database.db)

            val request = avtaleRequest.copy(
                id = avtale.id,
                startDato = today,
                sluttDato = today,
            )
            val previous = database.run { queries.avtale.get(avtale.id) }

            createValidator().validate(request, previous).shouldBeRight().should {
                it.status shouldBe AvtaleStatusType.AVBRUTT
            }
        }
    }
})
