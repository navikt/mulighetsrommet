package no.nav.mulighetsrommet.admin.gjennomforing

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.mulighetsrommet.admin.deltaker.DeltakelserError
import no.nav.mulighetsrommet.admin.deltaker.DeltakelserPort
import no.nav.mulighetsrommet.admin.navansatt.NavAnsattContext
import no.nav.mulighetsrommet.admin.testing.GjennomforingFixtures
import no.nav.mulighetsrommet.admin.testing.TestAdminDatabase
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeService
import no.nav.mulighetsrommet.api.domain.deltaker.Deltaker
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingArena
import no.nav.mulighetsrommet.api.domain.personalia.AvvistGrunn
import no.nav.mulighetsrommet.api.domain.personalia.Gradering
import no.nav.mulighetsrommet.api.domain.personalia.Personalia
import no.nav.mulighetsrommet.api.domain.testing.fixture.DeltakerFixtures
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeFeature
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.validation.FieldError
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class ArenaEnkeltplassDtoQueryTest : FunSpec({
    val accessType = AccessType.OBO.AzureAd("test-token")
    val navIdent = NavIdent("Z123456")
    val ansattContext = NavAnsattContext(navIdent = navIdent, accessType = accessType)
    lateinit var arena: GjennomforingArena
    lateinit var deltaker: Deltaker
    lateinit var db: TestAdminDatabase
    lateinit var detaljer: GjennomforingDetaljerService

    beforeEach {
        arena = GjennomforingFixtures.createGjennomforingArena(tiltaksnummer = Tiltaksnummer("2020#42"))
        deltaker = DeltakerFixtures.createDeltaker(
            gjennomforingId = arena.id,
            status = DeltakerStatusType.DELTAR,
            startDato = LocalDate.of(2026, 1, 1),
            sluttDato = LocalDate.of(2026, 12, 31),
            endretTidspunkt = Instant.parse("2026-01-02T10:00:00Z"),
        )
        db = TestAdminDatabase()
        db.repository.deltaker.save(deltaker)
        every { db.repository.gjennomforing.getGjennomforing(arena.id) } returns arena
        every { db.queries.gjennomforing.findIdsByArenaTiltaksnummer(Tiltaksnummer("2020/42")) } returns listOf(arena.id)
        detaljer = mockk<GjennomforingDetaljerService>()
    }

    fun deltakerDto(deltaker: Deltaker) = DeltakerDto.from(
        deltaker,
        Personalia(
            deltakerId = deltaker.id,
            norskIdent = NorskIdent("12345678901"),
            navn = "Test Testesen",
            oppfolgingEnhet = null,
            geografiskEnhet = null,
            region = null,
            gradering = Gradering.UGRADERT,
            avvistGrunn = null,
        ),
        navVeilederNavn = "Mus, Mikke",
        dato = LocalDate.of(2026, 1, 1),
    )

    fun createQuery(
        db: TestAdminDatabase,
        detaljer: GjennomforingDetaljerService = mockk(),
        deltakelser: DeltakelserPort = mockk(),
        migrerteTiltakskoder: Set<Tiltakskode> = setOf(Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING),
    ) = ArenaEnkeltplassDtoQuery(
        db,
        TiltakstypeService(
            TiltakstypeService.Config(migrerteTiltakskoder.associateWith { setOf(TiltakstypeFeature.MIGRERT) }),
            db,
        ),
        detaljer,
        deltakelser,
    )

    test("returnerer komplett forhåndsvisning med deltakeropplysninger for innlogget saksbehandler") {
        coEvery { detaljer.findDeltakerDto(arena.id, accessType, navIdent) } returns deltakerDto(deltaker).right()

        val resultat = createQuery(db, detaljer)
            .execute(FinnArenaEnkeltplassInput("2020/42"), ansattContext)
            .shouldBeRight()
            .single()

        resultat.deltaker.shouldBeTypeOf<DeltakerDto>().navn shouldBe "Test Testesen"
        resultat.deltaker.norskIdent shouldBe NorskIdent("12345678901")
        resultat.deltaker.navVeilederNavn shouldBe "Mus, Mikke"
    }

    test("oppslag godtar skråstrek og omkringliggende mellomrom") {
        val detaljer = mockk<GjennomforingDetaljerService>()
        coEvery { detaljer.findDeltakerDto(arena.id, accessType, navIdent) } returns deltakerDto(deltaker).right()

        val resultat = createQuery(db, detaljer)
            .execute(FinnArenaEnkeltplassInput(" 2020/42 "), ansattContext)
            .shouldBeRight()
            .single()

        resultat.tiltaksnummer shouldBe Tiltaksnummer("2020#42")
        verify(exactly = 1) {
            db.queries.gjennomforing.findIdsByArenaTiltaksnummer(Tiltaksnummer("2020/42"))
        }
    }

    test("ukjent tiltaksnummer gir tomt resultat uten oppslag av deltakeropplysninger") {
        every { db.queries.gjennomforing.findIdsByArenaTiltaksnummer(Tiltaksnummer("2026/999999")) } returns emptyList()
        val detaljer = mockk<GjennomforingDetaljerService>()

        createQuery(db, detaljer)
            .execute(FinnArenaEnkeltplassInput("2026/999999"), ansattContext)
            .shouldBeRight()
            .shouldBeEmpty()

        coVerify { detaljer wasNot Called }
    }

    test("oppslag på fødselsnummer returnerer alle relevante Arena-gjennomføringer") {
        val annenArena = GjennomforingFixtures.createGjennomforingArena(tiltaksnummer = Tiltaksnummer("2026#43"))
        val annenDeltaker = DeltakerFixtures.createDeltaker(
            gjennomforingId = annenArena.id,
            status = DeltakerStatusType.FULLFORT,
            startDato = LocalDate.of(2026, 1, 1),
            sluttDato = LocalDate.of(2026, 6, 30),
            endretTidspunkt = Instant.parse("2026-07-01T10:00:00Z"),
        )
        every { db.repository.gjennomforing.getGjennomforing(annenArena.id) } returns annenArena
        db.repository.deltaker.save(annenDeltaker)
        val deltakelser = mockk<DeltakelserPort>()
        coEvery {
            deltakelser.finnDeltakerIds(NorskIdent("12345678901"), accessType)
        } returns listOf(deltaker.id, annenDeltaker.id).right()
        coEvery {
            detaljer.findDeltakerDto(arena.id, accessType, navIdent)
        } returns deltakerDto(deltaker).right()
        coEvery {
            detaljer.findDeltakerDto(annenArena.id, accessType, navIdent)
        } returns deltakerDto(annenDeltaker).right()

        val resultat = createQuery(db, detaljer, deltakelser)
            .execute(FinnArenaEnkeltplassInput(" 12345678901 "), ansattContext)
            .shouldBeRight()

        resultat.map { it.id } shouldContainExactly listOf(arena.id, annenArena.id)
    }

    test("oppslag på fødselsnummer utelater deltakelser avsluttet før 2026") {
        db.repository.deltaker.save(
            DeltakerFixtures.createDeltaker(
                id = deltaker.id,
                gjennomforingId = arena.id,
                status = DeltakerStatusType.FULLFORT,
                startDato = LocalDate.of(2020, 1, 1),
                sluttDato = LocalDate.of(2020, 6, 30),
                endretTidspunkt = Instant.parse("2020-07-01T10:00:00Z"),
            ),
        )
        val deltakelser = mockk<DeltakelserPort>()
        coEvery {
            deltakelser.finnDeltakerIds(NorskIdent("12345678901"), accessType)
        } returns listOf(deltaker.id).right()

        createQuery(db, detaljer, deltakelser)
            .execute(FinnArenaEnkeltplassInput("12345678901"), ansattContext)
            .shouldBeRight()
            .shouldBeEmpty()

        coVerify { detaljer wasNot Called }
    }

    test("oppslag på fødsnummer uten deltakelser gir tomt resultat") {
        val deltakelser = mockk<DeltakelserPort>()
        coEvery { deltakelser.finnDeltakerIds(NorskIdent("12345678901"), accessType) } returns emptyList<UUID>().right()

        createQuery(db, deltakelser = deltakelser)
            .execute(FinnArenaEnkeltplassInput("12345678901"), ansattContext)
            .shouldBeRight()
            .shouldBeEmpty()
    }

    test("tiltakstypen må være migrert før den tilbys for migrering") {
        createQuery(db, migrerteTiltakskoder = emptySet())
            .execute(FinnArenaEnkeltplassInput("2020/42"), ansattContext)
            .shouldBeRight()
            .shouldBeEmpty()
    }

    test("migrering av en annen enkeltplasstype åpner ikke migrering av gjennomføringen") {
        val detaljer = mockk<GjennomforingDetaljerService>()
        val query = createQuery(
            db,
            detaljer,
            migrerteTiltakskoder = setOf(Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING),
        )

        query.execute(FinnArenaEnkeltplassInput("2020/42"), ansattContext).shouldBeRight().shouldBeEmpty()

        coVerify { detaljer wasNot Called }
    }

    test("oppslag avviser en deltaker som ikke kan migreres") {
        db.repository.deltaker.save(
            deltaker.registrerStatus(
                DeltakerStatusType.FEILREGISTRERT,
                endretTidspunkt = Instant.parse("2026-02-01T10:00:00Z"),
            ),
        )
        val detaljer = mockk<GjennomforingDetaljerService>()

        createQuery(db, detaljer)
            .execute(FinnArenaEnkeltplassInput("2020/42"), ansattContext)
            .shouldBeRight()
            .shouldBeEmpty()

        coVerify { detaljer wasNot Called }
    }

    test("tilbyr ikke migrering når deltakeren ikke lenger finnes ved oppslag") {
        val detaljer = mockk<GjennomforingDetaljerService>()
        coEvery { detaljer.findDeltakerDto(arena.id, accessType, navIdent) } returns null.right()

        createQuery(db, detaljer)
            .execute(FinnArenaEnkeltplassInput("2020/42"), ansattContext)
            .shouldBeRight()
            .shouldBeEmpty()
    }

    test("ugyldig søk avvises uten oppslag av deltakeropplysninger") {
        val detaljer = mockk<GjennomforingDetaljerService>()

        val error = createQuery(db, detaljer).execute(FinnArenaEnkeltplassInput("ugyldig"), ansattContext)
            .shouldBeLeft()
            .shouldBeTypeOf<FinnArenaEnkeltplassError.Valideringsfeil>()

        error.errors shouldBe listOf(
            FieldError("/", "Oppgi et tiltaksnummer (for eksempel 2026/1234) eller et fødselsnummer på 11 siffer"),
        )
        coVerify { detaljer wasNot Called }
    }

    test("returnerer tilgangsfeil uten forhåndsvisning når deltakeropplysninger er avvist") {
        val detaljer = mockk<GjennomforingDetaljerService>()
        coEvery { detaljer.findDeltakerDto(arena.id, accessType, navIdent) } answers {
            GjennomforingDetaljerError.PersonaliaSjekkAvvist(AvvistGrunn.AVVIST_FORTROLIG_ADRESSE).left()
        }

        createQuery(db, detaljer).execute(FinnArenaEnkeltplassInput("2020/42"), ansattContext).shouldBeLeft(
            FinnArenaEnkeltplassError.ManglerTilgang(AvvistGrunn.AVVIST_FORTROLIG_ADRESSE),
        )
    }

    test("identitetssøk uten persontilgang avvises før forhåndsvisning") {
        val detaljer = mockk<GjennomforingDetaljerService>()
        val deltakelser = mockk<DeltakelserPort>()
        coEvery { deltakelser.finnDeltakerIds(NorskIdent("12345678901"), accessType) } answers {
            DeltakelserError.ManglerTilgang(AvvistGrunn.AVVIST_FORTROLIG_ADRESSE).left()
        }

        createQuery(db, detaljer, deltakelser)
            .execute(FinnArenaEnkeltplassInput("12345678901"), ansattContext)
            .shouldBeLeft(FinnArenaEnkeltplassError.ManglerTilgang(AvvistGrunn.AVVIST_FORTROLIG_ADRESSE))

        coVerify { detaljer wasNot Called }
    }

    test("oppslagsfeil fra Komet returneres uten detaljer om deltaker") {
        val detaljer = mockk<GjennomforingDetaljerService>()
        val deltakelser = mockk<DeltakelserPort>()
        coEvery {
            deltakelser.finnDeltakerIds(NorskIdent("12345678901"), accessType)
        } returns DeltakelserError.OppslagFeilet.left()

        createQuery(db, detaljer, deltakelser)
            .execute(FinnArenaEnkeltplassInput("12345678901"), ansattContext)
            .shouldBeLeft(FinnArenaEnkeltplassError.OppslagFeilet)

        coVerify { detaljer wasNot Called }
    }
})
