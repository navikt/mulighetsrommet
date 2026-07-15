package no.nav.mulighetsrommet.api.individuellgjennomforing.db

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

class IndividuellGjennomforingQueriesTest : FunSpec({
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
            queries.individuellGjennomforing.upsert(
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

            val result = queries.individuellGjennomforing.get(gjennomforingId)
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
            queries.individuellGjennomforing.upsert(
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

            queries.individuellGjennomforing.upsert(
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

            val result = queries.individuellGjennomforing.get(gjennomforingId)
            result.shouldNotBeNull()
            result.navn shouldBe "Oppdatert navn"
            result.tiltakstype.id shouldBe TiltakstypeFixtures.AFT.id
            result.stedForGjennomforing shouldBe "Bergen"
            result.beskrivelse shouldBe "Ny beskrivelse"
        }
    }

    test("get returnerer null for ukjent id") {
        database.runAndRollback {
            queries.individuellGjennomforing.get(UUID.randomUUID()) shouldBe null
        }
    }

    test("getBySanityId") {
        database.runAndRollback {
            val sanityId = UUID.randomUUID()
            queries.individuellGjennomforing.upsert(
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

            val result = queries.individuellGjennomforing.getBySanityId(sanityId)
            result.shouldNotBeNull().id shouldBe gjennomforingId

            queries.individuellGjennomforing.getBySanityId(UUID.randomUUID()).shouldBeNull()
        }
    }

    test("getAll uten filter returnerer alle") {
        database.runAndRollback {
            val id1 = UUID.randomUUID()
            val id2 = UUID.randomUUID()

            queries.individuellGjennomforing.upsert(
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
            queries.individuellGjennomforing.upsert(
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

            queries.individuellGjennomforing.getAll() shouldHaveSize 2
        }
    }

    test("getAll filtrerer på tiltakstype") {
        database.runAndRollback {
            queries.individuellGjennomforing.upsert(
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
            queries.individuellGjennomforing.upsert(
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

            val result = queries.individuellGjennomforing.getAll(
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

            queries.individuellGjennomforing.upsert(
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
            queries.individuellGjennomforing.upsert(
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
            queries.individuellGjennomforing.setPublisert(publisertId, true)

            queries.individuellGjennomforing.getAll(publisert = true) shouldHaveSize 1
            queries.individuellGjennomforing.getAll(publisert = false) shouldHaveSize 1
            queries.individuellGjennomforing.getAll() shouldHaveSize 2
        }
    }

    test("getAll filtrerer på navEnhet") {
        database.runAndRollback {
            val medEnhetId = UUID.randomUUID()
            val utenEnhetId = UUID.randomUUID()

            queries.individuellGjennomforing.upsert(
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
            queries.individuellGjennomforing.setNavEnheter(medEnhetId, setOf(NavEnhetFixtures.Gjovik.enhetsnummer))

            queries.individuellGjennomforing.upsert(
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

            val result = queries.individuellGjennomforing.getAll(
                navEnheter = listOf(NavEnhetFixtures.Gjovik.enhetsnummer),
            )
            result shouldHaveSize 1
            result[0].id shouldBe medEnhetId
        }
    }

    test("setPublisert") {
        database.runAndRollback {
            queries.individuellGjennomforing.upsert(
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

            queries.individuellGjennomforing.setPublisert(gjennomforingId, true)
            queries.individuellGjennomforing.get(gjennomforingId)?.publisert shouldBe true

            queries.individuellGjennomforing.setPublisert(gjennomforingId, false)
            queries.individuellGjennomforing.get(gjennomforingId)?.publisert shouldBe false
        }
    }

    test("setAdministratorer legger til og fjerner") {
        database.runAndRollback {
            queries.individuellGjennomforing.upsert(
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

            queries.individuellGjennomforing.setAdministratorer(
                gjennomforingId,
                setOf(NavAnsattFixture.DonaldDuck.navIdent, NavAnsattFixture.MikkeMus.navIdent),
            )
            queries.individuellGjennomforing.get(gjennomforingId)?.administratorer?.shouldHaveSize(2)

            queries.individuellGjennomforing.setAdministratorer(
                gjennomforingId,
                setOf(NavAnsattFixture.DonaldDuck.navIdent),
            )
            val result = queries.individuellGjennomforing.get(gjennomforingId)
            result?.administratorer?.shouldHaveSize(1)
            result?.administratorer?.first()?.navIdent shouldBe NavAnsattFixture.DonaldDuck.navIdent
        }
    }

    test("setNavEnheter legger til og fjerner") {
        database.runAndRollback {
            queries.individuellGjennomforing.upsert(
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

            queries.individuellGjennomforing.setNavEnheter(
                gjennomforingId,
                setOf(NavEnhetFixtures.Innlandet.enhetsnummer, NavEnhetFixtures.Gjovik.enhetsnummer),
            )
            queries.individuellGjennomforing.get(gjennomforingId)?.kontorstruktur.shouldNotBeNull().shouldNotBeEmpty()

            queries.individuellGjennomforing.setNavEnheter(gjennomforingId, emptySet())
            queries.individuellGjennomforing.get(gjennomforingId)?.kontorstruktur.shouldBeEmpty()
        }
    }

    test("setKontaktpersoner legger til og fjerner") {
        database.runAndRollback {
            queries.individuellGjennomforing.upsert(
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

            queries.individuellGjennomforing.setKontaktpersoner(
                gjennomforingId,
                setOf(
                    IndividuellGjennomforingQueries.KontaktpersonDbo(
                        navIdent = NavAnsattFixture.DonaldDuck.navIdent,
                        beskrivelse = "Kontaktperson for test",
                    ),
                ),
            )
            queries.individuellGjennomforing.get(gjennomforingId)?.kontaktpersoner?.shouldHaveSize(1)

            queries.individuellGjennomforing.setKontaktpersoner(gjennomforingId, emptySet())
            queries.individuellGjennomforing.get(gjennomforingId)?.kontaktpersoner.shouldBeEmpty()
        }
    }

    test("delete fjerner gjennomføringen") {
        database.runAndRollback {
            queries.individuellGjennomforing.upsert(
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

            queries.individuellGjennomforing.get(gjennomforingId).shouldNotBeNull()
            queries.individuellGjennomforing.delete(gjennomforingId)
            queries.individuellGjennomforing.get(gjennomforingId).shouldBeNull()
        }
    }
})
