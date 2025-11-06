package no.nav.mulighetsrommet.api.avtale

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.avtale.AvtaleValidator.Ctx
import no.nav.mulighetsrommet.api.avtale.AvtaleValidator.Ctx.Tiltakstype
import no.nav.mulighetsrommet.api.avtale.api.AvtaleRequest
import no.nav.mulighetsrommet.api.avtale.api.PersonvernRequest
import no.nav.mulighetsrommet.api.avtale.api.VeilederinfoRequest
import no.nav.mulighetsrommet.api.avtale.mapper.AvtaleDboMapper
import no.nav.mulighetsrommet.api.avtale.model.*
import no.nav.mulighetsrommet.api.avtale.model.OpsjonLoggStatus
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.navenhet.toDto
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class AvtaleValidatorTest : FunSpec({
    val avtaleRequest = AvtaleRequest(
        id = UUID.randomUUID(),
        navn = "Avtale",
        tiltakskode = TiltakstypeFixtures.Oppfolging.tiltakskode!!,
        arrangor = AvtaleRequest.Arrangor(
            hovedenhet = ArrangorFixtures.hovedenhet.organisasjonsnummer,
            underenheter = listOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
            kontaktpersoner = emptyList(),
        ),
        sakarkivNummer = SakarkivNummer("24/1234"),
        startDato = LocalDate.now().minusDays(1),
        sluttDato = LocalDate.now().plusMonths(1),
        administratorer = listOf(NavAnsattFixture.DonaldDuck.navIdent),
        avtaletype = Avtaletype.RAMMEAVTALE,
        veilederinformasjon = VeilederinfoRequest(
            navEnheter = listOf(NavEnhetNummer("0400"), NavEnhetNummer("0502")),
            beskrivelse = null,
            faneinnhold = null,
        ),
        personvern = PersonvernRequest(
            personopplysninger = emptyList(),
            personvernBekreftet = false,
        ),
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
        avtaleRequest.arrangor,
        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
    )
    val forhaandsgodkjent = AvtaleDboMapper.toAvtaleRequest(
        AvtaleFixtures.AFT,
        avtaleRequest.arrangor,
        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
    )
    val avtaleTypeAvtale = AvtaleDboMapper.toAvtaleRequest(
        AvtaleFixtures.oppfolgingMedAvtale,
        avtaleRequest.arrangor,
        Tiltakskode.OPPFOLGING,
    )
    val oppfolgingMedRammeAvtale = AvtaleDboMapper.toAvtaleRequest(
        AvtaleFixtures.oppfolging,
        avtaleRequest.arrangor,
        Tiltakskode.OPPFOLGING,
    )
    val ctx = Ctx(
        previous = null,
        arrangor = ArrangorFixtures.hovedenhet.copy(
            underenheter = listOf(ArrangorFixtures.underenhet1),
        ),
        administratorer = emptyList(),
        tiltakstype = Tiltakstype(
            navn = TiltakstypeFixtures.Oppfolging.navn,
            id = TiltakstypeFixtures.Oppfolging.id,
        ),
        navEnheter = listOf(NavEnhetFixtures.Innlandet.toDto(), NavEnhetFixtures.Gjovik.toDto()),
    )

    val previous = Ctx.Avtale(
        status = AvtaleStatusType.AKTIV,
        opphav = ArenaMigrering.Opphav.TILTAKSADMINISTRASJON,
        opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.VALGFRI_SLUTTDATO, LocalDate.now().plusYears(4)),
        opsjonerRegistrert = emptyList(),
        avtaletype = Avtaletype.AVTALE,
        tiltakskode = TiltakstypeFixtures.GruppeAmo.tiltakskode!!,
        gjennomforinger = emptyList(),
    )

    test("should accumulate errors when request has multiple issues") {
        val request = avtaleRequest.copy(
            startDato = LocalDate.of(2023, 1, 1),
            sluttDato = LocalDate.of(2020, 1, 1),
            arrangor = AvtaleRequest.Arrangor(
                hovedenhet = ArrangorFixtures.hovedenhet.organisasjonsnummer,
                underenheter = emptyList(),
                kontaktpersoner = emptyList(),
            ),
        )

        AvtaleValidator.validate(
            request,
            ctx.copy(
                navEnheter = emptyList(),
            ),
        ).shouldBeLeft().shouldContainAll(
            listOf(
                FieldError("/startDato", "Startdato må være før sluttdato"),
                FieldError("/navRegioner", "Du må velge minst én Nav-region"),
                FieldError("/navKontorer", "Du må velge minst én Nav-enhet"),
                FieldError("/arrangorUnderenheter", "Du må velge minst én underenhet for tiltaksarrangør"),
            ),
        )
    }

    test("Avtalenavn må være minst 5 tegn når avtalen er opprettet i Admin-flate") {
        val request = avtaleRequest.copy(navn = "Avt")

        AvtaleValidator.validate(request, ctx).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(
                FieldError("/navn", "Avtalenavn må være minst 5 tegn langt"),
            ),
        )
    }

    test("Avtalens startdato må være før eller lik som sluttdato") {
        val dagensDato = LocalDate.now()
        val request = avtaleRequest.copy(startDato = dagensDato, sluttDato = dagensDato)

        AvtaleValidator.validate(request, ctx).shouldBeRight()

        val request2 = avtaleRequest.copy(startDato = dagensDato.plusDays(5), sluttDato = dagensDato)

        AvtaleValidator.validate(request2, ctx).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(FieldError("/startDato", "Startdato må være før sluttdato")),
        )
    }

    test("Avtalens sluttdato være lik eller etter startdato") {
        val dagensDato = LocalDate.now()
        val request = avtaleRequest.copy(startDato = dagensDato, sluttDato = dagensDato.minusDays(5))

        AvtaleValidator.validate(request, ctx).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(FieldError("/startDato", "Startdato må være før sluttdato")),
        )

        val request2 = avtaleRequest.copy(startDato = dagensDato, sluttDato = dagensDato)
        AvtaleValidator.validate(request2, ctx).shouldBeRight()
    }

    test("skal validere at Nav-fylke og Nav-enheter er påkrevd") {
        AvtaleValidator.validate(avtaleRequest, ctx.copy(navEnheter = emptyList())).shouldBeLeft().shouldContainExactlyInAnyOrder(
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

        AvtaleValidator.validate(forhaandsgodkjent1, ctx).shouldBeRight()
        AvtaleValidator.validate(oppfolgingMedRammeAvtale.copy(sluttDato = null), ctx).shouldBeLeft(
            listOf(FieldError("/sluttDato", "Du må legge inn sluttdato for avtalen")),
        )
        AvtaleValidator.validate(avtaleTypeAvtale.copy(sluttDato = null), ctx).shouldBeLeft(
            listOf(FieldError("/sluttDato", "Du må legge inn sluttdato for avtalen")),
        )
        AvtaleValidator.validate(offentligOffentlig.copy(sluttDato = null), ctx).shouldBeLeft(
            listOf(FieldError("/sluttDato", "Du må legge inn sluttdato for avtalen")),
        )
    }

    test("amoKategorisering er påkrevd hvis gruppe amo") {
        AvtaleValidator.validate(
            gruppeAmo.copy(amoKategorisering = null),
            ctx,
        ).shouldBeLeft(
            listOf(FieldError("/amoKategorisering.kurstype", "Du må velge en kurstype")),
        )
    }

    test("Opsjonsmodell må være VALGFRI_SLUTTDATO når avtale er forhåndsgodkjent") {
        AvtaleValidator.validate(
            forhaandsgodkjent.copy(opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.VALGFRI_SLUTTDATO, null)),
            ctx,
        ).shouldBeRight()
        AvtaleValidator.validate(
            forhaandsgodkjent.copy(opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN, null)),
            ctx,
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
        AvtaleValidator.validate(
            avtaleTypeAvtale.copy(opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN, null)),
            ctx,
        ).shouldBeLeft(
            listOf(
                FieldError("/opsjonsmodell/opsjonMaksVarighet", "Du må legge inn maks varighet for opsjonen"),
            ),
        )
        AvtaleValidator.validate(
            gruppeAmo.copy(
                avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG,
                opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.VALGFRI_SLUTTDATO, null),
            ),
            ctx,
        ).shouldBeRight()
    }

    test("Custom navn for opsjon må være satt hvis opsjonsmodell er ANNET") {
        AvtaleValidator.validate(
            oppfolgingMedRammeAvtale.copy(
                opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.ANNET, LocalDate.now().plusYears(3)),
            ),
            ctx,
        ).shouldBeLeft(
            listOf(
                FieldError("/opsjonsmodell/customOpsjonsmodellNavn", "Du må beskrive opsjonsmodellen"),
            ),
        )
    }

    test("avtaletype må stemme overens med tiltakstypen") {
        AvtaleValidator.validate(
            forhaandsgodkjent.copy(avtaletype = Avtaletype.RAMMEAVTALE),
            ctx.copy(tiltakstype = ctx.tiltakstype.copy(navn = TiltakstypeFixtures.AFT.navn)),
        ).shouldBeLeft().shouldContain(
            FieldError("/avtaletype", "Rammeavtale er ikke tillatt for tiltakstype Arbeidsforberedende trening"),
        )
        AvtaleValidator.validate(
            forhaandsgodkjent.copy(
                tiltakskode = Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
                avtaletype = Avtaletype.AVTALE,
            ),
            ctx.copy(tiltakstype = ctx.tiltakstype.copy(navn = TiltakstypeFixtures.VTA.navn)),
        ).shouldBeLeft().shouldContain(
            FieldError("/avtaletype", "Avtale er ikke tillatt for tiltakstype Varig tilrettelagt arbeid i skjermet virksomhet"),
        )
        AvtaleValidator.validate(
            avtaleTypeAvtale.copy(avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG),
            ctx.copy(tiltakstype = ctx.tiltakstype.copy(navn = TiltakstypeFixtures.Oppfolging.navn)),
        ).shouldBeLeft().shouldContain(
            FieldError("/avtaletype", "Offentlig-offentlig samarbeid er ikke tillatt for tiltakstype Oppfølging"),
        )
        AvtaleValidator.validate(
            gruppeAmo.copy(avtaletype = Avtaletype.FORHANDSGODKJENT),
            ctx.copy(tiltakstype = ctx.tiltakstype.copy(navn = TiltakstypeFixtures.GruppeAmo.navn)),
        ).shouldBeLeft().shouldContain(
            FieldError("/avtaletype", "Forhåndsgodkjent er ikke tillatt for tiltakstype Arbeidsmarkedsopplæring (Gruppe)"),
        )

        AvtaleValidator.validate(forhaandsgodkjent, ctx).shouldBeRight()
        AvtaleValidator.validate(oppfolgingMedRammeAvtale, ctx).shouldBeRight()
        AvtaleValidator.validate(gruppeAmo, ctx).shouldBeRight()
    }

    test("SakarkivNummer må være med når avtalen er avtale eller rammeavtale") {
        AvtaleValidator.validate(oppfolgingMedRammeAvtale.copy(sakarkivNummer = null), ctx).shouldBeLeft(
            listOf(FieldError("/sakarkivNummer", "Du må skrive inn saksnummer til avtalesaken")),
        )

        AvtaleValidator.validate(avtaleTypeAvtale.copy(sakarkivNummer = null), ctx).shouldBeLeft(
            listOf(FieldError("/sakarkivNummer", "Du må skrive inn saksnummer til avtalesaken")),
        )

        AvtaleValidator.validate(
            gruppeAmo.copy(
                avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG,
                sakarkivNummer = null,
                amoKategorisering = AmoKategorisering.Studiespesialisering,
            ),
            ctx,
        ).shouldBeRight()
    }

    test("arrangørens underenheter må tilhøre hovedenhet i Brreg") {
        AvtaleValidator.validate(
            avtaleRequest,
            ctx.copy(
                arrangor = ArrangorFixtures.Fretex.hovedenhet.copy(
                    underenheter = listOf(ArrangorFixtures.underenhet1),
                ),
            ),
        ).shouldBeLeft().shouldContainExactlyInAnyOrder(
            FieldError(
                "/arrangorUnderenheter",
                "Arrangøren Underenhet 1 AS er ikke en gyldig underenhet til hovedenheten FRETEX AS.",
            ),
        )

        AvtaleValidator.validate(
            avtaleRequest,
            ctx.copy(
                arrangor = ArrangorFixtures.Fretex.hovedenhet.copy(
                    underenheter = listOf(ArrangorFixtures.Fretex.underenhet1),
                ),
            ),
        ).shouldBeRight()
    }

    test("arrangøren må være aktiv i Brreg") {
        AvtaleValidator.validate(
            avtaleRequest,
            ctx.copy(
                arrangor = ArrangorFixtures.Fretex.hovedenhet.copy(
                    slettetDato = LocalDate.now(),
                    underenheter = listOf(ArrangorFixtures.Fretex.underenhet1.copy(slettetDato = LocalDate.now())),
                ),
            ),
        ).shouldBeLeft().shouldContainExactlyInAnyOrder(
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

        AvtaleValidator.validate(avtaleMedEndringer, ctx) shouldBeLeft listOf(
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

        AvtaleValidator.validate(avtaleMedEndringer, ctx).shouldBeLeft(
            listOf(
                FieldError("/utdanningslop", "Du må velge minst ett lærefag"),
            ),
        )
    }

    context("prismodell") {
        test("prismodell må stemme overens med tiltakstypen") {
            AvtaleValidator.validate(
                avtaleRequest.copy(
                    tiltakskode = Tiltakskode.OPPFOLGING,
                    prismodell = PrismodellRequest(
                        type = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
                        prisbetingelser = null,
                        satser = emptyList(),
                    ),
                ),
                ctx,
            ).shouldBeLeft().shouldContain(
                FieldError("/prismodell", "Fast sats per tiltaksplass per måned er ikke tillatt for tiltakstype Oppfølging"),
            )
            AvtaleValidator.validate(
                forhaandsgodkjent.copy(
                    prismodell = PrismodellRequest(
                        type = PrismodellType.ANNEN_AVTALT_PRIS,
                        prisbetingelser = null,
                        satser = emptyList(),
                    ),
                ),
                ctx.copy(tiltakstype = ctx.tiltakstype.copy(navn = TiltakstypeFixtures.AFT.navn)),
            ).shouldBeLeft().shouldContain(
                FieldError("/prismodell", "Annen avtalt pris er ikke tillatt for tiltakstype Arbeidsforberedende trening"),
            )
            AvtaleValidator.validate(
                gruppeAmo.copy(
                    prismodell = PrismodellRequest(
                        type = PrismodellType.AVTALT_PRIS_PER_UKESVERK,
                        prisbetingelser = null,
                        satser = emptyList(),
                    ),
                ),
                ctx.copy(tiltakstype = ctx.tiltakstype.copy(navn = TiltakstypeFixtures.GruppeAmo.navn)),
            ).shouldBeLeft().shouldContain(
                FieldError("/prismodell", "Avtalt ukespris per tiltaksplass er ikke tillatt for tiltakstype Arbeidsmarkedsopplæring (Gruppe)"),
            )

            val fri = avtaleRequest.copy(
                prismodell = PrismodellRequest(
                    type = PrismodellType.ANNEN_AVTALT_PRIS,
                    prisbetingelser = null,
                    satser = emptyList(),
                ),
            )
            AvtaleValidator.validate(fri, ctx).shouldBeRight()
        }
    }

    context("når avtalen allerede eksisterer") {
        test("Skal ikke kunne endre opsjonsmodell eller avtaletype når opsjon er registrert") {
            val startDato = LocalDate.of(2024, 5, 7)
            val avtale = gruppeAmo.copy(
                avtaletype = Avtaletype.AVTALE,
                opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN, startDato.plusYears(3)),
            )

            AvtaleValidator.validate(
                avtale,
                ctx.copy(
                    previous = previous.copy(
                        avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG,
                        opsjonerRegistrert = listOf(
                            Avtale.OpsjonLoggDto(
                                id = UUID.randomUUID(),
                                createdAt = LocalDateTime.now(),
                                sluttDato = LocalDate.now(),
                                forrigeSluttDato = LocalDate.now(),
                                status = OpsjonLoggStatus.OPSJON_UTLOST,
                            ),
                        ),
                    ),
                ),
            ) shouldBeLeft listOf(
                FieldError("/avtaletype", "Du kan ikke endre avtaletype når opsjoner er registrert"),
                FieldError("/opsjonsmodell", "Du kan ikke endre opsjonsmodell når opsjoner er registrert"),
            )
        }

        context("når avtalen har gjennomføringer") {
            val startDatoForGjennomforing = LocalDate.now()

            test("skal validere at data samsvarer med avtalens gjennomføringer") {
                val request = oppfolgingMedRammeAvtale.copy(
                    tiltakskode = Tiltakskode.ARBEIDSRETTET_REHABILITERING,
                    startDato = startDatoForGjennomforing.plusDays(1),
                    arrangor = AvtaleRequest.Arrangor(
                        hovedenhet = ArrangorFixtures.hovedenhet.organisasjonsnummer,
                        underenheter = listOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                        kontaktpersoner = emptyList(),
                    ),
                )

                val formatertDato = startDatoForGjennomforing.formaterDatoTilEuropeiskDatoformat()

                AvtaleValidator.validate(
                    request,
                    ctx.copy(
                        previous = previous.copy(
                            gjennomforinger = listOf(
                                Ctx.Gjennomforing(
                                    arrangor = Gjennomforing.ArrangorUnderenhet(
                                        id = ArrangorFixtures.underenhet2.id,
                                        organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                                        navn = ArrangorFixtures.underenhet2.navn,
                                        kontaktpersoner = emptyList(),
                                        slettet = false,
                                    ),
                                    startDato = LocalDate.now(),
                                    utdanningslop = null,
                                ),
                            ),
                        ),
                    ),
                ).shouldBeLeft() shouldContainExactlyInAnyOrder listOf(
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

                AvtaleValidator.validate(request, ctx.copy(previous = previous)).shouldBeRight()
            }

            test("kan ikke endre tiltakstype hvis prismodell er inkompatibel") {
                val request = avtaleRequest.copy(
                    avtaletype = Avtaletype.FORHANDSGODKJENT,
                    opsjonsmodell = Opsjonsmodell(
                        type = OpsjonsmodellType.VALGFRI_SLUTTDATO,
                        opsjonMaksVarighet = null,
                    ),
                    tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
                )

                AvtaleValidator.validate(request, ctx.copy(previous = previous)).shouldBeLeft() shouldContain
                    FieldError(
                        "/tiltakskode",
                        "Tiltakstype kan ikke endres ikke fordi prismodellen “Annen avtalt pris” er i bruk",
                    )
            }
        }
    }

    test("Slettede administratorer valideres") {
        AvtaleValidator.validate(
            avtaleRequest,
            ctx.copy(
                administratorer = listOf(
                    NavAnsattFixture.DonaldDuck.copy(skalSlettesDato = LocalDate.now()).toNavAnsatt(emptySet()),
                ),
            ),
        ).shouldBeLeft().shouldContainExactlyInAnyOrder(
            FieldError("/administratorer", "Nav identer DD1 er slettet og må fjernes"),
        )
    }

    context("status endringer") {
        test("status blir UTKAST når avtalen lagres uten en arrangør") {
            AvtaleValidator.validate(avtaleRequest.copy(arrangor = null), ctx).shouldBeRight().should {
                it.status shouldBe AvtaleStatusType.UTKAST
            }
        }

        test("status blir AKTIV når avtalen lagres med sluttdato i fremtiden") {
            AvtaleValidator.validate(avtaleRequest, ctx).shouldBeRight().should {
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

            AvtaleValidator.validate(request, ctx).shouldBeRight().should {
                it.status shouldBe AvtaleStatusType.AVSLUTTET
            }
        }

        test("status forblir AVBRUTT på en avtale som allerede er AVBRUTT") {
            val today = LocalDate.now()

            val avtale = AvtaleFixtures.oppfolging

            /*
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
             */
            val request = avtaleRequest.copy(
                id = avtale.id,
                startDato = today,
                sluttDato = today,
            )

            AvtaleValidator.validate(
                request,
                ctx.copy(
                    previous = previous.copy(status = AvtaleStatusType.AVBRUTT),
                ),
            ).shouldBeRight().should {
                it.status shouldBe AvtaleStatusType.AVBRUTT
            }
        }
    }
})
