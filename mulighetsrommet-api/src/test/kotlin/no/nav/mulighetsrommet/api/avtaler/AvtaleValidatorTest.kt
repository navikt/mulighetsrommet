package no.nav.mulighetsrommet.api.avtaler

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.repositories.*
import no.nav.mulighetsrommet.api.routes.v1.responses.ValidationError
import no.nav.mulighetsrommet.api.services.NavEnhetService
import no.nav.mulighetsrommet.api.services.TiltakstypeService
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.dto.Websaknummer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

class AvtaleValidatorTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val domain = MulighetsrommetTestDomain(
        enheter = listOf(
            NavEnhetDbo(
                navn = "NAV Oslo",
                enhetsnummer = "0300",
                status = NavEnhetStatus.AKTIV,
                type = Norg2Type.FYLKE,
                overordnetEnhet = null,
            ),
            NavEnhetDbo(
                navn = "NAV Innlandet",
                enhetsnummer = "0400",
                status = NavEnhetStatus.AKTIV,
                type = Norg2Type.FYLKE,
                overordnetEnhet = null,
            ),
            NavEnhetDbo(
                navn = "NAV Gjøvik",
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
        nusData = null,
    )

    lateinit var navEnheterService: NavEnhetService
    lateinit var tiltakstyper: TiltakstypeService
    lateinit var avtaler: AvtaleRepository
    lateinit var gjennomforinger: TiltaksgjennomforingRepository
    lateinit var arrangorer: ArrangorRepository

    beforeEach {
        domain.initialize(database.db)

        tiltakstyper = TiltakstypeService(TiltakstypeRepository(database.db), listOf(Tiltakskode.OPPFOLGING))
        navEnheterService = NavEnhetService(NavEnhetRepository(database.db))
        avtaler = AvtaleRepository(database.db)
        gjennomforinger = TiltaksgjennomforingRepository(database.db)
        arrangorer = ArrangorRepository(database.db)
    }

    afterEach {
        database.db.truncateAll()
    }

    test("skal feile når tiltakstypen ikke er aktivert") {
        tiltakstyper = TiltakstypeService(TiltakstypeRepository(database.db), emptyList())
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer)

        val dbo = avtaleDbo.copy(
            tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        )

        validator.validate(dbo, null).shouldBeLeft().shouldContainAll(
            ValidationError(
                "tiltakstypeId",
                "Opprettelse av avtale for tiltakstype: 'Oppfølging' er ikke skrudd på enda.",
            ),
        )
    }

    test("skal ikke feile når tiltakstypen er AFT, VTA, eller aktivert") {
        tiltakstyper = TiltakstypeService(TiltakstypeRepository(database.db), listOf(Tiltakskode.OPPFOLGING))
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer)

        validator.validate(AvtaleFixtures.AFT, null).shouldBeRight()
        validator.validate(AvtaleFixtures.VTA, null).shouldBeRight()
        validator.validate(AvtaleFixtures.oppfolging, null).shouldBeRight()
    }

    test("should accumulate errors when dbo has multiple issues") {
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer)

        val dbo = avtaleDbo.copy(
            startDato = LocalDate.of(2023, 1, 1),
            sluttDato = LocalDate.of(2020, 1, 1),
            navEnheter = emptyList(),
            arrangorUnderenheter = emptyList(),
        )

        validator.validate(dbo, null).shouldBeLeft().shouldContainAll(
            listOf(
                ValidationError("startDato", "Startdato må være før sluttdato"),
                ValidationError("navEnheter", "Du må velge minst én NAV-region"),
                ValidationError("arrangorUnderenheter", "Du må velge minst én underenhet for tiltaksarrangør"),
            ),
        )
    }

    test("Avtalenavn må være minst 5 tegn når avtalen er opprettet i Admin-flate") {
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer)

        val dbo = avtaleDbo.copy(navn = "Avt")

        validator.validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(
                ValidationError("navn", "Avtalenavn må være minst 5 tegn langt"),
            ),
        )
    }

    test("Avtalens startdato må være før eller lik som sluttdato") {
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer)

        val dagensDato = LocalDate.now()
        val dbo = avtaleDbo.copy(startDato = dagensDato, sluttDato = dagensDato)

        validator.validate(dbo, null).shouldBeRight()

        val dbo2 = avtaleDbo.copy(startDato = dagensDato.plusDays(5), sluttDato = dagensDato)

        validator.validate(dbo2, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(ValidationError("startDato", "Startdato må være før sluttdato")),
        )
    }

    test("Avtalens lengde er maks 5 år for ikke AFT/VTA") {
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer)

        val dagensDato = LocalDate.now()
        val dbo = avtaleDbo.copy(startDato = dagensDato, sluttDato = dagensDato.plusYears(5))

        validator.validate(dbo, null).shouldBeRight()

        val dbo2 = avtaleDbo.copy(startDato = dagensDato, sluttDato = dagensDato.plusYears(6))

        validator.validate(dbo2, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(ValidationError("sluttDato", "Avtaleperioden kan ikke vare lenger enn 5 år for anskaffede tiltak")),
        )
    }

    test("Avtalens sluttdato være lik eller etter startdato") {
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer)

        val dagensDato = LocalDate.now()
        val dbo = avtaleDbo.copy(startDato = dagensDato, sluttDato = dagensDato.minusDays(5))

        validator.validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(ValidationError("startDato", "Startdato må være før sluttdato")),
        )

        val dbo2 = avtaleDbo.copy(startDato = dagensDato, sluttDato = dagensDato)
        validator.validate(dbo2, null).shouldBeRight()
    }

    test("skal validere at NAV-enheter må være koblet til NAV-fylke") {
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer)

        val dbo = avtaleDbo.copy(
            navEnheter = listOf("0300", "0502"),
        )

        validator.validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(
                ValidationError("navEnheter", "NAV-enheten 0502 passer ikke i avtalens kontorstruktur"),
            ),
        )
    }

    test("sluttDato er påkrevd hvis ikke forhåndsgodkjent") {
        val validator = AvtaleValidator(
            TiltakstypeService(TiltakstypeRepository(database.db), Tiltakskode.values().toList()),
            gjennomforinger,
            navEnheterService,
            arrangorer,
        )
        val forhaandsgodkjent = AvtaleFixtures.AFT.copy(sluttDato = null)
        val rammeAvtale = AvtaleFixtures.oppfolging.copy(sluttDato = null)
        val avtale = AvtaleFixtures.oppfolgingMedAvtale.copy(sluttDato = null)
        val offentligOffentlig = AvtaleFixtures.gruppeAmo.copy(sluttDato = null)

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

    test("avtaletype må være allowed") {
        val validator = AvtaleValidator(
            TiltakstypeService(TiltakstypeRepository(database.db), Tiltakskode.entries),
            gjennomforinger,
            navEnheterService,
            arrangorer,
        )

        val aft = AvtaleFixtures.AFT.copy(avtaletype = Avtaletype.Rammeavtale)
        val vta = AvtaleFixtures.VTA.copy(avtaletype = Avtaletype.Avtale)
        val oppfolging = AvtaleFixtures.oppfolging.copy(avtaletype = Avtaletype.OffentligOffentlig)
        val gruppeAmo = AvtaleFixtures.gruppeAmo.copy(avtaletype = Avtaletype.Forhaandsgodkjent)
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
        val gruppeAmoOffentlig = AvtaleFixtures.gruppeAmo.copy(avtaletype = Avtaletype.OffentligOffentlig)
        validator.validate(aftForhaands, null).shouldBeRight()
        validator.validate(vtaForhaands, null).shouldBeRight()
        validator.validate(oppfolgingRamme, null).shouldBeRight()
        validator.validate(gruppeAmoOffentlig, null).shouldBeRight()
    }

    test("Websak-referanse må være med når avtalen er avtale eller rammeavtale") {
        val validator = AvtaleValidator(
            TiltakstypeService(TiltakstypeRepository(database.db), Tiltakskode.entries),
            gjennomforinger,
            navEnheterService,
            arrangorer,
        )

        val rammeavtale = AvtaleFixtures.oppfolging.copy(avtaletype = Avtaletype.Rammeavtale, websaknummer = null)
        validator.validate(rammeavtale, null).shouldBeLeft(
            listOf(ValidationError("websaknummer", "Du må skrive inn Websaknummer til avtalesaken")),
        )

        val avtale = AvtaleFixtures.oppfolging.copy(avtaletype = Avtaletype.Avtale, websaknummer = null)
        validator.validate(avtale, null).shouldBeLeft(
            listOf(ValidationError("websaknummer", "Du må skrive inn Websaknummer til avtalesaken")),
        )

        val offentligOffentligSamarbeid =
            AvtaleFixtures.gruppeAmo.copy(avtaletype = Avtaletype.OffentligOffentlig, websaknummer = null)
        validator.validate(offentligOffentligSamarbeid, null).shouldBeRight()
    }

    test("arrangørens underenheter må tilhøre hovedenhet i Brreg") {
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer)

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

        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer)

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

    context("når avtalen allerede eksisterer") {
        test("skal kunne endre felter med opphav fra Arena når vi har tatt eierskap til tiltakstypen") {
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
                nusData = null,
            )

            avtaler.upsert(avtaleDbo.copy(administratorer = listOf()))
            avtaler.setOpphav(avtaleDbo.id, ArenaMigrering.Opphav.ARENA)

            val validator = AvtaleValidator(
                TiltakstypeService(
                    TiltakstypeRepository(database.db),
                    listOf(Tiltakskode.OPPFOLGING, Tiltakskode.ARBEIDSFORBEREDENDE_TRENING),
                ),
                gjennomforinger,
                navEnheterService,
                arrangorer,
            )

            val previous = avtaler.get(avtaleDbo.id)
            validator.validate(avtaleMedEndringer, previous).shouldBeRight()
        }

        test("skal ikke kunne endre felter med opphav fra Arena når vi ikke har tatt eierskap til tiltakstypen") {
            val avtaleMedEndringer = AvtaleDbo(
                id = avtaleDbo.id,
                navn = "Nytt navn",
                tiltakstypeId = TiltakstypeFixtures.Jobbklubb.id,
                arrangorId = ArrangorFixtures.underenhet1.id,
                arrangorUnderenheter = listOf(ArrangorFixtures.underenhet1.id),
                arrangorKontaktpersoner = emptyList(),
                avtalenummer = "123456",
                websaknummer = Websaknummer("24/1234"),
                startDato = LocalDate.now(),
                sluttDato = LocalDate.now().plusYears(1),
                administratorer = listOf(NavIdent("B123456")),
                avtaletype = Avtaletype.Avtale,
                prisbetingelser = null,
                navEnheter = listOf("0300"),
                antallPlasser = null,
                beskrivelse = null,
                faneinnhold = null,
                personopplysninger = emptyList(),
                personvernBekreftet = false,
                nusData = null,
            )

            avtaler.upsert(avtaleDbo.copy(administratorer = listOf(), tiltakstypeId = TiltakstypeFixtures.Jobbklubb.id))
            avtaler.setOpphav(avtaleDbo.id, ArenaMigrering.Opphav.ARENA)

            val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, arrangorer)

            val previous = avtaler.get(avtaleDbo.id)
            validator.validate(avtaleMedEndringer, previous).shouldBeLeft().shouldContainExactlyInAnyOrder(
                ValidationError("navn", "Navn kan ikke endres utenfor Arena"),
                ValidationError("startDato", "Startdato kan ikke endres utenfor Arena"),
                ValidationError("sluttDato", "Sluttdato kan ikke endres utenfor Arena"),
                ValidationError("avtaletype", "Avtaletype kan ikke endres utenfor Arena"),
                ValidationError("arrangorId", "Tiltaksarrangøren kan ikke endres utenfor Arena"),
            )
        }

        context("når avtalen har gjennomføringer") {
            val startDatoForGjennomforing = avtaleDbo.startDato
            beforeAny {
                avtaler.upsert(avtaleDbo.copy(administratorer = listOf()))
                gjennomforinger.upsert(
                    TiltaksgjennomforingFixtures.Oppfolging1.copy(
                        administratorer = emptyList(),
                        avtaleId = avtaleDbo.id,
                        arrangorId = ArrangorFixtures.underenhet2.id,
                        navRegion = "0400",
                        navEnheter = listOf("0502"),
                        startDato = startDatoForGjennomforing,
                    ),
                )
            }

            afterAny {
                gjennomforinger.delete(TiltaksgjennomforingFixtures.Oppfolging1.id)
            }

            test("skal validere at data samsvarer med avtalens gjennomføringer") {
                val validator = AvtaleValidator(
                    TiltakstypeService(
                        TiltakstypeRepository(database.db),
                        listOf(Tiltakskode.OPPFOLGING, Tiltakskode.ARBEIDSFORBEREDENDE_TRENING),
                    ),
                    gjennomforinger,
                    navEnheterService,
                    arrangorer,
                )

                val dbo = avtaleDbo.copy(
                    tiltakstypeId = TiltakstypeFixtures.AFT.id,
                    avtaletype = Avtaletype.Forhaandsgodkjent,
                    navEnheter = listOf("0400"),
                    startDato = avtaleDbo.startDato.plusDays(4),
                )

                val previous = avtaler.get(avtaleDbo.id)
                val formatertDato = startDatoForGjennomforing.format(
                    DateTimeFormatter.ofLocalizedDate(
                        FormatStyle.SHORT,
                    ),
                )
                validator.validate(dbo, previous).shouldBeLeft().shouldContainExactlyInAnyOrder(
                    listOf(
                        ValidationError(
                            "tiltakstypeId",
                            "Tiltakstype kan ikke endres fordi det finnes gjennomføringer for avtalen",
                        ),
                        ValidationError(
                            "avtaletype",
                            "Avtaletype kan ikke endres fordi det finnes gjennomføringer for avtalen",
                        ),
                        ValidationError(
                            "arrangorUnderenheter",
                            "Arrangøren Underenhet 2 AS er i bruk på en av avtalens gjennomføringer, men mangler blant tiltaksarrangørens underenheter",
                        ),
                        ValidationError(
                            "navEnheter",
                            "NAV-enheten 0502 er i bruk på en av avtalens gjennomføringer, men mangler blant avtalens NAV-enheter",
                        ),
                        ValidationError(
                            "startDato",
                            "Startdato kan ikke være før startdatoen til tiltaksgjennomføringer koblet til avtalen. Minst en gjennomføring har startdato: $formatertDato",
                        ),
                    ),
                )
            }
        }
    }
})
