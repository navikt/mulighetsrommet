package no.nav.mulighetsrommet.api.persistence.arrangor

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.admin.arrangor.ArrangorDto
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.domain.arrangor.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.persistence.SqlAdminDatabaseTestListener
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.time.LocalDate
import java.util.UUID

class ArrangorQueriesTest : FunSpec({
    val database = extension(SqlAdminDatabaseTestListener())

    context("crud") {
        test("søk og filtrering på arrangører") {
            database.runAndRollback {
                val overordnet = Arrangor(
                    id = UUID.randomUUID(),
                    navn = "REMA 1000 AS",
                    organisasjonsnummer = Organisasjonsnummer("982254604"),
                    organisasjonsform = "AS",
                    erUtenlandsk = false,
                )
                repository.arrangor.save(overordnet)

                val underenhet1 = Arrangor(
                    id = UUID.randomUUID(),
                    organisasjonsnummer = Organisasjonsnummer("880907522"),
                    organisasjonsform = "BEDR",
                    overordnetEnhet = overordnet.organisasjonsnummer,
                    navn = "REMA 1000 NORGE AS REGION NORDLAND",
                    erUtenlandsk = false,
                )
                repository.arrangor.save(underenhet1)

                val underenhet2 = Arrangor(
                    id = UUID.randomUUID(),
                    organisasjonsnummer = Organisasjonsnummer("912704327"),
                    organisasjonsform = "BEDR",
                    overordnetEnhet = overordnet.organisasjonsnummer,
                    navn = "REMA 1000 NORGE AS REGION VESTRE ØSTLAND",
                    erUtenlandsk = false,
                )
                repository.arrangor.save(underenhet2)

                val utenlandsk = Arrangor(
                    id = UUID.randomUUID(),
                    organisasjonsnummer = Organisasjonsnummer("100000001"),
                    organisasjonsform = "IKS",
                    navn = "X - Utenlandsk arrangør",
                    erUtenlandsk = true,
                )
                repository.arrangor.save(utenlandsk)

                queries.arrangor.getAll(utenlandsk = true).items shouldContainExactlyInAnyOrder listOf(
                    utenlandsk.toArrangorDto(),
                )
                queries.arrangor.getAll(utenlandsk = false).items shouldContainExactlyInAnyOrder listOf(
                    overordnet.toArrangorDto(),
                    underenhet1.toArrangorDto(),
                    underenhet2.toArrangorDto(),
                )

                queries.arrangor.getAll(sok = "utenlandsk").items shouldContainExactlyInAnyOrder listOf(utenlandsk.toArrangorDto())
                queries.arrangor.getAll(sok = "østland").items shouldContainExactlyInAnyOrder listOf(underenhet2.toArrangorDto())

                queries.arrangor.getAll(overordnetEnhetOrgnr = overordnet.organisasjonsnummer).items shouldContainExactlyInAnyOrder listOf(
                    underenhet1.toArrangorDto(),
                    underenhet2.toArrangorDto(),
                )
                queries.arrangor.getAll(overordnetEnhetOrgnr = underenhet1.organisasjonsnummer).items.shouldBeEmpty()
            }
        }

        test("Upsert underenhet etter overenhet") {
            database.runAndRollback {
                val underenhet1 = Arrangor(
                    id = UUID.randomUUID(),
                    organisasjonsnummer = Organisasjonsnummer("880907522"),
                    organisasjonsform = "BEDR",
                    overordnetEnhet = Organisasjonsnummer("982254604"),
                    erUtenlandsk = false,
                    navn = "REMA 1000 NORGE AS REGION NORDLAND",
                )

                val overordnet = Arrangor(
                    id = UUID.randomUUID(),
                    navn = "REMA 1000 AS",
                    organisasjonsnummer = Organisasjonsnummer("982254604"),
                    organisasjonsform = "AS",
                    erUtenlandsk = false,
                )

                repository.arrangor.save(overordnet)
                repository.arrangor.save(underenhet1)

                queries.arrangor.get(underenhet1.organisasjonsnummer).shouldNotBeNull().should {
                    it.organisasjonsnummer shouldBe underenhet1.organisasjonsnummer
                }
                queries.arrangor.get(overordnet.organisasjonsnummer).shouldNotBeNull().should {
                    it.underenheter.shouldNotBeNull().shouldHaveSize(1).first().should { e ->
                        e.navn shouldBe underenhet1.navn
                        e.organisasjonsnummer shouldBe underenhet1.organisasjonsnummer
                    }
                }
            }
        }

        test("getByHovedenhet") {
            val hovedenhet = Arrangor(
                id = UUID.randomUUID(),
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                organisasjonsform = "AS",
                navn = "Hovedenhet AS",
                erUtenlandsk = false,
            )

            val underenhet = Arrangor(
                id = UUID.randomUUID(),
                organisasjonsnummer = Organisasjonsnummer("976663934"),
                organisasjonsform = "BEDR",
                overordnetEnhet = Organisasjonsnummer("123456789"),
                navn = "Underenhet 1 AS",
                erUtenlandsk = false,
            )

            database.runAndRollback {
                repository.arrangor.save(hovedenhet)
                repository.arrangor.save(underenhet)

                queries.arrangor.getHovedenhetById(hovedenhet.id).should {
                    it.underenheter.shouldNotBeNull() shouldContainExactlyIds listOf(underenhet.id)
                }
            }
        }

        test("Upsert slettet enhet") {
            database.runAndRollback {
                val slettetDato = LocalDate.of(2024, 1, 1)

                val underenhet1 = Arrangor(
                    id = UUID.randomUUID(),
                    organisasjonsnummer = Organisasjonsnummer("880907522"),
                    organisasjonsform = "BEDR",
                    overordnetEnhet = Organisasjonsnummer("982254604"),
                    navn = "REMA 1000 NORGE AS REGION NORDLAND",
                    slettetDato = slettetDato,
                    erUtenlandsk = false,
                )

                val overordnet = Arrangor(
                    id = UUID.randomUUID(),
                    navn = "REMA 1000 AS",
                    organisasjonsnummer = Organisasjonsnummer("982254604"),
                    organisasjonsform = "AS",
                    erUtenlandsk = false,
                )

                repository.arrangor.save(overordnet)
                repository.arrangor.save(underenhet1)

                queries.arrangor.get(overordnet.organisasjonsnummer).shouldNotBeNull().should {
                    it.slettetDato shouldBe null
                }
                queries.arrangor.get(underenhet1.organisasjonsnummer).shouldNotBeNull().should {
                    it.slettetDato shouldBe slettetDato
                }
                queries.arrangor.getAll(slettet = true).items shouldContainExactlyInAnyOrder listOf(underenhet1.toArrangorDto())
                queries.arrangor.getAll(slettet = false).items shouldContainExactlyInAnyOrder listOf(overordnet.toArrangorDto())
            }
        }
    }

    context("kontaktperson hos arrangør") {
        test("crud") {
            database.runAndRollback {
                val arrangor = Arrangor(
                    id = UUID.randomUUID(),
                    navn = "REMA 1000 AS",
                    organisasjonsnummer = Organisasjonsnummer("982254604"),
                    organisasjonsform = "AS",
                    erUtenlandsk = false,
                )
                repository.arrangor.save(arrangor)

                val kontaktperson1 = ArrangorKontaktperson(
                    id = UUID.randomUUID(),
                    arrangorId = arrangor.id,
                    navn = "Fredrik",
                    telefon = "322232323",
                    epost = "fredrik@gmail.com",
                    beskrivelse = null,
                    ansvarligFor = listOf(),
                )

                val kontaktperson2 = ArrangorKontaktperson(
                    id = UUID.randomUUID(),
                    arrangorId = arrangor.id,
                    navn = "Trond",
                    telefon = "232232323",
                    epost = "trond@gmail.com",
                    beskrivelse = "Adm. dir.",
                    ansvarligFor = listOf(),
                )

                repository.arrangor.save(arrangor.copy(kontaktpersoner = listOf(kontaktperson1, kontaktperson2)))

                repository.arrangor.get(arrangor.id)
                    .shouldNotBeNull().kontaktpersoner shouldContainExactlyInAnyOrder listOf(
                    kontaktperson1,
                    kontaktperson2,
                )

                queries.arrangor.getKontaktpersoner(arrangor.id) shouldContainExactlyInAnyOrder listOf(
                    kontaktperson1,
                    kontaktperson2,
                )

                repository.arrangor.save(arrangor.copy(kontaktpersoner = listOf(kontaktperson2)))

                repository.arrangor.getByOrganisasjonsnummer(arrangor.organisasjonsnummer)
                    .shouldNotBeNull()
                    .kontaktpersoner shouldContainExactlyInAnyOrder listOf(kontaktperson2)

                queries.arrangor.getKontaktpersoner(arrangor.id) shouldContainExactlyInAnyOrder listOf(kontaktperson2)
            }
        }
    }
})

private fun Arrangor.toArrangorDto() = ArrangorDto(
    id = id,
    organisasjonsnummer = organisasjonsnummer,
    organisasjonsform = organisasjonsform,
    navn = navn,
    overordnetEnhet = overordnetEnhet,
    slettetDato = slettetDato,
    erUtenlandsk = erUtenlandsk,
)

private infix fun Collection<ArrangorDto>.shouldContainExactlyIds(listOf: Collection<UUID>) {
    map { it.id }.shouldContainExactlyInAnyOrder(listOf)
}
