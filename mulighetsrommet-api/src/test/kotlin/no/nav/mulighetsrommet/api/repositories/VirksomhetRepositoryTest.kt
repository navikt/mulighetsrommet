package no.nav.mulighetsrommet.api.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.OverordnetEnhetDbo
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetKontaktperson
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.utils.VirksomhetTil
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import java.time.LocalDate
import java.util.*

class VirksomhetRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    context("crud") {
        test("Upsert virksomhet med underenheter") {
            val virksomhetRepository = VirksomhetRepository(database.db)

            val underenhet1 = VirksomhetDto(
                organisasjonsnummer = "880907522",
                overordnetEnhet = "982254604",
                navn = "REMA 1000 NORGE AS REGION NORDLAND",
                postnummer = "5174",
                poststed = "Mathopen",
            )
            val underenhet2 = VirksomhetDto(
                organisasjonsnummer = "912704327",
                overordnetEnhet = "982254604",
                navn = "REMA 1000 NORGE AS REGION VESTRE ØSTLAND",
                postnummer = "5174",
                poststed = "Mathopen",
            )

            val overordnet = OverordnetEnhetDbo(
                navn = "REMA 1000 AS",
                organisasjonsnummer = "982254604",
                underenheter = listOf(underenhet1, underenhet2),
                slettetDato = null,
                postnummer = "5174",
                poststed = "Mathopen",
            )
            virksomhetRepository.upsertOverordnetEnhet(overordnet)

            virksomhetRepository.get(overordnet.organisasjonsnummer).should {
                it.shouldNotBeNull()
                it.navn shouldBe "REMA 1000 AS"
                it.underenheter.shouldNotBeNull() shouldContainExactlyInAnyOrder listOf(
                    underenhet1,
                    underenhet2,
                )
            }

            virksomhetRepository.upsertOverordnetEnhet(overordnet.copy(underenheter = listOf(underenhet1)))
            virksomhetRepository.get(overordnet.organisasjonsnummer).should {
                it.shouldNotBeNull()
                it.underenheter.shouldNotBeNull() shouldContainExactlyInAnyOrder listOf(underenhet1)
            }
        }

        test("Upsert virksomhet med underenheter oppdaterer korrekt data ved conflict på organisasjonsnummer") {
            val virksomhetRepository = VirksomhetRepository(database.db)

            val underenhet1 = VirksomhetDto(
                organisasjonsnummer = "880907522",
                overordnetEnhet = "982254604",
                navn = "REMA 1000 NORGE AS REGION NORDLAND",
                postnummer = "5174",
                poststed = "Mathopen",
            )

            val overordnet = OverordnetEnhetDbo(
                navn = "REMA 1000 AS",
                organisasjonsnummer = "982254604",
                underenheter = listOf(underenhet1),
                slettetDato = null,
                postnummer = "5174",
                poststed = "Mathopen",
            )

            virksomhetRepository.upsertOverordnetEnhet(overordnet)
            virksomhetRepository.upsertOverordnetEnhet(
                overordnet.copy(
                    postnummer = "9988",
                    poststed = "Olsenåsen",
                    navn = "Stopp konflikten",
                ),
            )

            virksomhetRepository.get(overordnet.organisasjonsnummer).should {
                it.shouldNotBeNull()
                it.navn shouldBe "Stopp konflikten"
                it.postnummer shouldBe "9988"
                it.poststed shouldBe "Olsenåsen"
                it.underenheter.shouldNotBeNull() shouldContainExactlyInAnyOrder listOf(underenhet1)
            }
        }

        test("Upsert underenhet etter overenhet") {
            val virksomhetRepository = VirksomhetRepository(database.db)

            val underenhet1 = VirksomhetDto(
                organisasjonsnummer = "880907522",
                overordnetEnhet = "982254604",
                navn = "REMA 1000 NORGE AS REGION NORDLAND",
                postnummer = "5174",
                poststed = "Mathopen",
            )

            val overordnet = VirksomhetDto(
                navn = "REMA 1000 AS",
                organisasjonsnummer = "982254604",
                underenheter = listOf(),
                postnummer = "5174",
                poststed = "Mathopen",
            )

            virksomhetRepository.upsert(overordnet)
            virksomhetRepository.upsert(underenhet1)

            virksomhetRepository.get(underenhet1.organisasjonsnummer).should {
                it.shouldNotBeNull()
                it.organisasjonsnummer shouldBe underenhet1.organisasjonsnummer
            }
            virksomhetRepository.get(overordnet.organisasjonsnummer).should {
                it.shouldNotBeNull()
                it.underenheter shouldContainExactly listOf(underenhet1)
            }
        }

        test("Upsert slettet enhet") {
            val virksomhetRepository = VirksomhetRepository(database.db)

            val slettetDato = LocalDate.of(2024, 1, 1)

            val underenhet1 = VirksomhetDto(
                organisasjonsnummer = "880907522",
                overordnetEnhet = "982254604",
                navn = "REMA 1000 NORGE AS REGION NORDLAND",
                slettetDato = slettetDato,
                postnummer = "5174",
                poststed = "Mathopen",
            )

            val overordnet = OverordnetEnhetDbo(
                navn = "REMA 1000 AS",
                organisasjonsnummer = "982254604",
                underenheter = listOf(underenhet1),
                slettetDato = slettetDato,
                postnummer = "5174",
                poststed = "Mathopen",
            )

            virksomhetRepository.upsertOverordnetEnhet(overordnet)

            virksomhetRepository.get(overordnet.organisasjonsnummer).should {
                it.shouldNotBeNull()
                it.slettetDato shouldBe slettetDato
            }
            virksomhetRepository.get(underenhet1.organisasjonsnummer).should {
                it.shouldNotBeNull()
                it.slettetDato shouldBe slettetDato
            }
        }

        test("Delete overordnet cascader") {
            val virksomhetRepository = VirksomhetRepository(database.db)

            val underenhet1 = VirksomhetDto(
                organisasjonsnummer = "880907522",
                overordnetEnhet = "982254604",
                navn = "REMA 1000 NORGE AS REGION NORDLAND",
                postnummer = "5174",
                poststed = "Mathopen",
            )

            val overordnet = OverordnetEnhetDbo(
                navn = "REMA 1000 AS",
                organisasjonsnummer = "982254604",
                underenheter = listOf(underenhet1),
                postnummer = "5174",
                poststed = "Mathopen",
                slettetDato = null,
            )
            virksomhetRepository.upsertOverordnetEnhet(overordnet)

            virksomhetRepository.get(underenhet1.organisasjonsnummer).should {
                it.shouldNotBeNull()
                it.organisasjonsnummer shouldBe underenhet1.organisasjonsnummer
            }

            virksomhetRepository.delete(overordnet.organisasjonsnummer)
            virksomhetRepository.get(underenhet1.organisasjonsnummer).should {
                it.shouldBeNull()
            }
            virksomhetRepository.get(overordnet.organisasjonsnummer).should {
                it.shouldBeNull()
            }
        }

        test("Filter på avtale eller gjennomforing") {
            MulighetsrommetTestDomain(
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(AvtaleFixtures.oppfolging.copy(leverandorOrganisasjonsnummer = "982254604")),
                gjennomforinger = listOf(TiltaksgjennomforingFixtures.Oppfolging1.copy(arrangorOrganisasjonsnummer = "112254604")),
            ).initialize(database.db)

            val virksomhet1 = VirksomhetDto(
                navn = "REMA 1000 AS",
                organisasjonsnummer = "982254604",
                underenheter = null,
                postnummer = "5174",
                poststed = "Mathopen",
            )
            val virksomhet2 = VirksomhetDto(
                navn = "TEMA 2004 AS",
                organisasjonsnummer = "112254604",
                underenheter = null,
                postnummer = "5174",
                poststed = "Mathopen",
            )
            val virksomhetRepository = VirksomhetRepository(database.db)
            virksomhetRepository.upsert(virksomhet1)
            virksomhetRepository.upsert(virksomhet2)

            virksomhetRepository.getAll(til = VirksomhetTil.AVTALE).should {
                it.size shouldBe 1
                it[0] shouldBe virksomhet1
            }
            virksomhetRepository.getAll(til = VirksomhetTil.TILTAKSGJENNOMFORING).should {
                it.size shouldBe 1
                it[0] shouldBe virksomhet2
            }
            virksomhetRepository.getAll().should {
                it shouldContainExactlyInAnyOrder listOf(virksomhet1, virksomhet2)
            }
        }
    }

    context("virksomhet_kontaktperson") {
        test("crud") {
            val virksomhetRepository = VirksomhetRepository(database.db)
            val virksomhet = VirksomhetDto(
                navn = "REMA 1000 AS",
                organisasjonsnummer = "982254604",
                underenheter = null,
                postnummer = "5174",
                poststed = "Mathopen",
            )
            virksomhetRepository.upsert(virksomhet)

            val kontaktperson = VirksomhetKontaktperson(
                id = UUID.randomUUID(),
                navn = "Fredrik",
                organisasjonsnummer = "982254604",
                telefon = "322232323",
                epost = "fredrik@gmail.com",
                beskrivelse = null,
            )
            val kontaktperson2 = VirksomhetKontaktperson(
                id = UUID.randomUUID(),
                navn = "Trond",
                organisasjonsnummer = "982254604",
                telefon = "232232323",
                epost = "trond@gmail.com",
                beskrivelse = "Adm. dir.",
            )
            virksomhetRepository.upsertKontaktperson(kontaktperson)
            virksomhetRepository.upsertKontaktperson(kontaktperson2)

            virksomhetRepository.getKontaktpersoner("982254604") shouldContainExactlyInAnyOrder listOf(
                kontaktperson,
                kontaktperson2,
            )
        }
    }
})
