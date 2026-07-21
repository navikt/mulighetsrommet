package no.nav.mulighetsrommet.api.persistence.tiltakdokument

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.domain.tiltakdokument.TiltakDokument
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.persistence.SqlAdminDatabaseTestListener
import java.util.UUID

class TiltakDokumentQueriesTest : FunSpec({
    val database = extension(SqlAdminDatabaseTestListener())

    val gjennomforingId = UUID.randomUUID()

    fun minimalDokument(
        id: UUID = UUID.randomUUID(),
        tiltakstypeId: UUID = TiltakstypeFixtures.Oppfolging.id,
        navn: String = "Test",
    ) = TiltakDokument(
        id = id,
        navn = navn,
        tiltakstypeId = tiltakstypeId,
        stedForGjennomforing = null,
        arrangorId = null,
        faneinnhold = null,
        beskrivelse = null,
        tiltaksnummer = null,
        sanityId = null,
        publisert = false,
        administratorer = emptyList(),
        navEnheter = emptyList(),
        kontaktpersoner = emptyList(),
        arrangorKontaktpersoner = emptyList(),
    )

    test("upsert og get") {
        database.runAndRollback {
            repository.navEnhet.save(NavEnhetFixtures.Innlandet)
            repository.navEnhet.save(NavEnhetFixtures.Gjovik)
            repository.navAnsatt.save(NavAnsattFixture.DonaldDuck)
            repository.arrangor.save(ArrangorFixtures.hovedenhet)
            repository.arrangor.save(ArrangorFixtures.underenhet1)
            repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)

            repository.tiltakDokument.save(
                TiltakDokument(
                    id = gjennomforingId,
                    navn = "Test gjennomføring",
                    tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                    stedForGjennomforing = "Oslo",
                    arrangorId = ArrangorFixtures.underenhet1.id,
                    faneinnhold = null,
                    beskrivelse = "En beskrivelse",
                    tiltaksnummer = "2024/1",
                    sanityId = null,
                    publisert = false,
                    administratorer = emptyList(),
                    navEnheter = emptyList(),
                    kontaktpersoner = emptyList(),
                    arrangorKontaktpersoner = emptyList(),
                ),
            )

            val result = queries.tiltakDokument.getTiltakDokumentDto(gjennomforingId)
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
            repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)
            repository.tiltakstype.save(TiltakstypeFixtures.AFT)

            repository.tiltakDokument.save(minimalDokument(id = gjennomforingId, navn = "Originalt navn"))

            repository.tiltakDokument.save(
                minimalDokument(id = gjennomforingId, tiltakstypeId = TiltakstypeFixtures.AFT.id, navn = "Oppdatert navn").copy(
                    stedForGjennomforing = "Bergen",
                    beskrivelse = "Ny beskrivelse",
                ),
            )

            val result = queries.tiltakDokument.getTiltakDokumentDto(gjennomforingId)
            result.shouldNotBeNull()
            result.navn shouldBe "Oppdatert navn"
            result.tiltakstype.id shouldBe TiltakstypeFixtures.AFT.id
            result.stedForGjennomforing shouldBe "Bergen"
            result.beskrivelse shouldBe "Ny beskrivelse"
        }
    }

    test("get returnerer null for ukjent id") {
        database.runAndRollback {
            queries.tiltakDokument.getTiltakDokumentDto(UUID.randomUUID()) shouldBe null
        }
    }

    test("getAll uten filter returnerer alle") {
        database.runAndRollback {
            repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)
            repository.tiltakstype.save(TiltakstypeFixtures.AFT)

            repository.tiltakDokument.save(minimalDokument(navn = "Gjennomføring 1"))
            repository.tiltakDokument.save(minimalDokument(tiltakstypeId = TiltakstypeFixtures.AFT.id, navn = "Gjennomføring 2"))

            queries.tiltakDokument.getAllKompaktDto() shouldHaveSize 2
        }
    }

    test("getAll filtrerer på tiltakstype") {
        database.runAndRollback {
            repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)
            repository.tiltakstype.save(TiltakstypeFixtures.AFT)

            repository.tiltakDokument.save(minimalDokument(navn = "Oppfølging"))
            repository.tiltakDokument.save(minimalDokument(tiltakstypeId = TiltakstypeFixtures.AFT.id, navn = "AFT"))

            val result = queries.tiltakDokument.getAllKompaktDto(
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging.tiltakskode),
            )
            result shouldHaveSize 1
            result[0].tiltakstype.id shouldBe TiltakstypeFixtures.Oppfolging.id
        }
    }

    test("getAll filtrerer på publisert") {
        database.runAndRollback {
            repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)

            val upublisertId = UUID.randomUUID()
            val publisertId = UUID.randomUUID()

            repository.tiltakDokument.save(minimalDokument(id = upublisertId, navn = "Upublisert"))
            repository.tiltakDokument.save(minimalDokument(id = publisertId, navn = "Publisert"))
            queries.tiltakDokument.setPublisert(publisertId, true)

            queries.tiltakDokument.getAllKompaktDto(publisert = true) shouldHaveSize 1
            queries.tiltakDokument.getAllKompaktDto(publisert = false) shouldHaveSize 1
            queries.tiltakDokument.getAllKompaktDto() shouldHaveSize 2
        }
    }

    test("getAll filtrerer på navEnhet") {
        database.runAndRollback {
            repository.navEnhet.save(NavEnhetFixtures.Innlandet)
            repository.navEnhet.save(NavEnhetFixtures.Gjovik)
            repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)

            val medEnhetId = UUID.randomUUID()
            val utenEnhetId = UUID.randomUUID()

            repository.tiltakDokument.save(
                minimalDokument(id = medEnhetId, navn = "Med enhet").copy(
                    navEnheter = listOf(NavEnhetFixtures.Gjovik.enhetsnummer),
                ),
            )
            repository.tiltakDokument.save(minimalDokument(id = utenEnhetId, navn = "Uten enhet"))

            val result = queries.tiltakDokument.getAllKompaktDto(
                navEnheter = listOf(NavEnhetFixtures.Gjovik.enhetsnummer),
            )
            result shouldHaveSize 1
            result[0].id shouldBe medEnhetId
        }
    }

    test("setPublisert") {
        database.runAndRollback {
            repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)
            repository.tiltakDokument.save(minimalDokument(id = gjennomforingId))

            queries.tiltakDokument.setPublisert(gjennomforingId, true)
            repository.tiltakDokument.get(gjennomforingId)?.publisert shouldBe true

            queries.tiltakDokument.setPublisert(gjennomforingId, false)
            repository.tiltakDokument.get(gjennomforingId)?.publisert shouldBe false
        }
    }

    test("setAdministratorer legger til og fjerner") {
        database.runAndRollback {
            repository.navEnhet.save(NavEnhetFixtures.Innlandet)
            repository.navAnsatt.save(NavAnsattFixture.DonaldDuck)
            repository.navAnsatt.save(NavAnsattFixture.MikkeMus)
            repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)
            repository.tiltakDokument.save(minimalDokument(id = gjennomforingId))

            repository.tiltakDokument.save(
                repository.tiltakDokument.get(gjennomforingId)!!.copy(
                    administratorer = listOf(NavAnsattFixture.DonaldDuck.navIdent, NavAnsattFixture.MikkeMus.navIdent),
                ),
            )
            repository.tiltakDokument.get(gjennomforingId)?.administratorer?.shouldHaveSize(2)

            repository.tiltakDokument.save(
                repository.tiltakDokument.get(gjennomforingId)!!.copy(
                    administratorer = listOf(NavAnsattFixture.DonaldDuck.navIdent),
                ),
            )
            val result = repository.tiltakDokument.get(gjennomforingId)
            result?.administratorer?.shouldHaveSize(1)
            result?.administratorer?.first() shouldBe NavAnsattFixture.DonaldDuck.navIdent
        }
    }

    test("setNavEnheter legger til og fjerner") {
        database.runAndRollback {
            repository.navEnhet.save(NavEnhetFixtures.Innlandet)
            repository.navEnhet.save(NavEnhetFixtures.Gjovik)
            repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)
            repository.tiltakDokument.save(minimalDokument(id = gjennomforingId))

            repository.tiltakDokument.save(
                repository.tiltakDokument.get(gjennomforingId)!!.copy(
                    navEnheter = listOf(NavEnhetFixtures.Innlandet.enhetsnummer, NavEnhetFixtures.Gjovik.enhetsnummer),
                ),
            )
            queries.tiltakDokument.getTiltakDokumentDto(gjennomforingId)?.kontorstruktur.shouldNotBeNull().shouldNotBeEmpty()

            repository.tiltakDokument.save(
                repository.tiltakDokument.get(gjennomforingId)!!.copy(navEnheter = emptyList()),
            )
            queries.tiltakDokument.getTiltakDokumentDto(gjennomforingId)?.kontorstruktur.shouldBeEmpty()
        }
    }

    test("setKontaktpersoner legger til og fjerner") {
        database.runAndRollback {
            repository.navEnhet.save(NavEnhetFixtures.Innlandet)
            repository.navAnsatt.save(NavAnsattFixture.DonaldDuck)
            repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)
            repository.tiltakDokument.save(minimalDokument(id = gjennomforingId))

            repository.tiltakDokument.save(
                repository.tiltakDokument.get(gjennomforingId)!!.copy(
                    kontaktpersoner = listOf(
                        TiltakDokument.Kontaktperson(
                            navIdent = NavAnsattFixture.DonaldDuck.navIdent,
                            beskrivelse = "Kontaktperson for test",
                        ),
                    ),
                ),
            )
            repository.tiltakDokument.get(gjennomforingId)?.kontaktpersoner?.shouldHaveSize(1)

            repository.tiltakDokument.save(
                repository.tiltakDokument.get(gjennomforingId)!!.copy(kontaktpersoner = emptyList()),
            )
            repository.tiltakDokument.get(gjennomforingId)?.kontaktpersoner.shouldBeEmpty()
        }
    }

    test("delete fjerner gjennomføringen") {
        database.runAndRollback {
            repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)
            repository.tiltakDokument.save(minimalDokument(id = gjennomforingId, navn = "Skal slettes"))

            repository.tiltakDokument.get(gjennomforingId).shouldNotBeNull()
            repository.tiltakDokument.delete(gjennomforingId)
            repository.tiltakDokument.get(gjennomforingId).shouldBeNull()
        }
    }
})
