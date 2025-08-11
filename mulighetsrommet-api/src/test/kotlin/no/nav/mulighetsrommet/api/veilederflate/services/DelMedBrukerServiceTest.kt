package no.nav.mulighetsrommet.api.veilederflate.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.api.sanity.SanityTiltaksgjennomforing
import no.nav.mulighetsrommet.api.sanity.SanityTiltakstype
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Innsatsgruppe
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import java.util.*

class DelMedBrukerServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))
    val sanityService: SanityService = mockk(relaxed = true)

    afterEach {
        database.truncateAll()
    }

    context("DelMedBrukerService") {
        val service = DelMedBrukerService(database.db, sanityService, NavEnhetService(database.db))

        beforeEach {
            MulighetsrommetTestDomain(
                navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
            ).initialize(database.db)
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
                gjennomforinger = listOf(GjennomforingFixtures.Oppfolging1),
            ).initialize(database.db)

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
                gjennomforinger = listOf(GjennomforingFixtures.Oppfolging1),
            ).initialize(database.db)

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

            database.assertTable("del_med_bruker").row()
                .value("delt_fra_enhet").isEqualTo(NavEnhetFixtures.Gjovik.enhetsnummer.value)
                .value("delt_fra_fylke").isEqualTo(NavEnhetFixtures.Innlandet.enhetsnummer.value)
        }

        test("hent historikk over tiltak delt med bruker") {
            MulighetsrommetTestDomain(
                tiltakstyper = listOf(
                    TiltakstypeFixtures.Oppfolging,
                    TiltakstypeFixtures.EnkelAmo,
                    TiltakstypeFixtures.Arbeidstrening,
                ),
                gjennomforinger = listOf(GjennomforingFixtures.Oppfolging1.copy(navn = "Delt med bruker - tabell")),
            ).initialize(database.db)

            val sanityGjennomforingIdForEnkeltplass = UUID.randomUUID()
            val sanityGjennomforingIdForArbeidstrening = UUID.randomUUID()

            coEvery {
                sanityService.getAllTiltak(any(), any())
            } returns listOf(
                SanityTiltaksgjennomforing(
                    _id = sanityGjennomforingIdForEnkeltplass.toString(),
                    tiltaksgjennomforingNavn = "Delt med bruker - Lokalt navn fra Sanity",
                    tiltakstype = SanityTiltakstype(
                        _id = UUID.randomUUID().toString(),
                        tiltakstypeNavn = "Arbeidsmarkedsopplæring (AMO) enkeltplass",
                        innsatsgrupper = Innsatsgruppe.entries.toSet(),
                    ),
                ),
                SanityTiltaksgjennomforing(
                    _id = sanityGjennomforingIdForArbeidstrening.toString(),
                    tiltaksgjennomforingNavn = "Delt med bruker - Sanity",
                    tiltakstype = SanityTiltakstype(
                        _id = UUID.randomUUID().toString(),
                        tiltakstypeNavn = "Arbeidstrening",
                        innsatsgrupper = Innsatsgruppe.entries.toSet(),
                    ),
                ),
            )

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
                sanityId = sanityGjennomforingIdForEnkeltplass,
                gjennomforingId = null,
                dialogId = "2",
                tiltakstypeId = TiltakstypeFixtures.EnkelAmo.id,
                deltFraEnhet = NavEnhetFixtures.Gjovik.enhetsnummer,
            )

            val deling3 = DelMedBrukerDbo(
                norskIdent = NorskIdent("12345678910"),
                navIdent = NavIdent("B123456"),
                sanityId = sanityGjennomforingIdForArbeidstrening,
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
