package no.nav.mulighetsrommet.api.veilederflate

import arrow.core.nonEmptyListOf
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeService
import no.nav.mulighetsrommet.api.domain.testing.fixture.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeFeature
import no.nav.mulighetsrommet.api.sanity.VeilederflateSanityService
import no.nav.mulighetsrommet.api.veilederflate.testing.TestVeilederflateDatabase
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Innsatsgruppe
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDate
import java.util.UUID

class VeilederflateServiceTest : FunSpec({

    fun createService(
        features: Map<Tiltakskode, Set<TiltakstypeFeature>> = mapOf(
            Tiltakskode.OPPFOLGING to setOf(TiltakstypeFeature.VISES_I_MODIA),
            Tiltakskode.ARBEIDSTRENING to setOf(TiltakstypeFeature.VISES_I_MODIA),
        ),
    ): Pair<TestVeilederflateDatabase, VeilederflateService> {
        val db = TestVeilederflateDatabase()
        db.repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)
        db.repository.tiltakstype.save(TiltakstypeFixtures.Arbeidstrening)
        val service = VeilederflateService(
            db = db,
            tiltakstypeService = TiltakstypeService(TiltakstypeService.Config(features), mockk(relaxed = true)),
            sanityService = mockk<VeilederflateSanityService>(relaxed = true),
        )
        return db to service
    }

    fun gruppetiltak(id: UUID, tiltakskode: Tiltakskode) = VeilederflateTiltakDbo(
        id = id,
        tiltakskode = tiltakskode,
        navn = "Gruppetiltak",
        status = VeilederflateTiltakGruppeStatus(GjennomforingStatusType.GJENNOMFORES, "Gjennomføres"),
        tiltaksnummer = null,
        apentForPamelding = true,
        oppstartsdato = LocalDate.of(2024, 1, 1),
        sluttdato = null,
        oppstart = GjennomforingOppstartstype.LOPENDE,
        oppmoteSted = null,
        arrangor = VeilederflateArrangor(
            selskapsnavn = "Arrangør AS",
            organisasjonsnummer = "123456789",
            kontaktpersoner = emptyList(),
        ),
        kontaktinfo = VeilederflateKontaktinfo(emptyList()),
        beskrivelse = null,
        faneinnhold = null,
        fylker = emptyList(),
        enheter = emptyList(),
        estimertVentetid = null,
        personvernBekreftet = false,
        personopplysningerSomKanBehandles = emptyList(),
        lopenummer = "2024/1",
        stengt = emptyList(),
    )

    fun tiltakDokument(id: UUID, tiltakskode: Tiltakskode) = VeilederflateTiltakDokument(
        id = id,
        sanityId = null,
        navn = "Tiltak dokument",
        beskrivelse = null,
        faneinnhold = null,
        tiltaksnummer = null,
        tiltakskode = tiltakskode,
        stedForGjennomforing = null,
        navEnheter = emptyList(),
        kontaktpersoner = emptyList(),
        arrangor = null,
        arrangorKontaktpersoner = emptyList(),
    )

    test("hentInnsatsgrupper returnerer alle innsatsgrupper") {
        val (_, service) = createService()

        service.hentInnsatsgrupper() shouldHaveSize Innsatsgruppe.entries.size
    }

    test("hentTiltakstyper filtrerer bort tiltakstyper uten VISES_I_MODIA") {
        val (_, service) = createService(
            features = mapOf(
                Tiltakskode.OPPFOLGING to setOf(TiltakstypeFeature.VISES_I_MODIA),
                Tiltakskode.ARBEIDSTRENING to emptySet(),
            ),
        )

        val tiltakstyper = service.hentTiltakstyper()

        tiltakstyper.map { it.tiltakskode } shouldBe listOf(Tiltakskode.OPPFOLGING)
    }

    test("hentTiltaksgjennomforing returnerer gruppetiltak fra veilederTiltak-query") {
        val (db, service) = createService()
        val id = UUID.randomUUID()
        every { db.queries.veilederTiltak.get(id) } returns gruppetiltak(id, Tiltakskode.OPPFOLGING)

        val tiltak = service.hentTiltaksgjennomforing(id)

        tiltak.shouldBeInstanceOf<VeilederflateTiltakGruppe>().id shouldBe id
    }

    test("hentTiltaksgjennomforing faller tilbake til tiltak-dokument når gruppetiltak mangler") {
        val (db, service) = createService()
        val id = UUID.randomUUID()
        every { db.queries.veilederTiltak.get(id) } returns null
        every { db.queries.veilederTiltak.getTiltakDokument(id) } returns tiltakDokument(id, Tiltakskode.OPPFOLGING)

        val tiltak = service.hentTiltaksgjennomforing(id)

        tiltak.shouldBeInstanceOf<VeilederflateTiltakEnkeltplass>().id shouldBe id
    }

    test("hentTiltaksgjennomforing kaster StatusException NotFound når tiltaket ikke finnes") {
        val (db, service) = createService()
        val id = UUID.randomUUID()
        every { db.queries.veilederTiltak.get(id) } returns null
        every { db.queries.veilederTiltak.getTiltakDokument(id) } returns null

        shouldThrow<StatusException> {
            service.hentTiltaksgjennomforing(id)
        }.status shouldBe HttpStatusCode.NotFound
    }

    test("hentTiltaksgjennomforinger returnerer gruppetiltak og filtrerer på VISES_I_MODIA") {
        val (db, service) = createService(
            features = mapOf(Tiltakskode.OPPFOLGING to setOf(TiltakstypeFeature.VISES_I_MODIA)),
        )
        val id = UUID.randomUUID()
        every {
            db.queries.veilederTiltak.getAll(any(), any(), any(), any(), any(), any())
        } returns listOf(gruppetiltak(id, Tiltakskode.OPPFOLGING))

        val tiltak = service.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf(NavEnhetNummer("0400")),
            innsatsgruppe = Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
            apentForPamelding = listOf(ApentForPamelding.APENT),
            erSykmeldtMedArbeidsgiver = false,
        )

        tiltak.shouldHaveSize(1).first().shouldBeInstanceOf<VeilederflateTiltakGruppe>().id shouldBe id
    }
})
