package no.nav.mulighetsrommet.api.avtale

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.mockk.every
import io.mockk.mockk
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.model.OpsjonLoggEntry
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dto.*
import no.nav.mulighetsrommet.unleash.Toggle
import no.nav.mulighetsrommet.unleash.UnleashService
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import java.time.LocalDate
import java.util.*

class AvtaleValidatorTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        enheter = listOf(
            NavEnhetFixtures.IT,
            NavEnhetFixtures.Oslo,
            NavEnhetFixtures.Innlandet,
            NavEnhetFixtures.Gjovik,
        ),
        ansatte = listOf(NavAnsattFixture.ansatt1),
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
        arrangorId = ArrangorFixtures.hovedenhet.id,
        arrangorUnderenheter = listOf(ArrangorFixtures.underenhet1.id),
        arrangorKontaktpersoner = emptyList(),
        avtalenummer = "123456",
        websaknummer = Websaknummer("24/1234"),
        startDato = LocalDate.now().minusDays(1),
        sluttDato = LocalDate.now().plusMonths(1),
        administratorer = listOf(NavAnsattFixture.ansatt1.navIdent),
        avtaletype = Avtaletype.Rammeavtale,
        prisbetingelser = null,
        navEnheter = listOf("0400", "0502"),
        antallPlasser = null,
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = emptyList(),
        personvernBekreftet = false,
        amoKategorisering = null,
        opsjonMaksVarighet = LocalDate.now().plusYears(3),
        opsjonsmodell = Opsjonsmodell.TO_PLUSS_EN,
        customOpsjonsmodellNavn = null,
        utdanningslop = null,
        prismodell = null,
    )

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    fun createValidator(
        unleash: UnleashService = mockk(relaxed = true),
    ) = AvtaleValidator(
        db = database.db,
        tiltakstyper = TiltakstypeService(database.db),
        navEnheterService = NavEnhetService(database.db),
        unleash = unleash,
    )

    test("should accumulate errors when dbo has multiple issues") {
        val validator = createValidator()

        val dbo = avtaleDbo.copy(
            startDato = LocalDate.of(2023, 1, 1),
            sluttDato = LocalDate.of(2020, 1, 1),
            navEnheter = emptyList(),
            arrangorUnderenheter = emptyList(),
        )

        validator.validate(dbo, null).shouldBeLeft().shouldContainAll(
            listOf(
                ValidationError("startDato", "Startdato må være før sluttdato"),
                ValidationError("navEnheter", "Du må velge minst én Nav-region"),
                ValidationError("arrangorUnderenheter", "Du må velge minst én underenhet for tiltaksarrangør"),
            ),
        )
    }

    test("Avtalenavn må være minst 5 tegn når avtalen er opprettet i Admin-flate") {
        val validator = createValidator()

        val dbo = avtaleDbo.copy(navn = "Avt")

        validator.validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(
                ValidationError("navn", "Avtalenavn må være minst 5 tegn langt"),
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
            listOf(ValidationError("startDato", "Startdato må være før sluttdato")),
        )
    }

    test("Avtalens sluttdato være lik eller etter startdato") {
        val validator = createValidator()

        val dagensDato = LocalDate.now()
        val dbo = avtaleDbo.copy(startDato = dagensDato, sluttDato = dagensDato.minusDays(5))

        validator.validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(ValidationError("startDato", "Startdato må være før sluttdato")),
        )

        val dbo2 = avtaleDbo.copy(startDato = dagensDato, sluttDato = dagensDato)
        validator.validate(dbo2, null).shouldBeRight()
    }

    test("skal validere at Nav-enheter må være koblet til Nav-fylke") {
        val validator = createValidator()

        val dbo = avtaleDbo.copy(
            navEnheter = listOf("0300", "0502"),
        )

        validator.validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(
                ValidationError("navEnheter", "Nav-enheten 0502 passer ikke i avtalens kontorstruktur"),
            ),
        )
    }

    test("sluttDato er påkrevd hvis ikke forhåndsgodkjent") {
        val forhaandsgodkjent = AvtaleFixtures.AFT.copy(sluttDato = null)
        val rammeAvtale = AvtaleFixtures.oppfolging.copy(sluttDato = null)
        val avtale = AvtaleFixtures.oppfolgingMedAvtale.copy(sluttDato = null)
        val offentligOffentlig = AvtaleFixtures.gruppeAmo.copy(
            sluttDato = null,
            amoKategorisering = AmoKategorisering.Studiespesialisering,
        )

        val validator = createValidator()

        validator.validate(forhaandsgodkjent, null).shouldBeRight()
        validator.validate(rammeAvtale, null).shouldBeLeft(
            listOf(ValidationError("sluttDato", "Du må legge inn sluttdato for avtalen")),
        )
        validator.validate(avtale, null).shouldBeLeft(
            listOf(ValidationError("sluttDato", "Du må legge inn sluttdato for avtalen")),
        )
        validator.validate(offentligOffentlig, null).shouldBeLeft(
            listOf(ValidationError("sluttDato", "Du må legge inn sluttdato for avtalen")),
        )
    }

    test("amoKategorisering er påkrevd hvis gruppe amo") {
        val validator = createValidator()
        val gruppeAmo = AvtaleFixtures.gruppeAmo.copy(amoKategorisering = null)
        validator.validate(gruppeAmo, null).shouldBeLeft(
            listOf(ValidationError("amoKategorisering.kurstype", "Du må velge en kurstype")),
        )
    }

    test("Opsjonsdata må være satt hvis ikke avtaletypen er forhåndsgodkjent") {
        val forhaandsgodkjent = AvtaleFixtures.AFT
        val rammeAvtale = AvtaleFixtures.oppfolging.copy(opsjonsmodell = null, opsjonMaksVarighet = null)
        val avtale = AvtaleFixtures.oppfolgingMedAvtale.copy(opsjonsmodell = null, opsjonMaksVarighet = null)
        val offentligOffentlig = AvtaleFixtures.gruppeAmo.copy(
            opsjonsmodell = null,
            opsjonMaksVarighet = null,
            amoKategorisering = AmoKategorisering.Studiespesialisering,
        )

        val validator = createValidator()

        validator.validate(forhaandsgodkjent, null).shouldBeRight()
        validator.validate(rammeAvtale, null).shouldBeLeft(
            listOf(
                ValidationError("opsjonMaksVarighet", "Du må legge inn maks varighet for opsjonen"),
                ValidationError("opsjonsmodell", "Du må velge en opsjonsmodell"),
            ),
        )
        validator.validate(avtale, null).shouldBeLeft(
            listOf(
                ValidationError("opsjonMaksVarighet", "Du må legge inn maks varighet for opsjonen"),
                ValidationError("opsjonsmodell", "Du må velge en opsjonsmodell"),
            ),
        )
        validator.validate(offentligOffentlig, null).shouldBeLeft(
            listOf(
                ValidationError("opsjonMaksVarighet", "Du må legge inn maks varighet for opsjonen"),
                ValidationError("opsjonsmodell", "Du må velge en opsjonsmodell"),
            ),
        )
    }

    test("Custom navn for opsjon må være satt hvis opsjonsmodell er ANNET") {
        val rammeAvtale = AvtaleFixtures.oppfolging.copy(
            opsjonsmodell = Opsjonsmodell.ANNET,
            opsjonMaksVarighet = LocalDate.now(),
        )

        val validator = createValidator()

        validator.validate(rammeAvtale, null).shouldBeLeft(
            listOf(
                ValidationError("customOpsjonsmodellNavn", "Du må beskrive opsjonsmodellen"),
            ),
        )
    }

    test("avtaletype må være allowed") {
        val aft = AvtaleFixtures.AFT.copy(
            avtaletype = Avtaletype.Rammeavtale,
            opsjonMaksVarighet = LocalDate.now(),
            opsjonsmodell = Opsjonsmodell.TO_PLUSS_EN,
        )
        val vta = AvtaleFixtures.VTA.copy(
            avtaletype = Avtaletype.Avtale,
            opsjonMaksVarighet = LocalDate.now(),
            opsjonsmodell = Opsjonsmodell.TO_PLUSS_EN,
        )
        val oppfolging = AvtaleFixtures.oppfolging.copy(avtaletype = Avtaletype.OffentligOffentlig)
        val gruppeAmo = AvtaleFixtures.gruppeAmo.copy(
            avtaletype = Avtaletype.Forhaandsgodkjent,
            amoKategorisering = AmoKategorisering.Studiespesialisering,
        )

        val validator = createValidator()

        validator.validate(aft, null).shouldBeLeft(
            listOf(
                ValidationError(
                    "avtaletype",
                    "Rammeavtale er ikke tillatt for tiltakstype Arbeidsforberedende trening (AFT)",
                ),
            ),
        )
        validator.validate(vta, null).shouldBeLeft(
            listOf(
                ValidationError(
                    "avtaletype",
                    "Avtale er ikke tillatt for tiltakstype Varig tilrettelagt arbeid i skjermet virksomhet",
                ),
            ),
        )
        validator.validate(oppfolging, null).shouldBeLeft(
            listOf(ValidationError("avtaletype", "OffentligOffentlig er ikke tillatt for tiltakstype Oppfølging")),
        )
        validator.validate(gruppeAmo, null).shouldBeLeft(
            listOf(ValidationError("avtaletype", "Forhaandsgodkjent er ikke tillatt for tiltakstype Gruppe amo")),
        )

        val aftForhaands = AvtaleFixtures.AFT.copy(avtaletype = Avtaletype.Forhaandsgodkjent)
        val vtaForhaands = AvtaleFixtures.AFT.copy(avtaletype = Avtaletype.Forhaandsgodkjent)
        val oppfolgingRamme = AvtaleFixtures.oppfolging.copy(avtaletype = Avtaletype.Rammeavtale)
        val gruppeAmoOffentlig = AvtaleFixtures.gruppeAmo.copy(
            avtaletype = Avtaletype.OffentligOffentlig,
            amoKategorisering = AmoKategorisering.Studiespesialisering,
        )
        validator.validate(aftForhaands, null).shouldBeRight()
        validator.validate(vtaForhaands, null).shouldBeRight()
        validator.validate(oppfolgingRamme, null).shouldBeRight()
        validator.validate(gruppeAmoOffentlig, null).shouldBeRight()
    }

    test("Websak-referanse må være med når avtalen er avtale eller rammeavtale") {
        val validator = createValidator()

        val rammeavtale = AvtaleFixtures.oppfolging.copy(avtaletype = Avtaletype.Rammeavtale, websaknummer = null)
        validator.validate(rammeavtale, null).shouldBeLeft(
            listOf(ValidationError("websaknummer", "Du må skrive inn Websaknummer til avtalesaken")),
        )

        val avtale = AvtaleFixtures.oppfolging.copy(avtaletype = Avtaletype.Avtale, websaknummer = null)
        validator.validate(avtale, null).shouldBeLeft(
            listOf(ValidationError("websaknummer", "Du må skrive inn Websaknummer til avtalesaken")),
        )

        val offentligOffentligSamarbeid = AvtaleFixtures.gruppeAmo.copy(
            avtaletype = Avtaletype.OffentligOffentlig,
            websaknummer = null,
            amoKategorisering = AmoKategorisering.Studiespesialisering,
        )
        validator.validate(offentligOffentligSamarbeid, null).shouldBeRight()
    }

    test("arrangørens underenheter må tilhøre hovedenhet i Brreg") {
        val validator = createValidator()

        val avtale1 = AvtaleFixtures.oppfolging.copy(
            arrangorId = ArrangorFixtures.Fretex.hovedenhet.id,
            arrangorUnderenheter = listOf(ArrangorFixtures.underenhet1.id),
        )

        validator.validate(avtale1, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            ValidationError(
                "arrangorUnderenheter",
                "Arrangøren Underenhet 1 AS er ikke en gyldig underenhet til hovedenheten FRETEX AS.",
            ),
        )

        val avtale2 = AvtaleFixtures.oppfolging.copy(
            arrangorId = ArrangorFixtures.Fretex.hovedenhet.id,
            arrangorUnderenheter = listOf(ArrangorFixtures.Fretex.underenhet1.id),
        )
        validator.validate(avtale2, null).shouldBeRight()
    }

    test("arrangøren må være aktiv i Brreg") {
        database.run {
            queries.arrangor.upsert(ArrangorFixtures.Fretex.hovedenhet.copy(slettetDato = LocalDate.of(2024, 1, 1)))
            queries.arrangor.upsert(ArrangorFixtures.Fretex.underenhet1.copy(slettetDato = LocalDate.of(2024, 1, 1)))
        }

        val avtale1 = AvtaleFixtures.oppfolging.copy(
            arrangorId = ArrangorFixtures.Fretex.hovedenhet.id,
            arrangorUnderenheter = listOf(ArrangorFixtures.Fretex.underenhet1.id),
        )

        createValidator().validate(avtale1, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            ValidationError(
                "arrangorId",
                "Arrangøren FRETEX AS er slettet i Brønnøysundregistrene. Avtaler kan ikke opprettes for slettede bedrifter.",
            ),
            ValidationError(
                "arrangorUnderenheter",
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
            ValidationError("utdanningslop", "Du må velge et utdanningsprogram og minst ett lærefag"),
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
                ValidationError("utdanningslop", "Du må velge minst ett lærefag"),
            ),
        )
    }

    context("prismodell") {
        test("kan ikke settes når feature er disabled") {
            val unleash = mockk<UnleashService>()
            every {
                unleash.isEnabledForTiltakstype(Toggle.MIGRERING_OKONOMI, Tiltakskode.OPPFOLGING)
            } returns false

            val validator = createValidator(unleash)

            val dbo = avtaleDbo.copy(prismodell = Prismodell.FORHANDSGODKJENT)

            validator.validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
                ValidationError("prismodell", "Prismodell kan foreløpig ikke velges for tiltakstypen OPPFOLGING"),
            )
        }

        test("må settes når feature er enabled") {
            val unleash = mockk<UnleashService>()
            every {
                unleash.isEnabledForTiltakstype(Toggle.MIGRERING_OKONOMI, Tiltakskode.OPPFOLGING)
            } returns true

            val validator = createValidator(unleash)

            val dbo = avtaleDbo.copy(prismodell = null)
            validator.validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
                ValidationError("prismodell", "Du må velge en prismodell"),
            )
        }

        test("prismodell må stemme overens med avtaletypen") {
            val unleash = mockk<UnleashService>()
            every {
                unleash.isEnabledForTiltakstype(Toggle.MIGRERING_OKONOMI, Tiltakskode.OPPFOLGING)
            } returns true

            val validator = createValidator(unleash)

            val forhandsgodkjent = avtaleDbo.copy(prismodell = Prismodell.FORHANDSGODKJENT)
            validator.validate(forhandsgodkjent, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
                ValidationError("prismodell", "Prismodellen kan ikke være forhåndsgodkjent"),
            )

            val fri = avtaleDbo.copy(prismodell = Prismodell.FRI)
            validator.validate(fri, null).shouldBeRight()
        }
    }

    context("når avtalen allerede eksisterer") {
        test("Skal ikke kunne endre opsjonsmodell eller avtaletype når opsjon er registrert") {
            database.run {
                queries.avtale.upsert(
                    avtaleDbo.copy(
                        avtaletype = Avtaletype.Rammeavtale,
                        opsjonsmodell = Opsjonsmodell.TO_PLUSS_EN_PLUSS_EN,
                        opsjonMaksVarighet = LocalDate.of(2024, 5, 7).plusYears(3),
                        startDato = LocalDate.of(2024, 5, 7),
                        sluttDato = LocalDate.of(2024, 5, 7).plusYears(1),
                    ),
                )
            }

            val opsjonLoggService = OpsjonLoggService(database.db)
            opsjonLoggService.lagreOpsjonLoggEntry(
                OpsjonLoggEntry(
                    avtaleId = avtaleDbo.id,
                    sluttdato = avtaleDbo.sluttDato?.plusYears(1),
                    forrigeSluttdato = avtaleDbo.sluttDato,
                    status = OpsjonLoggRequest.OpsjonsLoggStatus.OPSJON_UTLØST,
                    registrertAv = NavIdent("M123456"),
                ),
            )

            val previous = database.run { queries.avtale.get(avtaleDbo.id) }
            val avtale = avtaleDbo.copy(
                avtaletype = Avtaletype.Avtale,
                opsjonsmodell = Opsjonsmodell.TO_PLUSS_EN,
                opsjonMaksVarighet = LocalDate.of(2024, 5, 7).plusYears(3),
            )

            createValidator().validate(avtale, previous) shouldBeLeft listOf(
                ValidationError("avtaletype", "Du kan ikke endre avtaletype når opsjoner er registrert"),
                ValidationError("opsjonsmodell", "Du kan ikke endre opsjonsmodell når opsjoner er registrert"),
            )
        }

        context("når avtalen har gjennomføringer") {
            val startDatoForGjennomforing = avtaleDbo.startDato

            val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtaleDbo.id,
                administratorer = emptyList(),
                navRegion = "0400",
                startDato = startDatoForGjennomforing,
            )

            test("skal validere at data samsvarer med avtalens gjennomføringer") {
                MulighetsrommetTestDomain(
                    avtaler = listOf(avtaleDbo),
                    gjennomforinger = listOf(gjennomforing.copy(arrangorId = ArrangorFixtures.underenhet2.id)),
                ).initialize(database.db)

                val dbo = avtaleDbo.copy(
                    tiltakstypeId = TiltakstypeFixtures.AFT.id,
                    avtaletype = Avtaletype.Forhaandsgodkjent,
                    startDato = avtaleDbo.startDato.plusDays(4),
                )

                val previous = database.run { queries.avtale.get(avtaleDbo.id) }
                val formatertDato = startDatoForGjennomforing.formaterDatoTilEuropeiskDatoformat()

                createValidator().validate(dbo, previous).shouldBeLeft() shouldContainExactlyInAnyOrder listOf(
                    ValidationError(
                        "tiltakstypeId",
                        "Tiltakstype kan ikke endres fordi det finnes gjennomføringer for avtalen",
                    ),
                    ValidationError(
                        "arrangorUnderenheter",
                        "Arrangøren Underenhet 2 AS er i bruk på en av avtalens gjennomføringer, men mangler blant tiltaksarrangørens underenheter",
                    ),
                    ValidationError(
                        "startDato",
                        "Startdato kan ikke være etter startdatoen til gjennomføringer koblet til avtalen. Minst en gjennomføring har startdato: $formatertDato",
                    ),
                )
            }

            test("skal godta at gjennomføring har andre Nav-enheter enn avtalen") {
                MulighetsrommetTestDomain(
                    avtaler = listOf(avtaleDbo),
                    gjennomforinger = listOf(gjennomforing.copy(navRegion = "0400")),
                ).initialize(database.db)

                val dbo = avtaleDbo.copy(
                    navEnheter = listOf("0400"),
                )

                val previous = database.run { queries.avtale.get(avtaleDbo.id) }

                createValidator().validate(dbo, previous).shouldBeRight()
            }
        }
    }

    test("Slettede administratorer valideres") {
        MulighetsrommetTestDomain(
            ansatte = listOf(NavAnsattFixture.ansatt1.copy(skalSlettesDato = LocalDate.now())),
            avtaler = listOf(avtaleDbo),
        ).initialize(database.db)

        val dbo = avtaleDbo.copy(
            navEnheter = listOf("0400"),
            administratorer = listOf(NavIdent("DD1")),
        )

        createValidator().validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            ValidationError("administratorer", "Administratorene med Nav ident DD1 er slettet og må fjernes"),
        )
    }
})
