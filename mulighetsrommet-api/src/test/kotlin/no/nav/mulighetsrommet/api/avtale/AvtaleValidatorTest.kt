package no.nav.mulighetsrommet.api.avtale

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import no.nav.mulighetsrommet.api.avtale.api.AvtaleRequest
import no.nav.mulighetsrommet.api.avtale.mapper.AvtaleDboMapper
import no.nav.mulighetsrommet.api.avtale.model.*
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
import no.nav.mulighetsrommet.api.navenhet.toDto
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

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
            type = Prismodell.ANNEN_AVTALT_PRIS,
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
        AvtaleFixtures.oppfolgingDbo,
        null,
        Tiltakskode.OPPFOLGING,
    )
    val ctx = AvtaleValidator.Ctx(
        previous = null,
        arrangor = null,
        gjennomforinger = emptyList(),
        administratorer = emptyList(),
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        navEnheter = listOf(NavEnhetFixtures.Innlandet.toDto(), NavEnhetFixtures.Gjovik.toDto()),
        status = AvtaleStatusType.AKTIV,
    )

    test("should accumulate errors when request has multiple issues") {
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

        AvtaleValidator.validate(request, ctx.copy(navEnheter = emptyList())).shouldBeLeft().shouldContainAll(
            listOf(
                FieldError("/startDato", "Startdato må være før sluttdato"),
                FieldError("/navRegioner", "Du må velge minst én Nav-region"),
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
        forAll(
            row(
                forhaandsgodkjent.copy(avtaletype = Avtaletype.RAMMEAVTALE),
                FieldError(
                    "/avtaletype",
                    "Rammeavtale er ikke tillatt for tiltakstypen",
                ),
            ),
            row(
                forhaandsgodkjent.copy(
                    tiltakskode = Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
                    avtaletype = Avtaletype.AVTALE,
                ),
                FieldError(
                    "/avtaletype",
                    "Avtale er ikke tillatt for tiltakstypen",
                ),
            ),
            row(
                avtaleTypeAvtale.copy(avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG),
                FieldError("/avtaletype", "Offentlig-offentlig samarbeid er ikke tillatt for tiltakstypen"),
            ),
            row(
                gruppeAmo.copy(avtaletype = Avtaletype.FORHANDSGODKJENT),
                FieldError(
                    "/avtaletype",
                    "Forhåndsgodkjent er ikke tillatt for tiltakstypen",
                ),
            ),
        ) { avtale, expectedError ->
            AvtaleValidator.validate(avtale, ctx).shouldBeLeft().shouldContain(expectedError)
        }

        forAll(
            row(forhaandsgodkjent),
            row(oppfolgingMedRammeAvtale),
            row(gruppeAmo),
        ) { avtale ->
            AvtaleValidator.validate(avtale, ctx).shouldBeRight()
        }
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
            avtaleTypeAvtale,
            ctx.copy(
                arrangor = AvtaleValidator.Ctx.Arrangor(
                    hovedenhet = ArrangorFixtures.Fretex.hovedenhet,
                    underenheter = listOf(ArrangorFixtures.underenhet1),
                    kontaktpersoner = emptyList(),
                ),
            ),
        ).shouldBeLeft().shouldContainExactlyInAnyOrder(
            FieldError(
                "/arrangorUnderenheter",
                "Arrangøren Underenhet 1 AS er ikke en gyldig underenhet til hovedenheten FRETEX AS.",
            ),
        )

        AvtaleValidator.validate(
            avtaleTypeAvtale,
            ctx.copy(
                arrangor = AvtaleValidator.Ctx.Arrangor(
                    hovedenhet = ArrangorFixtures.Fretex.hovedenhet,
                    underenheter = listOf(ArrangorFixtures.Fretex.underenhet1),
                    kontaktpersoner = emptyList(),
                ),
            ),
        ).shouldBeRight()
    }

    test("arrangøren må være aktiv i Brreg") {
        AvtaleValidator.validate(
            avtaleTypeAvtale,
            ctx.copy(
                arrangor = AvtaleValidator.Ctx.Arrangor(
                    hovedenhet = ArrangorFixtures.Fretex.hovedenhet.copy(slettetDato = LocalDate.of(2024, 1, 1)),
                    underenheter = listOf(ArrangorFixtures.Fretex.underenhet1.copy(slettetDato = LocalDate.of(2024, 1, 1))),
                    kontaktpersoner = emptyList(),
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
        AvtaleValidator.validate(
            gruppeAmo.copy(
                utdanningslop = null,
                tiltakskode = Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
            ),
            ctx,
        ) shouldBeLeft listOf(
            FieldError("/utdanningslop", "Du må velge et utdanningsprogram og minst ett lærefag"),
        )
    }

    test("minst én utdanning er påkrevd når tiltakstypen er Gruppe Fag- og yrkesopplæring") {
        AvtaleValidator.validate(
            gruppeAmo.copy(
                tiltakskode = Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
                utdanningslop = UtdanningslopDbo(
                    utdanningsprogram = UUID.randomUUID(),
                    utdanninger = emptyList(),
                ),
            ),
            ctx,
        ) shouldBeLeft listOf(
            FieldError("/utdanningslop", "Du må velge minst ett lærefag"),
        )
    }

    context("prismodell") {
        test("prismodell må stemme overens med tiltakstypen") {
            forAll(
                row(
                    avtaleRequest.copy(
                        tiltakskode = Tiltakskode.OPPFOLGING,
                        prismodell = PrismodellRequest(
                            type = Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
                            prisbetingelser = null,
                            satser = emptyList(),
                        ),
                    ),
                    FieldError(
                        "/prismodell",
                        "Fast sats per tiltaksplass per måned er ikke tillatt for tiltakstypen",
                    ),
                ),
                row(
                    forhaandsgodkjent.copy(
                        prismodell = PrismodellRequest(
                            type = Prismodell.ANNEN_AVTALT_PRIS,
                            prisbetingelser = null,
                            satser = emptyList(),
                        ),
                    ),
                    FieldError(
                        "/prismodell",
                        "Annen avtalt pris er ikke tillatt for tiltakstypen",
                    ),
                ),
                row(
                    gruppeAmo.copy(
                        prismodell = PrismodellRequest(
                            type = Prismodell.AVTALT_PRIS_PER_UKESVERK,
                            prisbetingelser = null,
                            satser = emptyList(),
                        ),
                    ),
                    FieldError(
                        "/prismodell",
                        "Avtalt ukespris per tiltaksplass er ikke tillatt for tiltakstypen",
                    ),
                ),
            ) { avtale, expectedError ->
                AvtaleValidator.validate(avtale, ctx).shouldBeLeft().shouldContain(expectedError)
            }

            val fri = avtaleRequest.copy(
                prismodell = PrismodellRequest(
                    type = Prismodell.ANNEN_AVTALT_PRIS,
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
            val request = avtaleRequest.copy(
                startDato = startDato,
                sluttDato = startDato.plusYears(1),
                opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN_PLUSS_EN, startDato.plusYears(4)),
                avtaletype = Avtaletype.AVTALE,
            )

            val previous = AvtaleFixtures.oppfolging.copy(
                opsjonerRegistrert = listOf(
                    Avtale.OpsjonLoggDto(
                        id = UUID.randomUUID(),
                        createdAt = LocalDateTime.now(),
                        sluttDato = avtaleRequest.sluttDato?.plusYears(1),
                        forrigeSluttDato = avtaleRequest.sluttDato!!,
                        status = OpsjonLoggStatus.OPSJON_UTLOST,
                    ),
                ),
            )

            AvtaleValidator.validate(request, ctx.copy(previous = previous)) shouldBeLeft listOf(
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
                        previous = AvtaleFixtures.oppfolging,
                        arrangor = AvtaleValidator.Ctx.Arrangor(
                            hovedenhet = ArrangorFixtures.hovedenhet,
                            underenheter = emptyList(),
                            kontaktpersoner = emptyList(),
                        ),
                        gjennomforinger = listOf(
                            AvtaleValidator.Ctx.Gjennomforing(
                                utdanningslop = null,
                                startDato = startDatoForGjennomforing,
                                arrangor = GjennomforingDto.ArrangorUnderenhet(
                                    id = ArrangorFixtures.underenhet2.id,
                                    organisasjonsnummer = ArrangorFixtures.underenhet2.organisasjonsnummer,
                                    navn = ArrangorFixtures.underenhet2.navn,
                                    kontaktpersoner = emptyList(),
                                    slettet = false,
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

            test("kan ikke endre tiltakstype hvis prismodell er inkompatibel") {
                val request = avtaleRequest.copy(
                    avtaletype = Avtaletype.FORHANDSGODKJENT,
                    opsjonsmodell = Opsjonsmodell(
                        type = OpsjonsmodellType.VALGFRI_SLUTTDATO,
                        opsjonMaksVarighet = null,
                    ),
                    tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
                )

                AvtaleValidator.validate(request, ctx.copy(previous = AvtaleFixtures.oppfolging)).shouldBeLeft() shouldContain
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
                administratorer = listOf(NavAnsattFixture.DonaldDuckDto.copy(skalSlettesDato = LocalDate.now())),
            ),
        ).shouldBeLeft().shouldContainExactlyInAnyOrder(
            FieldError("/administratorer", "Administratorene med Nav ident DD1 er slettet og må fjernes"),
        )
    }
})
