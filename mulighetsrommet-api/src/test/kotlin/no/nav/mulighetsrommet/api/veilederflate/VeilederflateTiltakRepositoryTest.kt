package no.nav.mulighetsrommet.api.veilederflate

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotliquery.Query
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Innlandet
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Lillehammer
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Oslo
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Sel
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.Oppfolging1
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.dto.Innsatsgruppe
import java.util.*

class VeilederflateTiltakRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        enheter = listOf(
            NavEnhetFixtures.IT,
            Innlandet,
            Gjovik,
            Lillehammer,
            Sel,
            Oslo,
        ),
        tiltakstyper = listOf(
            TiltakstypeFixtures.AFT,
            TiltakstypeFixtures.VTA,
            TiltakstypeFixtures.ArbeidsrettetRehabilitering,
            TiltakstypeFixtures.GruppeAmo,
            TiltakstypeFixtures.Oppfolging,
            TiltakstypeFixtures.Jobbklubb,
            TiltakstypeFixtures.DigitalOppfolging,
            TiltakstypeFixtures.Avklaring,
            TiltakstypeFixtures.GruppeFagOgYrkesopplaering,
            TiltakstypeFixtures.EnkelAmo,
        ),
        avtaler = listOf(
            AvtaleFixtures.oppfolging,
            AvtaleFixtures.VTA,
            AvtaleFixtures.AFT,
            AvtaleFixtures.jobbklubb,
            AvtaleFixtures.EnkelAmo,
            AvtaleFixtures.ArbeidsrettetRehabilitering,
        ),
    )

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.db.truncateAll()
    }

    context("getAll") {
        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
        val veilederflateTiltakRepository = VeilederflateTiltakRepository(database.db)

        val oppfolgingSanityId = UUID.randomUUID()
        val arbeidstreningSanityId = UUID.randomUUID()
        val arbeidsrettetRehabilitering = UUID.randomUUID()

        beforeEach {
            Query("update tiltakstype set sanity_id = '$oppfolgingSanityId' where id = '${TiltakstypeFixtures.Oppfolging.id}'")
                .asUpdate
                .let { database.db.run(it) }
            Query("update tiltakstype set sanity_id = '$arbeidstreningSanityId' where id = '${TiltakstypeFixtures.AFT.id}'")
                .asUpdate
                .let { database.db.run(it) }
            Query("update tiltakstype set sanity_id = '$arbeidsrettetRehabilitering' where id = '${TiltakstypeFixtures.ArbeidsrettetRehabilitering.id}'")
                .asUpdate
                .let { database.db.run(it) }
            Query("update tiltakstype set innsatsgrupper = array ['${Innsatsgruppe.VARIG_TILPASSET_INNSATS}'::innsatsgruppe]")
                .asUpdate
                .let { database.db.run(it) }
        }

        test("skal filtrere basert på om tiltaket er publisert") {
            tiltaksgjennomforinger.upsert(Oppfolging1)
            tiltaksgjennomforinger.setPublisert(Oppfolging1.id, true)

            tiltaksgjennomforinger.upsert(AFT1)
            tiltaksgjennomforinger.setPublisert(AFT1.id, true)

            veilederflateTiltakRepository.getAll(
                brukersEnheter = listOf("0502"),
                innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
            ) shouldHaveSize 2

            tiltaksgjennomforinger.setPublisert(Oppfolging1.id, false)

            veilederflateTiltakRepository.getAll(
                brukersEnheter = listOf("0502"),
                innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
            ) shouldHaveSize 1

            tiltaksgjennomforinger.setPublisert(AFT1.id, false)

            veilederflateTiltakRepository.getAll(
                brukersEnheter = listOf("0502"),
                innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
            ) shouldHaveSize 0
        }

        test("skal filtrere basert på innsatsgruppe") {
            tiltaksgjennomforinger.upsert(Oppfolging1)
            tiltaksgjennomforinger.setPublisert(Oppfolging1.id, true)

            tiltaksgjennomforinger.upsert(AFT1)
            tiltaksgjennomforinger.setPublisert(AFT1.id, true)

            Query("update tiltakstype set innsatsgrupper = array ['${Innsatsgruppe.SPESIELT_TILPASSET_INNSATS}'::innsatsgruppe] where id = '${TiltakstypeFixtures.Oppfolging.id}'")
                .asUpdate
                .let { database.db.run(it) }
            Query("update tiltakstype set innsatsgrupper = array ['${Innsatsgruppe.STANDARD_INNSATS}'::innsatsgruppe, '${Innsatsgruppe.SPESIELT_TILPASSET_INNSATS}'::innsatsgruppe] where id = '${TiltakstypeFixtures.AFT.id}'")
                .asUpdate
                .let { database.db.run(it) }

            veilederflateTiltakRepository.getAll(
                innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS,
                brukersEnheter = listOf("0502"),
            ).should {
                it.shouldHaveSize(1).first().id.shouldBe(AFT1.id)
            }

            veilederflateTiltakRepository.getAll(
                innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
                brukersEnheter = listOf("0502"),
            ) shouldHaveSize 2
        }

        test("skal filtrere på brukers enheter") {
            tiltaksgjennomforinger.upsert(Oppfolging1.copy(sluttDato = null, navEnheter = listOf("2990", "0400")))
            tiltaksgjennomforinger.setPublisert(Oppfolging1.id, true)

            tiltaksgjennomforinger.upsert(AFT1.copy(navEnheter = listOf("2990", "0300")))
            tiltaksgjennomforinger.setPublisert(AFT1.id, true)

            veilederflateTiltakRepository.getAll(
                brukersEnheter = listOf("0400"),
                innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
            ).should {
                it.shouldHaveSize(1).first().id.shouldBe(Oppfolging1.id)
            }

            veilederflateTiltakRepository.getAll(
                innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                brukersEnheter = listOf("0300"),
            ).should {
                it.shouldHaveSize(1).first().id.shouldBe(AFT1.id)
            }

            veilederflateTiltakRepository.getAll(
                innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                brukersEnheter = listOf("0400", "0300"),
            ) shouldHaveSize 2

            veilederflateTiltakRepository.getAll(
                innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                brukersEnheter = listOf("2990"),
            ) shouldHaveSize 2
        }

        test("skal filtrere basert på tiltakstype sanity Id") {
            tiltaksgjennomforinger.upsert(Oppfolging1)
            tiltaksgjennomforinger.setPublisert(Oppfolging1.id, true)

            tiltaksgjennomforinger.upsert(AFT1)
            tiltaksgjennomforinger.setPublisert(AFT1.id, true)

            veilederflateTiltakRepository.getAll(
                sanityTiltakstypeIds = null,
                brukersEnheter = listOf("0502"),
                innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
            ) shouldHaveSize 2

            veilederflateTiltakRepository.getAll(
                innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                sanityTiltakstypeIds = listOf(oppfolgingSanityId),
                brukersEnheter = listOf("0502"),
            ).should {
                it.shouldHaveSize(1).first().id.shouldBe(Oppfolging1.id)
            }

            veilederflateTiltakRepository.getAll(
                innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                sanityTiltakstypeIds = listOf(arbeidstreningSanityId),
                brukersEnheter = listOf("0502"),
            ).should {
                it.shouldHaveSize(1).first().id.shouldBe(AFT1.id)
            }
        }

        test("skal filtrere basert fritekst i navn") {
            tiltaksgjennomforinger.upsert(Oppfolging1.copy(sluttDato = null, navn = "Oppfølging hos Erik"))
            tiltaksgjennomforinger.upsert(AFT1.copy(navn = "AFT hos Frank"))
            tiltaksgjennomforinger.setPublisert(Oppfolging1.id, true)
            tiltaksgjennomforinger.setPublisert(AFT1.id, true)

            veilederflateTiltakRepository.getAll(
                innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                brukersEnheter = listOf("0502"),
                search = "erik",
            ).should {
                it.shouldHaveSize(1).first().id.shouldBe(Oppfolging1.id)
            }

            veilederflateTiltakRepository.getAll(
                innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                brukersEnheter = listOf("0502"),
                search = "frank aft",
            ).should {
                it.shouldHaveSize(1).first().id.shouldBe(AFT1.id)
            }

            veilederflateTiltakRepository.getAll(
                innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                brukersEnheter = listOf("0502"),
                search = "aft erik",
            ).should {
                it shouldHaveSize 0
            }

            veilederflateTiltakRepository.getAll(
                innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                brukersEnheter = listOf("0502"),
                search = "af",
            ).should {
                it.shouldHaveSize(1).first().id.shouldBe(AFT1.id)
            }
        }

        test("skal filtrere basert på apent_for_pamelding") {
            tiltaksgjennomforinger.upsert(Oppfolging1)
            tiltaksgjennomforinger.setPublisert(Oppfolging1.id, true)

            tiltaksgjennomforinger.upsert(AFT1)
            tiltaksgjennomforinger.setPublisert(AFT1.id, true)
            tiltaksgjennomforinger.setApentForPamelding(AFT1.id, false)

            veilederflateTiltakRepository.getAll(
                innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                apentForPamelding = true,
                brukersEnheter = listOf("0502"),
            ).should {
                it.shouldHaveSize(1).first().id.shouldBe(Oppfolging1.id)
            }

            veilederflateTiltakRepository.getAll(
                innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                apentForPamelding = false,
                brukersEnheter = listOf("0502"),
            ).should {
                it.shouldHaveSize(1).first().id.shouldBe(AFT1.id)
            }

            veilederflateTiltakRepository.getAll(
                innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                apentForPamelding = null,
                brukersEnheter = listOf("0502"),
            ) shouldHaveSize 2
        }

        test("skal ta med ARR hvis sykmeldt med SITUASJONSBESTEMT_INNSATS") {
            tiltaksgjennomforinger.upsert(TiltaksgjennomforingFixtures.ArbeidsrettetRehabilitering)
            tiltaksgjennomforinger.setPublisert(TiltaksgjennomforingFixtures.ArbeidsrettetRehabilitering.id, true)

            // Riktig innsatsgruppe
            veilederflateTiltakRepository.getAll(
                innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                brukersEnheter = listOf("0502"),
            ).size shouldBe 1

            // Feil innsatsgruppe
            veilederflateTiltakRepository.getAll(
                innsatsgruppe = Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
                brukersEnheter = listOf("0502"),
            ).size shouldBe 0

            // Feil innsatsgruppe men sykmeldt
            veilederflateTiltakRepository.getAll(
                innsatsgruppe = Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
                brukersEnheter = listOf("0502"),
                erSykmeldtMedArbeidsgiver = true,
            ).size shouldBe 1
        }
    }
})
