package no.nav.mulighetsrommet.api.gjennomforing.service

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.avtale.model.Avtale
import no.nav.mulighetsrommet.api.avtale.model.AvtaleStatus
import no.nav.mulighetsrommet.api.avtale.model.Opsjonsmodell
import no.nav.mulighetsrommet.api.avtale.model.OpsjonsmodellType
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Innlandet
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Oslo
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Sel
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.TiltakOslo
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingDetaljerRequest
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.navenhet.Kontorstruktur
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.SakarkivNummer
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.model.Valuta
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class GjennomforingValidatorTest : FunSpec({
    val avtale = Avtale(
        id = AvtaleFixtures.oppfolging.id,
        navn = "Avtalenavn",
        avtalenummer = "2023#1",
        sakarkivNummer = SakarkivNummer("24/1234"),
        tiltakstype = Avtale.Tiltakstype(
            navn = TiltakstypeFixtures.Oppfolging.navn,
            id = TiltakstypeFixtures.Oppfolging.id,
            tiltakskode = TiltakstypeFixtures.Oppfolging.tiltakskode,
        ),
        arrangor = Avtale.ArrangorHovedenhet(
            id = ArrangorFixtures.hovedenhet.id,
            organisasjonsnummer = ArrangorFixtures.hovedenhet.organisasjonsnummer,
            navn = ArrangorFixtures.hovedenhet.navn,
            underenheter = listOf(
                Avtale.ArrangorUnderenhet(
                    navn = ArrangorFixtures.underenhet1.navn,
                    id = ArrangorFixtures.underenhet1.id,
                    organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                    slettet = false,
                ),
            ),
            kontaktpersoner = emptyList(),
            slettet = false,
        ),
        startDato = LocalDate.now(),
        sluttDato = LocalDate.now().plusMonths(1),
        status = AvtaleStatus.Aktiv,
        avtaletype = Avtaletype.RAMMEAVTALE,
        administratorer = emptyList(),
        kontorstruktur = listOf(
            Kontorstruktur(
                region = Kontorstruktur.Region(Innlandet.navn, Innlandet.enhetsnummer),
                kontorer = listOf(
                    Kontorstruktur.Kontor(Gjovik.navn, Gjovik.enhetsnummer, Kontorstruktur.Kontortype.LOKAL),
                ),
            ),
        ),
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = emptyList(),
        personvernBekreftet = false,
        amoKategorisering = null,
        opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN, LocalDate.now().plusYears(3)),
        utdanningslop = null,
        prismodeller = listOf(
            Prismodell.AnnenAvtaltPris(
                id = UUID.randomUUID(),
                valuta = Valuta.NOK,
                prisbetingelser = null,
                tilsagnPerDeltaker = false,
            ),
        ),
        opsjonerRegistrert = emptyList(),
    )

    val request = GjennomforingFixtures.createGjennomforingRequest(
        AvtaleFixtures.oppfolging,
        startDato = avtale.startDato,
        sluttDato = avtale.sluttDato,
        navRegioner = setOf(NavEnhetNummer("0400")),
        navKontorer = setOf(NavEnhetNummer("0502")),
        arrangorId = ArrangorFixtures.underenhet1.id,
        administratorer = setOf(NavAnsattFixture.DonaldDuck.navIdent),
    )

    val ctx = GjennomforingValidator.Ctx(
        previous = null,
        avtale = avtale,
        arrangor = ArrangorFixtures.underenhet1,
        antallDeltakere = 0,
        status = GjennomforingStatusType.GJENNOMFORES,
    )

    fun validate(r: no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingRequest, c: GjennomforingValidator.Ctx) = GjennomforingValidator.validateDetaljer(r.id, r.tiltakstypeId, r.avtaleId, r.detaljer, c)

    test("validerer estimertVentetid") {
        validate(
            request.copy(
                detaljer = request.detaljer.copy(
                    estimertVentetid = GjennomforingDetaljerRequest.EstimertVentetid(
                        verdi = null,
                        enhet = null,
                    ),
                ),
            ),
            ctx,
        ).shouldBeLeft().shouldContainExactlyInAnyOrder(
            FieldError("/estimertVentetid/enhet", "Du må velge en enhet"),
            FieldError("/estimertVentetid/verdi", "Du må velge en verdi større enn 0"),
        )
    }

    test("skal feile når tiltakstypen ikke overlapper med avtalen") {
        validate(
            request.copy(tiltakstypeId = TiltakstypeFixtures.AFT.id),
            ctx,
        ).shouldBeLeft().shouldContainExactlyInAnyOrder(
            FieldError("/tiltakstypeId", "Tiltakstypen må være den samme som for avtalen"),
        )
    }

    test("skal ikke kunne sette felles oppstart når tiltaket krever løpende oppstart") {
        validate(request.copy(detaljer = request.detaljer.copy(oppstart = GjennomforingOppstartstype.FELLES)), ctx)
            .shouldBeLeft()
            .shouldContain(FieldError("/oppstart", "Tiltaket må ha løpende oppstart"))
    }

    test("skal ikke kunne sette oppstartstype til enkeltplass") {
        validate(request.copy(detaljer = request.detaljer.copy(oppstart = GjennomforingOppstartstype.ENKELTPLASS)), ctx)
            .shouldBeLeft()
            .shouldContain(FieldError("/oppstart", "Tiltaket må ha løpende oppstart"))
    }

    test("skal ikke kunne sette direkte vedtak når tiltaket har felles oppstart") {
        validate(
            request.copy(
                tiltakstypeId = TiltakstypeFixtures.Jobbklubb.id,
                detaljer = request.detaljer.copy(
                    oppstart = GjennomforingOppstartstype.FELLES,
                    pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
                ),
            ),
            ctx.copy(
                avtale = ctx.avtale.copy(
                    tiltakstype = Avtale.Tiltakstype(
                        navn = TiltakstypeFixtures.Jobbklubb.navn,
                        id = TiltakstypeFixtures.Jobbklubb.id,
                        tiltakskode = TiltakstypeFixtures.Jobbklubb.tiltakskode,
                    ),
                ),
            ),
        )
            .shouldBeLeft()
            .shouldContainExactlyInAnyOrder(
                FieldError(
                    "/pameldingType",
                    "Påmeldingstype må være “trenger godkjenning” når tiltaket har felles oppstart",
                ),
            )
    }

    test("avtalen må være aktiv") {
        validate(
            request,
            ctx.copy(avtale = ctx.avtale.copy(status = AvtaleStatus.Avsluttet)),
        ).shouldBeLeft(
            listOf(FieldError("/avtaleId", "Avtalen må være aktiv for å kunne opprette tiltak")),
        )
    }

    test("kan ikke opprette før Avtale startDato") {
        validate(
            request.copy(detaljer = request.detaljer.copy(startDato = avtale.startDato.minusDays(1))),
            ctx,
        ).shouldBeLeft(
            listOf(FieldError("/startDato", "Du må legge inn en startdato som er etter avtalens startdato")),
        )
    }

    test("kan ikke bare opprettes med status GJENNOMFORES") {
        validate(request, ctx.copy(status = GjennomforingStatusType.GJENNOMFORES))
            .shouldBeRight()
        validate(request, ctx.copy(status = GjennomforingStatusType.AVSLUTTET)).shouldBeLeft(
            listOf(FieldError("/navn", "Du kan ikke opprette en gjennomføring med status Avsluttet")),
        )
        validate(request, ctx.copy(status = GjennomforingStatusType.AVBRUTT)).shouldBeLeft(
            listOf(FieldError("/navn", "Du kan ikke opprette en gjennomføring med status Avbrutt")),
        )
        validate(request, ctx.copy(status = GjennomforingStatusType.AVLYST)).shouldBeLeft(
            listOf(FieldError("/navn", "Du kan ikke opprette en gjennomføring med status Avlyst")),
        )
    }

    test("skal returnere en ny verdi for 'tilgjengelig for arrangør'-dato når datoen er utenfor gyldig tidsrom") {
        val req = request.copy(detaljer = request.detaljer.copy(startDato = LocalDate.now().plusMonths(1)))
        validate(
            req.copy(detaljer = req.detaljer.copy(tilgjengeligForArrangorDato = avtale.startDato.minusMonths(3))),
            ctx,
        ).shouldBeRight().should {
            it.detaljer.tilgjengeligForArrangorDato.shouldBeNull()
        }

        validate(
            req.copy(detaljer = req.detaljer.copy(tilgjengeligForArrangorDato = req.detaljer.startDato!!.plusDays(1))),
            ctx,
        ).shouldBeRight().should {
            it.detaljer.tilgjengeligForArrangorDato.shouldBeNull()
        }

        validate(
            req.copy(detaljer = req.detaljer.copy(tilgjengeligForArrangorDato = req.detaljer.startDato.minusDays(1))),
            ctx,
        ).shouldBeRight().should {
            it.detaljer.tilgjengeligForArrangorDato shouldBe req.detaljer.startDato.minusDays(1)
        }
    }

    test("sluttDato er påkrevd hvis ikke forhåndsgodkjent avtale") {
        val utenSluttDato = request.copy(detaljer = request.detaljer.copy(sluttDato = null))

        validate(
            utenSluttDato,
            ctx.copy(avtale = ctx.avtale.copy(avtaletype = Avtaletype.FORHANDSGODKJENT)),
        ).shouldBeRight()
        validate(
            utenSluttDato,
            ctx.copy(avtale = ctx.avtale.copy(avtaletype = Avtaletype.AVTALE)),
        ).shouldBeLeft(
            listOf(FieldError("/sluttDato", "Du må legge inn sluttdato for gjennomføringen")),
        )
        validate(
            utenSluttDato,
            ctx.copy(avtale = ctx.avtale.copy(avtaletype = Avtaletype.RAMMEAVTALE)),
        ).shouldBeLeft(
            listOf(FieldError("/sluttDato", "Du må legge inn sluttdato for gjennomføringen")),
        )
        validate(
            utenSluttDato,
            ctx.copy(avtale = ctx.avtale.copy(avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG)),
        ).shouldBeLeft(
            listOf(FieldError("/sluttDato", "Du må legge inn sluttdato for gjennomføringen")),
        )
    }

    test("amoKategorisering er påkrevd for avtale og gjennomføring når tiltakstype er Gruppe AMO") {
        val avtaleUtenAmokategorisering = avtale.copy(
            tiltakstype = Avtale.Tiltakstype(
                tiltakskode = TiltakstypeFixtures.GruppeAmo.tiltakskode,
                id = TiltakstypeFixtures.GruppeAmo.id,
                navn = TiltakstypeFixtures.GruppeAmo.navn,
            ),
            amoKategorisering = null,
        )

        GjennomforingValidator.validateAmoKategorisering(
            avtaleUtenAmokategorisering,
            request.detaljer.amoKategorisering,
        ).shouldBeLeft(
            listOf(
                FieldError("/avtaleId", "Du må velge en kurstype for avtalen"),
                FieldError("/amoKategorisering/kurstype", "Du må velge en kurstype"),
            ),
        )
    }

    test("Kurselement må velges for gjennomføring når tiltakstype er Gruppe AMO") {
        val avtaleUtenAmokategorisering = avtale.copy(
            tiltakstype = Avtale.Tiltakstype(
                tiltakskode = TiltakstypeFixtures.GruppeAmo.tiltakskode,
                id = TiltakstypeFixtures.GruppeAmo.id,
                navn = TiltakstypeFixtures.GruppeAmo.navn,
            ),
            amoKategorisering = AmoKategorisering.Studiespesialisering,
        )

        GjennomforingValidator.validateAmoKategorisering(
            avtaleUtenAmokategorisering,
            request.detaljer.amoKategorisering,
        ).shouldBeLeft(
            listOf(
                FieldError("/amoKategorisering/kurstype", "Du må velge en kurstype"),
            ),
        )
    }

    test("utdanningsprogram og lærefag er påkrevd når tiltakstypen er Gruppe Fag- og yrkesopplæring") {
        val avtaleGruFag = avtale.copy(
            tiltakstype = Avtale.Tiltakstype(
                tiltakskode = TiltakstypeFixtures.GruppeFagOgYrkesopplaering.tiltakskode,
                id = TiltakstypeFixtures.GruppeFagOgYrkesopplaering.id,
                navn = TiltakstypeFixtures.GruppeFagOgYrkesopplaering.navn,
            ),
        )

        GjennomforingValidator.validateUtdanningslop(
            avtaleGruFag,
            request.detaljer.utdanningslop,
        ).shouldBeLeft(
            listOf(FieldError("/utdanningslop", "Du må velge utdanningsprogram og lærefag på avtalen")),
        )
    }

    test("arrangøren må være aktiv i Brreg") {
        validate(
            request,
            ctx.copy(arrangor = ArrangorFixtures.underenhet1.copy(slettetDato = LocalDate.of(2024, 1, 1))),
        ).shouldBeLeft().shouldContainExactlyInAnyOrder(
            FieldError(
                "/arrangorId",
                "Arrangøren Underenhet 1 AS er slettet i Brønnøysundregistrene. Gjennomføringer kan ikke opprettes for slettede bedrifter",
            ),
        )
    }

    test("validerer at datafelter i gjennomføring i henhold til data i avtalen") {
        validate(
            request.copy(
                detaljer = request.detaljer.copy(
                    startDato = avtale.startDato.minusDays(1),
                    sluttDato = avtale.startDato,
                ),
            ),
            ctx,
        ).shouldBeLeft(
            listOf(FieldError("/startDato", "Du må legge inn en startdato som er etter avtalens startdato")),
        )
        validate(
            request.copy(
                detaljer = request.detaljer.copy(
                    startDato = avtale.sluttDato!!,
                    sluttDato = avtale.startDato,
                ),
            ),
            ctx,
        ).shouldBeLeft(
            listOf(FieldError("/startDato", "Startdato må være før sluttdato")),
        )
        validate(
            request.copy(detaljer = request.detaljer.copy(antallPlasser = 0)),
            ctx,
        ).shouldBeLeft(
            listOf(FieldError("/antallPlasser", "Du må legge inn antall plasser større enn 0")),
        )
        validate(
            request.copy(detaljer = request.detaljer.copy(arrangorId = ArrangorFixtures.underenhet2.id)),
            ctx,
        ).shouldBeLeft(
            listOf(FieldError("/arrangorId", "Du må velge en arrangør fra avtalen")),
        )
    }

    context("oppmotested") {
        test("oppmotested er ikke påkrevd") {
            validate(
                request.copy(detaljer = request.detaljer.copy(oppmoteSted = null)),
                ctx,
            ).shouldBeRight()
        }

        test("oppmotested må være innenfor maks tegn grense") {
            validate(
                request.copy(detaljer = request.detaljer.copy(oppmoteSted = ":)".repeat(251))),
                ctx,
            ).shouldBeRight()
        }
    }

    test("Nav-regioner og Nav-enheter er påkrevd") {
        GjennomforingValidator.validateVeilederinfo(
            request.veilederinformasjon.copy(
                navRegioner = setOf(),
                navKontorer = setOf(),
            ),
            ctx.avtale,
            emptyList(),
        ).shouldBeLeft(
            listOf(
                FieldError("/veilederinformasjon/navRegioner", "Du må velge minst én Nav-region fra avtalen"),
                FieldError("/veilederinformasjon/navKontorer", "Du må velge minst én Nav-enhet fra avtalen"),
            ),
        )
    }

    test("fjerner nav-enheter som ikke er en del av avtalen") {
        GjennomforingValidator.validateVeilederinfo(
            request.veilederinformasjon.copy(
                navRegioner = setOf(Innlandet.enhetsnummer, Oslo.enhetsnummer),
                navKontorer = setOf(Gjovik.enhetsnummer, Sel.enhetsnummer),
                navAndreEnheter = setOf(TiltakOslo.enhetsnummer),
            ),
            ctx.avtale,
            emptyList(),
        ).shouldBeRight().should {
            it.navEnheter.shouldBe(setOf(Innlandet.enhetsnummer, Gjovik.enhetsnummer))
        }
    }
    context("når gjennonmføring allerede eksisterer") {
        val gjennomforing = GjennomforingValidator.Ctx.Gjennomforing(
            arrangorId = ArrangorFixtures.underenhet1.id,
            sluttDato = AvtaleFixtures.oppfolging.detaljerDbo.sluttDato,
            status = GjennomforingStatusType.GJENNOMFORES,
            oppstart = GjennomforingOppstartstype.LOPENDE,
            avtaleId = avtale.id,
            pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
            arena = Gjennomforing.ArenaData(Tiltaksnummer("2020#1"), "1234"),
        )

        test("Skal godta endringer for startdato selv om gjennomføringen er aktiv, men startdato skal ikke kunne settes til før avtaledatoen") {
            validate(
                request.copy(detaljer = request.detaljer.copy(startDato = avtale.startDato.plusDays(5))),
                ctx.copy(previous = gjennomforing),
            )
                .shouldBeRight()
            validate(
                request.copy(detaljer = request.detaljer.copy(startDato = avtale.startDato.minusDays(1))),
                ctx.copy(previous = gjennomforing),
            )
                .shouldBeLeft(
                    listOf(
                        FieldError("/startDato", "Du må legge inn en startdato som er etter avtalens startdato"),
                    ),
                )
        }

        test("inkluderer arena-felter i validert modell") {
            validate(
                request,
                ctx.copy(previous = gjennomforing),
            ).shouldBeRight().gjennomforing.should {
                it.arenaTiltaksnummer shouldBe Tiltaksnummer("2020#1")
                it.arenaAnsvarligEnhet shouldBe "1234"
            }
        }

        test("Skal godta endringer for sluttdato frem i tid selv om gjennomføringen er aktiv") {
            validate(
                request.copy(detaljer = request.detaljer.copy(sluttDato = avtale.sluttDato)),
                ctx.copy(previous = gjennomforing.copy(sluttDato = avtale.sluttDato!!.minusDays(1))),
            )
                .shouldBeRight()
        }

        test("godtar ikke endringer for sluttdato tilbake i tid når gjennomføringen er aktiv") {
            validate(
                request.copy(
                    detaljer = request.detaljer.copy(
                        startDato = LocalDate.now().minusDays(2),
                        sluttDato = LocalDate.now().minusDays(1),
                    ),
                ),
                ctx.copy(previous = gjennomforing, avtale = ctx.avtale.copy(startDato = LocalDate.now().minusDays(2))),
            ).shouldBeLeft(
                listOf(
                    FieldError(
                        "/sluttDato",
                        "Du kan ikke sette en sluttdato bakover i tid når gjennomføringen er aktiv",
                    ),
                ),
            )
            validate(
                request.copy(
                    detaljer = request.detaljer.copy(
                        startDato = LocalDate.now().minusDays(2),
                        sluttDato = LocalDate.now().minusDays(1),
                    ),
                ),
                ctx.copy(
                    previous = gjennomforing.copy(sluttDato = null),
                    avtale = ctx.avtale.copy(startDato = LocalDate.now().minusDays(2)),
                ),
            ).shouldBeLeft(
                listOf(
                    FieldError(
                        "/sluttDato",
                        "Du kan ikke sette en sluttdato bakover i tid når gjennomføringen er aktiv",
                    ),
                ),
            )
        }

        test("skal godta endringer selv om avtale er avbrutt") {
            validate(
                request,
                ctx.copy(
                    previous = gjennomforing,
                    avtale = ctx.avtale.copy(
                        status = AvtaleStatus.Avbrutt(
                            tidspunkt = LocalDateTime.now(),
                            aarsaker = emptyList(),
                            forklaring = null,
                        ),
                    ),
                ),
            ).shouldBeRight()
        }

        test("skal feile når gjennomføring er avbrutt") {
            validate(
                request,
                ctx.copy(previous = gjennomforing.copy(status = GjennomforingStatusType.AVBRUTT)),
            ).shouldBeLeft().shouldContainExactlyInAnyOrder(
                FieldError("/navn", "Du kan ikke gjøre endringer på en gjennomføring med status Avbrutt"),
            )
        }

        test("skal feile når gjennomføring er avsluttet") {
            validate(
                request,
                ctx.copy(previous = gjennomforing.copy(status = GjennomforingStatusType.AVSLUTTET)),
            ).shouldBeLeft().shouldContainExactlyInAnyOrder(
                FieldError("/navn", "Du kan ikke gjøre endringer på en gjennomføring med status Avsluttet"),
            )
        }
    }

    context("når gjennomføring har deltakere") {
        val gjennomforing = GjennomforingValidator.Ctx.Gjennomforing(
            arrangorId = ArrangorFixtures.underenhet1.id,
            sluttDato = AvtaleFixtures.oppfolging.detaljerDbo.sluttDato,
            status = GjennomforingStatusType.GJENNOMFORES,
            oppstart = GjennomforingOppstartstype.FELLES,
            avtaleId = avtale.id,
            pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
            arena = null,
        )

        test("skal ikke kunne endre oppstartstype") {
            validate(
                request.copy(
                    tiltakstypeId = TiltakstypeFixtures.Jobbklubb.id,
                    detaljer = request.detaljer.copy(oppstart = GjennomforingOppstartstype.LOPENDE),
                ),
                ctx.copy(
                    previous = gjennomforing,
                    antallDeltakere = 4,
                    avtale = ctx.avtale.copy(
                        tiltakstype = Avtale.Tiltakstype(
                            navn = TiltakstypeFixtures.Jobbklubb.navn,
                            id = TiltakstypeFixtures.Jobbklubb.id,
                            tiltakskode = TiltakstypeFixtures.Jobbklubb.tiltakskode,
                        ),
                    ),
                ),
            ).shouldBeLeft().shouldContainExactlyInAnyOrder(
                FieldError(
                    "/oppstart",
                    "Oppstartstype kan ikke endres fordi det er deltakere koblet til gjennomføringen",
                ),
            )
        }

        test("skal ikke kunne endre påmeldingstype") {
            validate(
                request.copy(
                    tiltakstypeId = TiltakstypeFixtures.Jobbklubb.id,
                    detaljer = request.detaljer.copy(
                        pameldingType = GjennomforingPameldingType.TRENGER_GODKJENNING,
                        oppstart = GjennomforingOppstartstype.FELLES,
                    ),
                ),
                ctx.copy(
                    previous = gjennomforing,
                    antallDeltakere = 4,
                    avtale = ctx.avtale.copy(
                        tiltakstype = Avtale.Tiltakstype(
                            navn = TiltakstypeFixtures.Jobbklubb.navn,
                            id = TiltakstypeFixtures.Jobbklubb.id,
                            tiltakskode = TiltakstypeFixtures.Jobbklubb.tiltakskode,
                        ),
                    ),
                ),
            ).shouldBeLeft().shouldContainExactlyInAnyOrder(
                FieldError(
                    "/pameldingType",
                    "Påmeldingstype kan ikke endres fordi det er deltakere koblet til gjennomføringen",
                ),
            )
        }
    }

    context("slettede nav ansatte") {
        val ansatt = NavAnsatt(
            entraObjectId = UUID.randomUUID(),
            navIdent = NavIdent("B123456"),
            fornavn = "",
            etternavn = "",
            hovedenhet = NavAnsatt.Hovedenhet(
                enhetsnummer = Gjovik.enhetsnummer,
                navn = Gjovik.navn,
            ),
            mobilnummer = null,
            epost = "",
            roller = emptySet(),
            skalSlettesDato = LocalDate.now(),
        )

        test("Slettede kontaktpersoner valideres") {
            GjennomforingValidator.validateVeilederinfo(
                request.veilederinformasjon,
                ctx.avtale,
                listOf(ansatt),
            ).shouldBeLeft().shouldContainExactlyInAnyOrder(
                FieldError("/veilederinformasjon/kontaktpersoner", "Nav identer B123456 er slettet og må fjernes"),
            )
        }
    }
})
