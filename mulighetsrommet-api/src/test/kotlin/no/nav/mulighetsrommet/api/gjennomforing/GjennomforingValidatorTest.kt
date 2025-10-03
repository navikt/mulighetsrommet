package no.nav.mulighetsrommet.api.gjennomforing

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.avtale.model.*
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.navenhet.toDto
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.model.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class GjennomforingValidatorTest : FunSpec({
    val avtale = Avtale(
        id = UUID.randomUUID(),
        navn = "Avtalenavn",
        avtalenummer = "2023#1",
        sakarkivNummer = SakarkivNummer("24/1234"),
        tiltakstype = Avtale.Tiltakstype(
            navn = TiltakstypeFixtures.Oppfolging.navn,
            id = TiltakstypeFixtures.Oppfolging.id,
            tiltakskode = TiltakstypeFixtures.Oppfolging.tiltakskode!!,
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
        kontorstruktur = listOf(Kontorstruktur(region = NavEnhetFixtures.Innlandet.toDto(), kontorer = listOf(NavEnhetFixtures.Gjovik.toDto()))),
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = emptyList(),
        personvernBekreftet = false,
        amoKategorisering = null,
        opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN, LocalDate.now().plusYears(3)),
        utdanningslop = null,
        prismodell = Prismodell.AnnenAvtaltPris(
            prisbetingelser = null,
        ),
        arenaAnsvarligEnhet = null,
        opphav = ArenaMigrering.Opphav.TILTAKSADMINISTRASJON,
        opsjonerRegistrert = emptyList(),
    )

    val request = GjennomforingFixtures.Oppfolging1Request.copy(
        avtaleId = avtale.id,
        startDato = avtale.startDato,
        sluttDato = avtale.sluttDato,
        navEnheter = setOf(NavEnhetNummer("0400"), NavEnhetNummer("0502")),
        arrangorId = ArrangorFixtures.underenhet1.id,
        administratorer = listOf(NavAnsattFixture.DonaldDuck.navIdent),
    )

    val ctx = GjennomforingValidator.Ctx(
        previous = null,
        avtale = avtale,
        arrangor = ArrangorFixtures.underenhet1,
        kontaktpersoner = emptyList(),
        administratorer = emptyList(),
        antallDeltakere = 0,
        status = GjennomforingStatusType.GJENNOMFORES,
    )

    test("skal feile når tiltakstypen ikke overlapper med avtalen") {
        GjennomforingValidator.validate(
            request.copy(tiltakstypeId = TiltakstypeFixtures.AFT.id),
            ctx,
        ).shouldBeLeft().shouldContainExactlyInAnyOrder(
            FieldError("/tiltakstypeId", "Tiltakstypen må være den samme som for avtalen"),
        )
    }

    test("skal ikke kunne sette felles oppsart når tiltaket krever løpende oppstart") {
        GjennomforingValidator.validate(request.copy(oppstart = GjennomforingOppstartstype.FELLES), ctx)
            .shouldBeLeft()
            .shouldContainExactlyInAnyOrder(FieldError("/oppstart", "Tiltaket må ha løpende oppstartstype"))
    }

    test("avtalen må være aktiv") {
        GjennomforingValidator.validate(
            request,
            ctx.copy(avtale = ctx.avtale.copy(status = AvtaleStatus.Avsluttet)),
        ).shouldBeLeft(
            listOf(FieldError("/avtaleId", "Avtalen må være aktiv for å kunne opprette tiltak")),
        )
    }

    test("kan ikke opprette før Avtale startDato") {
        GjennomforingValidator.validate(request.copy(startDato = avtale.startDato.minusDays(1)), ctx).shouldBeLeft(
            listOf(FieldError("/startDato", "Du må legge inn en startdato som er etter avtalens startdato")),
        )
    }

    test("kan ikke bare opprettes med status GJENNOMFORES") {
        GjennomforingValidator.validate(request, ctx.copy(status = GjennomforingStatusType.GJENNOMFORES)).shouldBeRight()
        GjennomforingValidator.validate(request, ctx.copy(status = GjennomforingStatusType.AVSLUTTET)).shouldBeLeft(
            listOf(FieldError("/navn", "Du kan ikke opprette en gjennomføring som er avsluttet")),
        )
        GjennomforingValidator.validate(request, ctx.copy(status = GjennomforingStatusType.AVBRUTT)).shouldBeLeft(
            listOf(FieldError("/navn", "Du kan ikke opprette en gjennomføring som er avbrutt")),
        )
        GjennomforingValidator.validate(request, ctx.copy(status = GjennomforingStatusType.AVLYST)).shouldBeLeft(
            listOf(FieldError("/navn", "Du kan ikke opprette en gjennomføring som er avlyst")),
        )
    }

    test("skal returnere en ny verdi for 'tilgjengelig for arrangør'-dato når datoen er utenfor gyldig tidsrom") {
        val req = request.copy(startDato = LocalDate.now().plusMonths(1))
        GjennomforingValidator.validate(
            req.copy(tilgjengeligForArrangorDato = avtale.startDato.minusMonths(3)),
            ctx,
        )
            .shouldBeRight().should {
                it.tilgjengeligForArrangorDato.shouldBeNull()
            }

        GjennomforingValidator.validate(
            req.copy(tilgjengeligForArrangorDato = req.startDato.plusDays(1)),
            ctx,
        )
            .shouldBeRight().should {
                it.tilgjengeligForArrangorDato.shouldBeNull()
            }

        GjennomforingValidator.validate(
            req.copy(tilgjengeligForArrangorDato = req.startDato.minusDays(1)),
            ctx,
        ).shouldBeRight().should {
            it.tilgjengeligForArrangorDato shouldBe req.startDato.minusDays(1)
        }
    }

    test("sluttDato er påkrevd hvis ikke forhåndsgodkjent avtale") {
        val utenSluttDato = request.copy(sluttDato = null)

        GjennomforingValidator.validate(
            utenSluttDato,
            ctx.copy(avtale = ctx.avtale.copy(avtaletype = Avtaletype.FORHANDSGODKJENT)),
        ).shouldBeRight()
        GjennomforingValidator.validate(
            utenSluttDato,
            ctx.copy(avtale = ctx.avtale.copy(avtaletype = Avtaletype.AVTALE)),
        ).shouldBeLeft(
            listOf(FieldError("/sluttDato", "Du må legge inn sluttdato for gjennomføringen")),
        )
        GjennomforingValidator.validate(
            utenSluttDato,
            ctx.copy(avtale = ctx.avtale.copy(avtaletype = Avtaletype.RAMMEAVTALE)),
        ).shouldBeLeft(
            listOf(FieldError("/sluttDato", "Du må legge inn sluttdato for gjennomføringen")),
        )
        GjennomforingValidator.validate(
            utenSluttDato,
            ctx.copy(avtale = ctx.avtale.copy(avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG)),
        ).shouldBeLeft(
            listOf(FieldError("/sluttDato", "Du må legge inn sluttdato for gjennomføringen")),
        )
    }

    test("amoKategorisering er påkrevd for avtale og gjennomføring når tiltakstype er Gruppe AMO") {
        val avtaleUtenAmokategorisering = avtale.copy(
            tiltakstype = Avtale.Tiltakstype(
                tiltakskode = TiltakstypeFixtures.GruppeAmo.tiltakskode!!,
                id = TiltakstypeFixtures.GruppeAmo.id,
                navn = TiltakstypeFixtures.GruppeAmo.navn,
            ),
            amoKategorisering = null,
        )

        GjennomforingValidator.validate(
            request.copy(tiltakstypeId = TiltakstypeFixtures.GruppeAmo.id),
            ctx.copy(avtale = avtaleUtenAmokategorisering),
        ).shouldBeLeft(
            listOf(
                FieldError("/avtale.amoKategorisering", "Du må velge en kurstype for avtalen"),
                FieldError("/amoKategorisering", "Du må velge et kurselement for gjennomføringen"),
            ),
        )
    }

    test("Kurselement må velges for gjennomføring når tiltakstype er Gruppe AMO") {
        val avtaleUtenAmokategorisering = avtale.copy(
            tiltakstype = Avtale.Tiltakstype(
                tiltakskode = TiltakstypeFixtures.GruppeAmo.tiltakskode!!,
                id = TiltakstypeFixtures.GruppeAmo.id,
                navn = TiltakstypeFixtures.GruppeAmo.navn,
            ),
            amoKategorisering = AmoKategorisering.Studiespesialisering,
        )

        GjennomforingValidator.validate(
            request.copy(tiltakstypeId = TiltakstypeFixtures.GruppeAmo.id),
            ctx.copy(avtale = avtaleUtenAmokategorisering),
        ).shouldBeLeft(
            listOf(
                FieldError("/amoKategorisering", "Du må velge et kurselement for gjennomføringen"),
            ),
        )
    }

    test("utdanningsprogram og lærefag er påkrevd når tiltakstypen er Gruppe Fag- og yrkesopplæring") {
        val avtaleGruFag = avtale.copy(
            tiltakstype = Avtale.Tiltakstype(
                tiltakskode = TiltakstypeFixtures.GruppeFagOgYrkesopplaering.tiltakskode!!,
                id = TiltakstypeFixtures.GruppeFagOgYrkesopplaering.id,
                navn = TiltakstypeFixtures.GruppeFagOgYrkesopplaering.navn,
            ),
        )

        GjennomforingValidator.validate(
            request.copy(tiltakstypeId = TiltakstypeFixtures.GruppeFagOgYrkesopplaering.id),
            ctx.copy(avtale = avtaleGruFag),
        ).shouldBeLeft(
            listOf(FieldError("/utdanningslop", "Du må velge utdanningsprogram og lærefag på avtalen")),
        )
    }

    test("arrangøren må være aktiv i Brreg") {
        GjennomforingValidator.validate(
            request,
            ctx.copy(arrangor = ArrangorFixtures.underenhet1.copy(slettetDato = LocalDate.of(2024, 1, 1))),
        ).shouldBeLeft().shouldContainExactlyInAnyOrder(
            FieldError(
                "/arrangorId",
                "Arrangøren Underenhet 1 AS er slettet i Brønnøysundregistrene. Gjennomføringer kan ikke opprettes for slettede bedrifter.",
            ),
        )
    }

    test("validerer at datafelter i gjennomføring i henhold til data i avtalen") {
        forAll(
            row(
                request.copy(
                    startDato = avtale.startDato.minusDays(1),
                    sluttDato = avtale.startDato,
                ),
                listOf(
                    FieldError(
                        "/startDato",
                        "Du må legge inn en startdato som er etter avtalens startdato",
                    ),
                ),
            ),
            row(
                request.copy(
                    startDato = avtale.sluttDato!!,
                    sluttDato = avtale.startDato,
                ),
                listOf(FieldError("/startDato", "Startdato må være før sluttdato")),
            ),
            row(
                request.copy(antallPlasser = 0),
                listOf(FieldError("/antallPlasser", "Du må legge inn antall plasser større enn 0")),
            ),
            row(
                request.copy(navEnheter = setOf()),
                listOf(
                    FieldError("/navEnheter", "Du må velge minst én Nav-region fra avtalen"),
                    FieldError("/navEnheter", "Du må velge minst én Nav-enhet fra avtalen"),
                ),
            ),
            row(
                request.copy(arrangorId = ArrangorFixtures.underenhet2.id),
                listOf(FieldError("/arrangorId", "Du må velge en arrangør fra avtalen")),
            ),
        ) { input, error ->
            GjennomforingValidator.validate(input, ctx).shouldBeLeft(error)
        }
    }

    context("når gjennonmføring allerede eksisterer") {
        val gjennomforing = GjennomforingValidator.Ctx.Gjennomforing(
            arrangorId = ArrangorFixtures.underenhet1.id,
            sluttDato = AvtaleFixtures.oppfolging.detaljer.sluttDato,
            status = GjennomforingStatusType.GJENNOMFORES,
            oppstart = GjennomforingOppstartstype.LOPENDE,
            avtaleId = avtale.id,
        )

        test("Skal godta endringer for startdato selv om gjennomføringen er aktiv, men startdato skal ikke kunne settes til før avtaledatoen") {
            GjennomforingValidator.validate(request.copy(startDato = avtale.startDato.plusDays(5)), ctx.copy(previous = gjennomforing))
                .shouldBeRight()
            GjennomforingValidator.validate(request.copy(startDato = avtale.startDato.minusDays(1)), ctx.copy(previous = gjennomforing))
                .shouldBeLeft(
                    listOf(
                        FieldError("/startDato", "Du må legge inn en startdato som er etter avtalens startdato"),
                    ),
                )
        }

        test("Skal godta endringer for sluttdato frem i tid selv om gjennomføringen er aktiv") {
            GjennomforingValidator.validate(
                request.copy(sluttDato = avtale.sluttDato),
                ctx.copy(previous = gjennomforing.copy(sluttDato = avtale.sluttDato!!.minusDays(1))),
            )
                .shouldBeRight()

            GjennomforingValidator.validate(
                request.copy(
                    startDato = LocalDate.now().minusDays(2),
                    sluttDato = LocalDate.now().minusDays(1),
                ),
                ctx.copy(previous = gjennomforing, avtale = ctx.avtale.copy(startDato = LocalDate.now().minusDays(2))),
            ).shouldBeLeft(
                listOf(FieldError("/sluttDato", "Du kan ikke sette en sluttdato bakover i tid når gjennomføringen er aktiv")),
            )
        }

        test("skal godta endringer selv om avtale er avbrutt") {
            GjennomforingValidator.validate(
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
            GjennomforingValidator.validate(
                request,
                ctx.copy(previous = gjennomforing.copy(status = GjennomforingStatusType.AVBRUTT)),
            ).shouldBeLeft().shouldContainExactlyInAnyOrder(
                FieldError("/navn", "Du kan ikke gjøre endringer på en gjennomføring som er avbrutt"),
            )
        }

        test("skal feile når gjennomføring er avsluttet") {
            GjennomforingValidator.validate(
                request,
                ctx.copy(previous = gjennomforing.copy(status = GjennomforingStatusType.AVSLUTTET)),
            ).shouldBeLeft().shouldContainExactlyInAnyOrder(
                FieldError("/navn", "Du kan ikke gjøre endringer på en gjennomføring som er avsluttet"),
            )
        }
    }

    context("når gjennomføring har deltakere") {
        val gjennomforing = GjennomforingValidator.Ctx.Gjennomforing(
            arrangorId = ArrangorFixtures.underenhet1.id,
            sluttDato = AvtaleFixtures.oppfolging.detaljer.sluttDato,
            status = GjennomforingStatusType.GJENNOMFORES,
            oppstart = GjennomforingOppstartstype.LOPENDE,
            avtaleId = avtale.id,
        )

        test("skal ikke kunne endre oppstartstype") {
            GjennomforingValidator.validate(
                request.copy(
                    tiltakstypeId = TiltakstypeFixtures.Jobbklubb.id,
                    oppstart = GjennomforingOppstartstype.FELLES,
                ),
                ctx.copy(
                    previous = gjennomforing,
                    antallDeltakere = 4,
                    avtale = ctx.avtale.copy(
                        tiltakstype = Avtale.Tiltakstype(
                            navn = TiltakstypeFixtures.Jobbklubb.navn,
                            id = TiltakstypeFixtures.Jobbklubb.id,
                            tiltakskode = TiltakstypeFixtures.Jobbklubb.tiltakskode!!,
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
    }

    context("slettede nav ansatte") {
        val ansatt = NavAnsatt(
            entraObjectId = UUID.randomUUID(),
            navIdent = NavIdent("B123456"),
            fornavn = "",
            etternavn = "",
            hovedenhet = NavAnsatt.Hovedenhet(
                enhetsnummer = NavEnhetFixtures.Gjovik.enhetsnummer,
                navn = NavEnhetFixtures.Gjovik.navn,
            ),
            mobilnummer = null,
            epost = "",
            roller = emptySet(),
            skalSlettesDato = LocalDate.now(),
        )

        test("Slettede kontaktpersoner valideres") {
            GjennomforingValidator.validate(
                request,
                ctx.copy(kontaktpersoner = listOf(ansatt)),
            ).shouldBeLeft().shouldContainExactlyInAnyOrder(
                FieldError("/kontaktpersoner", "Nav identer B123456 er slettet og må fjernes"),
            )
        }

        test("Slettede admins valideres") {
            GjennomforingValidator.validate(
                request,
                ctx.copy(administratorer = listOf(ansatt)),
            ).shouldBeLeft().shouldContainExactlyInAnyOrder(
                FieldError("/administratorer", "Nav identer B123456 er slettet og må fjernes"),
            )
        }
    }
})
