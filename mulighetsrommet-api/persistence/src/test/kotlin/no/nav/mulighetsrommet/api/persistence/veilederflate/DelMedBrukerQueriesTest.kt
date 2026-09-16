package no.nav.mulighetsrommet.api.persistence.veilederflate

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.delmedbruker.DelMedBrukerDbo
import no.nav.mulighetsrommet.api.domain.testing.fixture.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.domain.tiltakdokument.TiltakDokument
import no.nav.mulighetsrommet.api.persistence.SqlAdminDatabaseTestListener
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID

class DelMedBrukerQueriesTest : FunSpec({
    val database = extension(SqlAdminDatabaseTestListener())

    val norskIdent = NorskIdent("12345678910")
    val tiltakDokumentId = UUID.randomUUID()

    fun dbo() = DelMedBrukerDbo(
        norskIdent = norskIdent,
        navIdent = NavIdent("B123456"),
        dialogId = "1",
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        tiltakDokumentId = tiltakDokumentId,
        gjennomforingId = null,
        deltFraEnhet = NavEnhetNummer("0502"),
    )

    fun tiltakDokument(id: UUID = tiltakDokumentId) = TiltakDokument(
        id = id,
        navn = "Test tiltak",
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
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

    test("insert og getLast returnerer delingen med tiltaksinfo") {
        database.runAndRollback {
            repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)
            repository.tiltakDokument.save(tiltakDokument())

            delMedBruker.insert(dbo(), deltFraFylke = NavEnhetNummer("0400"))

            val result = delMedBruker.getLast(norskIdent, tiltakDokumentId)
            result.shouldNotBeNull()
            result.dialogId shouldBe "1"
            result.tiltak.id shouldBe tiltakDokumentId
            result.tiltak.navn shouldBe "Test tiltak"
            result.tiltak.slettet shouldBe false
            result.tiltakstype.navn shouldBe "Oppfølging"
            result.tiltakstype.tiltakskode shouldBe Tiltakskode.OPPFOLGING
        }
    }

    test("getLast returnerer null for ukjent tiltak") {
        database.runAndRollback {
            delMedBruker.getLast(norskIdent, UUID.randomUUID()).shouldBeNull()
        }
    }

    test("getAll returnerer alle delinger for en person") {
        database.runAndRollback {
            repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)
            repository.tiltakDokument.save(tiltakDokument())

            delMedBruker.insert(dbo(), deltFraFylke = null)
            delMedBruker.insert(dbo(), deltFraFylke = null)

            delMedBruker.getAll(norskIdent) shouldHaveSize 2
            delMedBruker.getAll(NorskIdent("10987654321")) shouldHaveSize 0
        }
    }
})
