package no.nav.mulighetsrommet.api.persistence.arrangor

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.mulighetsrommet.admin.arrangor.ArrangorDto
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.domain.arrangor.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.domain.arrangor.Betalingsinformasjon
import no.nav.mulighetsrommet.api.persistence.SqlAdminDatabaseTestListener
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.time.LocalDate
import java.util.UUID

class ArrangorQueriesTest : FunSpec({
    val database = extension(SqlAdminDatabaseTestListener())

    context("crud") {
        test("søk og filtrering på arrangører") {
            database.runAndRollback {
                val overordnet = Arrangor.Norsk(
                    id = UUID.randomUUID(),
                    navn = "REMA 1000 AS",
                    organisasjonsnummer = Organisasjonsnummer("982254604"),
                    organisasjonsform = "AS",
                )
                repository.arrangor.save(overordnet)

                val underenhet1 = Arrangor.Norsk(
                    id = UUID.randomUUID(),
                    organisasjonsnummer = Organisasjonsnummer("880907522"),
                    organisasjonsform = "BEDR",
                    overordnetEnhet = overordnet.organisasjonsnummer,
                    navn = "REMA 1000 NORGE AS REGION NORDLAND",
                )
                repository.arrangor.save(underenhet1)

                val underenhet2 = Arrangor.Norsk(
                    id = UUID.randomUUID(),
                    organisasjonsnummer = Organisasjonsnummer("912704327"),
                    organisasjonsform = "BEDR",
                    overordnetEnhet = overordnet.organisasjonsnummer,
                    navn = "REMA 1000 NORGE AS REGION VESTRE ØSTLAND",
                )
                repository.arrangor.save(underenhet2)

                val utenlandsk = Arrangor.Utenlandsk(
                    id = UUID.randomUUID(),
                    organisasjonsnummer = Organisasjonsnummer("100000001"),
                    organisasjonsform = "IKS",
                    navn = "X - Utenlandsk arrangør",
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
                val underenhet1 = Arrangor.Norsk(
                    id = UUID.randomUUID(),
                    organisasjonsnummer = Organisasjonsnummer("880907522"),
                    organisasjonsform = "BEDR",
                    overordnetEnhet = Organisasjonsnummer("982254604"),
                    navn = "REMA 1000 NORGE AS REGION NORDLAND",
                )

                val overordnet = Arrangor.Norsk(
                    id = UUID.randomUUID(),
                    navn = "REMA 1000 AS",
                    organisasjonsnummer = Organisasjonsnummer("982254604"),
                    organisasjonsform = "AS",
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
            val hovedenhet = Arrangor.Norsk(
                id = UUID.randomUUID(),
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                organisasjonsform = "AS",
                navn = "Hovedenhet AS",
            )

            val underenhet = Arrangor.Norsk(
                id = UUID.randomUUID(),
                organisasjonsnummer = Organisasjonsnummer("976663934"),
                organisasjonsform = "BEDR",
                overordnetEnhet = Organisasjonsnummer("123456789"),
                navn = "Underenhet 1 AS",
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

                val underenhet1 = Arrangor.Norsk(
                    id = UUID.randomUUID(),
                    organisasjonsnummer = Organisasjonsnummer("880907522"),
                    organisasjonsform = "BEDR",
                    overordnetEnhet = Organisasjonsnummer("982254604"),
                    navn = "REMA 1000 NORGE AS REGION NORDLAND",
                    slettetDato = slettetDato,
                )

                val overordnet = Arrangor.Norsk(
                    id = UUID.randomUUID(),
                    navn = "REMA 1000 AS",
                    organisasjonsnummer = Organisasjonsnummer("982254604"),
                    organisasjonsform = "AS",
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

    context("utenlandsk arrangør") {
        test("betalingsinformasjon og adresse blir persistert og hentet via repository.save/get") {
            database.runAndRollback {
                val arrangor = Arrangor.Utenlandsk(
                    id = UUID.randomUUID(),
                    organisasjonsnummer = Organisasjonsnummer("100000002"),
                    organisasjonsform = "IKS",
                    navn = "Irsk arrangør",
                    betalingsinformasjon = Betalingsinformasjon.IBan(
                        bic = "DABAIE2D",
                        iban = "IE29AIBK93115212345678",
                        bankNavn = "AIB Bank",
                        bankLandKode = "IE",
                    ),
                    adresse = Arrangor.Utenlandsk.Adresse(
                        gateNavn = "O'Connell Street 1",
                        by = "Dublin",
                        postNummer = "D01",
                        landKode = "IE",
                    ),
                )

                repository.arrangor.save(arrangor)

                repository.arrangor.get(arrangor.id) shouldBe arrangor
                repository.arrangor.getByOrganisasjonsnummer(arrangor.organisasjonsnummer) shouldBe arrangor
            }
        }

        test("betalingsinformasjon og adresse er null for utenlandsk arrangør som mangler dette") {
            database.runAndRollback {
                val arrangor = Arrangor.Utenlandsk(
                    id = UUID.randomUUID(),
                    organisasjonsnummer = Organisasjonsnummer("100000003"),
                    organisasjonsform = "IKS",
                    navn = "Utenlandsk arrangør uten bankdata",
                )
                repository.arrangor.save(arrangor)

                repository.arrangor.get(arrangor.id).shouldBeInstanceOf<Arrangor.Utenlandsk>().should {
                    it.betalingsinformasjon shouldBe null
                    it.adresse shouldBe null
                }
            }
        }

        test("save() bevarer betalingsinformasjon og adresse når andre felter oppdateres") {
            database.runAndRollback {
                val arrangor = Arrangor.Utenlandsk(
                    id = UUID.randomUUID(),
                    organisasjonsnummer = Organisasjonsnummer("100000004"),
                    organisasjonsform = "IKS",
                    navn = "Irsk arrangør",
                    betalingsinformasjon = Betalingsinformasjon.IBan(
                        bic = "DABAIE2D",
                        iban = "IE29AIBK93115212345678",
                        bankNavn = "AIB Bank",
                        bankLandKode = "IE",
                    ),
                    adresse = Arrangor.Utenlandsk.Adresse(
                        gateNavn = "O'Connell Street 1",
                        by = "Dublin",
                        postNummer = "D01",
                        landKode = "IE",
                    ),
                )
                repository.arrangor.save(arrangor)

                val hentet = repository.arrangor.get(arrangor.id).shouldBeInstanceOf<Arrangor.Utenlandsk>()
                repository.arrangor.save(hentet.copy(navn = "Irsk arrangør (oppdatert navn)"))

                repository.arrangor.get(arrangor.id) shouldBe arrangor.copy(navn = "Irsk arrangør (oppdatert navn)")
            }
        }

        test("save() overskriver eksisterende betalingsinformasjon og adresse ved endring") {
            database.runAndRollback {
                val arrangor = Arrangor.Utenlandsk(
                    id = UUID.randomUUID(),
                    organisasjonsnummer = Organisasjonsnummer("100000006"),
                    organisasjonsform = "IKS",
                    navn = "Irsk arrangør",
                    betalingsinformasjon = Betalingsinformasjon.IBan(
                        bic = "DABAIE2D",
                        iban = "IE29AIBK93115212345678",
                        bankNavn = "AIB Bank",
                        bankLandKode = "IE",
                    ),
                    adresse = Arrangor.Utenlandsk.Adresse(
                        gateNavn = "O'Connell Street 1",
                        by = "Dublin",
                        postNummer = "D01",
                        landKode = "IE",
                    ),
                )
                repository.arrangor.save(arrangor)

                val oppdatert = arrangor.copy(
                    betalingsinformasjon = arrangor.betalingsinformasjon!!.copy(bic = "NEWBIC1"),
                    adresse = arrangor.adresse!!.copy(by = "Cork"),
                )
                repository.arrangor.save(oppdatert)

                repository.arrangor.get(arrangor.id) shouldBe oppdatert
            }
        }
    }

    context("kontaktperson hos arrangør") {
        test("crud") {
            database.runAndRollback {
                val arrangor = Arrangor.Norsk(
                    id = UUID.randomUUID(),
                    navn = "REMA 1000 AS",
                    organisasjonsnummer = Organisasjonsnummer("982254604"),
                    organisasjonsform = "AS",
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

                repository.arrangor.get(arrangor.id).kontaktpersoner shouldContainExactlyInAnyOrder listOf(
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

        test("medKontaktpersoner fungerer også for utenlandsk arrangør") {
            database.runAndRollback {
                val arrangor = Arrangor.Utenlandsk(
                    id = UUID.randomUUID(),
                    organisasjonsnummer = Organisasjonsnummer("100000005"),
                    organisasjonsform = "IKS",
                    navn = "Utenlandsk arrangør",
                )
                repository.arrangor.save(arrangor)

                val kontaktperson = ArrangorKontaktperson(
                    id = UUID.randomUUID(),
                    arrangorId = arrangor.id,
                    navn = "Fredrik",
                    telefon = "322232323",
                    epost = "fredrik@gmail.com",
                    beskrivelse = null,
                    ansvarligFor = listOf(),
                )

                repository.arrangor.save(arrangor.medKontaktpersoner(listOf(kontaktperson)))

                repository.arrangor.get(arrangor.id).kontaktpersoner shouldContainExactlyInAnyOrder listOf(
                    kontaktperson,
                )
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
    erUtenlandsk = this is Arrangor.Utenlandsk,
)

private infix fun Collection<ArrangorDto>.shouldContainExactlyIds(listOf: Collection<UUID>) {
    map { it.id }.shouldContainExactlyInAnyOrder(listOf)
}
