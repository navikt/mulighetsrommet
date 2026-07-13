package no.nav.mulighetsrommet.admin.arrangor

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.admin.enhetsregister.EnhetsregisterError
import no.nav.mulighetsrommet.admin.enhetsregister.EnhetsregisterGateway
import no.nav.mulighetsrommet.admin.enhetsregister.Hovedenhet
import no.nav.mulighetsrommet.admin.enhetsregister.Underenhet
import no.nav.mulighetsrommet.admin.enhetsregister.VirksomhetOppslag
import no.nav.mulighetsrommet.admin.testing.TestAdminDatabase
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.time.LocalDate
import java.util.UUID

class SyncArrangorUseCaseTest : FunSpec({
    val underenhet = Underenhet(
        organisasjonsnummer = Organisasjonsnummer("234567891"),
        organisasjonsform = "BEDR",
        navn = "Underenhet til Testbedriften AS",
        overordnetEnhet = Organisasjonsnummer("123456789"),
    )
    val hovedenhet = Hovedenhet(
        organisasjonsnummer = Organisasjonsnummer("123456789"),
        organisasjonsform = "AS",
        navn = "Testbedriften AS",
    )

    context("get or sync arrangør fra enhetsregisteret") {
        test("skal synkronisere hovedenhet uten underenheter fra enhetsregisteret til databasen gitt orgnr til hovedenhet") {
            val db = TestAdminDatabase()
            val enhetsregister = mockk<EnhetsregisterGateway> {
                coEvery { hentVirksomhet(hovedenhet.organisasjonsnummer) } answers {
                    VirksomhetOppslag.Funnet(hovedenhet).right()
                }
            }
            val syncArrangor = SyncArrangorUseCase(db, enhetsregister)

            syncArrangor.execute(SyncArrangorIfMissing(hovedenhet.organisasjonsnummer)).shouldBeRight().should {
                it.id.shouldNotBeNull()
                it.navn shouldBe "Testbedriften AS"
                it.organisasjonsnummer shouldBe Organisasjonsnummer("123456789")
            }

            db.repository.arrangor.getByOrganisasjonsnummer(underenhet.organisasjonsnummer).shouldBeNull()
        }

        test("skal synkronisere hovedenhet i tillegg til underenhet fra enhetsregisteret til databasen gitt orgnr til underenhet") {
            val db = TestAdminDatabase()
            val enhetsregister = mockk<EnhetsregisterGateway> {
                coEvery { hentVirksomhet(hovedenhet.organisasjonsnummer) } answers {
                    VirksomhetOppslag.Funnet(hovedenhet).right()
                }
                coEvery { hentVirksomhet(underenhet.organisasjonsnummer) } answers {
                    VirksomhetOppslag.Funnet(underenhet).right()
                }
            }
            val syncArrangor = SyncArrangorUseCase(db, enhetsregister)

            syncArrangor.execute(SyncArrangorIfMissing(underenhet.organisasjonsnummer)).shouldBeRight().should {
                it.navn shouldBe "Underenhet til Testbedriften AS"
                it.organisasjonsnummer shouldBe Organisasjonsnummer("234567891")
            }

            db.repository.arrangor.getByOrganisasjonsnummer(hovedenhet.organisasjonsnummer).shouldNotBeNull().should {
                it.navn shouldBe "Testbedriften AS"
                it.organisasjonsnummer shouldBe Organisasjonsnummer("123456789")
            }
        }

        test("skal synkronisere slettet enhet fra enhetsregisteret og til databasen gitt orgnr til enheten") {
            val db = TestAdminDatabase()

            val orgnr = Organisasjonsnummer("100200300")
            val slettetVirksomhet = Hovedenhet(
                organisasjonsnummer = orgnr,
                organisasjonsform = "AS",
                navn = "Slettet bedrift",
                slettetDato = LocalDate.of(2020, 1, 1),
            )

            val enhetsregister = mockk<EnhetsregisterGateway> {
                coEvery { hentVirksomhet(orgnr) } answers {
                    VirksomhetOppslag.Funnet(slettetVirksomhet).right()
                }
            }
            val syncArrangor = SyncArrangorUseCase(db, enhetsregister)

            syncArrangor.execute(SyncArrangorIfMissing(orgnr)).shouldBeRight().should {
                it.navn shouldBe "Slettet bedrift"
                it.organisasjonsnummer shouldBe orgnr
                it.slettetDato shouldBe LocalDate.of(2020, 1, 1)
            }
        }

        test("skal synkronisere slettetDato for enhet fjernet av juridiske årsaker når arrangør eksisterer") {
            val db = TestAdminDatabase()

            val arrangor = Arrangor.Norsk(
                id = UUID.randomUUID(),
                organisasjonsnummer = Organisasjonsnummer("100200300"),
                organisasjonsform = "AS",
                navn = "Bedrift Bedriftsson",
                slettetDato = null,
            )
            db.repository.arrangor.save(arrangor)

            val orgnr = Organisasjonsnummer("100200300")
            val slettetDato = LocalDate.of(2020, 1, 1)

            val enhetsregister = mockk<EnhetsregisterGateway> {
                coEvery { hentVirksomhet(orgnr) } answers {
                    VirksomhetOppslag.FjernetAvJuridiskeArsaker(orgnr, slettetDato).right()
                }
            }
            val syncArrangor = SyncArrangorUseCase(db, enhetsregister)

            syncArrangor.execute(SyncArrangor(orgnr))
                .shouldBeLeft(SyncArrangorError.FjernetAvJuridiskeArsaker(orgnr, slettetDato))

            db.repository.arrangor.getByOrganisasjonsnummer(orgnr).shouldNotBeNull().should {
                it.navn shouldBe "Bedrift Bedriftsson"
                it.organisasjonsnummer shouldBe orgnr
                it.slettetDato shouldBe LocalDate.of(2020, 1, 1)
            }
        }

        test("skal ikke synkronisere enhet fjernet av juridiske årsaker når arrangør ikke eksisterer") {
            val db = TestAdminDatabase()

            val orgnr = Organisasjonsnummer("100200300")
            val slettetDato = LocalDate.of(2020, 1, 1)

            val enhetsregister = mockk<EnhetsregisterGateway> {
                coEvery { hentVirksomhet(orgnr) } answers {
                    VirksomhetOppslag.FjernetAvJuridiskeArsaker(orgnr, slettetDato).right()
                }
            }
            val syncArrangor = SyncArrangorUseCase(db, enhetsregister)

            syncArrangor.execute(SyncArrangor(orgnr))
                .shouldBeLeft(SyncArrangorError.FjernetAvJuridiskeArsaker(orgnr, slettetDato))

            db.repository.arrangor.getByOrganisasjonsnummer(orgnr).shouldBeNull()
        }

        test("EnhetsregisterError når enhet ikke finnes") {
            val db = TestAdminDatabase()

            val orgnr = Organisasjonsnummer("123123123")

            val enhetsregister = mockk<EnhetsregisterGateway> {
                coEvery { hentVirksomhet(orgnr) } answers {
                    EnhetsregisterError.IkkeFunnet.left()
                }
            }
            val syncArrangor = SyncArrangorUseCase(db, enhetsregister)

            syncArrangor.execute(SyncArrangorIfMissing(orgnr)) shouldBeLeft SyncArrangorError.Enhetsregister(
                EnhetsregisterError.IkkeFunnet,
            )

            db.repository.arrangor.getByOrganisasjonsnummer(orgnr).shouldBeNull()
        }
    }
})
