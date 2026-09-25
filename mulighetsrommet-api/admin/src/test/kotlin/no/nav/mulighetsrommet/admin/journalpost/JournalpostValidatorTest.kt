package no.nav.mulighetsrommet.admin.journalpost

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.saf.SafBruker
import no.nav.mulighetsrommet.api.clients.saf.SafBrukerIdType
import no.nav.mulighetsrommet.api.clients.saf.SafClient
import no.nav.mulighetsrommet.api.clients.saf.SafError
import no.nav.mulighetsrommet.api.clients.saf.SafJournalpost
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.tokenprovider.AccessType

class JournalpostValidatorTest : FunSpec({
    val pointer = "/tilskudd/0/soknadJournalpostId"
    val person = NorskIdent("12345678910")
    val arrangor = Organisasjonsnummer("123456789")

    fun validatorFor(journalpost: SafJournalpost): JournalpostValidator {
        val saf = mockk<SafClient>()
        coEvery { saf.hentJournalpost(any(), any()) } returns journalpost.right()
        return JournalpostValidator(saf)
    }

    test("journalpost uten bruker er gyldig") {
        val validator = validatorFor(SafJournalpost("453857496", bruker = null))

        validator.validerJournalpost("453857496", person, arrangor, pointer, AccessType.M2M).shouldBeRight()
    }

    test("journalpost knyttet til riktig person er gyldig") {
        val validator = validatorFor(
            SafJournalpost("453857496", bruker = SafBruker(id = person.value, type = SafBrukerIdType.FNR)),
        )

        validator.validerJournalpost("453857496", person, arrangor, pointer, AccessType.M2M).shouldBeRight()
    }

    test("journalpost knyttet til feil person gir feil") {
        val validator = validatorFor(
            SafJournalpost("453857496", bruker = SafBruker(id = "10987654321", type = SafBrukerIdType.FNR)),
        )

        validator.validerJournalpost("453857496", person, arrangor, pointer, AccessType.M2M).shouldBeLeft(
            listOf(FieldError(pointer, "Journalposten tilhører en annen person enn deltakeren")),
        )
    }

    test("journalpost knyttet til riktig virksomhet er gyldig") {
        val validator = validatorFor(
            SafJournalpost("453857496", bruker = SafBruker(id = arrangor.value, type = SafBrukerIdType.ORGNR)),
        )

        validator.validerJournalpost("453857496", person, arrangor, pointer, AccessType.M2M).shouldBeRight()
    }

    test("journalpost knyttet til feil virksomhet gir feil") {
        val validator = validatorFor(
            SafJournalpost("453857496", bruker = SafBruker(id = "987654321", type = SafBrukerIdType.ORGNR)),
        )

        validator.validerJournalpost("453857496", person, arrangor, pointer, AccessType.M2M).shouldBeLeft(
            listOf(FieldError(pointer, "Journalposten tilhører en annen virksomhet enn arrangøren")),
        )
    }

    test("journalpost med aktørid kan ikke verifiseres") {
        val validator = validatorFor(
            SafJournalpost("453857496", bruker = SafBruker(id = "1000012345678", type = SafBrukerIdType.AKTOERID)),
        )

        validator.validerJournalpost("453857496", person, arrangor, pointer, AccessType.M2M).shouldBeLeft(
            listOf(FieldError(pointer, "Kunne ikke verifisere hvem journalposten tilhører")),
        )
    }

    test("journalpost som ikke finnes gir feil") {
        val saf = mockk<SafClient>()
        coEvery { saf.hentJournalpost(any(), any()) } returns SafError.NotFound.left()

        val validator = JournalpostValidator(saf)

        validator.validerJournalpost("finnes-ikke", person, arrangor, pointer, AccessType.M2M).shouldBeLeft(
            listOf(FieldError(pointer, "Fant ingen journalpost med id finnes-ikke")),
        )
    }

    test("feil mot saf gir feil") {
        val saf = mockk<SafClient>()
        coEvery { saf.hentJournalpost(any(), any()) } returns SafError.Error.left()

        val validator = JournalpostValidator(saf)

        validator.validerJournalpost("453857496", person, arrangor, pointer, AccessType.M2M).shouldBeLeft(
            listOf(FieldError(pointer, "Klarte ikke å slå opp journalpost. Prøv igjen senere.")),
        )
    }
})
