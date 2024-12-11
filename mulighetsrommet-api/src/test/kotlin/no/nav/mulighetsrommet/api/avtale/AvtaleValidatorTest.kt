package no.nav.mulighetsrommet.api.avtale

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.arrangor.db.ArrangorRepository
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.db.AvtaleRepository
import no.nav.mulighetsrommet.api.avtale.db.OpsjonLoggRepository
import no.nav.mulighetsrommet.api.avtale.model.OpsjonLoggEntry
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetRepository
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.services.EndringshistorikkService
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.tiltakstype.db.TiltakstypeRepository
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.*
import no.nav.mulighetsrommet.unleash.UnleashService
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import java.time.LocalDate
import java.util.*

class AvtaleValidatorTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))
    val unleash: UnleashService = mockk(relaxed = true)
    coEvery { unleash.isEnabled(any()) } returns true

    val domain = MulighetsrommetTestDomain(
        enheter = listOf(
            NavEnhetDbo(
                navn = "Nav Oslo",
                enhetsnummer = "0300",
                status = NavEnhetStatus.AKTIV,
                type = Norg2Type.FYLKE,
                overordnetEnhet = null,
            ),
            NavEnhetDbo(
                navn = "Nav Innlandet",
                enhetsnummer = "0400",
                status = NavEnhetStatus.AKTIV,
                type = Norg2Type.FYLKE,
                overordnetEnhet = null,
            ),
            NavEnhetDbo(
                navn = "Nav Gjøvik",
                enhetsnummer = "0502",
                status = NavEnhetStatus.AKTIV,
                type = Norg2Type.LOKAL,
                overordnetEnhet = "0400",
            ),
        ),
        ansatte = listOf(),
        arrangorer = listOf(
            ArrangorFixtures.hovedenhet,
            ArrangorFixtures.underenhet1,
            ArrangorFixtures.underenhet2,
            ArrangorFixtures.Fretex.hovedenhet,
            ArrangorFixtures.Fretex.underenhet1,
        ),
        tiltakstyper = listOf(
            TiltakstypeFixtures.AFT,
            TiltakstypeFixtures.VTA,
            TiltakstypeFixtures.Oppfolging,
            TiltakstypeFixtures.Jobbklubb,
            TiltakstypeFixtures.GruppeAmo,
            TiltakstypeFixtures.GruppeFagOgYrkesopplaering,
            TiltakstypeFixtures.Arbeidstrening,
            TiltakstypeFixtures.Avklaring,
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
        administratorer = listOf(NavIdent("B123456")),
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
    )

    lateinit var navEnheterService: NavEnhetService
    lateinit var tiltakstyper: TiltakstypeService
    lateinit var avtaler: AvtaleRepository
    lateinit var opsjonslogg: OpsjonLoggRepository
    lateinit var gjennomforinger: TiltaksgjennomforingRepository
    lateinit var arrangorer: ArrangorRepository

    beforeEach {
        domain.initialize(database.db)

        tiltakstyper = TiltakstypeService(TiltakstypeRepository(database.db))
        navEnheterService = NavEnhetService(NavEnhetRepository(database.db))
        avtaler = AvtaleRepository(database.db)
        opsjonslogg = OpsjonLoggRepository(database.db)
        gjennomforinger = TiltaksgjennomforingRepository(database.db)
        arrangorer = ArrangorRepository(database.db)
    }

    afterEach {
        database.db.truncateAll()
    }

    test("should accumulate errors when dbo has multiple issues") {
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer, unleash)

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
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer, unleash)

        val dbo = avtaleDbo.copy(navn = "Avt")

        validator.validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(
                ValidationError("navn", "Avtalenavn må være minst 5 tegn langt"),
            ),
        )
    }

    test("Avtalens startdato må være før eller lik som sluttdato") {
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer, unleash)

        val dagensDato = LocalDate.now()
        val dbo = avtaleDbo.copy(startDato = dagensDato, sluttDato = dagensDato)

        validator.validate(dbo, null).shouldBeRight()

        val dbo2 = avtaleDbo.copy(startDato = dagensDato.plusDays(5), sluttDato = dagensDato)

        validator.validate(dbo2, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(ValidationError("startDato", "Startdato må være før sluttdato")),
        )
    }

    test("Avtalens lengde er maks 5 år for ikke AFT/VTA") {
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer, unleash)

        val dagensDato = LocalDate.now()
        val dbo = avtaleDbo.copy(startDato = dagensDato, sluttDato = dagensDato.plusYears(5))

        validator.validate(dbo, null).shouldBeRight()

        val dbo2 = avtaleDbo.copy(startDato = dagensDato, sluttDato = dagensDato.plusYears(6))

        validator.validate(dbo2, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(
                ValidationError(
                    "sluttDato",
                    "Avtaleperioden kan ikke vare lenger enn 5 år for anskaffede tiltak",
                ),
            ),
        )
    }

    test("Avtalens sluttdato være lik eller etter startdato") {
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer, unleash)

        val dagensDato = LocalDate.now()
        val dbo = avtaleDbo.copy(startDato = dagensDato, sluttDato = dagensDato.minusDays(5))

        validator.validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(ValidationError("startDato", "Startdato må være før sluttdato")),
        )

        val dbo2 = avtaleDbo.copy(startDato = dagensDato, sluttDato = dagensDato)
        validator.validate(dbo2, null).shouldBeRight()
    }

    test("skal validere at Nav-enheter må være koblet til Nav-fylke") {
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer, unleash)

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

        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer, unleash)

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
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer, unleash)
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

        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer, unleash)

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

        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer, unleash)

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

        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer, unleash)

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
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer, unleash)

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
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer, unleash)

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
        arrangorer.upsert(ArrangorFixtures.Fretex.hovedenhet.copy(slettetDato = LocalDate.of(2024, 1, 1)))
        arrangorer.upsert(ArrangorFixtures.Fretex.underenhet1.copy(slettetDato = LocalDate.of(2024, 1, 1)))

        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer, unleash)

        val avtale1 = AvtaleFixtures.oppfolging.copy(
            arrangorId = ArrangorFixtures.Fretex.hovedenhet.id,
            arrangorUnderenheter = listOf(ArrangorFixtures.Fretex.underenhet1.id),
        )

        validator.validate(avtale1, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
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

        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer, unleash)

        validator.validate(avtaleMedEndringer, null).shouldBeLeft(
            listOf(
                ValidationError("utdanningslop", "Du må velge et utdanningsprogram og minst ett lærefag"),
            ),
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

        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer, unleash)

        validator.validate(avtaleMedEndringer, null).shouldBeLeft(
            listOf(
                ValidationError("utdanningslop", "Du må velge minst ett lærefag"),
            ),
        )
    }

    context("når avtalen allerede eksisterer") {
        test("skal kunne endre felter med opphav fra Arena") {
            val avtaleMedEndringer = AvtaleDbo(
                id = avtaleDbo.id,
                navn = "Nytt navn",
                tiltakstypeId = TiltakstypeFixtures.AFT.id,
                arrangorId = ArrangorFixtures.underenhet1.id,
                arrangorUnderenheter = listOf(ArrangorFixtures.underenhet1.id),
                arrangorKontaktpersoner = emptyList(),
                avtalenummer = "123456",
                websaknummer = Websaknummer("24/1234"),
                startDato = LocalDate.now(),
                sluttDato = LocalDate.now().plusYears(1),
                administratorer = listOf(NavIdent("B123456")),
                avtaletype = Avtaletype.Forhaandsgodkjent,
                prisbetingelser = null,
                navEnheter = listOf("0300"),
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
            )

            avtaler.upsert(avtaleDbo.copy(administratorer = listOf()))
            avtaler.setOpphav(avtaleDbo.id, ArenaMigrering.Opphav.ARENA)

            val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer, unleash)

            val previous = avtaler.get(avtaleDbo.id)
            validator.validate(avtaleMedEndringer, previous).shouldBeRight()
        }

        test("Skal ikke kunne endre opsjonsmodell når opsjon er registrert") {
            val endringshistorikkService: EndringshistorikkService = mockk(relaxed = true)
            val opsjonValidator = OpsjonLoggValidator()

            val opsjonLoggService =
                OpsjonLoggService(database.db, opsjonValidator, avtaler, opsjonslogg, endringshistorikkService)

            avtaler.upsert(
                avtaleDbo.copy(
                    administratorer = listOf(),
                    tiltakstypeId = TiltakstypeFixtures.Jobbklubb.id,
                    opsjonsmodell = Opsjonsmodell.TO_PLUSS_EN_PLUSS_EN,
                    opsjonMaksVarighet = LocalDate.of(2024, 5, 7).plusYears(3),
                    startDato = LocalDate.of(2024, 5, 7),
                    sluttDato = LocalDate.of(2024, 5, 7).plusYears(1),
                ),
            )
            avtaler.setOpphav(avtaleDbo.id, ArenaMigrering.Opphav.MR_ADMIN_FLATE)

            opsjonLoggService.lagreOpsjonLoggEntry(
                OpsjonLoggEntry(
                    avtaleId = avtaleDbo.id,
                    sluttdato = avtaleDbo.sluttDato?.plusYears(1),
                    forrigeSluttdato = avtaleDbo.sluttDato,
                    status = OpsjonLoggRequest.OpsjonsLoggStatus.OPSJON_UTLØST,
                    registrertAv = NavIdent("M123456"),
                ),
            )

            val previous = avtaler.get(avtaleDbo.id)

            val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer, unleash)

            validator.validate(
                avtaleDbo.copy(
                    administratorer = listOf(NavIdent("B123456")),
                    tiltakstypeId = TiltakstypeFixtures.Jobbklubb.id,
                    opsjonsmodell = Opsjonsmodell.TO_PLUSS_EN,
                    opsjonMaksVarighet = LocalDate.of(2024, 5, 7).plusYears(3),
                ),
                previous,
            ).shouldBeLeft(
                listOf(
                    ValidationError("opsjonsmodell", "Du kan ikke endre opsjonsmodell når opsjoner er registrert"),
                ),
            )
        }

        test("Skal ikke kunne endre avtaletype når opsjon er registrert") {
            val endringshistorikkService: EndringshistorikkService = mockk(relaxed = true)
            val opsjonValidator = OpsjonLoggValidator()

            val opsjonLoggService =
                OpsjonLoggService(database.db, opsjonValidator, avtaler, opsjonslogg, endringshistorikkService)

            avtaler.upsert(
                AvtaleFixtures.oppfolging.copy(
                    opsjonsmodell = Opsjonsmodell.TO_PLUSS_EN_PLUSS_EN,
                    administratorer = emptyList(),
                    opsjonMaksVarighet = LocalDate.of(2024, 5, 7).plusYears(3),
                    avtaletype = Avtaletype.Rammeavtale,
                ),
            )
            opsjonLoggService.lagreOpsjonLoggEntry(
                OpsjonLoggEntry(
                    avtaleId = AvtaleFixtures.oppfolging.id,
                    sluttdato = AvtaleFixtures.oppfolging.sluttDato?.plusYears(1),
                    forrigeSluttdato = AvtaleFixtures.oppfolging.sluttDato,
                    status = OpsjonLoggRequest.OpsjonsLoggStatus.OPSJON_UTLØST,
                    registrertAv = NavIdent("M123456"),
                ),
            )

            val previous = avtaler.get(AvtaleFixtures.oppfolging.id)

            val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer, unleash)

            validator.validate(
                AvtaleFixtures.oppfolging.copy(
                    administratorer = listOf(NavIdent("B123456")),
                    avtaletype = Avtaletype.Avtale,
                    opsjonsmodell = Opsjonsmodell.TO_PLUSS_EN_PLUSS_EN,
                    opsjonMaksVarighet = LocalDate.of(2024, 5, 7).plusYears(3),
                ),
                previous,
            ).shouldBeLeft(
                listOf(
                    ValidationError("avtaletype", "Du kan ikke endre avtaletype når opsjoner er registrert"),
                ),
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

            beforeAny {
                avtaler.upsert(avtaleDbo.copy(administratorer = listOf()))
            }

            afterAny {
                gjennomforinger.delete(TiltaksgjennomforingFixtures.Oppfolging1.id)
            }

            test("skal validere at data samsvarer med avtalens gjennomføringer") {
                gjennomforinger.upsert(
                    gjennomforing.copy(arrangorId = ArrangorFixtures.underenhet2.id),
                )

                val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer, unleash)

                val dbo = avtaleDbo.copy(
                    tiltakstypeId = TiltakstypeFixtures.AFT.id,
                    avtaletype = Avtaletype.Forhaandsgodkjent,
                    startDato = avtaleDbo.startDato.plusDays(4),
                )

                val previous = avtaler.get(avtaleDbo.id)
                val formatertDato = startDatoForGjennomforing.formaterDatoTilEuropeiskDatoformat()
                validator.validate(dbo, previous).shouldBeLeft().shouldContainExactlyInAnyOrder(
                    listOf(
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
                    ),
                )
            }

            test("skal godta at gjennomføring har andre Nav-enheter enn avtalen") {
                gjennomforinger.upsert(
                    gjennomforing.copy(navRegion = "0400"),
                )

                val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer, unleash)

                val dbo = avtaleDbo.copy(
                    navEnheter = listOf("0400"),
                )

                val previous = avtaler.get(avtaleDbo.id)
                validator.validate(dbo, previous).shouldBeRight()
            }
        }
    }
})
