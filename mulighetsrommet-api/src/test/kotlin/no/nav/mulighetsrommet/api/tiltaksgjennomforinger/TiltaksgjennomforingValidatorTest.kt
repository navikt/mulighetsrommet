package no.nav.mulighetsrommet.api.tiltaksgjennomforinger

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.ValidationError
import no.nav.mulighetsrommet.api.services.TiltakstypeService
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import java.time.LocalDate
import java.util.*

class TiltaksgjennomforingValidatorTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val avtaleStartDato = LocalDate.now()
    val avtaleSluttDato = LocalDate.now().plusMonths(1)
    val avtale = AvtaleFixtures.oppfolging.copy(
        startDato = avtaleStartDato,
        sluttDato = avtaleSluttDato,
        leverandorOrganisasjonsnummer = "000000000",
        leverandorUnderenheter = listOf("000000001", "000000002"),
        navEnheter = listOf("0400", "0502"),
    )

    val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1.copy(
        startDato = avtaleStartDato,
        sluttDato = avtaleSluttDato,
        navRegion = "0400",
        navEnheter = listOf("0502"),
        arrangorOrganisasjonsnummer = "000000001",
        administratorer = listOf(NavAnsattFixture.ansatt1.navIdent),
    )

    lateinit var tiltakstyper: TiltakstypeService
    lateinit var avtaler: AvtaleRepository
    lateinit var tiltaksgjennomforinger: TiltaksgjennomforingRepository

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
            NavEnhetDbo(
                navn = "NAV IT",
                enhetsnummer = "2990",
                status = NavEnhetStatus.AKTIV,
                type = Norg2Type.IT,
                overordnetEnhet = null,
            ),
        ),
        ansatte = listOf(NavAnsattFixture.ansatt1),
        tiltakstyper = listOf(TiltakstypeFixtures.AFT, TiltakstypeFixtures.Jobbklubb, TiltakstypeFixtures.Oppfolging),
        avtaler = listOf(avtale),
    )

    beforeEach {
        domain.initialize(database.db)

        tiltakstyper = TiltakstypeService(TiltakstypeRepository(database.db), listOf("INDOPPFAG"))
        avtaler = AvtaleRepository(database.db)
        tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
    }

    test("should fail when tiltakstype is not enabled") {
        val arenakodeEnabledTiltakstyper = listOf<String>()
        tiltakstyper = TiltakstypeService(TiltakstypeRepository(database.db), arenakodeEnabledTiltakstyper)
        val validator = TiltaksgjennomforingValidator(tiltakstyper, avtaler)

        validator.validate(gjennomforing, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            ValidationError(
                "avtaleId",
                "Opprettelse av tiltaksgjennomføring for tiltakstype: 'Oppfølging' er ikke skrudd på enda.",
            ),
        )
    }

    test("should fail when avtale does not exist") {
        val unknownAvtaleId = UUID.randomUUID()

        val validator = TiltaksgjennomforingValidator(tiltakstyper, avtaler)

        val dbo = gjennomforing.copy(avtaleId = unknownAvtaleId)

        validator.validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            ValidationError("avtaleId", "Avtalen finnes ikke"),
        )
    }

    test("should fail when tiltakstype does not match with avtale") {
        avtaler.upsert(avtale.copy(tiltakstypeId = TiltakstypeFixtures.AFT.id))

        val validator = TiltaksgjennomforingValidator(tiltakstyper, avtaler)

        validator.validate(gjennomforing, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            ValidationError("tiltakstypeId", "Tiltakstypen må være den samme som for avtalen"),
        )
    }

    test("should fail when tiltakstype does not support change of oppstartstype") {
        val validator = TiltaksgjennomforingValidator(tiltakstyper, avtaler)

        validator.validate(gjennomforing.copy(oppstart = TiltaksgjennomforingOppstartstype.FELLES), null)
            .shouldBeLeft()
            .shouldContainExactlyInAnyOrder(ValidationError("oppstart", "Tiltaket må ha løpende oppstartstype"))
    }

    test("kan ikke opprette på ikke Aktiv avtale") {
        val id = UUID.randomUUID()
        avtaler.upsert(avtale.copy(id = id))

        forAll(
            row(Avslutningsstatus.AVBRUTT),
            row(Avslutningsstatus.AVSLUTTET),
        ) { status ->
            avtaler.setAvslutningsstatus(id, status)

            val validator = TiltaksgjennomforingValidator(tiltakstyper, avtaler)
            val dbo = gjennomforing.copy(avtaleId = id)

            validator.validate(dbo, null).shouldBeLeft(
                listOf(ValidationError("avtaleId", "Avtalen må være aktiv for å kunne opprette tiltak")),
            )
        }
    }

    test("kan ikke opprette før Avtale startDato") {
        val validator = TiltaksgjennomforingValidator(tiltakstyper, avtaler)
        val dbo = gjennomforing.copy(
            startDato = avtale.startDato.minusDays(1),
        )

        validator.validate(dbo, null).shouldBeLeft(
            listOf(ValidationError("startDato", "Startdato må være etter avtalens startdato")),
        )
    }

    test("should validate fields in the gjennomføring and fields related to the avtale") {
        val validator = TiltaksgjennomforingValidator(tiltakstyper, avtaler)

        forAll(
            row(
                gjennomforing.copy(
                    startDato = avtaleStartDato.minusDays(1),
                    sluttDato = avtaleStartDato,
                ),
                listOf(ValidationError("startDato", "Startdato må være etter avtalens startdato")),
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
                listOf(ValidationError("antallPlasser", "Antall plasser må være større enn 0")),
            ),
            row(
                gjennomforing.copy(navEnheter = listOf("0401")),
                listOf(ValidationError("navEnheter", "NAV-enhet 0401 mangler i avtalen")),
            ),
            row(
                gjennomforing.copy(arrangorOrganisasjonsnummer = "000000003"),
                listOf(ValidationError("arrangorOrganisasjonsnummer", "Arrangøren mangler i avtalen")),
            ),
        ) { input, error ->
            validator.validate(input, null).shouldBeLeft(error)
        }
    }

    context("when gjennomføring already exists") {
        beforeEach {
            tiltaksgjennomforinger.upsert(gjennomforing.copy(administratorer = listOf()))
        }

        afterEach {
            tiltaksgjennomforinger.delete(gjennomforing.id)
        }

        test("Skal godta endringer for antall plasser selv om gjennomføringen er aktiv") {
            val validator = TiltaksgjennomforingValidator(tiltakstyper, avtaler)
            val previous = tiltaksgjennomforinger.get(gjennomforing.id)
            validator.validate(gjennomforing.copy(antallPlasser = 15), previous).shouldBeRight()
        }

        test("Skal godta endringer for startdato selv om gjennomføringen er aktiv, men startdato skal ikke kunne settes til før avtaledatoen") {
            val validator = TiltaksgjennomforingValidator(tiltakstyper, avtaler)
            val previous = tiltaksgjennomforinger.get(gjennomforing.id)
            validator.validate(gjennomforing.copy(startDato = LocalDate.now().plusDays(5)), previous).shouldBeRight()
            validator.validate(gjennomforing.copy(startDato = avtaleStartDato.minusDays(1)), previous).shouldBeLeft(
                listOf(
                    ValidationError("startDato", "Startdato må være etter avtalens startdato"),
                ),
            )
        }

        test("Skal godta endringer for sluttdato frem i tid selv om gjennomføringen er aktiv") {
            val validator = TiltaksgjennomforingValidator(tiltakstyper, avtaler)
            val previous = tiltaksgjennomforinger.get(gjennomforing.id)
            validator.validate(gjennomforing.copy(sluttDato = avtaleSluttDato.plusDays(5)), previous).shouldBeRight()
            validator.validate(gjennomforing.copy(sluttDato = avtaleSluttDato.minusDays(1)), previous).shouldBeLeft(
                listOf(
                    ValidationError("sluttDato", "Sluttdato kan ikke endres bakover i tid når gjennomføringen er aktiv"),
                ),
            )
        }

        test("skal godta endringer selv om avtale er avbrutt") {
            forAll(
                row(Avslutningsstatus.AVBRUTT),
                row(Avslutningsstatus.AVSLUTTET),
            ) { status ->
                avtaler.setAvslutningsstatus(avtale.id, status)

                val validator = TiltaksgjennomforingValidator(tiltakstyper, avtaler)

                val previous = tiltaksgjennomforinger.get(gjennomforing.id)
                validator.validate(gjennomforing, previous).shouldBeRight()
            }
        }

        test("should fail when status is Avsluttet") {
            forAll(
                row(Avslutningsstatus.AVBRUTT),
                row(Avslutningsstatus.AVLYST),
                row(Avslutningsstatus.AVSLUTTET),
            ) { status ->
                tiltaksgjennomforinger.setAvslutningsstatus(gjennomforing.id, status)

                val validator = TiltaksgjennomforingValidator(tiltakstyper, avtaler)

                val previous = tiltaksgjennomforinger.get(gjennomforing.id)
                validator.validate(gjennomforing, previous).shouldBeLeft().shouldContainExactlyInAnyOrder(
                    ValidationError("navn", "Kan bare gjøre endringer når gjennomføringen er aktiv"),
                )
            }
        }
    }
})
