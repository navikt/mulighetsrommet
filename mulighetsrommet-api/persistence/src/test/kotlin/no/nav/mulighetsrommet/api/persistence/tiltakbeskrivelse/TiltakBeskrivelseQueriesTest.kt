package no.nav.mulighetsrommet.api.persistence.tiltakbeskrivelse

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.domain.testing.fixture.ArrangorFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavEnhetFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.domain.tiltakbeskrivelse.TiltakBeskrivelse
import no.nav.mulighetsrommet.api.persistence.SqlAdminDatabaseTestListener
import java.util.UUID

class TiltakBeskrivelseQueriesTest : FunSpec({
    val database = extension(SqlAdminDatabaseTestListener())

    val gjennomforingId = UUID.randomUUID()

    fun minimalDokument(
        id: UUID = UUID.randomUUID(),
        tiltakstypeId: UUID = TiltakstypeFixtures.Oppfolging.id,
        navn: String = "Test",
    ) = TiltakBeskrivelse(
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

            repository.tiltakBeskrivelse.save(
                TiltakBeskrivelse(
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

            val result = queries.tiltakBeskrivelse.getTiltakBeskrivelseDto(gjennomforingId)
            result.shouldNotBeNull()
            result.id shouldBe gjennomforingId
            result.navn shouldBe "Test gjennomføring"
            result.stedForGjennomforing shouldBe "Oslo"
            result.veilederinfo.beskrivelse shouldBe "En beskrivelse"
            result.tiltaksnummer shouldBe "2024/1"
            result.tiltakstype.id shouldBe TiltakstypeFixtures.Oppfolging.id
            result.arrangor.shouldNotBeNull().id shouldBe ArrangorFixtures.underenhet1.id
            result.veilederinfo.publisert shouldBe false
        }
    }

    test("upsert oppdaterer eksisterende rad") {
        database.runAndRollback {
            repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)
            repository.tiltakstype.save(TiltakstypeFixtures.AFT)

            repository.tiltakBeskrivelse.save(minimalDokument(id = gjennomforingId, navn = "Originalt navn"))

            repository.tiltakBeskrivelse.save(
                minimalDokument(
                    id = gjennomforingId,
                    tiltakstypeId = TiltakstypeFixtures.AFT.id,
                    navn = "Oppdatert navn",
                ).copy(
                    stedForGjennomforing = "Bergen",
                    beskrivelse = "Ny beskrivelse",
                ),
            )

            val result = queries.tiltakBeskrivelse.getTiltakBeskrivelseDto(gjennomforingId)
            result.shouldNotBeNull()
            result.navn shouldBe "Oppdatert navn"
            result.tiltakstype.id shouldBe TiltakstypeFixtures.AFT.id
            result.stedForGjennomforing shouldBe "Bergen"
            result.veilederinfo.beskrivelse shouldBe "Ny beskrivelse"
        }
    }

    test("get returnerer null for ukjent id") {
        database.runAndRollback {
            queries.tiltakBeskrivelse.getTiltakBeskrivelseDto(UUID.randomUUID()) shouldBe null
        }
    }

    test("getAll uten filter returnerer alle") {
        database.runAndRollback {
            repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)
            repository.tiltakstype.save(TiltakstypeFixtures.AFT)

            repository.tiltakBeskrivelse.save(minimalDokument(navn = "Gjennomføring 1"))
            repository.tiltakBeskrivelse.save(
                minimalDokument(
                    tiltakstypeId = TiltakstypeFixtures.AFT.id,
                    navn = "Gjennomføring 2",
                ),
            )

            queries.tiltakBeskrivelse.getAllKompaktDto().items shouldHaveSize 2
        }
    }

    test("getAll filtrerer på tiltakstype") {
        database.runAndRollback {
            repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)
            repository.tiltakstype.save(TiltakstypeFixtures.AFT)

            repository.tiltakBeskrivelse.save(minimalDokument(navn = "Oppfølging"))
            repository.tiltakBeskrivelse.save(minimalDokument(tiltakstypeId = TiltakstypeFixtures.AFT.id, navn = "AFT"))

            val result = queries.tiltakBeskrivelse.getAllKompaktDto(
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging.tiltakskode),
            )
            result.items shouldHaveSize 1
            result.items[0].tiltakstype.id shouldBe TiltakstypeFixtures.Oppfolging.id
        }
    }

    test("getAll filtrerer på publisert") {
        database.runAndRollback {
            repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)

            val upublisertId = UUID.randomUUID()
            val publisertId = UUID.randomUUID()

            repository.tiltakBeskrivelse.save(minimalDokument(id = upublisertId, navn = "Upublisert"))
            repository.tiltakBeskrivelse.save(minimalDokument(id = publisertId, navn = "Publisert"))
            queries.tiltakBeskrivelse.setPublisert(publisertId, true)

            queries.tiltakBeskrivelse.getAllKompaktDto(publisert = true).items shouldHaveSize 1
            queries.tiltakBeskrivelse.getAllKompaktDto(publisert = false).items shouldHaveSize 1
            queries.tiltakBeskrivelse.getAllKompaktDto().items shouldHaveSize 2
        }
    }

    test("getAll filtrerer på navEnhet") {
        database.runAndRollback {
            repository.navEnhet.save(NavEnhetFixtures.Innlandet)
            repository.navEnhet.save(NavEnhetFixtures.Gjovik)
            repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)

            val medEnhetId = UUID.randomUUID()
            val utenEnhetId = UUID.randomUUID()

            repository.tiltakBeskrivelse.save(
                minimalDokument(id = medEnhetId, navn = "Med enhet").copy(
                    navEnheter = listOf(NavEnhetFixtures.Gjovik.enhetsnummer),
                ),
            )
            repository.tiltakBeskrivelse.save(minimalDokument(id = utenEnhetId, navn = "Uten enhet"))

            val result = queries.tiltakBeskrivelse.getAllKompaktDto(
                navEnheter = listOf(NavEnhetFixtures.Gjovik.enhetsnummer),
            )
            result.items shouldHaveSize 1
            result.items[0].id shouldBe medEnhetId
        }
    }

    test("getAll filtrerer på navEnhet inkluderer dokument med kun fylke når enhet i fylket filtreres") {
        database.runAndRollback {
            repository.navEnhet.save(NavEnhetFixtures.Innlandet) // FYLKE 0400
            repository.navEnhet.save(NavEnhetFixtures.Gjovik) // LOKAL 0502, overordnet 0400
            repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)

            val medFylkeId = UUID.randomUUID()
            val medEnhetId = UUID.randomUUID()
            val utenEnhetId = UUID.randomUUID()

            repository.tiltakBeskrivelse.save(
                minimalDokument(id = medFylkeId, navn = "Kun fylke").copy(
                    navEnheter = listOf(NavEnhetFixtures.Innlandet.enhetsnummer),
                ),
            )
            repository.tiltakBeskrivelse.save(
                minimalDokument(id = medEnhetId, navn = "Med enhet").copy(
                    navEnheter = listOf(NavEnhetFixtures.Gjovik.enhetsnummer),
                ),
            )
            repository.tiltakBeskrivelse.save(minimalDokument(id = utenEnhetId, navn = "Uten enhet"))

            val result = queries.tiltakBeskrivelse.getAllKompaktDto(
                navEnheter = listOf(NavEnhetFixtures.Gjovik.enhetsnummer),
            )
            result.items shouldHaveSize 2
            result.items.map { it.id } shouldContainExactlyInAnyOrder listOf(medFylkeId, medEnhetId)
        }
    }

    test("getAll filtrerer på navEnhet ekskluderer dokument med annen fylke") {
        database.runAndRollback {
            repository.navEnhet.save(NavEnhetFixtures.Innlandet) // FYLKE 0400
            repository.navEnhet.save(NavEnhetFixtures.Oslo) // FYLKE 0300
            repository.navEnhet.save(NavEnhetFixtures.Gjovik) // LOKAL 0502, overordnet 0400
            repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)

            val medAnnenFylkeId = UUID.randomUUID()

            repository.tiltakBeskrivelse.save(
                minimalDokument(id = medAnnenFylkeId, navn = "Annen fylke").copy(
                    navEnheter = listOf(NavEnhetFixtures.Oslo.enhetsnummer),
                ),
            )

            val result = queries.tiltakBeskrivelse.getAllKompaktDto(
                navEnheter = listOf(NavEnhetFixtures.Gjovik.enhetsnummer),
            )
            result.items.shouldBeEmpty()
        }
    }

    test("setPublisert") {
        database.runAndRollback {
            repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)
            repository.tiltakBeskrivelse.save(minimalDokument(id = gjennomforingId))

            queries.tiltakBeskrivelse.setPublisert(gjennomforingId, true)
            repository.tiltakBeskrivelse.get(gjennomforingId)?.publisert shouldBe true

            queries.tiltakBeskrivelse.setPublisert(gjennomforingId, false)
            repository.tiltakBeskrivelse.get(gjennomforingId)?.publisert shouldBe false
        }
    }

    test("setAdministratorer legger til og fjerner") {
        database.runAndRollback {
            repository.navEnhet.save(NavEnhetFixtures.Innlandet)
            repository.navAnsatt.save(NavAnsattFixture.DonaldDuck)
            repository.navAnsatt.save(NavAnsattFixture.MikkeMus)
            repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)
            repository.tiltakBeskrivelse.save(minimalDokument(id = gjennomforingId))

            repository.tiltakBeskrivelse.save(
                repository.tiltakBeskrivelse.get(gjennomforingId)!!.copy(
                    administratorer = listOf(NavAnsattFixture.DonaldDuck.navIdent, NavAnsattFixture.MikkeMus.navIdent),
                ),
            )
            repository.tiltakBeskrivelse.get(gjennomforingId)?.administratorer?.shouldHaveSize(2)

            repository.tiltakBeskrivelse.save(
                repository.tiltakBeskrivelse.get(gjennomforingId)!!.copy(
                    administratorer = listOf(NavAnsattFixture.DonaldDuck.navIdent),
                ),
            )
            val result = repository.tiltakBeskrivelse.get(gjennomforingId)
            result?.administratorer?.shouldHaveSize(1)
            result?.administratorer?.first() shouldBe NavAnsattFixture.DonaldDuck.navIdent
        }
    }

    test("setNavEnheter legger til og fjerner") {
        database.runAndRollback {
            repository.navEnhet.save(NavEnhetFixtures.Innlandet)
            repository.navEnhet.save(NavEnhetFixtures.Gjovik)
            repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)
            repository.tiltakBeskrivelse.save(minimalDokument(id = gjennomforingId))

            repository.tiltakBeskrivelse.save(
                repository.tiltakBeskrivelse.get(gjennomforingId)!!.copy(
                    navEnheter = listOf(NavEnhetFixtures.Innlandet.enhetsnummer, NavEnhetFixtures.Gjovik.enhetsnummer),
                ),
            )
            queries.tiltakBeskrivelse.getTiltakBeskrivelseDto(gjennomforingId)
                ?.veilederinfo
                ?.kontorstruktur
                .shouldNotBeNull()
                .shouldNotBeEmpty()

            repository.tiltakBeskrivelse.save(
                repository.tiltakBeskrivelse.get(gjennomforingId)!!.copy(navEnheter = emptyList()),
            )
            queries.tiltakBeskrivelse.getTiltakBeskrivelseDto(gjennomforingId)?.veilederinfo?.kontorstruktur.shouldBeEmpty()
        }
    }

    test("setKontaktpersoner legger til og fjerner") {
        database.runAndRollback {
            repository.navEnhet.save(NavEnhetFixtures.Innlandet)
            repository.navAnsatt.save(NavAnsattFixture.DonaldDuck)
            repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)
            repository.tiltakBeskrivelse.save(minimalDokument(id = gjennomforingId))

            repository.tiltakBeskrivelse.save(
                repository.tiltakBeskrivelse.get(gjennomforingId)!!.copy(
                    kontaktpersoner = listOf(
                        TiltakBeskrivelse.Kontaktperson(
                            navIdent = NavAnsattFixture.DonaldDuck.navIdent,
                            beskrivelse = "Kontaktperson for test",
                        ),
                    ),
                ),
            )
            repository.tiltakBeskrivelse.get(gjennomforingId)?.kontaktpersoner?.shouldHaveSize(1)

            repository.tiltakBeskrivelse.save(
                repository.tiltakBeskrivelse.get(gjennomforingId)!!.copy(kontaktpersoner = emptyList()),
            )
            repository.tiltakBeskrivelse.get(gjennomforingId)?.kontaktpersoner.shouldBeEmpty()
        }
    }

    test("delete fjerner gjennomføringen") {
        database.runAndRollback {
            repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)
            repository.tiltakBeskrivelse.save(minimalDokument(id = gjennomforingId, navn = "Skal slettes"))

            repository.tiltakBeskrivelse.get(gjennomforingId).shouldNotBeNull()
            repository.tiltakBeskrivelse.delete(gjennomforingId)
            repository.tiltakBeskrivelse.get(gjennomforingId).shouldBeNull()
        }
    }

    context("upsertFromArena") {
        test("oppretter ny rad med kun arena-felter som standard") {
            database.runAndRollback {
                repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)

                val id = UUID.randomUUID()
                repository.tiltakBeskrivelse.upsertFromArena(
                    minimalDokument(id = id, navn = "Arena-tiltak").copy(
                        sanityId = UUID.randomUUID(),
                        tiltaksnummer = "2024/42",
                    ),
                )

                val result = repository.tiltakBeskrivelse.get(id)
                result.shouldNotBeNull()
                result.navn shouldBe "Arena-tiltak"
                result.tiltaksnummer shouldBe "2024/42"
                result.beskrivelse shouldBe null
                result.faneinnhold shouldBe null
                result.publisert shouldBe false
            }
        }

        test("oppdaterer arena-felter uten å overskrive redaktør-felter") {
            database.runAndRollback {
                repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)

                val id = UUID.randomUUID()
                val sanityId = UUID.randomUUID()

                repository.tiltakBeskrivelse.save(
                    minimalDokument(id = id, navn = "Originalt navn").copy(
                        sanityId = sanityId,
                        tiltaksnummer = "2024/1",
                        beskrivelse = "Redaktørtekst som skal bevares",
                    ),
                )

                repository.tiltakBeskrivelse.upsertFromArena(
                    minimalDokument(id = id, navn = "Oppdatert av arena").copy(
                        sanityId = sanityId,
                        tiltaksnummer = "2024/1-oppdatert",
                    ),
                )

                val result = repository.tiltakBeskrivelse.get(id)
                result.shouldNotBeNull()
                result.id shouldBe id
                result.navn shouldBe "Oppdatert av arena"
                result.tiltaksnummer shouldBe "2024/1-oppdatert"
                result.beskrivelse shouldBe "Redaktørtekst som skal bevares"
            }
        }

        test("bruker eksisterende rad ved sanity_id-konflikt og bevarer redaktør-felter") {
            database.runAndRollback {
                repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)
                repository.tiltakstype.save(TiltakstypeFixtures.AFT)

                val migrasjonsId = UUID.randomUUID()
                val arenaId = UUID.randomUUID()
                val sanityId = UUID.randomUUID()

                // Rad opprettet av MigrerSanityTiltaksgjennomforinger med tilfeldig id
                repository.tiltakBeskrivelse.save(
                    minimalDokument(id = migrasjonsId, navn = "Migrasjonsnavn").copy(
                        sanityId = sanityId,
                        tiltaksnummer = "2024/1",
                        beskrivelse = "Redaktørtekst som skal bevares",
                    ),
                )

                // Arena-adapter sender oppdatering med sin entity-id – konflikt på sanity_id
                repository.tiltakBeskrivelse.upsertFromArena(
                    minimalDokument(id = arenaId, navn = "Arena-navn").copy(
                        sanityId = sanityId,
                        tiltaksnummer = "2024/1-oppdatert",
                        tiltakstypeId = TiltakstypeFixtures.AFT.id,
                    ),
                )

                // Ingen ny rad med arena-id
                repository.tiltakBeskrivelse.get(arenaId).shouldBeNull()

                // Eksisterende rad oppdatert med arena-felter, id beholdt
                val result = repository.tiltakBeskrivelse.get(migrasjonsId)
                result.shouldNotBeNull()
                result.id shouldBe migrasjonsId
                result.navn shouldBe "Arena-navn"
                result.tiltaksnummer shouldBe "2024/1-oppdatert"
                result.tiltakstypeId shouldBe TiltakstypeFixtures.AFT.id
                result.beskrivelse shouldBe "Redaktørtekst som skal bevares"
            }
        }
    }
})
