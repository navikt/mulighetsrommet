package no.nav.mulighetsrommet.admin.journalpost

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.saf.SafClient
import no.nav.mulighetsrommet.api.clients.saf.SafError
import no.nav.mulighetsrommet.api.clients.saf.SafJournalpost
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.tokenprovider.AccessType

class JournalpostValidatorTest : FunSpec({
    val pointer = "/tilskudd/0/soknadJournalpostId"

    test("journalpost som finnes er gyldig") {
        val saf = mockk<SafClient>()
        coEvery { saf.hentJournalpost(any(), any()) } returns SafJournalpost("453857496").right()

        val validator = JournalpostValidator(saf)

        validator.validerJournalpostFinnes("453857496", pointer, AccessType.M2M).shouldBeRight()
    }

    test("journalpost som ikke finnes gir feil") {
        val saf = mockk<SafClient>()
        coEvery { saf.hentJournalpost(any(), any()) } returns SafError.NotFound.left()

        val validator = JournalpostValidator(saf)

        validator.validerJournalpostFinnes("finnes-ikke", pointer, AccessType.M2M).shouldBeLeft(
            listOf(FieldError(pointer, "Fant ingen journalpost med id finnes-ikke")),
        )
    }

    test("feil mot saf gir feil") {
        val saf = mockk<SafClient>()
        coEvery { saf.hentJournalpost(any(), any()) } returns SafError.Error.left()

        val validator = JournalpostValidator(saf)

        validator.validerJournalpostFinnes("453857496", pointer, AccessType.M2M).shouldBeLeft(
            listOf(FieldError(pointer, "Klarte ikke å slå opp journalpost. Prøv igjen senere.")),
        )
    }
})
