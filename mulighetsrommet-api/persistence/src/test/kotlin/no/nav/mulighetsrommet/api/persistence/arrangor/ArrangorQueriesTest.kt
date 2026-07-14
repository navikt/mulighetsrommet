package no.nav.mulighetsrommet.api.persistence.arrangor

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.mulighetsrommet.admin.arrangor.toDto
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.domain.arrangor.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.domain.arrangor.Betalingsinformasjon
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.persistence.SqlAdminDatabaseTestListener
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.time.LocalDate
import java.util.UUID

class ArrangorQueriesTest : FunSpec({
    val database = extension(SqlAdminDatabaseTestListener())

    context("crud") {
        test("søk og filtrering på arrangører") {
            database.runAndRollback {
                val hovedenhet = Arrangor.Norsk(
                    id = UUID.randomUUID(),
                    navn = "REMA 1000 AS",
                    organisasjonsnummer = Organisasjonsnummer("982254604"),
                    organisasjonsform = "AS",
                )
                repository.arrangor.save(hovedenhet)

                val underenhet1 = Arrangor.Norsk(
                    id = UUID.randomUUID(),
                    organisasjonsnummer = Organisasjonsnummer("880907522"),
                    organisasjonsform = "BEDR",
                    overordnetEnhet = hovedenhet.organisasjonsnummer,
                    navn = "REMA 1000 NORGE AS REGION NORDLAND",
                )
                repository.arrangor.save(underenhet1)

                val underenhet2 = Arrangor.Norsk(
                    id = UUID.randomUUID(),
                    organisasjonsnummer = Organisasjonsnummer("912704327"),
                    organisasjonsform = "BEDR",
                    overordnetEnhet = hovedenhet.organisasjonsnummer,
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
                    utenlandsk.toDto(),
                )
                queries.arrangor.getAll(utenlandsk = false).items shouldContainExactlyInAnyOrder listOf(
                    hovedenhet.toDto(),
                    underenhet1.toDto(),
                    underenhet2.toDto(),
                )

                queries.arrangor.getAll(sok = "utenlandsk").items shouldContainExactlyInAnyOrder listOf(utenlandsk.toDto())
                queries.arrangor.getAll(sok = "østland").items shouldContainExactlyInAnyOrder listOf(underenhet2.toDto())

                queries.arrangor.getAll(overordnetEnhetOrgnr = hovedenhet.organisasjonsnummer).items shouldContainExactlyInAnyOrder listOf(
                    underenhet1.toDto(),
                    underenhet2.toDto(),
                )
                queries.arrangor.getAll(overordnetEnhetOrgnr = underenhet1.organisasjonsnummer).items.shouldBeEmpty()
            }
        }

        test("Upsert underenhet etter overenhet") {
            database.runAndRollback {
                val hovedenhet = ArrangorFixtures.hovedenhet
                val underenhet1 = ArrangorFixtures.underenhet1

                repository.arrangor.save(hovedenhet)
                repository.arrangor.save(underenhet1)

                queries.arrangor.get(underenhet1.organisasjonsnummer).shouldNotBeNull().should {
                    it.organisasjonsnummer shouldBe underenhet1.organisasjonsnummer
                }
                queries.arrangor.get(hovedenhet.organisasjonsnummer).shouldNotBeNull().should {
                    it.underenheter.shouldNotBeNull().shouldHaveSize(1).first().should { e ->
                        e.navn shouldBe underenhet1.navn
                        e.organisasjonsnummer shouldBe underenhet1.organisasjonsnummer
                    }
                }
            }
        }

        test("getByHovedenhet") {
            val hovedenhet = ArrangorFixtures.hovedenhet
            val underenhet = ArrangorFixtures.underenhet1

            database.runAndRollback {
                repository.arrangor.save(hovedenhet)
                repository.arrangor.save(underenhet)

                queries.arrangor.getHovedenhetById(hovedenhet.id).should {
                    it.underenheter.shouldNotBeNull().shouldHaveSize(1).first().id shouldBe underenhet.id
                }
            }
        }

        test("Upsert slettet enhet") {
            database.runAndRollback {
                val slettetDato = LocalDate.of(2024, 1, 1)

                val hovedenhet = ArrangorFixtures.hovedenhet
                val underenhet1 = ArrangorFixtures.underenhet1.copy(slettetDato = slettetDato)

                repository.arrangor.save(hovedenhet)
                repository.arrangor.save(underenhet1)

                queries.arrangor.get(hovedenhet.organisasjonsnummer).shouldNotBeNull().should {
                    it.slettetDato shouldBe null
                }
                queries.arrangor.get(underenhet1.organisasjonsnummer).shouldNotBeNull().should {
                    it.slettetDato shouldBe slettetDato
                }
                queries.arrangor.getAll(slettet = true).items shouldContainExactlyInAnyOrder listOf(underenhet1.toDto())
                queries.arrangor.getAll(slettet = false).items shouldContainExactlyInAnyOrder listOf(hovedenhet.toDto())
            }
        }
    }

    context("utenlandsk arrangør") {
        test("betalingsinformasjon og adresse blir persistert og hentet via repository.save/get") {
            database.runAndRollback {
                val arrangor = ArrangorFixtures.Utenlandsk.hovedenhet.copy(
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
                val arrangor = ArrangorFixtures.Utenlandsk.hovedenhet.copy(navn = "Utenlandsk arrangør uten bankdata")
                repository.arrangor.save(arrangor)

                repository.arrangor.get(arrangor.id).shouldBeInstanceOf<Arrangor.Utenlandsk>().should {
                    it.betalingsinformasjon shouldBe null
                    it.adresse shouldBe null
                }
            }
        }

        test("save() bevarer betalingsinformasjon og adresse når andre felter oppdateres") {
            database.runAndRollback {
                val arrangor = ArrangorFixtures.Utenlandsk.hovedenhet.copy(
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
                val arrangor = ArrangorFixtures.Utenlandsk.hovedenhet.copy(
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
                val arrangor = ArrangorFixtures.hovedenhet
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
                val arrangor = ArrangorFixtures.Utenlandsk.hovedenhet
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
