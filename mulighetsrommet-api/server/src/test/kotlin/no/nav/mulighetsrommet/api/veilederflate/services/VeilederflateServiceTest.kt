package no.nav.mulighetsrommet.api.veilederflate.services

import arrow.core.nonEmptyListOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.mockk
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeService
import no.nav.mulighetsrommet.api.domain.testing.fixture.ArrangorFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.AvtaleFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavEnhetFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeFeature
import no.nav.mulighetsrommet.api.domain.tiltakdokument.TiltakDokument
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.api.sanity.SanityTiltakstype
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakEnkeltplass
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakEnkeltplassAnskaffet
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakGruppe
import no.nav.mulighetsrommet.api.veilederflate.routes.ApentForPamelding
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.Innsatsgruppe
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.utils.toUUID
import java.util.UUID

class VeilederflateServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val tiltakstypeOppfolging = SanityTiltakstype(
        _id = UUID.randomUUID().toString(),
    )
    val tiltakstypeEnkelAmo = SanityTiltakstype(
        _id = UUID.randomUUID().toString(),
    )
    val tiltakstypeArbeidstrening = SanityTiltakstype(
        _id = UUID.randomUUID().toString(),
    )

    val tiltakEnkelAmo = TiltakDokument(
        id = UUID.randomUUID(),
        sanityId = "6c64a4bd-2ae1-4aee-ad19-716884bf3b5e".toUUID(),
        navn = "Enkel AMO",
        tiltaksnummer = "2023#176408",
        tiltakstypeId = TiltakstypeFixtures.EnkelAmo.id,
        navEnheter = listOf(NavEnhetNummer("0300")),
        arrangorId = ArrangorFixtures.hovedenhet.id,
        stedForGjennomforing = null,
        faneinnhold = null,
        beskrivelse = null,
        publisert = true,
        administratorer = emptyList(),
        kontaktpersoner = emptyList(),
        arrangorKontaktpersoner = emptyList(),
    )

    val tiltakArbeidstrening1 = TiltakDokument(
        id = UUID.randomUUID(),
        sanityId = "f21d1e35-d63b-4de7-a0a5-589e57111527".toUUID(),
        navn = "Arbeidstrening Innlandet",
        tiltaksnummer = null,
        tiltakstypeId = TiltakstypeFixtures.Arbeidstrening.id,
        navEnheter = listOf(NavEnhetNummer("0400"), NavEnhetNummer("0501")),
        arrangorId = null,
        stedForGjennomforing = null,
        faneinnhold = null,
        beskrivelse = null,
        publisert = true,
        administratorer = emptyList(),
        kontaktpersoner = emptyList(),
        arrangorKontaktpersoner = emptyList(),
    )

    val tiltakArbeidstrening2 = TiltakDokument(
        navEnheter = listOf(NavEnhetNummer("0501"), NavEnhetNummer("0400")),
        faneinnhold = Faneinnhold(forHvemInfoboks = "infoboks"),
        id = UUID.randomUUID(),
        sanityId = "82cebdb9-24ef-4f6d-b6b2-6ed45c67d3b6".toUUID(),
        navn = "Arbeidstrening",
        tiltaksnummer = null,
        tiltakstypeId = TiltakstypeFixtures.Arbeidstrening.id,
        arrangorId = null,
        stedForGjennomforing = null,
        beskrivelse = null,
        publisert = true,
        administratorer = emptyList(),
        kontaktpersoner = emptyList(),
        arrangorKontaktpersoner = emptyList(),
    )

    val domain = MulighetsrommetTestDomain(
        navEnheter = listOf(
            NavEnhetFixtures.Innlandet,
            NavEnhetFixtures.Lillehammer,
            NavEnhetFixtures.Gjovik,
            NavEnhetFixtures.Oslo,
        ),
        tiltakstyper = listOf(
            TiltakstypeFixtures.Oppfolging,
            TiltakstypeFixtures.EnkelAmo,
            TiltakstypeFixtures.Arbeidstrening,
            TiltakstypeFixtures.IPS,
        ),
        avtaler = listOf(AvtaleFixtures.oppfolging),
        gjennomforinger = listOf(GjennomforingFixtures.Oppfolging1),
    ) {
        val innsatsgrupper = setOf(
            Innsatsgruppe.TRENGER_VEILEDNING,
            Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
            Innsatsgruppe.JOBBE_DELVIS,
            Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
        )

        repository.tiltakstype.save(TiltakstypeFixtures.EnkelAmo.copy(sanityId = tiltakstypeEnkelAmo._id.toUUID(), innsatsgrupper = innsatsgrupper))

        repository.tiltakstype.save(TiltakstypeFixtures.Arbeidstrening.copy(sanityId = tiltakstypeArbeidstrening._id.toUUID(), innsatsgrupper = innsatsgrupper))

        repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging.copy(sanityId = tiltakstypeOppfolging._id.toUUID(), innsatsgrupper = innsatsgrupper))

        repository.tiltakstype.save(TiltakstypeFixtures.IPS.copy(innsatsgrupper = innsatsgrupper))

        queries.gjennomforing.setPublisert(GjennomforingFixtures.Oppfolging1.id, true)
        queries.gjennomforing.setNavEnheter(
            GjennomforingFixtures.Oppfolging1.id,
            setOf(NavEnhetFixtures.Innlandet.enhetsnummer),
        )
        repository.tiltakDokument.save(tiltakEnkelAmo)
        repository.tiltakDokument.save(tiltakArbeidstrening1)
        repository.tiltakDokument.save(tiltakArbeidstrening2)
    }

    beforeSpec {
        domain.initialize(database.api)
    }

    val sanityService: SanityService = mockk(relaxed = true)

    fun createService(
        features: Map<Tiltakskode, Set<TiltakstypeFeature>> = mapOf(
            Tiltakskode.OPPFOLGING to setOf(TiltakstypeFeature.VISES_I_MODIA),
            Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to setOf(TiltakstypeFeature.VISES_I_MODIA),
            Tiltakskode.ARBEIDSTRENING to setOf(TiltakstypeFeature.VISES_I_MODIA),
        ),
    ): VeilederflateService {
        val tiltakstypeService = TiltakstypeService(
            config = TiltakstypeService.Config(features),
            db = database.admin,
        )
        return VeilederflateService(
            db = database.api,
            tiltakstypeService = tiltakstypeService,
            sanityService = sanityService,
        )
    }

    context("vises i modia") {
        val veilederFlateService = createService(
            features = mapOf(
                Tiltakskode.OPPFOLGING to setOf(TiltakstypeFeature.VISES_I_MODIA),
                Tiltakskode.ARBEIDSTRENING to emptySet(),
            ),
        )

        test("henter tiltakstyper når VISES_I_MODIA er aktivert") {
            val tiltakstyper = veilederFlateService.hentTiltakstyper()

            tiltakstyper.any { it.tiltakskode == Tiltakskode.OPPFOLGING } shouldBe true
            tiltakstyper.any { it.tiltakskode == Tiltakskode.ARBEIDSTRENING } shouldBe false
        }

        test("hentTiltaksgjennomforinger filtrerer bort gjennomføringer for tiltakstyper uten VISES_I_MODIA") {
            val tiltak = veilederFlateService.hentTiltaksgjennomforinger(
                enheter = nonEmptyListOf(NavEnhetNummer("0501")),
                apentForPamelding = listOf(ApentForPamelding.APENT),
                innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
                erSykmeldtMedArbeidsgiver = false,
            )

            tiltak.shouldBeEmpty()
        }

        test("hentTiltaksgjennomforing henter gjennomføringer selv om tiltakstypen er uten VISES_I_MODIA") {
            veilederFlateService.hentTiltaksgjennomforing(
                GjennomforingFixtures.Oppfolging1.id,
            ).shouldNotBeNull()

            veilederFlateService.hentTiltaksgjennomforing(
                tiltakArbeidstrening1.sanityId!!,
            ).shouldNotBeNull()
        }
    }

    test("utleder gjennomføringer som enkeltplass anskaffet tiltak når de har arrangør") {
        val veilederFlateService = createService()

        val tiltak = veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf(NavEnhetNummer("0300")),
            apentForPamelding = listOf(ApentForPamelding.APENT),
            innsatsgruppe = Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
            erSykmeldtMedArbeidsgiver = false,
        )

        tiltak.shouldHaveSize(1).first().shouldBeInstanceOf<VeilederflateTiltakEnkeltplassAnskaffet>().should {
            it.id shouldBe tiltakEnkelAmo.id
            it.arrangor.selskapsnavn shouldBe "Hovedenhet AS"
        }
    }

    test("henter gjennomføringer for anskaffede tiltak når de er publisert") {
        val veilederFlateService = createService()

        database.run {
            queries.gjennomforing.setPublisert(GjennomforingFixtures.Oppfolging1.id, false)
        }

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf(NavEnhetNummer("0400")),
            innsatsgruppe = Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
            tiltakskoder = listOf(Tiltakskode.OPPFOLGING),
            erSykmeldtMedArbeidsgiver = false,
        ).shouldBeEmpty()

        database.run {
            queries.gjennomforing.setPublisert(GjennomforingFixtures.Oppfolging1.id, true)
        }

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf(NavEnhetNummer("0400")),
            innsatsgruppe = Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
            tiltakskoder = listOf(Tiltakskode.OPPFOLGING),
            erSykmeldtMedArbeidsgiver = false,
        ).shouldHaveSize(1).should { (first) ->
            first.shouldBeInstanceOf<VeilederflateTiltakGruppe>().id shouldBe GjennomforingFixtures.Oppfolging1.id
        }
    }

    test("henter gjennomføringer for anskaffede tiltak basert på åpent for påmelding") {
        val veilederFlateService = createService()

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf(NavEnhetNummer("0400")),
            innsatsgruppe = Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
            tiltakskoder = listOf(Tiltakskode.OPPFOLGING),
            apentForPamelding = null,
            erSykmeldtMedArbeidsgiver = false,
        ).shouldHaveSize(1).should { (first) ->
            first.shouldBeInstanceOf<VeilederflateTiltakGruppe>().id shouldBe GjennomforingFixtures.Oppfolging1.id
        }

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf(NavEnhetNummer("0400")),
            innsatsgruppe = Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
            tiltakskoder = listOf(Tiltakskode.OPPFOLGING),
            apentForPamelding = listOf(ApentForPamelding.STENGT),
            erSykmeldtMedArbeidsgiver = false,
        ).shouldBeEmpty()
    }

    test("henter ikke gjennomføringer fra Sanity når filter for 'Åpent for påmelding' er STENGT") {
        val veilederFlateService = createService()

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf(NavEnhetNummer("0501")),
            apentForPamelding = listOf(ApentForPamelding.APENT),
            innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
            erSykmeldtMedArbeidsgiver = false,
        ).shouldHaveSize(2).should { (first, second) ->
            first.shouldBeTypeOf<VeilederflateTiltakEnkeltplass>().id shouldBe tiltakArbeidstrening1.id
            second.shouldBeTypeOf<VeilederflateTiltakEnkeltplass>().id shouldBe tiltakArbeidstrening2.id
        }

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf(NavEnhetNummer("0501")),
            apentForPamelding = null,
            innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
            erSykmeldtMedArbeidsgiver = false,
        ).shouldHaveSize(2).should { (first, second) ->
            first.shouldBeTypeOf<VeilederflateTiltakEnkeltplass>().id shouldBe tiltakArbeidstrening1.id
            second.shouldBeTypeOf<VeilederflateTiltakEnkeltplass>().id shouldBe tiltakArbeidstrening2.id
        }

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf(NavEnhetNummer("0501")),
            apentForPamelding = listOf(ApentForPamelding.STENGT),
            innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
            erSykmeldtMedArbeidsgiver = false,
        ).shouldBeEmpty()
    }

    context("redaksjonelt innhold for tiltakstype hentes fra riktig kilde") {
        val dbBeskrivelse = "Beskrivelse fra databasen"
        val dbFaneinnhold = Faneinnhold(forHvemInfoboks = "DB faneinnhold")

        beforeEach {
            database.run {
                queries.tiltakstype.upsertRedaksjoneltInnhold(
                    TiltakstypeFixtures.Oppfolging.id,
                    dbBeskrivelse,
                    dbFaneinnhold,
                )
            }
        }

        afterEach {
            database.run {
                queries.tiltakstype.upsertRedaksjoneltInnhold(TiltakstypeFixtures.Oppfolging.id, null, null)
            }
        }

        test("henter beskrivelse og faneinnhold fra databasen") {
            val features = mapOf(
                Tiltakskode.OPPFOLGING to setOf(TiltakstypeFeature.VISES_I_MODIA),
            )
            val tiltakstyper = createService(features).hentTiltakstyper()

            val oppfolging = tiltakstyper.find { it.id == TiltakstypeFixtures.Oppfolging.id }
            oppfolging.shouldNotBeNull()
            oppfolging.beskrivelse shouldBe dbBeskrivelse
            oppfolging.faneinnhold shouldBe dbFaneinnhold
        }
    }

    context("tiltak_dokument — DB-first for sanity-tiltak") {
        afterEach {
            database.run {
                queries.tiltakDokument.getAllKompaktDto().items.forEach { g ->
                    queries.tiltakDokument.delete(g.id)
                }
            }
        }

        val veilederFlateService by lazy {
            createService(
                features = mapOf(
                    Tiltakskode.INDIVIDUELL_JOBBSTOTTE to setOf(TiltakstypeFeature.VISES_I_MODIA),
                ),
            )
        }

        test("hentTiltaksgjennomforing finner rad direkte på intern id") {
            val internId = UUID.randomUUID()
            database.run {
                queries.tiltakDokument.save(
                    TiltakDokument(
                        id = internId,
                        navn = "IPS direkte oppslag",
                        tiltakstypeId = TiltakstypeFixtures.IPS.id,
                        tiltaksnummer = "2024#99004",
                        sanityId = null,
                        stedForGjennomforing = null,
                        arrangorId = null,
                        faneinnhold = null,
                        beskrivelse = null,
                        administratorer = emptyList(),
                        kontaktpersoner = emptyList(),
                        arrangorKontaktpersoner = emptyList(),
                        navEnheter = emptyList(),
                        publisert = true,
                    ),
                )
            }

            val tiltak = veilederFlateService.hentTiltaksgjennomforing(internId)
            tiltak.navn shouldBe "IPS direkte oppslag"
        }

        test("hentTiltaksgjennomforing finner rad via sanityId") {
            val sanityId = UUID.randomUUID()
            database.run {
                queries.tiltakDokument.save(
                    TiltakDokument(
                        id = UUID.randomUUID(),
                        navn = "IPS via sanityId",
                        tiltakstypeId = TiltakstypeFixtures.IPS.id,
                        tiltaksnummer = "2024#99005",
                        sanityId = sanityId,
                        stedForGjennomforing = null,
                        arrangorId = null,
                        faneinnhold = null,
                        beskrivelse = null,
                        administratorer = emptyList(),
                        kontaktpersoner = emptyList(),
                        arrangorKontaktpersoner = emptyList(),
                        navEnheter = emptyList(),
                        publisert = true,
                    ),
                )
            }

            val tiltak = veilederFlateService.hentTiltaksgjennomforing(sanityId)
            tiltak.navn shouldBe "IPS via sanityId"
        }
    }
})
