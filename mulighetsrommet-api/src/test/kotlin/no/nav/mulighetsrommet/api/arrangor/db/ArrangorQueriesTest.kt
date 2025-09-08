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
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKobling
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.time.LocalDate
import java.util.*

class ArrangorQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    context("crud") {
        test("søk og filtrering på arrangører") {
            database.runAndRollback { session ->
                val queries = ArrangorQueries(session)

                val overordnet = ArrangorDto(
                    id = UUID.randomUUID(),
                    navn = "REMA 1000 AS",
                    organisasjonsnummer = Organisasjonsnummer("982254604"),
                    organisasjonsform = "AS",
                )
                queries.upsert(overordnet)

                val underenhet1 = ArrangorDto(
                    id = UUID.randomUUID(),
                    organisasjonsnummer = Organisasjonsnummer("880907522"),
                    organisasjonsform = "BEDR",
                    overordnetEnhet = overordnet.organisasjonsnummer,
                    navn = "REMA 1000 NORGE AS REGION NORDLAND",
                )
                queries.upsert(underenhet1)

                val underenhet2 = ArrangorDto(
                    id = UUID.randomUUID(),
                    organisasjonsnummer = Organisasjonsnummer("912704327"),
                    organisasjonsform = "BEDR",
                    overordnetEnhet = overordnet.organisasjonsnummer,
                    navn = "REMA 1000 NORGE AS REGION VESTRE ØSTLAND",
                )
                queries.upsert(underenhet2)

                val utenlandsk = ArrangorDto(
                    id = UUID.randomUUID(),
                    organisasjonsnummer = Organisasjonsnummer("100000001"),
                    organisasjonsform = "IKS",
                    navn = "X - Utenlandsk arrangør",
                )
                queries.upsert(utenlandsk)
                session.execute(queryOf("update arrangor set er_utenlandsk_virksomhet = true where organisasjonsnummer = '${utenlandsk.organisasjonsnummer.value}'"))

                queries.getAll(utenlandsk = true).items shouldContainExactlyInAnyOrder listOf(utenlandsk)
                queries.getAll(utenlandsk = false).items shouldContainExactlyInAnyOrder listOf(
                    overordnet,
                    underenhet1,
                    underenhet2,
                )

                queries.getAll(sok = "utenlandsk").items shouldContainExactlyInAnyOrder listOf(utenlandsk)
                queries.getAll(sok = "østland").items shouldContainExactlyInAnyOrder listOf(underenhet2)

                queries.getAll(overordnetEnhetOrgnr = overordnet.organisasjonsnummer).items shouldContainExactlyInAnyOrder listOf(
                    underenhet1,
                    underenhet2,
                )
                queries.getAll(overordnetEnhetOrgnr = underenhet1.organisasjonsnummer).items.shouldBeEmpty()
            }
        }

        test("Upsert underenhet etter overenhet") {
            database.runAndRollback { session ->
                val queries = ArrangorQueries(session)

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

                queries.upsert(overordnet)
                queries.upsert(underenhet1)

                queries.get(underenhet1.organisasjonsnummer).shouldNotBeNull().should {
                    it.organisasjonsnummer shouldBe underenhet1.organisasjonsnummer
                }
                queries.get(overordnet.organisasjonsnummer).shouldNotBeNull().should {
                    it.underenheter.shouldNotBeNull().shouldHaveSize(1).first().should { e ->
                        e.navn shouldBe underenhet1.navn
                        e.organisasjonsnummer shouldBe underenhet1.organisasjonsnummer
                    }
                }
            }
        }

        test("Upsert slettet enhet") {
            database.runAndRollback { session ->
                val queries = ArrangorQueries(session)

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

                queries.upsert(overordnet)
                queries.upsert(underenhet1)

                queries.get(overordnet.organisasjonsnummer).shouldNotBeNull().should {
                    it.slettetDato shouldBe null
                }
                queries.get(underenhet1.organisasjonsnummer).shouldNotBeNull().should {
                    it.slettetDato shouldBe slettetDato
                }
                queries.getAll(slettet = true).items shouldContainExactlyInAnyOrder listOf(underenhet1)
                queries.getAll(slettet = false).items shouldContainExactlyInAnyOrder listOf(overordnet)
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

            database.runAndRollback { session ->
                domain.setup(session)

                val queries = ArrangorQueries(session)

                queries.getAll().items shouldContainExactlyInAnyOrder listOf(hovedenhet, underenhet)
                queries.getAll(kobling = ArrangorKobling.AVTALE).should {
                    it.items shouldContainExactlyIds listOf(hovedenhet.id)
                }
                queries.getAll(kobling = ArrangorKobling.TILTAKSGJENNOMFORING).should {
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

                val queries = ArrangorQueries(session)

                queries.getHovedenhetById(hovedenhet.id).should {
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
            )

            val kontaktperson2 = ArrangorKontaktperson(
                id = UUID.randomUUID(),
                arrangorId = arrangorId,
                navn = "Trond",
                telefon = "232232323",
                epost = "trond@gmail.com",
                beskrivelse = "Adm. dir.",
            )

            val domain = MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet),
                arrangorKontaktpersoner = listOf(kontaktperson1, kontaktperson2),
                avtaler = listOf(),
            )

            database.runAndRollback { session ->
                domain.setup(session)

                val queries = ArrangorQueries(session)

                queries.getKontaktpersoner(arrangorId) shouldContainExactlyInAnyOrder listOf(
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
