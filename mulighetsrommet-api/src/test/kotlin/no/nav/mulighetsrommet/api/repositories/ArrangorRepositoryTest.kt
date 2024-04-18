package no.nav.mulighetsrommet.api.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dto.ArrangorDto
import no.nav.mulighetsrommet.api.domain.dto.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.domain.dto.ArrangorTil
import no.nav.mulighetsrommet.api.domain.dto.BrregVirksomhetDto
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import java.time.LocalDate
import java.util.*

class ArrangorRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    afterEach {
        database.db.truncateAll()
    }

    context("crud") {
        test("søk og filtrering på arrangører") {
            val arrangorRepository = ArrangorRepository(database.db)

            val overordnet = ArrangorDto(
                id = UUID.randomUUID(),
                navn = "REMA 1000 AS",
                organisasjonsnummer = "982254604",
                postnummer = "5174",
                poststed = "Mathopen",
            )
            arrangorRepository.upsert(overordnet)

            val underenhet1 = ArrangorDto(
                id = UUID.randomUUID(),
                organisasjonsnummer = "880907522",
                overordnetEnhet = overordnet.organisasjonsnummer,
                navn = "REMA 1000 NORGE AS REGION NORDLAND",
                postnummer = "5174",
                poststed = "Mathopen",
            )
            arrangorRepository.upsert(underenhet1)

            val underenhet2 = ArrangorDto(
                id = UUID.randomUUID(),
                organisasjonsnummer = "912704327",
                overordnetEnhet = overordnet.organisasjonsnummer,
                navn = "REMA 1000 NORGE AS REGION VESTRE ØSTLAND",
                postnummer = "5174",
                poststed = "Mathopen",
            )
            arrangorRepository.upsert(underenhet2)

            val utenlandsk = ArrangorDto(
                id = UUID.randomUUID(),
                organisasjonsnummer = "100000001",
                navn = "X - Utenlandsk arrangør",
                postnummer = null,
                poststed = null,
            )
            arrangorRepository.upsert(utenlandsk)
            queryOf("update arrangor set er_utenlandsk_virksomhet = true where organisasjonsnummer = '${utenlandsk.organisasjonsnummer}'")
                .asExecute
                .let { database.db.run(it) }

            arrangorRepository.getAll(utenlandsk = true).items shouldContainExactlyInAnyOrder listOf(utenlandsk)
            arrangorRepository.getAll(utenlandsk = false).items shouldContainExactlyInAnyOrder listOf(
                overordnet,
                underenhet1,
                underenhet2,
            )

            arrangorRepository.getAll(sok = "utenlandsk").items shouldContainExactlyInAnyOrder listOf(utenlandsk)
            arrangorRepository.getAll(sok = "østland").items shouldContainExactlyInAnyOrder listOf(underenhet2)

            arrangorRepository.getAll(overordnetEnhetOrgnr = overordnet.organisasjonsnummer).items shouldContainExactlyInAnyOrder listOf(
                underenhet1,
                underenhet2,
            )
            arrangorRepository.getAll(overordnetEnhetOrgnr = underenhet1.organisasjonsnummer).items.shouldBeEmpty()
        }

        test("Upsert underenhet etter overenhet") {
            val arrangorRepository = ArrangorRepository(database.db)

            val underenhet1 = BrregVirksomhetDto(
                organisasjonsnummer = "880907522",
                overordnetEnhet = "982254604",
                navn = "REMA 1000 NORGE AS REGION NORDLAND",
                postnummer = "5174",
                poststed = "Mathopen",
            )

            val overordnet = BrregVirksomhetDto(
                navn = "REMA 1000 AS",
                organisasjonsnummer = "982254604",
                underenheter = listOf(),
                postnummer = "5174",
                poststed = "Mathopen",
            )

            arrangorRepository.upsert(overordnet)
            arrangorRepository.upsert(underenhet1)

            arrangorRepository.get(underenhet1.organisasjonsnummer).should {
                it.shouldNotBeNull()
                it.organisasjonsnummer shouldBe underenhet1.organisasjonsnummer
            }
            arrangorRepository.get(overordnet.organisasjonsnummer).should {
                it.shouldNotBeNull()
                it.underenheter.shouldNotBeNull().shouldHaveSize(1).first().should { e ->
                    e.navn shouldBe underenhet1.navn
                    e.organisasjonsnummer shouldBe underenhet1.organisasjonsnummer
                }
            }
        }

        test("Upsert slettet enhet") {
            val arrangorRepository = ArrangorRepository(database.db)

            val slettetDato = LocalDate.of(2024, 1, 1)

            val underenhet1 = ArrangorDto(
                id = UUID.randomUUID(),
                organisasjonsnummer = "880907522",
                overordnetEnhet = "982254604",
                navn = "REMA 1000 NORGE AS REGION NORDLAND",
                slettetDato = slettetDato,
                postnummer = "5174",
                poststed = "Mathopen",
            )

            val overordnet = ArrangorDto(
                id = UUID.randomUUID(),
                navn = "REMA 1000 AS",
                organisasjonsnummer = "982254604",
                postnummer = "5174",
                poststed = "Mathopen",
            )

            arrangorRepository.upsert(overordnet)
            arrangorRepository.upsert(underenhet1)

            arrangorRepository.get(overordnet.organisasjonsnummer).should {
                it.shouldNotBeNull()
                it.slettetDato shouldBe null
            }
            arrangorRepository.get(underenhet1.organisasjonsnummer).should {
                it.shouldNotBeNull()
                it.slettetDato shouldBe slettetDato
            }
            arrangorRepository.getAll(slettet = true).items shouldContainExactlyInAnyOrder listOf(underenhet1)
            arrangorRepository.getAll(slettet = false).items shouldContainExactlyInAnyOrder listOf(overordnet)
        }

        test("Filter på avtale eller gjennomforing") {
            val hovedenhet = ArrangorFixtures.hovedenhet
            val underenhet = ArrangorFixtures.underenhet1

            MulighetsrommetTestDomain(
                arrangorer = listOf(hovedenhet, underenhet),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(AvtaleFixtures.oppfolging),
                gjennomforinger = listOf(TiltaksgjennomforingFixtures.Oppfolging1),
            ).initialize(database.db)

            val arrangorRepository = ArrangorRepository(database.db)

            arrangorRepository.getAll().items shouldContainExactlyInAnyOrder listOf(hovedenhet, underenhet)
            arrangorRepository.getAll(til = ArrangorTil.AVTALE).should {
                it.items.size shouldBe 1
                it.items[0] shouldBe hovedenhet
            }
            arrangorRepository.getAll(til = ArrangorTil.TILTAKSGJENNOMFORING).should {
                it.items.size shouldBe 1
                it.items[0] shouldBe underenhet
            }
        }
    }

    context("kontaktperson hos arrangør") {
        test("crud") {
            val arrangorRepository = ArrangorRepository(database.db)
            val arrangorId = UUID.randomUUID()
            val arrangor = ArrangorDto(
                id = arrangorId,
                navn = "REMA 1000 AS",
                organisasjonsnummer = "982254604",
                underenheter = null,
                postnummer = "5174",
                poststed = "Mathopen",
            )
            arrangorRepository.upsert(arrangor)

            val kontaktperson = ArrangorKontaktperson(
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
            arrangorRepository.upsertKontaktperson(kontaktperson)
            arrangorRepository.upsertKontaktperson(kontaktperson2)

            arrangorRepository.getKontaktpersoner(arrangorId) shouldContainExactlyInAnyOrder listOf(
                kontaktperson,
                kontaktperson2,
            )
        }
    }
})
