package no.nav.mulighetsrommet.api.repositories

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.*
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.*
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetKontaktperson
import no.nav.mulighetsrommet.api.utils.VirksomhetTil
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import java.time.LocalDate
import java.time.LocalDateTime
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
            val underenhet3 = VirksomhetDto(
                organisasjonsnummer = "912704394",
                overordnetEnhet = "982254604",
                navn = "REMA 1000 NORGE AS REGION NORD",
                postnummer = "5174",
                poststed = "Mathopen",
            )

            val overordnet = OverordnetEnhetDbo(
                navn = "REMA 1000 AS",
                organisasjonsnummer = "982254604",
                underenheter = listOf(underenhet1, underenhet2, underenhet3),
                postnummer = "5174",
                poststed = "Mathopen",
            )
            virksomhetRepository.upsertOverordnetEnhet(overordnet).shouldBeRight()

            virksomhetRepository.get(overordnet.organisasjonsnummer).shouldBeRight().should {
                it!!.navn shouldBe "REMA 1000 AS"
                it.underenheter!! shouldHaveSize 3
                it.underenheter!! shouldContainAll listOf(underenhet1, underenhet2, underenhet3)
            }

            virksomhetRepository.upsertOverordnetEnhet(overordnet.copy(underenheter = listOf(underenhet1)))
                .shouldBeRight()
            virksomhetRepository.get(overordnet.organisasjonsnummer).shouldBeRight().should {
                it!!.underenheter!! shouldHaveSize 1
                it.underenheter!! shouldContain underenhet1
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
            val underenhet2 = VirksomhetDto(
                organisasjonsnummer = "912704327",
                overordnetEnhet = "982254604",
                navn = "REMA 1000 NORGE AS REGION VESTRE ØSTLAND",
                postnummer = "5174",
                poststed = "Mathopen",
            )
            val underenhet3 = VirksomhetDto(
                organisasjonsnummer = "912704394",
                overordnetEnhet = "982254604",
                navn = "REMA 1000 NORGE AS REGION NORD",
                postnummer = "5174",
                poststed = "Mathopen",
            )

            val overordnet = OverordnetEnhetDbo(
                navn = "REMA 1000 AS",
                organisasjonsnummer = "982254604",
                underenheter = listOf(underenhet1, underenhet2, underenhet3),
                postnummer = "5174",
                poststed = "Mathopen",
            )
            virksomhetRepository.upsertOverordnetEnhet(overordnet).shouldBeRight()
            virksomhetRepository.upsertOverordnetEnhet(
                overordnet.copy(
                    postnummer = "9988",
                    poststed = "Olsenåsen",
                    navn = "Stopp konflikten",
                ),
            ).shouldBeRight()

            virksomhetRepository.get(overordnet.organisasjonsnummer).shouldBeRight().should {
                it!!.navn shouldBe "Stopp konflikten"
                it.postnummer shouldBe "9988"
                it.poststed shouldBe "Olsenåsen"
                it.underenheter!! shouldHaveSize 3
                it.underenheter!! shouldContainAll listOf(underenhet1, underenhet2, underenhet3)
            }

            virksomhetRepository.upsertOverordnetEnhet(overordnet.copy(underenheter = listOf(underenhet1)))
                .shouldBeRight()
            virksomhetRepository.get(overordnet.organisasjonsnummer).shouldBeRight().should {
                it!!.underenheter!! shouldHaveSize 1
                it.underenheter!! shouldContain underenhet1
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

            virksomhetRepository.upsert(overordnet).shouldBeRight()
            virksomhetRepository.upsert(underenhet1).shouldBeRight()

            virksomhetRepository.get(underenhet1.organisasjonsnummer).shouldBeRight().should {
                it!!.organisasjonsnummer shouldBe underenhet1.organisasjonsnummer
            }
            virksomhetRepository.get(overordnet.organisasjonsnummer).shouldBeRight().should {
                it!!.underenheter shouldContainExactly listOf(underenhet1)
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
            )
            virksomhetRepository.upsertOverordnetEnhet(overordnet).shouldBeRight()

            virksomhetRepository.get(underenhet1.organisasjonsnummer).shouldBeRight().should {
                it!!.organisasjonsnummer shouldBe underenhet1.organisasjonsnummer
            }

            virksomhetRepository.delete(overordnet.organisasjonsnummer).shouldBeRight()
            virksomhetRepository.get(underenhet1.organisasjonsnummer).shouldBeRight().should {
                it shouldBe null
            }
            virksomhetRepository.get(overordnet.organisasjonsnummer).shouldBeRight().should {
                it shouldBe null
            }
        }

        test("Filter på avtale eller gjennomforing") {
            val virksomhetRepository = VirksomhetRepository(database.db)
            val avtaleRepository = AvtaleRepository(database.db)
            val tiltakstypeRepository = TiltakstypeRepository(database.db)
            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
            val navEnhetRepository = NavEnhetRepository(database.db)
            navEnhetRepository.upsert(
                NavEnhetDbo(
                    navn = "Oslo",
                    enhetsnummer = "0100",
                    status = NavEnhetStatus.AKTIV,
                    type = Norg2Type.FYLKE,
                    overordnetEnhet = null,
                ),
            ).shouldBeRight()

            val tiltakstypeId = UUID.randomUUID()
            tiltakstypeRepository.upsert(
                TiltakstypeDbo(
                    tiltakstypeId,
                    "",
                    "",
                    rettPaaTiltakspenger = true,
                    registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
                    sistEndretDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
                    fraDato = LocalDate.of(2023, 1, 11),
                    tilDato = LocalDate.of(2023, 1, 12),
                ),
            ).getOrThrow()

            val avtale = AvtaleDbo(
                id = UUID.randomUUID(),
                navn = "Navn",
                tiltakstypeId = tiltakstypeId,
                leverandorOrganisasjonsnummer = "982254604",
                leverandorUnderenheter = emptyList(),
                startDato = LocalDate.now(),
                sluttDato = LocalDate.now(),
                navEnheter = listOf("0100"),
                avtaletype = Avtaletype.Avtale,
                opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
                avtalenummer = null,
                leverandorKontaktpersonId = null,
                prisbetingelser = null,
                antallPlasser = null,
                url = null,
                administratorer = emptyList(),
                updatedAt = LocalDate.now().atStartOfDay(),
                beskrivelse = null,
                faneinnhold = null,
            )
            avtaleRepository.upsert(avtale)
            val tiltaksgjennomforing = TiltaksgjennomforingDbo(
                id = UUID.randomUUID(),
                navn = "Navn",
                tiltakstypeId = tiltakstypeId,
                tiltaksnummer = null,
                arrangorOrganisasjonsnummer = "112254604",
                startDato = LocalDate.now(),
                avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
                apentForInnsok = true,
                antallPlasser = 12,
                administratorer = emptyList(),
                navRegion = "0100",
                navEnheter = emptyList(),
                oppstart = TiltaksgjennomforingOppstartstype.FELLES,
                opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
                sluttDato = null,
                kontaktpersoner = emptyList(),
                arrangorKontaktpersonId = null,
                stengtFra = null,
                stengtTil = null,
                stedForGjennomforing = "Oslo",
                avtaleId = avtale.id,
                faneinnhold = null,
                beskrivelse = null,
                fremmoteTidspunkt = null,
                fremmoteSted = null,
                deltidsprosent = null,
            )
            tiltaksgjennomforingRepository.upsert(tiltaksgjennomforing)

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
            virksomhetRepository.upsert(virksomhet1).shouldBeRight()
            virksomhetRepository.upsert(virksomhet2).shouldBeRight()

            virksomhetRepository.getAll(til = VirksomhetTil.AVTALE).shouldBeRight().should {
                it.size shouldBe 1
                it[0] shouldBe virksomhet1
            }
            virksomhetRepository.getAll(til = VirksomhetTil.TILTAKSGJENNOMFORING).shouldBeRight()
                .should {
                    it.size shouldBe 1
                    it[0] shouldBe virksomhet2
                }
            virksomhetRepository.getAll().shouldBeRight().should {
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
            virksomhetRepository.upsert(virksomhet).shouldBeRight()

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
