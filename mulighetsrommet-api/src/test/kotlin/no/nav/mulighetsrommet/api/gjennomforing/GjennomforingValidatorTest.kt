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
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingKontaktpersonDbo
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.domain.dbo.GjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.AmoKategorisering
import no.nav.mulighetsrommet.domain.dto.AvbruttAarsak
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
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
        arrangorId = ArrangorFixtures.hovedenhet.id,
        arrangorUnderenheter = listOf(ArrangorFixtures.underenhet1.id),
        navEnheter = listOf("0400", "0502"),
    )

    val gjennomforing = GjennomforingFixtures.Oppfolging1.copy(
        avtaleId = avtale.id,
        startDato = avtaleStartDato,
        sluttDato = avtaleSluttDato,
        navRegion = "0400",
        navEnheter = listOf("0502"),
        arrangorId = ArrangorFixtures.underenhet1.id,
        administratorer = listOf(NavAnsattFixture.ansatt1.navIdent),
    )

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
            NavEnhetDbo(
                navn = "Nav IT",
                enhetsnummer = "2990",
                status = NavEnhetStatus.AKTIV,
                type = Norg2Type.IT,
                overordnetEnhet = null,
            ),
        ),
        arrangorer = listOf(
            ArrangorFixtures.hovedenhet,
            ArrangorFixtures.underenhet1,
            ArrangorFixtures.underenhet2,
        ),
        ansatte = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
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

    test("should fail when avtale does not exist") {
        val unknownAvtaleId = UUID.randomUUID()

        val dbo = gjennomforing.copy(avtaleId = unknownAvtaleId)

        createValidator().validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            ValidationError("avtaleId", "Avtalen finnes ikke"),
        )
    }

    test("should fail when tiltakstype does not match with avtale") {
        database.run {
            queries.avtale.upsert(avtale.copy(tiltakstypeId = TiltakstypeFixtures.AFT.id))
        }

        createValidator().validate(gjennomforing, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            ValidationError("tiltakstypeId", "Tiltakstypen må være den samme som for avtalen"),
        )
    }

    test("should fail when tiltakstype does not support change of oppstartstype") {

        createValidator().validate(gjennomforing.copy(oppstart = GjennomforingOppstartstype.FELLES), null)
            .shouldBeLeft()
            .shouldContainExactlyInAnyOrder(ValidationError("oppstart", "Tiltaket må ha løpende oppstartstype"))
    }

    test("kan ikke opprette på ikke Aktiv avtale") {
        val id = UUID.randomUUID()
        database.run {
            queries.avtale.upsert(avtale.copy(id = id))
            queries.avtale.avbryt(id, LocalDateTime.now(), AvbruttAarsak.BudsjettHensyn)
        }

        val dbo = gjennomforing.copy(avtaleId = id)

        createValidator().validate(dbo, null).shouldBeLeft(
            listOf(ValidationError("avtaleId", "Avtalen må være aktiv for å kunne opprette tiltak")),
        )

        val id2 = UUID.randomUUID()
        database.run {
            queries.avtale.upsert(avtale.copy(id = id2, sluttDato = LocalDate.now().minusDays(1)))
        }

        val dbo2 = gjennomforing.copy(avtaleId = id2)

        createValidator().validate(dbo2, null).shouldBeLeft(
            listOf(ValidationError("avtaleId", "Avtalen må være aktiv for å kunne opprette tiltak")),
        )
    }

    test("kan ikke opprette før Avtale startDato") {
        val dbo = gjennomforing.copy(
            startDato = avtale.startDato.minusDays(1),
        )

        createValidator().validate(dbo, null).shouldBeLeft(
            listOf(ValidationError("startDato", "Du må legge inn en startdato som er etter avtalens startdato")),
        )
    }

    test("skal returnere en ny verdi for 'tilgjengelig for arrangør'-dato når datoen er utenfor gyldig tidsrom") {
        val startDato = LocalDate.now().plusMonths(1)
        val dbo = gjennomforing.copy(startDato = startDato)
        database.run { queries.gjennomforing.upsert(dbo) }

        val beforeAllowedDato = startDato.minusMonths(3)
        createValidator().validate(gjennomforing.copy(tilgjengeligForArrangorFraOgMedDato = beforeAllowedDato), null)
            .shouldBeRight().should {
                it.tilgjengeligForArrangorFraOgMedDato.shouldBeNull()
            }

        val afterStartDato = startDato.plusDays(1)
        createValidator().validate(dbo.copy(tilgjengeligForArrangorFraOgMedDato = afterStartDato), null)
            .shouldBeRight().should {
                it.tilgjengeligForArrangorFraOgMedDato.shouldBeNull()
            }

        val beforeStartDato = startDato.minusDays(1)
        createValidator().validate(dbo.copy(tilgjengeligForArrangorFraOgMedDato = beforeStartDato), null)
            .shouldBeRight().should {
                it.tilgjengeligForArrangorFraOgMedDato shouldBe beforeStartDato
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
            listOf(ValidationError("sluttDato", "Du må legge inn sluttdato for gjennomføringen")),
        )
        createValidator().validate(vanligAvtale, null).shouldBeLeft(
            listOf(ValidationError("sluttDato", "Du må legge inn sluttdato for gjennomføringen")),
        )
        createValidator().validate(offentligOffentlig, null).shouldBeLeft(
            listOf(ValidationError("sluttDato", "Du må legge inn sluttdato for gjennomføringen")),
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
                ValidationError("avtale.amoKategorisering", "Du må velge en kurstype for avtalen"),
                ValidationError("amoKategorisering", "Du må velge et kurselement for gjennomføringen"),
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
                ValidationError("amoKategorisering", "Du må velge et kurselement for gjennomføringen"),
            ),
        )
    }

    test("utdanningsprogram og lærefag er påkrevd når tiltakstypen er Gruppe Fag- og yrkesopplæring") {
        val gruppeFagYrke = GjennomforingFixtures.GruppeFagYrke1.copy(utdanningslop = null)

        createValidator().validate(gruppeFagYrke, null).shouldBeLeft(
            listOf(ValidationError("utdanningslop", "Du må velge utdanningsprogram og lærefag på avtalen")),
        )
    }

    // TODO: fiks test
    xtest("utdanningsløp må være valgt fra avtalen når tiltakstypen er Gruppe Fag- og yrkesopplæring") {
        val gruppeFagYrke = GjennomforingFixtures.GruppeFagYrke1.copy(
            utdanningslop = UtdanningslopDbo(
                utdanningsprogram = UUID.randomUUID(),
                utdanninger = listOf(UUID.randomUUID()),
            ),
        )

        createValidator().validate(gruppeFagYrke, null).shouldBeRight()
    }

    test("arrangøren må være aktiv i Brreg") {
        database.run {
            queries.arrangor.upsert(ArrangorFixtures.underenhet1.copy(slettetDato = LocalDate.of(2024, 1, 1)))
        }

        createValidator().validate(gjennomforing, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            ValidationError(
                "arrangorId",
                "Arrangøren Underenhet 1 AS er slettet i Brønnøysundregistrene. Gjennomføringer kan ikke opprettes for slettede bedrifter.",
            ),
        )
    }

    test("should validate fields in the gjennomføring and fields related to the avtale") {
        forAll(
            row(
                gjennomforing.copy(
                    startDato = avtaleStartDato.minusDays(1),
                    sluttDato = avtaleStartDato,
                ),
                listOf(
                    ValidationError(
                        "startDato",
                        "Du må legge inn en startdato som er etter avtalens startdato",
                    ),
                ),
            ),
            row(
                gjennomforing.copy(
                    startDato = avtaleSluttDato,
                    sluttDato = avtaleStartDato,
                ),
                listOf(ValidationError("startDato", "Startdato må være før sluttdato")),
            ),
            row(
                gjennomforing.copy(antallPlasser = 0),
                listOf(ValidationError("antallPlasser", "Du må legge inn antall plasser større enn 0")),
            ),
            row(
                gjennomforing.copy(navEnheter = listOf("0401")),
                listOf(ValidationError("navEnheter", "Nav-enhet 0401 mangler i avtalen")),
            ),
            row(
                gjennomforing.copy(arrangorId = ArrangorFixtures.underenhet2.id),
                listOf(ValidationError("arrangorId", "Du må velge en arrangør for avtalen")),
            ),
        ) { input, error ->
            createValidator().validate(input, null).shouldBeLeft(error)
        }
    }

    context("when gjennomføring already exists") {
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

            validator.validate(gjennomforing.copy(startDato = LocalDate.now().plusDays(5)), previous)
                .shouldBeRight()
            validator.validate(gjennomforing.copy(startDato = avtaleStartDato.minusDays(1)), previous)
                .shouldBeLeft(
                    listOf(
                        ValidationError("startDato", "Du må legge inn en startdato som er etter avtalens startdato"),
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
                    ValidationError(
                        "sluttDato",
                        "Du kan ikke sette en sluttdato bakover i tid når gjennomføringen er aktiv",
                    ),
                ),
            )
        }

        test("skal godta endringer selv om avtale er avbrutt") {
            val previous = database.run {
                queries.avtale.avbryt(avtale.id, LocalDateTime.now(), AvbruttAarsak.BudsjettHensyn)
                queries.gjennomforing.get(gjennomforing.id)
            }

            validator.validate(gjennomforing, previous).shouldBeRight()
        }

        test("should fail when is avbrutt") {
            val previous = database.run {
                queries.gjennomforing.setAvsluttet(
                    gjennomforing.id,
                    LocalDateTime.now(),
                    AvbruttAarsak.Feilregistrering,
                )
                queries.gjennomforing.get(gjennomforing.id)
            }

            validator.validate(gjennomforing, previous).shouldBeLeft().shouldContainExactlyInAnyOrder(
                ValidationError("navn", "Du kan ikke gjøre endringer på en gjennomføring som er avbrutt"),
            )
        }

        test("should fail when is avsluttet") {
            val previous = database.run {
                queries.gjennomforing.upsert(gjennomforing.copy(sluttDato = LocalDate.now().minusDays(2)))
                queries.gjennomforing.setAvsluttet(gjennomforing.id, LocalDateTime.now(), null)
                queries.gjennomforing.get(gjennomforing.id)
            }

            validator.validate(gjennomforing, previous).shouldBeLeft().shouldContainExactlyInAnyOrder(
                ValidationError("navn", "Du kan ikke gjøre endringer på en gjennomføring som er avsluttet"),
            )
        }
    }

    context("slettede nav ansatte") {
        val validator = createValidator()

        test("Slettede kontaktpersoner valideres") {
            database.run {
                queries.ansatt.upsert(NavAnsattFixture.ansatt2.copy(skalSlettesDato = LocalDate.now()))
            }

            val dbo = gjennomforing.copy(
                kontaktpersoner = listOf(
                    GjennomforingKontaktpersonDbo(
                        navIdent = NavAnsattFixture.ansatt2.navIdent,
                        navEnheter = emptyList(),
                        beskrivelse = null,
                    ),
                ),
            )

            validator.validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
                ValidationError("kontaktpersoner", "Kontaktpersonene med Nav ident DD2 er slettet og må fjernes"),
            )
        }

        test("Slettede administratorer valideres") {
            database.run {
                queries.ansatt.upsert(NavAnsattFixture.ansatt1.copy(skalSlettesDato = LocalDate.now()))
            }

            val dbo = gjennomforing.copy(administratorer = listOf(NavAnsattFixture.ansatt1.navIdent))

            validator.validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
                ValidationError("administratorer", "Administratorene med Nav ident DD1 er slettet og må fjernes"),
            )
        }
    }
})
