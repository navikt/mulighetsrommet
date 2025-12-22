package no.nav.mulighetsrommet.api.arrangor.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKobling
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.time.LocalDate
import java.util.UUID

class ArrangorQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    context("crud") {
        test("søk og filtrering på arrangører") {
            database.runAndRollback { session ->
                val overordnet = ArrangorDto(
                    id = UUID.randomUUID(),
                    navn = "REMA 1000 AS",
                    organisasjonsnummer = Organisasjonsnummer("982254604"),
                    organisasjonsform = "AS",
                )
                queries.arrangor.upsert(overordnet)

                val underenhet1 = ArrangorDto(
                    id = UUID.randomUUID(),
                    organisasjonsnummer = Organisasjonsnummer("880907522"),
                    organisasjonsform = "BEDR",
                    overordnetEnhet = overordnet.organisasjonsnummer,
                    navn = "REMA 1000 NORGE AS REGION NORDLAND",
                )
                queries.arrangor.upsert(underenhet1)

                val underenhet2 = ArrangorDto(
                    id = UUID.randomUUID(),
                    organisasjonsnummer = Organisasjonsnummer("912704327"),
                    organisasjonsform = "BEDR",
                    overordnetEnhet = overordnet.organisasjonsnummer,
                    navn = "REMA 1000 NORGE AS REGION VESTRE ØSTLAND",
                )
                queries.arrangor.upsert(underenhet2)

                val utenlandsk = ArrangorDto(
                    id = UUID.randomUUID(),
                    organisasjonsnummer = Organisasjonsnummer("100000001"),
                    organisasjonsform = "IKS",
                    navn = "X - Utenlandsk arrangør",
                )
                queries.arrangor.upsert(utenlandsk)
                session.execute(queryOf("update arrangor set er_utenlandsk_virksomhet = true where organisasjonsnummer = '${utenlandsk.organisasjonsnummer.value}'"))

                queries.arrangor.getAll(utenlandsk = true).items shouldContainExactlyInAnyOrder listOf(utenlandsk)
                queries.arrangor.getAll(utenlandsk = false).items shouldContainExactlyInAnyOrder listOf(
                    overordnet,
                    underenhet1,
                    underenhet2,
                )

                queries.arrangor.getAll(sok = "utenlandsk").items shouldContainExactlyInAnyOrder listOf(utenlandsk)
                queries.arrangor.getAll(sok = "østland").items shouldContainExactlyInAnyOrder listOf(underenhet2)

                queries.arrangor.getAll(overordnetEnhetOrgnr = overordnet.organisasjonsnummer).items shouldContainExactlyInAnyOrder listOf(
                    underenhet1,
                    underenhet2,
                )
                queries.arrangor.getAll(overordnetEnhetOrgnr = underenhet1.organisasjonsnummer).items.shouldBeEmpty()
            }
        }

        test("Upsert underenhet etter overenhet") {
            database.runAndRollback {
                val underenhet1 = ArrangorDto(
                    id = UUID.randomUUID(),
                    organisasjonsnummer = Organisasjonsnummer("880907522"),
                    organisasjonsform = "BEDR",
                    overordnetEnhet = Organisasjonsnummer("982254604"),
                    navn = "REMA 1000 NORGE AS REGION NORDLAND",
                )

                val overordnet = ArrangorDto(
                    id = UUID.randomUUID(),
                    navn = "REMA 1000 AS",
                    organisasjonsnummer = Organisasjonsnummer("982254604"),
                    organisasjonsform = "AS",
                )

                queries.arrangor.upsert(overordnet)
                queries.arrangor.upsert(underenhet1)

                queries.arrangor.get(underenhet1.organisasjonsnummer).shouldNotBeNull().should {
                    it.organisasjonsnummer shouldBe underenhet1.organisasjonsnummer
                }
                queries.arrangor.get(overordnet.organisasjonsnummer).shouldNotBeNull().should {
                    it.underenheter.shouldNotBeNull().shouldHaveSize(1).first().should { e ->
                        e.navn shouldBe underenhet1.navn
                        e.organisasjonsnummer shouldBe underenhet1.organisasjonsnummer
                    }
                }
                queries.arrangor.get(listOf(overordnet.organisasjonsnummer)).firstOrNull().shouldNotBeNull().should {
                    it.underenheter.shouldNotBeNull().shouldHaveSize(1).first().should { e ->
                        e.navn shouldBe underenhet1.navn
                        e.organisasjonsnummer shouldBe underenhet1.organisasjonsnummer
                    }
                }
            }
        }

        test("Upsert slettet enhet") {
            database.runAndRollback {
                val slettetDato = LocalDate.of(2024, 1, 1)

                val underenhet1 = ArrangorDto(
                    id = UUID.randomUUID(),
                    organisasjonsnummer = Organisasjonsnummer("880907522"),
                    organisasjonsform = "BEDR",
                    overordnetEnhet = Organisasjonsnummer("982254604"),
                    navn = "REMA 1000 NORGE AS REGION NORDLAND",
                    slettetDato = slettetDato,
                )

                val overordnet = ArrangorDto(
                    id = UUID.randomUUID(),
                    navn = "REMA 1000 AS",
                    organisasjonsnummer = Organisasjonsnummer("982254604"),
                    organisasjonsform = "AS",
                )

                queries.arrangor.upsert(overordnet)
                queries.arrangor.upsert(underenhet1)

                queries.arrangor.get(overordnet.organisasjonsnummer).shouldNotBeNull().should {
                    it.slettetDato shouldBe null
                }
                queries.arrangor.get(underenhet1.organisasjonsnummer).shouldNotBeNull().should {
                    it.slettetDato shouldBe slettetDato
                }
                queries.arrangor.getAll(slettet = true).items shouldContainExactlyInAnyOrder listOf(underenhet1)
                queries.arrangor.getAll(slettet = false).items shouldContainExactlyInAnyOrder listOf(overordnet)
            }
        }

        test("Filter på avtale eller gjennomforing") {
            val hovedenhet = ArrangorFixtures.hovedenhet
            val underenhet = ArrangorFixtures.underenhet1

            val domain = MulighetsrommetTestDomain(
                arrangorer = listOf(hovedenhet, underenhet),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(AvtaleFixtures.oppfolging),
                gjennomforinger = listOf(GjennomforingFixtures.Oppfolging1),
            )

            database.runAndRollback {
                domain.setup(session)

                queries.arrangor.getAll().items shouldContainExactlyInAnyOrder listOf(hovedenhet, underenhet)
                queries.arrangor.getAll(kobling = ArrangorKobling.AVTALE).should {
                    it.items shouldContainExactlyIds listOf(hovedenhet.id)
                }
                queries.arrangor.getAll(kobling = ArrangorKobling.TILTAKSGJENNOMFORING).should {
                    it.items shouldContainExactlyIds listOf(underenhet.id)
                }
            }
        }

        test("getByHovedenhet") {
            val hovedenhet = ArrangorFixtures.hovedenhet
            val underenhet = ArrangorFixtures.underenhet1

            val domain = MulighetsrommetTestDomain(
                arrangorer = listOf(hovedenhet, underenhet),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(AvtaleFixtures.oppfolging),
                gjennomforinger = listOf(GjennomforingFixtures.Oppfolging1),
            )

            database.runAndRollback { session ->
                domain.setup(session)

                queries.arrangor.getHovedenhetById(hovedenhet.id).should {
                    it.underenheter.shouldNotBeNull() shouldContainExactlyIds listOf(underenhet.id)
                }
            }
        }
    }

    context("kontaktperson hos arrangør") {
        test("crud") {
            val arrangorId = ArrangorFixtures.hovedenhet.id

            val kontaktperson1 = ArrangorKontaktperson(
                id = UUID.randomUUID(),
                arrangorId = arrangorId,
                navn = "Fredrik",
                telefon = "322232323",
                epost = "fredrik@gmail.com",
                beskrivelse = null,
                ansvarligFor = listOf(),
            )

            val kontaktperson2 = ArrangorKontaktperson(
                id = UUID.randomUUID(),
                arrangorId = arrangorId,
                navn = "Trond",
                telefon = "232232323",
                epost = "trond@gmail.com",
                beskrivelse = "Adm. dir.",
                ansvarligFor = listOf(),
            )

            val domain = MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet),
                arrangorKontaktpersoner = listOf(kontaktperson1, kontaktperson2),
            )

            database.runAndRollback {
                domain.setup(session)

                queries.arrangor.getKontaktpersoner(arrangorId) shouldContainExactlyInAnyOrder listOf(
                    kontaktperson1,
                    kontaktperson2,
                )
            }
        }
    }
})

private infix fun Collection<ArrangorDto>.shouldContainExactlyIds(listOf: Collection<UUID>) {
    map { it.id }.shouldContainExactlyInAnyOrder(listOf)
}
