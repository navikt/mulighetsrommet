package no.nav.mulighetsrommet.api.tilskuddbehandling

import arrow.core.right
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.admin.journalpost.JournalpostValidator

/**
 * Test-double som lar alle journalposter passere valideringen mot SAF.
 */
fun gyldigJournalpostValidator(): JournalpostValidator = mockk {
    coEvery { validerJournalpostFinnes(any(), any(), any()) } returns Unit.right()
}
