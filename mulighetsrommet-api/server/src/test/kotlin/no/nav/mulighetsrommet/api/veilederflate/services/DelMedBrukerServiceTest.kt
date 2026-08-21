package no.nav.mulighetsrommet.api.veilederflate.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.domain.testing.fixture.AvtaleFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavEnhetFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.domain.tiltakdokument.TiltakDokument
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.persistence.navenhet.SqlNavEnhetRepository
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import java.util.UUID

class DelMedBrukerServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    context("DelMedBrukerService") {
        val service = DelMedBrukerService(database.api, NavEnhetService(SqlNavEnhetRepository(database.api.db)))

        beforeEach {
            MulighetsrommetTestDomain(
                navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
            ).initialize(database.api)
        }

        afterEach {
            database.truncateAll()
        }

        test("opprett deling med bruker for sanity-tiltak") {
            val sanityId = UUID.randomUUID()

            val deling = DelMedBrukerDbo(
                norskIdent = NorskIdent("12345678910"),
                navIdent = NavIdent("B123456"),
                sanityId = sanityId,
                gjennomforingId = null,
                dialogId = "1",
                tiltakstypeId = TiltakstypeFixtures.EnkelAmo.id,
                deltFraEnhet = NavEnhetFixtures.Gjovik.enhetsnummer,
            )

            service.insertDelMedBruker(deling)

            service.getLastDelingMedBruker(
                fnr = NorskIdent("12345678910"),
                sanityOrGjennomforingId = sanityId,
            ).shouldNotBeNull().should {
                it.tiltakId shouldBe sanityId
                it.deling.dialogId shouldBe "1"
            }

            service.insertDelMedBruker(deling.copy(navIdent = NavIdent("B123456"), dialogId = "2"))

            service.getLastDelingMedBruker(
                fnr = NorskIdent("12345678910"),
                sanityOrGjennomforingId = sanityId,
            ).shouldNotBeNull().should {
                it.tiltakId shouldBe sanityId
                it.deling.dialogId shouldBe "2"
            }
        }

        test("opprett deling med bruker for gjennomføring") {
            MulighetsrommetTestDomain(
                avtaler = listOf(AvtaleFixtures.oppfolging),
                gjennomforinger = listOf(GjennomforingFixtures.Oppfolging1),
            ).initialize(database.api)

            val request = DelMedBrukerDbo(
                norskIdent = NorskIdent("12345678910"),
                navIdent = NavIdent("B123456"),
                sanityId = null,
                gjennomforingId = GjennomforingFixtures.Oppfolging1.id,
                dialogId = "1",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                deltFraEnhet = NavEnhetFixtures.Gjovik.enhetsnummer,
            )

            service.insertDelMedBruker(request)

            val delMedBruker = service.getLastDelingMedBruker(
                fnr = NorskIdent("12345678910"),
                sanityOrGjennomforingId = GjennomforingFixtures.Oppfolging1.id,
            )

            delMedBruker.shouldNotBeNull().should {
                it.tiltakId shouldBe GjennomforingFixtures.Oppfolging1.id
                it.deling.dialogId shouldBe "1"
            }
        }

        test("hent siste delinger med bruker per tiltak") {
            MulighetsrommetTestDomain(
                avtaler = listOf(AvtaleFixtures.oppfolging),
                gjennomforinger = listOf(GjennomforingFixtures.Oppfolging1),
            ).initialize(database.api)

            val deling1 = DelMedBrukerDbo(
                norskIdent = NorskIdent("12345678910"),
                navIdent = NavIdent("B123456"),
                sanityId = null,
                gjennomforingId = GjennomforingFixtures.Oppfolging1.id,
                dialogId = "1",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                deltFraEnhet = NavEnhetFixtures.Gjovik.enhetsnummer,
            )

            val deling2 = DelMedBrukerDbo(
                norskIdent = NorskIdent("12345678910"),
                navIdent = NavIdent("B123456"),
                sanityId = null,
                gjennomforingId = GjennomforingFixtures.Oppfolging1.id,
                dialogId = "2",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                deltFraEnhet = NavEnhetFixtures.Gjovik.enhetsnummer,
            )

            val sanityId = UUID.randomUUID()
            val deling3 = DelMedBrukerDbo(
                norskIdent = NorskIdent("12345678910"),
                navIdent = NavIdent("B123456"),
                sanityId = sanityId,
                gjennomforingId = null,
                dialogId = "3",
                tiltakstypeId = TiltakstypeFixtures.EnkelAmo.id,
                deltFraEnhet = NavEnhetFixtures.Gjovik.enhetsnummer,
            )

            service.insertDelMedBruker(deling1)
            service.insertDelMedBruker(deling2)
            service.insertDelMedBruker(deling3)

            service.getAllDistinctDelingMedBruker(fnr = NorskIdent("12345678910")).should {
                it.size shouldBe 2

                it[0].tiltakId shouldBe GjennomforingFixtures.Oppfolging1.id
                it[0].deling.dialogId shouldBe "2"

                it[1].tiltakId shouldBe sanityId
                it[1].deling.dialogId shouldBe "3"
            }
        }

        test("hent historikk over tiltak delt med bruker") {
            val tiltakDokumentIdForEnkeltplass = UUID.randomUUID()
            val tiltakDokumentIdForArbeidstrening = UUID.randomUUID()

            MulighetsrommetTestDomain(
                tiltakstyper = listOf(
                    TiltakstypeFixtures.Oppfolging,
                    TiltakstypeFixtures.EnkelAmo,
                    TiltakstypeFixtures.Arbeidstrening,
                ),
                avtaler = listOf(AvtaleFixtures.oppfolging),
                gjennomforinger = listOf(GjennomforingFixtures.Oppfolging1.copy(navn = "Delt med bruker - tabell")),
            ) {
                repository.tiltakDokument.save(
                    TiltakDokument(
                        id = UUID.randomUUID(),
                        sanityId = tiltakDokumentIdForArbeidstrening,
                        navn = "Delt med bruker - Sanity",
                        tiltakstypeId = TiltakstypeFixtures.Arbeidstrening.id,
                        navEnheter = listOf(NavEnhetNummer("0300")),
                        arrangorId = null,
                        stedForGjennomforing = null,
                        faneinnhold = null,
                        beskrivelse = null,
                        publisert = true,
                        administratorer = emptyList(),
                        kontaktpersoner = emptyList(),
                        arrangorKontaktpersoner = emptyList(),
                        tiltaksnummer = null,
                    ),
                )
                repository.tiltakDokument.save(
                    TiltakDokument(
                        id = UUID.randomUUID(),
                        sanityId = tiltakDokumentIdForEnkeltplass,
                        navn = "Delt med bruker - Lokalt navn fra Sanity",
                        tiltakstypeId = TiltakstypeFixtures.EnkelAmo.id,
                        navEnheter = listOf(NavEnhetNummer("0300")),
                        arrangorId = null,
                        stedForGjennomforing = null,
                        faneinnhold = null,
                        beskrivelse = null,
                        publisert = true,
                        administratorer = emptyList(),
                        kontaktpersoner = emptyList(),
                        arrangorKontaktpersoner = emptyList(),
                        tiltaksnummer = null,
                    ),
                )
            }.initialize(database.api)

            val deling1 = DelMedBrukerDbo(
                norskIdent = NorskIdent("12345678910"),
                navIdent = NavIdent("B123456"),
                sanityId = null,
                gjennomforingId = GjennomforingFixtures.Oppfolging1.id,
                dialogId = "1",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                deltFraEnhet = NavEnhetFixtures.Gjovik.enhetsnummer,
            )

            val deling2 = DelMedBrukerDbo(
                norskIdent = NorskIdent("12345678910"),
                navIdent = NavIdent("B123456"),
                sanityId = tiltakDokumentIdForEnkeltplass,
                gjennomforingId = null,
                dialogId = "2",
                tiltakstypeId = TiltakstypeFixtures.EnkelAmo.id,
                deltFraEnhet = NavEnhetFixtures.Gjovik.enhetsnummer,
            )

            val deling3 = DelMedBrukerDbo(
                norskIdent = NorskIdent("12345678910"),
                navIdent = NavIdent("B123456"),
                sanityId = tiltakDokumentIdForArbeidstrening,
                gjennomforingId = null,
                dialogId = "3",
                tiltakstypeId = TiltakstypeFixtures.Arbeidstrening.id,
                deltFraEnhet = NavEnhetFixtures.Gjovik.enhetsnummer,
            )

            service.insertDelMedBruker(deling1)
            service.insertDelMedBruker(deling2)
            service.insertDelMedBruker(deling3)

            val delMedBruker = service.getAllTiltakDeltMedBruker(NorskIdent("12345678910"))

            delMedBruker.shouldNotBeNull().should {
                it.size shouldBe 3

                it[0].tiltakstype.navn shouldBe "Arbeidstrening"
                it[0].tiltak.navn shouldBe "Delt med bruker - Sanity"

                it[1].tiltakstype.navn shouldBe "Enkel AMO"
                it[1].tiltak.navn shouldBe "Delt med bruker - Lokalt navn fra Sanity"

                it[2].tiltakstype.navn shouldBe "Oppfølging"
                it[2].tiltak.navn shouldBe "Delt med bruker - tabell"
            }
        }
    }
})
