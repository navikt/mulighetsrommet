package no.nav.mulighetsrommet.api.tiltakdokument.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import java.util.UUID

class TiltakDokumentQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val domain = MulighetsrommetTestDomain(
        navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
        ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
        arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
        tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging, TiltakstypeFixtures.AFT),
    )

    beforeSpec {
        domain.initialize(database.api)
    }

    val gjennomforingId = UUID.randomUUID()

    test("upsert og get") {
        database.runAndRollback {
            queries.tiltakDokument.upsert(
                id = gjennomforingId,
                navn = "Test gjennomføring",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                stedForGjennomforing = "Oslo",
                arrangorId = ArrangorFixtures.underenhet1.id,
                faneinnhold = null,
                beskrivelse = "En beskrivelse",
                tiltaksnummer = "2024/1",
                sanityId = null,
            )

            val result = queries.tiltakDokument.get(gjennomforingId)
            result.shouldNotBeNull()
            result.id shouldBe gjennomforingId
            result.navn shouldBe "Test gjennomføring"
            result.stedForGjennomforing shouldBe "Oslo"
            result.beskrivelse shouldBe "En beskrivelse"
            result.tiltaksnummer shouldBe "2024/1"
            result.tiltakstype.id shouldBe TiltakstypeFixtures.Oppfolging.id
            result.arrangor.shouldNotBeNull().id shouldBe ArrangorFixtures.underenhet1.id
            result.publisert shouldBe false
        }
    }

    test("upsert oppdaterer eksisterende rad") {
        database.runAndRollback {
            queries.tiltakDokument.upsert(
                id = gjennomforingId,
                navn = "Originalt navn",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                stedForGjennomforing = null,
                arrangorId = null,
                faneinnhold = null,
                beskrivelse = null,
                tiltaksnummer = null,
                sanityId = null,
            )

            queries.tiltakDokument.upsert(
                id = gjennomforingId,
                navn = "Oppdatert navn",
                tiltakstypeId = TiltakstypeFixtures.AFT.id,
                stedForGjennomforing = "Bergen",
                arrangorId = null,
                faneinnhold = null,
                beskrivelse = "Ny beskrivelse",
                tiltaksnummer = null,
                sanityId = null,
            )

            val result = queries.tiltakDokument.get(gjennomforingId)
            result.shouldNotBeNull()
            result.navn shouldBe "Oppdatert navn"
            result.tiltakstype.id shouldBe TiltakstypeFixtures.AFT.id
            result.stedForGjennomforing shouldBe "Bergen"
            result.beskrivelse shouldBe "Ny beskrivelse"
        }
    }

    test("get returnerer null for ukjent id") {
        database.runAndRollback {
            queries.tiltakDokument.get(UUID.randomUUID()) shouldBe null
        }
    }

    test("getBySanityId") {
        database.runAndRollback {
            val sanityId = UUID.randomUUID()
            queries.tiltakDokument.upsert(
                id = gjennomforingId,
                navn = "Med sanity",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                stedForGjennomforing = null,
                arrangorId = null,
                faneinnhold = null,
                beskrivelse = null,
                tiltaksnummer = null,
                sanityId = sanityId,
            )

            val result = queries.tiltakDokument.getBySanityId(sanityId)
            result.shouldNotBeNull().id shouldBe gjennomforingId

            queries.tiltakDokument.getBySanityId(UUID.randomUUID()).shouldBeNull()
        }
    }

    test("getAll uten filter returnerer alle") {
        database.runAndRollback {
            val id1 = UUID.randomUUID()
            val id2 = UUID.randomUUID()

            queries.tiltakDokument.upsert(
                id = id1,
                navn = "Gjennomføring 1",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                stedForGjennomforing = null,
                arrangorId = null,
                faneinnhold = null,
                beskrivelse = null,
                tiltaksnummer = null,
                sanityId = null,
            )
            queries.tiltakDokument.upsert(
                id = id2,
                navn = "Gjennomføring 2",
                tiltakstypeId = TiltakstypeFixtures.AFT.id,
                stedForGjennomforing = null,
                arrangorId = null,
                faneinnhold = null,
                beskrivelse = null,
                tiltaksnummer = null,
                sanityId = null,
            )

            queries.tiltakDokument.getAll() shouldHaveSize 2
        }
    }

    test("getAll filtrerer på tiltakstype") {
        database.runAndRollback {
            queries.tiltakDokument.upsert(
                id = UUID.randomUUID(),
                navn = "Oppfølging",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                stedForGjennomforing = null,
                arrangorId = null,
                faneinnhold = null,
                beskrivelse = null,
                tiltaksnummer = null,
                sanityId = null,
            )
            queries.tiltakDokument.upsert(
                id = UUID.randomUUID(),
                navn = "AFT",
                tiltakstypeId = TiltakstypeFixtures.AFT.id,
                stedForGjennomforing = null,
                arrangorId = null,
                faneinnhold = null,
                beskrivelse = null,
                tiltaksnummer = null,
                sanityId = null,
            )

            val result = queries.tiltakDokument.getAll(
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging.id),
            )
            result shouldHaveSize 1
            result[0].tiltakstype.id shouldBe TiltakstypeFixtures.Oppfolging.id
        }
    }

    test("getAll filtrerer på publisert") {
        database.runAndRollback {
            val upublisertId = UUID.randomUUID()
            val publisertId = UUID.randomUUID()

            queries.tiltakDokument.upsert(
                id = upublisertId,
                navn = "Upublisert",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                stedForGjennomforing = null,
                arrangorId = null,
                faneinnhold = null,
                beskrivelse = null,
                tiltaksnummer = null,
                sanityId = null,
            )
            queries.tiltakDokument.upsert(
                id = publisertId,
                navn = "Publisert",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                stedForGjennomforing = null,
                arrangorId = null,
                faneinnhold = null,
                beskrivelse = null,
                tiltaksnummer = null,
                sanityId = null,
            )
            queries.tiltakDokument.setPublisert(publisertId, true)

            queries.tiltakDokument.getAll(publisert = true) shouldHaveSize 1
            queries.tiltakDokument.getAll(publisert = false) shouldHaveSize 1
            queries.tiltakDokument.getAll() shouldHaveSize 2
        }
    }

    test("getAll filtrerer på navEnhet") {
        database.runAndRollback {
            val medEnhetId = UUID.randomUUID()
            val utenEnhetId = UUID.randomUUID()

            queries.tiltakDokument.upsert(
                id = medEnhetId,
                navn = "Med enhet",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                stedForGjennomforing = null,
                arrangorId = null,
                faneinnhold = null,
                beskrivelse = null,
                tiltaksnummer = null,
                sanityId = null,
            )
            queries.tiltakDokument.setNavEnheter(medEnhetId, setOf(NavEnhetFixtures.Gjovik.enhetsnummer))

            queries.tiltakDokument.upsert(
                id = utenEnhetId,
                navn = "Uten enhet",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                stedForGjennomforing = null,
                arrangorId = null,
                faneinnhold = null,
                beskrivelse = null,
                tiltaksnummer = null,
                sanityId = null,
            )

            val result = queries.tiltakDokument.getAll(
                navEnheter = listOf(NavEnhetFixtures.Gjovik.enhetsnummer),
            )
            result shouldHaveSize 1
            result[0].id shouldBe medEnhetId
        }
    }

    test("setPublisert") {
        database.runAndRollback {
            queries.tiltakDokument.upsert(
                id = gjennomforingId,
                navn = "Test",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                stedForGjennomforing = null,
                arrangorId = null,
                faneinnhold = null,
                beskrivelse = null,
                tiltaksnummer = null,
                sanityId = null,
            )

            queries.tiltakDokument.setPublisert(gjennomforingId, true)
            queries.tiltakDokument.get(gjennomforingId)?.publisert shouldBe true

            queries.tiltakDokument.setPublisert(gjennomforingId, false)
            queries.tiltakDokument.get(gjennomforingId)?.publisert shouldBe false
        }
    }

    test("setAdministratorer legger til og fjerner") {
        database.runAndRollback {
            queries.tiltakDokument.upsert(
                id = gjennomforingId,
                navn = "Test",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                stedForGjennomforing = null,
                arrangorId = null,
                faneinnhold = null,
                beskrivelse = null,
                tiltaksnummer = null,
                sanityId = null,
            )

            queries.tiltakDokument.setAdministratorer(
                gjennomforingId,
                setOf(NavAnsattFixture.DonaldDuck.navIdent, NavAnsattFixture.MikkeMus.navIdent),
            )
            queries.tiltakDokument.get(gjennomforingId)?.administratorer?.shouldHaveSize(2)

            queries.tiltakDokument.setAdministratorer(
                gjennomforingId,
                setOf(NavAnsattFixture.DonaldDuck.navIdent),
            )
            val result = queries.tiltakDokument.get(gjennomforingId)
            result?.administratorer?.shouldHaveSize(1)
            result?.administratorer?.first()?.navIdent shouldBe NavAnsattFixture.DonaldDuck.navIdent
        }
    }

    test("setNavEnheter legger til og fjerner") {
        database.runAndRollback {
            queries.tiltakDokument.upsert(
                id = gjennomforingId,
                navn = "Test",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                stedForGjennomforing = null,
                arrangorId = null,
                faneinnhold = null,
                beskrivelse = null,
                tiltaksnummer = null,
                sanityId = null,
            )

            queries.tiltakDokument.setNavEnheter(
                gjennomforingId,
                setOf(NavEnhetFixtures.Innlandet.enhetsnummer, NavEnhetFixtures.Gjovik.enhetsnummer),
            )
            queries.tiltakDokument.get(gjennomforingId)?.kontorstruktur.shouldNotBeNull().shouldNotBeEmpty()

            queries.tiltakDokument.setNavEnheter(gjennomforingId, emptySet())
            queries.tiltakDokument.get(gjennomforingId)?.kontorstruktur.shouldBeEmpty()
        }
    }

    test("setKontaktpersoner legger til og fjerner") {
        database.runAndRollback {
            queries.tiltakDokument.upsert(
                id = gjennomforingId,
                navn = "Test",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                stedForGjennomforing = null,
                arrangorId = null,
                faneinnhold = null,
                beskrivelse = null,
                tiltaksnummer = null,
                sanityId = null,
            )

            queries.tiltakDokument.setKontaktpersoner(
                gjennomforingId,
                setOf(
                    TiltakDokumentQueries.KontaktpersonDbo(
                        navIdent = NavAnsattFixture.DonaldDuck.navIdent,
                        beskrivelse = "Kontaktperson for test",
                    ),
                ),
            )
            queries.tiltakDokument.get(gjennomforingId)?.kontaktpersoner?.shouldHaveSize(1)

            queries.tiltakDokument.setKontaktpersoner(gjennomforingId, emptySet())
            queries.tiltakDokument.get(gjennomforingId)?.kontaktpersoner.shouldBeEmpty()
        }
    }

    test("delete fjerner gjennomføringen") {
        database.runAndRollback {
            queries.tiltakDokument.upsert(
                id = gjennomforingId,
                navn = "Skal slettes",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                stedForGjennomforing = null,
                arrangorId = null,
                faneinnhold = null,
                beskrivelse = null,
                tiltaksnummer = null,
                sanityId = null,
            )

            queries.tiltakDokument.get(gjennomforingId).shouldNotBeNull()
            queries.tiltakDokument.delete(gjennomforingId)
            queries.tiltakDokument.get(gjennomforingId).shouldBeNull()
        }
    }
})
