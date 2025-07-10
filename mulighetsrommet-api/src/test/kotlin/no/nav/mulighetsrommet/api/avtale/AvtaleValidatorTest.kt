package no.nav.mulighetsrommet.api.avtale

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.model.*
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import java.time.LocalDate
import java.util.*

class AvtaleValidatorTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        navEnheter = listOf(
            NavEnhetFixtures.IT,
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

    val avtaleDbo = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Avtale",
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        arrangor = AvtaleDbo.Arrangor(
            hovedenhet = ArrangorFixtures.hovedenhet.id,
            underenheter = listOf(ArrangorFixtures.underenhet1.id),
            kontaktpersoner = emptyList(),
        ),
        avtalenummer = "123456",
        sakarkivNummer = SakarkivNummer("24/1234"),
        startDato = LocalDate.now().minusDays(1),
        sluttDato = LocalDate.now().plusMonths(1),
        status = AvtaleStatus.AKTIV,
        administratorer = listOf(NavAnsattFixture.DonaldDuck.navIdent),
        avtaletype = Avtaletype.RAMMEAVTALE,
        prisbetingelser = null,
        navEnheter = listOf(NavEnhetNummer("0400"), NavEnhetNummer("0502")),
        antallPlasser = null,
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = emptyList(),
        personvernBekreftet = false,
        amoKategorisering = null,
        opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN, LocalDate.now().plusYears(3)),
        utdanningslop = null,
        prismodell = Prismodell.ANNEN_AVTALT_PRIS,
        satser = listOf(),
    )

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    fun createValidator() = AvtaleValidator(
        db = database.db,
        tiltakstyper = TiltakstypeService(database.db),
        navEnheterService = NavEnhetService(database.db),
    )

    test("should accumulate errors when dbo has multiple issues") {
        val validator = createValidator()

        val dbo = avtaleDbo.copy(
            startDato = LocalDate.of(2023, 1, 1),
            sluttDato = LocalDate.of(2020, 1, 1),
            navEnheter = emptyList(),
            arrangor = AvtaleDbo.Arrangor(
                hovedenhet = ArrangorFixtures.hovedenhet.id,
                underenheter = emptyList(),
                kontaktpersoner = emptyList(),
            ),
        )

        validator.validate(dbo, null).shouldBeLeft().shouldContainAll(
            listOf(
                FieldError("/startDato", "Startdato må være før sluttdato"),
                FieldError("/navRegioner", "Du må velge minst én Nav-region"),
                FieldError("/arrangorUnderenheter", "Du må velge minst én underenhet for tiltaksarrangør"),
            ),
        )
    }

    test("Avtalenavn må være minst 5 tegn når avtalen er opprettet i Admin-flate") {
        val validator = createValidator()

        val dbo = avtaleDbo.copy(navn = "Avt")

        validator.validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(
                FieldError("/navn", "Avtalenavn må være minst 5 tegn langt"),
            ),
        )
    }

    test("Avtalens startdato må være før eller lik som sluttdato") {
        val validator = createValidator()

        val dagensDato = LocalDate.now()
        val dbo = avtaleDbo.copy(startDato = dagensDato, sluttDato = dagensDato)

        validator.validate(dbo, null).shouldBeRight()

        val dbo2 = avtaleDbo.copy(startDato = dagensDato.plusDays(5), sluttDato = dagensDato)

        validator.validate(dbo2, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(FieldError("/startDato", "Startdato må være før sluttdato")),
        )
    }

    test("Avtalens sluttdato være lik eller etter startdato") {
        val validator = createValidator()

        val dagensDato = LocalDate.now()
        val dbo = avtaleDbo.copy(startDato = dagensDato, sluttDato = dagensDato.minusDays(5))

        validator.validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(FieldError("/startDato", "Startdato må være før sluttdato")),
        )

        val dbo2 = avtaleDbo.copy(startDato = dagensDato, sluttDato = dagensDato)
        validator.validate(dbo2, null).shouldBeRight()
    }

    test("skal validere at Nav-fylke og Nav-enheter er påkrevd") {
        val validator = createValidator()

        val dbo = avtaleDbo.copy(
            navEnheter = listOf(),
        )

        validator.validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(
                FieldError("/navRegioner", "Du må velge minst én Nav-region"),
                FieldError("/navKontorer", "Du må velge minst én Nav-enhet"),
            ),
        )
    }

    test("skal validere at Nav-enheter må være koblet til Nav-fylke") {
        val validator = createValidator()

        val dbo = avtaleDbo.copy(
            navEnheter = listOf(
                NavEnhetFixtures.Oslo.enhetsnummer,
                NavEnhetFixtures.Sagene.enhetsnummer,
                NavEnhetFixtures.Gjovik.enhetsnummer,
            ),
        )

        validator.validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(
                FieldError("/navKontorer", "Nav-enheten 0502 passer ikke i avtalens kontorstruktur"),
            ),
        )
    }

    test("sluttDato er påkrevd hvis ikke forhåndsgodkjent") {
        val forhaandsgodkjent = AvtaleFixtures.AFT.copy(
            sluttDato = null,
            opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.VALGFRI_SLUTTDATO, null),
        )
        val rammeAvtale = AvtaleFixtures.oppfolging.copy(sluttDato = null)
        val avtale = AvtaleFixtures.oppfolgingMedAvtale.copy(sluttDato = null)
        val offentligOffentlig = AvtaleFixtures.gruppeAmo.copy(
            sluttDato = null,
            amoKategorisering = AmoKategorisering.Studiespesialisering,
        )

        val validator = createValidator()

        validator.validate(forhaandsgodkjent, null).shouldBeRight()
        validator.validate(rammeAvtale, null).shouldBeLeft(
            listOf(FieldError("/sluttDato", "Du må legge inn sluttdato for avtalen")),
        )
        validator.validate(avtale, null).shouldBeLeft(
            listOf(FieldError("/sluttDato", "Du må legge inn sluttdato for avtalen")),
        )
        validator.validate(offentligOffentlig, null).shouldBeLeft(
            listOf(FieldError("/sluttDato", "Du må legge inn sluttdato for avtalen")),
        )
    }

    test("amoKategorisering er påkrevd hvis gruppe amo") {
        val validator = createValidator()
        val gruppeAmo = AvtaleFixtures.gruppeAmo.copy(amoKategorisering = null)
        validator.validate(gruppeAmo, null).shouldBeLeft(
            listOf(FieldError("/amoKategorisering.kurstype", "Du må velge en kurstype")),
        )
    }

    test("Opsjonsmodell må være VALGFRI_SLUTTDATO når avtale er forhåndsgodkjent") {
        val validator = createValidator()

        val forhaandsgodkjent1 = AvtaleFixtures.AFT.copy(
            opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.VALGFRI_SLUTTDATO, null),
        )
        validator.validate(forhaandsgodkjent1, null).shouldBeRight()

        val forhaandsgodkjent2 = AvtaleFixtures.AFT.copy(
            opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN, null),
        )
        validator.validate(forhaandsgodkjent2, null).shouldBeLeft(
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

        val avtale = AvtaleFixtures.oppfolgingMedAvtale.copy(
            opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN, null),
        )
        validator.validate(avtale, null).shouldBeLeft(
            listOf(
                FieldError("/opsjonsmodell/opsjonMaksVarighet", "Du må legge inn maks varighet for opsjonen"),
            ),
        )

        val offentligOffentlig = AvtaleFixtures.gruppeAmo.copy(
            avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG,
            opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.VALGFRI_SLUTTDATO, null),
        )
        validator.validate(offentligOffentlig, null).shouldBeRight()
    }

    test("Custom navn for opsjon må være satt hvis opsjonsmodell er ANNET") {
        val validator = createValidator()

        val rammeAvtale = AvtaleFixtures.oppfolging.copy(
            opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.ANNET, LocalDate.now().plusYears(3)),
        )
        validator.validate(rammeAvtale, null).shouldBeLeft(
            listOf(
                FieldError("/opsjonsmodell/customOpsjonsmodellNavn", "Du må beskrive opsjonsmodellen"),
            ),
        )
    }

    test("avtaletype må stemme overens med tiltakstypen") {
        val validator = createValidator()

        forAll(
            row(
                AvtaleFixtures.AFT.copy(avtaletype = Avtaletype.RAMMEAVTALE),
                FieldError(
                    "/avtaletype",
                    "Rammeavtale er ikke tillatt for tiltakstype Arbeidsforberedende trening",
                ),
            ),
            row(
                AvtaleFixtures.VTA.copy(avtaletype = Avtaletype.AVTALE),
                FieldError(
                    "/avtaletype",
                    "Avtale er ikke tillatt for tiltakstype Varig tilrettelagt arbeid i skjermet virksomhet",
                ),
            ),
            row(
                AvtaleFixtures.oppfolging.copy(avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG),
                FieldError("/avtaletype", "Offentlig-offentlig samarbeid er ikke tillatt for tiltakstype Oppfølging"),
            ),
            row(
                AvtaleFixtures.gruppeAmo.copy(avtaletype = Avtaletype.FORHANDSGODKJENT),
                FieldError(
                    "/avtaletype",
                    "Forhåndsgodkjent er ikke tillatt for tiltakstype Arbeidsmarkedsopplæring (Gruppe)",
                ),
            ),
        ) { avtale, expectedError ->
            validator.validate(avtale, null).shouldBeLeft().shouldContain(expectedError)
        }

        forAll(
            row(AvtaleFixtures.AFT.copy(avtaletype = Avtaletype.FORHANDSGODKJENT)),
            row(AvtaleFixtures.AFT.copy(avtaletype = Avtaletype.FORHANDSGODKJENT)),
            row(AvtaleFixtures.oppfolging.copy(avtaletype = Avtaletype.RAMMEAVTALE)),
            row(AvtaleFixtures.gruppeAmo.copy(avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG)),
        ) { avtale ->
            validator.validate(avtale, null).shouldBeRight()
        }
    }

    test("SakarkivNummer må være med når avtalen er avtale eller rammeavtale") {
        val validator = createValidator()

        val rammeavtale = AvtaleFixtures.oppfolging.copy(avtaletype = Avtaletype.RAMMEAVTALE, sakarkivNummer = null)
        validator.validate(rammeavtale, null).shouldBeLeft(
            listOf(FieldError("/sakarkivNummer", "Du må skrive inn saksnummer til avtalesaken")),
        )

        val avtale = AvtaleFixtures.oppfolging.copy(avtaletype = Avtaletype.AVTALE, sakarkivNummer = null)
        validator.validate(avtale, null).shouldBeLeft(
            listOf(FieldError("/sakarkivNummer", "Du må skrive inn saksnummer til avtalesaken")),
        )

        val offentligOffentligSamarbeid = AvtaleFixtures.gruppeAmo.copy(
            avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG,
            sakarkivNummer = null,
            amoKategorisering = AmoKategorisering.Studiespesialisering,
        )
        validator.validate(offentligOffentligSamarbeid, null).shouldBeRight()
    }

    test("arrangørens underenheter må tilhøre hovedenhet i Brreg") {
        val validator = createValidator()

        val avtale1 = AvtaleFixtures.oppfolging.copy(
            arrangor = AvtaleFixtures.oppfolging.arrangor?.copy(
                hovedenhet = ArrangorFixtures.Fretex.hovedenhet.id,
                underenheter = listOf(ArrangorFixtures.underenhet1.id),
            ),
        )

        validator.validate(avtale1, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            FieldError(
                "/arrangor/underenheter",
                "Arrangøren Underenhet 1 AS er ikke en gyldig underenhet til hovedenheten FRETEX AS.",
            ),
        )

        val avtale2 = AvtaleFixtures.oppfolging.copy(
            arrangor = AvtaleFixtures.oppfolging.arrangor?.copy(
                hovedenhet = ArrangorFixtures.Fretex.hovedenhet.id,
                underenheter = listOf(ArrangorFixtures.Fretex.underenhet1.id),
            ),
        )
        validator.validate(avtale2, null).shouldBeRight()
    }

    test("arrangøren må være aktiv i Brreg") {
        database.run {
            queries.arrangor.upsert(ArrangorFixtures.Fretex.hovedenhet.copy(slettetDato = LocalDate.of(2024, 1, 1)))
            queries.arrangor.upsert(ArrangorFixtures.Fretex.underenhet1.copy(slettetDato = LocalDate.of(2024, 1, 1)))
        }

        val avtale1 = AvtaleFixtures.oppfolging.copy(
            arrangor = AvtaleFixtures.oppfolging.arrangor?.copy(
                hovedenhet = ArrangorFixtures.Fretex.hovedenhet.id,
                underenheter = listOf(ArrangorFixtures.Fretex.underenhet1.id),
            ),
        )

        createValidator().validate(avtale1, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            FieldError(
                "/arrangor/hovedenhet",
                "Arrangøren FRETEX AS er slettet i Brønnøysundregistrene. Avtaler kan ikke opprettes for slettede bedrifter.",
            ),
            FieldError(
                "/arrangor/underenheter",
                "Arrangøren FRETEX AS AVD OSLO er slettet i Brønnøysundregistrene. Avtaler kan ikke opprettes for slettede bedrifter.",
            ),
        )
    }

    test("utdanningsprogram er påkrevd når tiltakstypen er Gruppe Fag- og yrkesopplæring") {
        val avtaleMedEndringer = avtaleDbo.copy(
            tiltakstypeId = TiltakstypeFixtures.GruppeFagOgYrkesopplaering.id,
            utdanningslop = null,
        )

        val validator = createValidator()

        validator.validate(avtaleMedEndringer, null) shouldBeLeft listOf(
            FieldError("/utdanningslop", "Du må velge et utdanningsprogram og minst ett lærefag"),
        )
    }

    test("minst én utdanning er påkrevd når tiltakstypen er Gruppe Fag- og yrkesopplæring") {
        val avtaleMedEndringer = avtaleDbo.copy(
            tiltakstypeId = TiltakstypeFixtures.GruppeFagOgYrkesopplaering.id,
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
                    avtaleDbo.copy(
                        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                        prismodell = Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
                    ),
                    FieldError(
                        "/prismodell",
                        "Fast månedspris per tiltaksplass er ikke tillatt for tiltakstype Oppfølging",
                    ),
                ),
                row(
                    avtaleDbo.copy(
                        tiltakstypeId = TiltakstypeFixtures.AFT.id,
                        prismodell = Prismodell.ANNEN_AVTALT_PRIS,
                    ),
                    FieldError(
                        "/prismodell",
                        "Annen avtalt pris er ikke tillatt for tiltakstype Arbeidsforberedende trening",
                    ),
                ),
                row(
                    avtaleDbo.copy(
                        tiltakstypeId = TiltakstypeFixtures.GruppeAmo.id,
                        prismodell = Prismodell.AVTALT_PRIS_PER_UKESVERK,
                    ),
                    FieldError(
                        "/prismodell",
                        "Avtalt ukespris per tiltaksplass er ikke tillatt for tiltakstype Arbeidsmarkedsopplæring (Gruppe)",
                    ),
                ),
            ) { avtale, expectedError ->
                validator.validate(avtale, null).shouldBeLeft().shouldContain(expectedError)
            }

            val fri = avtaleDbo.copy(prismodell = Prismodell.ANNEN_AVTALT_PRIS)
            validator.validate(fri, null).shouldBeRight()
        }
    }

    context("når avtalen allerede eksisterer") {
        test("Skal ikke kunne endre opsjonsmodell eller avtaletype når opsjon er registrert") {
            val startDato = LocalDate.of(2024, 5, 7)
            database.run {
                queries.avtale.upsert(
                    avtaleDbo.copy(
                        avtaletype = Avtaletype.RAMMEAVTALE,
                        startDato = startDato,
                        sluttDato = startDato.plusYears(1),
                        opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN_PLUSS_EN, startDato.plusYears(4)),
                    ),
                )
                queries.opsjoner.insert(
                    OpsjonLoggEntry(
                        id = UUID.randomUUID(),
                        avtaleId = avtaleDbo.id,
                        sluttdato = avtaleDbo.sluttDato?.plusYears(1),
                        forrigeSluttdato = avtaleDbo.sluttDato,
                        status = OpsjonLoggStatus.OPSJON_UTLOST,
                        registretDato = LocalDate.of(2024, 7, 6),
                        registrertAv = NavIdent("M123456"),
                    ),
                )
            }

            val previous = database.run { queries.avtale.get(avtaleDbo.id) }
            val avtale = avtaleDbo.copy(
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
                avtaleId = avtaleDbo.id,
                administratorer = emptyList(),
                startDato = startDatoForGjennomforing,
            )

            test("skal validere at data samsvarer med avtalens gjennomføringer") {
                MulighetsrommetTestDomain(
                    avtaler = listOf(avtaleDbo),
                    gjennomforinger = listOf(gjennomforing.copy(arrangorId = ArrangorFixtures.underenhet2.id)),
                ).initialize(database.db)

                val dbo = avtaleDbo.copy(
                    tiltakstypeId = TiltakstypeFixtures.ArbeidsrettetRehabilitering.id,
                    startDato = startDatoForGjennomforing.plusDays(1),
                )

                val previous = database.run { queries.avtale.get(avtaleDbo.id) }
                val formatertDato = startDatoForGjennomforing.formaterDatoTilEuropeiskDatoformat()

                createValidator().validate(dbo, previous).shouldBeLeft() shouldContainExactlyInAnyOrder listOf(
                    FieldError(
                        "/tiltakstypeId",
                        "Tiltakstype kan ikke endres fordi det finnes gjennomføringer for avtalen",
                    ),
                    FieldError(
                        "/arrangor/underenheter",
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
                    avtaler = listOf(avtaleDbo),
                    gjennomforinger = listOf(
                        gjennomforing.copy(
                            navEnheter = setOf(
                                NavEnhetFixtures.Innlandet.enhetsnummer,
                                NavEnhetFixtures.Gjovik.enhetsnummer,
                            ),
                        ),
                    ),
                ).initialize(database.db)

                val dbo = avtaleDbo.copy(
                    navEnheter = listOf(NavEnhetFixtures.Oslo.enhetsnummer, NavEnhetFixtures.Sagene.enhetsnummer),
                )

                val previous = database.run { queries.avtale.get(avtaleDbo.id) }

                createValidator().validate(dbo, previous).shouldBeRight()
            }
        }
    }

    test("Slettede administratorer valideres") {
        MulighetsrommetTestDomain(
            ansatte = listOf(NavAnsattFixture.DonaldDuck.copy(skalSlettesDato = LocalDate.now())),
            avtaler = listOf(avtaleDbo),
        ).initialize(database.db)

        val dbo = avtaleDbo.copy(
            administratorer = listOf(NavIdent("DD1")),
        )

        createValidator().validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            FieldError("/administratorer", "Administratorene med Nav ident DD1 er slettet og må fjernes"),
        )
    }
})
