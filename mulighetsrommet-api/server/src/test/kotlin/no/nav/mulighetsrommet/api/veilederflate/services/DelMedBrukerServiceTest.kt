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

        test("opprett deling med bruker for tiltak-dokument") {
            val tiltakDokumentId = UUID.randomUUID()

            MulighetsrommetTestDomain {
                repository.tiltakDokument.save(
                    TiltakDokument(
                        id = tiltakDokumentId,
                        sanityId = null,
                        navn = "Test-tiltak",
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

            val deling = DelMedBrukerDbo(
                norskIdent = NorskIdent("12345678910"),
                navIdent = NavIdent("B123456"),
                tiltakDokumentId = tiltakDokumentId,
                gjennomforingId = null,
                dialogId = "1",
                tiltakstypeId = TiltakstypeFixtures.EnkelAmo.id,
                deltFraEnhet = NavEnhetFixtures.Gjovik.enhetsnummer,
            )

            service.insertDelMedBruker(deling)

            service.getLast(
                fnr = NorskIdent("12345678910"),
                tiltakDokumentOrGjennomforingId = tiltakDokumentId,
            ).shouldNotBeNull().should {
                it.tiltak.id shouldBe tiltakDokumentId
                it.dialogId shouldBe "1"
            }

            service.insertDelMedBruker(deling.copy(navIdent = NavIdent("B123456"), dialogId = "2"))

            service.getLast(
                fnr = NorskIdent("12345678910"),
                tiltakDokumentOrGjennomforingId = tiltakDokumentId,
            ).shouldNotBeNull().should {
                it.tiltak.id shouldBe tiltakDokumentId
                it.dialogId shouldBe "2"
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
                tiltakDokumentId = null,
                gjennomforingId = GjennomforingFixtures.Oppfolging1.id,
                dialogId = "1",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                deltFraEnhet = NavEnhetFixtures.Gjovik.enhetsnummer,
            )

            service.insertDelMedBruker(request)

            val delMedBruker = service.getLast(
                fnr = NorskIdent("12345678910"),
                tiltakDokumentOrGjennomforingId = GjennomforingFixtures.Oppfolging1.id,
            )

            delMedBruker.shouldNotBeNull().should {
                it.tiltak.id shouldBe GjennomforingFixtures.Oppfolging1.id
                it.dialogId shouldBe "1"
            }
        }

        test("hent siste delinger med bruker per tiltak") {
            val tiltakDokumentId = UUID.randomUUID()

            MulighetsrommetTestDomain(
                avtaler = listOf(AvtaleFixtures.oppfolging),
                gjennomforinger = listOf(GjennomforingFixtures.Oppfolging1),
            ) {
                repository.tiltakDokument.save(
                    TiltakDokument(
                        id = tiltakDokumentId,
                        sanityId = null,
                        navn = "Test-tiltak",
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
                tiltakDokumentId = null,
                gjennomforingId = GjennomforingFixtures.Oppfolging1.id,
                dialogId = "1",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                deltFraEnhet = NavEnhetFixtures.Gjovik.enhetsnummer,
            )

            val deling2 = DelMedBrukerDbo(
                norskIdent = NorskIdent("12345678910"),
                navIdent = NavIdent("B123456"),
                tiltakDokumentId = null,
                gjennomforingId = GjennomforingFixtures.Oppfolging1.id,
                dialogId = "2",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                deltFraEnhet = NavEnhetFixtures.Gjovik.enhetsnummer,
            )

            val deling3 = DelMedBrukerDbo(
                norskIdent = NorskIdent("12345678910"),
                navIdent = NavIdent("B123456"),
                tiltakDokumentId = tiltakDokumentId,
                gjennomforingId = null,
                dialogId = "3",
                tiltakstypeId = TiltakstypeFixtures.EnkelAmo.id,
                deltFraEnhet = NavEnhetFixtures.Gjovik.enhetsnummer,
            )

            service.insertDelMedBruker(deling1)
            service.insertDelMedBruker(deling2)
            service.insertDelMedBruker(deling3)

            service.getAll(fnr = NorskIdent("12345678910")).should {
                it.size shouldBe 3

                it[0].tiltak.id shouldBe tiltakDokumentId
                it[0].dialogId shouldBe "3"

                it[1].tiltak.id shouldBe GjennomforingFixtures.Oppfolging1.id
                it[1].dialogId shouldBe "2"
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
                        id = tiltakDokumentIdForArbeidstrening,
                        sanityId = null,
                        navn = "Delt med bruker - tiltak dokument",
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
                        id = tiltakDokumentIdForEnkeltplass,
                        sanityId = null,
                        navn = "Delt med bruker - Lokalt navn",
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
                tiltakDokumentId = null,
                gjennomforingId = GjennomforingFixtures.Oppfolging1.id,
                dialogId = "1",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                deltFraEnhet = NavEnhetFixtures.Gjovik.enhetsnummer,
            )

            val deling2 = DelMedBrukerDbo(
                norskIdent = NorskIdent("12345678910"),
                navIdent = NavIdent("B123456"),
                tiltakDokumentId = tiltakDokumentIdForEnkeltplass,
                gjennomforingId = null,
                dialogId = "2",
                tiltakstypeId = TiltakstypeFixtures.EnkelAmo.id,
                deltFraEnhet = NavEnhetFixtures.Gjovik.enhetsnummer,
            )

            val deling3 = DelMedBrukerDbo(
                norskIdent = NorskIdent("12345678910"),
                navIdent = NavIdent("B123456"),
                tiltakDokumentId = tiltakDokumentIdForArbeidstrening,
                gjennomforingId = null,
                dialogId = "3",
                tiltakstypeId = TiltakstypeFixtures.Arbeidstrening.id,
                deltFraEnhet = NavEnhetFixtures.Gjovik.enhetsnummer,
            )

            service.insertDelMedBruker(deling1)
            service.insertDelMedBruker(deling2)
            service.insertDelMedBruker(deling3)

            val delMedBruker = service.getAll(NorskIdent("12345678910"))

            delMedBruker.shouldNotBeNull().should {
                it.size shouldBe 3

                it[0].tiltakstype.navn shouldBe "Arbeidstrening"
                it[0].tiltak.navn shouldBe "Delt med bruker - tiltak dokument"

                it[1].tiltakstype.navn shouldBe "Enkel AMO"
                it[1].tiltak.navn shouldBe "Delt med bruker - Lokalt navn"

                it[2].tiltakstype.navn shouldBe "Oppfølging"
                it[2].tiltak.navn shouldBe "Delt med bruker - tabell"
            }
        }
    }
})
