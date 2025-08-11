package no.nav.mulighetsrommet.api.gjennomforing

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingKontaktpersonDbo
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class GjennomforingValidatorTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val avtaleStartDato = LocalDate.now()
    val avtaleSluttDato = LocalDate.now().plusMonths(1)
    val avtale = AvtaleFixtures.oppfolging.copy(
        id = UUID.randomUUID(),
        startDato = avtaleStartDato,
        sluttDato = avtaleSluttDato,
        arrangor = AvtaleFixtures.oppfolging.arrangor?.copy(
            hovedenhet = ArrangorFixtures.hovedenhet.id,
            underenheter = listOf(ArrangorFixtures.underenhet1.id),
        ),
        navEnheter = listOf(NavEnhetNummer("0400"), NavEnhetNummer("0502")),
    )

    val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(
        avtaleId = avtale.id,
        startDato = avtaleStartDato,
        sluttDato = avtaleSluttDato,
        navEnheter = setOf(NavEnhetNummer("0400"), NavEnhetNummer("0502")),
        arrangorId = ArrangorFixtures.underenhet1.id,
        administratorer = listOf(NavAnsattFixture.DonaldDuck.navIdent),
    )

    val domain = MulighetsrommetTestDomain(
        navEnheter = listOf(
            NavEnhetDbo(
                navn = "Nav Oslo",
                enhetsnummer = NavEnhetNummer("0300"),
                status = NavEnhetStatus.AKTIV,
                type = Norg2Type.FYLKE,
                overordnetEnhet = null,
            ),
            NavEnhetDbo(
                navn = "Nav Innlandet",
                enhetsnummer = NavEnhetNummer("0400"),
                status = NavEnhetStatus.AKTIV,
                type = Norg2Type.FYLKE,
                overordnetEnhet = null,
            ),
            NavEnhetDbo(
                navn = "Nav Gjøvik",
                enhetsnummer = NavEnhetNummer("0502"),
                status = NavEnhetStatus.AKTIV,
                type = Norg2Type.LOKAL,
                overordnetEnhet = NavEnhetNummer("0400"),
            ),
        ),
        arrangorer = listOf(
            ArrangorFixtures.hovedenhet,
            ArrangorFixtures.underenhet1,
            ArrangorFixtures.underenhet2,
        ),
        ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
        tiltakstyper = listOf(
            TiltakstypeFixtures.VTA,
            TiltakstypeFixtures.AFT,
            TiltakstypeFixtures.Jobbklubb,
            TiltakstypeFixtures.GruppeAmo,
            TiltakstypeFixtures.Oppfolging,
            TiltakstypeFixtures.GruppeFagOgYrkesopplaering,
        ),
        avtaler = listOf(
            avtale,
            AvtaleFixtures.oppfolgingMedAvtale,
            AvtaleFixtures.oppfolging,
            AvtaleFixtures.gruppeAmo,
            AvtaleFixtures.jobbklubb,
            AvtaleFixtures.VTA,
            AvtaleFixtures.AFT,
            AvtaleFixtures.gruppeFagYrke,
        ),
    )

    beforeEach {
        domain.initialize(database.db)
    }

    afterTest {
        database.truncateAll()
    }

    fun createValidator() = GjennomforingValidator(database.db)

    test("skal feile når avtale ikke finnes") {
        val unknownAvtaleId = UUID.randomUUID()

        val dbo = gjennomforing.copy(avtaleId = unknownAvtaleId)

        createValidator().validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            FieldError("/avtaleId", "Avtalen finnes ikke"),
        )
    }

    test("skal feile når tiltakstypen ikke overlapper med avtalen") {
        database.run {
            queries.avtale.upsert(avtale.copy(tiltakstypeId = TiltakstypeFixtures.AFT.id))
        }

        createValidator().validate(gjennomforing, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            FieldError("/tiltakstypeId", "Tiltakstypen må være den samme som for avtalen"),
        )
    }

    test("skal ikke kunne sette felles oppsart når tiltaket krever løpende oppstart") {
        createValidator().validate(gjennomforing.copy(oppstart = GjennomforingOppstartstype.FELLES), null)
            .shouldBeLeft()
            .shouldContainExactlyInAnyOrder(FieldError("/oppstart", "Tiltaket må ha løpende oppstartstype"))
    }

    test("avtalen må være aktiv") {
        database.run {
            queries.avtale.setStatus(avtale.id, AvtaleStatus.AVBRUTT, LocalDateTime.now(), AarsakerOgForklaringRequest(listOf(AvbruttAarsak.BUDSJETT_HENSYN), null))
        }
        createValidator().validate(gjennomforing, null).shouldBeLeft(
            listOf(FieldError("/avtaleId", "Avtalen må være aktiv for å kunne opprette tiltak")),
        )

        database.run {
            queries.avtale.setStatus(avtale.id, AvtaleStatus.AVSLUTTET, null, null)
        }
        createValidator().validate(gjennomforing, null).shouldBeLeft(
            listOf(FieldError("/avtaleId", "Avtalen må være aktiv for å kunne opprette tiltak")),
        )
    }

    test("kan ikke opprette før Avtale startDato") {
        val dbo = gjennomforing.copy(
            startDato = avtale.startDato.minusDays(1),
        )

        createValidator().validate(dbo, null).shouldBeLeft(
            listOf(FieldError("/startDato", "Du må legge inn en startdato som er etter avtalens startdato")),
        )
    }

    test("kan ikke bare opprettes med status GJENNOMFORES") {
        val gjennomfores = gjennomforing.copy(status = GjennomforingStatus.GJENNOMFORES)
        val avsluttet = gjennomforing.copy(status = GjennomforingStatus.AVSLUTTET)
        val avbrutt = gjennomforing.copy(status = GjennomforingStatus.AVBRUTT)
        val avlyst = gjennomforing.copy(status = GjennomforingStatus.AVLYST)

        createValidator().validate(gjennomfores, null).shouldBeRight()
        createValidator().validate(avsluttet, null).shouldBeLeft(
            listOf(FieldError("/navn", "Du kan ikke opprette en gjennomføring som er avsluttet")),
        )
        createValidator().validate(avbrutt, null).shouldBeLeft(
            listOf(FieldError("/navn", "Du kan ikke opprette en gjennomføring som er avbrutt")),
        )
        createValidator().validate(avlyst, null).shouldBeLeft(
            listOf(FieldError("/navn", "Du kan ikke opprette en gjennomføring som er avlyst")),
        )
    }

    test("skal returnere en ny verdi for 'tilgjengelig for arrangør'-dato når datoen er utenfor gyldig tidsrom") {
        val startDato = LocalDate.now().plusMonths(1)
        val dbo = gjennomforing.copy(startDato = startDato)
        database.run { queries.gjennomforing.upsert(dbo) }

        val beforeAllowedDato = startDato.minusMonths(3)
        createValidator().validate(gjennomforing.copy(tilgjengeligForArrangorDato = beforeAllowedDato), null)
            .shouldBeRight().should {
                it.tilgjengeligForArrangorDato.shouldBeNull()
            }

        val afterStartDato = startDato.plusDays(1)
        createValidator().validate(dbo.copy(tilgjengeligForArrangorDato = afterStartDato), null)
            .shouldBeRight().should {
                it.tilgjengeligForArrangorDato.shouldBeNull()
            }

        val beforeStartDato = startDato.minusDays(1)
        createValidator().validate(dbo.copy(tilgjengeligForArrangorDato = beforeStartDato), null)
            .shouldBeRight().should {
                it.tilgjengeligForArrangorDato shouldBe beforeStartDato
            }
    }

    test("sluttDato er påkrevd hvis ikke forhåndsgodkjent avtale") {
        val forhaandsgodkjent = GjennomforingFixtures.AFT1.copy(sluttDato = null)
        val rammeAvtale = GjennomforingFixtures.Oppfolging1.copy(sluttDato = null)
        val vanligAvtale = GjennomforingFixtures.Oppfolging1.copy(
            sluttDato = null,
            avtaleId = AvtaleFixtures.oppfolgingMedAvtale.id,
        )
        val offentligOffentlig = GjennomforingFixtures.GruppeAmo1.copy(
            sluttDato = null,
            amoKategorisering = AmoKategorisering.Studiespesialisering,
        )

        createValidator().validate(forhaandsgodkjent, null).shouldBeRight()
        createValidator().validate(rammeAvtale, null).shouldBeLeft(
            listOf(FieldError("/sluttDato", "Du må legge inn sluttdato for gjennomføringen")),
        )
        createValidator().validate(vanligAvtale, null).shouldBeLeft(
            listOf(FieldError("/sluttDato", "Du må legge inn sluttdato for gjennomføringen")),
        )
        createValidator().validate(offentligOffentlig, null).shouldBeLeft(
            listOf(FieldError("/sluttDato", "Du må legge inn sluttdato for gjennomføringen")),
        )
    }

    test("amoKategorisering er påkrevd for avtale og gjennomføring når tiltakstype er Gruppe AMO") {
        val avtaleUtenAmokategorisering = AvtaleFixtures.gruppeAmo.copy(
            tiltakstypeId = TiltakstypeFixtures.GruppeAmo.id,
            amoKategorisering = null,
        )
        database.run { queries.avtale.upsert(avtaleUtenAmokategorisering) }

        val gruppeAmo = GjennomforingFixtures.GruppeAmo1.copy(
            amoKategorisering = null,
            avtaleId = avtaleUtenAmokategorisering.id,
        )

        createValidator().validate(gruppeAmo, null).shouldBeLeft(
            listOf(
                FieldError("/avtale.amoKategorisering", "Du må velge en kurstype for avtalen"),
                FieldError("/amoKategorisering", "Du må velge et kurselement for gjennomføringen"),
            ),
        )
    }

    test("Kurselement må velges for gjennomføring når tiltakstype er Gruppe AMO") {
        val avtaleMedAmokategorisering = AvtaleFixtures.gruppeAmo.copy(
            tiltakstypeId = TiltakstypeFixtures.GruppeAmo.id,
            amoKategorisering = AmoKategorisering.Studiespesialisering,
        )
        database.run { queries.avtale.upsert(avtaleMedAmokategorisering) }

        val gruppeAmo = GjennomforingFixtures.GruppeAmo1.copy(
            amoKategorisering = null,
            avtaleId = avtaleMedAmokategorisering.id,
        )

        createValidator().validate(gruppeAmo, null).shouldBeLeft(
            listOf(
                FieldError("/amoKategorisering", "Du må velge et kurselement for gjennomføringen"),
            ),
        )
    }

    test("utdanningsprogram og lærefag er påkrevd når tiltakstypen er Gruppe Fag- og yrkesopplæring") {
        val gruppeFagYrke = GjennomforingFixtures.GruppeFagYrke1.copy(utdanningslop = null)

        createValidator().validate(gruppeFagYrke, null).shouldBeLeft(
            listOf(FieldError("/utdanningslop", "Du må velge utdanningsprogram og lærefag på avtalen")),
        )
    }

    test("arrangøren må være aktiv i Brreg") {
        database.run {
            queries.arrangor.upsert(ArrangorFixtures.underenhet1.copy(slettetDato = LocalDate.of(2024, 1, 1)))
        }

        createValidator().validate(gjennomforing, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            FieldError(
                "/arrangorId",
                "Arrangøren Underenhet 1 AS er slettet i Brønnøysundregistrene. Gjennomføringer kan ikke opprettes for slettede bedrifter.",
            ),
        )
    }

    test("validerer at datafelter i gjennomføring i henhold til data i avtalen") {
        forAll(
            row(
                gjennomforing.copy(
                    startDato = avtaleStartDato.minusDays(1),
                    sluttDato = avtaleStartDato,
                ),
                listOf(
                    FieldError(
                        "/startDato",
                        "Du må legge inn en startdato som er etter avtalens startdato",
                    ),
                ),
            ),
            row(
                gjennomforing.copy(
                    startDato = avtaleSluttDato,
                    sluttDato = avtaleStartDato,
                ),
                listOf(FieldError("/startDato", "Startdato må være før sluttdato")),
            ),
            row(
                gjennomforing.copy(antallPlasser = 0),
                listOf(FieldError("/antallPlasser", "Du må legge inn antall plasser større enn 0")),
            ),
            row(
                gjennomforing.copy(
                    navEnheter = setOf(
                        NavEnhetNummer("0400"),
                        NavEnhetNummer("0502"),
                        NavEnhetNummer("0401"),
                    ),
                ),
                listOf(FieldError("/navEnheter", "Nav-enhet 0401 mangler i avtalen")),
            ),
            row(
                gjennomforing.copy(navEnheter = setOf()),
                listOf(
                    FieldError("/navEnheter", "Du må velge minst én Nav-region fra avtalen"),
                    FieldError("/navEnheter", "Du må velge minst én Nav-enhet fra avtalen"),
                ),
            ),
            row(
                gjennomforing.copy(arrangorId = ArrangorFixtures.underenhet2.id),
                listOf(FieldError("/arrangorId", "Du må velge en arrangør fra avtalen")),
            ),
        ) { input, error ->
            createValidator().validate(input, null).shouldBeLeft(error)
        }
    }

    context("når gjennonmføring allerede eksisterer") {
        beforeEach {
            database.run { queries.gjennomforing.upsert(gjennomforing.copy(administratorer = listOf())) }
        }

        afterEach {
            database.run { queries.gjennomforing.delete(gjennomforing.id) }
        }

        val validator = createValidator()

        test("Skal godta endringer for antall plasser selv om gjennomføringen er aktiv") {
            val previous = database.run { queries.gjennomforing.get(gjennomforing.id) }

            validator.validate(gjennomforing.copy(antallPlasser = 15), previous).shouldBeRight()
        }

        test("Skal godta endringer for startdato selv om gjennomføringen er aktiv, men startdato skal ikke kunne settes til før avtaledatoen") {
            val previous = database.run { queries.gjennomforing.get(gjennomforing.id) }

            validator.validate(gjennomforing.copy(startDato = avtaleStartDato.plusDays(5)), previous)
                .shouldBeRight()
            validator.validate(gjennomforing.copy(startDato = avtaleStartDato.minusDays(1)), previous)
                .shouldBeLeft(
                    listOf(
                        FieldError("/startDato", "Du må legge inn en startdato som er etter avtalens startdato"),
                    ),
                )
        }

        test("Skal godta endringer for sluttdato frem i tid selv om gjennomføringen er aktiv") {
            val previous = database.run {
                queries.avtale.upsert(avtale.copy(startDato = LocalDate.now().minusDays(3)))
                queries.gjennomforing.get(gjennomforing.id)
            }

            validator.validate(gjennomforing.copy(sluttDato = avtaleSluttDato.plusDays(5)), previous)
                .shouldBeRight()

            validator.validate(
                gjennomforing.copy(
                    startDato = LocalDate.now().minusDays(2),
                    sluttDato = LocalDate.now().minusDays(1),
                ),
                previous,
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
            val previous = database.run {
                queries.avtale.setStatus(
                    avtale.id,
                    AvtaleStatus.AVBRUTT,
                    LocalDateTime.now(),
                    AarsakerOgForklaringRequest(listOf(AvbruttAarsak.BUDSJETT_HENSYN), null),
                )
                queries.gjennomforing.get(gjennomforing.id)
            }

            validator.validate(gjennomforing, previous).shouldBeRight()
        }

        test("skal feile når gjennomføring er avbrutt") {
            val previous = database.run {
                queries.gjennomforing.setStatus(
                    id = gjennomforing.id,
                    status = GjennomforingStatus.AVBRUTT,
                    tidspunkt = LocalDateTime.now(),
                    AarsakerOgForklaringRequest(listOf(AvbruttAarsak.FEILREGISTRERING), null),
                )
                queries.gjennomforing.get(gjennomforing.id)
            }

            validator.validate(gjennomforing, previous).shouldBeLeft().shouldContainExactlyInAnyOrder(
                FieldError("/navn", "Du kan ikke gjøre endringer på en gjennomføring som er avbrutt"),
            )
        }

        test("skal feile når gjennomføring er avsluttet") {
            val previous = database.run {
                queries.gjennomforing.setStatus(
                    id = gjennomforing.id,
                    status = GjennomforingStatus.AVSLUTTET,
                    tidspunkt = LocalDateTime.now(),
                    null,
                )
                queries.gjennomforing.get(gjennomforing.id)
            }

            validator.validate(gjennomforing, previous).shouldBeLeft().shouldContainExactlyInAnyOrder(
                FieldError("/navn", "Du kan ikke gjøre endringer på en gjennomføring som er avsluttet"),
            )
        }
    }

    context("når gjennomføring har deltakere") {
        val validator = createValidator()

        test("skal ikke kunne endre oppstartstype") {
            val previous = database.run {
                queries.gjennomforing.upsert(gjennomforing.copy(oppstart = GjennomforingOppstartstype.FELLES))
                queries.deltaker.upsert(DeltakerFixtures.createDeltakerDbo(gjennomforing.id))
                queries.gjennomforing.get(gjennomforing.id)
            }

            val dbo = gjennomforing.copy(oppstart = GjennomforingOppstartstype.LOPENDE)

            validator.validate(dbo, previous).shouldBeLeft().shouldContainExactlyInAnyOrder(
                FieldError(
                    "/oppstart",
                    "Oppstartstype kan ikke endres fordi det er deltakere koblet til gjennomføringen",
                ),
            )
        }
    }

    context("slettede nav ansatte") {
        val validator = createValidator()

        test("Slettede kontaktpersoner valideres") {
            database.run {
                queries.ansatt.upsert(NavAnsattFixture.MikkeMus.copy(skalSlettesDato = LocalDate.now()))
            }

            val dbo = gjennomforing.copy(
                kontaktpersoner = listOf(
                    GjennomforingKontaktpersonDbo(
                        navIdent = NavAnsattFixture.MikkeMus.navIdent,
                        navEnheter = emptyList(),
                        beskrivelse = null,
                    ),
                ),
            )

            validator.validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
                FieldError("/kontaktpersoner", "Kontaktpersonene med Nav ident DD2 er slettet og må fjernes"),
            )
        }

        test("Slettede administratorer valideres") {
            database.run {
                queries.ansatt.upsert(NavAnsattFixture.DonaldDuck.copy(skalSlettesDato = LocalDate.now()))
            }

            val dbo = gjennomforing.copy(administratorer = listOf(NavAnsattFixture.DonaldDuck.navIdent))

            validator.validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
                FieldError("/administratorer", "Administratorene med Nav ident DD1 er slettet og må fjernes"),
            )
        }
    }
})
