package no.nav.mulighetsrommet.api.domain.avtale

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.mulighetsrommet.api.domain.opplaring.OpplaringKategorisering
import no.nav.mulighetsrommet.api.domain.testing.fixture.AvtaleFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavEnhetFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.PrismodellFixtures
import no.nav.mulighetsrommet.api.domain.tiltak.PrismodellType
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Personopplysning
import no.nav.mulighetsrommet.model.SakarkivNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.validation.FieldError
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class AvtaleTest : FunSpec({
    context("medRammedetaljer") {
        test("kan ikke legges til for systembestemte (forhåndsgodkjente) avtaler") {
            val avtale = AvtaleFixtures.AFT

            avtale.medRammedetaljer(totalRamme = 1000, utbetaltArena = null).shouldBeLeft(
                listOf(
                    FieldError(
                        "/totalRamme",
                        "Rammedetaljer kan kun legges til anskaffet avtaler",
                    ),
                ),
            )
        }

        test("krever at alle prismodeller på avtalen har samme valuta") {
            val avtale = AvtaleFixtures.ARR.copy(
                prisinfo = Avtale.Prisinfo.Egendefinert(
                    listOf(
                        PrismodellFixtures.AvtaltPrisPerManedsverk.copy(valuta = Valuta.NOK),
                        PrismodellFixtures.AnnenAvtaltPris.copy(valuta = Valuta.SEK),
                    ),
                ),
            )

            avtale.medRammedetaljer(totalRamme = 1000, utbetaltArena = null).shouldBeLeft(
                listOf(
                    FieldError(
                        "/totalRamme",
                        "Rammedetaljer kan kun legges til avtaler med én type valuta på prismodellene",
                    ),
                ),
            )
        }

        test("total ramme må være et positivt beløp") {
            val avtale = AvtaleFixtures.ARR.copy(
                prisinfo = Avtale.Prisinfo.Egendefinert(
                    listOf(PrismodellFixtures.AvtaltPrisPerManedsverk.copy(valuta = Valuta.NOK)),
                ),
            )

            avtale.medRammedetaljer(totalRamme = -1, utbetaltArena = null).shouldBeLeft(
                listOf(
                    FieldError("/totalRamme", "Total ramme må være et positivt beløp"),
                ),
            )
        }

        test("utbetalt beløp fra Arena må være et positivt beløp") {
            val avtale = AvtaleFixtures.ARR.copy(
                prisinfo = Avtale.Prisinfo.Egendefinert(
                    listOf(PrismodellFixtures.AvtaltPrisPerManedsverk.copy(valuta = Valuta.NOK)),
                ),
            )

            avtale.medRammedetaljer(totalRamme = 1000, utbetaltArena = -1).shouldBeLeft(
                listOf(
                    FieldError("/utbetaltArena", "Utbetalt beløp fra Arena må være et positivt beløp"),
                ),
            )
        }

        test("fjerner rammedetaljer når begge felt er null") {
            val avtale = AvtaleFixtures.ARR.copy(
                prisinfo = Avtale.Prisinfo.Egendefinert(
                    listOf(PrismodellFixtures.AvtaltPrisPerManedsverk.copy(valuta = Valuta.NOK)),
                ),
                rammedetaljer = Avtale.Rammedetaljer(totalRamme = 1000, utbetaltArena = 100, valuta = Valuta.NOK),
            )

            val oppdatert = avtale.medRammedetaljer(totalRamme = null, utbetaltArena = null).shouldBeRight()

            oppdatert.rammedetaljer.shouldBeNull()
        }

        test("samler opp alle feil samtidig") {
            val avtale = AvtaleFixtures.AFT

            avtale.medRammedetaljer(totalRamme = -1, utbetaltArena = -1).shouldBeLeft().shouldContainExactlyInAnyOrder(
                listOf(
                    FieldError("/totalRamme", "Rammedetaljer kan kun legges til anskaffet avtaler"),
                    FieldError("/totalRamme", "Total ramme må være et positivt beløp"),
                    FieldError("/utbetaltArena", "Utbetalt beløp fra Arena må være et positivt beløp"),
                ),
            )
        }

        test("legger til rammedetaljer med valuta hentet fra prismodellene når validering går bra") {
            val avtale = AvtaleFixtures.ARR.copy(
                prisinfo = Avtale.Prisinfo.Egendefinert(
                    listOf(PrismodellFixtures.AvtaltPrisPerManedsverk.copy(valuta = Valuta.NOK)),
                ),
            )

            val oppdatert = avtale.medRammedetaljer(totalRamme = 1000, utbetaltArena = null).shouldBeRight()

            oppdatert.rammedetaljer shouldBe Avtale.Rammedetaljer(
                totalRamme = 1000,
                utbetaltArena = null,
                valuta = Valuta.NOK,
            )
        }
    }

    context("medPrismodeller") {
        test("kan ikke endres for forhåndsgodkjente avtaler") {
            val avtale = AvtaleFixtures.AFT

            avtale.medPrismodeller(listOf(PrismodellFixtures.AnnenAvtaltPris)).shouldBeLeft(
                listOf(FieldError.of("Prismodell kan ikke endres for forhåndsgodkjente avtaler")),
            )
        }

        test("krever minst én prismodell") {
            val avtale = AvtaleFixtures.oppfolging

            avtale.medPrismodeller(emptyList()).shouldBeLeft(
                listOf(FieldError("/prismodeller", "Minst én prismodell er påkrevd")),
            )
        }

        test("prismodelltype må være tillatt for avtalens tiltakskode") {
            val avtale = AvtaleFixtures.oppfolging

            avtale.medPrismodeller(
                listOf(PrismodellFixtures.createPrismodell(type = PrismodellType.FAST_SATS_PER_AVTALT_PLASS_PER_MANED)),
            ).shouldBeLeft(
                listOf(
                    FieldError(
                        "/prismodeller/0/type",
                        "Fast sats per avtalt tiltaksplass per måned er ikke tillatt for tiltakskode OPPFOLGING",
                    ),
                ),
            )
        }

        test("FAST_SATS_PER_BENYTTET_PLASS_PER_MANED er forbeholdt systembestemte prismodeller") {
            val avtale = AvtaleFixtures.AFT.copy(
                avtaletype = Avtaletype.RAMMEAVTALE,
                prisinfo = Avtale.Prisinfo.Egendefinert(listOf(PrismodellFixtures.ForhandsgodkjentAft)),
            )

            avtale.medPrismodeller(listOf(PrismodellFixtures.ForhandsgodkjentAft)).shouldBeLeft(
                listOf(
                    FieldError(
                        "/prismodeller",
                        "Prismodell kan ikke opprettes med typen Fast sats per benyttet tiltaksplass per måned",
                    ),
                ),
            )
        }

        test("oppdaterer prisinfo til egendefinerte prismodeller når validering går bra") {
            val avtale = AvtaleFixtures.oppfolging

            val oppdatert = avtale.medPrismodeller(listOf(PrismodellFixtures.AnnenAvtaltPris)).shouldBeRight()

            oppdatert.prisinfo shouldBe Avtale.Prisinfo.Egendefinert(listOf(PrismodellFixtures.AnnenAvtaltPris))
        }
    }

    context("Personvern.of") {
        test("krever beskrivelse når ANNET er valgt") {
            Avtale.Personvern.of(
                personopplysninger = setOf(Personopplysning.Type.ANNET),
                annetBeskrivelse = null,
                erBekreftet = true,
            ).shouldBeLeft(
                listOf(FieldError("/personvern/annetBeskrivelse", "Beskrivelse er påkrevd når annet er valgt")),
            )

            Avtale.Personvern.of(
                personopplysninger = setOf(Personopplysning.Type.ANNET),
                annetBeskrivelse = "   ",
                erBekreftet = true,
            ).shouldBeLeft(
                listOf(FieldError("/personvern/annetBeskrivelse", "Beskrivelse er påkrevd når annet er valgt")),
            )
        }

        test("beskrivelse kan ikke være lengre enn 300 tegn") {
            Avtale.Personvern.of(
                personopplysninger = setOf(Personopplysning.Type.ANNET),
                annetBeskrivelse = "a".repeat(301),
                erBekreftet = true,
            ).shouldBeLeft(
                listOf(FieldError("/personvern/annetBeskrivelse", "Beskrivelse kan maks være 300 tegn")),
            )
        }

        test("nullstiller beskrivelse når ANNET ikke er valgt") {
            val personvern = Avtale.Personvern.of(
                personopplysninger = setOf(Personopplysning.Type.NAVN),
                annetBeskrivelse = "Skal fjernes",
                erBekreftet = true,
            ).shouldBeRight()

            personvern.annetBeskrivelse.shouldBeNull()
        }

        test("oppretter personvern når validering går bra") {
            val personvern = Avtale.Personvern.of(
                personopplysninger = setOf(Personopplysning.Type.ANNET),
                annetBeskrivelse = "En beskrivelse",
                erBekreftet = true,
            ).shouldBeRight()

            personvern shouldBe Avtale.Personvern(
                personopplysninger = setOf(Personopplysning.Type.ANNET),
                annetBeskrivelse = "En beskrivelse",
                erBekreftet = true,
            )
        }
    }

    context("VeilederInfo.of") {
        test("krever minst én Nav-region") {
            Avtale.VeilederInfo.of(
                beskrivelse = null,
                faneinnhold = null,
                navEnheter = setOf(NavEnhetFixtures.Gjovik),
            ).shouldBeLeft(
                listOf(
                    FieldError("/veilederinformasjon/navRegioner", "Du må velge minst én Nav-region"),
                    FieldError("/veilederinformasjon/navKontorer", "Du må velge minst én Nav-enhet"),
                ),
            )
        }

        test("krever minst én Nav-enhet innenfor de valgte regionene") {
            Avtale.VeilederInfo.of(
                beskrivelse = null,
                faneinnhold = null,
                navEnheter = setOf(NavEnhetFixtures.Innlandet),
            ).shouldBeLeft(
                listOf(FieldError("/veilederinformasjon/navKontorer", "Du må velge minst én Nav-enhet")),
            )
        }

        test("akkumulerer feil når verken region eller enhet er valgt") {
            Avtale.VeilederInfo.of(
                beskrivelse = null,
                faneinnhold = null,
                navEnheter = setOf(),
            ).shouldBeLeft(
                listOf(
                    FieldError("/veilederinformasjon/navRegioner", "Du må velge minst én Nav-region"),
                    FieldError("/veilederinformasjon/navKontorer", "Du må velge minst én Nav-enhet"),
                ),
            )
        }

        test("filtrerer bort enheter som ikke er regioner eller underenheter av valgte regioner") {
            val veilederinfo = Avtale.VeilederInfo.of(
                beskrivelse = "Beskrivelse",
                faneinnhold = null,
                navEnheter = setOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik, NavEnhetFixtures.TiltakOslo),
            ).shouldBeRight()

            veilederinfo.navEnheter shouldContainExactlyInAnyOrder setOf(
                NavEnhetFixtures.Innlandet.enhetsnummer,
                NavEnhetFixtures.Gjovik.enhetsnummer,
            )
        }
    }

    context("Avtale.opprett") {
        fun opprett(
            id: UUID = UUID.randomUUID(),
            tiltakskode: Tiltakskode = AvtaleFixtures.oppfolging.tiltakskode,
            navn: String = "En avtale",
            sakarkivNummer: SakarkivNummer? = null,
            arrangor: Avtale.Arrangor? = AvtaleFixtures.defaultArrangor,
            startDato: LocalDate = LocalDate.of(2024, 1, 1),
            opsjonsmodell: Opsjonsmodell = Opsjonsmodell.ValgfriSluttdato(LocalDate.of(2024, 12, 31)),
            avtaletype: Avtaletype = Avtaletype.RAMMEAVTALE,
            administratorer: Set<NavIdent> = setOf(NavIdent("DD1")),
            veilederinfo: Avtale.VeilederInfo = AvtaleFixtures.defaultVeilederinfo,
            personvern: Avtale.Personvern = AvtaleFixtures.defaultPersonvern,
            opplaring: OpplaringKategorisering? = null,
            prisinfo: Avtale.Prisinfo = Avtale.Prisinfo.Egendefinert(
                listOf(PrismodellFixtures.AvtaltPrisPerTimeOppfolging),
            ),
            vurderingsdato: LocalDate = startDato,
        ) = Avtale.opprett(
            command = OpprettAvtaleCommand(
                id = id,
                tiltakskode = tiltakskode,
                navn = navn,
                sakarkivNummer = sakarkivNummer,
                arrangor = arrangor,
                startDato = startDato,
                opsjonsmodell = opsjonsmodell,
                avtaletype = avtaletype,
                administratorer = administratorer,
                veilederinfo = veilederinfo,
                personvern = personvern,
                opplaring = opplaring,
                prisinfo = prisinfo,
            ),
            vurderingsdato = Vurderingsdato(vurderingsdato),
        )

        test("setter sluttdato basert på opsjonsmodellen når avtalen opprettes") {
            val startDato = LocalDate.of(2024, 1, 1)

            val medToPlussEn = opprett(
                startDato = startDato,
                opsjonsmodell = Opsjonsmodell.ToPlussEn.forStartDato(startDato),
            ).shouldBeRight()

            medToPlussEn.sluttDato shouldBe LocalDate.of(2025, 12, 31)
            medToPlussEn.opsjoner.registreringer.shouldBeEmpty()
        }

        test("krever at startdato er før sluttdato") {
            val startDato = LocalDate.of(2024, 1, 1)

            opprett(
                startDato = startDato,
                opsjonsmodell = Opsjonsmodell.IngenOpsjonsmulighet(sluttDato = startDato.minusDays(1)),
            ).shouldBeLeft(
                listOf(FieldError.of("Startdato må være før sluttdato", Avtale::startDato)),
            )
        }

        test("krever opsjonsmodell med valgfri sluttdato når avtalen er forhåndsgodkjent") {
            val startDato = LocalDate.of(2024, 1, 1)

            opprett(
                startDato = startDato,
                opsjonsmodell = Opsjonsmodell.ValgfriSluttdato(sluttDato = null),
                avtaletype = Avtaletype.FORHANDSGODKJENT,
            ).shouldBeRight()

            opprett(
                startDato = startDato,
                opsjonsmodell = Opsjonsmodell.ToPlussEn.forStartDato(startDato),
                avtaletype = Avtaletype.FORHANDSGODKJENT,
            ).shouldBeLeft(
                listOf(
                    FieldError.of(
                        "Du må velge opsjonsmodell med valgfri sluttdato når avtalen er forhåndsgodkjent",
                        Avtale::opsjoner,
                    ),
                ),
            )
        }

        test("setter status basert på arrangør og sluttdato") {
            val startDato = LocalDate.of(2024, 1, 1)

            opprett(
                arrangor = null,
                startDato = startDato,
                opsjonsmodell = Opsjonsmodell.ValgfriSluttdato(startDato.plusYears(1)),
            ).shouldBeRight().status shouldBe AvtaleStatus.Utkast

            opprett(
                arrangor = AvtaleFixtures.defaultArrangor,
                startDato = startDato,
                opsjonsmodell = Opsjonsmodell.ValgfriSluttdato(startDato.plusYears(1)),
            ).shouldBeRight().status shouldBe AvtaleStatus.Aktiv

            opprett(
                arrangor = AvtaleFixtures.defaultArrangor,
                startDato = startDato,
                opsjonsmodell = Opsjonsmodell.ValgfriSluttdato(startDato.plusDays(1)),
                vurderingsdato = startDato.plusDays(2),
            ).shouldBeRight().status shouldBe AvtaleStatus.Avsluttet
        }
    }

    context("oppdaterDetaljer") {
        fun oppdaterDetaljer(
            avtale: Avtale = AvtaleFixtures.oppfolging,
            navn: String = avtale.navn,
            sakarkivNummer: SakarkivNummer? = avtale.sakarkivNummer,
            arrangor: Avtale.Arrangor? = avtale.arrangor,
            startDato: LocalDate = avtale.startDato,
            opsjonsmodell: Opsjonsmodell = avtale.opsjoner.modell,
            avtaletype: Avtaletype = avtale.avtaletype,
            administratorer: Set<NavIdent> = avtale.administratorer,
            opplaring: OpplaringKategorisering? = avtale.opplaring,
            vurderingsdato: LocalDate = avtale.startDato,
        ) = avtale.oppdaterDetaljer(
            command = OppdaterAvtaledetaljerCommand(
                navn = navn,
                sakarkivNummer = sakarkivNummer,
                arrangor = arrangor,
                startDato = startDato,
                opsjonsmodell = opsjonsmodell,
                avtaletype = avtaletype,
                administratorer = administratorer,
                opplaring = opplaring,
            ),
            vurderingsdato = Vurderingsdato(vurderingsdato),
        )

        test("kan endre startdato og opsjonsmodell sammen når ingen opsjoner er registrert") {
            val avtale = AvtaleFixtures.oppfolging
            val nyStartDato = avtale.startDato.plusMonths(1)

            val oppdatert = oppdaterDetaljer(
                avtale = avtale,
                startDato = nyStartDato,
                opsjonsmodell = Opsjonsmodell.ToPlussEn.forStartDato(nyStartDato),
            ).shouldBeRight()

            oppdatert.startDato shouldBe nyStartDato
            oppdatert.opsjoner.modell.shouldBeInstanceOf<Opsjonsmodell.ToPlussEn>()
                .opsjonMaksVarighet shouldBe LocalDate.of(2026, 1, 31)
            oppdatert.sluttDato shouldBe LocalDate.of(2025, 1, 31)
        }

        test("kan ikke endre startdato når opsjoner er registrert") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                opsjoner = Avtale.Opsjoner(
                    modell = Opsjonsmodell.ToPlussEn.forStartDato(AvtaleFixtures.oppfolging.startDato),
                    registreringer = listOf(
                        Avtale.Opsjon.SkalIkkeUtloseOpsjon(
                            id = UUID.randomUUID(),
                            registrertTidspunkt = Instant.now(),
                            forrigeSluttDato = LocalDate.of(2025, 1, 1),
                            registrertAv = NavIdent("B123456"),
                        ),
                    ),
                ),
            )

            oppdaterDetaljer(
                avtale = avtale,
                startDato = avtale.startDato.plusDays(1),
                opsjonsmodell = avtale.opsjoner.modell,
            ).shouldBeLeft(
                listOf(FieldError.of("Startdato kan ikke endres når opsjoner er registrert", Avtale::startDato)),
            )
        }

        test("kan ikke endre opsjonsmodell når opsjoner er registrert") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                opsjoner = Avtale.Opsjoner(
                    modell = Opsjonsmodell.ToPlussEn.forStartDato(AvtaleFixtures.oppfolging.startDato),
                    registreringer = listOf(
                        Avtale.Opsjon.SkalIkkeUtloseOpsjon(
                            id = UUID.randomUUID(),
                            registrertTidspunkt = Instant.now(),
                            forrigeSluttDato = LocalDate.of(2025, 1, 1),
                            registrertAv = NavIdent("B123456"),
                        ),
                    ),
                ),
            )

            oppdaterDetaljer(
                avtale = avtale,
                startDato = avtale.startDato,
                opsjonsmodell = Opsjonsmodell.ToPlussEnPlussEn.forStartDato(avtale.startDato),
            ).shouldBeLeft(
                listOf(FieldError.of("Opsjonsmodell kan ikke endres når opsjoner er registrert", Avtale::opsjoner)),
            )
        }

        test("beholder gjeldende sluttdato uendret når opsjoner er registrert") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                sluttDato = LocalDate.of(2026, 6, 1),
                opsjoner = Avtale.Opsjoner(
                    modell = Opsjonsmodell.ToPlussEn.forStartDato(AvtaleFixtures.oppfolging.startDato),
                    registreringer = listOf(
                        Avtale.Opsjon.OpsjonUtlost(
                            id = UUID.randomUUID(),
                            registrertTidspunkt = Instant.now(),
                            forrigeSluttDato = LocalDate.of(2025, 1, 1),
                            nySluttDato = LocalDate.of(2026, 6, 1),
                            registrertAv = NavIdent("B123456"),
                        ),
                    ),
                ),
            )

            val oppdatert = oppdaterDetaljer(avtale = avtale).shouldBeRight()

            oppdatert.sluttDato shouldBe LocalDate.of(2026, 6, 1)
        }

        test("krever opsjonsmodell med valgfri sluttdato når avtalen er forhåndsgodkjent") {
            val avtale = AvtaleFixtures.oppfolging.copy(avtaletype = Avtaletype.FORHANDSGODKJENT)

            oppdaterDetaljer(
                avtale = avtale,
                opsjonsmodell = Opsjonsmodell.ToPlussEn.forStartDato(avtale.startDato),
            ).shouldBeLeft(
                listOf(
                    FieldError.of(
                        "Du må velge opsjonsmodell med valgfri sluttdato når avtalen er forhåndsgodkjent",
                        Avtale::opsjoner,
                    ),
                ),
            )
        }

        test("setter status basert på arrangør og sluttdato") {
            val startDato = LocalDate.of(2024, 1, 1)
            val avtale = AvtaleFixtures.oppfolging.copy(startDato = startDato)

            oppdaterDetaljer(
                avtale = avtale,
                arrangor = null,
                opsjonsmodell = Opsjonsmodell.ValgfriSluttdato(startDato.plusYears(1)),
                vurderingsdato = startDato,
            ).shouldBeRight().status shouldBe AvtaleStatus.Utkast

            oppdaterDetaljer(
                avtale = avtale,
                arrangor = AvtaleFixtures.defaultArrangor,
                opsjonsmodell = Opsjonsmodell.ValgfriSluttdato(startDato.plusYears(1)),
                vurderingsdato = startDato,
            ).shouldBeRight().status shouldBe AvtaleStatus.Aktiv

            oppdaterDetaljer(
                avtale = avtale,
                arrangor = AvtaleFixtures.defaultArrangor,
                opsjonsmodell = Opsjonsmodell.ValgfriSluttdato(startDato.plusDays(1)),
                vurderingsdato = startDato.plusDays(2),
            ).shouldBeRight().status shouldBe AvtaleStatus.Avsluttet
        }

        test("beholder status Avbrutt ved oppdatering") {
            val avbrutt = AvtaleStatus.Avbrutt(LocalDateTime.now(), listOf(AvbrytAvtaleAarsak.ANNET), null)
            val avtale = AvtaleFixtures.oppfolging.copy(status = avbrutt)

            oppdaterDetaljer(avtale = avtale).shouldBeRight().status shouldBe avbrutt
        }
    }

    context("avslutt") {
        test("krever at avtalen er aktiv") {
            val avtale = AvtaleFixtures.oppfolging.copy(status = AvtaleStatus.Utkast)

            avtale.avslutt(LocalDateTime.now()).shouldBeLeft(
                listOf(
                    FieldError.of("Avtalen må være aktiv for å kunne avsluttes"),
                    FieldError.of("Avtalen kan ikke avsluttes før sluttdato"),
                ),
            )
        }

        test("krever at avsluttet-tidspunkt er etter sluttdato") {
            val avtale = AvtaleFixtures.oppfolging

            avtale.avslutt(avtale.sluttDato!!.atStartOfDay()).shouldBeLeft(
                listOf(FieldError.of("Avtalen kan ikke avsluttes før sluttdato")),
            )
        }

        test("setter status til Avsluttet når avtalen er aktiv og tidspunktet er etter sluttdato") {
            val avtale = AvtaleFixtures.oppfolging

            avtale.avslutt(avtale.sluttDato!!.plusDays(1).atStartOfDay())
                .shouldBeRight().status shouldBe AvtaleStatus.Avsluttet
        }
    }

    context("avbryt") {
        test("kan avbryte avtale som er Utkast eller Aktiv") {
            val tidspunkt = LocalDateTime.now()

            AvtaleFixtures.oppfolging.copy(status = AvtaleStatus.Utkast)
                .avbryt(tidspunkt, listOf(AvbrytAvtaleAarsak.ANNET), null)
                .shouldBeRight().status shouldBe AvtaleStatus.Avbrutt(tidspunkt, listOf(AvbrytAvtaleAarsak.ANNET), null)

            AvtaleFixtures.oppfolging.copy(status = AvtaleStatus.Aktiv)
                .avbryt(tidspunkt, listOf(AvbrytAvtaleAarsak.ANNET), null)
                .shouldBeRight().status shouldBe AvtaleStatus.Avbrutt(tidspunkt, listOf(AvbrytAvtaleAarsak.ANNET), null)
        }

        test("kan ikke avbryte avtale som allerede er avbrutt") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                status = AvtaleStatus.Avbrutt(LocalDateTime.now(), listOf(AvbrytAvtaleAarsak.ANNET), null),
            )

            avtale.avbryt(LocalDateTime.now(), listOf(AvbrytAvtaleAarsak.ANNET), null).shouldBeLeft(
                listOf(FieldError.of("Avtalen er allerede avbrutt")),
            )
        }

        test("kan ikke avbryte avtale som allerede er avsluttet") {
            val avtale = AvtaleFixtures.oppfolging.copy(status = AvtaleStatus.Avsluttet)

            avtale.avbryt(LocalDateTime.now(), listOf(AvbrytAvtaleAarsak.ANNET), null).shouldBeLeft(
                listOf(FieldError.of("Avtalen er allerede avsluttet")),
            )
        }
    }

    context("registrerOpsjon") {
        val today = LocalDate.of(2025, 1, 1)
        val registrertAv = NavIdent("B123456")

        test("kan ikke registreres for opsjonsmodell uten opsjonsmulighet") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                opsjoner = Avtale.Opsjoner(
                    modell = Opsjonsmodell.ValgfriSluttdato(sluttDato = today),
                    registreringer = emptyList(),
                ),
            )

            avtale.registrerOpsjon(
                command = RegistrerOpsjonCommand.UtloseOpsjon(today.plusMonths(1), registrertAv),
                vurderingsdato = Vurderingsdato(today),
            ).shouldBeLeft(
                listOf(FieldError.of("Opsjon kan ikke registreres for denne opsjonsmodellen")),
            )
        }

        test("ny sluttdato kan ikke være etter opsjonens maks varighet") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                opsjoner = Avtale.Opsjoner(
                    modell = Opsjonsmodell.Annet(
                        sluttDato = LocalDate.of(2025, 1, 31),
                        opsjonMaksVarighet = LocalDate.of(2025, 2, 1),
                        customOpsjonsmodellNavn = "Kort opsjonsmodell",
                    ),
                    registreringer = emptyList(),
                ),
            )

            avtale.registrerOpsjon(
                command = RegistrerOpsjonCommand.UtloseOpsjon(today.plusMonths(2), registrertAv),
                vurderingsdato = Vurderingsdato(today),
            ).shouldBeLeft(
                listOf(FieldError.of("Ny sluttdato er forbi maks varighet av avtalen")),
            )
        }

        test("kan ikke utløse flere opsjoner når det allerede er besluttet at ingen flere skal utløses") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                opsjoner = Avtale.Opsjoner(
                    modell = Opsjonsmodell.ToPlussEn.forStartDato(AvtaleFixtures.oppfolging.startDato),
                    registreringer = listOf(
                        Avtale.Opsjon.SkalIkkeUtloseOpsjon(
                            id = UUID.randomUUID(),
                            registrertTidspunkt = Instant.now(),
                            forrigeSluttDato = today,
                            registrertAv = registrertAv,
                        ),
                    ),
                ),
            )

            avtale.registrerOpsjon(
                command = RegistrerOpsjonCommand.UtloseOpsjon(today.plusMonths(1), registrertAv),
                vurderingsdato = Vurderingsdato(today),
            ).shouldBeLeft(
                listOf(FieldError.of("Kan ikke utløse flere opsjoner")),
            )
        }

        test("legger til opsjon i registreringer og oppdaterer varighet når validering går bra") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                opsjoner = Avtale.Opsjoner(
                    modell = Opsjonsmodell.ToPlussEn.forStartDato(AvtaleFixtures.oppfolging.startDato),
                    registreringer = emptyList(),
                ),
            )
            val forrigeSluttDato = avtale.sluttDato!!

            val oppdatert = avtale.registrerOpsjon(
                command = RegistrerOpsjonCommand.UtloseOpsjon(today.plusMonths(1), registrertAv),
                vurderingsdato = Vurderingsdato(today),
            ).shouldBeRight()

            oppdatert.sluttDato shouldBe today.plusMonths(1)
            oppdatert.opsjoner.registreringer shouldHaveSize 1
            oppdatert.opsjoner.registreringer[0].should {
                it.shouldBeInstanceOf<Avtale.Opsjon.OpsjonUtlost>()
                it.nySluttDato shouldBe today.plusMonths(1)
                it.forrigeSluttDato shouldBe forrigeSluttDato
                it.registrertAv shouldBe registrertAv
            }
        }

        test("registrerer at opsjon ikke skal utløses uten å endre avtalens varighet") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                opsjoner = Avtale.Opsjoner(
                    modell = Opsjonsmodell.ToPlussEn.forStartDato(AvtaleFixtures.oppfolging.startDato),
                    registreringer = emptyList(),
                ),
            )
            val opprinneligSluttDato = avtale.sluttDato

            val oppdatert = avtale.registrerOpsjon(
                command = RegistrerOpsjonCommand.SkalIkkeUtloseOpsjon(registrertAv),
                vurderingsdato = Vurderingsdato(today),
            ).shouldBeRight()

            oppdatert.sluttDato shouldBe opprinneligSluttDato
            oppdatert.opsjoner.registreringer shouldHaveSize 1
            oppdatert.opsjoner.registreringer[0].shouldBeInstanceOf<Avtale.Opsjon.SkalIkkeUtloseOpsjon>()
        }
    }

    context("slettOpsjon") {
        val today = LocalDate.of(2025, 1, 1)
        val registrertAv = NavIdent("B123456")

        test("kan bare slette den sist registrerte opsjonen") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                opsjoner = Avtale.Opsjoner(
                    modell = Opsjonsmodell.ToPlussEn.forStartDato(AvtaleFixtures.oppfolging.startDato),
                    registreringer = listOf(
                        Avtale.Opsjon.OpsjonUtlost(
                            id = UUID.randomUUID(),
                            registrertTidspunkt = Instant.now().minusSeconds(1),
                            nySluttDato = today.plusMonths(1),
                            forrigeSluttDato = today,
                            registrertAv = registrertAv,
                        ),
                        Avtale.Opsjon.OpsjonUtlost(
                            id = UUID.randomUUID(),
                            registrertTidspunkt = Instant.now(),
                            nySluttDato = today.plusMonths(2),
                            forrigeSluttDato = today.plusMonths(1),
                            registrertAv = registrertAv,
                        ),
                    ),
                ),
            )

            avtale.slettOpsjon(
                avtale.opsjoner.registreringer[0].id,
                Vurderingsdato(today),
            ).shouldBeLeft(
                listOf(FieldError.of("Opsjonen kan ikke slettes fordi det ikke er den siste utløste opsjonen")),
            )
        }

        test("fjerner opsjon fra registreringer og reverterer varighet når status var OPSJON_UTLOST") {
            val forrigeSluttDato = LocalDate.of(2025, 3, 1)
            val logg = Avtale.Opsjon.OpsjonUtlost(
                id = UUID.randomUUID(),
                registrertTidspunkt = Instant.now(),
                nySluttDato = LocalDate.of(2025, 6, 1),
                forrigeSluttDato = forrigeSluttDato,
                registrertAv = registrertAv,
            )
            val avtale = AvtaleFixtures.oppfolging.copy(
                sluttDato = logg.nySluttDato,
                opsjoner = Avtale.Opsjoner(
                    modell = Opsjonsmodell.ToPlussEn.forStartDato(AvtaleFixtures.oppfolging.startDato),
                    registreringer = listOf(logg),
                ),
            )

            val oppdatert = avtale.slettOpsjon(logg.id, Vurderingsdato(today)).shouldBeRight()

            oppdatert.sluttDato shouldBe forrigeSluttDato
            oppdatert.opsjoner.registreringer.shouldBeEmpty()
        }

        test("fjerner opsjon fra registreringer uten å endre varighet når status var SKAL_IKKE_UTLOSE_OPSJON") {
            val logg = Avtale.Opsjon.SkalIkkeUtloseOpsjon(
                id = UUID.randomUUID(),
                registrertTidspunkt = Instant.now(),
                forrigeSluttDato = today,
                registrertAv = registrertAv,
            )
            val avtale = AvtaleFixtures.oppfolging.copy(
                opsjoner = Avtale.Opsjoner(
                    modell = Opsjonsmodell.ToPlussEn.forStartDato(AvtaleFixtures.oppfolging.startDato),
                    registreringer = listOf(logg),
                ),
            )
            val opprinneligSluttDato = avtale.sluttDato

            val oppdatert = avtale.slettOpsjon(logg.id, Vurderingsdato(today)).shouldBeRight()

            oppdatert.sluttDato shouldBe opprinneligSluttDato
            oppdatert.opsjoner.registreringer.shouldBeEmpty()
        }
    }

    context("oppdaterVarighet") {
        test("setter avtalen til Aktiv når ny sluttdato ikke har passert") {
            val avtale = AvtaleFixtures.oppfolging.copy(status = AvtaleStatus.Avsluttet)
            val today = LocalDate.of(2025, 1, 1)

            val oppdatert = avtale.oppdaterVarighet(today.plusDays(1), Vurderingsdato(today))

            oppdatert.sluttDato shouldBe today.plusDays(1)
            oppdatert.status shouldBe AvtaleStatus.Aktiv
        }

        test("setter avtalen til Avsluttet når ny sluttdato har passert") {
            val avtale = AvtaleFixtures.oppfolging.copy(status = AvtaleStatus.Aktiv)
            val today = LocalDate.of(2025, 1, 1)

            val oppdatert = avtale.oppdaterVarighet(today.minusDays(1), Vurderingsdato(today))

            oppdatert.sluttDato shouldBe today.minusDays(1)
            oppdatert.status shouldBe AvtaleStatus.Avsluttet
        }

        test("beholder status Utkast og Avbrutt uansett ny sluttdato") {
            val today = LocalDate.of(2025, 1, 1)

            AvtaleFixtures.oppfolging.copy(status = AvtaleStatus.Utkast)
                .oppdaterVarighet(today.minusDays(1), Vurderingsdato(today)).status shouldBe AvtaleStatus.Utkast

            val avbrutt = AvtaleStatus.Avbrutt(LocalDateTime.now(), listOf(AvbrytAvtaleAarsak.ANNET), null)
            AvtaleFixtures.oppfolging.copy(status = avbrutt)
                .oppdaterVarighet(today.minusDays(1), Vurderingsdato(today)).status shouldBe avbrutt
        }
    }
})
