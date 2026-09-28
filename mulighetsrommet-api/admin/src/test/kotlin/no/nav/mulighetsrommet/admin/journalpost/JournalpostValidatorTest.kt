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
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.tokenprovider.AccessType

class JournalpostValidatorTest : FunSpec({
    val person = ForventetBruker.Person(NorskIdent("12345678910"))
    val arrangor = ForventetBruker.Bedrift(Organisasjonsnummer("123456789"))
    val saf = mockk<SafClient>()

    test("journalpost uten bruker er ikke gyldig") {
        coEvery { saf.hentJournalpost(any(), any()) } returns SafJournalpost("453857496").right()

        JournalpostValidator.validerJournalpost("453857496", person, AccessType.M2M, saf).shouldBeLeft(
            JournalpostValidator.JournalpostValideringError.IngenTilknytning,
        )
    }

    test("journalpost knyttet til riktig person er gyldig") {
        coEvery { saf.hentJournalpost(any(), any()) } returns SafJournalpost(
            "453857496",
            bruker = SafBruker(id = person.norskIdent.value, type = SafBrukerIdType.FNR),
        ).right()

        JournalpostValidator.validerJournalpost("453857496", person, AccessType.M2M, saf).shouldBeRight()
    }

    test("journalpost knyttet til feil person gir feil") {
        coEvery { saf.hentJournalpost(any(), any()) } returns SafJournalpost(
            "453857496",
            bruker = SafBruker(id = "10987654321", type = SafBrukerIdType.FNR),
        ).right()

        JournalpostValidator.validerJournalpost("453857496", person, AccessType.M2M, saf).shouldBeLeft(
            JournalpostValidator.JournalpostValideringError.TilhorerAnnenPerson,
        )
    }

    test("journalpost knyttet til riktig virksomhet er gyldig") {
        coEvery { saf.hentJournalpost(any(), any()) } returns SafJournalpost(
            "453857496",
            bruker = SafBruker(id = arrangor.organisasjonsnummer.value, type = SafBrukerIdType.ORGNR),
        ).right()

        JournalpostValidator.validerJournalpost("453857496", arrangor, AccessType.M2M, saf).shouldBeRight()
    }

    test("journalpost knyttet til feil virksomhet gir feil") {
        coEvery { saf.hentJournalpost(any(), any()) } returns SafJournalpost(
            "453857496",
            bruker = SafBruker(id = "987654321", type = SafBrukerIdType.ORGNR),
        ).right()

        JournalpostValidator.validerJournalpost("453857496", arrangor, AccessType.M2M, saf).shouldBeLeft(
            JournalpostValidator.JournalpostValideringError.TilhorerAnnenVirksomhet,
        )
    }

    test("journalpost med aktørid kan ikke verifiseres") {
        coEvery { saf.hentJournalpost(any(), any()) } returns SafJournalpost(
            "453857496",
            bruker = SafBruker(id = "1000012345678", type = SafBrukerIdType.AKTOERID),
        ).right()

        JournalpostValidator.validerJournalpost("453857496", person, AccessType.M2M, saf).shouldBeLeft(
            JournalpostValidator.JournalpostValideringError.AktoerId,
        )
    }

    test("journalpost på feil format gir feil") {
        coEvery { saf.hentJournalpost(any(), any()) } returns SafError.NotFound.left()

        JournalpostValidator.validerJournalpost("finnes-ikke", person, AccessType.M2M, saf).shouldBeLeft(
            JournalpostValidator.JournalpostValideringError.FeilFormat,
        )
    }

    test("journalpost som ikke finnes gir feil") {
        coEvery { saf.hentJournalpost(any(), any()) } returns SafError.NotFound.left()

        JournalpostValidator.validerJournalpost("123", person, AccessType.M2M, saf).shouldBeLeft(
            JournalpostValidator.JournalpostValideringError.NotFound,
        )
    }

    test("feil mot saf gir feil") {
        coEvery { saf.hentJournalpost(any(), any()) } returns SafError.Error.left()

        JournalpostValidator.validerJournalpost("453857496", person, AccessType.M2M, saf).shouldBeLeft(
            JournalpostValidator.JournalpostValideringError.SafError,
        )
    }
})
