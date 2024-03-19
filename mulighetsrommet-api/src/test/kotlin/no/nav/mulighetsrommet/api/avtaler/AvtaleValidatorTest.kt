package no.nav.mulighetsrommet.api.avtaler

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
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
import java.time.LocalDate
import java.util.*

class AvtaleValidatorTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    lateinit var navEnheterService: NavEnhetService
    lateinit var tiltakstyper: TiltakstypeService
    lateinit var avtaler: AvtaleRepository
    lateinit var gjennomforinger: TiltaksgjennomforingRepository
    lateinit var virksomheter: VirksomhetRepository

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
        virksomheter = listOf(
            VirksomhetFixtures.hovedenhet,
            VirksomhetFixtures.underenhet1,
            VirksomhetFixtures.underenhet2,
        ),
        tiltakstyper = listOf(
            TiltakstypeFixtures.AFT,
            TiltakstypeFixtures.VTA,
            TiltakstypeFixtures.Oppfolging,
            TiltakstypeFixtures.Jobbklubb,
        ),
        avtaler = listOf(),
    )

    val avtaleDbo = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Avtale",
        tiltakstypeId = TiltakstypeFixtures.AFT.id,
        leverandorVirksomhetId = VirksomhetFixtures.hovedenhet.id,
        leverandorUnderenheter = listOf(VirksomhetFixtures.underenhet1.id),
        leverandorKontaktpersonId = null,
        avtalenummer = "123456",
        startDato = LocalDate.now().minusDays(1),
        sluttDato = LocalDate.now().plusMonths(1),
        url = "http://localhost:8080",
        administratorer = listOf(NavIdent("B123456")),
        avtaletype = Avtaletype.Avtale,
        prisbetingelser = null,
        navEnheter = listOf("0400", "0502"),
        antallPlasser = null,
        beskrivelse = null,
        faneinnhold = null,
    )

    beforeEach {
        domain.initialize(database.db)

        tiltakstyper = TiltakstypeService(TiltakstypeRepository(database.db), listOf(Tiltakskode.OPPFOLGING))
        navEnheterService = NavEnhetService(NavEnhetRepository(database.db))
        avtaler = AvtaleRepository(database.db)
        gjennomforinger = TiltaksgjennomforingRepository(database.db)
        virksomheter = VirksomhetRepository(database.db)
    }

    afterEach {
        database.db.truncateAll()
    }

    test("skal feile når tiltakstypen ikke er aktivert") {
        tiltakstyper = TiltakstypeService(TiltakstypeRepository(database.db), emptyList())
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, virksomheter)

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

    test("skal ikke feile når når tiltakstypen er AFT, VTA, eller aktivert") {
        tiltakstyper = TiltakstypeService(TiltakstypeRepository(database.db), listOf(Tiltakskode.OPPFOLGING))
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, virksomheter)

        forAll(
            row(TiltakstypeFixtures.AFT),
            row(TiltakstypeFixtures.VTA),
            row(TiltakstypeFixtures.Oppfolging),
        ) { tiltakstype ->
            val dbo = avtaleDbo.copy(tiltakstypeId = tiltakstype.id)

            validator.validate(dbo, null).shouldBeRight()
        }
    }

    test("should accumulate errors when dbo has multiple issues") {
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, virksomheter)

        val dbo = avtaleDbo.copy(
            startDato = LocalDate.of(2023, 1, 1),
            sluttDato = LocalDate.of(2020, 1, 1),
            navEnheter = emptyList(),
            leverandorUnderenheter = emptyList(),
        )

        validator.validate(dbo, null).shouldBeLeft().shouldContainAll(
            listOf(
                ValidationError("startDato", "Startdato må være før sluttdato"),
                ValidationError("navEnheter", "Minst én NAV-region må være valgt"),
                ValidationError("leverandorUnderenheter", "Minst én underenhet til leverandøren må være valgt"),
            ),
        )
    }

    test("Avtalenavn må være minst 5 tegn når avtalen er opprettet i Admin-flate") {
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, virksomheter)

        val dbo = avtaleDbo.copy(navn = "Avt")

        validator.validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(
                ValidationError("navn", "Avtalenavn må være minst 5 tegn langt"),
            ),
        )
    }

    test("Avtalens startdato må være før eller lik som sluttdato") {
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, virksomheter)

        val dagensDato = LocalDate.now()
        val dbo = avtaleDbo.copy(startDato = dagensDato, sluttDato = dagensDato)

        validator.validate(dbo, null).shouldBeRight()

        val dbo2 = avtaleDbo.copy(startDato = dagensDato.plusDays(5), sluttDato = dagensDato)

        validator.validate(dbo2, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(ValidationError("startDato", "Startdato må være før sluttdato")),
        )
    }

    test("Avtalens sluttdato være lik eller etter startdato") {
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, virksomheter)

        val dagensDato = LocalDate.now()
        val dbo = avtaleDbo.copy(startDato = dagensDato, sluttDato = dagensDato.minusDays(5))

        validator.validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(ValidationError("startDato", "Startdato må være før sluttdato")),
        )

        val dbo2 = avtaleDbo.copy(startDato = dagensDato, sluttDato = dagensDato)
        validator.validate(dbo2, null).shouldBeRight()
    }

    test("skal validere at NAV-enheter må være koblet til NAV-fylke") {
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, virksomheter)

        val dbo = avtaleDbo.copy(
            navEnheter = listOf("0300", "0502"),
        )

        validator.validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(
                ValidationError("navEnheter", "NAV-enheten 0502 passer ikke i avtalens kontorstruktur"),
            ),
        )
    }

    test("sluttDato er påkrevd hvis ikke VTA eller AFT") {
        val validator = AvtaleValidator(
            TiltakstypeService(TiltakstypeRepository(database.db), Tiltakskode.values().toList()),
            gjennomforinger,
            navEnheterService,
            virksomheter,
        )
        val aft = AvtaleFixtures.AFT.copy(sluttDato = null)
        val vta = AvtaleFixtures.VTA.copy(sluttDato = null)
        val oppfolging = AvtaleFixtures.oppfolging.copy(sluttDato = null)

        validator.validate(aft, null).shouldBeRight()
        validator.validate(vta, null).shouldBeRight()
        validator.validate(oppfolging, null).shouldBeLeft(
            listOf(ValidationError("sluttDato", "Sluttdato må være valgt")),
        )
    }

    context("når avtalen allerede eksisterer") {
        test("skal ikke kunne endre felter med opphav fra Arena") {
            val avtaleMedEndringer = AvtaleDbo(
                id = avtaleDbo.id,
                navn = "Nytt navn",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                leverandorVirksomhetId = VirksomhetFixtures.underenhet1.id,
                leverandorUnderenheter = listOf(VirksomhetFixtures.underenhet1.id),
                leverandorKontaktpersonId = null,
                avtalenummer = "123456",
                startDato = LocalDate.now(),
                sluttDato = LocalDate.now().plusYears(1),
                url = "nav.no",
                administratorer = listOf(NavIdent("B123456")),
                avtaletype = Avtaletype.Rammeavtale,
                prisbetingelser = null,
                navEnheter = listOf("0300"),
                antallPlasser = null,
                beskrivelse = null,
                faneinnhold = null,
            )

            avtaler.upsert(avtaleDbo.copy(administratorer = listOf()))
            avtaler.setOpphav(avtaleDbo.id, ArenaMigrering.Opphav.ARENA)

            val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, virksomheter)

            val previous = avtaler.get(avtaleDbo.id)
            validator.validate(avtaleMedEndringer, previous).shouldBeLeft().shouldContainExactlyInAnyOrder(
                listOf(
                    ValidationError("navn", "Navn kan ikke endres utenfor Arena"),
                    ValidationError("tiltakstypeId", "Tiltakstype kan ikke endres utenfor Arena"),
                    ValidationError("startDato", "Startdato kan ikke endres utenfor Arena"),
                    ValidationError("sluttDato", "Sluttdato kan ikke endres utenfor Arena"),
                    ValidationError("avtaletype", "Avtaletype kan ikke endres utenfor Arena"),
                    ValidationError("leverandorVirksomhetId", "Leverandøren kan ikke endres utenfor Arena"),
                ),
            )
        }

        context("når avtalen har gjennomføringer") {
            beforeAny {
                avtaler.upsert(avtaleDbo.copy(administratorer = listOf()))
                gjennomforinger.upsert(
                    TiltaksgjennomforingFixtures.Oppfolging1.copy(
                        administratorer = emptyList(),
                        avtaleId = avtaleDbo.id,
                        arrangorVirksomhetId = VirksomhetFixtures.underenhet2.id,
                        navRegion = "0400",
                        navEnheter = listOf("0502"),
                    ),
                )
            }

            afterAny {
                gjennomforinger.delete(TiltaksgjennomforingFixtures.Oppfolging1.id)
            }

            test("skal validere at data samsvarer med avtalens gjennomføringer") {
                val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService, virksomheter)

                val dbo = avtaleDbo.copy(
                    tiltakstypeId = TiltakstypeFixtures.VTA.id,
                    avtaletype = Avtaletype.Forhaandsgodkjent,
                    navEnheter = listOf("0400"),
                )

                val previous = avtaler.get(avtaleDbo.id)
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
                            "leverandorUnderenheter",
                            "Arrangøren Underenhet 2 AS er i bruk på en av avtalens gjennomføringer, men mangler blandt leverandørens underenheter",
                        ),
                        ValidationError(
                            "navEnheter",
                            "NAV-enheten 0502 er i bruk på en av avtalens gjennomføringer, men mangler blandt avtalens NAV-enheter",
                        ),
                    ),
                )
            }
        }
    }
})
